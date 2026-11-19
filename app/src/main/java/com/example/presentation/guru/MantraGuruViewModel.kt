package com.example.presentation.guru

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ai.GuruPlaybackState
import com.example.ai.MantraGuruEngine
import com.example.data.SettingsDataStore
import com.example.database.MantraWithFavorite
import com.example.database.UserSettings
import com.example.domain.GuruPracticeMode
import com.example.domain.MantraRepository
import com.example.domain.RepeatOption
import com.example.domain.SettingsRepository
import com.example.domain.TtsEnginePreference
import com.example.domain.VoiceGenderFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MantraGuruUiState(
    val inputText: String = "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे",
    val activeTitle: String = "Navarna Durga Mantra",
    val playbackState: GuruPlaybackState = GuruPlaybackState(),
    val quickMantras: List<MantraWithFavorite> = emptyList(),
    val settings: UserSettings = UserSettings()
)

class MantraGuruViewModel(
    private val guruEngine: MantraGuruEngine,
    private val mantraRepository: MantraRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val inputTextFlow = MutableStateFlow("ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे")
    private val activeTitleFlow = MutableStateFlow("Navarna Durga Mantra")

    init {
        viewModelScope.launch {
            val savedText = settingsDataStore.lastGuruText.first()
            inputTextFlow.value = savedText
            guruEngine.updateMantraText(savedText)

            val settings = settingsRepository.getSettings()
            guruEngine.setSpeechRate(settings.preferredSpeechRate)
            runCatching {
                guruEngine.setGenderFilter(VoiceGenderFilter.valueOf(settings.preferredVoiceGender))
            }
            runCatching {
                guruEngine.setEnginePreference(TtsEnginePreference.valueOf(settings.preferredTtsEngine))
            }
        }
    }

    val uiState: StateFlow<MantraGuruUiState> = combine(
        inputTextFlow,
        activeTitleFlow,
        guruEngine.state,
        mantraRepository.observeMantrasWithFavorites(),
        settingsRepository.observeSettings()
    ) { text, title, playback, mantras, settings ->
        MantraGuruUiState(
            inputText = text,
            activeTitle = title,
            playbackState = playback,
            quickMantras = mantras,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MantraGuruUiState()
    )

    fun onTextChanged(text: String) {
        inputTextFlow.value = text
        activeTitleFlow.value = "Custom Mantra"
        guruEngine.updateMantraText(text)
        viewModelScope.launch {
            settingsDataStore.setLastGuruText(text)
        }
    }

    fun selectPresetMantra(mantraId: String) {
        viewModelScope.launch {
            val m = mantraRepository.getMantraById(mantraId) ?: return@launch
            guruEngine.stop()
            inputTextFlow.value = m.sanskritText
            activeTitleFlow.value = m.title
            guruEngine.updateMantraText(m.sanskritText)
            settingsDataStore.setLastGuruText(m.sanskritText)
        }
    }

    fun loadTextAndPlay(title: String, text: String) {
        guruEngine.stop()
        inputTextFlow.value = text
        activeTitleFlow.value = title
        guruEngine.updateMantraText(text)
        viewModelScope.launch {
            settingsDataStore.setLastGuruText(text)
        }
    }

    fun playOrResume() {
        val state = uiState.value
        if (state.playbackState.isPlaying) {
            guruEngine.pause()
        } else {
            guruEngine.play(
                mantraText = state.inputText,
                mantraTitle = state.activeTitle,
                cloudEndpointUrl = state.settings.cloudTtsEndpointUrl
            )
        }
    }

    fun stop() = guruEngine.stop()

    fun speakWord(word: String) = guruEngine.speakSingleToken(word, slow = true)

    fun setPracticeMode(mode: GuruPracticeMode) = guruEngine.setPracticeMode(mode)

    fun setRepeatOption(option: RepeatOption) = guruEngine.setRepeatOption(option)

    fun setSpeechRate(rate: Float) {
        guruEngine.setSpeechRate(rate)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(preferredSpeechRate = rate) }
        }
    }

    fun setGenderFilter(filter: VoiceGenderFilter) {
        guruEngine.setGenderFilter(filter)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(preferredVoiceGender = filter.name) }
        }
    }

    fun selectVoice(voiceName: String) = guruEngine.selectVoice(voiceName)

    fun setEnginePreference(pref: TtsEnginePreference) {
        guruEngine.setEnginePreference(pref)
        viewModelScope.launch {
            settingsRepository.updateSettings { it.copy(preferredTtsEngine = pref.name) }
        }
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        guruEngine.setBackgroundPlaybackEnabled(enabled)
    }
}
