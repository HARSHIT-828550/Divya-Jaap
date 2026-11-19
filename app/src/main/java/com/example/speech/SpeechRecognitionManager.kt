package com.example.speech

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface SpeechRecognitionState {
    data object Idle : SpeechRecognitionState
    data class Listening(val partialText: String = "", val rmsDb: Float = 0f) : SpeechRecognitionState
    data class Result(val recognizedText: String, val alternatives: List<String> = emptyList()) : SpeechRecognitionState
    data class Error(val errorCode: Int, val message: String) : SpeechRecognitionState
}

class SpeechRecognitionManager(private val context: Context) {

    private val _state = MutableStateFlow<SpeechRecognitionState>(SpeechRecognitionState.Idle)
    val state: StateFlow<SpeechRecognitionState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening(languageTag: String = "hi-IN") {
        if (!isAvailable()) {
            _state.value = SpeechRecognitionState.Error(
                SpeechRecognizer.ERROR_CLIENT,
                "Speech recognition service is not available on this device. Please ensure Google Speech Services is installed."
            )
            return
        }

        destroyRecognizer()
        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer
        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = SpeechRecognitionState.Listening()
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {
                val current = _state.value
                if (current is SpeechRecognitionState.Listening) {
                    _state.value = current.copy(rmsDb = rmsdB)
                }
            }

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {}

            override fun onError(error: Int) {
                _state.value = SpeechRecognitionState.Error(error, mapSpeechErrorCode(error))
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION).orEmpty()
                val primary = matches.firstOrNull().orEmpty()
                if (primary.isBlank()) {
                    _state.value = SpeechRecognitionState.Error(
                        SpeechRecognizer.ERROR_NO_MATCH,
                        mapSpeechErrorCode(SpeechRecognizer.ERROR_NO_MATCH)
                    )
                } else {
                    _state.value = SpeechRecognitionState.Result(
                        recognizedText = primary,
                        alternatives = matches
                    )
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                if (partial.isNotBlank()) {
                    _state.value = SpeechRecognitionState.Listening(partialText = partial)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, languageTag)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        _state.value = SpeechRecognitionState.Listening()
        recognizer.startListening(intent)
    }

    fun stopListening() {
        runCatching { speechRecognizer?.stopListening() }
    }

    fun resetState() {
        _state.value = SpeechRecognitionState.Idle
    }

    fun destroyRecognizer() {
        runCatching {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        }
        speechRecognizer = null
    }

    companion object {
        fun mapSpeechErrorCode(errorCode: Int): String = when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error. Please check your microphone."
            SpeechRecognizer.ERROR_CLIENT -> "Speech client error. Please try again."
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required for Pronunciation Practice."
            SpeechRecognizer.ERROR_NETWORK -> "Network error while recognizing speech. Try enabling offline Hindi speech pack."
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out due to network latency."
            SpeechRecognizer.ERROR_NO_MATCH -> "Could not recognize words clearly. Speak slowly and closer to the mic."
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech recognizer is busy. Wait a moment and retry."
            SpeechRecognizer.ERROR_SERVER -> "Speech recognition server error."
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech detected. Tap the microphone and chant clearly."
            else -> "Speech recognition could not complete (code $errorCode). Please retry."
        }
    }
}
