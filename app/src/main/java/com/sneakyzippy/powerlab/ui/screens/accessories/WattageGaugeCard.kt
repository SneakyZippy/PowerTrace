package com.sneakyzippy.powerlab.ui.screens.accessories

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sneakyzippy.powerlab.core.model.LiveChargeTelemetry
import com.sneakyzippy.powerlab.ui.theme.DarkBackground
import com.sneakyzippy.powerlab.ui.theme.DarkSurface
import com.sneakyzippy.powerlab.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.ceil

@Composable
fun WattageGaugeCard(
    telemetry: LiveChargeTelemetry,
    gaugeColor: Color
) {
    if (!telemetry.isCharging) {
        StandbyTestCard(isGracePeriodActive = telemetry.isGracePeriodActive)
        return
    }

    val peakOrCurrent = maxOf(telemetry.currentWattage, telemetry.peakWattageSoFar, 1f)
    val maxScaleWatts = (ceil(peakOrCurrent / 15f) * 15f).coerceAtLeast(30f)
    val targetFraction = (telemetry.currentWattage / maxScaleWatts).coerceIn(0f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = targetFraction,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "wattageSweep"
    )

    val pulseTransition = rememberInfiniteTransition(label = "livePulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val tierLabel = when {
        telemetry.currentWattage >= 30f -> "Ultra Fast (USB-PD / SuperCharge)"
        telemetry.currentWattage >= 18f -> "Fast Charging"
        telemetry.currentWattage >= 10f -> "Standard Charging"
        telemetry.currentWattage > 0f -> "Trickle / Near Full"
        else -> "Plugged In (Maintaining)"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .alpha(pulseAlpha)
                        .clip(CircleShape)
                        .background(gaugeColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "LIVE CHARGING POWER",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(190.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.size(190.dp)) {
                    val strokeWidth = 16.dp.toPx()
                    val inset = strokeWidth / 2f + 4.dp.toPx()
                    val arcSize = Size(size.width - inset * 2f, size.height - inset * 2f)
                    val topLeft = Offset(inset, inset)
                    val startAngle = 150f
                    val totalSweep = 240f

                    // Track
                    drawArc(
                        color = DarkBackground,
                        startAngle = startAngle,
                        sweepAngle = totalSweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Subtle outer glow
                    if (animatedFraction > 0.01f) {
                        drawArc(
                            color = gaugeColor.copy(alpha = 0.22f),
                            startAngle = startAngle,
                            sweepAngle = totalSweep * animatedFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = strokeWidth + 8.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }

                    // Active arc
                    drawArc(
                        color = gaugeColor,
                        startAngle = startAngle,
                        sweepAngle = totalSweep * animatedFraction,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = String.format(Locale.US, "%.1f", telemetry.currentWattage),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = gaugeColor
                    )
                    Text(
                        text = "WATTS",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = gaugeColor.copy(alpha = 0.85f),
                        letterSpacing = 1.5.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(0.72f),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("0 W", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                Text("${maxScaleWatts.toInt()} W", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = tierLabel,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun StandbyTestCard(isGracePeriodActive: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(DarkBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = if (isGracePeriodActive) "Test Paused — Grace Period" else "Ready for Live Test",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Text(
                    text = "Plug in a charger & cable to measure live wattage",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary.copy(alpha = 0.8f)
                )
            }
        }
    }
}
