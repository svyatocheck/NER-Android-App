package com.svyatoslav.mlapp.core.models

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.svyatoslav.mlapp.core.INerProcessing
import com.svyatoslav.mlapp.core.IWordPieceTokenizer
import com.svyatoslav.mlapp.data.model.FeatureModel
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.collections.component1
import kotlin.collections.component2

/**
 * Class responsible for performing NER (Named Entity Recognition) inference on-device using a TFLite model.
 * It tokenizes input text, runs inference, and reconstructs the output with masked PII labels.
 */
class NERPreprocessing(
    context: Context,
    private val tokenizer: IWordPieceTokenizer
) : INerProcessing {

    override val id2label: Map<Int, String>
    private val interpreter: Interpreter

    companion object {
        const val MAX_QUERY_LEN = 64         // Maximum number of tokens in the optional query
        const val MAX_SEQ_LEN = 128          // Total model input length (including special tokens)
        const val PAD_ID = 0                 // Padding token ID
        private const val TAG = "NERDataSource"
    }

    init {
        // Load label mapping (id2label and label2id) from config.json in assets
        val stream = context.assets.open("config.json")
        val json = stream.bufferedReader().use { it.readText() }
        val root = JSONObject(json)
        id2label = labelMapper(root)

        // Load the TFLite model from assets and prepare the interpreter
        val modelBuffer = FileUtil.loadMappedFile(context, "pii_128_fp32.tflite")
        val options = Interpreter.Options().setNumThreads(4)
        interpreter = Interpreter(modelBuffer, options)
        interpreter.allocateTensors()

        logModelShapes() // Optional: logs input/output tensor shapes
    }

    /**
     * Runs inference and returns the final masked string along with latency.
     */
    override fun infer(inputText: String): Pair<String, Long> {
        // Convert input text to feature model for the NER
        val feature = tokenizer.convert(null, inputText)

        // Extract and truncate model input features
        val inputIds = feature.inputIds.take(MAX_SEQ_LEN).toIntArray()
        val attentionMask = feature.inputMask.take(MAX_SEQ_LEN).toIntArray()
        val tokenTypeIds = feature.segmentIds.take(MAX_SEQ_LEN).toIntArray()

        // Map tensor names to their respective indices
        val inputIndexMap = (0 until interpreter.inputTensorCount).associateBy {
            interpreter.getInputTensor(it).name()
        }

        // Prepare inputs as ByteBuffers
        val inputMap = mapOf(
            "serving_default_input_ids:0" to toInputBuffer(inputIds),
            "serving_default_attention_mask:0" to toInputBuffer(attentionMask),
            "serving_default_token_type_ids:0" to toInputBuffer(tokenTypeIds)
        )

        // Match tensor buffers to correct input indices
        val inputs = Array<Any?>(interpreter.inputTensorCount) { null }
        for ((name, buffer) in inputMap) {
            val index = inputIndexMap[name] ?: error("Tensor $name not found in model")
            inputs[index] = buffer
        }

        // Prepare output buffer
        val outputTensor = interpreter.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
        val outputMap = mapOf(0 to outputBuffer.buffer)

        // Run inference and measure latency
        val t0 = SystemClock.uptimeMillis()
        interpreter.runForMultipleInputsOutputs(inputs, outputMap)
        val latency = SystemClock.uptimeMillis() - t0

        // Postprocess the output tensor to extract tag predictions
        val fb = outputBuffer.floatArray
        val numLabels = outputTensor.shape()[2]

        val tags = IntArray(inputIds.size) { tok ->
            var best = 0
            var bestVal = Float.NEGATIVE_INFINITY
            for (lbl in 0 until numLabels) {
                val v = fb[tok * numLabels + lbl]
                if (v > bestVal) {
                    bestVal = v
                    best = lbl
                }
            }
            best
        }

        // Reconstruct the original sentence with masked PII tokens
        val maskedText = rebuildTextWithPII(feature.origTokens, feature.tokenToOrigMap, tags)
        return Pair(maskedText, latency)
    }

    /**
     * Variant of [infer] that also returns raw NER tags for downstream use.
     */
    override fun predictTags(inputText: String): Triple<FeatureModel, IntArray, Long> {
        val feature = tokenizer.convert(null, inputText)
        val inputIds = feature.inputIds.take(MAX_SEQ_LEN).toIntArray()
        val attentionMask = feature.inputMask.take(MAX_SEQ_LEN).toIntArray()
        val tokenTypeIds = feature.segmentIds.take(MAX_SEQ_LEN).toIntArray()

        val inputIndexMap = (0 until interpreter.inputTensorCount).associateBy {
            interpreter.getInputTensor(it).name()
        }

        val inputMap = mapOf(
            "serving_default_input_ids:0" to toInputBuffer(inputIds),
            "serving_default_attention_mask:0" to toInputBuffer(attentionMask),
            "serving_default_token_type_ids:0" to toInputBuffer(tokenTypeIds)
        )

        val inputs = Array<Any?>(interpreter.inputTensorCount) { null }
        for ((name, buffer) in inputMap) {
            val index = inputIndexMap[name] ?: error("Tensor $name not found in model")
            inputs[index] = buffer
        }

        val outputTensor = interpreter.getOutputTensor(0)
        val outputBuffer = TensorBuffer.createFixedSize(outputTensor.shape(), outputTensor.dataType())
        val outputMap = mapOf(0 to outputBuffer.buffer)

        val t0 = SystemClock.uptimeMillis()
        interpreter.runForMultipleInputsOutputs(inputs, outputMap)
        val latency = SystemClock.uptimeMillis() - t0

        val fb = outputBuffer.floatArray
        val numLabels = outputTensor.shape()[2]

        val tags = IntArray(inputIds.size) { tok ->
            var best = 0
            var bestVal = Float.NEGATIVE_INFINITY
            for (lbl in 0 until numLabels) {
                val v = fb[tok * numLabels + lbl]
                if (v > bestVal) {
                    bestVal = v
                    best = lbl
                }
            }
            best
        }

        return Triple(feature, tags, latency)
    }

    /**
     * Reconstructs original text, replacing PII entities with standardized labels.
     */
    private fun rebuildTextWithPII(
        origTokens: List<String>,
        tokenToOrigMap: Map<Int, Int>,
        tags: IntArray,
    ): String {
        val labelReplacements = mapOf(
            "FIRST_NAME" to "[GIVENNAME]",
            "MIDDLE_NAME" to "[GIVENNAME]",
            "LAST_NAME" to "[LASTNAME]",
            "EMAIL" to "[EMAIL]",
            "PHONE" to "[PHONENUMBER]",
            "ID" to "[ID]",
            "IBAN" to "[IBAN]",
            "CRYPTO" to "[CRYPTO]",
            "CITY" to "[LOCATION]",
            "USERNAME" to "[USERNAME]",
            "ACCOUNTNUMBER" to "[MASKEDNUMBER]"
        )

        val sb = StringBuilder()
        var prevOrigIdx = -1

        for ((tokenIdx, origIdx) in tokenToOrigMap) {
            val tagId = tags.getOrElse(tokenIdx) { 0 }
            val label = id2label[tagId] ?: "O"
            val isPII = label.startsWith("B-") || label.startsWith("I-")

            if (isPII && origIdx != prevOrigIdx) {
                val base = label.removePrefix("B-").removePrefix("I-").uppercase()
                val repl = labelReplacements[base] ?: "[$base]"
                if (sb.isNotEmpty() && !sb.last().isWhitespace()) sb.append(' ')
                sb.append(repl)
            } else if (!isPII && origIdx != prevOrigIdx) {
                val token = origTokens.getOrNull(origIdx)?.trim() ?: continue
                if (token.isNotEmpty()) {
                    if (sb.isNotEmpty() && !sb.last().isWhitespace()) sb.append(' ')
                    sb.append(token)
                }
            }
            prevOrigIdx = origIdx
        }
        return sb.toString().trimEnd()
    }

    /**
     * Converts an int array into a ByteBuffer compatible with TFLite.
     */
    private fun toInputBuffer(data: IntArray): ByteBuffer {
        val buffer = ByteBuffer.allocateDirect(1 * data.size * 4).order(ByteOrder.nativeOrder())
        buffer.asIntBuffer().put(data)
        return buffer
    }

    /**
     * Logs input and output tensor shapes for debugging.
     */
    private fun logModelShapes() {
        for (i in 0 until interpreter.inputTensorCount) {
            val shape = interpreter.getInputTensor(i).shape().joinToString()
            Log.i(TAG, "Input #$i: $shape")
        }
        val outShape = interpreter.getOutputTensor(0).shape().joinToString()
        Log.i(TAG, "Output shape: $outShape")
    }

    /**
     * Parses the config.json file and builds id2label and label2id maps.
     */
    private fun labelMapper(root: JSONObject): MutableMap<Int, String> {
        val id2labelObj = root.getJSONObject("id2label")
        val tmpId2Label = mutableMapOf<Int, String>()
        id2labelObj.keys().forEach { key ->
            tmpId2Label[key.toInt()] = id2labelObj.getString(key)
        }

        val label2idObj = root.getJSONObject("label2id")
        val tmpLabel2Id = mutableMapOf<String, Int>()
        label2idObj.keys().forEach { key ->
            tmpLabel2Id[key] = label2idObj.getInt(key)
        }

        val piiIds: Set<Int> = tmpId2Label.filter { (_, label) ->
            label.startsWith("B-") || label.startsWith("I-")
        }.keys.toSet()

        Log.i(TAG, "Loaded ${tmpId2Label.size} labels, ${piiIds.size} PII tags")

        return tmpId2Label
    }
}
