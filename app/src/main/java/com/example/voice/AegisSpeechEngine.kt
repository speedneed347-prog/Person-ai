package com.example.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class AegisSpeechEngine(private val context: Context) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady: Boolean = false

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _audioRmsDb = MutableStateFlow(0f)
    val audioRmsDb: StateFlow<Float> = _audioRmsDb.asStateFlow()

    private val _lastSpeechText = MutableStateFlow("")
    val lastSpeechText: StateFlow<String> = _lastSpeechText.asStateFlow()

    private var onSpeechResultCallback: ((String) -> Unit)? = null

    init {
        initializeTts()
    }

    private fun initializeTts() {
        try {
            textToSpeech = TextToSpeech(context) { status ->
                isTtsReady = (status == TextToSpeech.SUCCESS)
                if (isTtsReady) {
                    textToSpeech?.language = Locale.US
                }
            }
        } catch (_: Exception) {}
    }

    fun startListening(
        languageCode: String = "en-US",
        onResult: (String) -> Unit
    ) {
        onSpeechResultCallback = onResult

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            // Emulators or devices without Google Speech Services
            return
        }

        stopListening()

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _isListening.value = true
                    }

                    override fun onBeginningOfSpeech() {}

                    override fun onRmsChanged(rmsdB: Float) {
                        // Normalize RMS dB typically -2 to 10
                        _audioRmsDb.value = rmsdB.coerceIn(0f, 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        _isListening.value = false
                    }

                    override fun onError(error: Int) {
                        _isListening.value = false
                        _audioRmsDb.value = 0f
                    }

                    override fun onResults(results: Bundle?) {
                        _isListening.value = false
                        _audioRmsDb.value = 0f
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull() ?: ""
                        if (text.isNotEmpty()) {
                            _lastSpeechText.value = text
                            onSpeechResultCallback?.invoke(text)
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        matches?.firstOrNull()?.let {
                            _lastSpeechText.value = it
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            _isListening.value = false
        }
    }

    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) {}
        _isListening.value = false
        _audioRmsDb.value = 0f
    }

    fun speak(
        text: String,
        langCode: String = "en",
        pitch: Float = 1.0f,
        speed: Float = 1.0f
    ) {
        if (!isTtsReady || textToSpeech == null) return

        try {
            val locale = when (langCode.lowercase()) {
                "bn" -> Locale("bn", "IN")
                "hi" -> Locale("hi", "IN")
                "ur" -> Locale("ur", "PK")
                "es" -> Locale("es", "ES")
                "fr" -> Locale.FRENCH
                "de" -> Locale.GERMAN
                "ja" -> Locale.JAPANESE
                "ko" -> Locale.KOREAN
                "zh" -> Locale.CHINESE
                "ar" -> Locale("ar")
                "ru" -> Locale("ru", "RU")
                "it" -> Locale.ITALIAN
                else -> Locale.US
            }

            textToSpeech?.language = locale
            textToSpeech?.setPitch(pitch)
            textToSpeech?.setSpeechRate(speed)
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "aegis_tts_${System.currentTimeMillis()}")
        } catch (_: Exception) {}
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        stopListening()
        try {
            textToSpeech?.shutdown()
            textToSpeech = null
        } catch (_: Exception) {}
    }
}
