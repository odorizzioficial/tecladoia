package com.odorizzioficial.tecladoia.ai.offline

import android.content.Context
import android.util.Log
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/** Pedido de IA para o modelo que roda no aparelho. */
data class OfflineCall(
    val modelPath: String,
    val systemInstruction: String,
    val userText: String,
    val temperature: Float,
    val maxOutputTokens: Int
)

/**
 * Roda um modelo no proprio aparelho: .litertlm com o motor LiteRT-LM da Google,
 * .gguf com o llama.cpp. O motor e escolhido pelo formato do arquivo, e so um
 * modelo fica na memoria de cada vez. Nenhum texto sai do celular e nao precisa
 * de internet.
 *
 * Carregar um modelo leva alguns segundos e ocupa bastante memoria, por isso o
 * motor so e criado no primeiro pedido, reaproveitado nos seguintes e devolvido
 * ao sistema depois de alguns minutos sem uso. Os pedidos entram um de cada vez.
 * Qualquer falha da biblioteca nativa vira um aviso: nunca derruba o app.
 */
class OfflineLlm(context: Context) {

    private val appContext = context.applicationContext
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val gguf = GgufEngine()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var idleJob: Job? = null

    suspend fun generate(call: OfflineCall): AiResult<String> = mutex.withLock {
        idleJob?.cancel()
        if (call.modelPath.endsWith(ModelFormat.GGUF.extension, ignoreCase = true)) {
            return@withLock try {
                // Um modelo por vez na memoria: solta o do outro motor antes.
                releaseEngine()
                gguf.generate(call)
            } finally {
                scheduleIdleRelease()
            }
        }
        gguf.release()
        try {
            val ready = try {
                ensureEngine(call.modelPath)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao carregar o modelo offline", t)
                releaseEngine()
                return@withLock AiResult.Failure(AiError.OfflineLoadFailed(brief(t)))
            }

            val raw = try {
                respond(ready, call)
            } catch (e: CancellationException) {
                throw e
            } catch (e: GenerationTimeout) {
                return@withLock AiResult.Failure(AiError.OfflineTimeout)
            } catch (t: Throwable) {
                Log.e(TAG, "Falha ao gerar a resposta offline", t)
                releaseEngine()
                return@withLock AiResult.Failure(AiError.OfflineFailed(brief(t)))
            }

            val clean = clean(raw)
            if (clean.isBlank()) AiResult.Failure(AiError.EmptyResponse) else AiResult.Success(clean)
        } finally {
            scheduleIdleRelease()
        }
    }

    /** Libera a memoria do modelo (ao trocar ou apagar o arquivo). */
    suspend fun release() {
        mutex.withLock {
            idleJob?.cancel()
            releaseEngine()
            gguf.release()
        }
    }

    // --- Motor ------------------------------------------------------------

    private suspend fun ensureEngine(path: String): Engine = withContext(Dispatchers.Default) {
        val current = engine
        if (current != null && loadedPath == path && current.isInitialized()) {
            return@withContext current
        }
        releaseEngine()
        if (!File(path).isFile) throw IllegalStateException("arquivo do modelo nao encontrado")

        val cacheDir = File(appContext.cacheDir, "litertlm").apply { mkdirs() }.path
        val opened = try {
            open(path, Backend.CPU(), cacheDir)
        } catch (cpuError: Throwable) {
            // Alguns modelos so rodam na GPU; so entao a GPU e tentada.
            if (!mentionsGpu(cpuError)) throw cpuError
            open(path, Backend.GPU(), cacheDir)
        }
        engine = opened
        loadedPath = path
        opened
    }

    private fun open(path: String, backend: Backend, cacheDir: String): Engine {
        val created = Engine(EngineConfig(modelPath = path, backend = backend, cacheDir = cacheDir))
        try {
            created.initialize()
        } catch (t: Throwable) {
            runCatching { created.close() }
            throw t
        }
        return created
    }

    private fun mentionsGpu(t: Throwable): Boolean {
        val text = (t.message ?: "").lowercase()
        return text.contains("gpu") || text.contains("backend")
    }

    private fun releaseEngine() {
        runCatching { engine?.close() }
        engine = null
        loadedPath = null
    }

    private fun scheduleIdleRelease() {
        idleJob?.cancel()
        idleJob = scope.launch {
            delay(IDLE_RELEASE_MS)
            mutex.withLock {
                releaseEngine()
                gguf.release()
            }
        }
    }

    // --- Resposta ---------------------------------------------------------

    private suspend fun respond(engine: Engine, call: OfflineCall): String = coroutineScope {
        val config = ConversationConfig(
            systemInstruction = Contents.of(call.systemInstruction),
            samplerConfig = SamplerConfig(
                topK = TOP_K,
                topP = TOP_P,
                temperature = call.temperature.toDouble().coerceIn(0.0, 2.0)
            ),
            // Modelos que "pensam" em voz alta (Qwen3, Gemma 4) so devem responder.
            extraContext = mapOf("enable_thinking" to false),
            maxOutputToken = call.maxOutputTokens
        )
        val conversation = engine.createConversation(config)
        try {
            val timedOut = AtomicBoolean(false)
            // sendMessage bloqueia a thread; o vigia cancela a geracao se passar do prazo.
            val watchdog = launch {
                delay(GENERATION_TIMEOUT_MS)
                timedOut.set(true)
                runCatching { conversation.cancelProcess() }
            }
            try {
                val message = withContext(Dispatchers.Default) {
                    conversation.sendMessage(call.userText)
                }
                if (timedOut.get()) throw GenerationTimeout()
                message.toString()
            } finally {
                watchdog.cancel()
            }
        } finally {
            runCatching { conversation.close() }
        }
    }

    /** Tira o raciocinio interno (<think>...</think>) e aspas que sobram em volta. */
    private fun clean(raw: String): String {
        var text = raw.replace(THINK_BLOCK, "")
        val open = text.indexOf("<think>")
        if (open >= 0) text = text.substring(0, open)
        return text.trim().removeSurrounding("\"").trim()
    }

    private fun brief(t: Throwable): String {
        val first = (t.message ?: "").lineSequence().firstOrNull { it.isNotBlank() }?.trim()
        return (first ?: t.javaClass.simpleName).take(MAX_DETAIL)
    }

    private class GenerationTimeout : Exception()

    private companion object {
        const val TAG = "OfflineLlm"
        const val TOP_K = 40
        const val TOP_P = 0.95
        const val IDLE_RELEASE_MS = 3 * 60_000L
        const val GENERATION_TIMEOUT_MS = 120_000L
        const val MAX_DETAIL = 160
        val THINK_BLOCK = Regex("<think>.*?</think>", RegexOption.DOT_MATCHES_ALL)
    }
}
