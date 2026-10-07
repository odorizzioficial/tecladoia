package com.odorizzioficial.tecladoia.ai.offline

import android.util.Log
import com.llamatik.library.platform.LlamaBridge
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Roda arquivos .gguf com o llama.cpp (biblioteca Llamatik). Quem chama ([OfflineLlm])
 * garante que um pedido por vez chega aqui, ja que a biblioteca guarda um unico
 * modelo carregado por processo.
 */
internal class GgufEngine {

    private var loadedPath: String? = null

    /** Gera a resposta. Falhas da biblioteca nativa viram [AiResult.Failure]. */
    suspend fun generate(call: OfflineCall): AiResult<String> {
        try {
            val params = Params(
                temperature = call.temperature.coerceIn(0f, 2f),
                maxTokens = call.maxOutputTokens
            )
            val loaded = withContext(Dispatchers.Default) { ensureLoaded(call.modelPath, params) }
            if (!loaded) {
                release()
                return AiResult.Failure(AiError.OfflineLoadFailed(UNSUPPORTED_MODEL))
            }

            val prompt = withContext(Dispatchers.Default) {
                buildPrompt(call.systemInstruction, call.userText)
            }
            val raw = withContext(Dispatchers.Default) { respond(prompt) }
            val clean = clean(raw)
            return if (clean.isBlank()) {
                AiResult.Failure(AiError.EmptyResponse)
            } else {
                AiResult.Success(clean)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: GenerationTimeout) {
            return AiResult.Failure(AiError.OfflineTimeout)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha no motor GGUF", t)
            release()
            val detail = (t.message ?: t.javaClass.simpleName).lineSequence().first().take(MAX_DETAIL)
            return AiResult.Failure(AiError.OfflineFailed(detail))
        }
    }

    fun release() {
        loadedPath = null
        runCatching { LlamaBridge.shutdown() }
    }

    // --- Carregar -----------------------------------------------------------

    private data class Params(val temperature: Float, val maxTokens: Int)

    /**
     * Os parametros de carga (contexto, threads, mmap) precisam ser definidos
     * antes do modelo abrir; temperatura e limite de saida valem a cada pedido.
     */
    private fun ensureLoaded(path: String, params: Params): Boolean {
        applyParams(params)
        if (loadedPath == path) return true
        release()
        applyParams(params)
        val ok = LlamaBridge.initGenerateModel(path)
        if (ok) loadedPath = path
        return ok
    }

    private fun applyParams(params: Params) {
        LlamaBridge.updateGenerateParams(
            temperature = params.temperature,
            maxTokens = params.maxTokens,
            topP = TOP_P,
            topK = TOP_K,
            repeatPenalty = REPEAT_PENALTY,
            contextLength = CONTEXT_TOKENS,
            numThreads = threadCount(),
            useMmap = true,
            flashAttention = false,
            batchSize = BATCH_SIZE,
            gpuLayers = 0
        )
    }

    /** Em celular com nucleos grandes e pequenos, passar de 4 threads costuma piorar. */
    private fun threadCount(): Int = Runtime.getRuntime().availableProcessors().coerceIn(2, MAX_THREADS)

    // --- Pedir --------------------------------------------------------------

    /**
     * Usa o modelo de conversa que vem dentro do proprio arquivo .gguf. Se o
     * arquivo nao trouxer um, cai num formato de instrucao generico.
     */
    private fun buildPrompt(system: String, user: String): String {
        val templated: String? = runCatching {
            LlamaBridge.applyChatTemplate(
                messages = listOf("system" to system, "user" to user),
                addAssistantPrefix = true
            )
        }.getOrNull()
        if (templated != null && templated.isNotBlank()) return templated
        return "### Instruction:\n$system\n\n### Input:\n$user\n\n### Response:\n"
    }

    private suspend fun respond(prompt: String): String = coroutineScope {
        val timedOut = AtomicBoolean(false)
        // generate() bloqueia a thread; o vigia cancela a geracao se passar do prazo.
        val watchdog = launch {
            delay(GENERATION_TIMEOUT_MS)
            timedOut.set(true)
            runCatching { LlamaBridge.nativeCancelGenerate() }
        }
        try {
            val text: String = LlamaBridge.generate(prompt)
            if (timedOut.get()) throw GenerationTimeout()
            text
        } finally {
            watchdog.cancel()
        }
    }

    /** Tira marcas de fim de turno que alguns modelos deixam escapar para o texto. */
    private fun clean(raw: String): String {
        var text = raw
        STOP_MARKERS.forEach { marker ->
            val index = text.indexOf(marker)
            if (index >= 0) text = text.substring(0, index)
        }
        text = text.replace(THINK_BLOCK, "")
        val open = text.indexOf("<think>")
        if (open >= 0) text = text.substring(0, open)
        return text.trim().removeSurrounding("\"").trim()
    }

    private class GenerationTimeout : Exception()

    private companion object {
        const val TAG = "GgufEngine"
        const val TOP_P = 0.9f
        const val TOP_K = 40
        const val REPEAT_PENALTY = 1.1f
        const val CONTEXT_TOKENS = 4096
        const val BATCH_SIZE = 512
        const val MAX_THREADS = 4
        const val GENERATION_TIMEOUT_MS = 180_000L
        const val MAX_DETAIL = 160
        const val UNSUPPORTED_MODEL = "arquitetura nao suportada ou arquivo corrompido"
        val STOP_MARKERS = listOf("<end_of_turn>", "<|im_end|>", "<|eot_id|>", "</s>", "<|endoftext|>")
        val THINK_BLOCK = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
    }
}
