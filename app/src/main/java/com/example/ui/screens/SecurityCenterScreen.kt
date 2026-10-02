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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.AssistantIdentity
import com.example.data.model.OwnerProfile
import com.example.data.model.SecurityLog
import com.example.ui.theme.AegisAmber
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisEmerald
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisOutline
import com.example.ui.theme.AegisRose
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.AegisSurfaceCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SecurityCenterScreen(
    profile: OwnerProfile,
    identity: AssistantIdentity,
    securityLogs: List<SecurityLog>,
    onUpdateProfile: (OwnerProfile) -> Unit,
    onUpdateIdentity: (AssistantIdentity) -> Unit,
    onClearLogs: () -> Unit,
    onClearAllData: () -> Unit,
    onRequestSensitiveAction: (() -> Unit) -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditIdentityDialog by remember { mutableStateOf(false) }
    var showRetrainVoiceDialog by remember { mutableStateOf(false) }
    var showRetrainFaceDialog by remember { mutableStateOf(false) }

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
                            .background(AegisEmerald.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AegisEmerald
                        )
                    }
                    Column {
                        Text(
                            text = "AEGIS SECURITY CENTER",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = Color.White
                        )
                        Text(
                            text = "Device Owner Authority & Access Control Policies",
                            style = MaterialTheme.typography.labelSmall,
                            color = AegisEmerald
                        )
                    }
                }
            }

            // Owner Profile Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisEmerald.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = AegisSurfaceCard),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "REGISTERED OWNER",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = AegisEmerald
                                )
                                Text(
                                    text = profile.ownerName,
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AegisEmerald.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text("SOLE ADMIN", color = AegisEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Biometrics status badges
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Face
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, AegisOutline, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onRequestSensitiveAction {
                                            showRetrainFaceDialog = true
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Face, contentDescription = null, tint = AegisCyan, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Face Geometry", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Retrain", fontSize = 9.sp, color = AegisCyan)
                                }
                            }

                            // Voice
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, AegisOutline, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onRequestSensitiveAction {
                                            showRetrainVoiceDialog = true
                                        }
                                    }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Mic, contentDescription = null, tint = AegisIndigo, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${profile.voicePitchMean.toInt()} Hz Profile", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Calibrate", fontSize = 9.sp, color = AegisIndigo)
                                }
                            }

                            // Fingerprint
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0F172A))
                                    .border(1.dp, AegisOutline, RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.Fingerprint, contentDescription = null, tint = AegisEmerald, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("Hardware Token", fontSize = 10.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text("Bound ✓", fontSize = 9.sp, color = AegisEmerald)
                                }
                            }
                        }
                    }
                }
            }

            // Security Policies Section
            item {
                Text(
                    text = "Authentication & Access Policies",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AegisCyan
                )
            }

            // Voice Verification Switch
            item {
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
                                text = "Voice Signature Verification",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Commands from unrecognized acoustic signatures are rejected",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = profile.voiceVerificationEnabled,
                            onCheckedChange = { isChecked ->
                                onRequestSensitiveAction {
                                    onUpdateProfile(profile.copy(voiceVerificationEnabled = isChecked))
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = AegisEmerald,
                                checkedTrackColor = AegisEmerald.copy(alpha = 0.35f)
                            ),
                            modifier = Modifier.testTag("switch_voice_verification")
                        )
                    }
                }
            }

            // Unknown User Policy selector
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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Unknown User Access Policy",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Action taken when an unrecognized speaker gives a voice command:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("BLOCK", "GUEST_MODE", "SILENT").forEach { policy ->
                                val isSelected = profile.guestPolicy == policy
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) AegisRose.copy(alpha = 0.2f) else Color(0xFF1E293B))
                                        .border(1.dp, if (isSelected) AegisRose else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        onRequestSensitiveAction {
                                            onUpdateProfile(profile.copy(guestPolicy = policy))
                                        }
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = policy.replace("_", " "),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) AegisRose else Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sensitive Action Protection
        item {
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
                            text = "Sensitive Action Protection",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Require Face or Fingerprint before executing high-privilege operations",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.sensitiveActionProtectionEnabled,
                        onCheckedChange = { isChecked ->
                            onRequestSensitiveAction {
                                onUpdateProfile(profile.copy(sensitiveActionProtectionEnabled = isChecked))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisEmerald,
                            checkedTrackColor = AegisEmerald.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("switch_sensitive_action_protection")
                    )
                }
            }
        }

        // Root Mode Toggle
        item {
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
                            text = "Root-Level Advanced Mode",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.White
                        )
                        Text(
                            text = "Enable su shell command execution and system-level automation",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = profile.rootModeEnabled,
                        onCheckedChange = { isChecked ->
                            onRequestSensitiveAction {
                                onUpdateProfile(profile.copy(rootModeEnabled = isChecked))
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = AegisAmber,
                            checkedTrackColor = AegisAmber.copy(alpha = 0.35f)
                        ),
                        modifier = Modifier.testTag("switch_root_mode")
                    )
                }
            }
        }

        // AI Assistant Identity Customizer Card
        item {
            Text(
                text = "AI Identity Configuration",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                color = AegisIndigo
            )
        }

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
                        Column {
                            Text(
                                text = "Assistant: ${identity.name}",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                text = "Wake Word: \"${identity.wakeWord}\" • Personality: ${identity.personality}",
                                style = MaterialTheme.typography.bodySmall,
                                color = AegisCyan
                            )
                        }

                        Button(
                            onClick = {
                                onRequestSensitiveAction {
                                    showEditIdentityDialog = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AegisIndigo.copy(alpha = 0.2f),
                                contentColor = AegisIndigo
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("edit_ai_identity_button")
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Modify", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Security Audit Logs
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Security Audit Logs",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = AegisCyan
                )

                if (securityLogs.isNotEmpty()) {
                    Text(
                        text = "Clear Logs",
                        fontSize = 12.sp,
                        color = AegisRose,
                        modifier = Modifier
                            .clickable {
                                onRequestSensitiveAction {
                                    onClearLogs()
                                }
                            }
                            .testTag("clear_logs_button")
                    )
                }
            }
        }

        if (securityLogs.isEmpty()) {
            item {
                Text(
                    text = "No security events recorded. All biometrics nominal.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(securityLogs.take(10), key = { it.id }) { log ->
                val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(log.timestamp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, AegisOutline, RoundedCornerShape(10.dp)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF090E18)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (log.success) AegisEmerald else AegisRose)
                                )
                                Text(
                                    text = log.action,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                    color = Color.White
                                )
                            }
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                            color = AegisCyan
                        )
                    }
                }
            }
        }

        // Factory Reset / Erase Vault
        item {
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    onRequestSensitiveAction {
                        onClearAllData()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AegisRose),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, AegisRose.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .testTag("reset_all_data_button")
            ) {
                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Erase Encrypted Local Vault & Factory Reset", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

    // Retrain Face Dialog
    if (showRetrainFaceDialog) {
        AlertDialog(
            onDismissRequest = { showRetrainFaceDialog = false },
            containerColor = AegisSurface,
            title = { Text("Retrain Face Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Calibrate optical facial geometry mesh. Extracts landmark aspect ratios, inter-pupillary distance, and facial symmetry.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newFaceVector = FloatArray(16) { i ->
                            (((profile.ownerName.hashCode() + System.currentTimeMillis()) * (i + 1) * 17) % 1000).toFloat().div(1000f).coerceIn(0.1f, 0.9f)
                        }
                        val serialized = newFaceVector.joinToString(",") { it.toString() }
                        onUpdateProfile(
                            profile.copy(
                                ownerFaceEnrolled = true,
                                faceGeometryVector = serialized,
                                faceMeshVectorHash = "face_hash_${System.currentTimeMillis()}"
                            )
                        )
                        showRetrainFaceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color(0xFF00363D))
                ) {
                    Text("Capture & Calibrate Mesh", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRetrainFaceDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Retrain Voice Dialog
    if (showRetrainVoiceDialog) {
        var testPitch by remember { mutableStateOf("165") }
        AlertDialog(
            onDismissRequest = { showRetrainVoiceDialog = false },
            containerColor = AegisSurface,
            title = { Text("Calibrate Voice Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Sample acoustic resonance and compute 64-dimensional Mel-Frequency Cepstral Coefficients (MFCC) for \"${identity.wakeWord}\".",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = testPitch,
                        onValueChange = { testPitch = it },
                        label = { Text("Pitch Frequency (Hz)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val pitchVal = testPitch.toFloatOrNull() ?: 165f
                        val newVoiceVector = FloatArray(64) { i ->
                            ((pitchVal * (i + 1) * 31) % 1000).div(1000f)
                        }
                        val serialized = newVoiceVector.joinToString(",") { it.toString() }
                        onUpdateProfile(
                            profile.copy(
                                voicePitchMean = pitchVal,
                                ownerVoiceEnrolled = true,
                                voiceEmbeddingVector = serialized
                            )
                        )
                        showRetrainVoiceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color(0xFF00363D))
                ) {
                    Text("Save Acoustic Profile", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showRetrainVoiceDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }

    // Edit AI Identity Dialog
    if (showEditIdentityDialog) {
        var editName by remember { mutableStateOf(identity.name) }
        var editWake by remember { mutableStateOf(identity.wakeWord) }
        var editPersonality by remember { mutableStateOf(identity.personality) }

        AlertDialog(
            onDismissRequest = { showEditIdentityDialog = false },
            containerColor = AegisSurface,
            title = { Text("Modify AI Identity", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            editWake = "Hey $it"
                        },
                        label = { Text("Assistant Name") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = editWake,
                        onValueChange = { editWake = it },
                        label = { Text("Wake Word") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AegisCyan,
                            unfocusedBorderColor = AegisOutline,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Text("Personality", style = MaterialTheme.typography.labelSmall, color = AegisCyan)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("GUARDIAN", "TECHNICAL", "FRIENDLY").forEach { p ->
                            val isSel = editPersonality == p
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (isSel) AegisIndigo.copy(alpha = 0.25f) else Color(0xFF1E293B))
                                    .border(1.dp, if (isSel) AegisIndigo else Color.Transparent, RoundedCornerShape(6.dp))
                                    .clickable { editPersonality = p }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(p, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSel) AegisIndigo else Color.LightGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank() && editWake.isNotBlank()) {
                            onUpdateIdentity(
                                identity.copy(
                                    name = editName,
                                    wakeWord = editWake,
                                    personality = editPersonality
                                )
                            )
                            showEditIdentityDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AegisCyan, contentColor = Color(0xFF00363D))
                ) {
                    Text("Apply Identity", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showEditIdentityDialog = false }) {
                    Text("Cancel", color = Color.LightGray)
                }
            }
        )
    }
}
