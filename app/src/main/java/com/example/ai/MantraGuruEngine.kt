package com.example.ai

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.example.audio.MantraBackgroundAudioClient
import com.example.core.DevanagariTextAnalyzer
import com.example.core.MantraWordUnit
import com.example.data.CloudTtsRepository
import com.example.domain.GuruPracticeMode
import com.example.domain.RepeatOption
import com.example.domain.TtsEnginePreference
import com.example.domain.VoiceGenderFilter
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class GuruVoiceOption(
    val name: String,
    val displayLabel: String,
    val localeTag: String,
    val inferredGender: VoiceGenderFilter,
    val isNetworkConnectionRequired: Boolean
)

data class GuruPlaybackState(
    val isTtsReady: Boolean = false,
    val ttsStatusMessage: String = "Initializing Hindi Voice Guru...",
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val isLoadingCloudAudio: Boolean = false,
    val backgroundPlaybackEnabled: Boolean = false,
    val practiceMode: GuruPracticeMode = GuruPracticeMode.FULL_MANTRA,
    val repeatOption: RepeatOption = RepeatOption.THREE,
    val speechRate: Float = 0.85f,
    val genderFilter: VoiceGenderFilter = VoiceGenderFilter.ALL,
    val enginePreference: TtsEnginePreference = TtsEnginePreference.OFFLINE_ANDROID,
    val availableVoices: List<GuruVoiceOption> = emptyList(),
    val selectedVoiceName: String? = null,
    val currentText: String = "",
    val words: List<MantraWordUnit> = emptyList(),
    val activeWordIndex: Int = -1,
    val activeSyllableIndex: Int = -1,
    val currentRepetition: Int = 0,
    val totalRepetitions: Int? = 3
)

class MantraGuruEngine(
    private val context: Context,
    private val cloudTtsRepository: CloudTtsRepository,
    private val backgroundAudioClient: MantraBackgroundAudioClient
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var tts: TextToSpeech? = null
    private var playbackJob: Job? = null

    private val utteranceWaiters = ConcurrentHashMap<String, CompletableDeferred<Boolean>>()
    private val rangeListeners = ConcurrentHashMap<String, (Int, Int) -> Unit>()

    private var resumeWordIndex: Int = 0
    private var resumeSyllableIndex: Int = 0
    private var resumeRepetition: Int = 1

    private val _state = MutableStateFlow(GuruPlaybackState())
    val state: StateFlow<GuruPlaybackState> = _state.asStateFlow()

    init {
        initTts()
    }

    private fun initTts() {
        runCatching {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    val engine = tts ?: return@TextToSpeech
                    val hindi = Locale.forLanguageTag("hi-IN")
                    val langResult = engine.setLanguage(hindi)
                    val voices = discoverHindiVoices(engine)
                    val statusMsg = when {
                        langResult == TextToSpeech.LANG_MISSING_DATA ||
                            langResult == TextToSpeech.LANG_NOT_SUPPORTED ->
                            "Hindi offline voice pack missing; using device default voice."
                        else -> "Offline Hindi Voice Ready"
                    }
                    engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {}

                        override fun onDone(utteranceId: String?) {
                            utteranceId?.let { id ->
                                rangeListeners.remove(id)
                                utteranceWaiters.remove(id)?.complete(true)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            utteranceId?.let { id ->
                                rangeListeners.remove(id)
                                utteranceWaiters.remove(id)?.complete(false)
                            }
                        }

                        override fun onError(utteranceId: String?, errorCode: Int) {
                            utteranceId?.let { id ->
                                rangeListeners.remove(id)
                                utteranceWaiters.remove(id)?.complete(false)
                            }
                        }

                        override fun onRangeStart(
                            utteranceId: String?,
                            start: Int,
                            end: Int,
                            frame: Int
                        ) {
                            utteranceId?.let { id ->
                                rangeListeners[id]?.invoke(start, end)
                            }
                        }
                    })
                    _state.value = _state.value.copy(
                        isTtsReady = true,
                        ttsStatusMessage = statusMsg,
                        availableVoices = voices,
                        selectedVoiceName = voices.firstOrNull()?.name
                    )
                } else {
                    _state.value = _state.value.copy(
                        isTtsReady = false,
                        ttsStatusMessage = "Android TextToSpeech unavailable on this device."
                    )
                }
            }
        }.onFailure {
            _state.value = _state.value.copy(
                isTtsReady = false,
                ttsStatusMessage = "TTS initialization deferred: ${it.localizedMessage ?: "unknown"}"
            )
        }
    }

    private fun discoverHindiVoices(engine: TextToSpeech): List<GuruVoiceOption> {
        val rawVoices: Set<Voice> = runCatching { engine.voices }.getOrNull().orEmpty()
        val hindiOrIndian = rawVoices.filter { voice ->
            val lang = voice.locale.language.lowercase()
            val country = voice.locale.country.uppercase()
            lang == "hi" || lang == "sa" || country == "IN"
        }.ifEmpty { rawVoices.take(8) }

        return hindiOrIndian
            .sortedWith(compareBy<Voice> { it.isNetworkConnectionRequired }.thenBy { it.name })
            .mapIndexed { index, voice ->
                val lower = voice.name.lowercase()
                val featuresStr = runCatching { voice.features.joinToString(" ").lowercase() }.getOrDefault("")
                val combined = "$lower $featuresStr"
                val gender = when {
                    combined.contains("female") || combined.contains("-f-") ||
                        combined.contains("hie") || combined.contains("hia") -> VoiceGenderFilter.FEMALE
                    combined.contains("male") || combined.contains("-m-") ||
                        combined.contains("hid") || combined.contains("hic") -> VoiceGenderFilter.MALE
                    else -> if (index % 2 == 0) VoiceGenderFilter.FEMALE else VoiceGenderFilter.MALE
                }
                GuruVoiceOption(
                    name = voice.name,
                    displayLabel = "${voice.locale.displayLanguage} (${gender.label}) #${index + 1}",
                    localeTag = voice.locale.toLanguageTag(),
                    inferredGender = gender,
                    isNetworkConnectionRequired = voice.isNetworkConnectionRequired
                )
            }
    }

    fun updateMantraText(text: String) {
        val words = DevanagariTextAnalyzer.tokenizeWords(text)
        _state.value = _state.value.copy(
            currentText = text,
            words = words
        )
    }

    fun setPracticeMode(mode: GuruPracticeMode) {
        _state.value = _state.value.copy(practiceMode = mode)
    }

    fun setRepeatOption(option: RepeatOption) {
        _state.value = _state.value.copy(
            repeatOption = option,
            totalRepetitions = option.count
        )
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 1.75f)
        _state.value = _state.value.copy(speechRate = clamped)
        runCatching { tts?.setSpeechRate(clamped) }
    }

    fun setGenderFilter(filter: VoiceGenderFilter) {
        val filtered = filteredVoices(filter)
        _state.value = _state.value.copy(
            genderFilter = filter,
            selectedVoiceName = filtered.firstOrNull()?.name ?: _state.value.selectedVoiceName
        )
    }

    fun selectVoice(voiceName: String) {
        _state.value = _state.value.copy(selectedVoiceName = voiceName)
    }

    fun setEnginePreference(pref: TtsEnginePreference) {
        _state.value = _state.value.copy(enginePreference = pref)
    }

    fun setBackgroundPlaybackEnabled(enabled: Boolean) {
        _state.value = _state.value.copy(backgroundPlaybackEnabled = enabled)
    }

    fun filteredVoices(filter: VoiceGenderFilter = _state.value.genderFilter): List<GuruVoiceOption> {
        val all = _state.value.availableVoices
        return if (filter == VoiceGenderFilter.ALL) all else all.filter { it.inferredGender == filter }
    }

    fun speakSingleToken(text: String, slow: Boolean = true) {
        val clean = text.trim()
        if (clean.isEmpty()) return
        stop()
        scope.launch {
            val rate = if (slow) 0.65f else _state.value.speechRate
            speakUtteranceSuspend(clean, rate)
        }
    }

    fun play(
        mantraText: String,
        mantraTitle: String = "Divya Jaap Mantra",
        cloudEndpointUrl: String = ""
    ) {
        val clean = mantraText.trim()
        if (clean.isEmpty()) return
        val words = DevanagariTextAnalyzer.tokenizeWords(clean)
        _state.value = _state.value.copy(
            currentText = clean,
            words = words
        )

        if (_state.value.isPaused) {
            resume(mantraTitle, cloudEndpointUrl)
            return
        }

        resumeWordIndex = 0
        resumeSyllableIndex = 0
        resumeRepetition = 1
        startPlaybackLoop(mantraTitle, cloudEndpointUrl)
    }

    fun pause() {
        if (!_state.value.isPlaying) return
        playbackJob?.cancel()
        playbackJob = null
        runCatching { tts?.stop() }
        if (_state.value.backgroundPlaybackEnabled) {
            backgroundAudioClient.pause()
        }
        clearPendingUtterances()
        _state.value = _state.value.copy(
            isPlaying = false,
            isPaused = true
        )
    }

    fun resume(mantraTitle: String = "Divya Jaap Mantra", cloudEndpointUrl: String = "") {
        if (_state.value.isPlaying) return
        if (_state.value.backgroundPlaybackEnabled &&
            backgroundAudioClient.state.value.currentTitle != null
        ) {
            backgroundAudioClient.resume()
            _state.value = _state.value.copy(isPlaying = true, isPaused = false)
            return
        }
        startPlaybackLoop(mantraTitle, cloudEndpointUrl)
    }

    fun stop() {
        playbackJob?.cancel()
        playbackJob = null
        runCatching { tts?.stop() }
        backgroundAudioClient.stop()
        clearPendingUtterances()
        resumeWordIndex = 0
        resumeSyllableIndex = 0
        resumeRepetition = 1
        _state.value = _state.value.copy(
            isPlaying = false,
            isPaused = false,
            isLoadingCloudAudio = false,
            activeWordIndex = -1,
            activeSyllableIndex = -1,
            currentRepetition = 0
        )
    }

    private fun startPlaybackLoop(mantraTitle: String, cloudEndpointUrl: String) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            val snapshot = _state.value
            val useCloud = snapshot.enginePreference == TtsEnginePreference.CLOUD_NATURAL_AI &&
                cloudEndpointUrl.isNotBlank()

            if (snapshot.enginePreference == TtsEnginePreference.CLOUD_NATURAL_AI &&
                cloudEndpointUrl.isBlank()
            ) {
                _state.value = _state.value.copy(
                    ttsStatusMessage = "Cloud TTS backend URL not configured in Settings — using Offline Hindi TTS."
                )
            }

            if (useCloud) {
                _state.value = _state.value.copy(
                    isLoadingCloudAudio = true,
                    ttsStatusMessage = "Fetching Natural AI Voice from secure backend..."
                )
                val cloudResult = cloudTtsRepository.synthesizeToAudioFile(
                    endpointUrl = cloudEndpointUrl,
                    text = snapshot.currentText,
                    voiceGender = snapshot.genderFilter.name,
                    speakingRate = snapshot.speechRate
                )
                _state.value = _state.value.copy(isLoadingCloudAudio = false)

                cloudResult.onSuccess { audioFile ->
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        isPaused = false,
                        ttsStatusMessage = "Playing Natural AI Voice via Media3"
                    )
                    backgroundAudioClient.playAudioFile(
                        file = audioFile,
                        title = mantraTitle,
                        subtitle = snapshot.currentText,
                        repeatCount = snapshot.repeatOption.count,
                        speed = snapshot.speechRate
                    )
                    return@launch
                }.onFailure { err ->
                    _state.value = _state.value.copy(
                        ttsStatusMessage = "Cloud TTS unavailable (${err.message ?: "error"}); switched to Offline Hindi TTS."
                    )
                }
            }

            // If Background Playback is explicitly enabled with Offline TTS, synthesize WAV and play via Media3 MediaSessionService!
            if (snapshot.backgroundPlaybackEnabled && snapshot.practiceMode == GuruPracticeMode.FULL_MANTRA) {
                val wavFile = synthesizeOfflineToFile(snapshot.currentText, snapshot.speechRate)
                if (wavFile != null) {
                    _state.value = _state.value.copy(
                        isPlaying = true,
                        isPaused = false,
                        ttsStatusMessage = "Background lock-screen playback active"
                    )
                    backgroundAudioClient.playAudioFile(
                        file = wavFile,
                        title = mantraTitle,
                        subtitle = snapshot.currentText,
                        repeatCount = snapshot.repeatOption.count,
                        speed = 1.0f
                    )
                    return@launch
                }
            }

            // Foreground interactive TTS with word / syllable highlighting & repeat loops
            val maxReps = snapshot.repeatOption.count
            _state.value = _state.value.copy(
                isPlaying = true,
                isPaused = false,
                totalRepetitions = maxReps
            )

            var rep = resumeRepetition.coerceAtLeast(1)
            while (maxReps == null || rep <= maxReps) {
                resumeRepetition = rep
                _state.value = _state.value.copy(currentRepetition = rep)

                when (_state.value.practiceMode) {
                    GuruPracticeMode.FULL_MANTRA -> {
                        speakFullMantraWithWordHighlight(_state.value.currentText, _state.value.words)
                    }
                    GuruPracticeMode.WORD_BY_WORD -> {
                        val words = _state.value.words
                        for (wIdx in resumeWordIndex until words.size) {
                            resumeWordIndex = wIdx
                            _state.value = _state.value.copy(
                                activeWordIndex = wIdx,
                                activeSyllableIndex = -1
                            )
                            speakUtteranceSuspend(words[wIdx].word, _state.value.speechRate)
                            delay(340L)
                        }
                        resumeWordIndex = 0
                    }
                    GuruPracticeMode.SYLLABLE_BY_SYLLABLE -> {
                        val words = _state.value.words
                        for (wIdx in resumeWordIndex until words.size) {
                            resumeWordIndex = wIdx
                            val syllables = words[wIdx].syllables
                            val startSyl = if (wIdx == resumeWordIndex) resumeSyllableIndex else 0
                            for (sIdx in startSyl until syllables.size) {
                                resumeSyllableIndex = sIdx
                                _state.value = _state.value.copy(
                                    activeWordIndex = wIdx,
                                    activeSyllableIndex = sIdx
                                )
                                speakUtteranceSuspend(
                                    syllables[sIdx],
                                    (_state.value.speechRate * 0.85f).coerceAtLeast(0.5f)
                                )
                                delay(280L)
                            }
                            resumeSyllableIndex = 0
                            delay(240L)
                        }
                        resumeWordIndex = 0
                    }
                }

                if (maxReps != null && rep >= maxReps) break
                rep++
                delay(450L)
            }

            resumeWordIndex = 0
            resumeSyllableIndex = 0
            resumeRepetition = 1
            _state.value = _state.value.copy(
                isPlaying = false,
                isPaused = false,
                activeWordIndex = -1,
                activeSyllableIndex = -1,
                currentRepetition = 0
            )
        }
    }

    private suspend fun speakFullMantraWithWordHighlight(
        fullText: String,
        words: List<MantraWordUnit>
    ) {
        val wordCharRanges = computeWordRanges(fullText, words)
        val utteranceId = UUID.randomUUID().toString()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            rangeListeners[utteranceId] = { start, _ ->
                val matchedIdx = wordCharRanges.indexOfFirst { range -> start in range }
                if (matchedIdx >= 0) {
                    _state.value = _state.value.copy(
                        activeWordIndex = matchedIdx,
                        activeSyllableIndex = -1
                    )
                }
            }
        }
        speakUtteranceSuspend(fullText, _state.value.speechRate, utteranceId)
        _state.value = _state.value.copy(activeWordIndex = -1, activeSyllableIndex = -1)
    }

    private fun computeWordRanges(fullText: String, words: List<MantraWordUnit>): List<IntRange> {
        val ranges = mutableListOf<IntRange>()
        var cursor = 0
        for (unit in words) {
            val found = fullText.indexOf(unit.word, startIndex = cursor)
            if (found >= 0) {
                ranges.add(found until (found + unit.word.length))
                cursor = found + unit.word.length
            } else {
                ranges.add(IntRange.EMPTY)
            }
        }
        return ranges
    }

    private suspend fun speakUtteranceSuspend(
        text: String,
        speechRate: Float,
        utteranceId: String = UUID.randomUUID().toString()
    ): Boolean {
        val engine = tts ?: return false
        val deferred = CompletableDeferred<Boolean>()
        utteranceWaiters[utteranceId] = deferred

        applySelectedVoiceAndRate(engine, speechRate)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        val queued = engine.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
        if (queued != TextToSpeech.SUCCESS) {
            utteranceWaiters.remove(utteranceId)
            rangeListeners.remove(utteranceId)
            return false
        }
        return deferred.await()
    }

    private suspend fun synthesizeOfflineToFile(text: String, speechRate: Float): File? {
        val engine = tts ?: return null
        val outFile = withContext(Dispatchers.IO) {
            File(context.cacheDir, "offline_mantra_${System.currentTimeMillis()}.wav")
        }
        val utteranceId = UUID.randomUUID().toString()
        val deferred = CompletableDeferred<Boolean>()
        utteranceWaiters[utteranceId] = deferred

        applySelectedVoiceAndRate(engine, speechRate)

        val params = Bundle().apply {
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
        }
        val res = engine.synthesizeToFile(text, params, outFile, utteranceId)
        if (res != TextToSpeech.SUCCESS) {
            utteranceWaiters.remove(utteranceId)
            return null
        }
        val ok = deferred.await()
        return if (ok && outFile.exists() && outFile.length() > 0L) outFile else null
    }

    private fun applySelectedVoiceAndRate(engine: TextToSpeech, speechRate: Float) {
        engine.setSpeechRate(speechRate.coerceIn(0.5f, 1.75f))
        val targetVoiceName = _state.value.selectedVoiceName
        if (targetVoiceName != null) {
            runCatching {
                engine.voices?.firstOrNull { it.name == targetVoiceName }?.let { v ->
                    engine.voice = v
                }
            }
        }
    }

    private fun clearPendingUtterances() {
        utteranceWaiters.values.forEach { it.complete(false) }
        utteranceWaiters.clear()
        rangeListeners.clear()
    }

    fun release() {
        stop()
        backgroundAudioClient.release()
        runCatching { tts?.shutdown() }
        tts = null
    }
}
