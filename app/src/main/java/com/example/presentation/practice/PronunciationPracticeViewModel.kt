package com.example.presentation.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GuruPlaybackState
import com.example.ai.MantraGuruEngine
import com.example.database.MantraWithFavorite
import com.example.database.PracticeHistory
import com.example.domain.MantraRepository
import com.example.domain.PracticeRepository
import com.example.domain.PronunciationEvaluation
import com.example.speech.PronunciationComparator
import com.example.speech.SpeechRecognitionManager
import com.example.speech.SpeechRecognitionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class GuidedPracticeStep(val stepNumber: Int, val title: String) {
    LISTEN(1, "Step 1: Listen to Mantra Guru"),
    SPEAK(2, "Step 2: Tap Mic & Chant Clearly"),
    REVIEW(3, "Step 3: Word-Level Feedback")
}

data class PronunciationPracticeUiState(
    val selectedMantraId: String = "durga_navarna_mantra",
    val selectedMantraTitle: String = "Navarna Durga Mantra",
    val targetText: String = "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",
    val step: GuidedPracticeStep = GuidedPracticeStep.LISTEN,
    val speechState: SpeechRecognitionState = SpeechRecognitionState.Idle,
    val guruState: GuruPlaybackState = GuruPlaybackState(),
    val evaluation: PronunciationEvaluation? = null,
    val availableMantras: List<MantraWithFavorite> = emptyList(),
    val recentHistory: List<PracticeHistory> = emptyList(),
    val isSpeechAvailable: Boolean = true
)

class PronunciationPracticeViewModel(
    private val guruEngine: MantraGuruEngine,
    private val speechRecognitionManager: SpeechRecognitionManager,
    private val mantraRepository: MantraRepository,
    private val practiceRepository: PracticeRepository
) : ViewModel() {

    private val selectedMantraId = MutableStateFlow("durga_navarna_mantra")
    private val selectedMantraTitle = MutableStateFlow("Navarna Durga Mantra")
    private val targetText = MutableStateFlow("ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे")
    private val step = MutableStateFlow(GuidedPracticeStep.LISTEN)
    private val evaluation = MutableStateFlow<PronunciationEvaluation?>(null)

    init {
        viewModelScope.launch {
            speechRecognitionManager.state.collect { speech ->
                if (speech is SpeechRecognitionState.Result) {
                    val result = PronunciationComparator.compare(
                        targetText = targetText.value,
                        recognizedText = speech.recognizedText
                    )
                    evaluation.value = result
                    step.value = GuidedPracticeStep.REVIEW
                    practiceRepository.savePracticeResult(
                        mantraId = selectedMantraId.value,
                        mantraTitle = selectedMantraTitle.value,
                        evaluation = result
                    )
                }
            }
        }
    }

    val uiState: StateFlow<PronunciationPracticeUiState> = combine(
        selectedMantraId,
        selectedMantraTitle,
        targetText,
        step,
        speechRecognitionManager.state,
        guruEngine.state,
        evaluation,
        mantraRepository.observeMantrasWithFavorites(),
        practiceRepository.observeRecentPracticeHistory()
    ) { values ->
        PronunciationPracticeUiState(
            selectedMantraId = values[0] as String,
            selectedMantraTitle = values[1] as String,
            targetText = values[2] as String,
            step = values[3] as GuidedPracticeStep,
            speechState = values[4] as SpeechRecognitionState,
            guruState = values[5] as GuruPlaybackState,
            evaluation = values[6] as PronunciationEvaluation?,
            availableMantras = values[7] as List<MantraWithFavorite>,
            recentHistory = values[8] as List<PracticeHistory>,
            isSpeechAvailable = speechRecognitionManager.isAvailable()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PronunciationPracticeUiState(
            isSpeechAvailable = speechRecognitionManager.isAvailable()
        )
    )

    fun selectMantra(mantraId: String) {
        viewModelScope.launch {
            val m = mantraRepository.getMantraById(mantraId) ?: return@launch
            guruEngine.stop()
            speechRecognitionManager.destroyRecognizer()
            speechRecognitionManager.resetState()
            selectedMantraId.value = m.id
            selectedMantraTitle.value = m.title
            targetText.value = m.sanskritText
            evaluation.value = null
            step.value = GuidedPracticeStep.LISTEN
        }
    }

    fun setCustomTargetText(text: String) {
        selectedMantraId.value = "custom_practice"
        selectedMantraTitle.value = "Custom Practice Mantra"
        targetText.value = text
        evaluation.value = null
        step.value = GuidedPracticeStep.LISTEN
    }

    fun listenSlowly() {
        guruEngine.setSpeechRate(0.70f)
        guruEngine.play(
            mantraText = targetText.value,
            mantraTitle = selectedMantraTitle.value
        )
        step.value = GuidedPracticeStep.SPEAK
    }

    fun speakDifficultWord(word: String) {
        guruEngine.speakSingleToken(word, slow = true)
    }

    fun startSpeechCapture() {
        guruEngine.stop()
        step.value = GuidedPracticeStep.SPEAK
        speechRecognitionManager.startListening("hi-IN")
    }

    fun stopSpeechCapture() {
        speechRecognitionManager.stopListening()
    }

    fun retryPractice() {
        speechRecognitionManager.resetState()
        evaluation.value = null
        step.value = GuidedPracticeStep.SPEAK
    }
}
