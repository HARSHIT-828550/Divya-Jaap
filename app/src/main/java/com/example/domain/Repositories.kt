package com.example.domain

import com.example.database.DailyJaapSummaryRow
import com.example.database.JaapHistory
import com.example.database.JaapSession
import com.example.database.MalaProgress
import com.example.database.Mantra
import com.example.database.MantraCategory
import com.example.database.MantraStatsRow
import com.example.database.MantraWithFavorite
import com.example.database.PracticeHistory
import com.example.database.UserSettings
import kotlinx.coroutines.flow.Flow

interface MantraRepository {
    fun observeCategories(): Flow<List<MantraCategory>>
    fun observeMantrasWithFavorites(): Flow<List<MantraWithFavorite>>
    fun observeMantraById(id: String): Flow<Mantra?>
    suspend fun getMantraById(id: String): Mantra?
    suspend fun saveCustomMantra(
        id: String?,
        categoryId: String,
        title: String,
        sanskritText: String,
        transliteration: String,
        hindiMeaning: String,
        defaultTargetCount: Int
    ): Mantra
    suspend fun updateMantra(mantra: Mantra)
    suspend fun deleteCustomMantra(id: String)
    suspend fun toggleFavorite(mantraId: String)
}

interface JaapRepository {
    fun observeActiveSession(): Flow<JaapSession?>
    fun observeTodayJaapCount(): Flow<Int>
    fun observeLifetimeJaapCount(): Flow<Int>
    fun observeTotalMalasCompleted(): Flow<Int>
    fun observeTotalMeditationDurationSeconds(): Flow<Long>
    fun observeDailySummaries(): Flow<List<DailyJaapSummaryRow>>
    fun observeMantraStats(): Flow<List<MantraStatsRow>>

    suspend fun recordTap(elapsedSecondsSinceLastTap: Long): TapResult
    suspend fun undoLastTap(): JaapSession?
    suspend fun resetCurrentMala(): JaapSession?
    suspend fun startNextMala(): JaapSession?
    suspend fun selectActiveMantra(mantraId: String, targetCountOverride: Int? = null): JaapSession
    suspend fun updateMalaTarget(newTarget: Int): JaapSession?
    suspend fun getAllHistory(): List<JaapHistory>
    suspend fun getAllMalaProgress(): List<MalaProgress>
}

interface PracticeRepository {
    fun observeRecentPracticeHistory(): Flow<List<PracticeHistory>>
    suspend fun savePracticeResult(
        mantraId: String,
        mantraTitle: String,
        evaluation: PronunciationEvaluation
    )
}

interface SettingsRepository {
    fun observeSettings(): Flow<UserSettings>
    suspend fun getSettings(): UserSettings
    suspend fun updateSettings(transform: (UserSettings) -> UserSettings)
}

interface BackupRepository {
    suspend fun exportBackupJson(): String
    suspend fun restoreBackupJson(json: String): Result<Unit>
    suspend fun exportHistoryCsv(): String
}
