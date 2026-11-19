package com.example.presentation.counter

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.theme.NavyElevated
import com.example.presentation.theme.NavySanctum
import com.example.presentation.theme.NavyVoid
import com.example.presentation.theme.RudrakshaDark
import com.example.presentation.theme.RudrakshaLight
import com.example.presentation.theme.RudrakshaMid
import com.example.presentation.theme.SacredGold
import com.example.presentation.theme.SacredGoldBright
import com.example.presentation.theme.SacredGoldDim
import com.example.presentation.theme.SaffronAmber
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RudrakshaMalaWheel(
    currentCount: Int,
    targetCount: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 450f),
        label = "mala_tap_scale"
    )

    val visualBeads = when {
        targetCount <= 108 -> targetCount.coerceAtLeast(1)
        else -> 108
    }
    val activeBeadIndex = if (currentCount == 0) -1 else ((currentCount - 1) % visualBeads)
    val progress = (currentCount.toFloat() / targetCount.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .widthIn(max = 370.dp)
            .fillMaxWidth()
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val minDim = min(size.width, size.height)
            val center = Offset(size.width / 2f, size.height / 2f)
            val malaRadius = minDim * 0.42f
            val progressRingRadius = minDim * 0.34f

            // Subtle outer ambient aura
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        SacredGold.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = minDim * 0.49f
                ),
                radius = minDim * 0.49f,
                center = center
            )

            // Golden thread connecting beads
            drawCircle(
                color = SacredGoldDim.copy(alpha = 0.45f),
                radius = malaRadius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )

            // Inner golden progress track
            drawCircle(
                color = NavyElevated,
                radius = progressRingRadius,
                center = center,
                style = Stroke(width = 6.dp.toPx())
            )
            drawArc(
                brush = Brush.sweepGradient(
                    colors = listOf(SaffronAmber, SacredGold, SacredGoldBright, SacredGold)
                ),
                startAngle = -90f,
                sweepAngle = progress * 360f,
                useCenter = false,
                topLeft = Offset(center.x - progressRingRadius, center.y - progressRingRadius),
                size = Size(progressRingRadius * 2f, progressRingRadius * 2f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 108 (or target) Rudraksha beads around the circle
            val beadRadius = when {
                visualBeads <= 27 -> minDim * 0.028f
                visualBeads <= 54 -> minDim * 0.020f
                else -> minDim * 0.0115f
            }

            // Start just clockwise after Meru bead at -90 degrees
            val stepAngle = 360.0 / (visualBeads + 2)
            for (i in 0 until visualBeads) {
                val angleDeg = -90.0 + (i + 1.5) * stepAngle
                val rad = angleDeg * (PI / 180.0)
                val bx = center.x + (malaRadius * cos(rad)).toFloat()
                val by = center.y + (malaRadius * sin(rad)).toFloat()
                val beadCenter = Offset(bx, by)

                val isCompleted = i < activeBeadIndex
                val isActive = i == activeBeadIndex

                if (isActive) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                SacredGoldBright.copy(alpha = 0.9f),
                                SacredGold.copy(alpha = 0.35f),
                                Color.Transparent
                            ),
                            center = beadCenter,
                            radius = beadRadius * 2.8f
                        ),
                        radius = beadRadius * 2.8f,
                        center = beadCenter
                    )
                    drawCircle(
                        color = SacredGoldBright,
                        radius = beadRadius * 1.45f,
                        center = beadCenter
                    )
                } else if (isCompleted) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(SacredGold, RudrakshaLight),
                            center = beadCenter,
                            radius = beadRadius * 1.1f
                        ),
                        radius = beadRadius * 1.08f,
                        center = beadCenter
                    )
                } else {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(RudrakshaLight, RudrakshaMid, RudrakshaDark),
                            center = Offset(bx - beadRadius * 0.25f, by - beadRadius * 0.25f),
                            radius = beadRadius * 1.2f
                        ),
                        radius = beadRadius,
                        center = beadCenter
                    )
                }
            }

            // Prominent Meru / Guru Bead at 12 o'clock (-90 deg)
            val meruPos = Offset(center.x, center.y - malaRadius)
            val meruRadius = (beadRadius * 2.2f).coerceAtLeast(minDim * 0.028f)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SacredGoldBright.copy(alpha = 0.55f), Color.Transparent),
                    center = meruPos,
                    radius = meruRadius * 2.1f
                ),
                radius = meruRadius * 2.1f,
                center = meruPos
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(SacredGoldBright, SaffronAmber, RudrakshaDark),
                    center = Offset(meruPos.x - meruRadius * 0.2f, meruPos.y - meruRadius * 0.2f),
                    radius = meruRadius * 1.1f
                ),
                radius = meruRadius,
                center = meruPos
            )
            // Meru crown tassel indicator
            drawLine(
                color = SacredGold,
                start = Offset(meruPos.x, meruPos.y - meruRadius),
                end = Offset(meruPos.x, meruPos.y - meruRadius * 1.75f),
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        // Central Large Circular Tap Counter Button
        Box(
            modifier = Modifier
                .scale(scale)
                .size(196.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(NavyElevated, NavySanctum, NavyVoid)
                    )
                )
                .clickable(
                    interactionSource = interactionSource,
                    indication = ripple(bounded = true, color = SacredGold),
                    onClick = onTap
                )
                .testTag("jaap_tap_button")
                .semantics {
                    contentDescription = "Tap to count Jaap. Current count $currentCount of $targetCount"
                }
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "ॐ",
                    style = MaterialTheme.typography.titleLarge.copy(
                        color = SacredGold.copy(alpha = 0.85f),
                        fontSize = 22.sp
                    )
                )
                Text(
                    text = "$currentCount",
                    style = MaterialTheme.typography.displayLarge.copy(
                        color = SacredGoldBright,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "of $targetCount",
                    style = MaterialTheme.typography.labelLarge.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "TAP TO CHANT",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = SacredGold.copy(alpha = 0.78f),
                        letterSpacing = 1.4.sp
                    )
                )
            }
        }
    }
}
