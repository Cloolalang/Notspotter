package io.github.cloolalang.notspotdetector.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import io.github.cloolalang.notspotdetector.R
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
    modifier: Modifier = Modifier
) {
    val operatorLabel = OperatorTitleStyle.resolveLabel(stats)
    val brand = OperatorTitleStyle.detectBrand(stats)
    val isLowOrNoSignal = stats.isLowOrNoSignalForTitle(passiveSignalSettings)
    val operatorColor = operatorTitleColor(brand, isLowOrNoSignal)
    val titleStyle = MaterialTheme.typography.headlineMedium
    val titleWeight = FontWeight.Bold

    Row(modifier = modifier) {
        Text(
            text = if (operatorLabel == null) {
                stringResource(R.string.home_title)
            } else {
                "${stringResource(R.string.home_title)} - "
            },
            style = titleStyle,
            color = Sushi,
            fontWeight = titleWeight
        )
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
