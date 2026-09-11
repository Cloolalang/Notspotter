package io.github.cloolalang.notspotdetector.network

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.AccessNetworkConstants
import android.telephony.CellIdentity
import android.telephony.CellIdentityGsm
import android.telephony.CellIdentityLte
import android.telephony.CellIdentityNr
import android.telephony.CellInfo
import android.telephony.CellInfoGsm
import android.telephony.CellInfoLte
import android.telephony.CellInfoNr
import android.telephony.CellSignalStrengthGsm
import android.telephony.CellSignalStrengthLte
import android.telephony.CellSignalStrengthNr
import android.telephony.NetworkRegistrationInfo
import android.telephony.ServiceState
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import io.github.cloolalang.notspotdetector.model.CellularRadioMetrics
import io.github.cloolalang.notspotdetector.model.LteLayerResilienceReading
import io.github.cloolalang.notspotdetector.model.MonitoringSettings
import io.github.cloolalang.notspotdetector.model.NetworkModePreference
import io.github.cloolalang.notspotdetector.model.NetworkServiceMode
import io.github.cloolalang.notspotdetector.model.isNoCellularService
import io.github.cloolalang.notspotdetector.model.isRadioPoweredOff
import io.github.cloolalang.notspotdetector.model.readNetworkModePreference
import io.github.cloolalang.notspotdetector.model.resolveServingOperatorFromCell

object CellularSignalReader {

    const val RADIO_2G = "2G"
    const val RADIO_4G = "4G"
    const val RADIO_5G = "5G"
    const val RADIO_5G_ENDC = "5G EN-DC"

    fun read(
        context: Context,
        monitor2gFallback: Boolean = false,
        subscriptionId: Int = MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
    ): CellularRadioMetrics {
        if (!hasPhoneStatePermission(context)) {
            return CellularRadioMetrics(permissionGranted = false)
        }

        return try {
            readInternal(context, monitor2gFallback, subscriptionId)
        } catch (_: SecurityException) {
            CellularRadioMetrics(permissionGranted = true)
        } catch (_: RuntimeException) {
            CellularRadioMetrics(permissionGranted = true)
        }
    }

    private fun readInternal(
        context: Context,
        monitor2gFallback: Boolean,
        subscriptionId: Int
    ): CellularRadioMetrics {
        val telephonyManager = SimSubscriptionHelper.telephonyManagerFor(context, subscriptionId)
            ?: return CellularRadioMetrics(permissionGranted = true)

        val cellIdentityPermissionGranted = hasCellIdentityPermission(context)
        val operatorInfo = readOperatorInfo(context, telephonyManager, subscriptionId)
        val plmn = operatorInfo.servingPlmn
        val networkReports2g = isServing2gNetwork(telephonyManager)
        val networkModePreference = readNetworkModePreference(telephonyManager)
        val restrictedTo2gNetwork = networkModePreference == NetworkModePreference.FORCED_2G
        val networkServiceMode = readNetworkServiceMode(telephonyManager)
        val isWifiCallingActive = readWifiCallingActive(telephonyManager)
        val simSlotIndex = SimSubscriptionHelper.resolveSlotIndex(context, subscriptionId)
        val simDisplayName = SimSubscriptionHelper.resolveSubscriptionLabel(context, subscriptionId)
        val hasLimitedServiceOnAnySim = hasLimitedServiceOnAnySubscription(context)
        val isDualSimActive = SimSubscriptionHelper.listActiveSubscriptions(context).size > 1
        val radioOff = networkServiceMode.isRadioPoweredOff()
        val isLimitedService = networkServiceMode == NetworkServiceMode.LIMITED_SERVICE
        val hasHomeGsmSignal = hasHomeGsmSignal(
            signalStrength = telephonyManager.signalStrength,
            monitor2gFallback = monitor2gFallback
        )
        val signalMetrics = readSignalStrength(
            signalStrength = telephonyManager.signalStrength,
            monitor2gFallback = monitor2gFallback,
            isOn2g = networkReports2g
        )
        val servingCell = if (radioOff || networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE) {
            ServingCellIdentity()
        } else {
            readServingCellForSignal(
                telephonyManager = telephonyManager,
                cellIdentityPermissionGranted = cellIdentityPermissionGranted,
                monitor2gFallback = monitor2gFallback,
                expectedPlmn = plmn,
                homePlmn = operatorInfo.homePlmn,
                signalMetrics = signalMetrics,
                networkReports2g = networkReports2g,
                isDualSimActive = isDualSimActive,
                isLimitedService = isLimitedService ||
                    networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE,
                registeredOnly = networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE
            )
        }
        val isOn2g = when {
            servingCell.radioAccessType in LTE_NR_RADIO_TYPES -> false
            servingCell.radioAccessType == RADIO_2G -> true
            else -> isEffectivelyOn2g(signalMetrics, networkReports2g)
        }
        val servingPlmn = servingCell.servingPlmn ?: plmn
        val servingOperatorName = resolveServingOperatorFromCell(
            telephonyServingName = operatorInfo.servingOperatorName,
            homeName = operatorInfo.homeOperatorName,
            homePlmn = operatorInfo.homePlmn,
            cellServingName = servingCell.servingOperatorName,
            cellServingPlmn = servingCell.servingPlmn
        )
        val lteLayerResilience = if (radioOff) {
            null
        } else {
            readLteLayerResilience(
                telephonyManager = telephonyManager,
                cellIdentityPermissionGranted = cellIdentityPermissionGranted,
                expectedPlmns = listOfNotNull(servingPlmn?.takeIf { it.length >= 5 }),
                isDualSimActive = isDualSimActive,
                primaryLteEarfcn = servingCell.lteEarfcn,
                acceptRegisteredPlmnMismatch = isLimitedService && servingCell.servingPlmn == null
            )
        }

        var metrics = signalMetrics.copy(
            rsrpDbm = signalMetrics.rsrpDbm ?: servingCell.rsrpDbm,
            rsrqDb = signalMetrics.rsrqDb ?: servingCell.rsrqDb,
            radioAccessType = mergeRadioAccessType(
                fromSignal = signalMetrics.radioAccessType,
                fromCell = servingCell.radioAccessType
            ),
            hasLteNrSignal = signalMetrics.hasLteNrSignal ||
                servingCell.radioAccessType in LTE_NR_RADIO_TYPES,
            networkOperatorName = operatorInfo.displayOperatorName,
            homeNetworkOperatorName = operatorInfo.homeOperatorName,
            servingNetworkOperatorName = servingOperatorName ?: operatorInfo.servingOperatorName,
            plmn = servingPlmn,
            homePlmn = operatorInfo.homePlmn,
            lteEarfcn = servingCell.lteEarfcn,
            ltePci = servingCell.ltePci,
            nrEarfcn = servingCell.nrEarfcn,
            nrPci = servingCell.nrPci,
            nrBand = servingCell.nrBand,
            gsmEarfcn = servingCell.gsmEarfcn,
            gsmBsic = servingCell.gsmBsic,
            cellIdentityPermissionGranted = cellIdentityPermissionGranted,
            isOn2g = isOn2g,
            networkModePreference = networkModePreference,
            restrictedTo2gNetwork = restrictedTo2gNetwork,
            isLimitedService = isLimitedService,
            networkServiceMode = networkServiceMode,
            isWifiCallingActive = isWifiCallingActive,
            hasHomeGsmSignal = hasHomeGsmSignal,
            subscriptionId = subscriptionId.takeIf {
                it != MonitoringSettings.DEFAULT_SUBSCRIPTION_ID
            },
            simSlotIndex = simSlotIndex,
            simDisplayName = simDisplayName,
            lteLayerResilience = lteLayerResilience
        )

        if (isOn2g && !monitor2gFallback && metrics.radioAccessType !in LTE_NR_RADIO_TYPES) {
            metrics = metrics.copy(
                radioAccessType = RADIO_2G,
                rsrpDbm = null,
                rsrqDb = null,
                lteEarfcn = null,
                ltePci = null,
                nrEarfcn = null,
                nrPci = null,
                gsmEarfcn = null,
                gsmBsic = null
            )
        } else if (isOn2g && monitor2gFallback && metrics.radioAccessType == null) {
            metrics = metrics.copy(radioAccessType = RADIO_2G)
        } else if (monitor2gFallback && restrictedTo2gNetwork && metrics.radioAccessType == null) {
            metrics = metrics.copy(
                radioAccessType = RADIO_2G,
                isOn2g = true
            )
        }

        if (radioOff || networkServiceMode == NetworkServiceMode.OUT_OF_SERVICE) {
            metrics = metrics.withoutCampedRadio().copy(
                isWifiCallingActive = if (radioOff) false else metrics.isWifiCallingActive
            )
        }
        val limitedOnAnySim = hasLimitedServiceOnAnySim || metrics.isLimitedService
        metrics = metrics.copy(
            hasLimitedServiceOnAnySim = limitedOnAnySim,
            isCompleteNoService = isCompleteNoService(metrics, limitedOnAnySim)
        )

        return metrics
    }

    /**
     * Telephony can still report 2G while LTE/NR is the active data bearer after an IRAT change
     * (e.g. voice camped on GSM with [TelephonyManager.getDataNetworkType] briefly unknown).
     */
    private fun isEffectivelyOn2g(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean
    ): Boolean {
        return when (signalMetrics.radioAccessType) {
            RADIO_2G -> true
            RADIO_4G, RADIO_5G, RADIO_5G_ENDC -> false
            else -> networkReports2g && !signalMetrics.hasLteNrSignal
        }
    }

    private fun shouldReadLteNrCellIdentity(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean
    ): Boolean {
        if (signalMetrics.radioAccessType in LTE_NR_RADIO_TYPES) return true
        if (signalMetrics.hasLteNrSignal) return true
        if (signalMetrics.radioAccessType == RADIO_2G) return false
        return !networkReports2g
    }

    private fun shouldReadGsmCellIdentity(
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean,
        monitor2gFallback: Boolean
    ): Boolean {
        if (!monitor2gFallback) return false
        if (signalMetrics.radioAccessType == RADIO_2G) return true
        return networkReports2g &&
            !signalMetrics.hasLteNrSignal &&
            signalMetrics.radioAccessType !in LTE_NR_RADIO_TYPES
    }

    @SuppressLint("MissingPermission")
    private fun readServingCellForSignal(
        telephonyManager: TelephonyManager,
        cellIdentityPermissionGranted: Boolean,
        monitor2gFallback: Boolean,
        expectedPlmn: String?,
        homePlmn: String?,
        signalMetrics: CellularRadioMetrics,
        networkReports2g: Boolean,
        isDualSimActive: Boolean,
        isLimitedService: Boolean,
        registeredOnly: Boolean = false
    ): ServingCellIdentity {
        if (!cellIdentityPermissionGranted) return ServingCellIdentity()

        val expectedPlmns = expectedPlmns(expectedPlmn, homePlmn)
        return when {
            isLimitedService || registeredOnly ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = monitor2gFallback,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = true,
                    registeredOnly = registeredOnly
                )
            shouldReadLteNrCellIdentity(signalMetrics, networkReports2g) ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = false,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = false,
                    registeredOnly = registeredOnly
                )
            shouldReadGsmCellIdentity(signalMetrics, networkReports2g, monitor2gFallback) ->
                readServingCellIdentities(
                    telephonyManager = telephonyManager,
                    monitor2gFallback = true,
                    expectedPlmns = expectedPlmns,
                    isDualSimActive = isDualSimActive,
                    acceptRegisteredPlmnMismatch = false,
                    registeredOnly = registeredOnly
                )
            else -> ServingCellIdentity()
        }
    }

    private fun mergeRadioAccessType(fromSignal: String?, fromCell: String?): String? {
        if (fromCell in LTE_NR_RADIO_TYPES && (fromSignal == null || fromSignal == RADIO_2G)) {
            return fromCell
        }
        return fromSignal ?: fromCell
    }

    private fun expectedPlmns(servingPlmn: String?, homePlmn: String?): List<String> {
        return listOfNotNull(
            servingPlmn?.trim()?.takeIf { it.length >= 5 },
            homePlmn?.trim()?.takeIf { it.length >= 5 }
        ).distinct()
    }

    @SuppressLint("MissingPermission")
    fun hasLimitedServiceOnAnySubscription(context: Context): Boolean {
        if (!hasPhoneStatePermission(context)) {
            return false
        }

        val subscriptions = SimSubscriptionHelper.listActiveSubscriptions(context)
        if (subscriptions.isEmpty()) {
            val telephonyManager = context.getSystemService(TelephonyManager::class.java)
                ?: return false
            return readNetworkServiceMode(telephonyManager) == NetworkServiceMode.LIMITED_SERVICE
        }

        return subscriptions.any { subscription ->
            val telephonyManager = SimSubscriptionHelper.telephonyManagerFor(
                context,
                subscription.subscriptionId
            ) ?: return@any false
            readNetworkServiceMode(telephonyManager) == NetworkServiceMode.LIMITED_SERVICE
        }
    }

    private fun isCompleteNoService(
        metrics: CellularRadioMetrics,
        hasLimitedServiceOnAnySim: Boolean
    ): Boolean {
        if (hasLimitedServiceOnAnySim) return false
        if (!metrics.permissionGranted) return false
        if (!metrics.networkServiceMode.isNoCellularService()) return false
        if (metrics.isOn2g) return false
        if (metrics.radioAccessType != null || metrics.rsrpDbm != null || metrics.rsrqDb != null) {
            return false
        }
        return true
    }

    private fun hasHomeGsmSignal(
        signalStrength: SignalStrength?,
        monitor2gFallback: Boolean
    ): Boolean {
        if (!monitor2gFallback || signalStrength == null) {
            return false
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            for (strength in signalStrength.cellSignalStrengths) {
                if (strength is CellSignalStrengthGsm && isValidMetric(strength.dbm)) {
                    return true
                }
            }
        } else {
            val dbm = gsmDbmFromLegacy(signalStrength)
            if (dbm != null) {
                return true
            }
        }

        return false
    }

    fun hasPhoneStatePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_PHONE_STATE
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasCellIdentityPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val fineGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
            return fineGranted || coarseGranted
        }
        return hasPhoneStatePermission(context)
    }

    private data class OperatorInfo(
        val homeOperatorName: String?,
        val servingOperatorName: String?,
        val homePlmn: String?,
        val servingPlmn: String?,
        val displayOperatorName: String?
    )

    @SuppressLint("MissingPermission")
    private fun readOperatorInfo(
        context: Context,
        telephonyManager: TelephonyManager,
        subscriptionId: Int
    ): OperatorInfo {
        val homeOperatorName = normalizeOperatorName(telephonyManager.simOperatorName)
            ?: SimSubscriptionHelper.resolveCarrierName(context, subscriptionId)
        val servingOperatorName = normalizeOperatorName(telephonyManager.networkOperatorName)
        val homePlmn = telephonyManager.simOperator
            .takeIf { it.isNotBlank() && it.length >= 5 }
        val servingPlmn = telephonyManager.networkOperator
            .takeIf { it.isNotBlank() && it.length >= 5 }
        val displayOperatorName = servingOperatorName ?: homeOperatorName
        return OperatorInfo(
            homeOperatorName = homeOperatorName,
            servingOperatorName = servingOperatorName,
            homePlmn = homePlmn,
            servingPlmn = servingPlmn,
            displayOperatorName = displayOperatorName
        )
    }

    private fun normalizeOperatorName(raw: String?): String? {
        return raw?.trim()?.takeIf { it.isNotBlank() && !it.equals("null", ignoreCase = true) }
    }

    @SuppressLint("MissingPermission")
    private fun readNetworkServiceMode(telephonyManager: TelephonyManager): NetworkServiceMode {
        val serviceState = telephonyManager.serviceState ?: return NetworkServiceMode.UNKNOWN

        // The status-bar no-service icon follows POWER_OFF / OUT_OF_SERVICE. Do not let
        // leftover emergency-only hints override that into limited service (RXSS 12).
        return when (serviceState.state) {
            ServiceState.STATE_POWER_OFF -> NetworkServiceMode.RADIO_OFF
            ServiceState.STATE_OUT_OF_SERVICE -> NetworkServiceMode.OUT_OF_SERVICE
            ServiceState.STATE_EMERGENCY_ONLY -> NetworkServiceMode.LIMITED_SERVICE
            ServiceState.STATE_IN_SERVICE -> NetworkServiceMode.IN_SERVICE
            else -> if (readEnhancedLimitedServiceState(serviceState)) {
                NetworkServiceMode.LIMITED_SERVICE
            } else {
                NetworkServiceMode.UNKNOWN
            }
        }
    }

    /**
     * API 30+ exposes registration states that can indicate emergency-only service even when
     * [ServiceState.getState] has not yet moved to [ServiceState.STATE_EMERGENCY_ONLY].
     */
    private fun readEnhancedLimitedServiceState(serviceState: ServiceState): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            invokeBooleanMethod(serviceState, "isEmergencyOnly")
        ) {
            return true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val notRegEmergency = runCatching {
                ServiceState::class.java.getField("REGISTRATION_STATE_NOT_REG_EMERGENCY").getInt(null)
            }.getOrNull() ?: return false
            val dataReg = invokeIntMethod(serviceState, "getDataRegistrationState")
            val voiceReg = invokeIntMethod(serviceState, "getVoiceRegistrationState")
            if (dataReg == notRegEmergency || voiceReg == notRegEmergency) {
                return true
            }
        }

        return false
    }

    /**
     * True when the modem is registered for service over a WLAN transport (WiFi calling / VoWiFi)
     * per [ServiceState.getNetworkRegistrationInfoList] — a public API added in API 30. Uses
     * [NetworkRegistrationInfo.isRegistered] rather than the registration-state getter/constants
     * (`getRegistrationState()`, `REGISTRATION_STATE_HOME`), which are `@SystemApi`-restricted and
     * unavailable to third-party apps. Falls back to the legacy public
     * [TelephonyManager.getDataNetworkType] IWLAN check below API 30 (less precise, OEM variable
     * — [ServiceState.getDataNetworkType] is `@SystemApi`-restricted, unlike the `TelephonyManager`
     * equivalent). See `RXSS_CATALOGUE.md` RXSS 31.
     */
    private fun readWifiCallingActive(telephonyManager: TelephonyManager): Boolean {
        val serviceState = telephonyManager.serviceState ?: return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            return runCatching {
                serviceState.networkRegistrationInfoList.any { info ->
                    info.transportType == AccessNetworkConstants.TRANSPORT_TYPE_WLAN &&
                        @Suppress("DEPRECATION") info.isRegistered
                }
            }.getOrDefault(false)
        }
        @Suppress("DEPRECATION")
        return runCatching {
            telephonyManager.dataNetworkType == TelephonyManager.NETWORK_TYPE_IWLAN
        }.getOrDefault(false)
    }

    private fun invokeBooleanMethod(target: Any, methodName: String): Boolean {
        return runCatching {
            target.javaClass.getMethod(methodName).invoke(target) as Boolean
        }.getOrDefault(false)
    }

    private fun invokeIntMethod(target: Any, methodName: String): Int {
        return runCatching {
            target.javaClass.getMethod(methodName).invoke(target) as Int
        }.getOrDefault(Int.MIN_VALUE)
    }

    @SuppressLint("MissingPermission")
    private fun isServing2gNetwork(telephonyManager: TelephonyManager): Boolean {
        val dataNetworkType = telephonyManager.dataNetworkType
        if (dataNetworkType in TWO_G_NETWORK_TYPES) {
            return true
        }
        val voiceNetworkType = telephonyManager.voiceNetworkType
        return voiceNetworkType in TWO_G_NETWORK_TYPES &&
            dataNetworkType == TelephonyManager.NETWORK_TYPE_UNKNOWN
    }

    @SuppressLint("MissingPermission")
    private fun readSignalStrength(
        signalStrength: SignalStrength?,
        monitor2gFallback: Boolean,
        isOn2g: Boolean
    ): CellularRadioMetrics {
        if (signalStrength == null) {
            return CellularRadioMetrics(permissionGranted = true)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            var lteMetrics: CellularRadioMetrics? = null
            var nrMetrics: CellularRadioMetrics? = null
            var gsmMetrics: CellularRadioMetrics? = null
            var hasLteNrSignal = false

            for (strength in signalStrength.cellSignalStrengths) {
                when (strength) {
                    is CellSignalStrengthNr -> {
                        val rsrp = strength.ssRsrp
                        val rsrq = strength.ssRsrq
                        if (isValidMetric(rsrp) || isValidMetric(rsrq)) {
                            hasLteNrSignal = true
                            nrMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
                                permissionGranted = true
                            )
                        }
                    }
                    is CellSignalStrengthLte -> {
                        val rsrp = strength.rsrp
                        val rsrq = strength.rsrq
                        if (isValidMetric(rsrp) || isValidMetric(rsrq)) {
                            hasLteNrSignal = true
                            lteMetrics = CellularRadioMetrics(
                                rsrpDbm = rsrp.takeIf { isValidMetric(it) },
                                rsrqDb = rsrq.takeIf { isValidMetric(it) },
                                permissionGranted = true
                            )
                        }
                    }
                    is CellSignalStrengthGsm -> {
                        if (monitor2gFallback && isOn2g) {
                            val dbm = strength.dbm
                            if (isValidMetric(dbm)) {
                                gsmMetrics = CellularRadioMetrics(
                                    rsrpDbm = dbm,
                                    permissionGranted = true
                                )
                            }
                        }
                    }
                }
            }

            val serving = when {
                lteMetrics != null && nrMetrics != null ->
                    lteMetrics.copy(radioAccessType = RADIO_5G_ENDC)
                nrMetrics != null ->
                    nrMetrics.copy(radioAccessType = RADIO_5G)
                lteMetrics != null ->
                    lteMetrics.copy(radioAccessType = RADIO_4G)
                gsmMetrics != null ->
                    gsmMetrics.copy(radioAccessType = RADIO_2G)
                else -> CellularRadioMetrics(permissionGranted = true)
            }
            return serving.copy(hasLteNrSignal = hasLteNrSignal)
        } else if (monitor2gFallback && isOn2g) {
            val dbm = gsmDbmFromLegacy(signalStrength)
            if (dbm != null) {
                return CellularRadioMetrics(
                    rsrpDbm = dbm,
                    radioAccessType = RADIO_2G,
                    permissionGranted = true,
                    hasLteNrSignal = false
                )
            }
        }

        return CellularRadioMetrics(permissionGranted = true)
    }

    private fun gsmDbmFromLegacy(signalStrength: SignalStrength): Int? {
        val asu = signalStrength.gsmSignalStrength
        if (asu == 99 || asu == CellInfo.UNAVAILABLE) {
            return null
        }
        return -113 + 2 * asu
    }

    /**
     * "4G layers detected" — splits currently visible LTE frequency layers (grouped by EARFCN)
     * into the primary channel (the one matching [primaryLteEarfcn], i.e. the serving/anchor
     * layer this device is actually camped on) and every other, "alternate", layer:
     *
     * - `primaryLayerCellCount`: number of detected cells (sectors) sharing the primary EARFCN.
     * - `alternateLayerCount`: number of distinct *other* EARFCNs detected.
     * - `alternateLayerCellCount`: total cells across all of those alternate EARFCNs combined.
     * - `primaryLayerDominanceDb`: RSRP gap (dB) between the primary/serving sector and the
     *   next-strongest *other* sector on that same primary EARFCN — see
     *   [computePrimaryLayerDominanceDb].
     *
     * Deliberately uses whatever the modem/UE already reports with no additional signal-quality
     * floor — any [CellInfoLte] entry with a valid EARFCN counts, regardless of its RSRP/RSRQ.
     *
     * Idle-mode neighbour visibility is inherently limited by 3GPP TS 36.304 measurement rules —
     * the UE only measures/reports frequency layers its serving cell's SIB4/SIB5 neighbour lists
     * reference, typically only once its own signal degrades — so this reflects *currently
     * detected* layers/cells, not an exhaustive survey of every LTE carrier physically present.
     *
     * Returns null when the cell-identity permission isn't granted or the read fails.
     */
    @SuppressLint("MissingPermission")
    private fun readLteLayerResilience(
        telephonyManager: TelephonyManager,
        cellIdentityPermissionGranted: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        primaryLteEarfcn: Int?,
        acceptRegisteredPlmnMismatch: Boolean
    ): LteLayerResilienceReading? {
        if (!cellIdentityPermissionGranted) return null

        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return null
            val hasExplicitLtePlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoLte }

            val eligibleCells = cellInfoList
                .filterIsInstance<CellInfoLte>()
                .filter { info ->
                    shouldUseCellIdentity(
                        identity = info.cellIdentity,
                        expectedPlmns = expectedPlmns,
                        hasExplicitPlmnMatches = hasExplicitLtePlmnMatches,
                        isDualSimActive = isDualSimActive,
                        isRegistered = info.isRegistered,
                        acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                    )
                }

            val earfcnsByLayer = eligibleCells
                .mapNotNull { info -> info.cellIdentity.earfcn.takeIf { isValidCellIdentityValue(it) } }
                .groupingBy { it }
                .eachCount()

            val primaryLayerCellCount = primaryLteEarfcn?.let { earfcnsByLayer[it] } ?: 0
            val alternateLayers = earfcnsByLayer.filterKeys { it != primaryLteEarfcn }

            LteLayerResilienceReading(
                primaryLayerCellCount = primaryLayerCellCount,
                alternateLayerCount = alternateLayers.size,
                alternateLayerCellCount = alternateLayers.values.sum(),
                primaryLayerDominanceDb = computePrimaryLayerDominanceDb(eligibleCells, primaryLteEarfcn)
            )
        } catch (_: SecurityException) {
            null
        } catch (_: RuntimeException) {
            null
        }
    }

    /**
     * "Primary cell level dominance" — the RSRP gap in dB between the serving/primary LTE sector
     * and the next-strongest *other* sector sharing the same (primary) EARFCN. The "serving"
     * sector within the primary-EARFCN group is picked by [CellInfoLte.isRegistered] when
     * available, otherwise by highest RSRP. Returns null when fewer than two sectors are
     * detected on the primary EARFCN, or when RSRP isn't available for the comparison — i.e.
     * dominance is only meaningful when there's an actual intra-channel competitor to compare
     * against.
     */
    @Suppress("DEPRECATION")
    private fun computePrimaryLayerDominanceDb(
        eligibleCells: List<CellInfoLte>,
        primaryLteEarfcn: Int?
    ): Int? {
        if (primaryLteEarfcn == null) return null

        val primaryLayerCells = eligibleCells.filter { info ->
            info.cellIdentity.earfcn.takeIf { isValidCellIdentityValue(it) } == primaryLteEarfcn
        }
        if (primaryLayerCells.size < 2) return null

        val samples = primaryLayerCells.map { info ->
            val rsrp = info.cellSignalStrength.rsrp.takeIf { isValidMetric(it) }
            Triple(info, info.isRegistered, rsrp)
        }

        val serving = samples.firstOrNull { it.second } ?: samples.maxByOrNull { it.third ?: Int.MIN_VALUE }
        val servingRsrp = serving?.third ?: return null
        val nextHighestRsrp = samples
            .filter { it.first !== serving.first }
            .mapNotNull { it.third }
            .maxOrNull()
            ?: return null

        return servingRsrp - nextHighestRsrp
    }

    @SuppressLint("MissingPermission")
    private fun readServingCellIdentities(
        telephonyManager: TelephonyManager,
        monitor2gFallback: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        acceptRegisteredPlmnMismatch: Boolean,
        registeredOnly: Boolean = false
    ): ServingCellIdentity {
        return try {
            val cellInfoList = telephonyManager.allCellInfo ?: return ServingCellIdentity()
            val registered = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = true,
                monitor2gFallback,
                expectedPlmns,
                isDualSimActive,
                acceptRegisteredPlmnMismatch
            )
            if (registeredOnly) {
                return registered ?: ServingCellIdentity()
            }
            val fromAllCells = extractServingCellIdentities(
                cellInfoList,
                registeredOnly = false,
                monitor2gFallback,
                expectedPlmns,
                isDualSimActive,
                acceptRegisteredPlmnMismatch
            )
            when {
                registered == null -> fromAllCells ?: ServingCellIdentity()
                fromAllCells == null -> registered
                else -> registered.fillGapsFrom(fromAllCells)
            }
        } catch (_: SecurityException) {
            ServingCellIdentity()
        } catch (_: RuntimeException) {
            ServingCellIdentity()
        }
    }

    private fun extractServingCellIdentities(
        cellInfoList: List<CellInfo>,
        registeredOnly: Boolean,
        monitor2gFallback: Boolean,
        expectedPlmns: Collection<String>,
        isDualSimActive: Boolean,
        acceptRegisteredPlmnMismatch: Boolean
    ): ServingCellIdentity? {
        var bestLte: RankedServingCell? = null
        var bestNr: RankedServingCell? = null
        var bestGsm: RankedServingCell? = null
        val hasExplicitLtePlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoLte }
        val hasExplicitNrPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoNr }
        val hasExplicitGsmPlmnMatches = hasExplicitPlmnMatchForRat(cellInfoList, expectedPlmns) { it is CellInfoGsm }

        for (info in cellInfoList) {
            if (registeredOnly && !info.isRegistered) continue
            val connectionRank = cellConnectionRank(info)
            if (registeredOnly && connectionRank == 0) continue

            when (info) {
                is CellInfoLte -> {
                    if (!shouldUseCellIdentity(
                            identity = info.cellIdentity,
                            expectedPlmns = expectedPlmns,
                            hasExplicitPlmnMatches = hasExplicitLtePlmnMatches,
                            isDualSimActive = isDualSimActive,
                            isRegistered = info.isRegistered,
                            acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                        )
                    ) {
                        continue
                    }
                    val identity = info.cellIdentity
                    val rsrp = info.cellSignalStrength.rsrp.takeIf { isValidMetric(it) }
                    val rsrq = info.cellSignalStrength.rsrq.takeIf { isValidMetric(it) }
                    val candidate = RankedServingCell(
                        connectionRank = connectionRank,
                        lteEarfcn = identity.earfcn.takeIf { isValidCellIdentityValue(it) },
                        ltePci = identity.pci.takeIf { isValidCellIdentityValue(it) },
                        rsrpDbm = rsrp,
                        rsrqDb = rsrq,
                        servingPlmn = formatIdentityPlmn(identity),
                        servingOperatorName = readIdentityOperatorName(identity)
                    )
                    if (candidate.hasValues && (bestLte == null || candidate beats bestLte)) {
                        bestLte = candidate
                    }
                }
                is CellInfoNr -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        if (!shouldUseCellIdentity(
                                identity = info.cellIdentity,
                                expectedPlmns = expectedPlmns,
                                hasExplicitPlmnMatches = hasExplicitNrPlmnMatches,
                                isDualSimActive = isDualSimActive,
                                isRegistered = info.isRegistered,
                                acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                            )
                        ) {
                            continue
                        }
                        val identity = info.cellIdentity
                        if (identity is CellIdentityNr) {
                            val strength = info.cellSignalStrength
                            val rsrp = if (strength is CellSignalStrengthNr) {
                                strength.ssRsrp.takeIf { isValidMetric(it) }
                            } else {
                                null
                            }
                            val rsrq = if (strength is CellSignalStrengthNr) {
                                strength.ssRsrq.takeIf { isValidMetric(it) }
                            } else {
                                null
                            }
                            val candidate = RankedServingCell(
                                connectionRank = connectionRank,
                                nrEarfcn = identity.nrarfcn.takeIf { isValidCellIdentityValue(it) },
                                nrPci = identity.pci.takeIf { isValidCellIdentityValue(it) },
                                nrBand = readNrBand(identity),
                                rsrpDbm = rsrp,
                                rsrqDb = rsrq,
                                servingPlmn = formatIdentityPlmn(identity),
                                servingOperatorName = readIdentityOperatorName(identity)
                            )
                            if (candidate.hasValues && (bestNr == null || candidate beats bestNr)) {
                                bestNr = candidate
                            }
                        }
                    }
                }
                is CellInfoGsm -> {
                    if (monitor2gFallback) {
                        if (!shouldUseCellIdentity(
                                identity = info.cellIdentity,
                                expectedPlmns = expectedPlmns,
                                hasExplicitPlmnMatches = hasExplicitGsmPlmnMatches,
                                isDualSimActive = isDualSimActive,
                                isRegistered = info.isRegistered,
                                acceptRegisteredPlmnMismatch = acceptRegisteredPlmnMismatch
                            )
                        ) {
                            continue
                        }
                        val identity = info.cellIdentity
                        val dbm = info.cellSignalStrength.dbm.takeIf { isValidMetric(it) }
                        val candidate = RankedServingCell(
                            connectionRank = connectionRank,
                            gsmEarfcn = identity.arfcn.takeIf { isValidCellIdentityValue(it) },
                            gsmBsic = identity.bsic.takeIf { isValidCellIdentityValue(it) },
                            rsrpDbm = dbm,
                            servingPlmn = formatIdentityPlmn(identity),
                            servingOperatorName = readIdentityOperatorName(identity)
                        )
                        if (candidate.hasValues && (bestGsm == null || candidate beats bestGsm)) {
                            bestGsm = candidate
                        }
                    }
                }
            }
        }

        return servingCellFromRanked(bestLte, bestNr, bestGsm)
    }

    private fun servingCellFromRanked(
        bestLte: RankedServingCell?,
        bestNr: RankedServingCell?,
        bestGsm: RankedServingCell?
    ): ServingCellIdentity? {
        val radioAccessType = when {
            bestLte != null && bestNr != null -> RADIO_5G_ENDC
            bestNr != null -> RADIO_5G
            bestLte != null -> RADIO_4G
            bestGsm != null -> RADIO_2G
            else -> null
        }
        val primary = when (radioAccessType) {
            RADIO_5G_ENDC, RADIO_4G -> bestLte
            RADIO_5G -> bestNr
            RADIO_2G -> bestGsm
            else -> bestLte ?: bestNr ?: bestGsm
        }
        val lteEarfcn = bestLte?.lteEarfcn
        val ltePci = bestLte?.ltePci
        val nrEarfcn = bestNr?.nrEarfcn
        val nrPci = bestNr?.nrPci
        val nrBand = bestNr?.nrBand
        val gsmEarfcn = bestGsm?.gsmEarfcn
        val gsmBsic = bestGsm?.gsmBsic

        if (lteEarfcn == null && ltePci == null && nrEarfcn == null && nrPci == null &&
            gsmEarfcn == null && gsmBsic == null
        ) {
            return null
        }
        return ServingCellIdentity(
            lteEarfcn = lteEarfcn,
            ltePci = ltePci,
            nrEarfcn = nrEarfcn,
            nrPci = nrPci,
            nrBand = nrBand,
            gsmEarfcn = gsmEarfcn,
            gsmBsic = gsmBsic,
            rsrpDbm = primary?.rsrpDbm,
            rsrqDb = primary?.rsrqDb,
            radioAccessType = radioAccessType,
            servingPlmn = primary?.servingPlmn,
            servingOperatorName = primary?.servingOperatorName
        )
    }

    /**
     * Reads the serving NR operating band directly from the modem via
     * [CellIdentityNr.getBands] (API 30+), rather than deriving it from the NR-ARFCN — NR-ARFCN
     * channel ranges overlap across multiple bands (e.g. n1/n66), so only the modem-reported band
     * is unambiguous. Returns the first reported band, or null below API 30 / when unavailable.
     */
    private fun readNrBand(identity: CellIdentityNr): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return runCatching { identity.bands.firstOrNull() }.getOrNull()
    }

    private fun hasExplicitPlmnMatchForRat(
        cellInfoList: List<CellInfo>,
        expectedPlmns: Collection<String>,
        ratFilter: (CellInfo) -> Boolean
    ): Boolean {
        return cellInfoList.any { info ->
            ratFilter(info) &&
                plmnMatchAny(info.cellIdentity, expectedPlmns) == PlmnMatchStatus.MATCH
        }
    }

    private enum class PlmnMatchStatus {
        MATCH,
        UNKNOWN,
        MISMATCH
    }

    private data class RankedServingCell(
        val connectionRank: Int,
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val nrBand: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null,
        val rsrpDbm: Int? = null,
        val rsrqDb: Int? = null,
        val servingPlmn: String? = null,
        val servingOperatorName: String? = null
    ) {
        val hasValues: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        infix fun beats(other: RankedServingCell): Boolean = connectionRank > other.connectionRank
    }

    /**
     * Decides whether a candidate [CellInfo] entry's identity may be used as the serving cell
     * for the *currently monitored* subscription.
     *
     * An explicit PLMN mismatch against every [expectedPlmns] value is rejected, except when
     * [acceptRegisteredPlmnMismatch] is set and the cell is registered — limited-service SOS
     * camp often keeps `TelephonyManager.networkOperator` on the home PLMN while the registered
     * cell is the visited operator. Cells with
     * [PlmnMatchStatus.UNKNOWN] (identity doesn't expose usable MCC/MNC) are accepted as
     * best-effort *unless* another cell of the same RAT in the same read explicitly matched —
     * in that case the explicit match already found the real serving cell, so the unverifiable
     * one is almost certainly stale/neighbor data and is dropped.
     *
     * [isDualSimActive] previously made this stricter — requiring an explicit PLMN match before
     * accepting anything — to guard against a suspected dual-SIM cross-contamination leak.
     * That turned out to be unsafe in practice: [expectedPlmn] (derived from
     * `TelephonyManager.networkOperator`) and/or the cell identity's own MCC/MNC are frequently
     * blank/unreliable on real devices (especially via a subscription-scoped `TelephonyManager`
     * from `createForSubscriptionId`), which made the strict gate reject *every* cell — wiping
     * out EARFCN/PCI/network-mode display for affected users regardless of SIM count. Reverted
     * to the lenient behavior; [isDualSimActive] is kept as a parameter (currently unused) so a
     * future, better-verified mitigation for the cross-SIM leak can reuse the plumbing without
     * re-touching every call site.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun shouldUseCellIdentity(
        identity: CellIdentity,
        expectedPlmns: Collection<String>,
        hasExplicitPlmnMatches: Boolean,
        isDualSimActive: Boolean,
        isRegistered: Boolean,
        acceptRegisteredPlmnMismatch: Boolean
    ): Boolean {
        return when (plmnMatchAny(identity, expectedPlmns)) {
            PlmnMatchStatus.MATCH -> true
            PlmnMatchStatus.UNKNOWN -> !hasExplicitPlmnMatches
            PlmnMatchStatus.MISMATCH -> acceptRegisteredPlmnMismatch && isRegistered
        }
    }

    private fun plmnMatchAny(
        identity: CellIdentity,
        expectedPlmns: Collection<String>
    ): PlmnMatchStatus {
        if (expectedPlmns.isEmpty()) return PlmnMatchStatus.UNKNOWN
        var sawMismatch = false
        for (plmn in expectedPlmns) {
            when (plmnMatchStatus(identity, plmn)) {
                PlmnMatchStatus.MATCH -> return PlmnMatchStatus.MATCH
                PlmnMatchStatus.UNKNOWN -> Unit
                PlmnMatchStatus.MISMATCH -> sawMismatch = true
            }
        }
        return if (sawMismatch) PlmnMatchStatus.MISMATCH else PlmnMatchStatus.UNKNOWN
    }

    private fun plmnMatchStatus(identity: CellIdentity, expectedPlmn: String?): PlmnMatchStatus {
        if (expectedPlmn.isNullOrBlank() || expectedPlmn.length < 5) {
            return PlmnMatchStatus.UNKNOWN
        }

        val (mcc, mnc) = readIdentityPlmn(identity) ?: return PlmnMatchStatus.UNKNOWN
        val expectedMcc = expectedPlmn.take(3)
        val expectedMnc = expectedPlmn.substring(3)
        return if (mcc == expectedMcc && normalizeMnc(mnc) == normalizeMnc(expectedMnc)) {
            PlmnMatchStatus.MATCH
        } else {
            PlmnMatchStatus.MISMATCH
        }
    }

    private fun readIdentityPlmn(identity: CellIdentity): Pair<String, String>? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val mcc = when (identity) {
                is CellIdentityLte -> identity.mccString
                is CellIdentityNr -> identity.mccString
                is CellIdentityGsm -> identity.mccString
                else -> null
            }?.takeIf { it.isNotBlank() }
            val mnc = when (identity) {
                is CellIdentityLte -> identity.mncString
                is CellIdentityNr -> identity.mncString
                is CellIdentityGsm -> identity.mncString
                else -> null
            }?.takeIf { it.isNotBlank() }
            if (mcc != null && mnc != null) {
                return mcc to mnc
            }
        }

        val mccInt = when (identity) {
            is CellIdentityLte -> identity.mcc
            is CellIdentityGsm -> identity.mcc
            else -> null
        }?.takeIf { isValidCellIdentityValue(it) }

        val mncInt = when (identity) {
            is CellIdentityLte -> identity.mnc
            is CellIdentityGsm -> identity.mnc
            else -> null
        }?.takeIf { isValidCellIdentityValue(it) }

        if (mccInt == null || mncInt == null) {
            return null
        }
        return mccInt.toString() to mncInt.toString()
    }

    private fun formatIdentityPlmn(identity: CellIdentity): String? {
        val (mcc, mnc) = readIdentityPlmn(identity) ?: return null
        val paddedMnc = if (mnc.length == 1) mnc.padStart(2, '0') else mnc
        return mcc + paddedMnc
    }

    private fun readIdentityOperatorName(identity: CellIdentity): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return normalizeOperatorName(identity.operatorAlphaLong?.toString())
            ?: normalizeOperatorName(identity.operatorAlphaShort?.toString())
    }

    private fun normalizeMnc(mnc: String): String {
        return mnc.trim().trimStart('0').ifEmpty { "0" }
    }

    private fun cellConnectionRank(cellInfo: CellInfo): Int {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return when (cellInfo.cellConnectionStatus) {
                CellInfo.CONNECTION_PRIMARY_SERVING -> 3
                CellInfo.CONNECTION_SECONDARY_SERVING -> 2
                else -> if (cellInfo.isRegistered) 1 else 0
            }
        }
        return if (cellInfo.isRegistered) 1 else 0
    }

    private data class ServingCellIdentity(
        val lteEarfcn: Int? = null,
        val ltePci: Int? = null,
        val nrEarfcn: Int? = null,
        val nrPci: Int? = null,
        val nrBand: Int? = null,
        val gsmEarfcn: Int? = null,
        val gsmBsic: Int? = null,
        val rsrpDbm: Int? = null,
        val rsrqDb: Int? = null,
        val radioAccessType: String? = null,
        val servingPlmn: String? = null,
        val servingOperatorName: String? = null
    ) {
        val hasCampedIdentity: Boolean
            get() = lteEarfcn != null || ltePci != null || nrEarfcn != null || nrPci != null ||
                gsmEarfcn != null || gsmBsic != null

        fun fillGapsFrom(fallback: ServingCellIdentity): ServingCellIdentity {
            return copy(
                lteEarfcn = lteEarfcn ?: fallback.lteEarfcn,
                ltePci = ltePci ?: fallback.ltePci?.takeIf {
                    lteEarfcn == null ||
                        fallback.lteEarfcn == null ||
                        lteEarfcn == fallback.lteEarfcn
                },
                nrEarfcn = nrEarfcn ?: fallback.nrEarfcn,
                nrPci = nrPci ?: fallback.nrPci?.takeIf {
                    nrEarfcn == null ||
                        fallback.nrEarfcn == null ||
                        nrEarfcn == fallback.nrEarfcn
                },
                nrBand = nrBand ?: fallback.nrBand?.takeIf {
                    nrEarfcn == null ||
                        fallback.nrEarfcn == null ||
                        nrEarfcn == fallback.nrEarfcn
                },
                gsmEarfcn = gsmEarfcn ?: fallback.gsmEarfcn,
                gsmBsic = gsmBsic ?: fallback.gsmBsic?.takeIf {
                    gsmEarfcn == null ||
                        fallback.gsmEarfcn == null ||
                        gsmEarfcn == fallback.gsmEarfcn
                },
                rsrpDbm = rsrpDbm ?: fallback.rsrpDbm,
                rsrqDb = rsrqDb ?: fallback.rsrqDb,
                radioAccessType = radioAccessType ?: fallback.radioAccessType,
                servingPlmn = servingPlmn ?: fallback.servingPlmn,
                servingOperatorName = servingOperatorName ?: fallback.servingOperatorName
            )
        }
    }

    private fun isValidCellIdentityValue(value: Int): Boolean {
        return value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE
    }

    private fun isValidMetric(value: Int): Boolean {
        return value != CellInfo.UNAVAILABLE && value != Int.MAX_VALUE && value != 0
    }

    private val TWO_G_NETWORK_TYPES = setOf(
        TelephonyManager.NETWORK_TYPE_GPRS,
        TelephonyManager.NETWORK_TYPE_EDGE,
        TelephonyManager.NETWORK_TYPE_GSM
    )

    private val LTE_NR_RADIO_TYPES = setOf(RADIO_4G, RADIO_5G, RADIO_5G_ENDC)
}
