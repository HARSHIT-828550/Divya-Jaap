package com.example.domain

enum class CounterDisplayMode(val label: String) {
    DIGITAL("Digital"),
    RUDRAKSHA_MALA("Rudraksha Mala"),
    MINIMAL_MEDITATION("Meditation")
}

enum class GuruPracticeMode(val label: String) {
    FULL_MANTRA("Full Mantra"),
    WORD_BY_WORD("Word by Word"),
    SYLLABLE_BY_SYLLABLE("Syllable by Syllable")
}

enum class RepeatOption(val count: Int?, val label: String) {
    ONCE(1, "1×"),
    THREE(3, "3×"),
    ELEVEN(11, "11×"),
    ONE_ZERO_EIGHT(108, "108×"),
    INFINITE(null, "∞ Loop")
}

enum class VoiceGenderFilter(val label: String) {
    ALL("All"),
    FEMALE("Female"),
    MALE("Male")
}

enum class TtsEnginePreference(val label: String) {
    OFFLINE_ANDROID("Offline Hindi TTS"),
    CLOUD_NATURAL_AI("Natural AI Voice (Cloud)")
}

data class TapResult(
    val currentCount: Int,
    val targetCount: Int,
    val completedMalasInSession: Int,
    val malaJustCompleted: Boolean
)

enum class WordMatchStatus {
    MATCHED,
    MISTAKE,
    MISSING
}

data class WordComparisonResult(
    val index: Int,
    val expectedWord: String,
    val recognizedWord: String?,
    val status: WordMatchStatus,
    val similarity: Float
)

data class PronunciationEvaluation(
    val targetText: String,
    val recognizedText: String,
    val wordResults: List<WordComparisonResult>,
    val extraSpokenWords: List<String>,
    val matchedCount: Int,
    val missingCount: Int,
    val mistakeCount: Int,
    val totalTargetWords: Int,
    val matchPercentage: Int
)

data class BackupPayload(
    val exportedAt: Long,
    val appVersion: String,
    val categories: List<com.example.database.MantraCategory>,
    val mantras: List<com.example.database.Mantra>,
    val favorites: List<com.example.database.FavoriteMantra>,
    val sessions: List<com.example.database.JaapSession>,
    val malaProgress: List<com.example.database.MalaProgress>,
    val jaapHistory: List<com.example.database.JaapHistory>,
    val practiceHistory: List<com.example.database.PracticeHistory>,
    val settings: com.example.database.UserSettings?
)
