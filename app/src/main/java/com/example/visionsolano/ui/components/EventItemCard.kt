package com.example.visionsolano.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.data.model.EventPriority
import com.example.visionsolano.data.model.EventSource
import com.example.visionsolano.data.model.SecurityEvent
import com.example.visionsolano.ui.theme.AlertRed
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import com.example.visionsolano.ui.theme.WarningAmber

@Composable
fun EventItemCard(
    event: SecurityEvent,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val priorityColor = event.priority.getColor()

    val sourceIcon = when (event.source) {
        EventSource.FPGA -> SurveillanceIcons.Chip
        EventSource.ESP_CAM -> SurveillanceIcons.Videocam
        EventSource.SISTEMA -> SurveillanceIcons.Shield
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceVariant.copy(alpha = 0.85f))
            .border(1.dp, CardBorderColor, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Source icon container
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurface)
                    .border(1.dp, priorityColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = event.source.label,
                    tint = priorityColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Main details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = event.title.uppercase(),
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        ),
                        maxLines = 1
                    )

                    Text(
                        text = event.time,
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Text(
                    text = event.description,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = TextSecondary,
                        fontSize = 12.sp
                    ),
                    maxLines = 1
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Source tag
                    Text(
                        text = "ORIGEN: ${event.source.label}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = CyberCyan,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    )

                    // Priority Pill
                    StatusBadge(
                        text = event.priority.label,
                        accentColor = priorityColor,
                        isPulsing = event.priority == EventPriority.ALTA
                    )
                }
            }
        }
    }
}
