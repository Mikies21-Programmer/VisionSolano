package com.example.visionsolano.ui.screens.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.example.visionsolano.ui.theme.StatusGreen
import com.example.visionsolano.ui.theme.TextPrimary
import com.example.visionsolano.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alphaAnim = remember { Animatable(0f) }
    val scaleAnim = remember { Animatable(0.88f) }
    val progressAnim = remember { Animatable(0f) }

    // Pulsing cyber glow effect
    val transition = rememberInfiniteTransition(label = "splashPulse")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    LaunchedEffect(Unit) {
        // Smooth entrance animation
        alphaAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800)
        )
    }

    LaunchedEffect(Unit) {
        scaleAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 900)
        )
    }

    LaunchedEffect(Unit) {
        // Progress animation simulating subsystem boot
        progressAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 2200, easing = LinearEasing)
        )
        delay(300)
        onSplashFinished()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentAlignment = Alignment.Center
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .size(340.dp)
                .alpha(pulseAlpha * 0.4f)
                .background(
                    Brush.radialGradient(
                        listOf(CyberCyan.copy(alpha = 0.35f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
                .alpha(alphaAnim.value)
                .scale(scaleAnim.value)
        ) {
            // Branding Image Container
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(32.dp))
                    .border(
                        width = 2.dp,
                        brush = Brush.linearGradient(
                            listOf(CyberCyan, CyberCyan.copy(alpha = 0.2f), CardBorderColor)
                        ),
                        shape = RoundedCornerShape(32.dp)
                    )
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.vision_solano_branding),
                    contentDescription = "VisionSolano Branding",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(32.dp))
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "VisionSolano",
                style = MaterialTheme.typography.headlineLarge.copy(
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )

            Spacer(modifier = Modifier.height(6.dp))

            // App Subtitle
            Text(
                text = "Sistema de Vigilancia Inteligente",
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = CyberCyan,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Subsystem boot telemetry bar
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(DarkSurface)
                    .border(1.dp, CardBorderColor, RoundedCornerShape(6.dp))
                    .padding(2.dp)
            ) {
                LinearProgressIndicator(
                    progress = { progressAnim.value },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = CyberCyan,
                    trackColor = DarkSurface
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status message
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(StatusGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (progressAnim.value < 0.5f) "CARGANDO PROTOCOLOS DE VIGILANCIA..." else "FPGA: OK | ESP-CAM: STANDBY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        color = TextSecondary,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                )
            }
        }

        // Bottom version watermark
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
        ) {
            Text(
                text = "FPGA + ESP-CAM SURVEILLANCE PLATFORM v1.0",
                style = MaterialTheme.typography.labelSmall.copy(
                    color = TextSecondary.copy(alpha = 0.6f),
                    fontSize = 9.sp
                )
            )
        }
    }
}
