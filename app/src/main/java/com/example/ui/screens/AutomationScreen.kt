package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SettingsRemote
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AutomationRoutine
import com.example.ui.components.DeviceToggleCard
import com.example.ui.components.RootTerminalCard
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisOutline
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceCard

@Composable
fun AutomationScreen(
    isTorchOn: Boolean,
    onToggleTorch: (Boolean) -> Unit,
    onOpenWifi: () -> Unit,
    onOpenBluetooth: () -> Unit,
    onSetVolume: (Int) -> Unit,
    isRootModeEnabled: Boolean,
    onExecuteRootCommand: (String) -> Unit,
    lastRootCommand: String?,
    lastRootOutput: String?,
    lastRootSuccess: Boolean,
    onRequestSensitiveAction: (() -> Unit) -> Unit,
    isBackgroundServiceEnabled: Boolean = true,
    onToggleBackgroundService: (Boolean) -> Unit = {},
    onExecuteAccessibilityAction: (actionType: String, target: String, payload: String?) -> Unit = { _, _, _ -> },
    onReadScreenContent: () -> Unit = {},
    screenContent: List<String> = emptyList(),
    modifier: Modifier = Modifier
) {
    var volumeSlider by remember { mutableFloatStateOf(70f) }
    var shellInput by remember { mutableStateOf("uname -a") }
    var accessibilityTarget by remember { mutableStateOf("Settings") }

    val presetRoutines = listOf(
        AutomationRoutine(
            id = 1,
            name = "Morning Awakening",
            triggerPhrase = "Good morning Aegis",
            description = "Reads weather, sets volume to 60%, summaries unread tasks",
            iconName = "sun"
        ),
        AutomationRoutine(
            id = 2,
            name = "Night Security Lockdown",
            triggerPhrase = "Lock down device",
            description = "Turns off flashlight, lowers media volume, arms biometric shields",
            iconName = "moon"
        ),
        AutomationRoutine(
            id = 3,
            name = "Work Focus Routine",
            triggerPhrase = "Start focus session",
            description = "Mutes volume, turns on Wi-Fi, launches preferred productivity app",
            iconName = "work"
        )
    )

    val quickRootSnippets = listOf("id", "uname -a", "df -h", "uptime", "pm list packages -3")

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AegisBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            contentPadding = PaddingValues(vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(AegisCyan.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoMode,
                            contentDescription = null,
                            tint = AegisCyan
                        )
                    }
                    Column {
                        Text(
                            text = "DEVICE AUTOMATIONS & CONTROLS",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Direct hardware toggles & local system orchestration",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Quick Device Toggles
            item {
                Text(
                    text = "Hardware Controls",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AegisCyan
                )
            }

            item {
                DeviceToggleCard(
                    title = "Flashlight / Torch",
                    stateDescription = if (isTorchOn) "Active (Hardware LED ON)" else "Inactive",
                    isChecked = isTorchOn,
                    onToggle = onToggleTorch,
                    iconType = "TORCH"
                )
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisOutline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(Icons.Default.VolumeUp, contentDescription = null, tint = AegisCyan)
                                Text(
                                    text = "Media Volume (${volumeSlider.toInt()}%)",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Button(
                                onClick = { onSetVolume(volumeSlider.toInt()) },
                                colors = ButtonDefaults.buttonColors(containerColor = AegisCyan.copy(alpha = 0.2f), contentColor = AegisCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("apply_volume_button")
                            ) {
                                Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Slider(
                            value = volumeSlider,
                            onValueChange = { volumeSlider = it },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(
                                thumbColor = AegisCyan,
                                activeTrackColor = AegisCyan,
                                inactiveTrackColor = Color(0xFF1E293B)
                            )
                        )
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onOpenWifi,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131D31), contentColor = AegisCyan),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, AegisCyan.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("open_wifi_panel_button")
                    ) {
                        Icon(Icons.Default.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Wi-Fi Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onOpenBluetooth,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF131D31), contentColor = AegisIndigo),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, AegisIndigo.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .testTag("open_bluetooth_panel_button")
                    ) {
                        Icon(Icons.Default.SettingsRemote, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Bluetooth Panel", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Background Service & Auto-Start
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisCyan.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Persistent Background Guard",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Keeps wake word detector active when app is closed • Auto-starts on device reboot",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Switch(
                            checked = isBackgroundServiceEnabled,
                            onCheckedChange = onToggleBackgroundService,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisCyan,
                                checkedTrackColor = AegisCyan.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.testTag("switch_bg_service")
                        )
                    }
                }
            }

            // Accessibility Automation Engine Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisIndigo.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Accessibility Automation Engine",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AegisIndigo.copy(alpha = 0.2f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text("SYSTEM INTEGRATION", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AegisIndigo)
                            }
                        }

                        Text(
                            text = "Automates clicking buttons, filling text fields, and reading on-screen elements.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = onReadScreenContent,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = AegisCyan),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("read_screen_button")
                            ) {
                                Text("Read Screen", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    onRequestSensitiveAction {
                                        onExecuteAccessibilityAction("CLICK_TEXT", accessibilityTarget, null)
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AegisIndigo.copy(alpha = 0.25f), contentColor = AegisIndigo),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.weight(1f).testTag("click_text_button")
                            ) {
                                Text("Simulate Click", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (screenContent.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF070B14))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "Screen Elements: " + screenContent.take(5).joinToString(", "),
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }

            // Smart Automation Routines
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Smart Automation Workflows",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AegisIndigo
                )
            }

            items(presetRoutines, key = { it.id }) { routine ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisOutline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = routine.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Trigger: \"${routine.triggerPhrase}\"",
                                style = MaterialTheme.typography.labelSmall,
                                color = AegisCyan
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = routine.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Button(
                            onClick = {
                                // Execute routine
                                onSetVolume(50)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisIndigo.copy(alpha = 0.25f),
                                contentColor = AegisIndigo
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("run_routine_${routine.id}")
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = "Run", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Root Mode Advanced Section
            item {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Terminal,
                            contentDescription = null,
                            tint = if (isRootModeEnabled) AegisEmerald else AegisAmber
                        )
                        Text(
                            text = "Root Terminal Executor",
                            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                            color = if (isRootModeEnabled) AegisEmerald else AegisAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isRootModeEnabled) AegisEmerald.copy(alpha = 0.2f) else AegisAmber.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = if (isRootModeEnabled) "ROOT UNLOCKED" else "ROOT DISABLED",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isRootModeEnabled) AegisEmerald else AegisAmber
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisOutline, RoundedCornerShape(14.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Execute low-level shell commands via su (requires biometric approval).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        // Quick snippet pills
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(quickRootSnippets) { snippet ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0F172A))
                                        .border(1.dp, AegisCyan.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                        .clickable { shellInput = snippet }
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(snippet, fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = AegisCyan)
                                }
                            }
                        }

                        OutlinedTextField(
                            value = shellInput,
                            onValueChange = { shellInput = it },
                            label = { Text("Shell Command") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().testTag("root_command_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisOutline,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White
                            )
                        )

                        Button(
                            onClick = {
                                onRequestSensitiveAction {
                                    onExecuteRootCommand(shellInput)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color(0xFF00363D)),
                            modifier = Modifier.fillMaxWidth().testTag("run_root_button")
                        ) {
                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Verify Biometrics & Execute Root Command", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        if (!lastRootCommand.isNullOrEmpty() && lastRootOutput != null) {
                            RootTerminalCard(
                                command = lastRootCommand,
                                output = lastRootOutput,
                                exitCodeZero = lastRootSuccess
                            )
                        }
                    }
                }
            }
        }
    }
}
