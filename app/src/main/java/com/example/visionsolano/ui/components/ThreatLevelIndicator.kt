package com.example.visionsolano.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.ThreatLevel
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber

@Composable
fun ThreatLevelIndicator(
    threatLevel: ThreatLevel,
    threatPercentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedPercent by animateFloatAsState(
        targetValue = threatPercentage.coerceIn(0.05f, 1f),
        animationSpec = tween(600),
        label = "threatAnimation"
    )

    val activeColor = threatLevel.getColor()

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NIVEL DE AMENAZA",
                style = MaterialTheme.typography.labelMedium.copy(
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${(animatedPercent * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium.copy(
                        color = activeColor,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(6.dp))
                StatusBadge(
                    text = threatLevel.label,
                    accentColor = activeColor,
                    isPulsing = threatLevel != ThreatLevel.BAJO
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Segmented threat gauge
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurface)
                .border(1.dp, CardBorderColor, RoundedCornerShape(6.dp))
                .padding(2.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val stages = listOf(
                ThreatLevel.BAJO to StatusGreen,
                ThreatLevel.MEDIO to WarningAmber,
                ThreatLevel.ALTO to AlertRed.copy(alpha = 0.85f),
                ThreatLevel.CRITICO to AlertRed
            )

            stages.forEachIndexed { index, (stage, color) ->
                val isFilled = threatLevel.levelIndex >= stage.levelIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (isFilled) color else color.copy(alpha = 0.12f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Segment labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "BAJO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (threatLevel == ThreatLevel.BAJO) StatusGreen else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            )
            Text(
                text = "MEDIO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (threatLevel == ThreatLevel.MEDIO) WarningAmber else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            )
            Text(
                text = "ALTO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (threatLevel == ThreatLevel.ALTO) AlertRed else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            )
            Text(
                text = "CRÍTICO",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = if (threatLevel == ThreatLevel.CRITICO) AlertRed else TextSecondary.copy(alpha = 0.5f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
