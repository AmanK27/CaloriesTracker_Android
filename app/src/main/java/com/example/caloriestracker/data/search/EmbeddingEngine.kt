package com.example.caloriestracker.data.search

interface EmbeddingEngine {
    fun generateEmbedding(text: String): FloatArray
}
