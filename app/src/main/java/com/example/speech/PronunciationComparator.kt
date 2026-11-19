package com.example.speech

import com.example.core.DevanagariTextAnalyzer
import com.example.domain.PronunciationEvaluation
import com.example.domain.WordComparisonResult
import com.example.domain.WordMatchStatus
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Aligns recognized Speech-to-Text tokens against the target mantra words.
 * Note: STT matching measures word recognition accuracy, not true Vedic swara or phonetic articulation.
 */
object PronunciationComparator {

    fun compare(targetText: String, recognizedText: String): PronunciationEvaluation {
        val targetUnits = DevanagariTextAnalyzer.tokenizeWords(targetText)
        val spokenUnits = DevanagariTextAnalyzer.tokenizeWords(recognizedText)

        if (targetUnits.isEmpty()) {
            return PronunciationEvaluation(
                targetText = targetText,
                recognizedText = recognizedText,
                wordResults = emptyList(),
                extraSpokenWords = spokenUnits.map { it.word },
                matchedCount = 0,
                missingCount = 0,
                mistakeCount = 0,
                totalTargetWords = 0,
                matchPercentage = 0
            )
        }

        if (spokenUnits.isEmpty()) {
            val missing = targetUnits.map {
                WordComparisonResult(
                    index = it.index,
                    expectedWord = it.word,
                    recognizedWord = null,
                    status = WordMatchStatus.MISSING,
                    similarity = 0f
                )
            }
            return PronunciationEvaluation(
                targetText = targetText,
                recognizedText = recognizedText,
                wordResults = missing,
                extraSpokenWords = emptyList(),
                matchedCount = 0,
                missingCount = targetUnits.size,
                mistakeCount = 0,
                totalTargetWords = targetUnits.size,
                matchPercentage = 0
            )
        }

        val n = targetUnits.size
        val m = spokenUnits.size
        // Needleman-Wunsch style DP alignment using normalized string similarity
        val dp = Array(n + 1) { FloatArray(m + 1) }
        val gapPenalty = -0.35f

        for (i in 1..n) dp[i][0] = i * gapPenalty
        for (j in 1..m) dp[0][j] = j * gapPenalty

        for (i in 1..n) {
            for (j in 1..m) {
                val sim = normalizedSimilarity(targetUnits[i - 1].normalized, spokenUnits[j - 1].normalized)
                val pairScore = if (sim >= 0.85f) 1.0f else if (sim >= 0.45f) 0.45f else -0.25f
                val diag = dp[i - 1][j - 1] + pairScore
                val up = dp[i - 1][j] + gapPenalty
                val left = dp[i][j - 1] + gapPenalty
                dp[i][j] = max(diag, max(up, left))
            }
        }

        // Backtrack alignment
        val results = arrayOfNulls<WordComparisonResult>(n)
        val extraWords = mutableListOf<String>()
        var i = n
        var j = m
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0) {
                val sim = normalizedSimilarity(targetUnits[i - 1].normalized, spokenUnits[j - 1].normalized)
                val pairScore = if (sim >= 0.85f) 1.0f else if (sim >= 0.45f) 0.45f else -0.25f
                if (kotlin.math.abs(dp[i][j] - (dp[i - 1][j - 1] + pairScore)) < 0.0001f) {
                    val status = when {
                        sim >= 0.85f -> WordMatchStatus.MATCHED
                        else -> WordMatchStatus.MISTAKE
                    }
                    results[i - 1] = WordComparisonResult(
                        index = targetUnits[i - 1].index,
                        expectedWord = targetUnits[i - 1].word,
                        recognizedWord = spokenUnits[j - 1].word,
                        status = status,
                        similarity = sim
                    )
                    i--
                    j--
                    continue
                }
            }
            if (i > 0 && kotlin.math.abs(dp[i][j] - (dp[i - 1][j] + gapPenalty)) < 0.0001f) {
                results[i - 1] = WordComparisonResult(
                    index = targetUnits[i - 1].index,
                    expectedWord = targetUnits[i - 1].word,
                    recognizedWord = null,
                    status = WordMatchStatus.MISSING,
                    similarity = 0f
                )
                i--
            } else if (j > 0) {
                extraWords.add(spokenUnits[j - 1].word)
                j--
            } else {
                break
            }
        }

        val finalResults = results.mapIndexed { idx, res ->
            res ?: WordComparisonResult(
                index = idx,
                expectedWord = targetUnits[idx].word,
                recognizedWord = null,
                status = WordMatchStatus.MISSING,
                similarity = 0f
            )
        }

        val matchedCount = finalResults.count { it.status == WordMatchStatus.MATCHED }
        val missingCount = finalResults.count { it.status == WordMatchStatus.MISSING }
        val mistakeCount = finalResults.count { it.status == WordMatchStatus.MISTAKE }
        val weightedScore = finalResults.sumOf {
            when (it.status) {
                WordMatchStatus.MATCHED -> 1.0
                WordMatchStatus.MISTAKE -> it.similarity.toDouble().coerceIn(0.0, 0.7)
                WordMatchStatus.MISSING -> 0.0
            }
        }
        val percentage = ((weightedScore / targetUnits.size.toDouble()) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

        return PronunciationEvaluation(
            targetText = targetText,
            recognizedText = recognizedText,
            wordResults = finalResults,
            extraSpokenWords = extraWords.reversed(),
            matchedCount = matchedCount,
            missingCount = missingCount,
            mistakeCount = mistakeCount,
            totalTargetWords = targetUnits.size,
            matchPercentage = percentage
        )
    }

    fun normalizedSimilarity(a: String, b: String): Float {
        if (a == b) return 1.0f
        if (a.isEmpty() || b.isEmpty()) return 0.0f
        val dist = levenshteinDistance(a, b)
        val maxLen = max(a.length, b.length)
        return (1.0f - (dist.toFloat() / maxLen.toFloat())).coerceIn(0f, 1f)
    }

    private fun levenshteinDistance(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        val prev = IntArray(n + 1) { it }
        val curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = min(
                    min(curr[j - 1] + 1, prev[j] + 1),
                    prev[j - 1] + cost
                )
            }
            curr.copyInto(prev)
        }
        return prev[n]
    }
}
