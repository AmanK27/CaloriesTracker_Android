package com.example.caloriestracker.data.search

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.regex.Pattern

class WordPieceTokenizer(private val vocab: Map<String, Int>) {

    private val unkTokenId = 100
    private val clsTokenId = 101
    private val sepTokenId = 102

    constructor(context: Context) : this(loadVocab(context))

    companion object {
        private fun loadVocab(context: Context): Map<String, Int> {
            val tempVocab = mutableMapOf<String, Int>()
            context.assets.open("vocab.txt").use { inputStream ->
                java.io.BufferedReader(java.io.InputStreamReader(inputStream)).use { reader ->
                    var index = 0
                    var line = reader.readLine()
                    while (line != null) {
                        tempVocab[line] = index
                        index++
                        line = reader.readLine()
                    }
                }
            }
            return tempVocab
        }
    }

    fun tokenize(text: String): TokenizedResult {
        val normalized = normalize(text)
        val words = normalized.split(Pattern.compile("\\s+")).filter { it.isNotEmpty() }
        
        val inputIds = mutableListOf<Long>()
        inputIds.add(clsTokenId.toLong()) // Start with [CLS]

        for (word in words) {
            val subTokens = wordPieceTokenize(word)
            inputIds.addAll(subTokens)
        }

        inputIds.add(sepTokenId.toLong()) // End with [SEP]

        val attentionMask = LongArray(inputIds.size) { 1L }

        return TokenizedResult(inputIds.toLongArray(), attentionMask)
    }

    private fun normalize(text: String): String {
        // Lowercase
        var cleaned = text.lowercase()
        // Strip accents
        cleaned = Normalizer.normalize(cleaned, Normalizer.Form.NFD)
        cleaned = cleaned.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
        return cleaned
    }

    private fun wordPieceTokenize(word: String): List<Long> {
        val wordLen = word.length
        var start = 0
        val subTokens = mutableListOf<Long>()

        while (start < wordLen) {
            var end = wordLen
            var matchId: Int? = null

            while (start < end) {
                var substr = word.substring(start, end)
                if (start > 0) {
                    substr = "##$substr"
                }

                val id = vocab[substr]
                if (id != null) {
                    matchId = id
                    break
                }
                end--
            }

            if (matchId == null) {
                subTokens.add(unkTokenId.toLong())
                break
            } else {
                subTokens.add(matchId.toLong())
                start = end
            }
        }

        return subTokens
    }
}

data class TokenizedResult(
    val inputIds: LongArray,
    val attentionMask: LongArray
)
