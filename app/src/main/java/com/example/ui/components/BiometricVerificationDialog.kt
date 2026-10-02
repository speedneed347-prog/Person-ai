package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisOutline
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceCard
import kotlinx.coroutines.delay

@Composable
fun BiometricVerificationDialog(
    title: String,
    subtitle: String,
    ownerName: String,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Face, 1: Fingerprint
    var isVerifying by remember { mutableStateOf(false) }
    var isVerified by remember { mutableStateOf(false) }
    var statusText by remember { mutableStateOf("Touch sensor or scan face to authenticate") }

    val infiniteTransition = rememberInfiniteTransition(label = "scan_laser")
    val laserY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "laser"
    )

    LaunchedEffect(isVerifying) {
        if (isVerifying) {
            statusText = if (selectedTab == 0) "Analyzing facial geometry mesh..." else "Scanning hardware fingerprint..."
            delay(1000)
            isVerified = true
            statusText = "Identity Confirmed: $ownerName"
            delay(600)
            onSuccess()
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isVerifying) onDismiss()
        },
        containerColor = AegisSurface,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = AegisCyan
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = Color.Transparent,
                    contentColor = AegisCyan,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = AegisCyan
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            if (!isVerifying) {
                                selectedTab = 0
                                isVerified = false
                                statusText = "Align face in oval scanner"
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Face, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Face", fontSize = 13.sp)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            if (!isVerifying) {
                                selectedTab = 1
                                isVerified = false
                                statusText = "Touch fingerprint scanner"
                            }
                        },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Fingerprint, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Fingerprint", fontSize = 13.sp)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedTab == 0) {
                    // Face scan preview / simulator
                    Box(
                        modifier = Modifier
                            .size(150.dp, 170.dp)
                            .clip(RoundedCornerShape(75.dp))
                            .background(Color(0xFF070B14))
                            .border(
                                2.dp,
                                if (isVerified) AegisEmerald else AegisCyan,
                                RoundedCornerShape(75.dp)
                            )
                            .clickable(enabled = !isVerifying && !isVerified) {
                                isVerifying = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(150.dp, 170.dp)) {
                            if (isVerifying && !isVerified) {
                                // Animated horizontal scan line
                                val currentY = size.height * laserY
                                drawLine(
                                    color = AegisCyan,
                                    start = Offset(10f, currentY),
                                    end = Offset(size.width - 10f, currentY),
                                    strokeWidth = 3f
                                )
                            }
                        }

                        if (isVerified) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Verified",
                                tint = AegisEmerald,
                                modifier = Modifier.size(64.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Face,
                                contentDescription = "Face Target",
                                tint = if (isVerifying) AegisCyan else Color.Gray,
                                modifier = Modifier.size(72.dp)
                            )
                        }
                    }
                } else {
                    // Fingerprint sensor
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF070B14))
                            .border(
                                2.dp,
                                if (isVerified) AegisEmerald else AegisIndigo,
                                CircleShape
                            )
                            .clickable(enabled = !isVerifying && !isVerified) {
                                isVerifying = true
                            }
                            .testTag("fingerprint_sensor_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isVerified) Icons.Default.CheckCircle else Icons.Default.Fingerprint,
                            contentDescription = "Sensor",
                            tint = if (isVerified) AegisEmerald else if (isVerifying) AegisCyan else AegisIndigo,
                            modifier = Modifier.size(68.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = statusText,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = if (isVerified) AegisEmerald else MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = { isVerifying = true },
                    enabled = !isVerifying && !isVerified,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AegisCyan,
                        contentColor = Color(0xFF00363D)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("auth_scan_button")
                ) {
                    Text(if (isVerifying) "Verifying..." else "Authorize as $ownerName", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.LightGray),
                modifier = Modifier.testTag("auth_cancel_button")
            ) {
                Text("Cancel")
            }
        }
    )
}
