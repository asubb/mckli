package com.mckli.tools

import kotlin.math.max

interface SimilarityService {
    fun calculateSimilarityScore(query: String, target: String): Double
    fun calculateMaxChunkSimilarity(query: String, target: String): Double
    fun levenshteinDistance(s1: String, s2: String): Int
}

class DefaultSimilarityService : SimilarityService {
    override fun levenshteinDistance(s1: String, s2: String): Int {
        if (s1 == s2) return 0
        if (s1.isEmpty()) return s2.length
        if (s2.isEmpty()) return s1.length

        val n = s1.length
        val m = s2.length

        var prevRow = IntArray(m + 1) { it }
        var currRow = IntArray(m + 1)

        for (i in 1..n) {
            currRow[0] = i
            for (j in 1..m) {
                val substitutionCost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                currRow[j] = minOf(
                    currRow[j - 1] + 1,        // insertion
                    prevRow[j] + 1,            // deletion
                    prevRow[j - 1] + substitutionCost // substitution
                )
            }
            val temp = prevRow
            prevRow = currRow
            currRow = temp
        }

        return prevRow[m]
    }

    override fun calculateSimilarityScore(query: String, target: String): Double {
        if (query.isEmpty() || target.isEmpty()) return 0.0
        val distance = levenshteinDistance(query.lowercase(), target.lowercase())
        val maxLength = max(query.length, target.length)
        return 1.0 - (distance.toDouble() / maxLength.toDouble())
    }

    override fun calculateMaxChunkSimilarity(query: String, target: String): Double {
        if (query.isEmpty() || target.isEmpty()) return 0.0
        
        // Exact substring match should always give a high score
        if (target.contains(query, ignoreCase = true)) {
            return 0.95 // Slightly less than 1.0 to prioritize exact full matches if any
        }

        val chunks = mutableListOf<String>()
        // Split by sentences
        target.split(".", "!", "?", "\n").forEach { sentence ->
            val trimmed = sentence.trim()
            if (trimmed.isNotEmpty()) {
                chunks.add(trimmed)
                // Also add individual words from long sentences to catch matches within them
                if (trimmed.contains(" ")) {
                    trimmed.split(" ").forEach { word ->
                        val wordTrimmed = word.trim().removeSurrounding("(", ")").removeSurrounding("[", "]").removeSurrounding("\"", "\"")
                        if (wordTrimmed.isNotEmpty()) {
                            chunks.add(wordTrimmed)
                        }
                    }
                }
            }
        }

        if (chunks.isEmpty()) return calculateSimilarityScore(query, target)

        return chunks.maxOfOrNull { calculateSimilarityScore(query, it) } ?: 0.0
    }
}
