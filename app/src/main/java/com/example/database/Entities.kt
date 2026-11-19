package com.example.database

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "mantra_categories")
data class MantraCategory(
    @PrimaryKey val id: String,
    val name: String,
    val hindiName: String,
    val description: String,
    val sortOrder: Int
)

@Entity(
    tableName = "mantras",
    foreignKeys = [
        ForeignKey(
            entity = MantraCategory::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("categoryId")]
)
data class Mantra(
    @PrimaryKey val id: String,
    val categoryId: String,
    val title: String,
    val sanskritText: String,
    val transliteration: String,
    val hindiMeaning: String,
    val defaultTargetCount: Int = 108,
    val isCustom: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "favorite_mantras",
    foreignKeys = [
        ForeignKey(
            entity = Mantra::class,
            parentColumns = ["id"],
            childColumns = ["mantraId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class FavoriteMantra(
    @PrimaryKey val mantraId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "jaap_sessions",
    indices = [Index("mantraId"), Index("isActive")]
)
data class JaapSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mantraId: String,
    val targetCount: Int = 108,
    val currentCount: Int = 0,
    val completedMalas: Int = 0,
    val startedAt: Long = System.currentTimeMillis(),
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val totalDurationSeconds: Long = 0,
    val isActive: Boolean = true
)

@Entity(
    tableName = "mala_progress",
    indices = [Index("dateKey"), Index("mantraId"), Index("sessionId")]
)
data class MalaProgress(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val mantraId: String,
    val targetCount: Int,
    val completedAt: Long = System.currentTimeMillis(),
    val dateKey: String,
    val durationSeconds: Long = 0
)

@Entity(
    tableName = "jaap_history",
    indices = [Index(value = ["dateKey", "mantraId"], unique = true)]
)
data class JaapHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String,
    val mantraId: String,
    val mantraTitle: String,
    val totalCount: Int = 0,
    val completedMalas: Int = 0,
    val durationSeconds: Long = 0,
    val lastUpdatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1,
    val activeMantraId: String = "shiv_om_namah_shivaya",
    val defaultMalaTarget: Int = 108,
    val dailyJaapGoal: Int = 108,
    val hapticEnabled: Boolean = true,
    val templeBellEnabled: Boolean = true,
    val screenAlwaysOn: Boolean = false,
    val reminderEnabled: Boolean = false,
    val reminderHour: Int = 6,
    val reminderMinute: Int = 30,
    val preferredSpeechRate: Float = 0.85f,
    val preferredVoiceGender: String = "ALL",
    val preferredTtsEngine: String = "OFFLINE_ANDROID",
    val cloudTtsEndpointUrl: String = ""
)

@Entity(
    tableName = "practice_history",
    indices = [Index("mantraId"), Index("practicedAt")]
)
data class PracticeHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mantraId: String,
    val mantraTitle: String,
    val targetText: String,
    val recognizedText: String,
    val wordMatchPercentage: Int,
    val matchedWordsCount: Int,
    val totalWordsCount: Int,
    val missingWords: String,
    val mistakeWords: String,
    val practicedAt: Long = System.currentTimeMillis()
)
