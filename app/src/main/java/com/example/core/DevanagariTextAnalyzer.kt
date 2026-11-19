package com.example.core

data class MantraWordUnit(
    val index: Int,
    val word: String,
    val normalized: String,
    val syllables: List<String>
)

/**
 * Deterministic analyzer for Sanskrit/Hindi Devanagari and Latin transliterated mantras.
 * Splits text into word tokens and Devanagari akshara (orthographic syllables), preserving
 * conjunct consonants (virama/halant), vowel matras, anusvara, chandrabindu, and visarga.
 */
object DevanagariTextAnalyzer {

    private val PUNCTUATION_REGEX = Regex("[।॥.,!?;:\"'()\\[\\]{}\\-—–/]+")
    private val WHITESPACE_REGEX = Regex("\\s+")

    fun tokenizeWords(text: String): List<MantraWordUnit> {
        val cleaned = text.trim()
        if (cleaned.isEmpty()) return emptyList()
        val rawTokens = cleaned.split(WHITESPACE_REGEX)
            .map { it.trim() }
            .filter { it.isNotEmpty() && !PUNCTUATION_REGEX.matches(it) }

        return rawTokens.mapIndexed { idx, token ->
            val stripped = token.replace(PUNCTUATION_REGEX, "").trim()
            val displayWord = stripped.ifEmpty { token }
            MantraWordUnit(
                index = idx,
                word = displayWord,
                normalized = normalizeWord(displayWord),
                syllables = splitIntoSyllables(displayWord)
            )
        }.filter { it.word.isNotBlank() }
    }

    fun normalizeText(text: String): List<String> {
        return tokenizeWords(text).map { it.normalized }.filter { it.isNotEmpty() }
    }

    fun normalizeWord(word: String): String {
        return word
            .replace(PUNCTUATION_REGEX, "")
            .replace("\u200D", "") // ZWJ
            .replace("\u200C", "") // ZWNJ
            .replace("ॐ", "ओम")
            .replace("ँ", "ं")
            .lowercase()
            .trim()
    }

    fun splitIntoSyllables(word: String): List<String> {
        val clean = word.replace(PUNCTUATION_REGEX, "").trim()
        if (clean.isEmpty()) return emptyList()

        // If mostly Latin characters, split into simple CV-like chunks
        if (clean.all { it.code < 0x0900 || it.code > 0x097F }) {
            return splitLatinWord(clean)
        }

        val syllables = mutableListOf<String>()
        val current = StringBuilder()
        var i = 0
        while (i < clean.length) {
            val ch = clean[i]
            val code = ch.code

            val isConsonant = (code in 0x0915..0x0939) || (code in 0x0958..0x095F)
            val isIndependentVowel = (code in 0x0904..0x0914) || code == 0x0960 || code == 0x0961
            val isOm = code == 0x0950
            val isVirama = code == 0x094D
            val isCombiningSign = isDevanagariCombiningMark(code)

            when {
                isOm -> {
                    if (current.isNotEmpty()) {
                        syllables.add(current.toString())
                        current.clear()
                    }
                    syllables.add(ch.toString())
                    i++
                }
                isIndependentVowel -> {
                    if (current.isNotEmpty()) {
                        syllables.add(current.toString())
                        current.clear()
                    }
                    current.append(ch)
                    i++
                    while (i < clean.length && isDevanagariCombiningMark(clean[i].code)) {
                        current.append(clean[i])
                        i++
                    }
                    syllables.add(current.toString())
                    current.clear()
                }
                isConsonant -> {
                    // Check if previous ends with virama (conjunct consonant)
                    val endsWithVirama = current.isNotEmpty() && current.last().code == 0x094D
                    if (current.isNotEmpty() && !endsWithVirama) {
                        syllables.add(current.toString())
                        current.clear()
                    }
                    current.append(ch)
                    i++
                    // Attach nukta if present
                    if (i < clean.length && clean[i].code == 0x093C) {
                        current.append(clean[i])
                        i++
                    }
                    // If followed by virama, attach virama and keep building conjunct unless at word end
                    if (i < clean.length && clean[i].code == 0x094D) {
                        current.append(clean[i])
                        i++
                        if (i >= clean.length) {
                            syllables.add(current.toString())
                            current.clear()
                        }
                    } else {
                        // Attach any matras / anusvara / visarga / vedic accents
                        while (i < clean.length && isDevanagariCombiningMark(clean[i].code)) {
                            current.append(clean[i])
                            i++
                        }
                        syllables.add(current.toString())
                        current.clear()
                    }
                }
                isCombiningSign || isVirama -> {
                    current.append(ch)
                    i++
                }
                else -> {
                    if (current.isNotEmpty()) {
                        syllables.add(current.toString())
                        current.clear()
                    }
                    syllables.add(ch.toString())
                    i++
                }
            }
        }
        if (current.isNotEmpty()) {
            syllables.add(current.toString())
        }
        return syllables.ifEmpty { listOf(clean) }
    }

    private fun isDevanagariCombiningMark(code: Int): Boolean {
        return (code in 0x0900..0x0903) || // Chandrabindu, Anusvara, Visarga
            code == 0x093A || code == 0x093B || code == 0x093C ||
            (code in 0x093E..0x094C) || // Vowel signs (matras)
            code == 0x094E || code == 0x094F ||
            (code in 0x0951..0x0957) || // Vedic tone marks
            code == 0x0962 || code == 0x0963
    }

    private fun splitLatinWord(word: String): List<String> {
        if (word.length <= 3) return listOf(word)
        val vowels = setOf('a', 'e', 'i', 'o', 'u', 'ā', 'ī', 'ū', 'ṛ', 'ṝ', 'ṃ', 'ḥ')
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        for (i in word.indices) {
            val c = word[i]
            sb.append(c)
            val isVowel = c.lowercaseChar() in vowels
            if (isVowel && i < word.length - 1 && word[i + 1].lowercaseChar() !in vowels) {
                result.add(sb.toString())
                sb.clear()
            }
        }
        if (sb.isNotEmpty()) {
            if (result.isNotEmpty() && sb.length == 1 && sb[0].lowercaseChar() !in vowels) {
                result[result.lastIndex] = result.last() + sb.toString()
            } else {
                result.add(sb.toString())
            }
        }
        return result.ifEmpty { listOf(word) }
    }
}
