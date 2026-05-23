package com.example.caloriestracker.data.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmbeddingSearchTest {

    @Test
    fun tokenizer_normalizesAndTokenizesCorrectly() {
        val vocab = mapOf(
            "[PAD]" to 0,
            "[UNK]" to 100,
            "[CLS]" to 101,
            "[SEP]" to 102,
            "had" to 4,
            "healthy" to 5,
            "breakfast" to 6,
            "##healthy" to 7
        )

        val tokenizer = WordPieceTokenizer(vocab)
        val result = tokenizer.tokenize("Had healthy breakfast!")

        // Expected tokens: [CLS] (101), had (4), healthy (5), breakfast (6), [UNK] (100), [SEP] (102)
        assertEquals(6, result.inputIds.size)
        assertEquals(101L, result.inputIds[0]) // [CLS]
        assertEquals(4L, result.inputIds[1])  // had
        assertEquals(5L, result.inputIds[2])  // healthy
        assertEquals(6L, result.inputIds[3])  // breakfast
        assertEquals(100L, result.inputIds[4]) // [UNK]
        assertEquals(102L, result.inputIds[5]) // [SEP]

        assertTrue(result.attentionMask.all { it == 1L })
    }

    @Test
    fun cosineSimilarity_calculatesCorrectly() {
        val v1 = floatArrayOf(1f, 0f, 0f)
        val v2 = floatArrayOf(1f, 0f, 0f)
        val v3 = floatArrayOf(0f, 1f, 0f)
        val v4 = floatArrayOf(-1f, 0f, 0f)

        fun similarity(a: FloatArray, b: FloatArray): Float {
            var dot = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
            }
            return dot
        }

        assertEquals(1f, similarity(v1, v2), 1e-5f)
        assertEquals(0f, similarity(v1, v3), 1e-5f)
        assertEquals(-1f, similarity(v1, v4), 1e-5f)
    }
}
