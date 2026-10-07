package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.util.Log
import okhttp3.ConnectionPool
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/** Parametros de uma chamada a IA, independentes da interface. */
data class GeminiCall(
    val apiKey: String,
    val model: String,
    val systemInstruction: String,
    val userText: String,
    val temperature: Float,
    val language: String
)

/**
 * Camada isolada de acesso a API Gemini. Nao conhece Compose, Android View nem
 * o AccessibilityService: recebe texto + instrucao e devolve resultado ou erro
 * estruturado.
 */
class GeminiService(
    private val client: OkHttpClient = OkHttpClient.Builder()
        // Timeouts curtos o suficiente para o erro aparecer rapido, e um pool
        // mantido quente: a conexao TLS e reaproveitada entre acoes, o que
        // economiza handshake em cada toque na barra.
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(3, 5, TimeUnit.MINUTES))
        .retryOnConnectionFailure(true)
        .build()
) {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    /**
     * [quick] encurta a espera pela resposta: usado nas tentativas de reserva,
     * para que um modelo que "trava" nao segure o usuario por muito tempo.
     */
    suspend fun generate(call: GeminiCall, quick: Boolean = false): AiResult<String> =
        generate(call, disableThinking = true, quick = quick)

    /** Mesmo cliente HTTP (pool de conexoes compartilhado), com espera menor. */
    private val quickClient: OkHttpClient by lazy {
        client.newBuilder().readTimeout(QUICK_READ_TIMEOUT_S, TimeUnit.SECONDS).build()
    }

    private suspend fun generate(
        call: GeminiCall,
        disableThinking: Boolean,
        quick: Boolean
    ): AiResult<String> = withContext(Dispatchers.IO) {
        if (call.apiKey.isBlank()) return@withContext AiResult.Failure(AiError.MissingApiKey)
        if (call.userText.isBlank()) return@withContext AiResult.Failure(AiError.EmptyInput)

        val startedAt = System.nanoTime()
        val payload = GeminiRequest(
            contents = listOf(
                Content(role = "user", parts = listOf(Part(text = call.userText)))
            ),
            systemInstruction = Content(parts = listOf(Part(text = call.systemInstruction))),
            generationConfig = GenerationConfig(
                temperature = call.temperature,
                // Teto proporcional ao texto: corrigir uma frase nao precisa do
                // mesmo espaco de saida que resumir um paragrafo longo.
                // Sem o orcamento zero o modelo "pensa" e esses tokens saem da
                // mesma cota de saida: sem folga a resposta voltava vazia.
                maxOutputTokens = outputBudgetFor(call.userText) +
                    if (disableThinking) 0 else THINKING_HEADROOM_TOKENS,
                // Modelos 2.5+ "pensam" antes de responder e isso domina a
                // latencia em tarefas simples de reescrita. O orcamento zero
                // devolve a resposta direta; modelos que nao aceitam o campo
                // caem no retry sem ele, logo abaixo.
                thinkingConfig = if (disableThinking) ThinkingConfig(0) else null
            )
        )

        val request = Request.Builder()
            .url("$BASE_URL/${call.model}:generateContent")
            .addHeader("x-goog-api-key", call.apiKey)
            .addHeader("Content-Type", "application/json")
            .post(json.encodeToString(GeminiRequest.serializer(), payload).toRequestBody(JSON_MEDIA))
            .build()
        val preparedAt = System.nanoTime()

        try {
            (if (quick) quickClient else client).newCall(request).execute().use { response ->
                val respondedAt = System.nanoTime()
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val api = parseApiError(body)
                    // Campo thinkingConfig recusado: repete a chamada sem ele.
                    if (disableThinking && response.code == 400 &&
                        api.second.contains("thinking", ignoreCase = true)
                    ) {
                        return@withContext generate(call, disableThinking = false, quick = quick)
                    }
                    logTiming(call.model, startedAt, preparedAt, respondedAt, response.code)
                    return@withContext AiResult.Failure(
                        AiError.fromHttp(
                            code = response.code,
                            apiStatus = api.first,
                            detail = api.second,
                            model = call.model
                        )
                    )
                }
                val parsed = runCatching { json.decodeFromString(GeminiResponse.serializer(), body) }
                    .getOrElse {
                        return@withContext AiResult.Failure(AiError.UnreadableResponse)
                    }
                logTiming(call.model, startedAt, preparedAt, respondedAt, response.code)

                parsed.promptFeedback?.blockReason?.let { reason ->
                    return@withContext AiResult.Failure(AiError.Blocked(reason))
                }

                val text = parsed.candidates
                    ?.firstOrNull()
                    ?.content
                    ?.parts
                    ?.mapNotNull { it.text }
                    ?.joinToString(separator = "")
                    ?.trim()
                    ?.removeSurrounding("\"")

                if (text.isNullOrBlank()) {
                    val finish = parsed.candidates?.firstOrNull()?.finishReason
                    if (finish != null && finish != "STOP" && finish != "MAX_TOKENS") {
                        return@withContext AiResult.Failure(AiError.Blocked(finish))
                    }
                    return@withContext AiResult.Failure(AiError.EmptyResponse)
                }
                AiResult.Success(text)
            }
        } catch (e: SocketTimeoutException) {
            AiResult.Failure(AiError.Timeout)
        } catch (e: UnknownHostException) {
            AiResult.Failure(AiError.NoConnection)
        } catch (e: IOException) {
            AiResult.Failure(AiError.NetworkFailure)
        } catch (t: Throwable) {
            AiResult.Failure(AiError.Unknown(t.message ?: t.javaClass.simpleName))
        }
    }

    /** Chamada minima usada pelo botao "Testar conexão" da tela de ajustes. */
    suspend fun testConnection(apiKey: String, model: String): AiResult<String> = generate(
        GeminiCall(
            apiKey = apiKey,
            model = model,
            systemInstruction = "Responda exatamente com a palavra OK.",
            userText = "ping",
            temperature = 0f,
            language = "pt-BR"
        )
    )

    /**
     * Pergunta a propria API quais modelos a chave do usuario pode usar com
     * generateContent. Resolve o 404 "modelo indisponivel" sem adivinhacao:
     * o app passa a oferecer exatamente o que aquela chave aceita.
     */
    suspend fun listModels(apiKey: String): AiResult<List<String>> = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext AiResult.Failure(AiError.MissingApiKey)

        val request = Request.Builder()
            .url("$BASE_URL?pageSize=200")
            .addHeader("x-goog-api-key", apiKey)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val api = parseApiError(body)
                    return@withContext AiResult.Failure(
                        AiError.fromHttp(
                            code = response.code,
                            apiStatus = api.first,
                            detail = api.second,
                            model = ""
                        )
                    )
                }
                val parsed = runCatching { json.decodeFromString(ModelList.serializer(), body) }
                    .getOrElse {
                        return@withContext AiResult.Failure(AiError.UnreadableResponse)
                    }
                val ids = parsed.models.orEmpty()
                    .filter { it.supportedGenerationMethods.orEmpty().contains("generateContent") }
                    .mapNotNull { it.name?.removePrefix("models/") }
                    .let { ModelRanking.rank(it) }
                if (ids.isEmpty()) {
                    AiResult.Failure(AiError.NoTextModels)
                } else {
                    AiResult.Success(ids)
                }
            }
        } catch (e: SocketTimeoutException) {
            AiResult.Failure(AiError.Timeout)
        } catch (e: UnknownHostException) {
            AiResult.Failure(AiError.NoConnection)
        } catch (e: IOException) {
            AiResult.Failure(AiError.NoConnection)
        } catch (t: Throwable) {
            AiResult.Failure(AiError.Unknown(t.message ?: t.javaClass.simpleName))
        }
    }

    /** Devolve (status, mensagem) do envelope de erro da API. */
    private fun parseApiError(body: String): Pair<String, String> = runCatching {
        val error = json.decodeFromString(GeminiErrorEnvelope.serializer(), body).error
        (error?.status.orEmpty()) to (error?.message.orEmpty())
    }.getOrDefault("" to "")

    private fun outputBudgetFor(text: String): Int =
        (text.length / 2 + 256).coerceIn(256, 2048)

    /**
     * Log de desenvolvimento com o tempo de cada etapa. A chave nunca entra
     * aqui: so modelo, duracoes e codigo HTTP.
     */
    private fun logTiming(
        model: String,
        startedAt: Long,
        preparedAt: Long,
        respondedAt: Long,
        code: Int
    ) {
        val prepareMs = (preparedAt - startedAt) / 1_000_000
        val networkMs = (respondedAt - preparedAt) / 1_000_000
        val totalMs = (System.nanoTime() - startedAt) / 1_000_000
        Log.d(
            TAG,
            "generateContent model=$model http=$code preparo=${prepareMs}ms " +
                "rede+IA=${networkMs}ms leitura=${totalMs - prepareMs - networkMs}ms " +
                "total=${totalMs}ms"
        )
    }

    private companion object {
        const val TAG = "GeminiService"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
        const val QUICK_READ_TIMEOUT_S = 14L
        const val THINKING_HEADROOM_TOKENS = 3072
        val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}

// --- Modelos de transporte ----------------------------------------------

@Serializable
private data class GeminiRequest(
    val contents: List<Content>,
    @SerialName("systemInstruction") val systemInstruction: Content? = null,
    @SerialName("generationConfig") val generationConfig: GenerationConfig? = null
)

@Serializable
private data class Content(
    val role: String? = null,
    val parts: List<Part>
)

@Serializable
private data class Part(val text: String? = null)

@Serializable
private data class GenerationConfig(
    val temperature: Float,
    @SerialName("maxOutputTokens") val maxOutputTokens: Int,
    @SerialName("thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@Serializable
private data class ThinkingConfig(
    @SerialName("thinkingBudget") val thinkingBudget: Int
)

@Serializable
private data class GeminiResponse(
    val candidates: List<Candidate>? = null,
    @SerialName("promptFeedback") val promptFeedback: PromptFeedback? = null
)

@Serializable
private data class Candidate(
    val content: Content? = null,
    @SerialName("finishReason") val finishReason: String? = null
)

@Serializable
private data class PromptFeedback(
    @SerialName("blockReason") val blockReason: String? = null
)

@Serializable
private data class GeminiErrorEnvelope(val error: ApiError? = null)

@Serializable
private data class ApiError(
    val code: Int? = null,
    val message: String? = null,
    val status: String? = null
)

@Serializable
private data class ModelList(val models: List<ModelInfo>? = null)

@Serializable
private data class ModelInfo(
    val name: String? = null,
    @SerialName("supportedGenerationMethods") val supportedGenerationMethods: List<String>? = null
)
