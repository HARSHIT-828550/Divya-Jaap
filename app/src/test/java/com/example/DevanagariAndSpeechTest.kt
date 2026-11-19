package com.example

import android.speech.SpeechRecognizer
import com.example.core.DevanagariTextAnalyzer
import com.example.core.TimeUtils
import com.example.domain.WordMatchStatus
import com.example.speech.PronunciationComparator
import com.example.speech.SpeechRecognitionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DevanagariAndSpeechTest {

    @Test
    fun `tokenize and split Sanskrit Navarna mantra into words and syllables`() {
        val mantra = "ॐ ऐं ह्रीं क्लीं चामुण्डायै विच्चे"
        val words = DevanagariTextAnalyzer.tokenizeWords(mantra)
        assertEquals(6, words.size)
        assertEquals("ॐ", words[0].word)
        assertEquals(listOf("ॐ"), words[0].syllables)
        assertEquals("चामुण्डायै", words[4].word)
        assertEquals(listOf("चा", "मु", "ण्डा", "यै"), words[4].syllables)
        assertEquals(listOf("वि", "च्चे"), words[5].syllables)
    }

    @Test
    fun `pronunciation comparator handles exact match, missing word, and substitution`() {
        val target = "ॐ नमः शिवाय"
        val exact = PronunciationComparator.compare(target, "ॐ नमः शिवाय")
        assertEquals(100, exact.matchPercentage)
        assertEquals(3, exact.matchedCount)
        assertEquals(0, exact.missingCount)

        val partial = PronunciationComparator.compare(target, "ओम शिवाय")
        assertEquals(2, partial.matchedCount)
        assertEquals(1, partial.missingCount)
        assertEquals(WordMatchStatus.MISSING, partial.wordResults[1].status)
    }

    @Test
    fun `speech recognizer error codes map to clear user messages`() {
        val permMsg = SpeechRecognitionManager.mapSpeechErrorCode(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        val noMatchMsg = SpeechRecognitionManager.mapSpeechErrorCode(SpeechRecognizer.ERROR_NO_MATCH)
        val audioMsg = SpeechRecognitionManager.mapSpeechErrorCode(SpeechRecognizer.ERROR_AUDIO)

        assertTrue(permMsg.contains("Microphone permission"))
        assertTrue(noMatchMsg.contains("Could not recognize"))
        assertTrue(audioMsg.contains("Audio recording error"))
    }

    @Test
    fun `streak calculation counts consecutive active days`() {
        val now = System.currentTimeMillis()
        val dates = setOf(
            TimeUtils.dateKeyOffset(0, now),
            TimeUtils.dateKeyOffset(1, now),
            TimeUtils.dateKeyOffset(2, now),
            TimeUtils.dateKeyOffset(4, now)
        )
        assertEquals(3, TimeUtils.calculateStreak(dates, now))
    }
}
