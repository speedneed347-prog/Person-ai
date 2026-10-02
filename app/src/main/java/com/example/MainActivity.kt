package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.AssistantIdentity
import com.example.data.model.OwnerProfile
import com.example.ui.MainViewModel
import com.example.ui.components.BiometricVerificationDialog
import com.example.ui.screens.AutomationScreen
import com.example.ui.screens.ChatAssistantScreen
import com.example.ui.screens.MemoryVaultScreen
import com.example.ui.screens.SecurityCenterScreen
import com.example.ui.screens.SetupWizardScreen
import com.example.ui.theme.AegisBackground
import com.example.ui.theme.AegisCyan
import com.example.ui.theme.AegisIndigo
import com.example.ui.theme.AegisSurface
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                AegisAppRoot()
            }
        }
    }
}

@Composable
fun AegisAppRoot(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val ownerProfile by viewModel.ownerProfile.collectAsStateWithLifecycle()
    val assistantIdentity by viewModel.assistantIdentity.collectAsStateWithLifecycle()
    val chatMessages by viewModel.chatMessages.collectAsStateWithLifecycle()
    val localMemories by viewModel.localMemories.collectAsStateWithLifecycle()
    val securityLogs by viewModel.securityLogs.collectAsStateWithLifecycle()
    val isListening by viewModel.isListening.collectAsStateWithLifecycle()
    val isSpeaking by viewModel.isSpeaking.collectAsStateWithLifecycle()
    val audioRmsDb by viewModel.audioRmsDb.collectAsStateWithLifecycle()
    val isTorchOn by viewModel.isTorchOn.collectAsStateWithLifecycle()
    val isGuestSimulation by viewModel.isGuestSimulation.collectAsStateWithLifecycle()
    val pendingSensitiveAction by viewModel.pendingSensitiveAction.collectAsStateWithLifecycle()
    val lastRootCommand by viewModel.lastRootCommand.collectAsStateWithLifecycle()
    val lastRootOutput by viewModel.lastRootOutput.collectAsStateWithLifecycle()
    val lastRootSuccess by viewModel.lastRootSuccess.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Chat, 1: Memory, 2: Automations, 3: Security

    // Request Audio & Camera permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { _ -> }

    LaunchedEffect(Unit) {
        val permissions = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
        )
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            permissionLauncher.launch(needed.toTypedArray())
        }
    }

    // Biometric Modal for sensitive actions
    if (pendingSensitiveAction != null) {
        BiometricVerificationDialog(
            title = "Biometric Verification Required",
            subtitle = "Sensitive action requested. Verify identity to authorize.",
            ownerName = ownerProfile?.ownerName ?: "Owner",
            onSuccess = {
                viewModel.onSensitiveActionAuthenticated()
            },
            onDismiss = {
                viewModel.dismissSensitiveAction()
            }
        )
    }

    if (ownerProfile == null || assistantIdentity == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AegisBackground),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = AegisCyan)
        }
        return
    }

    val currentProfile = ownerProfile!!
    val currentIdentity = assistantIdentity!!

    if (!currentProfile.setupCompleted) {
        // Show first launch setup wizard
        SetupWizardScreen(
            onCompleteSetup = { prof, id ->
                viewModel.completeSetup(prof, id)
            }
        )
    } else {
        // Handle Back button to return to chat
        if (selectedTab != 0) {
            BackHandler {
                selectedTab = 0
            }
        }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = AegisBackground,
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                    containerColor = AegisSurface,
                    contentColor = AegisCyan
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.ChatBubble,
                                contentDescription = "Assistant",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text(currentIdentity.name, fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AegisCyan,
                            selectedTextColor = AegisCyan,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = AegisCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_assistant")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Memory,
                                contentDescription = "Memory",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Memory", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AegisIndigo,
                            selectedTextColor = AegisIndigo,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = AegisIndigo.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_memory")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.AutoMode,
                                contentDescription = "Automations",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Automations", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AegisCyan,
                            selectedTextColor = AegisCyan,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = AegisCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_automations")
                    )

                    NavigationBarItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Security",
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        label = { Text("Security", fontSize = 11.sp) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AegisCyan,
                            selectedTextColor = AegisCyan,
                            unselectedIconColor = Color.LightGray,
                            unselectedTextColor = Color.LightGray,
                            indicatorColor = AegisCyan.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier.testTag("nav_security")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (selectedTab) {
                    0 -> ChatAssistantScreen(
                        identity = currentIdentity,
                        profile = currentProfile,
                        messages = chatMessages,
                        isListening = isListening,
                        isSpeaking = isSpeaking,
                        audioRmsDb = audioRmsDb,
                        isGuestSimulation = isGuestSimulation,
                        onToggleGuestSimulation = { viewModel.toggleGuestSimulation(it) },
                        onSendMessage = { viewModel.sendTextMessage(it) },
                        onStartVoice = { viewModel.startVoiceInput() },
                        onStopVoice = { viewModel.stopVoiceInput() },
                        onSpeakMessage = { text, lang -> viewModel.speak(text, lang) },
                        onLaunchApp = { q, name -> viewModel.launchApp(q, name) },
                        onToggleTorch = { viewModel.toggleTorch(it) },
                        onToggleWifi = { viewModel.openWifi() },
                        isTorchOn = isTorchOn
                    )

                    1 -> MemoryVaultScreen(
                        memories = localMemories,
                        onAddMemory = { cat, title, detail ->
                            viewModel.addMemory(cat, title, detail)
                        },
                        onDeleteMemory = { id ->
                            viewModel.deleteMemory(id)
                        },
                        onRequestSensitiveAction = { action ->
                            viewModel.requestSensitiveAction(action)
                        }
                    )

                    2 -> AutomationScreen(
                        isTorchOn = isTorchOn,
                        onToggleTorch = { viewModel.toggleTorch(it) },
                        onOpenWifi = { viewModel.openWifi() },
                        onOpenBluetooth = { viewModel.openBluetooth() },
                        onSetVolume = { viewModel.setVolume(it) },
                        isRootModeEnabled = currentProfile.rootModeEnabled,
                        onExecuteRootCommand = { cmd -> viewModel.executeRootCommand(cmd) },
                        lastRootCommand = lastRootCommand,
                        lastRootOutput = lastRootOutput,
                        lastRootSuccess = lastRootSuccess,
                        onRequestSensitiveAction = { action ->
                            viewModel.requestSensitiveAction(action)
                        }
                    )

                    3 -> SecurityCenterScreen(
                        profile = currentProfile,
                        identity = currentIdentity,
                        securityLogs = securityLogs,
                        onUpdateProfile = { viewModel.updateProfile(it) },
                        onUpdateIdentity = { viewModel.updateIdentity(it) },
                        onClearLogs = { viewModel.clearSecurityLogs() },
                        onClearAllData = { viewModel.clearAllData() },
                        onRequestSensitiveAction = { action ->
                            viewModel.requestSensitiveAction(action)
                        }
                    )
                }
            }
        }
    }
}
