package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.cloolalang.notspotdetector.R
import io.github.cloolalang.notspotdetector.model.AppBrand
import io.github.cloolalang.notspotdetector.model.AppUpdateControls
import io.github.cloolalang.notspotdetector.model.AppUpdateStatus
import io.github.cloolalang.notspotdetector.model.ConnectivityStats
import io.github.cloolalang.notspotdetector.model.OperatorTitleStyle
import io.github.cloolalang.notspotdetector.model.PassiveSignalSettings
import io.github.cloolalang.notspotdetector.model.UkOperatorBrand
import io.github.cloolalang.notspotdetector.model.isLowOrNoSignalForTitle
import io.github.cloolalang.notspotdetector.ui.theme.Sushi

private val EeGoodColor = Color(0xFF2E7D32)
private val Vmo2GoodColor = Color(0xFF0066CC)

@Composable
fun HomeTitleBar(
    stats: ConnectivityStats,
    passiveSignalSettings: PassiveSignalSettings,
    appBrand: AppBrand = AppBrand.CELLNEX,
    showManualSelectOperator: Boolean = false,
    onChooseOperator: () -> Unit = {},
    versionLabel: String? = null,
    appUpdate: AppUpdateControls = AppUpdateControls(),
    modifier: Modifier = Modifier
) {
    val operatorLabel = OperatorTitleStyle.resolveLabel(stats)
    val brand = OperatorTitleStyle.detectBrand(stats)
    val isLowOrNoSignal = stats.isLowOrNoSignalForTitle(passiveSignalSettings)
    val operatorColor = operatorTitleColor(brand, isLowOrNoSignal)
    val titleStyle = MaterialTheme.typography.headlineMedium
    val titleWeight = FontWeight.Bold

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(
                    if (appBrand == AppBrand.CELLNEX) R.string.home_title_cellnex else R.string.home_title
                ),
                style = titleStyle,
                color = Sushi,
                fontWeight = titleWeight,
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.End) {
                if (versionLabel != null) {
                    Text(
                        text = versionLabel,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }
                val busy = appUpdate.status is AppUpdateStatus.Checking ||
                    appUpdate.status is AppUpdateStatus.Downloading
                TextButton(
                    onClick = appUpdate.onCheck,
                    enabled = !busy,
                    contentPadding = PaddingValues(horizontal = 0.dp, vertical = 0.dp)
                ) {
                    Text(
                        text = stringResource(R.string.app_update_check),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
        AppUpdateStatusLine(appUpdate)
        if (operatorLabel != null) {
            Text(
                text = operatorLabel,
                style = titleStyle.merge(
                    TextStyle(
                        color = operatorColor,
                        shadow = vodafoneLabelShadow(brand, isLowOrNoSignal)
                    )
                ),
                fontWeight = titleWeight
            )
        }
        if (showManualSelectOperator) {
            OutlinedButton(
                onClick = onChooseOperator,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = stringResource(R.string.monitoring_manual_select_operator),
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

@Composable
private fun AppUpdateStatusLine(appUpdate: AppUpdateControls) {
    val status = appUpdate.status
    val message = when (status) {
        AppUpdateStatus.Idle -> null
        AppUpdateStatus.Checking -> stringResource(R.string.app_update_checking)
        AppUpdateStatus.UpToDate -> stringResource(R.string.app_update_up_to_date)
        is AppUpdateStatus.Available ->
            stringResource(R.string.app_update_available, status.versionName)
        AppUpdateStatus.Downloading -> stringResource(R.string.app_update_downloading)
        is AppUpdateStatus.Ready ->
            stringResource(R.string.app_update_ready, status.versionName)
        AppUpdateStatus.NeedPermission -> stringResource(R.string.app_update_need_permission)
        is AppUpdateStatus.Failed -> stringResource(
            when (status.reason) {
                AppUpdateStatus.Reason.NoRelease -> R.string.app_update_no_release
                AppUpdateStatus.Reason.NoApk -> R.string.app_update_no_apk
                AppUpdateStatus.Reason.Network -> R.string.app_update_failed
            }
        )
    }
    if (message != null) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    when (status) {
        is AppUpdateStatus.Available -> {
            Button(onClick = appUpdate.onDownloadAndInstall, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.app_update_download))
            }
        }
        is AppUpdateStatus.Ready,
        AppUpdateStatus.NeedPermission -> {
            Button(onClick = appUpdate.onInstall, modifier = Modifier.fillMaxWidth()) {
                Text(text = stringResource(R.string.app_update_install))
            }
        }
        else -> Unit
    }
}

@Composable
private fun operatorTitleColor(brand: UkOperatorBrand, isLowOrNoSignal: Boolean): Color {
    if (isLowOrNoSignal) {
        return SignalTierColors.noSignal
    }
    return when (brand) {
        UkOperatorBrand.EE -> EeGoodColor
        UkOperatorBrand.VODAFONE -> Color.White
        UkOperatorBrand.VMO2 -> Vmo2GoodColor
        UkOperatorBrand.OTHER -> MaterialTheme.colorScheme.onSurface
    }
}

private fun vodafoneLabelShadow(brand: UkOperatorBrand, isLowOrNoSignal: Boolean): Shadow? {
    if (brand != UkOperatorBrand.VODAFONE || isLowOrNoSignal) {
        return null
    }
    return Shadow(
        color = Color.Black.copy(alpha = 0.55f),
        offset = Offset(1f, 1f),
        blurRadius = 2f
    )
}
