package com.bookverse.contacts.utils

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.bookverse.contacts.feature.tools.R
import com.bookverse.contacts.settings.LocaleController
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.util.Locale
import kotlin.coroutines.resume
import com.bookverse.contacts.core.ui.R as CoreUiR

class SpeechUtils(private val context: Context) {
    private val localizedContext = LocaleController.localizedContext(context)
    private var textToSpeech: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isInitialized = false
    private var shouldContinue = false

    suspend fun initializeTextToSpeech(): Boolean = suspendCancellableCoroutine { cont ->
        try {
            textToSpeech = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val result = textToSpeech?.setLanguage(LocaleController.effectiveLocale(localizedContext))
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        textToSpeech?.language = Locale.US
                    }
                    isInitialized = true
                    cont.resume(true)
                } else {
                    cont.resume(false)
                }
            }
        } catch (_: Exception) {
            cont.resume(false)
        }
    }

    fun getAvailableVoices(): List<Voice> {
        return try {
            textToSpeech?.voices?.filter { voice ->
                val lang = voice.locale.language
                lang == "es" || lang == "en" || lang == LocaleController.effectiveLocale(localizedContext).language
            }?.sortedWith(compareBy<Voice> { voice ->
                when (voice.locale.language) {
                    "es" -> 0
                    LocaleController.effectiveLocale(localizedContext).language -> 1
                    "en" -> 2
                    else -> 3
                }
            }.thenBy { it.name }) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    suspend fun speakText(
        text: String,
        voice: Voice? = null,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            textToSpeech?.let { tts ->
                if (!isInitialized) {
                    onError(localizedContext.getString(R.string.tts_error_message, "TTS not initialized"))
                    cont.resume(false)
                    return@suspendCancellableCoroutine
                }
                voice?.let {
                    try { tts.voice = it } catch (_: Exception) { }
                }
                val utteranceId = "tts_${System.currentTimeMillis()}"
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onStart() }
                    override fun onDone(utteranceId: String?) {
                        onDone(); if (cont.isActive) cont.resume(true)
                    }
                    @Deprecated("Deprecated in API level 21")
                    override fun onError(utteranceId: String?) {
                        onError(localizedContext.getString(R.string.tts_error_message, localizedContext.getString(CoreUiR.string.unknown)))
                        if (cont.isActive) cont.resume(false)
                    }
                    @Deprecated("Deprecated in API level 21")
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onError(localizedContext.getString(R.string.tts_error_message, errorCode.toString()))
                        if (cont.isActive) cont.resume(false)
                    }
                })
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    onError(localizedContext.getString(R.string.tts_error_message, "Start error"))
                    cont.resume(false)
                }
            } ?: run {
                onError(localizedContext.getString(R.string.tts_error_message, "TTS null"))
                cont.resume(false)
            }
        } catch (e: Exception) {
            onError(localizedContext.getString(R.string.tts_error_message, e.message ?: "Exception"))
            cont.resume(false)
        }
    }

    suspend fun synthesizeToFile(
        text: String,
        voice: Voice? = null,
        outputFile: File,
        onStart: () -> Unit = {},
        onDone: () -> Unit = {},
        onError: (String) -> Unit = {}
    ): Boolean = suspendCancellableCoroutine { cont ->
        try {
            textToSpeech?.let { tts ->
                if (!isInitialized) {
                    onError(localizedContext.getString(R.string.tts_error_message, "TTS not initialized"))
                    cont.resume(false)
                    return@suspendCancellableCoroutine
                }
                voice?.let { try { tts.voice = it } catch (_: Exception) { } }
                val utteranceId = "tts_file_${System.currentTimeMillis()}"
                tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { onStart() }
                    override fun onDone(utteranceId: String?) { onDone(); if (cont.isActive) cont.resume(true) }
                    @Deprecated("Deprecated in API level 21")
                    override fun onError(utteranceId: String?) {
                        onError(localizedContext.getString(R.string.tts_error_message, localizedContext.getString(CoreUiR.string.unknown)))
                        if (cont.isActive) cont.resume(false)
                    }
                    @Deprecated("Deprecated in API level 21")
                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onError(localizedContext.getString(R.string.tts_error_message, errorCode.toString()))
                        if (cont.isActive) cont.resume(false)
                    }
                })
                val result = tts.synthesizeToFile(text, null, outputFile, utteranceId)
                if (result == TextToSpeech.ERROR) {
                    onError(localizedContext.getString(R.string.tts_error_message, "File error"))
                    cont.resume(false)
                }
            } ?: run {
                onError(localizedContext.getString(R.string.tts_error_message, "TTS null"))
                cont.resume(false)
            }
        } catch (e: Exception) {
            onError(localizedContext.getString(R.string.tts_error_message, e.message ?: "Exception"))
            cont.resume(false)
        }
    }

    fun isSpeechRecognitionAvailable(): Boolean {
        return try { SpeechRecognizer.isRecognitionAvailable(context) } catch (_: Exception) { false }
    }

    fun startListening(
        onResult: (String) -> Unit,
        onError: (String) -> Unit,
        onStart: () -> Unit = {},
        onEnd: () -> Unit = {},
        onPartial: (String) -> Unit = {},
        retryCount: Int = 0,
        continuous: Boolean = true,
        languageCode: String? = null
    ) {
        if (isListening) {
            onError(localizedContext.getString(R.string.speech_recognition_unavailable))
            return
        }
        try {
            stopListening()
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onError(localizedContext.getString(R.string.speech_recognition_unavailable))
                return
            }
            shouldContinue = continuous
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            if (speechRecognizer == null) {
                onError(localizedContext.getString(R.string.speech_recognition_unavailable))
                return
            }
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    isListening = true
                    onStart()
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() { isListening = false }
                override fun onError(error: Int) {
                    isListening = false
                    val errorMessage = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> if (retryCount < 2) null else localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> if (retryCount < 1) null else localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_CLIENT -> localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> localizedContext.getString(CoreUiR.string.microphone_permission_message)
                        SpeechRecognizer.ERROR_NETWORK -> localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_NO_MATCH -> null
                        SpeechRecognizer.ERROR_SERVER -> localizedContext.getString(R.string.speech_recognition_unavailable)
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> null
                        else -> localizedContext.getString(R.string.speech_recognition_unavailable)
                    }
                    if (errorMessage == null) {
                        if (retryCount < 3 && shouldContinue) {
                            Handler(Looper.getMainLooper()).postDelayed({
                                startListening(onResult, onError, onStart, onEnd, onPartial, retryCount + 1, continuous, languageCode)
                            }, 400)
                            return
                        } else {
                            onEnd()
                            return
                        }
                    } else {
                        onError(errorMessage)
                        onEnd()
                    }
                }
                override fun onResults(results: android.os.Bundle?) {
                    isListening = false
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!matches.isNullOrEmpty()) {
                        onResult(matches.first())
                    }
                    onEnd()
                    if (shouldContinue) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            if (shouldContinue) startListening(onResult, onError, onStart, onEnd, onPartial, 0, continuous, languageCode)
                        }, 250)
                    }
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {
                    val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (!partial.isNullOrEmpty()) onPartial(partial.first())
                }
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            val intent = createSpeechIntent(languageCode)
            speechRecognizer?.startListening(intent)
        } catch (_: Exception) {
            isListening = false
            onError(localizedContext.getString(R.string.speech_recognition_unavailable))
            onEnd()
        }
    }

    private fun createSpeechIntent(languageCode: String? = null): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageCode ?: getCurrentLanguage())
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300L)
            putExtra(RecognizerIntent.EXTRA_PROMPT, localizedContext.getString(R.string.speech_prompt))
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        }
    }

    fun stopListening() {
        try {
            shouldContinue = false
            isListening = false
            speechRecognizer?.stopListening()
            speechRecognizer?.destroy()
            speechRecognizer = null
        } catch (_: Exception) { }
    }

    fun getCurrentLanguage(): String {
        return try {
            val currentLocale = LocaleController.effectiveLocale(context)
            when (currentLocale.language) {
                "es" -> when (currentLocale.country.uppercase()) {
                    "ES" -> "es-ES"
                    "MX" -> "es-MX"
                    "AR" -> "es-AR"
                    "CO" -> "es-CO"
                    "CL" -> "es-CL"
                    "PE" -> "es-PE"
                    "VE" -> "es-VE"
                    else -> "es-ES"
                }
                "en" -> when (currentLocale.country.uppercase()) {
                    "US" -> "en-US"
                    "GB" -> "en-GB"
                    "AU" -> "en-AU"
                    "CA" -> "en-CA"
                    else -> "en-US"
                }
                "pt" -> when (currentLocale.country.uppercase()) {
                    "BR" -> "pt-BR"
                    "PT" -> "pt-PT"
                    else -> "pt-BR"
                }
                "fr" -> "fr-FR"
                "de" -> "de-DE"
                "it" -> "it-IT"
                "zh" -> "zh-CN"
                "ja" -> "ja-JP"
                "ko" -> "ko-KR"
                else -> "es-ES"
            }
        } catch (_: Exception) { "es-ES" }
    }

    fun getAvailableLanguages(): List<LanguageOption> {
        return listOf(
            LanguageOption("es-ES", "Español (España)", "es"),
            LanguageOption("es-MX", "Español (México)", "es"),
            LanguageOption("es-AR", "Español (Argentina)", "es"),
            LanguageOption("es-CO", "Español (Colombia)", "es"),
            LanguageOption("en-US", "English (US)", "en"),
            LanguageOption("en-GB", "English (UK)", "en"),
            LanguageOption("pt-BR", "Português (Brasil)", "pt"),
            LanguageOption("fr-FR", "Français", "fr"),
            LanguageOption("de-DE", "Deutsch", "de"),
            LanguageOption("it-IT", "Italiano", "it"),
            LanguageOption("zh-CN", "中文", "zh"),
            LanguageOption("ja-JP", "日本語", "ja"),
            LanguageOption("ko-KR", "한국어", "ko"),
        )
    }

    fun stopSpeaking() { try { textToSpeech?.stop() } catch (_: Exception) { } }

    fun destroy() {
        try {
            stopListening()
            stopSpeaking()
            textToSpeech?.shutdown()
            textToSpeech = null
            isInitialized = false
        } catch (_: Exception) { }
    }
}

data class VoiceOption(
    val voice: Voice,
    val displayName: String,
    val language: String
) {
    companion object {
        fun fromVoice(voice: Voice): VoiceOption {
            val languageDisplayName = when (voice.locale.language) {
                "es" -> "Español"
                "en" -> "English"
                "pt" -> "Português"
                "fr" -> "Français"
                "de" -> "Deutsch"
                "it" -> "Italiano"
                else -> voice.locale.displayLanguage
            }
            val countryDisplayName = if (voice.locale.country.isNotEmpty()) {
                when (voice.locale.country.uppercase()) {
                    "ES" -> " (España)"
                    "MX" -> " (México)"
                    "AR" -> " (Argentina)"
                    "CO" -> " (Colombia)"
                    "CL" -> " (Chile)"
                    "PE" -> " (Perú)"
                    "VE" -> " (Venezuela)"
                    "US" -> " (Estados Unidos)"
                    "GB" -> " (Reino Unido)"
                    "AU" -> " (Australia)"
                    "CA" -> " (Canadá)"
                    "BR" -> " (Brasil)"
                    "FR" -> " (Francia)"
                    "DE" -> " (Alemania)"
                    "IT" -> " (Italia)"
                    else -> " (${voice.locale.displayCountry})"
                }
            } else ""
            val networkType = if (voice.isNetworkConnectionRequired) " - Online" else " - Local"
            val voiceChar = when {
                voice.name.contains("-x-eea-") -> " - Voz A"
                voice.name.contains("-x-eeb-") -> " - Voz B"
                voice.name.contains("-x-eec-") -> " - Voz C"
                voice.name.contains("-x-eed-") -> " - Voz D"
                voice.name.contains("-x-eee-") -> " - Voz E"
                voice.name.contains("-x-eef-") -> " - Voz F"
                voice.name.contains("-x-esc-") -> " - Voz A"
                voice.name.contains("-x-esd-") -> " - Voz B"
                voice.name.contains("-x-esf-") -> " - Voz C"
                voice.name.contains("-x-sfb-") -> " - Voz D"
                voice.name.contains("-x-aea-") -> " - Voice A"
                voice.name.contains("-x-aeb-") -> " - Voice B"
                voice.name.contains("-x-aec-") -> " - Voice C"
                voice.name.contains("-x-aed-") -> " - Voice D"
                voice.name.contains("-x-aee-") -> " - Voice E"
                voice.name.contains("-x-aef-") -> " - Voice F"
                voice.name.contains("-x-aeg-") -> " - Voice G"
                voice.name.contains("-x-aeh-") -> " - Voice H"
                voice.name.contains("-x-sfb-") -> " - Voice A"
                voice.name.contains("-x-gba-") -> " - Voice A"
                voice.name.contains("-x-gbb-") -> " - Voice B"
                voice.name.contains("-x-gbc-") -> " - Voice C"
                voice.name.contains("-x-gbd-") -> " - Voice D"
                voice.name.contains("-x-aus-") -> " - Voice A"
                voice.name.contains("-x-aub-") -> " - Voice B"
                voice.name.contains("-x-auc-") -> " - Voice C"
                voice.name.contains("-x-aud-") -> " - Voice D"
                voice.name.contains("-x-caa-") -> " - Voice A"
                voice.name.contains("-x-cab-") -> " - Voice B"
                voice.name.contains("-x-cac-") -> " - Voice C"
                voice.name.contains("-x-cad-") -> " - Voice D"
                voice.name.contains("-language") -> " - Estándar"
                else -> {
                    val voiceName = voice.name
                    when {
                        voiceName.contains("-local") && voiceName.contains("-a-") -> " - Voice A"
                        voiceName.contains("-local") && voiceName.contains("-b-") -> " - Voice B"
                        voiceName.contains("-local") && voiceName.contains("-c-") -> " - Voice C"
                        voiceName.contains("-local") && voiceName.contains("-d-") -> " - Voice D"
                        voiceName.contains("-network") && voiceName.contains("-a-") -> " - Voice A"
                        voiceName.contains("-network") && voiceName.contains("-b-") -> " - Voice B"
                        voiceName.contains("-network") && voiceName.contains("-c-") -> " - Voice C"
                        voiceName.contains("-network") && voiceName.contains("-d-") -> " - Voice D"
                        voiceName.contains("female") -> " - Femenina"
                        voiceName.contains("male") -> " - Masculina"
                        voiceName.endsWith("-local") -> " - Local"
                        voiceName.endsWith("-network") -> " - Red"
                        else -> ""
                    }
                }
            }
            return VoiceOption(
                voice = voice,
                displayName = "$languageDisplayName$countryDisplayName$voiceChar$networkType",
                language = voice.locale.language
            )
        }
    }
}

data class LanguageOption(
    val code: String,
    val displayName: String,
    val languageCode: String
)
