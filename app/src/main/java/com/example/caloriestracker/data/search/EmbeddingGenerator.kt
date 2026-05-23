package com.example.caloriestracker.data.search

import android.content.Context
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EmbeddingGenerator @Inject constructor(
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: Context
) : EmbeddingEngine {
    private val tokenizer = WordPieceTokenizer(context)
    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession

    init {
        val modelBytes = context.assets.open("model_quantized.onnx").use { it.readBytes() }
        session = env.createSession(modelBytes, OrtSession.SessionOptions())
    }

    @Synchronized
    override fun generateEmbedding(text: String): FloatArray {
        val tokenized = tokenizer.tokenize(text)
        val seqLen = tokenized.inputIds.size

        // Build 2D inputs of shape [1, seqLen]
        val inputIds2D = arrayOf(tokenized.inputIds)
        val attentionMask2D = arrayOf(tokenized.attentionMask)
        val tokenTypeIds2D = arrayOf(LongArray(seqLen) { 0L })

        val inputs = mutableMapOf<String, OnnxTensor>()
        inputs["input_ids"] = OnnxTensor.createTensor(env, inputIds2D)
        inputs["attention_mask"] = OnnxTensor.createTensor(env, attentionMask2D)
        
        if (session.inputNames.contains("token_type_ids")) {
            inputs["token_type_ids"] = OnnxTensor.createTensor(env, tokenTypeIds2D)
        }

        try {
            val results = session.run(inputs)
            results.use {
                val outputTensor = results.get(0) as OnnxTensor
                val outputValue = outputTensor.value as Array<Array<FloatArray>> // Shape: [1, seqLen, 384]
                
                val tokenStates = outputValue[0] // Shape: [seqLen, 384]
                val embeddingDim = 384
                val meanEmbedding = FloatArray(embeddingDim)

                // Mean Pooling
                for (i in 0 until embeddingDim) {
                    var sum = 0f
                    for (j in 0 until seqLen) {
                        sum += tokenStates[j][i]
                    }
                    meanEmbedding[i] = sum / seqLen
                }

                // L2 Normalization
                var sumSquared = 0f
                for (v in meanEmbedding) {
                    sumSquared += v * v
                }
                val magnitude = kotlin.math.sqrt(sumSquared)
                if (magnitude > 0) {
                    for (i in meanEmbedding.indices) {
                        meanEmbedding[i] /= magnitude
                    }
                }

                return meanEmbedding
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    fun close() {
        session.close()
        env.close()
    }
}
