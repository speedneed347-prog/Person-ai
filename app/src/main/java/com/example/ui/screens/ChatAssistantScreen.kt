package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AssistantIdentity
import com.example.data.model.ChatMessage
import com.example.data.model.OwnerProfile
import com.example.nlp.LanguageEngine
import com.example.ui.components.AppLaunchCard
import com.example.ui.components.AssistantAvatarCore
import com.example.ui.components.BiometricShieldBadge
import com.example.ui.components.DeviceToggleCard
import com.example.ui.components.ReminderCard
import com.example.ui.components.RootTerminalCard
import com.example.ui.components.SecurityAlertCard
import com.example.ui.components.SoundWaveVisualizer
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisOutline
import com.example.ai.AccelerationDevice
import com.example.ai.ModelStatus
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceCard

@Composable
fun ChatAssistantScreen(
    identity: AssistantIdentity,
    profile: OwnerProfile,
    messages: List<ChatMessage>,
    isListening: Boolean,
    isSpeaking: Boolean,
    audioRmsDb: Float,
    isGuestSimulation: Boolean,
    onToggleGuestSimulation: (Boolean) -> Unit,
    onSendMessage: (String) -> Unit,
    onStartVoice: () -> Unit,
    onStopVoice: () -> Unit,
    onSpeakMessage: (String, String) -> Unit,
    onLaunchApp: (String, String) -> Unit,
    onToggleTorch: (Boolean) -> Unit,
    onToggleWifi: (Boolean) -> Unit,
    isTorchOn: Boolean,
    gemmaStatus: ModelStatus = ModelStatus.READY,
    gemmaAcceleration: AccelerationDevice = AccelerationDevice.NPU,
    gemmaMemoryUsageMb: Int = 680,
    onUnloadGemma: () -> Unit = {},
    onReadScreen: () -> Unit = {},
    onSummarizeMemory: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var textInput by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val suggestionChips = listOf(
        "Open WhatsApp",
        "Read Screen Content",
        "Summarize Habits & Memory",
        "হোয়াটসঅ্যাপ খোলো",
        "YouTube open karo",
        "ব্লুটুথ চালু করো",
        "Torch on",
        "Remind 6 PM to stretch",
        "Check security status"
    )

    Surface(
        modifier = modifier.fillMaxSize(),
        color = AegisBackground
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(AegisSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    AssistantAvatarCore(
                        avatarStyle = identity.avatarStyle,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        isBlocked = false,
                        size = 40.dp
                    )
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = identity.name,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            val lang = LanguageEngine.supportedLanguages.find { it.code == identity.primaryLanguage }
                            Text(
                                text = lang?.flag ?: "🌐",
                                fontSize = 14.sp
                            )
                        }
                        Text(
                            text = "Wake: \"${identity.wakeWord}\"",
                            style = MaterialTheme.typography.labelSmall,
                            color = AegisCyan
                        )
                    }
                }

                // Speaker Mode Switch (Owner vs Guest Simulation)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isGuestSimulation) AegisRose.copy(alpha = 0.2f) else AegisEmerald.copy(alpha = 0.2f))
                            .border(1.dp, if (isGuestSimulation) AegisRose else AegisEmerald, RoundedCornerShape(16.dp))
                            .clickable { onToggleGuestSimulation(!isGuestSimulation) }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .testTag("toggle_guest_speaker_button"),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isGuestSimulation) Icons.Default.PersonOff else Icons.Default.Person,
                                contentDescription = null,
                                tint = if (isGuestSimulation) AegisRose else AegisEmerald,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isGuestSimulation) "Guest Voice (Blocked)" else "Owner Voice",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isGuestSimulation) AegisRose else AegisEmerald
                            )
                        }
                    }
                }
            }

            // Status ribbon with Gemma Hardware Acceleration and RAM indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF070B14))
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BiometricShieldBadge(
                    isVoiceProtected = profile.voiceVerificationEnabled,
                    isOwnerOnly = profile.strictOwnerOnlyMode
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(AegisIndigo.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "Gemma 3 • ${gemmaAcceleration.name} (${if (gemmaMemoryUsageMb > 0) "${gemmaMemoryUsageMb}MB" else "Conserved"})",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = AegisCyan
                        )
                    }

                    if (gemmaMemoryUsageMb > 0) {
                        Text(
                            text = "Free RAM",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = AegisAmber,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF1E293B))
                                .clickable { onUnloadGemma() }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Message Stream
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp),
                contentPadding = PaddingValues(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 20.dp)
                                .border(1.dp, AegisOutline, RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                AssistantAvatarCore(
                                    avatarStyle = identity.avatarStyle,
                                    isListening = false,
                                    isSpeaking = false,
                                    isBlocked = false,
                                    size = 56.dp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Ready to assist, ${profile.ownerName}.",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Speak naturally or tap a quick action below. Voice verification is active for your acoustic profile (${profile.voicePitchMean.toInt()} Hz).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    val isUser = msg.sender == "USER"
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(bottom = 2.dp)
                        ) {
                            Text(
                                text = if (isUser) profile.ownerName else identity.name,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = if (isUser) AegisCyan else AegisIndigo
                            )
                            if (msg.isSecurityBlocked) {
                                Text(
                                    text = "• REJECTED",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AegisRose
                                )
                            } else if (isUser && msg.isVoiceVerified) {
                                Text(
                                    text = "• Voice Verified ✓",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = AegisEmerald
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .background(
                                    if (msg.isSecurityBlocked) Color(0xFF270F16)
                                    else if (isUser) AegisCyan.copy(alpha = 0.18f)
                                    else AegisSurfaceCard
                                )
                                .border(
                                    1.dp,
                                    if (msg.isSecurityBlocked) AegisRose.copy(alpha = 0.6f)
                                    else if (isUser) AegisCyan.copy(alpha = 0.4f)
                                    else AegisOutline,
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = if (isUser) 16.dp else 4.dp,
                                        bottomEnd = if (isUser) 4.dp else 16.dp
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Column {
                                Text(
                                    text = msg.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (msg.isSecurityBlocked) Color(0xFFFFD4D8) else Color.White
                                )

                                if (!isUser && !msg.isSecurityBlocked) {
                                    Row(
                                        modifier = Modifier.padding(top = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        IconButton(
                                            onClick = { onSpeakMessage(msg.text, msg.detectedLanguage) },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Speak",
                                                tint = AegisCyan,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Text(
                                            text = msg.detectedLanguage.uppercase(),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = AegisCyan
                                        )
                                    }
                                }
                            }
                        }

                        // Attached Action Cards
                        when (msg.actionCardType) {
                            "APP_LAUNCH" -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                AppLaunchCard(
                                    appName = msg.actionCardPayload ?: "App",
                                    summary = "Application launch executed locally",
                                    onLaunchAgain = { onLaunchApp(msg.actionCardPayload ?: "", msg.actionCardPayload ?: "") }
                                )
                            }
                            "DEVICE_TOGGLE" -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                DeviceToggleCard(
                                    title = "Device Flashlight",
                                    stateDescription = if (isTorchOn) "Hardware LED active" else "Hardware LED off",
                                    isChecked = isTorchOn,
                                    onToggle = onToggleTorch,
                                    iconType = "TORCH"
                                )
                            }
                            "SECURITY_ALERT" -> {
                                Spacer(modifier = Modifier.height(8.dp))
                                SecurityAlertCard(
                                    reason = "Unrecognized Voice Mismatch",
                                    details = "Voice profile did not match registered owner ${profile.ownerName}. Command ignored."
                                )
                            }
                        }
                    }
                }
            }

            // Quick suggestion chips
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                contentPadding = PaddingValues(horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(suggestionChips) { chip ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF131D31))
                            .border(1.dp, AegisCyan.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                            .clickable {
                                when (chip) {
                                    "Read Screen Content" -> onReadScreen()
                                    "Summarize Habits & Memory" -> onSummarizeMemory()
                                    else -> onSendMessage(chip)
                                }
                            }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                            .testTag("chip_${chip.take(8).replace(" ", "_")}")
                    ) {
                        Text(
                            text = chip,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                            color = AegisCyan
                        )
                    }
                }
            }

            // Voice Interaction / Text Input Dock
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AegisOutline, RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
                colors = CardDefaults.cardColors(containerColor = AegisSurface),
                shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    // Floating sound wave overlay when listening
                    AnimatedVisibility(
                        visible = isListening,
                        enter = fadeIn(),
                        exit = fadeOut()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(AegisCyan)
                                )
                                Text(
                                    text = "Listening for \"${identity.wakeWord}\"...",
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = AegisCyan
                                )
                            }
                            SoundWaveVisualizer(
                                isListening = true,
                                audioRmsDb = audioRmsDb,
                                barCount = 11
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = { Text("Ask ${identity.name} in any language...", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                            keyboardActions = KeyboardActions(
                                onSend = {
                                    if (textInput.isNotBlank()) {
                                        onSendMessage(textInput)
                                        textInput = ""
                                    }
                                }
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("chat_text_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AegisCyan,
                                unfocusedBorderColor = AegisOutline,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF0B101D),
                                unfocusedContainerColor = Color(0xFF0B101D)
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )

                        // Send Text button
                        if (textInput.isNotBlank()) {
                            IconButton(
                                onClick = {
                                    onSendMessage(textInput)
                                    textInput = ""
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(AegisCyan)
                                    .testTag("send_message_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Send,
                                    contentDescription = "Send",
                                    tint = Color(0xFF00363D)
                                )
                            }
                        } else {
                            // Mic Voice Trigger
                            IconButton(
                                onClick = {
                                    if (isListening) onStopVoice() else onStartVoice()
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) AegisRose else AegisCyan)
                                    .testTag("voice_mic_button")
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                                    contentDescription = "Voice Input",
                                    tint = if (isListening) Color.White else Color(0xFF00363D)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
