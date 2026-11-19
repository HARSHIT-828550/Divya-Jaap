package com.example.data

import android.content.Context
import android.util.Base64
import androidx.room.withTransaction
import com.example.core.TimeUtils
import com.example.database.DivyaJaapDatabase
import com.example.database.DailyJaapSummaryRow
import com.example.database.FavoriteMantra
import com.example.database.JaapHistory
import com.example.database.JaapSession
import com.example.database.MalaProgress
import com.example.database.Mantra
import com.example.database.MantraCategory
import com.example.database.MantraStatsRow
import com.example.database.MantraWithFavorite
import com.example.database.PracticeHistory
import com.example.database.UserSettings
import com.example.domain.BackupPayload
import com.example.domain.BackupRepository
import com.example.domain.JaapRepository
import com.example.domain.MantraRepository
import com.example.domain.PracticeRepository
import com.example.domain.PronunciationEvaluation
import com.example.domain.SettingsRepository
import com.example.domain.TapResult
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class MantraRepositoryImpl(
    private val db: DivyaJaapDatabase
) : MantraRepository {
    private val mantraDao = db.mantraDao()

    override fun observeCategories(): Flow<List<MantraCategory>> = mantraDao.observeCategories()

    override fun observeMantrasWithFavorites(): Flow<List<MantraWithFavorite>> =
        mantraDao.observeMantrasWithFavorites()

    override fun observeMantraById(id: String): Flow<Mantra?> = mantraDao.observeMantraById(id)

    override suspend fun getMantraById(id: String): Mantra? = mantraDao.getMantraById(id)

    override suspend fun saveCustomMantra(
        id: String?,
        categoryId: String,
        title: String,
        sanskritText: String,
        transliteration: String,
        hindiMeaning: String,
        defaultTargetCount: Int
    ): Mantra {
        val existing = id?.let { mantraDao.getMantraById(it) }
        val mantra = Mantra(
            id = existing?.id ?: "custom_${UUID.randomUUID().toString().take(8)}",
            categoryId = categoryId,
            title = title.trim(),
            sanskritText = sanskritText.trim(),
            transliteration = transliteration.trim(),
            hindiMeaning = hindiMeaning.trim(),
            defaultTargetCount = defaultTargetCount.coerceIn(1, 10008),
            isCustom = existing?.isCustom ?: true,
            createdAt = existing?.createdAt ?: System.currentTimeMillis()
        )
        mantraDao.upsertMantra(mantra)
        return mantra
    }

    override suspend fun updateMantra(mantra: Mantra) {
        mantraDao.upsertMantra(mantra)
    }

    override suspend fun deleteCustomMantra(id: String) {
        mantraDao.deleteCustomMantra(id)
    }

    override suspend fun toggleFavorite(mantraId: String) {
        if (mantraDao.isFavorite(mantraId)) {
            mantraDao.removeFavorite(mantraId)
        } else {
            mantraDao.addFavorite(FavoriteMantra(mantraId = mantraId))
        }
    }
}

class JaapRepositoryImpl(
    private val db: DivyaJaapDatabase
) : JaapRepository {
    private val jaapDao = db.jaapDao()
    private val mantraDao = db.mantraDao()
    private val settingsDao = db.settingsDao()
    private val tapMutex = Mutex()

    override fun observeActiveSession(): Flow<JaapSession?> = jaapDao.observeActiveSession()

    override fun observeTodayJaapCount(): Flow<Int> =
        jaapDao.observeTodayJaapCount(TimeUtils.todayDateKey())

    override fun observeLifetimeJaapCount(): Flow<Int> = jaapDao.observeLifetimeJaapCount()

    override fun observeTotalMalasCompleted(): Flow<Int> = jaapDao.observeTotalMalasCompleted()

    override fun observeTotalMeditationDurationSeconds(): Flow<Long> =
        jaapDao.observeTotalMeditationDurationSeconds()

    override fun observeDailySummaries(): Flow<List<DailyJaapSummaryRow>> =
        jaapDao.observeDailySummaries()

    override fun observeMantraStats(): Flow<List<MantraStatsRow>> = jaapDao.observeMantraStats()

    override suspend fun recordTap(elapsedSecondsSinceLastTap: Long): TapResult = tapMutex.withLock {
        db.withTransaction {
            db.ensureSeeded()
            val now = System.currentTimeMillis()
            val dateKey = TimeUtils.todayDateKey(now)
            val safeElapsed = elapsedSecondsSinceLastTap.coerceIn(1L, 15L)

            val active = jaapDao.getActiveSession() ?: run {
                val defaultMantraId = settingsDao.getSettings()?.activeMantraId
                    ?: mantraDao.getAllMantras().first().id
                val target = settingsDao.getSettings()?.defaultMalaTarget ?: 108
                val insertedId = jaapDao.insertSession(
                    JaapSession(mantraId = defaultMantraId, targetCount = target, isActive = true)
                )
                jaapDao.getActiveSession() ?: JaapSession(
                    id = insertedId,
                    mantraId = defaultMantraId,
                    targetCount = target
                )
            }

            val mantra = mantraDao.getMantraById(active.mantraId)
            val mantraTitle = mantra?.title ?: "Sacred Mantra"
            val target = active.targetCount.coerceAtLeast(1)
            val incremented = active.currentCount + 1
            val malaCompleted = incremented >= target

            val nextCount = if (malaCompleted) 0 else incremented
            val nextCompletedMalas = if (malaCompleted) active.completedMalas + 1 else active.completedMalas

            val updatedSession = active.copy(
                currentCount = nextCount,
                completedMalas = nextCompletedMalas,
                lastUpdatedAt = now,
                totalDurationSeconds = active.totalDurationSeconds + safeElapsed
            )
            jaapDao.updateSession(updatedSession)

            if (malaCompleted) {
                jaapDao.insertMalaProgress(
                    MalaProgress(
                        sessionId = active.id,
                        mantraId = active.mantraId,
                        targetCount = target,
                        completedAt = now,
                        dateKey = dateKey,
                        durationSeconds = updatedSession.totalDurationSeconds
                    )
                )
            }

            val existingHistory = jaapDao.getHistoryForDateAndMantra(dateKey, active.mantraId)
            if (existingHistory != null) {
                jaapDao.upsertJaapHistory(
                    existingHistory.copy(
                        mantraTitle = mantraTitle,
                        totalCount = existingHistory.totalCount + 1,
                        completedMalas = existingHistory.completedMalas + if (malaCompleted) 1 else 0,
                        durationSeconds = existingHistory.durationSeconds + safeElapsed,
                        lastUpdatedAt = now
                    )
                )
            } else {
                jaapDao.upsertJaapHistory(
                    JaapHistory(
                        dateKey = dateKey,
                        mantraId = active.mantraId,
                        mantraTitle = mantraTitle,
                        totalCount = 1,
                        completedMalas = if (malaCompleted) 1 else 0,
                        durationSeconds = safeElapsed,
                        lastUpdatedAt = now
                    )
                )
            }

            TapResult(
                currentCount = nextCount,
                targetCount = target,
                completedMalasInSession = nextCompletedMalas,
                malaJustCompleted = malaCompleted
            )
        }
    }

    override suspend fun undoLastTap(): JaapSession? = tapMutex.withLock {
        db.withTransaction {
            val active = jaapDao.getActiveSession() ?: return@withTransaction null
            if (active.currentCount <= 0) return@withTransaction active

            val now = System.currentTimeMillis()
            val dateKey = TimeUtils.todayDateKey(now)
            val updated = active.copy(
                currentCount = active.currentCount - 1,
                lastUpdatedAt = now
            )
            jaapDao.updateSession(updated)

            val history = jaapDao.getHistoryForDateAndMantra(dateKey, active.mantraId)
            if (history != null && history.totalCount > 0) {
                jaapDao.upsertJaapHistory(
                    history.copy(
                        totalCount = (history.totalCount - 1).coerceAtLeast(0),
                        lastUpdatedAt = now
                    )
                )
            }
            updated
        }
    }

    override suspend fun resetCurrentMala(): JaapSession? = tapMutex.withLock {
        db.withTransaction {
            val active = jaapDao.getActiveSession() ?: return@withTransaction null
            val updated = active.copy(
                currentCount = 0,
                lastUpdatedAt = System.currentTimeMillis()
            )
            jaapDao.updateSession(updated)
            updated
        }
    }

    override suspend fun startNextMala(): JaapSession? = tapMutex.withLock {
        db.withTransaction {
            val active = jaapDao.getActiveSession() ?: return@withTransaction null
            val updated = active.copy(
                currentCount = 0,
                lastUpdatedAt = System.currentTimeMillis()
            )
            jaapDao.updateSession(updated)
            updated
        }
    }

    override suspend fun selectActiveMantra(
        mantraId: String,
        targetCountOverride: Int?
    ): JaapSession = tapMutex.withLock {
        db.withTransaction {
            db.ensureSeeded()
            val settings = settingsDao.getSettings() ?: UserSettings()
            val mantra = mantraDao.getMantraById(mantraId)
            val target = (targetCountOverride ?: settings.defaultMalaTarget).coerceAtLeast(1)

            settingsDao.upsertSettings(
                settings.copy(
                    activeMantraId = mantraId,
                    defaultMalaTarget = target
                )
            )

            val current = jaapDao.getActiveSession()
            if (current != null && current.mantraId == mantraId) {
                val updated = current.copy(
                    targetCount = target,
                    lastUpdatedAt = System.currentTimeMillis()
                )
                jaapDao.updateSession(updated)
                return@withTransaction updated
            }

            jaapDao.deactivateAllSessions()
            val newSession = JaapSession(
                mantraId = mantra?.id ?: mantraId,
                targetCount = target,
                currentCount = 0,
                completedMalas = 0,
                isActive = true
            )
            val id = jaapDao.insertSession(newSession)
            newSession.copy(id = id)
        }
    }

    override suspend fun updateMalaTarget(newTarget: Int): JaapSession? = tapMutex.withLock {
        db.withTransaction {
            val safeTarget = newTarget.coerceIn(1, 10008)
            val settings = settingsDao.getSettings() ?: UserSettings()
            settingsDao.upsertSettings(settings.copy(defaultMalaTarget = safeTarget))

            val active = jaapDao.getActiveSession() ?: return@withTransaction null
            val adjustedCount = active.currentCount.coerceAtMost((safeTarget - 1).coerceAtLeast(0))
            val updated = active.copy(
                targetCount = safeTarget,
                currentCount = adjustedCount,
                lastUpdatedAt = System.currentTimeMillis()
            )
            jaapDao.updateSession(updated)
            updated
        }
    }

    override suspend fun getAllHistory(): List<JaapHistory> = jaapDao.getAllJaapHistory()

    override suspend fun getAllMalaProgress(): List<MalaProgress> = jaapDao.getAllMalaProgress()
}

class PracticeRepositoryImpl(
    private val db: DivyaJaapDatabase
) : PracticeRepository {
    private val practiceDao = db.practiceDao()

    override fun observeRecentPracticeHistory(): Flow<List<PracticeHistory>> =
        practiceDao.observeRecentPracticeHistory()

    override suspend fun savePracticeResult(
        mantraId: String,
        mantraTitle: String,
        evaluation: PronunciationEvaluation
    ) {
        val missingWords = evaluation.wordResults
            .filter { it.status == com.example.domain.WordMatchStatus.MISSING }
            .joinToString(", ") { it.expectedWord }

        val mistakeWords = evaluation.wordResults
            .filter { it.status == com.example.domain.WordMatchStatus.MISTAKE }
            .joinToString(", ") { "${it.expectedWord} → ${it.recognizedWord ?: "?"}" }

        practiceDao.insertPracticeHistory(
            PracticeHistory(
                mantraId = mantraId,
                mantraTitle = mantraTitle,
                targetText = evaluation.targetText,
                recognizedText = evaluation.recognizedText,
                wordMatchPercentage = evaluation.matchPercentage,
                matchedWordsCount = evaluation.matchedCount,
                totalWordsCount = evaluation.totalTargetWords,
                missingWords = missingWords,
                mistakeWords = mistakeWords
            )
        )
    }
}

class SettingsRepositoryImpl(
    private val db: DivyaJaapDatabase
) : SettingsRepository {
    private val settingsDao = db.settingsDao()

    override fun observeSettings(): Flow<UserSettings> =
        settingsDao.observeSettings().map { it ?: UserSettings() }

    override suspend fun getSettings(): UserSettings {
        db.ensureSeeded()
        return settingsDao.getSettings() ?: UserSettings()
    }

    override suspend fun updateSettings(transform: (UserSettings) -> UserSettings) {
        db.withTransaction {
            val current = settingsDao.getSettings() ?: UserSettings()
            settingsDao.upsertSettings(transform(current))
        }
    }
}

class BackupRepositoryImpl(
    private val db: DivyaJaapDatabase
) : BackupRepository {

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val adapter = moshi.adapter(BackupPayload::class.java).indent("  ")

    override suspend fun exportBackupJson(): String = withContext(Dispatchers.IO) {
        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            appVersion = "1.0",
            categories = db.mantraDao().getAllCategories(),
            mantras = db.mantraDao().getAllMantras(),
            favorites = db.mantraDao().getAllFavorites(),
            sessions = db.jaapDao().getAllSessions(),
            malaProgress = db.jaapDao().getAllMalaProgress(),
            jaapHistory = db.jaapDao().getAllJaapHistory(),
            practiceHistory = db.practiceDao().getAllPracticeHistory(),
            settings = db.settingsDao().getSettings()
        )
        adapter.toJson(payload)
    }

    override suspend fun restoreBackupJson(json: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = adapter.fromJson(json)
                ?: throw IllegalArgumentException("Invalid Divya Jaap backup file.")
            db.withTransaction {
                if (payload.categories.isNotEmpty()) {
                    db.mantraDao().insertCategories(payload.categories)
                }
                payload.mantras.forEach { db.mantraDao().upsertMantra(it) }
                db.mantraDao().insertFavorites(payload.favorites)

                db.jaapDao().clearSessions()
                db.jaapDao().clearMalaProgress()
                db.jaapDao().clearJaapHistory()
                db.practiceDao().clearPracticeHistory()

                db.jaapDao().insertSessions(payload.sessions)
                db.jaapDao().insertMalaProgressList(payload.malaProgress)
                db.jaapDao().insertJaapHistoryList(payload.jaapHistory)
                db.practiceDao().insertPracticeHistoryList(payload.practiceHistory)
                payload.settings?.let { db.settingsDao().upsertSettings(it) }
                db.ensureSeeded()
            }
            Unit
        }
    }

    override suspend fun exportHistoryCsv(): String = withContext(Dispatchers.IO) {
        val history = db.jaapDao().getAllJaapHistory()
        val sb = StringBuilder()
        sb.appendLine("Date,Mantra ID,Mantra Title,Total Jaap Count,Completed Malas,Duration Seconds")
        history.forEach { row ->
            val escapedTitle = row.mantraTitle.replace("\"", "\"\"")
            sb.appendLine("${row.dateKey},${row.mantraId},\"$escapedTitle\",${row.totalCount},${row.completedMalas},${row.durationSeconds}")
        }
        sb.toString()
    }
}

class CloudTtsRepository(
    private val context: Context,
    private val apiService: CloudTtsApiService
) {
    suspend fun synthesizeToAudioFile(
        endpointUrl: String,
        text: String,
        voiceGender: String,
        speakingRate: Float
    ): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanUrl = endpointUrl.trim()
            require(cleanUrl.startsWith("https://") || cleanUrl.startsWith("http://")) {
                "Configure a valid Cloud TTS backend URL in Settings first."
            }
            val response = apiService.synthesize(
                endpointUrl = cleanUrl,
                request = CloudTtsRequest(
                    text = text,
                    languageCode = "hi-IN",
                    voiceGender = voiceGender,
                    speakingRate = speakingRate
                )
            )
            if (!response.isSuccessful) {
                throw IllegalStateException("Cloud TTS server returned HTTP ${response.code()}")
            }
            val body = response.body() ?: throw IllegalStateException("Empty Cloud TTS response")
            val base64Audio = body.audioBase64
                ?: throw IllegalStateException("Backend response did not include audioBase64")
            val bytes = Base64.decode(base64Audio, Base64.DEFAULT)
            val ext = if (body.mimeType.contains("wav", ignoreCase = true)) "wav" else "mp3"
            val outFile = File(context.cacheDir, "cloud_mantra_${System.currentTimeMillis()}.$ext")
            outFile.writeBytes(bytes)
            outFile
        }
    }
}
