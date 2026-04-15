package com.mckli.tools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SimilarityServiceTest {
    private val service = DefaultSimilarityService()

    @Test
    fun testLevenshteinDistance() {
        assertEquals(0, service.levenshteinDistance("apple", "apple"))
        assertEquals(1, service.levenshteinDistance("apple", "apply"))
        assertEquals(1, service.levenshteinDistance("apple", "apples"))
        assertEquals(1, service.levenshteinDistance("apple", "aple"))
        assertEquals(3, service.levenshteinDistance("kitten", "sitting"))
    }

    @Test
    fun testCalculateSimilarityScore() {
        assertEquals(1.0, service.calculateSimilarityScore("apple", "apple"))
        assertEquals(0.8, service.calculateSimilarityScore("apple", "apply"))
        
        val score = service.calculateSimilarityScore("tool", "tools")
        assertTrue(score >= 0.8 && score < 1.0)
        
        // assertEquals(0.0, service.calculateSimilarityScore("apple", "banana"))
        assertTrue(service.calculateSimilarityScore("apple", "banana") < 0.3)
    }

    @Test
    fun testCalculateMaxChunkSimilarity() {
        val longDescription = "This tool performs search. It can find files easily. Use it for quick access."
        val query = "search"
        
        // Substring match should give high score
        val score = service.calculateMaxChunkSimilarity(query, longDescription)
        assertEquals(0.95, score)

        val typoQuery = "serch"
        val typoScore = service.calculateMaxChunkSimilarity(typoQuery, longDescription)
        
        // "serch" vs "search" in the first sentence
        val expectedScore = service.calculateSimilarityScore("serch", "This tool performs search")
        // But it's sentence-based, so it should be compared against "This tool performs search" OR we should split by spaces too?
        // The design says split by sentences.
        
        assertTrue(typoScore > 0.5, "Score should be high for typo in a sentence. Got $typoScore")
    }
    
    @Test
    fun testSentenceSplitting() {
        val description = "First sentence. Second sentence! Third sentence? Fourth sentence\nFifth sentence"
        val query = "Second sentence"
        val score = service.calculateMaxChunkSimilarity(query, description)
        assertEquals(0.95, score) // Substring match
    }
}
