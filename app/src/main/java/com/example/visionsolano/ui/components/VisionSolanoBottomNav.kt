package com.example.visionsolano.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.visionsolano.ui.screens.Screen
import com.example.visionsolano.ui.theme.CardBorderColor
import com.example.visionsolano.ui.theme.CyberCyan
import com.example.visionsolano.ui.theme.CyanGlow
import com.example.visionsolano.ui.theme.DarkSurface
import com.example.visionsolano.ui.theme.DarkSurfaceVariant
import com.example.visionsolano.ui.theme.TextSecondary

@Composable
fun VisionSolanoBottomNav(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .border(
                width = 1.dp,
                color = CardBorderColor,
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            )
            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Screen.items.forEach { screen ->
                val isSelected = currentScreen.route == screen.route

                val icon = when (screen) {
                    Screen.Dashboard -> SurveillanceIcons.Home
                    Screen.Camera -> SurveillanceIcons.Videocam
                    Screen.Events -> SurveillanceIcons.History
                    Screen.Settings -> SurveillanceIcons.Settings
                }

                NavTabItem(
                    title = screen.title,
                    icon = icon,
                    isSelected = isSelected,
                    onClick = { onScreenSelected(screen) }
                )
            }
        }
    }
}

@Composable
private fun NavTabItem(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val activeColor by animateColorAsState(
        targetValue = if (isSelected) CyberCyan else TextSecondary,
        animationSpec = tween(250),
        label = "navColor"
    )

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) CyanGlow.copy(alpha = 0.15f) else DarkSurface.copy(alpha = 0f))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = activeColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = activeColor,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    fontSize = 10.sp
                )
            )
        }
    }
}
