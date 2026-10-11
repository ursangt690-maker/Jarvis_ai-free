package com.example.domain

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class JarvisVoiceState {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING
}

class VoiceAssistantEngine(
    private val context: Context,
    private val onTranscriptReady: (transcript: String, detectedLanguage: String) -> Unit,
    private val onStatusMessage: (String) -> Unit
) : TextToSpeech.OnInitListener {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsInitialized = AtomicBoolean(false)
    private var lastSpokenText: String = ""
    private var lastSpokenFinishedAt: Long = 0L

    private val _voiceState = MutableStateFlow(JarvisVoiceState.IDLE)
    val voiceState: StateFlow<JarvisVoiceState> = _voiceState.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    private val _partialTranscript = MutableStateFlow("")
    val partialTranscript: StateFlow<String> = _partialTranscript.asStateFlow()

    private var speechRate: Float = 1.0f
    private var speechPitch: Float = 0.95f
    private var speechVolume: Float = 1.0f
    private var preferredLanguage: String = "AUTO"
    private var continuousListeningEnabled: Boolean = false
    private var wakeWordEnabled: Boolean = true
    private var wakePhrase: String = "Hey JARVIS"

    init {
        mainHandler.post {
            runCatching {
                textToSpeech = TextToSpeech(context.applicationContext, this)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized.set(true)
            textToSpeech?.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _voiceState.value = JarvisVoiceState.SPEAKING
                    _rmsLevel.value = 0.65f
                }

                override fun onDone(utteranceId: String?) {
                    lastSpokenFinishedAt = System.currentTimeMillis()
                    _rmsLevel.value = 0f
                    _voiceState.value = JarvisVoiceState.IDLE
                    if (continuousListeningEnabled) {
                        // Brief delay so mic does not pick up room echo of TTS
                        mainHandler.postDelayed({
                            if (_voiceState.value == JarvisVoiceState.IDLE && continuousListeningEnabled) {
                                startListening(isContinuousAutoRestart = true)
                            }
                        }, 700L)
                    }
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _rmsLevel.value = 0f
                    _voiceState.value = JarvisVoiceState.IDLE
                }

                override fun onError(utteranceId: String?, errorCode: Int) {
                    _rmsLevel.value = 0f
                    _voiceState.value = JarvisVoiceState.IDLE
                }
            })
        }
    }

    fun updateConfiguration(
        rate: Float,
        pitch: Float,
        volume: Float,
        language: String,
        continuous: Boolean,
        wakeWord: Boolean,
        phrase: String
    ) {
        speechRate = rate.coerceIn(0.5f, 2.0f)
        speechPitch = pitch.coerceIn(0.5f, 1.8f)
        speechVolume = volume.coerceIn(0.1f, 1.0f)
        preferredLanguage = language
        continuousListeningEnabled = continuous
        wakeWordEnabled = wakeWord
        wakePhrase = phrase.ifBlank { "Hey JARVIS" }
    }

    fun setThinkingState() {
        stopListeningInternal()
        _voiceState.value = JarvisVoiceState.THINKING
        _rmsLevel.value = 0.35f
    }

    fun setIdleState() {
        _voiceState.value = JarvisVoiceState.IDLE
        _rmsLevel.value = 0f
    }

    /**
     * Immediately interrupts any ongoing TTS speech or listening session.
     */
    fun interruptAll() {
        mainHandler.post {
            runCatching { textToSpeech?.stop() }
            stopListeningInternal()
            _voiceState.value = JarvisVoiceState.IDLE
            _rmsLevel.value = 0f
            _partialTranscript.value = ""
        }
    }

    fun startListening(isContinuousAutoRestart: Boolean = false) {
        mainHandler.post {
            // Stop TTS immediately when user initiates listening (barge-in interruption)
            if (textToSpeech?.isSpeaking == true) {
                runCatching { textToSpeech?.stop() }
            }

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onStatusMessage("On-device SpeechRecognizer service is unavailable on this emulator/device. Use the HUD command input bar below.")
                _voiceState.value = JarvisVoiceState.IDLE
                return@post
            }

            stopListeningInternal()

            runCatching {
                val recognizer = SpeechRecognizer.createSpeechRecognizer(context.applicationContext)
                speechRecognizer = recognizer
                recognizer.setRecognitionListener(createRecognitionListener(isContinuousAutoRestart))

                val localeTag = when (preferredLanguage) {
                    "hi-IN" -> "hi-IN"
                    "ne-NP" -> "ne-NP"
                    "en-US" -> "en-US"
                    else -> Locale.getDefault().toLanguageTag()
                }

                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, localeTag)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                }

                _partialTranscript.value = ""
                _voiceState.value = JarvisVoiceState.LISTENING
                recognizer.startListening(intent)
            }.onFailure { err ->
                _voiceState.value = JarvisVoiceState.IDLE
                onStatusMessage("Microphone recognition error: ${err.localizedMessage ?: "Unavailable"}")
            }
        }
    }

    private fun stopListeningInternal() {
        runCatching {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    private fun createRecognitionListener(isContinuousAutoRestart: Boolean) = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _voiceState.value = JarvisVoiceState.LISTENING
        }

        override fun onBeginningOfSpeech() {
            _voiceState.value = JarvisVoiceState.LISTENING
        }

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0.05f, 1.0f)
            _rmsLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _rmsLevel.value = 0.1f
        }

        override fun onError(error: Int) {
            _rmsLevel.value = 0f
            _voiceState.value = JarvisVoiceState.IDLE
            val msg = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error."
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission required."
                SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network speech timeout."
                SpeechRecognizer.ERROR_NO_MATCH -> if (isContinuousAutoRestart) "" else "No distinct voice command recognized."
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> if (isContinuousAutoRestart) "" else "Listening timed out waiting for speech."
                else -> ""
            }
            if (msg.isNotBlank()) {
                onStatusMessage(msg)
            }
            if (continuousListeningEnabled && (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
                mainHandler.postDelayed({
                    if (_voiceState.value == JarvisVoiceState.IDLE && continuousListeningEnabled) {
                        startListening(isContinuousAutoRestart = true)
                    }
                }, 1000L)
            }
        }

        override fun onResults(results: Bundle?) {
            _rmsLevel.value = 0f
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val rawText = matches?.firstOrNull()?.trim() ?: ""
            _partialTranscript.value = rawText

            if (rawText.isBlank()) {
                _voiceState.value = JarvisVoiceState.IDLE
                return
            }

            // Anti-feedback loop protection: ignore if identical to what JARVIS just spoke within 2.5 seconds
            val now = System.currentTimeMillis()
            if (now - lastSpokenFinishedAt < 2500L &&
                lastSpokenText.isNotBlank() &&
                rawText.equals(lastSpokenText, ignoreCase = true)
            ) {
                _voiceState.value = JarvisVoiceState.IDLE
                return
            }

            // Wake word filtering when in continuous auto-restart mode
            var cleanedCommand = rawText
            if (isContinuousAutoRestart && wakeWordEnabled) {
                val lower = rawText.lowercase(Locale.ROOT)
                val wakeLower = wakePhrase.lowercase(Locale.ROOT)
                if (lower.contains("jarvis") || lower.contains(wakeLower)) {
                    cleanedCommand = rawText
                        .replace(Regex("(?i)hey\\s+jarvis[,:\\s]*"), "")
                        .replace(Regex("(?i)jarvis[,:\\s]*"), "")
                        .trim()
                        .ifBlank { "Status report" }
                } else {
                    // Continue waiting for wake word
                    _voiceState.value = JarvisVoiceState.IDLE
                    mainHandler.postDelayed({
                        if (_voiceState.value == JarvisVoiceState.IDLE && continuousListeningEnabled) {
                            startListening(isContinuousAutoRestart = true)
                        }
                    }, 600L)
                    return
                }
            }

            val detectedLang = detectLanguageFromScript(cleanedCommand, preferredLanguage)
            _voiceState.value = JarvisVoiceState.THINKING
            onTranscriptReady(cleanedCommand, detectedLang)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                ?.trim()
            if (!partial.isNullOrBlank()) {
                _partialTranscript.value = partial
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    /**
     * Speaks the response using Android TextToSpeech with English, Hindi, or Nepali locale support.
     * Ensures microphone listening is stopped first so JARVIS never hears its own voice.
     */
    fun speak(text: String, languageTag: String = "en-US") {
        val cleanText = text.trim()
        if (cleanText.isBlank()) {
            _voiceState.value = JarvisVoiceState.IDLE
            return
        }

        mainHandler.post {
            // Stop microphone listening before speaking to prevent feedback loops
            stopListeningInternal()
            lastSpokenText = cleanText

            val tts = textToSpeech
            if (tts == null || !isTtsInitialized.get()) {
                _voiceState.value = JarvisVoiceState.IDLE
                return@post
            }

            requestAudioFocus()

            val targetLocale = when {
                languageTag.startsWith("ne", ignoreCase = true) -> Locale.forLanguageTag("ne-NP")
                languageTag.startsWith("hi", ignoreCase = true) -> Locale.forLanguageTag("hi-IN")
                else -> Locale.US
            }

            val langResult = tts.setLanguage(targetLocale)
            if (langResult == TextToSpeech.LANG_MISSING_DATA || langResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts.language = Locale.US
            }

            tts.setSpeechRate(speechRate)
            tts.setPitch(speechPitch)

            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, speechVolume)
            }
            val utteranceId = "jarvis_${UUID.randomUUID()}"
            _voiceState.value = JarvisVoiceState.SPEAKING
            tts.speak(cleanText, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        }
    }

    private fun requestAudioFocus() {
        runCatching {
            val audioMgr = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANT)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    .build()
                audioMgr.requestAudioFocus(req)
            } else {
                @Suppress("DEPRECATION")
                audioMgr.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        }
    }

    fun shutdown() {
        mainHandler.post {
            stopListeningInternal()
            runCatching {
                textToSpeech?.stop()
                textToSpeech?.shutdown()
            }
        }
    }

    companion object {
        /**
         * Detects Nepali / Hindi (Devanagari script or common transliterated phrases) vs English.
         */
        fun detectLanguageFromScript(text: String, preferred: String): String {
            if (preferred != "AUTO" && preferred.isNotBlank()) return preferred
            val hasDevanagari = text.any { it in '\u0900'..'\u097F' }
            if (hasDevanagari) {
                val hindiDistinctMarkers = listOf("है", "हूँ", "हैं", "का", "की", "के", "कैसा", "कैसी", "क्या", "बताओ")
                val nepaliDistinctMarkers = listOf("छ", "छु", "छन्", "हो", "हुन्छ", "गर्नुहोस्", "तपाईं", "कस्तो", "नेपाली", "भन्नुहोस्", "आजको")
                val nepaliScore = nepaliDistinctMarkers.count { text.contains(it) }
                val hindiScore = hindiDistinctMarkers.count { text.contains(it) }
                return if (nepaliScore > hindiScore) "ne-NP" else "hi-IN"
            }
            val lower = text.lowercase(Locale.ROOT)
            return when {
                lower.contains("kasto") || lower.contains("nepali") || lower.contains("tapai") || lower.contains("garnuhos") -> "ne-NP"
                lower.contains("kya haal") || lower.contains("kaise ho") || lower.contains("batao") -> "hi-IN"
                else -> "en-US"
            }
        }
    }
}
