package com.example.visionsolano.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.R
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.DarkBackground
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary

@Composable
fun VisionSolanoTopBar(
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSystemActive: Boolean = true
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Discreet Branding Emblem
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurface)
                        .border(1.dp, CyberCyan.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.vision_solano_branding),
                        contentDescription = "Logo VisionSolano",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(10.dp))
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "VisionSolano",
                        style = MaterialTheme.typography.titleLarge.copy(
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    )
                    Spacer(modifier = Modifier.height(1.dp))
                    Text(
                        text = "Sistema de Vigilancia Inteligente",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = TextSecondary,
                            fontWeight = FontWeight.Normal
                        )
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusBadge(
                    text = if (isSystemActive) "SISTEMA ACTIVO" else "EN REPOSO",
                    accentColor = if (isSystemActive) StatusGreen else TextSecondary,
                    isPulsing = isSystemActive
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, CardBorderColor, RoundedCornerShape(10.dp))
                        .clickable { onSettingsClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = SurveillanceIcons.Settings,
                        contentDescription = "Configuración",
                        tint = CyberCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
