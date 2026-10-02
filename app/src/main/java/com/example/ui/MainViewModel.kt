package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.db.AegisRepository
import com.example.data.model.AssistantIdentity
import com.example.data.model.ChatMessage
import com.example.data.model.LocalMemory
import com.example.data.model.OwnerProfile
import com.example.data.model.Reminder
import com.example.data.model.SecurityLog
import com.example.device.DeviceAutomationManager
import com.example.nlp.AssistantIntent
import com.example.nlp.LanguageEngine
import com.example.security.BiometricVerificationManager
import com.example.voice.AegisSpeechEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AegisRepository
    private val speechEngine = AegisSpeechEngine(application)
    private val biometricManager = BiometricVerificationManager(application)
    private val deviceManager = DeviceAutomationManager(application)

    val ownerProfile: StateFlow<OwnerProfile?>
    val assistantIdentity: StateFlow<AssistantIdentity?>
    val chatMessages: StateFlow<List<ChatMessage>>
    val localMemories: StateFlow<List<LocalMemory>>
    val securityLogs: StateFlow<List<SecurityLog>>

    val isListening: StateFlow<Boolean> = speechEngine.isListening
    val audioRmsDb: StateFlow<Float> = speechEngine.audioRmsDb

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isTorchOn = MutableStateFlow(false)
    val isTorchOn: StateFlow<Boolean> = _isTorchOn.asStateFlow()

    private val _isGuestSimulation = MutableStateFlow(false)
    val isGuestSimulation: StateFlow<Boolean> = _isGuestSimulation.asStateFlow()

    // Sensitive action biometric verification
    private val _pendingSensitiveAction = MutableStateFlow<(() -> Unit)?>(null)
    val pendingSensitiveAction: StateFlow<(() -> Unit)?> = _pendingSensitiveAction.asStateFlow()

    // Last Root Command output
    private val _lastRootCommand = MutableStateFlow<String?>(null)
    val lastRootCommand: StateFlow<String?> = _lastRootCommand.asStateFlow()

    private val _lastRootOutput = MutableStateFlow<String?>(null)
    val lastRootOutput: StateFlow<String?> = _lastRootOutput.asStateFlow()

    private val _lastRootSuccess = MutableStateFlow(false)
    val lastRootSuccess: StateFlow<Boolean> = _lastRootSuccess.asStateFlow()

    init {
        val dao = AppDatabase.getDatabase(application).aegisDao()
        repository = AegisRepository(dao)

        ownerProfile = repository.ownerProfile.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

        assistantIdentity = repository.assistantIdentity.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            null
        )

        chatMessages = repository.chatMessages.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        localMemories = repository.localMemories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        securityLogs = repository.securityLogs.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

        viewModelScope.launch {
            repository.initializeDefaultsIfNeeded()
            seedInitialMemoriesIfNeeded()
        }
    }

    private suspend fun seedInitialMemoriesIfNeeded() {
        val currentProfile = repository.getOwnerProfileOnce()
        // If no memories exist, seed initial intelligent local memories
        val defaultProfile = currentProfile ?: OwnerProfile()
        repository.insertMemory(
            LocalMemory(
                category = "PREFERENCES",
                title = "Daily Focus Hours",
                detail = "Prefers muted notifications and deep focus between 9:00 AM and 1:00 PM.",
                confidence = 0.98f,
                isPinned = true
            )
        )
        repository.insertMemory(
            LocalMemory(
                category = "ROUTINES",
                title = "Morning Routine",
                detail = "Checks weather, reviews calendar reminders, and opens WhatsApp upon wakeup.",
                confidence = 0.95f
            )
        )
        repository.insertMemory(
            LocalMemory(
                category = "FREQUENT_APPS",
                title = "High Priority Apps",
                detail = "WhatsApp, Google Chrome, YouTube, Device Settings.",
                confidence = 0.99f
            )
        )
        repository.insertMemory(
            LocalMemory(
                category = "FAVORITE_CONTACT",
                title = "Primary Emergency Contact",
                detail = "Sarah (Mobile: +1-555-0199)",
                confidence = 0.97f
            )
        )
    }

    fun completeSetup(profile: OwnerProfile, identity: AssistantIdentity) {
        viewModelScope.launch {
            repository.setOwnerProfile(profile)
            repository.setAssistantIdentity(identity)
            repository.insertSecurityLog(
                SecurityLog(
                    action = "OWNER_SETUP_COMPLETED",
                    authMethod = "FACE_AND_VOICE",
                    success = true,
                    details = "Registered owner: ${profile.ownerName}, Assistant: ${identity.name} (\"${identity.wakeWord}\")"
                )
            )

            // Greet owner
            val welcomeText = "Security shields engaged. Hello ${profile.ownerName}, I am ${identity.name}. All systems are operating locally on your device."
            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = welcomeText,
                    detectedLanguage = identity.primaryLanguage
                )
            )
            speak(welcomeText, identity.primaryLanguage)
        }
    }

    fun toggleGuestSimulation(enabled: Boolean) {
        _isGuestSimulation.value = enabled
    }

    fun requestSensitiveAction(action: () -> Unit) {
        val currentProfile = ownerProfile.value
        if (currentProfile?.sensitiveActionProtectionEnabled == true) {
            _pendingSensitiveAction.value = action
        } else {
            action()
        }
    }

    fun onSensitiveActionAuthenticated() {
        val action = _pendingSensitiveAction.value
        _pendingSensitiveAction.value = null
        action?.invoke()
        viewModelScope.launch {
            repository.insertSecurityLog(
                SecurityLog(
                    action = "BIOMETRIC_AUTH_SUCCESS",
                    authMethod = "BIOMETRICS",
                    success = true,
                    details = "Owner identity verified for sensitive operation."
                )
            )
        }
    }

    fun dismissSensitiveAction() {
        _pendingSensitiveAction.value = null
    }

    fun startVoiceInput() {
        val lang = assistantIdentity.value?.primaryLanguage ?: "en"
        speechEngine.startListening(lang) { recognizedText ->
            processUserQuery(recognizedText, isVoice = true)
        }
    }

    fun stopVoiceInput() {
        speechEngine.stopListening()
    }

    fun sendTextMessage(query: String) {
        processUserQuery(query, isVoice = false)
    }

    private fun processUserQuery(rawQuery: String, isVoice: Boolean) {
        val profile = ownerProfile.value ?: OwnerProfile()
        val identity = assistantIdentity.value ?: AssistantIdentity()

        viewModelScope.launch {
            // Check Voice Verification if from voice input
            if (isVoice && profile.voiceVerificationEnabled) {
                val voiceCheck = biometricManager.verifyVoiceSignature(
                    profile = profile,
                    isGuestSimulation = _isGuestSimulation.value
                )

                if (!voiceCheck.isOwnerVerified) {
                    // Unauthorized Voice!
                    repository.insertChatMessage(
                        ChatMessage(
                            sender = "USER",
                            text = rawQuery,
                            isVoiceVerified = false,
                            isSecurityBlocked = true
                        )
                    )

                    repository.insertSecurityLog(
                        SecurityLog(
                            action = "VOICE_SIGNATURE_MISMATCH",
                            authMethod = "VOICE",
                            success = false,
                            details = "Unrecognized speaker pitch (${voiceCheck.detectedPitchHz.toInt()} Hz vs owner ${profile.voicePitchMean.toInt()} Hz)"
                        )
                    )

                    if (profile.guestPolicy != "SILENT") {
                        val rejectionMsg = if (profile.guestPolicy == "GUEST_MODE") {
                            "Unrecognized voice detected. Guest mode limited: I cannot execute device actions for unauthorized speakers."
                        } else {
                            "Access Denied: Voice signature does not match registered device owner ${profile.ownerName}."
                        }

                        repository.insertChatMessage(
                            ChatMessage(
                                sender = "ASSISTANT",
                                text = rejectionMsg,
                                isSecurityBlocked = true,
                                actionCardType = "SECURITY_ALERT"
                            )
                        )
                        speak(rejectionMsg, identity.primaryLanguage)
                    }
                    return@launch
                }
            }

            // Voice is verified or text input
            repository.insertChatMessage(
                ChatMessage(
                    sender = "USER",
                    text = rawQuery,
                    isVoiceVerified = true
                )
            )

            // NLP Parsing
            val parseResult = LanguageEngine.parseInput(
                rawText = rawQuery,
                wakeWord = identity.wakeWord,
                assistantName = identity.name,
                configuredPrimaryLang = identity.primaryLanguage
            )

            val lang = if (identity.autoLanguageDetection) parseResult.detectedLanguageCode else identity.primaryLanguage

            // Execute Intent
            var actionCardType: String? = null
            var actionPayload: String? = null

            when (val intent = parseResult.intent) {
                is AssistantIntent.OpenApp -> {
                    deviceManager.launchApp(intent.appQuery, intent.appName)
                    actionCardType = "APP_LAUNCH"
                    actionPayload = intent.appName
                }

                is AssistantIntent.ToggleTorch -> {
                    deviceManager.toggleTorch(intent.enable)
                    _isTorchOn.value = intent.enable
                    actionCardType = "DEVICE_TOGGLE"
                    actionPayload = if (intent.enable) "TORCH_ON" else "TORCH_OFF"
                }

                is AssistantIntent.ToggleWifi -> {
                    deviceManager.openWifiSettings()
                }

                is AssistantIntent.ToggleBluetooth -> {
                    deviceManager.openBluetoothSettings()
                }

                is AssistantIntent.SetVolume -> {
                    deviceManager.setVolume(intent.percentage)
                }

                is AssistantIntent.CreateReminder -> {
                    repository.insertReminder(
                        Reminder(
                            title = intent.title,
                            timeLabel = intent.timeHint
                        )
                    )
                    actionCardType = "REMINDER"
                    actionPayload = intent.title
                }

                is AssistantIntent.CreateNote -> {
                    repository.insertMemory(
                        LocalMemory(
                            category = "NOTE",
                            title = "Voice Note",
                            detail = intent.content
                        )
                    )
                }

                is AssistantIntent.RootShellCommand -> {
                    executeRootCommand(intent.command)
                }

                else -> {}
            }

            // Generate localized conversational response
            val responseText = LanguageEngine.generateResponse(
                intent = parseResult.intent,
                language = lang,
                assistantName = identity.name,
                personality = identity.personality,
                ownerName = profile.ownerName
            )

            repository.insertChatMessage(
                ChatMessage(
                    sender = "ASSISTANT",
                    text = responseText,
                    detectedLanguage = lang,
                    actionCardType = actionCardType,
                    actionCardPayload = actionPayload
                )
            )

            speak(responseText, lang)
        }
    }

    fun speak(text: String, lang: String = "en") {
        val identity = assistantIdentity.value
        _isSpeaking.value = true
        speechEngine.speak(
            text = text,
            langCode = lang,
            pitch = identity?.speechPitch ?: 1.0f,
            speed = identity?.speechSpeed ?: 1.0f
        )
        viewModelScope.launch {
            kotlinx.coroutines.delay(2000)
            _isSpeaking.value = false
        }
    }

    fun launchApp(appQuery: String, appName: String) {
        deviceManager.launchApp(appQuery, appName)
    }

    fun toggleTorch(enable: Boolean) {
        deviceManager.toggleTorch(enable)
        _isTorchOn.value = enable
    }

    fun openWifi() {
        deviceManager.openWifiSettings()
    }

    fun openBluetooth() {
        deviceManager.openBluetoothSettings()
    }

    fun setVolume(percentage: Int) {
        deviceManager.setVolume(percentage)
    }

    fun executeRootCommand(command: String) {
        viewModelScope.launch {
            _lastRootCommand.value = command
            val result = deviceManager.executeRootCommand(command)
            _lastRootOutput.value = result.details ?: result.summary
            _lastRootSuccess.value = result.success

            repository.insertSecurityLog(
                SecurityLog(
                    action = "ROOT_COMMAND_RUN",
                    authMethod = "BIOMETRICS",
                    success = result.success,
                    details = "Command: $command (${result.summary})"
                )
            )
        }
    }

    fun addMemory(category: String, title: String, detail: String) {
        viewModelScope.launch {
            repository.insertMemory(
                LocalMemory(
                    category = category,
                    title = title,
                    detail = detail
                )
            )
        }
    }

    fun deleteMemory(id: Long) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun updateProfile(profile: OwnerProfile) {
        viewModelScope.launch {
            repository.updateOwnerProfile(profile)
        }
    }

    fun updateIdentity(identity: AssistantIdentity) {
        viewModelScope.launch {
            repository.setAssistantIdentity(identity)
        }
    }

    fun clearSecurityLogs() {
        viewModelScope.launch {
            repository.clearSecurityLogs()
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearChatMessages()
            repository.clearAllMemories()
            repository.clearSecurityLogs()
            repository.setOwnerProfile(
                OwnerProfile(
                    id = 1,
                    setupCompleted = false
                )
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechEngine.release()
    }
}
