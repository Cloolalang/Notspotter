package io.github.cloolalang.notspotdetector.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.MultiUeLinkPhase
import io.github.cloolalang.notspotdetector.model.MultiUeLinkSnapshot
import io.github.cloolalang.notspotdetector.model.MultiUePeer
import io.github.cloolalang.notspotdetector.model.MultiUeProtocol
import io.github.cloolalang.notspotdetector.model.MultiUeRole
import io.github.cloolalang.notspotdetector.multiue.MultiUePermissions
import io.github.cloolalang.notspotdetector.ui.theme.Sushi
import kotlinx.coroutines.delay

data class MultiUeActions(
    val onRoleChange: (MultiUeRole) -> Unit = {},
    val onConnect: (String) -> Unit = {},
    val onLookForMaster: () -> Unit = {},
    val onDisconnect: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onMasterSyncSettingsChange: (Boolean) -> Unit = {},
    val onSlaveSyncSettingsChange: (Boolean) -> Unit = {}
)

@Composable
fun MultiUeCard(
    snapshot: MultiUeLinkSnapshot,
    actions: MultiUeActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var expanded by panelFold("multi-ue", defaultExpanded = true)
    var pairingVisible by rememberSaveable { mutableStateOf(false) }
    var needsPermission by rememberSaveable {
        mutableStateOf(MultiUePermissions.missing(context).isNotEmpty())
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) {
        needsPermission = MultiUePermissions.missing(context).isNotEmpty()
        actions.onRefresh()
    }
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        actions.onRefresh()
    }
    val discoverableLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        pairingVisible = result.resultCode > 0
    }
    val unlocked = settingsControlsEnabled()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        needsPermission = MultiUePermissions.missing(context).isNotEmpty()
        actions.onRefresh()
    }
    LaunchedEffect(pairingVisible) {
        if (pairingVisible) {
            delay(300_000)
            pairingVisible = false
        }
    }

    fun selectRole(role: MultiUeRole) {
        actions.onRoleChange(role)
        if (role == MultiUeRole.OFF) return
        val missing = MultiUePermissions.missing(context)
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.multi_ue_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = Sushi,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!expanded) return@Column

            Text(
                text = stringResource(R.string.multi_ue_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            MultiUeRole.entries.forEach { role ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(
                            if (unlocked) {
                                Modifier.clickable { selectRole(role) }
                            } else {
                                Modifier
                            }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = snapshot.role == role,
                        onClick = { if (unlocked) selectRole(role) },
                        enabled = unlocked
                    )
                    Text(text = stringResource(roleLabel(role)))
                }
            }
            if (!unlocked) {
                Text(
                    text = stringResource(R.string.multi_ue_role_locked),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (snapshot.role == MultiUeRole.OFF) return@Column

            when {
                needsPermission -> {
                    Text(
                        text = stringResource(R.string.multi_ue_need_permission),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = {
                            val missing = MultiUePermissions.missing(context)
                            if (missing.isNotEmpty()) {
                                permissionLauncher.launch(missing.toTypedArray())
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.multi_ue_grant_permission))
                    }
                }
                !snapshot.bluetoothAvailable -> {
                    Text(
                        text = stringResource(R.string.multi_ue_not_available),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                !snapshot.bluetoothEnabled -> {
                    Text(
                        text = stringResource(R.string.multi_ue_bluetooth_off),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedButton(
                        onClick = { enableBluetoothLauncher.launch(bluetoothEnableIntent()) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = stringResource(R.string.multi_ue_turn_on_bluetooth))
                    }
                }
                snapshot.role == MultiUeRole.MASTER -> {
                    MasterLinkSection(
                        snapshot = snapshot,
                        pairingVisible = pairingVisible,
                        onAllowPairing = {
                            discoverableLauncher.launch(discoverableIntent())
                        },
                        onSyncSettingsChange = actions.onMasterSyncSettingsChange
                    )
                }
                else -> {
                    SlaveLinkSection(
                        snapshot = snapshot,
                        onLookForMaster = actions.onLookForMaster,
                        onConnect = actions.onConnect,
                        onDisconnect = actions.onDisconnect,
                        onSyncSettingsChange = actions.onSlaveSyncSettingsChange
                    )
                }
            }
        }
    }
}

@Composable
private fun MasterLinkSection(
    snapshot: MultiUeLinkSnapshot,
    pairingVisible: Boolean,
    onAllowPairing: () -> Unit,
    onSyncSettingsChange: (Boolean) -> Unit
) {
    SyncSettingsSwitch(
        checked = snapshot.masterSyncSettings,
        label = stringResource(R.string.multi_ue_sync_settings_master),
        hint = stringResource(R.string.multi_ue_sync_settings_master_hint),
        onCheckedChange = onSyncSettingsChange
    )
    if (snapshot.localName.isNotBlank()) {
        Text(
            text = stringResource(R.string.multi_ue_this_phone, snapshot.localName),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    Text(
        text = masterStatus(snapshot),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
    if (snapshot.connectedSlaves.isEmpty()) {
        Text(
            text = stringResource(R.string.multi_ue_no_slaves),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    } else {
        snapshot.connectedSlaves.forEach { slave ->
            Text(
                text = slave.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
    OutlinedButton(
        onClick = onAllowPairing,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(text = stringResource(R.string.multi_ue_allow_pairing))
    }
    Text(
        text = stringResource(
            if (pairingVisible) R.string.multi_ue_pairing_visible else R.string.multi_ue_pairing_hint
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Text(
        text = stringResource(R.string.multi_ue_master_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SlaveLinkSection(
    snapshot: MultiUeLinkSnapshot,
    onLookForMaster: () -> Unit,
    onConnect: (String) -> Unit,
    onDisconnect: () -> Unit,
    onSyncSettingsChange: (Boolean) -> Unit
) {
    SyncSettingsSwitch(
        checked = snapshot.slaveSyncSettings,
        label = stringResource(R.string.multi_ue_sync_settings_slave),
        hint = stringResource(R.string.multi_ue_sync_settings_slave_hint),
        onCheckedChange = onSyncSettingsChange
    )
    Text(
        text = slaveStatus(snapshot),
        style = MaterialTheme.typography.bodyMedium,
        fontWeight = FontWeight.Medium
    )
    if (snapshot.connectedMaster != null) {
        OutlinedButton(
            onClick = onDisconnect,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = stringResource(R.string.multi_ue_disconnect))
        }
    }
    OutlinedButton(
        onClick = onLookForMaster,
        enabled = snapshot.phase != MultiUeLinkPhase.CONNECTING,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = stringResource(
                if (snapshot.discovering) R.string.multi_ue_looking else R.string.multi_ue_look_for_master
            )
        )
    }
    val connectedAddress = snapshot.connectedMaster?.address
    val paired = snapshot.paired.filter { it.address != connectedAddress }
    val pairedAddresses = snapshot.paired.map { it.address }.toSet()
    val nearby = snapshot.nearby.filter {
        it.address != connectedAddress && it.address !in pairedAddresses
    }
    PeerList(
        title = stringResource(R.string.multi_ue_paired),
        peers = paired,
        enabled = snapshot.phase != MultiUeLinkPhase.CONNECTING,
        onConnect = onConnect
    )
    PeerList(
        title = stringResource(R.string.multi_ue_nearby),
        peers = nearby,
        enabled = snapshot.phase != MultiUeLinkPhase.CONNECTING,
        onConnect = onConnect
    )
    if (!snapshot.discovering && paired.isEmpty() && nearby.isEmpty() && snapshot.connectedMaster == null) {
        Text(
            text = stringResource(R.string.multi_ue_no_phones),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    Text(
        text = stringResource(
            if (snapshot.followsMaster) R.string.multi_ue_slave_hint else R.string.multi_ue_slave_local_hint
        ),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
private fun SyncSettingsSwitch(
    checked: Boolean,
    label: String,
    hint: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun PeerList(
    title: String,
    peers: List<MultiUePeer>,
    enabled: Boolean,
    onConnect: (String) -> Unit
) {
    if (peers.isEmpty()) return
    Text(
        text = title,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Medium
    )
    peers.forEach { peer ->
        OutlinedButton(
            onClick = { onConnect(peer.address) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = peer.name,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun masterStatus(snapshot: MultiUeLinkSnapshot): String {
    if (snapshot.phase == MultiUeLinkPhase.FAILED) {
        return stringResource(R.string.multi_ue_listen_failed)
    }
    return if (snapshot.connectedSlaves.isEmpty()) {
        stringResource(R.string.multi_ue_waiting_for_slaves)
    } else {
        stringResource(
            R.string.multi_ue_slaves_connected,
            snapshot.connectedSlaves.size,
            MultiUeProtocol.MAX_SLAVES
        )
    }
}

@Composable
private fun slaveStatus(snapshot: MultiUeLinkSnapshot): String {
    snapshot.connectedMaster?.let { master ->
        return stringResource(R.string.multi_ue_connected_to, master.name)
    }
    return when (snapshot.phase) {
        MultiUeLinkPhase.CONNECTING -> stringResource(
            R.string.multi_ue_connecting,
            targetName(snapshot)
        )
        MultiUeLinkPhase.RETRYING -> stringResource(R.string.multi_ue_retrying)
        MultiUeLinkPhase.FAILED -> stringResource(R.string.multi_ue_connection_failed)
        MultiUeLinkPhase.MASTER_FULL -> stringResource(R.string.multi_ue_master_full)
        MultiUeLinkPhase.IDLE -> stringResource(R.string.multi_ue_not_connected)
    }
}

private fun targetName(snapshot: MultiUeLinkSnapshot): String {
    val address = snapshot.savedMasterAddress
    val known = (snapshot.paired + snapshot.nearby).firstOrNull { it.address == address }
    return known?.name ?: address.orEmpty()
}

private fun roleLabel(role: MultiUeRole): Int {
    return when (role) {
        MultiUeRole.OFF -> R.string.multi_ue_role_off
        MultiUeRole.MASTER -> R.string.multi_ue_role_master
        MultiUeRole.SLAVE -> R.string.multi_ue_role_slave
    }
}

private fun discoverableIntent(): Intent {
    return Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
        .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
}

@SuppressLint("MissingPermission")
@Suppress("DEPRECATION")
private fun bluetoothEnableIntent(): Intent {
    return Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
}
