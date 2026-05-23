package com.example.caloriestracker.data.network

import com.example.caloriestracker.BuildConfig
import com.example.caloriestracker.domain.model.JournalEntry
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiService @Inject constructor(
    private val geminiApi: GeminiApi,
    private val json: Json
) {
    private val apiKey = BuildConfig.GEMINI_API_KEY

    suspend fun generateSummaryAndSentiment(content: String): SummarySentimentResult? {
        if (apiKey.isEmpty() || apiKey == "YOUR_KEY") return null
        if (content.trim().isEmpty()) return null

        val prompt = """
            You are a concise, empathetic personal journaling assistant.
            Analyze the following journal entry text and provide a structured JSON response.
            Focus on extracting a 2-3 sentence summary and an overall numeric sentiment score between -1.0 (very negative) and +1.0 (very positive).

            Response Mime Type must be JSON.
            Required JSON keys:
            - "summary": (String) A brief 2-3 sentence summary of events and feelings.
            - "sentiment": (Float) Sentiment score from -1.0 to 1.0.

            Journal Entry Text:
            "$content"
        """.trimIndent()

        return try {
            val response = geminiApi.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawJson != null) {
                json.decodeFromString<SummarySentimentResult>(rawJson)
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateSmartTags(content: String): List<String>? {
        if (apiKey.isEmpty() || apiKey == "YOUR_KEY") return null
        if (content.trim().isEmpty()) return null

        val prompt = """
            Analyze the following journal entry and suggest up to 3 relevant, concise keywords/tags (e.g., mindfulness, workout, healthy).
            Provide your response in JSON format.
            
            Required JSON keys:
            - "tags": (List of Strings) Up to 3 suggested tags, lowercased and clean.

            Journal Entry:
            "$content"
        """.trimIndent()

        return try {
            val response = geminiApi.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )

            val rawJson = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (rawJson != null) {
                json.decodeFromString<SmartTagsResult>(rawJson).tags
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun generateReflection(query: String, contextEntries: List<JournalEntry>): String {
        if (apiKey.isEmpty() || apiKey == "YOUR_KEY") {
            return "Reflection is unavailable because the Gemini API key is missing. Please set your key in local.properties."
        }

        val contextPrompt = if (contextEntries.isEmpty()) {
            "No matching entries were found in history."
        } else {
            contextEntries.joinToString("\n\n") { entry ->
                val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(entry.timestamp)
                "Entry on $dateStr:\nTitle: ${entry.title ?: "Untitled"}\nContent: ${entry.content}"
            }
        }

        val prompt = """
            System: You are an empathetic personal journaling assistant and life coach.
            Your role is to offer helpful reflections, identify patterns, and ask insightful questions based on the context of the user's past journal entries.
            Keep your reflections brief, encouraging, and actionable (maximum 4-5 sentences).
            If the user asks questions about their history and there is no relevant context, politely guide them to write more entries about it.

            ---
            Context (Past Journal Entries):
            $contextPrompt
            ---
            User Question:
            "$query"

            Response:
        """.trimIndent()

        return try {
            val response = geminiApi.generateContent(
                apiKey = apiKey,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt))))
                )
            )
            response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: "I couldn't generate a reflection. Please try again."
        } catch (e: Exception) {
            "Error generating reflection: ${e.message}"
        }
    }
}

@Serializable
data class SummarySentimentResult(
    val summary: String,
    val sentiment: Float
)

@Serializable
data class SmartTagsResult(
    val tags: List<String>
)
