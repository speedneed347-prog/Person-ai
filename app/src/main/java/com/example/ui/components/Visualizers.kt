package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisRose
import kotlin.math.sin

@Composable
fun SoundWaveVisualizer(
    isListening: Boolean,
    audioRmsDb: Float,
    modifier: Modifier = Modifier,
    barCount: Int = 9,
    accentColor: Color = AegisCyan
) {
    val transition = rememberInfiniteTransition(label = "wave_anim")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6.28f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase"
    )

    Row(
        modifier = modifier.height(38.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val baseRms = if (isListening) (audioRmsDb.coerceIn(0.5f, 10f) / 10f) else 0.15f

        for (i in 0 until barCount) {
            val waveFactor = sin(phase + i * 0.8f).coerceIn(-1f, 1f)
            val animatedHeight = if (isListening) {
                ((baseRms * 28f) + (waveFactor * 8f) + 8f).coerceIn(6f, 36f)
            } else {
                ((waveFactor * 4f) + 8f).coerceIn(4f, 14f)
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                accentColor,
                                AegisIndigo
                            )
                        )
                    )
            )
        }
    }
}

@Composable
fun AssistantAvatarCore(
    avatarStyle: String,
    isListening: Boolean,
    isSpeaking: Boolean,
    isBlocked: Boolean,
    size: Dp = 64.dp,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isListening || isSpeaking) 1.14f else 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isListening) 600 else 1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "avatar_pulse"
    )

    val coreColor = when {
        isBlocked -> AegisRose
        isListening -> AegisCyan
        isSpeaking -> AegisIndigo
        else -> AegisCyan
    }

    Box(
        modifier = modifier
            .size(size)
            .scale(pulseScale),
        contentAlignment = Alignment.Center
    ) {
        // Outer glow rings
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val radius = (size.toPx() / 2) - 4f

            drawCircle(
                color = coreColor.copy(alpha = 0.18f),
                radius = radius,
                center = center
            )
            drawCircle(
                color = coreColor.copy(alpha = 0.55f),
                radius = radius * 0.82f,
                center = center,
                style = Stroke(width = 2.5f)
            )
        }

        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            coreColor.copy(alpha = 0.9f),
                            coreColor.copy(alpha = 0.35f),
                            Color(0xFF0F172A)
                        )
                    )
                )
                .border(1.5.dp, coreColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            when (avatarStyle.uppercase()) {
                "SHIELD" -> Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.38f)
                )
                "CORE" -> Icon(
                    imageVector = Icons.Default.SmartToy,
                    contentDescription = "Core Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.38f)
                )
                "HOLO" -> Icon(
                    imageVector = Icons.Default.VerifiedUser,
                    contentDescription = "Holo Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.38f)
                )
                "NEON" -> Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Neon Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.38f)
                )
                else -> Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Orb Avatar",
                    tint = Color.White,
                    modifier = Modifier.size(size * 0.38f)
                )
            }
        }
    }
}

@Composable
fun BiometricShieldBadge(
    isVoiceProtected: Boolean,
    isOwnerOnly: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF131D31))
            .border(1.dp, AegisCyan.copy(alpha = 0.3f), RoundedCornerShape(20.dp)),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isVoiceProtected) AegisEmerald else AegisRose)
        )
        Text(
            text = if (isVoiceProtected) "VOICE LOCKED (OWNER)" else "VOICE OPEN",
            style = MaterialTheme.typography.labelSmall.copy(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            ),
            color = if (isVoiceProtected) AegisEmerald else AegisRose
        )
    }
}
