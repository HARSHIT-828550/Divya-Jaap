package com.example.presentation.counter

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.TempleBellPlayer
import com.example.data.SettingsDataStore
import com.example.database.JaapSession
import com.example.database.Mantra
import com.example.database.UserSettings
import com.example.domain.CounterDisplayMode
import com.example.domain.JaapRepository
import com.example.domain.MantraRepository
import com.example.domain.SettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

data class JaapCounterUiState(
    val activeSession: JaapSession? = null,
    val activeMantra: Mantra? = null,
    val todayJaapCount: Int = 0,
    val lifetimeJaapCount: Int = 0,
    val totalMalasCompleted: Int = 0,
    val settings: UserSettings = UserSettings(),
    val displayMode: CounterDisplayMode = CounterDisplayMode.RUDRAKSHA_MALA,
    val showMalaCompletedBanner: Boolean = false,
    val lastCompletedMalaNumber: Int = 0,
    val showResetConfirmDialog: Boolean = false,
    val showTargetPickerDialog: Boolean = false
) {
    val currentCount: Int get() = activeSession?.currentCount ?: 0
    val targetCount: Int get() = (activeSession?.targetCount ?: settings.defaultMalaTarget).coerceAtLeast(1)
    val sessionCompletedMalas: Int get() = activeSession?.completedMalas ?: 0
    val progressFraction: Float get() = (currentCount.toFloat() / targetCount.toFloat()).coerceIn(0f, 1f)
}

class JaapCounterViewModel(
    private val jaapRepository: JaapRepository,
    private val mantraRepository: MantraRepository,
    private val settingsRepository: SettingsRepository,
    private val settingsDataStore: SettingsDataStore,
    private val templeBellPlayer: TempleBellPlayer
) : ViewModel() {

    private val showMalaCompleted = MutableStateFlow(false)
    private val lastCompletedMalaNum = MutableStateFlow(0)
    private val showResetDialog = MutableStateFlow(false)
    private val showTargetDialog = MutableStateFlow(false)

    private val lastTapTimestampMs = AtomicLong(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val activeMantraFlow = jaapRepository.observeActiveSession()
        .flatMapLatest { session ->
            if (session == null) flowOf(null)
            else mantraRepository.observeMantraById(session.mantraId)
        }

    val uiState: StateFlow<JaapCounterUiState> = combine(
        jaapRepository.observeActiveSession(),
        activeMantraFlow,
        jaapRepository.observeTodayJaapCount(),
        jaapRepository.observeLifetimeJaapCount(),
        jaapRepository.observeTotalMalasCompleted(),
        settingsRepository.observeSettings(),
        settingsDataStore.counterDisplayMode,
        showMalaCompleted,
        lastCompletedMalaNum,
        showResetDialog,
        showTargetDialog
    ) { values ->
        val session = values[0] as JaapSession?
        val mantra = values[1] as Mantra?
        val today = values[2] as Int
        val lifetime = values[3] as Int
        val totalMalas = values[4] as Int
        val settings = values[5] as UserSettings
        val displayMode = values[6] as CounterDisplayMode
        val completedBanner = values[7] as Boolean
        val lastMalaNum = values[8] as Int
        val resetDlg = values[9] as Boolean
        val targetDlg = values[10] as Boolean

        JaapCounterUiState(
            activeSession = session,
            activeMantra = mantra,
            todayJaapCount = today,
            lifetimeJaapCount = lifetime,
            totalMalasCompleted = totalMalas,
            settings = settings,
            displayMode = displayMode,
            showMalaCompletedBanner = completedBanner,
            lastCompletedMalaNumber = lastMalaNum,
            showResetConfirmDialog = resetDlg,
            showTargetPickerDialog = targetDlg
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = JaapCounterUiState()
    )

    fun onCounterTap() {
        val now = System.currentTimeMillis()
        val prev = lastTapTimestampMs.getAndSet(now)
        val elapsedSeconds = if (prev == 0L) 2L else ((now - prev) / 1000L).coerceIn(1L, 15L)

        viewModelScope.launch {
            val result = jaapRepository.recordTap(elapsedSeconds)
            if (result.malaJustCompleted) {
                if (uiState.value.settings.templeBellEnabled) {
                    templeBellPlayer.playBell()
                }
                lastCompletedMalaNum.value = result.completedMalasInSession
                showMalaCompleted.value = true
            }
        }
    }

    fun dismissMalaCompletedBanner() {
        showMalaCompleted.value = false
    }

    fun onUndoTap() {
        viewModelScope.launch {
            jaapRepository.undoLastTap()
        }
    }

    fun requestResetConfirmation() {
        showResetDialog.value = true
    }

    fun dismissResetConfirmation() {
        showResetDialog.value = false
    }

    fun confirmResetCurrentMala() {
        showResetDialog.value = false
        viewModelScope.launch {
            jaapRepository.resetCurrentMala()
        }
    }

    fun onStartNextMala() {
        showMalaCompleted.value = false
        viewModelScope.launch {
            jaapRepository.startNextMala()
        }
    }

    fun openTargetPicker() {
        showTargetDialog.value = true
    }

    fun closeTargetPicker() {
        showTargetDialog.value = false
    }

    fun updateMalaTarget(target: Int) {
        showTargetDialog.value = false
        viewModelScope.launch {
            jaapRepository.updateMalaTarget(target)
        }
    }

    fun setDisplayMode(mode: CounterDisplayMode) {
        viewModelScope.launch {
            settingsDataStore.setCounterDisplayMode(mode)
        }
    }
}
