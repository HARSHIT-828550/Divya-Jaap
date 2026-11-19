package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.TimeUtils
import com.example.database.MantraStatsRow
import com.example.domain.JaapRepository
import com.example.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

data class DailyChartPoint(
    val dateKey: String,
    val label: String,
    val count: Int
)

data class DashboardUiState(
    val todayJaapCount: Int = 0,
    val dailyGoal: Int = 108,
    val totalMalasCompleted: Int = 0,
    val lifetimeJaapCount: Int = 0,
    val weeklyJaapCount: Int = 0,
    val monthlyJaapCount: Int = 0,
    val currentStreakDays: Int = 0,
    val totalMeditationDurationSeconds: Long = 0,
    val chartRangeDays: Int = 7,
    val chartPoints: List<DailyChartPoint> = emptyList(),
    val mantraStats: List<MantraStatsRow> = emptyList()
) {
    val dailyGoalProgress: Float
        get() = (todayJaapCount.toFloat() / dailyGoal.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
}

class DashboardViewModel(
    jaapRepository: JaapRepository,
    settingsRepository: SettingsRepository
) : ViewModel() {

    private val chartRangeDays = MutableStateFlow(7)

    val uiState: StateFlow<DashboardUiState> = combine(
        jaapRepository.observeTodayJaapCount(),
        jaapRepository.observeTotalMalasCompleted(),
        jaapRepository.observeLifetimeJaapCount(),
        jaapRepository.observeTotalMeditationDurationSeconds(),
        jaapRepository.observeDailySummaries(),
        jaapRepository.observeMantraStats(),
        settingsRepository.observeSettings(),
        chartRangeDays
    ) { values ->
        val todayCount = values[0] as Int
        val totalMalas = values[1] as Int
        val lifetime = values[2] as Int
        val durationSec = values[3] as Long
        @Suppress("UNCHECKED_CAST")
        val dailyRows = values[4] as List<com.example.database.DailyJaapSummaryRow>
        @Suppress("UNCHECKED_CAST")
        val mantraRows = values[5] as List<MantraStatsRow>
        val settings = values[6] as com.example.database.UserSettings
        val rangeDays = values[7] as Int

        val byDate = dailyRows.associate { it.dateKey to it.totalCount }
        val activeDates = dailyRows.filter { it.totalCount > 0 }.map { it.dateKey }.toSet()
        val streak = TimeUtils.calculateStreak(activeDates)

        val weeklyTotal = (0 until 7).sumOf { offset ->
            byDate[TimeUtils.dateKeyOffset(offset)] ?: 0
        }
        val monthlyTotal = (0 until 30).sumOf { offset ->
            byDate[TimeUtils.dateKeyOffset(offset)] ?: 0
        }

        val points = ((rangeDays - 1) downTo 0).map { offset ->
            val key = TimeUtils.dateKeyOffset(offset)
            DailyChartPoint(
                dateKey = key,
                label = TimeUtils.shortLabelForDateKey(key),
                count = byDate[key] ?: 0
            )
        }

        DashboardUiState(
            todayJaapCount = todayCount,
            dailyGoal = settings.dailyJaapGoal.coerceAtLeast(1),
            totalMalasCompleted = totalMalas,
            lifetimeJaapCount = lifetime,
            weeklyJaapCount = weeklyTotal,
            monthlyJaapCount = monthlyTotal,
            currentStreakDays = streak,
            totalMeditationDurationSeconds = durationSec,
            chartRangeDays = rangeDays,
            chartPoints = points,
            mantraStats = mantraRows
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = DashboardUiState()
    )

    fun setChartRangeDays(days: Int) {
        chartRangeDays.value = if (days == 30) 30 else 7
    }
}
