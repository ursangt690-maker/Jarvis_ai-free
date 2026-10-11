package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.BatteryStd
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.domain.DeviceTelemetry
import com.example.domain.JarvisVoiceState
import com.example.ui.theme.JarvisCriticalRed
import com.example.ui.theme.JarvisOnlineGreen
import com.example.ui.theme.JarvisSpeakingCyan
import com.example.ui.theme.JarvisThinkingViolet
import com.example.ui.theme.JarvisWarningAmber
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun JarvisAnimatedOrb(
    voiceState: JarvisVoiceState,
    rmsLevel: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jarvis_orb_transition")

    val outerRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    JarvisVoiceState.THINKING -> 3200
                    JarvisVoiceState.LISTENING -> 5000
                    JarvisVoiceState.SPEAKING -> 4200
                    JarvisVoiceState.IDLE -> 12000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "outer_rotation"
    )

    val innerRotation by infiniteTransition.animateFloat(
        initialValue = 360f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    JarvisVoiceState.THINKING -> 2400
                    JarvisVoiceState.SPEAKING -> 3400
                    else -> 8000
                },
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "inner_rotation"
    )

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.88f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (voiceState) {
                    JarvisVoiceState.LISTENING -> 550
                    JarvisVoiceState.SPEAKING -> 420
                    JarvisVoiceState.THINKING -> 700
                    JarvisVoiceState.IDLE -> 2200
                }
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val stateAccentColor = when (voiceState) {
        JarvisVoiceState.IDLE -> primaryColor
        JarvisVoiceState.LISTENING -> JarvisOnlineGreen
        JarvisVoiceState.THINKING -> JarvisThinkingViolet
        JarvisVoiceState.SPEAKING -> JarvisSpeakingCyan
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(230.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "JARVIS Arc Reactor Voice Orb" }
            .testTag("jarvis_orb_button")
    ) {
        Canvas(modifier = Modifier.size(220.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val maxRadius = size.minDimension / 2f
            val dynamicBoost = (rmsLevel * 18f).coerceIn(0f, 24f)

            // Radial outer energy halo
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        stateAccentColor.copy(alpha = 0.36f),
                        primaryColor.copy(alpha = 0.12f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = maxRadius * pulseScale
                ),
                radius = maxRadius,
                center = center
            )

            // Outer segmented telemetry ring
            rotate(degrees = outerRotation, pivot = center) {
                val ringRadius = maxRadius * 0.88f
                val arcSize = Size(ringRadius * 2, ringRadius * 2)
                val topLeft = Offset(center.x - ringRadius, center.y - ringRadius)
                for (i in 0 until 6) {
                    drawArc(
                        color = stateAccentColor.copy(alpha = 0.85f),
                        startAngle = i * 60f + 8f,
                        sweepAngle = 38f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            // Counter-rotating tactical compass ticks
            rotate(degrees = innerRotation, pivot = center) {
                val midRadius = maxRadius * 0.72f
                val arcSize = Size(midRadius * 2, midRadius * 2)
                val topLeft = Offset(center.x - midRadius, center.y - midRadius)
                for (i in 0 until 12) {
                    drawArc(
                        color = secondaryColor.copy(alpha = 0.7f),
                        startAngle = i * 30f,
                        sweepAngle = 14f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Butt)
                    )
                }

                // Geometric radial spokes
                for (i in 0 until 8) {
                    val angleRad = Math.toRadians((i * 45.0))
                    val startR = maxRadius * 0.42f
                    val endR = maxRadius * 0.65f
                    drawLine(
                        color = primaryColor.copy(alpha = 0.45f),
                        start = Offset(
                            center.x + (startR * cos(angleRad)).toFloat(),
                            center.y + (startR * sin(angleRad)).toFloat()
                        ),
                        end = Offset(
                            center.x + (endR * cos(angleRad)).toFloat(),
                            center.y + (endR * sin(angleRad)).toFloat()
                        ),
                        strokeWidth = 1.5.dp.toPx()
                    )
                }
            }

            // Inner reactive core ring
            val coreRadius = (maxRadius * 0.40f * pulseScale) + dynamicBoost
            drawCircle(
                color = stateAccentColor.copy(alpha = 0.25f),
                radius = coreRadius * 1.15f,
                center = center
            )
            drawCircle(
                color = stateAccentColor,
                radius = coreRadius,
                center = center,
                style = Stroke(width = 3.dp.toPx())
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.9f),
                        stateAccentColor.copy(alpha = 0.75f),
                        primaryColor.copy(alpha = 0.2f)
                    ),
                    center = center,
                    radius = coreRadius
                ),
                radius = coreRadius * 0.82f,
                center = center
            )
        }

        // Center icon & status label overlay
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = when (voiceState) {
                    JarvisVoiceState.IDLE -> Icons.Default.Mic
                    JarvisVoiceState.LISTENING -> Icons.Default.GraphicEq
                    JarvisVoiceState.THINKING -> Icons.Default.Memory
                    JarvisVoiceState.SPEAKING -> Icons.Default.Stop
                },
                contentDescription = voiceState.name,
                tint = Color(0xFF040812),
                modifier = Modifier.size(30.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = when (voiceState) {
                    JarvisVoiceState.IDLE -> "ENGAGE"
                    JarvisVoiceState.LISTENING -> "LISTENING"
                    JarvisVoiceState.THINKING -> "THINKING"
                    JarvisVoiceState.SPEAKING -> "SPEAKING"
                },
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF040812)
            )
        }
    }
}

@Composable
fun AudioWaveformVisualizer(
    voiceState: JarvisVoiceState,
    rmsLevel: Float,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "waveform_transition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_phase"
    )

    val active = voiceState != JarvisVoiceState.IDLE
    val barColor = when (voiceState) {
        JarvisVoiceState.LISTENING -> JarvisOnlineGreen
        JarvisVoiceState.THINKING -> JarvisThinkingViolet
        JarvisVoiceState.SPEAKING -> JarvisSpeakingCyan
        JarvisVoiceState.IDLE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(horizontal = 24.dp)
    ) {
        val barCount = 28
        val spacing = size.width / barCount
        val maxBarHeight = size.height * 0.9f
        val baseAmplitude = if (active) (rmsLevel.coerceAtLeast(0.3f)) else 0.12f

        for (i in 0 until barCount) {
            val wave = sin(phase + i * 0.45f).toFloat()
            val normalized = ((wave + 1f) / 2f) * baseAmplitude
            val barH = (maxBarHeight * normalized).coerceAtLeast(4.dp.toPx())
            val x = i * spacing + spacing / 2f
            val topY = (size.height - barH) / 2f
            drawLine(
                color = barColor,
                start = Offset(x, topY),
                end = Offset(x, topY + barH),
                strokeWidth = 3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
fun TopTelemetryHudBar(
    telemetry: DeviceTelemetry,
    isApiKeyConfigured: Boolean,
    isForegroundActive: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.78f),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Neural Link Status
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (telemetry.isOnline && isApiKeyConfigured) Icons.Default.CloudDone else Icons.Default.CloudOff,
                    contentDescription = "AI Connection Status",
                    tint = when {
                        !telemetry.isOnline -> JarvisCriticalRed
                        isApiKeyConfigured -> JarvisOnlineGreen
                        else -> JarvisWarningAmber
                    },
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when {
                        !telemetry.isOnline -> "OFFLINE MODE"
                        isApiKeyConfigured -> "GEMINI LINKED"
                        else -> "KEY REQUIRED"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Network Type & Foreground Service Pill
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Wifi,
                    contentDescription = "Network",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isForegroundActive) "${telemetry.networkType} • BG" else telemetry.networkType,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Battery Indicator
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (telemetry.isCharging) Icons.Default.BatteryChargingFull else Icons.Default.BatteryStd,
                    contentDescription = "Battery Status",
                    tint = if (telemetry.batteryPercent <= 15) JarvisCriticalRed else JarvisOnlineGreen,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${telemetry.batteryPercent}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun HudFramedCard(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    accentColor: Color = MaterialTheme.colorScheme.primary,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.82f)
        ),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title.uppercase(),
                    style = MaterialTheme.typography.titleSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(accentColor.copy(alpha = 0.22f))
            )
            Spacer(modifier = Modifier.height(10.dp))
            content()
        }
    }
}
