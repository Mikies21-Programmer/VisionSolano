package com.example.visionsolano.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.StatusGreenGlow
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary

@Composable
fun StatusBadge(
    text: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    isPulsing: Boolean = false
) {
    val alphaAnim = if (isPulsing) {
        val transition = rememberInfiniteTransition(label = "pulseTransition")
        val alpha by transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulseAlpha"
        )
        alpha
    } else {
        1f
    }

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(accentColor.copy(alpha = 0.12f))
            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .alpha(alphaAnim)
                .clip(CircleShape)
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 0.3.sp,
                fontSize = 9.sp
            ),
            maxLines = 1
        )
    }
}

@Composable
fun DeviceStatusCard(
    deviceTitle: String,
    statusLabel: String,
    statusColor: Color,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    extraInfo: String? = null
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.7f))
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurface)
                            .border(1.dp, CardBorderColor, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = deviceTitle,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = deviceTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                StatusBadge(
                    text = statusLabel,
                    accentColor = statusColor,
                    isPulsing = statusLabel == "ONLINE" || statusLabel == "ACTIVO"
                )
            }

            if (extraInfo != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = extraInfo,
                    style = MaterialTheme.typography.labelSmall.copy(color = TextSecondary)
                )
            }
        }
    }
}
