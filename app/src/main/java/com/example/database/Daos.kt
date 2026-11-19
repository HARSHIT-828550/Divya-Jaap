package com.example.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

data class MantraWithFavorite(
    val id: String,
    val categoryId: String,
    val title: String,
    val sanskritText: String,
    val transliteration: String,
    val hindiMeaning: String,
    val defaultTargetCount: Int,
    val isCustom: Boolean,
    val createdAt: Long,
    @ColumnInfo(name = "isFavorite") val isFavorite: Boolean
)

data class MantraStatsRow(
    val mantraId: String,
    val mantraTitle: String,
    val totalCount: Int,
    val completedMalas: Int,
    val totalDurationSeconds: Long
)

data class DailyJaapSummaryRow(
    val dateKey: String,
    val totalCount: Int,
    val completedMalas: Int,
    val totalDurationSeconds: Long
)

@Dao
interface MantraDao {
    @Query("SELECT * FROM mantra_categories ORDER BY sortOrder ASC")
    fun observeCategories(): Flow<List<MantraCategory>>

    @Query("SELECT * FROM mantra_categories ORDER BY sortOrder ASC")
    suspend fun getAllCategories(): List<MantraCategory>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCategories(categories: List<MantraCategory>)

    @Query(
        """
        SELECT m.*, EXISTS(SELECT 1 FROM favorite_mantras f WHERE f.mantraId = m.id) AS isFavorite
        FROM mantras m
        ORDER BY m.isCustom ASC, m.createdAt ASC
        """
    )
    fun observeMantrasWithFavorites(): Flow<List<MantraWithFavorite>>

    @Query("SELECT * FROM mantras ORDER BY isCustom ASC, createdAt ASC")
    suspend fun getAllMantras(): List<Mantra>

    @Query("SELECT * FROM mantras WHERE id = :id LIMIT 1")
    suspend fun getMantraById(id: String): Mantra?

    @Query("SELECT * FROM mantras WHERE id = :id LIMIT 1")
    fun observeMantraById(id: String): Flow<Mantra?>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMantrasIgnore(mantras: List<Mantra>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMantra(mantra: Mantra)

    @Update
    suspend fun updateMantra(mantra: Mantra)

    @Query("DELETE FROM mantras WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomMantra(id: String)

    @Query("SELECT COUNT(*) FROM mantras")
    suspend fun countMantras(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteMantra)

    @Query("DELETE FROM favorite_mantras WHERE mantraId = :mantraId")
    suspend fun removeFavorite(mantraId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorite_mantras WHERE mantraId = :mantraId)")
    suspend fun isFavorite(mantraId: String): Boolean

    @Query("SELECT * FROM favorite_mantras")
    suspend fun getAllFavorites(): List<FavoriteMantra>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFavorites(favorites: List<FavoriteMantra>)
}

@Dao
interface JaapDao {
    @Query("SELECT * FROM jaap_sessions WHERE isActive = 1 ORDER BY lastUpdatedAt DESC LIMIT 1")
    fun observeActiveSession(): Flow<JaapSession?>

    @Query("SELECT * FROM jaap_sessions WHERE isActive = 1 ORDER BY lastUpdatedAt DESC LIMIT 1")
    suspend fun getActiveSession(): JaapSession?

    @Query("SELECT * FROM jaap_sessions ORDER BY lastUpdatedAt DESC")
    suspend fun getAllSessions(): List<JaapSession>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: JaapSession): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<JaapSession>)

    @Update
    suspend fun updateSession(session: JaapSession)

    @Query("UPDATE jaap_sessions SET isActive = 0 WHERE isActive = 1")
    suspend fun deactivateAllSessions()

    @Insert
    suspend fun insertMalaProgress(progress: MalaProgress): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMalaProgressList(items: List<MalaProgress>)

    @Query("SELECT * FROM mala_progress ORDER BY completedAt DESC")
    suspend fun getAllMalaProgress(): List<MalaProgress>

    @Query("SELECT COUNT(*) FROM mala_progress")
    fun observeTotalMalasCompleted(): Flow<Int>

    @Query("SELECT * FROM jaap_history WHERE dateKey = :dateKey AND mantraId = :mantraId LIMIT 1")
    suspend fun getHistoryForDateAndMantra(dateKey: String, mantraId: String): JaapHistory?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertJaapHistory(history: JaapHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJaapHistoryList(items: List<JaapHistory>)

    @Query("SELECT * FROM jaap_history ORDER BY dateKey DESC, lastUpdatedAt DESC")
    suspend fun getAllJaapHistory(): List<JaapHistory>

    @Query("SELECT COALESCE(SUM(totalCount), 0) FROM jaap_history WHERE dateKey = :dateKey")
    fun observeTodayJaapCount(dateKey: String): Flow<Int>

    @Query("SELECT COALESCE(SUM(totalCount), 0) FROM jaap_history")
    fun observeLifetimeJaapCount(): Flow<Int>

    @Query("SELECT COALESCE(SUM(durationSeconds), 0) FROM jaap_history")
    fun observeTotalMeditationDurationSeconds(): Flow<Long>

    @Query(
        """
        SELECT dateKey,
               COALESCE(SUM(totalCount), 0) AS totalCount,
               COALESCE(SUM(completedMalas), 0) AS completedMalas,
               COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds
        FROM jaap_history
        GROUP BY dateKey
        ORDER BY dateKey DESC
        """
    )
    fun observeDailySummaries(): Flow<List<DailyJaapSummaryRow>>

    @Query(
        """
        SELECT mantraId,
               MAX(mantraTitle) AS mantraTitle,
               COALESCE(SUM(totalCount), 0) AS totalCount,
               COALESCE(SUM(completedMalas), 0) AS completedMalas,
               COALESCE(SUM(durationSeconds), 0) AS totalDurationSeconds
        FROM jaap_history
        GROUP BY mantraId
        ORDER BY totalCount DESC
        """
    )
    fun observeMantraStats(): Flow<List<MantraStatsRow>>

    @Query("DELETE FROM jaap_sessions")
    suspend fun clearSessions()

    @Query("DELETE FROM mala_progress")
    suspend fun clearMalaProgress()

    @Query("DELETE FROM jaap_history")
    suspend fun clearJaapHistory()
}

@Dao
interface PracticeDao {
    @Query("SELECT * FROM practice_history ORDER BY practicedAt DESC LIMIT 50")
    fun observeRecentPracticeHistory(): Flow<List<PracticeHistory>>

    @Query("SELECT * FROM practice_history ORDER BY practicedAt DESC")
    suspend fun getAllPracticeHistory(): List<PracticeHistory>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeHistory(item: PracticeHistory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPracticeHistoryList(items: List<PracticeHistory>)

    @Query("DELETE FROM practice_history")
    suspend fun clearPracticeHistory()
}

@Dao
interface SettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    fun observeSettings(): Flow<UserSettings?>

    @Query("SELECT * FROM user_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettings(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSettings(settings: UserSettings)
}
