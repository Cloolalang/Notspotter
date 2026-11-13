package io.github.cloolalang.notspotdetector.multiue

import android.annotation.SuppressLint
import android.app.Notification
import android.util.Base64
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.MainActivity
import io.github.cloolalang.notspotdetector.MonitorState
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.data.AppSettingsSnapshotCodec
import io.github.cloolalang.notspotdetector.data.MultiUeRoleRepository
import io.github.cloolalang.notspotdetector.data.RadioDebugSettingsRepository
import io.github.cloolalang.notspotdetector.model.AppSettingsSnapshot
import io.github.cloolalang.notspotdetector.model.MultiUeLinkPhase
import io.github.cloolalang.notspotdetector.model.RadioDebugSettings
import io.github.cloolalang.notspotdetector.model.MultiUePeer
import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import io.github.cloolalang.notspotdetector.model.MultiUeRole
import io.github.cloolalang.notspotdetector.model.MultiUeRunMode
import io.github.cloolalang.notspotdetector.model.multiUeRunPlan
import io.github.cloolalang.notspotdetector.service.ConnectivityMonitorService
import io.github.cloolalang.notspotdetector.service.NotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch

/**
 * Keeps the Bluetooth link alive while this phone is a master or a slave.
 * A slave follows the master's monitoring, including active mode, and applies
 * graph clears, panel open or collapsed state, scrolling, and the zone test.
 * The master sends those changes.
 */
class MultiUeLinkService : android.app.Service(), MultiUeSession.Listener {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val repository by lazy { MultiUeRoleRepository(this) }
    private val session = MultiUeSession(scope, ::peerOf, this)

    private var receiverRegistered = false
    private var sessionReady = false
    private var destroyed = false
    private var autoReconnect = true
    private var activeRole = MultiUeRole.OFF
    private var pendingBondAddress: String? = null
    private var sessionWatch: Job? = null
    private var panelWatch: Job? = null
    private var scrollWatch: Job? = null
    private var zoneWatch: Job? = null
    private var zoneNameWatch: Job? = null
    private var zoneHistogramWatch: Job? = null
    private var callConfigWatch: Job? = null
    private var callRunWatch: Job? = null
    private var planWatch: Job? = null
    private var pinWatch: Job? = null
    private var settingsWatch: Job? = null
    private val syncingSlaves = mutableSetOf<String>()
    private var reconnectJob: Job? = null
    private var switchJob: Job? = null
    private var listenRetry: Job? = null

    private val radioReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (destroyed || intent == null) return
            when (intent.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> onAdapterState(intent)
                BluetoothDevice.ACTION_FOUND -> onDeviceFound(intent)
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    MultiUeState.update { it.copy(discovering = false) }
                    refreshPaired()
                }
                BluetoothDevice.ACTION_BOND_STATE_CHANGED -> onBondState(intent)
            }
        }
    }

    override fun onBind(intent: Intent?) = null

    override fun onCreate() {
        super.onCreate()
        NotificationChannels.create(this)
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
        }
        ContextCompat.registerReceiver(this, radioReceiver, filter, ContextCompat.RECEIVER_EXPORTED)
        receiverRegistered = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!ensureForeground()) return START_NOT_STICKY
        when (intent?.action) {
            ACTION_DISCOVER -> {
                ensureSession()
                startDiscovery()
            }
            ACTION_CONNECT -> {
                ensureSession()
                val address = intent.getStringExtra(EXTRA_ADDRESS)
                if (!address.isNullOrBlank()) connectSlave(address)
            }
            ACTION_DISCONNECT -> disconnectSlave()
            ACTION_PUSH_SETTINGS -> {
                ensureSession()
                pushSettingsNow()
            }
            ACTION_SLAVE_SYNC -> {
                ensureSession()
                session.sendToMaster(MultiUeProtocol.sync(intent.getBooleanExtra(EXTRA_SYNC, false)))
            }
            ACTION_CLEAR -> {
                ensureSession()
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    session.broadcast(MultiUeProtocol.clearGraphs())
                }
            }
            ACTION_CLEAR_HISTORY -> {
                ensureSession()
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    session.broadcast(MultiUeProtocol.clearHistory())
                }
            }
            ACTION_ZONE_CLEAR -> {
                ensureSession()
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    session.broadcast(MultiUeProtocol.zoneClear())
                }
            }
            ACTION_PROFILE -> {
                ensureSession()
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    val payload = intent.getStringExtra(EXTRA_PROFILE_PAYLOAD).orEmpty()
                    if (payload.isNotEmpty()) {
                        val line = if (intent.getBooleanExtra(EXTRA_PROFILE_APPLY, false)) {
                            MultiUeProtocol.profileLoad(payload)
                        } else {
                            MultiUeProtocol.profilePut(payload)
                        }
                        session.broadcast(line)
                    }
                }
            }
            ACTION_ZONE_SAVE -> {
                ensureSession()
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    val includeHistogram = intent.getBooleanExtra(EXTRA_ZONE_SAVE_HISTOGRAM, false)
                    session.broadcast(MultiUeProtocol.zoneSave(includeHistogram))
                }
            }
            else -> applyRole(force = intent?.action == ACTION_APPLY_ROLE && sessionReady &&
                repository.loadRole() != activeRole)
        }
        return START_STICKY
    }

    override fun onDestroy() {
        destroyed = true
        autoReconnect = false
        sessionReady = false
        reconnectJob?.cancel()
        switchJob?.cancel()
        listenRetry?.cancel()
        sessionWatch?.cancel()
        panelWatch?.cancel()
        scrollWatch?.cancel()
        zoneWatch?.cancel()
        zoneNameWatch?.cancel()
        zoneHistogramWatch?.cancel()
        callConfigWatch?.cancel()
        callRunWatch?.cancel()
        planWatch?.cancel()
        pinWatch?.cancel()
        settingsWatch?.cancel()
        pendingBondAddress = null
        runCatching { MultiUeRadios.adapter(this)?.cancelDiscovery() }
        session.close()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(radioReceiver) }
            receiverRegistered = false
        }
        scope.coroutineContext[Job]?.cancel()
        super.onDestroy()
    }

    override fun onSlavesChanged(slaves: List<MultiUePeer>) {
        if (destroyed || activeRole != MultiUeRole.MASTER) return
        val addresses = slaves.map { it.address }.toSet()
        synchronized(syncingSlaves) { syncingSlaves.retainAll(addresses) }
        MultiUeState.update { it.copy(connectedSlaves = slaves) }
        updateNotification()
    }

    override fun onMasterReady() {
        if (destroyed) return
        MultiUeState.update { it.copy(phase = MultiUeLinkPhase.IDLE) }
        updateNotification()
    }

    override fun onListenFailed() {
        if (destroyed || activeRole != MultiUeRole.MASTER) return
        MultiUeState.update { it.copy(phase = MultiUeLinkPhase.FAILED) }
        listenRetry?.cancel()
        listenRetry = scope.launch {
            delay(LISTEN_RETRY_MS)
            val adapter = MultiUeRadios.adapter(this@MultiUeLinkService) ?: return@launch
            if (!destroyed && activeRole == MultiUeRole.MASTER && adapter.isEnabled && !session.listening) {
                session.startMaster(adapter)
            }
        }
    }

    override fun onConnecting(peer: MultiUePeer) {
        if (destroyed) return
        MultiUeState.update {
            it.copy(phase = MultiUeLinkPhase.CONNECTING, connectedMaster = null)
        }
        updateNotification()
    }

    override fun onMasterConnected(peer: MultiUePeer) {
        if (destroyed) return
        reconnectJob?.cancel()
        MultiUeState.update {
            it.copy(connectedMaster = peer, phase = MultiUeLinkPhase.IDLE)
        }
        session.sendToMaster(MultiUeProtocol.sync(repository.loadSlaveSyncSettings()))
        updateNotification()
    }

    override fun onSlaveSync(address: String, on: Boolean) {
        if (destroyed || activeRole != MultiUeRole.MASTER) return
        synchronized(syncingSlaves) {
            if (on) syncingSlaves.add(address) else syncingSlaves.remove(address)
        }
        if (on && repository.loadMasterSyncSettings()) {
            pushSettingsNow(setOf(address))
        }
    }

    override fun onSettings(payload: String) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        if (!repository.loadSlaveSyncSettings()) return
        val snapshot = decodeSettingsPayload(payload) ?: return
        MultiUeSettingsSync.offer(snapshot)
    }

    override fun onMasterLost() {
        if (destroyed || activeRole != MultiUeRole.SLAVE) return
        MultiUeState.update { it.copy(connectedMaster = null) }
        if (autoReconnect && pendingBondAddress == null) {
            MultiUeState.update { it.copy(phase = MultiUeLinkPhase.RETRYING) }
            scheduleReconnect()
        } else if (MultiUeState.snapshot.value.phase != MultiUeLinkPhase.MASTER_FULL) {
            MultiUeState.update { it.copy(phase = MultiUeLinkPhase.FAILED) }
        }
        updateNotification()
    }

    override fun onConnectionFailed() {
        if (destroyed || activeRole != MultiUeRole.SLAVE) return
        MultiUeState.update { it.copy(connectedMaster = null) }
        if (autoReconnect && pendingBondAddress == null) {
            MultiUeState.update { it.copy(phase = MultiUeLinkPhase.RETRYING) }
            scheduleReconnect()
        } else {
            MultiUeState.update { it.copy(phase = MultiUeLinkPhase.FAILED) }
        }
        updateNotification()
    }

    override fun onMasterFull() {
        if (destroyed) return
        autoReconnect = false
        reconnectJob?.cancel()
        pendingBondAddress = null
        MultiUeState.update {
            it.copy(connectedMaster = null, phase = MultiUeLinkPhase.MASTER_FULL)
        }
        updateNotification()
    }

    @SuppressLint("MissingPermission")
    override fun onBondRequired(device: BluetoothDevice) {
        if (destroyed) return
        pendingBondAddress = device.address
        val started = try {
            device.createBond()
        } catch (_: SecurityException) {
            false
        }
        if (!started) {
            pendingBondAddress = null
            autoReconnect = false
            onConnectionFailed()
        }
    }

    override fun onPassiveCommand(on: Boolean) {
        onSession(if (on) MultiUeRunMode.PASSIVE else MultiUeRunMode.STOPPED)
    }

    override fun onSession(mode: MultiUeRunMode) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        applyRunCommand(mode)
    }

    override fun onClearGraphs() {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MonitorState.clearRsrpHistogram()
        MonitorState.clearRttHistory()
        MultiUeGraphClear.resetTapes()
    }

    override fun onClearHistory() {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeGraphClear.resetTapes()
    }

    override fun onZone(running: Boolean, wholeTest: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeZoneSync.set(running, wholeTest)
    }

    override fun onZoneName(suffix: String) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeZoneSync.setNameSuffix(suffix)
    }

    override fun onPlan(jpegBase64: String?) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        if (jpegBase64.isNullOrEmpty()) {
            MultiUeFloorPlan.clearPlan()
            return
        }
        val bytes = runCatching { Base64.decode(jpegBase64, Base64.NO_WRAP) }.getOrNull() ?: return
        if (bytes.isEmpty()) return
        MultiUeFloorPlan.setJpeg(bytes)
    }

    override fun onPinAdd(x: Float, y: Float) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeFloorPlan.addPin(x, y)
    }

    override fun onPinUndo() {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeFloorPlan.undoPin()
    }

    override fun onPinClear() {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeFloorPlan.clearPins()
    }

    override fun onZoneClear() {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeZoneSync.clear()
    }

    override fun onZoneSave(includeHistogram: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeZoneSync.save(includeHistogram)
    }

    override fun onZoneHistogram(on: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeZoneSync.setSaveHistogram(on)
    }

    override fun onCallConfig(phoneNumber: String, callDurationSec: Int, delayBetweenCallsSec: Int) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeCallSync.setSlaveConfig(phoneNumber, callDurationSec, delayBetweenCallsSec)
    }

    override fun onCallLimit(on: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeCallSync.setMasterTimeout(on)
    }

    override fun onCallMute(on: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeCallSync.setMuteCallAudio(on)
    }

    override fun onCallRun(running: Boolean) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeCallSync.setRunning(running)
    }

    override fun onProfile(apply: Boolean, payload: String) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeProfileSync.offer(apply, payload)
    }

    override fun onPanels(open: Map<String, Boolean>) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        PanelFold.replace(open)
    }

    override fun onScroll(fraction: Int) {
        if (destroyed || repository.loadRole() != MultiUeRole.SLAVE) return
        MultiUeScroll.replace(fraction)
    }

    override fun runMode(): MultiUeRunMode = currentRunMode()

    override fun panelsLine(): String = MultiUeProtocol.panels(PanelFold.snapshot())

    override fun scrollLine(): String? {
        return MultiUeScroll.fraction.value?.let(MultiUeProtocol::scroll)
    }

    override fun zoneLine(): String? {
        val zone = MultiUeZoneSync.state.value ?: return null
        return MultiUeProtocol.zone(zone.running, zone.wholeTest)
    }

    override fun zoneNameLine(): String? {
        val suffix = MultiUeZoneSync.nameSuffix.value ?: return null
        return MultiUeProtocol.zoneName(suffix)
    }

    override fun zoneHistogramLine(): String? {
        val on = MultiUeZoneSync.saveHistogram.value ?: return null
        return MultiUeProtocol.zoneHistogram(on)
    }

    override fun callConfigLine(): String? {
        val config = MultiUeCallSync.config.value ?: return null
        return MultiUeProtocol.callConfig(
            config.phoneNumber,
            config.callDurationSec,
            config.delayBetweenCallsSec
        )
    }

    override fun callLimitLine(): String? {
        val config = MultiUeCallSync.config.value ?: return null
        return MultiUeProtocol.callLimit(config.masterTimeoutEnabled)
    }

    override fun callMuteLine(): String? {
        val config = MultiUeCallSync.config.value ?: return null
        return MultiUeProtocol.callMute(config.muteCallAudio)
    }

    override fun callRunLine(): String? {
        val running = MultiUeCallSync.running.value ?: return null
        return MultiUeProtocol.callRun(running)
    }

    override fun planLine(): String? {
        val jpeg = MultiUeFloorPlan.jpeg.value ?: return null
        if (jpeg.isEmpty()) return MultiUeProtocol.planClear()
        return MultiUeProtocol.plan(Base64.encodeToString(jpeg, Base64.NO_WRAP))
    }

    override fun pinLines(): List<String> {
        val pins = MultiUeFloorPlan.pins.value
        return listOf(MultiUeProtocol.pinClear()) + pins.map { MultiUeProtocol.pin(it.x, it.y) }
    }

    private fun ensureSession() {
        if (!sessionReady) applyRole(force = true)
    }

    private fun applyRole(force: Boolean) {
        if (destroyed) return
        val role = repository.loadRole()
        val info = MultiUeRadios.read(this)
        if (!MultiUePermissions.granted(this) || role == MultiUeRole.OFF) {
            stopSelf()
            return
        }
        if (!force && sessionReady && activeRole == role) {
            MultiUeState.update {
                it.copy(
                    role = role,
                    bluetoothAvailable = info.available,
                    bluetoothEnabled = info.enabled,
                    localName = info.localName,
                    permissionGranted = true,
                    savedMasterAddress = repository.loadMasterAddress()
                )
            }
            refreshPaired()
            resumeRadio(info.enabled)
            return
        }
        tearDownSession()
        activeRole = role
        autoReconnect = true
        sessionReady = true
        MultiUeState.update {
            it.copy(
                role = role,
                bluetoothAvailable = info.available,
                bluetoothEnabled = info.enabled,
                localName = info.localName,
                permissionGranted = true,
                savedMasterAddress = repository.loadMasterAddress(),
                connectedSlaves = emptyList(),
                connectedMaster = null,
                nearby = emptyList(),
                discovering = false,
                phase = MultiUeLinkPhase.IDLE
            )
        }
        refreshPaired()
        when (role) {
            MultiUeRole.MASTER -> {
                watchSession()
                watchPanels()
                watchScroll()
                watchZone()
                watchZoneName()
                watchZoneHistogram()
                watchCallTest()
                watchPlan()
                watchPins()
                watchSettings()
                resumeRadio(info.enabled)
            }
            MultiUeRole.SLAVE -> resumeRadio(info.enabled)
            MultiUeRole.OFF -> Unit
        }
        updateNotification()
    }

    @SuppressLint("MissingPermission")
    private fun resumeRadio(enabled: Boolean) {
        val adapter = MultiUeRadios.adapter(this) ?: return
        if (!enabled || !adapter.isEnabled) return
        when (activeRole) {
            MultiUeRole.MASTER -> if (!session.listening) session.startMaster(adapter)
            MultiUeRole.SLAVE -> {
                if (!session.slaveBusy && autoReconnect && pendingBondAddress == null) {
                    val address = repository.loadMasterAddress() ?: return
                    session.connect(adapter, address)
                }
            }
            MultiUeRole.OFF -> Unit
        }
    }

    private fun tearDownSession() {
        sessionReady = false
        sessionWatch?.cancel()
        sessionWatch = null
        panelWatch?.cancel()
        panelWatch = null
        scrollWatch?.cancel()
        scrollWatch = null
        zoneWatch?.cancel()
        zoneWatch = null
        zoneNameWatch?.cancel()
        zoneNameWatch = null
        zoneHistogramWatch?.cancel()
        zoneHistogramWatch = null
        callConfigWatch?.cancel()
        callConfigWatch = null
        callRunWatch?.cancel()
        callRunWatch = null
        planWatch?.cancel()
        planWatch = null
        pinWatch?.cancel()
        pinWatch = null
        settingsWatch?.cancel()
        settingsWatch = null
        synchronized(syncingSlaves) { syncingSlaves.clear() }
        reconnectJob?.cancel()
        listenRetry?.cancel()
        pendingBondAddress = null
        session.close()
    }

    @OptIn(FlowPreview::class)
    private fun watchSession() {
        sessionWatch?.cancel()
        sessionWatch = scope.launch {
            combine(MonitorState.isRunning, MonitorState.stats) { running, stats ->
                when {
                    running && stats.isMonitoring && stats.isPassiveOnlySession -> MultiUeRunMode.PASSIVE
                    running && stats.isMonitoring -> MultiUeRunMode.ACTIVE
                    else -> MultiUeRunMode.STOPPED
                }
            }
                .debounce(PASSIVE_PUBLISH_MS)
                .distinctUntilChanged()
                .collect { mode ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.session(mode))
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun watchPanels() {
        panelWatch?.cancel()
        panelWatch = scope.launch {
            PanelFold.open
                .debounce(PANEL_PUBLISH_MS)
                .distinctUntilChanged()
                .collect { open ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.panels(open))
                    }
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun watchZoneName() {
        zoneNameWatch?.cancel()
        zoneNameWatch = scope.launch {
            MultiUeZoneSync.nameSuffix
                .filterNotNull()
                .distinctUntilChanged()
                .debounce(ZONE_NAME_PUBLISH_MS)
                .collect { suffix ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.zoneName(suffix))
                    }
                }
        }
    }

    private fun watchPlan() {
        planWatch?.cancel()
        planWatch = scope.launch {
            MultiUeFloorPlan.jpeg
                .filterNotNull()
                .distinctUntilChanged { left, right -> left.contentEquals(right) }
                .collect { bytes ->
                    if (repository.loadRole() != MultiUeRole.MASTER) return@collect
                    val line = if (bytes.isEmpty()) {
                        MultiUeProtocol.planClear()
                    } else {
                        MultiUeProtocol.plan(Base64.encodeToString(bytes, Base64.NO_WRAP))
                    }
                    session.broadcast(line)
                }
        }
    }

    private fun watchPins() {
        pinWatch?.cancel()
        pinWatch = scope.launch {
            MultiUeFloorPlan.pinLines.collect { line ->
                if (repository.loadRole() == MultiUeRole.MASTER) {
                    session.broadcast(line)
                }
            }
        }
    }

    private fun watchZoneHistogram() {
        zoneHistogramWatch?.cancel()
        zoneHistogramWatch = scope.launch {
            MultiUeZoneSync.saveHistogram
                .filterNotNull()
                .distinctUntilChanged()
                .collect { on ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.zoneHistogram(on))
                    }
                }
        }
    }

    private fun watchCallTest() {
        callConfigWatch?.cancel()
        callConfigWatch = scope.launch {
            MultiUeCallSync.config
                .filterNotNull()
                .distinctUntilChanged()
                .collect { config ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.callLimit(config.masterTimeoutEnabled))
                        session.broadcast(MultiUeProtocol.callMute(config.muteCallAudio))
                        session.broadcast(
                            MultiUeProtocol.callConfig(
                                config.phoneNumber,
                                config.callDurationSec,
                                config.delayBetweenCallsSec
                            )
                        )
                    }
                }
        }
        callRunWatch?.cancel()
        callRunWatch = scope.launch {
            MultiUeCallSync.running
                .filterNotNull()
                .distinctUntilChanged()
                .collect { running ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.callRun(running))
                    }
                }
        }
    }

    private fun watchZone() {
        zoneWatch?.cancel()
        zoneWatch = scope.launch {
            MultiUeZoneSync.state
                .filterNotNull()
                .distinctUntilChanged()
                .collect { zone ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.zone(zone.running, zone.wholeTest))
                    }
                }
        }
    }

    private fun watchScroll() {
        scrollWatch?.cancel()
        scrollWatch = scope.launch {
            MultiUeScroll.fraction
                .filterNotNull()
                .distinctUntilChanged()
                .conflate()
                .collect { fraction ->
                    if (repository.loadRole() == MultiUeRole.MASTER) {
                        session.broadcast(MultiUeProtocol.scroll(fraction))
                    }
                    delay(SCROLL_PUBLISH_MS)
                }
        }
    }

    @OptIn(FlowPreview::class)
    private fun watchSettings() {
        settingsWatch?.cancel()
        settingsWatch = scope.launch {
            merge(
                MonitorState.thresholds.map { },
                MonitorState.pingSettings.map { },
                MonitorState.monitoringSettings.map { },
                MonitorState.passiveSignalSettings.map { },
                MonitorState.passiveMockSettings.map { },
                MonitorState.audioVolumes.map { },
                MultiUeSettingsSync.revision.map { }
            )
                .debounce(SETTINGS_PUBLISH_MS)
                .map { captureSettingsPayload() }
                .distinctUntilChanged()
                .collect { payload ->
                    if (repository.loadRole() != MultiUeRole.MASTER) return@collect
                    if (!repository.loadMasterSyncSettings()) return@collect
                    val addresses = synchronized(syncingSlaves) { syncingSlaves.toSet() }
                    session.sendToSlaves(addresses, MultiUeProtocol.settings(payload))
                }
        }
    }

    private fun pushSettingsNow(only: Set<String>? = null) {
        if (repository.loadRole() != MultiUeRole.MASTER || !repository.loadMasterSyncSettings()) return
        val addresses = synchronized(syncingSlaves) {
            if (only == null) syncingSlaves.toSet() else syncingSlaves.intersect(only)
        }
        if (addresses.isEmpty()) return
        session.sendToSlaves(addresses, MultiUeProtocol.settings(captureSettingsPayload()))
    }

    private fun captureSettingsPayload(): String {
        val snapshot = AppSettingsSnapshot(
            thresholds = MonitorState.thresholds.value,
            pingSettings = MonitorState.pingSettings.value,
            monitoringSettings = MonitorState.monitoringSettings.value,
            passiveSignalSettings = MonitorState.passiveSignalSettings.value,
            passiveMockSettings = MonitorState.passiveMockSettings.value,
            audioVolumes = MonitorState.audioVolumes.value,
            radioDebugSettings = RadioDebugSettings(
                enabled = RadioDebugSettingsRepository(this).loadEnabled()
            )
        ).normalized()
        val json = AppSettingsSnapshotCodec.encodeSnapshot(snapshot)
        return Base64.encodeToString(json.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
    }

    private fun decodeSettingsPayload(payload: String): AppSettingsSnapshot? {
        val json = runCatching {
            String(Base64.decode(payload, Base64.NO_WRAP), Charsets.UTF_8)
        }.getOrNull() ?: return null
        return AppSettingsSnapshotCodec.decodeSnapshot(json)
    }

    private fun connectSlave(address: String) {
        if (activeRole != MultiUeRole.SLAVE) return
        val adapter = MultiUeRadios.adapter(this) ?: return
        if (!adapter.isEnabled) return
        autoReconnect = true
        pendingBondAddress = null
        reconnectJob?.cancel()
        repository.saveMasterAddress(address)
        MultiUeState.update { it.copy(savedMasterAddress = address, nearby = it.nearby) }
        session.connect(adapter, address)
    }

    private fun disconnectSlave() {
        autoReconnect = false
        reconnectJob?.cancel()
        pendingBondAddress = null
        session.close()
        sessionReady = true
        MultiUeState.update {
            it.copy(connectedMaster = null, phase = MultiUeLinkPhase.IDLE, discovering = false)
        }
        updateNotification()
    }

    private fun scheduleReconnect() {
        reconnectJob?.cancel()
        val address = repository.loadMasterAddress() ?: return
        reconnectJob = scope.launch {
            delay(RECONNECT_MS)
            if (destroyed || !autoReconnect || activeRole != MultiUeRole.SLAVE || session.slaveBusy) return@launch
            val adapter = MultiUeRadios.adapter(this@MultiUeLinkService) ?: return@launch
            if (!adapter.isEnabled) return@launch
            session.connect(adapter, address)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        if (activeRole != MultiUeRole.SLAVE) return
        val adapter = MultiUeRadios.adapter(this) ?: return
        if (!adapter.isEnabled) return
        MultiUeState.update {
            it.copy(
                discovering = true,
                nearby = emptyList(),
                phase = if (it.connectedMaster != null) it.phase else MultiUeLinkPhase.IDLE
            )
        }
        val started = try {
            adapter.startDiscovery()
        } catch (_: SecurityException) {
            false
        }
        if (!started) {
            MultiUeState.update { current ->
                current.copy(
                    discovering = false,
                    phase = if (current.connectedMaster != null) {
                        current.phase
                    } else {
                        MultiUeLinkPhase.FAILED
                    }
                )
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun onDeviceFound(intent: Intent) {
        val device = deviceExtra(intent) ?: return
        val peer = try {
            peerOf(device)
        } catch (_: SecurityException) {
            return
        }
        MultiUeState.update { snapshot ->
            if (snapshot.nearby.any { it.address == peer.address }) snapshot
            else snapshot.copy(nearby = (snapshot.nearby + peer).take(MAX_NEARBY))
        }
    }

    @SuppressLint("MissingPermission")
    private fun onBondState(intent: Intent) {
        val device = deviceExtra(intent) ?: return
        val state = intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE, BluetoothDevice.BOND_NONE)
        val previous = intent.getIntExtra(
            BluetoothDevice.EXTRA_PREVIOUS_BOND_STATE,
            BluetoothDevice.BOND_NONE
        )
        refreshPaired()
        if (device.address != pendingBondAddress) return
        when (state) {
            BluetoothDevice.BOND_BONDED -> {
                pendingBondAddress = null
                val adapter = MultiUeRadios.adapter(this) ?: return
                session.connect(adapter, device.address)
            }
            BluetoothDevice.BOND_NONE -> if (previous == BluetoothDevice.BOND_BONDING) {
                pendingBondAddress = null
                autoReconnect = false
                onConnectionFailed()
            }
        }
    }

    private fun onAdapterState(intent: Intent) {
        val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
        val info = MultiUeRadios.read(this)
        MultiUeState.update {
            it.copy(
                bluetoothAvailable = info.available,
                bluetoothEnabled = state == BluetoothAdapter.STATE_ON,
                localName = info.localName.ifBlank { it.localName }
            )
        }
        when (state) {
            BluetoothAdapter.STATE_OFF, BluetoothAdapter.STATE_TURNING_OFF -> {
                session.close()
                reconnectJob?.cancel()
                MultiUeState.update {
                    it.copy(
                        connectedSlaves = emptyList(),
                        connectedMaster = null,
                        discovering = false,
                        phase = MultiUeLinkPhase.IDLE
                    )
                }
                updateNotification()
            }
            BluetoothAdapter.STATE_ON -> resumeRadio(enabled = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun refreshPaired() {
        val adapter = MultiUeRadios.adapter(this) ?: return
        if (!adapter.isEnabled || !MultiUePermissions.granted(this)) return
        val paired = try {
            adapter.bondedDevices.orEmpty().map { peerOf(it) }
        } catch (_: SecurityException) {
            emptyList()
        }
        MultiUeState.update { it.copy(paired = paired) }
    }

    private fun applyRunCommand(mode: MultiUeRunMode) {
        val running = MonitorState.isRunning.value
        val stats = MonitorState.stats.value
        val plan = multiUeRunPlan(
            isRunning = running,
            isMonitoring = running && stats.isMonitoring,
            passiveSession = running && stats.isMonitoring && stats.isPassiveOnlySession,
            command = mode
        )
        switchJob?.cancel()
        when {
            plan.stopFirst && !plan.startPassive && !plan.startActive ->
                ConnectivityMonitorService.stop(this)
            plan.startPassive && !plan.stopFirst ->
                ConnectivityMonitorService.start(this, passiveOnly = true)
            plan.startActive && !plan.stopFirst ->
                ConnectivityMonitorService.start(this, passiveOnly = false)
            plan.stopFirst && (plan.startPassive || plan.startActive) -> {
                val passive = plan.startPassive
                ConnectivityMonitorService.stop(this)
                switchJob = scope.launch {
                    delay(PASSIVE_RESTART_MS)
                    if (!destroyed && repository.loadRole() == MultiUeRole.SLAVE) {
                        ConnectivityMonitorService.start(
                            this@MultiUeLinkService,
                            passiveOnly = passive
                        )
                    }
                }
            }
        }
    }

    private fun currentRunMode(): MultiUeRunMode {
        val stats = MonitorState.stats.value
        val running = MonitorState.isRunning.value && stats.isMonitoring
        return when {
            !running -> MultiUeRunMode.STOPPED
            stats.isPassiveOnlySession -> MultiUeRunMode.PASSIVE
            else -> MultiUeRunMode.ACTIVE
        }
    }

    @SuppressLint("MissingPermission")
    private fun peerOf(device: BluetoothDevice): MultiUePeer {
        val raw = try {
            device.name
        } catch (_: SecurityException) {
            null
        }
        val name = raw?.takeIf { it.isNotBlank() }
            ?: getString(R.string.multi_ue_unknown_phone, device.address.takeLast(5))
        return MultiUePeer(address = device.address, name = name)
    }

    private fun deviceExtra(intent: Intent): BluetoothDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
        }
    }

    private fun ensureForeground(): Boolean {
        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            return true
        } catch (_: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                runCatching {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
                    )
                }
            }
            stopSelf()
            return false
        }
    }

    private fun updateNotification() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snapshot = MultiUeState.snapshot.value
        val title: String
        val text: String
        if (snapshot.role == MultiUeRole.MASTER) {
            title = getString(R.string.notification_multi_ue_master)
            text = getString(
                R.string.notification_multi_ue_master_text,
                snapshot.connectedSlaves.size,
                MultiUeProtocol.MAX_SLAVES
            )
        } else {
            title = getString(R.string.notification_multi_ue_slave)
            text = snapshot.connectedMaster?.let { master ->
                getString(R.string.notification_multi_ue_slave_connected, master.name)
            } ?: getString(R.string.notification_multi_ue_slave_waiting)
        }
        return NotificationCompat.Builder(this, NotificationChannels.MULTI_UE_CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openApp)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    companion object {
        private const val ACTION_APPLY_ROLE =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_APPLY"
        private const val ACTION_DISCOVER =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_DISCOVER"
        private const val ACTION_CONNECT =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_CONNECT"
        private const val ACTION_DISCONNECT =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_DISCONNECT"
        private const val ACTION_PUSH_SETTINGS =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_PUSH_SETTINGS"
        private const val ACTION_SLAVE_SYNC =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_SLAVE_SYNC"
        private const val ACTION_CLEAR =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_CLEAR"
        private const val ACTION_CLEAR_HISTORY =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_CLEAR_HISTORY"
        private const val ACTION_ZONE_CLEAR =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_ZONE_CLEAR"
        private const val ACTION_ZONE_SAVE =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_ZONE_SAVE"
        private const val ACTION_PROFILE =
            "io.github.cloolalang.notspotdetector.action.MULTI_UE_PROFILE"
        private const val EXTRA_PROFILE_APPLY =
            "io.github.cloolalang.notspotdetector.extra.MULTI_UE_PROFILE_APPLY"
        private const val EXTRA_PROFILE_PAYLOAD =
            "io.github.cloolalang.notspotdetector.extra.MULTI_UE_PROFILE_PAYLOAD"
        private const val EXTRA_ZONE_SAVE_HISTOGRAM =
            "io.github.cloolalang.notspotdetector.extra.MULTI_UE_ZONE_SAVE_HISTOGRAM"
        private const val EXTRA_ADDRESS =
            "io.github.cloolalang.notspotdetector.extra.MULTI_UE_ADDRESS"
        private const val EXTRA_SYNC =
            "io.github.cloolalang.notspotdetector.extra.MULTI_UE_SYNC"
        private const val NOTIFICATION_ID = 1002
        private const val PASSIVE_PUBLISH_MS = 200L
        private const val PANEL_PUBLISH_MS = 150L
        private const val SCROLL_PUBLISH_MS = 40L
        private const val ZONE_NAME_PUBLISH_MS = 200L
        private const val SETTINGS_PUBLISH_MS = 400L
        private const val PASSIVE_RESTART_MS = 1_000L
        private const val RECONNECT_MS = 3_000L
        private const val LISTEN_RETRY_MS = 2_000L
        private const val MAX_NEARBY = 20

        fun start(context: Context) = launch(context, ACTION_APPLY_ROLE)

        fun discover(context: Context) = launch(context, ACTION_DISCOVER)

        fun connect(context: Context, address: String) = launch(context, ACTION_CONNECT, address)

        fun disconnect(context: Context) = launch(context, ACTION_DISCONNECT)

        fun pushSettings(context: Context) = launch(context, ACTION_PUSH_SETTINGS)

        fun broadcastClear(context: Context) = launch(context, ACTION_CLEAR)

        fun broadcastClearHistory(context: Context) = launch(context, ACTION_CLEAR_HISTORY)

        fun broadcastZoneClear(context: Context) = launch(context, ACTION_ZONE_CLEAR)

        fun broadcastProfile(context: Context, apply: Boolean, payload: String) {
            val intent = Intent(context, MultiUeLinkService::class.java)
                .setAction(ACTION_PROFILE)
                .putExtra(EXTRA_PROFILE_APPLY, apply)
                .putExtra(EXTRA_PROFILE_PAYLOAD, payload)
            ContextCompat.startForegroundService(context, intent)
        }

        fun broadcastZoneSave(context: Context, includeHistogram: Boolean = false) {
            val intent = Intent(context, MultiUeLinkService::class.java)
                .setAction(ACTION_ZONE_SAVE)
                .putExtra(EXTRA_ZONE_SAVE_HISTOGRAM, includeHistogram)
            ContextCompat.startForegroundService(context, intent)
        }

        fun notifySlaveSync(context: Context, enabled: Boolean) {
            val intent = Intent(context, MultiUeLinkService::class.java)
                .setAction(ACTION_SLAVE_SYNC)
                .putExtra(EXTRA_SYNC, enabled)
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, MultiUeLinkService::class.java))
        }

        private fun launch(context: Context, action: String, address: String? = null) {
            val intent = Intent(context, MultiUeLinkService::class.java).setAction(action)
            if (address != null) intent.putExtra(EXTRA_ADDRESS, address)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}
