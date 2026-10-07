package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.ai.offline.OfflineCall
import com.odorizzioficial.tecladoia.ai.offline.OfflineLlm
import com.odorizzioficial.tecladoia.ai.offline.OfflineModelStore
import com.odorizzioficial.tecladoia.data.SettingsRepository
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.domain.AiResult
import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.domain.Tone
import kotlinx.coroutines.delay
import java.util.concurrent.ConcurrentHashMap

/**
 * Junta preferencias do usuario + biblioteca de prompts + [GeminiService].
 * Nenhum texto sai do aparelho sem que o chamador (barra ou tela) tenha
 * recebido um toque explicito do usuario.
 *
 * A API do Gemini passa por periodos de alta demanda (erro 503) que atingem
 * um modelo de cada vez. Por isso cada pedido percorre uma curta lista de
 * modelos: o escolhido pelo usuario primeiro e, se ele estiver sobrecarregado,
 * sem cota ou desligado, os proximos mais indicados da propria lista da chave.
 */
class AiEngine(
    private val settings: SettingsRepository,
    private val gemini: GeminiService,
    private val offline: OfflineLlm,
    private val offlineStore: OfflineModelStore
) {

    /** Modelo -> instante ate quando ele fica "de molho" por ter falhado. */
    private val restingUntil = ConcurrentHashMap<String, Long>()

    suspend fun run(
        action: AiAction,
        text: String,
        tone: Tone = Tone.PROFESSIONAL,
        instruction: String = "",
        translateTargetOverride: String? = null
    ): AiResult<String> {
        val snapshot = settings.snapshot()
        val offlineMode = snapshot.aiProvider == AiProvider.OFFLINE
        val apiKey = if (offlineMode) "" else settings.apiKey()
        if (!offlineMode && apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        if (text.isBlank()) return AiResult.Failure(AiError.EmptyInput)

        val system = PromptLibrary.forAction(
            action = action,
            language = snapshot.language,
            translateTarget = translateTargetOverride ?: snapshot.translateTarget,
            tone = tone,
            instruction = instruction,
            text = text
        )
        if (offlineMode) return generateOffline(snapshot, system, text, snapshot.offlineTemperature)

        return generateResilient(
            GeminiCall(
                apiKey = apiKey,
                model = snapshot.model,
                systemInstruction = system,
                userText = text,
                temperature = snapshot.temperature,
                language = snapshot.language
            ),
            snapshot.cachedModels
        )
    }

    suspend fun runCustom(prompt: CustomPrompt, text: String): AiResult<String> {
        val snapshot = settings.snapshot()
        val offlineMode = snapshot.aiProvider == AiProvider.OFFLINE
        val apiKey = if (offlineMode) "" else settings.apiKey()
        if (!offlineMode && apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        if (text.isBlank()) return AiResult.Failure(AiError.EmptyInput)
        if (offlineMode) {
            return generateOffline(
                snapshot, PromptLibrary.rewrite(prompt.prompt, text), text, snapshot.offlineTemperature
            )
        }

        return generateResilient(
            GeminiCall(
                apiKey = apiKey,
                model = snapshot.model,
                systemInstruction = PromptLibrary.rewrite(prompt.prompt, text),
                userText = text,
                temperature = snapshot.temperature,
                language = snapshot.language
            ),
            snapshot.cachedModels
        )
    }

    /**
     * Corrige gramatica, acentuacao e pontuacao de uma transcricao de voz antes
     * de inserir no campo. O erro sobe para quem chamou: a barra ainda insere o
     * texto cru, mas o usuario fica sabendo que a correcao nao rodou.
     */
    suspend fun polishTranscript(text: String): AiResult<String> {
        val snapshot = settings.snapshot()
        if (text.isBlank()) return AiResult.Success(text)
        if (snapshot.aiProvider == AiProvider.OFFLINE) {
            return generateOffline(snapshot, PromptLibrary.voicePolish(snapshot.language, text), text, 0.2f)
        }
        val apiKey = settings.apiKey()
        if (apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)

        return generateResilient(
            GeminiCall(
                apiKey = apiKey,
                model = snapshot.model,
                systemInstruction = PromptLibrary.voicePolish(snapshot.language, text),
                userText = text,
                temperature = 0.2f,
                language = snapshot.language
            ),
            snapshot.cachedModels
        )
    }

    /** Modelos que a chave do usuario realmente aceita em generateContent. */
    suspend fun availableModels(rawKey: String? = null): AiResult<List<String>> {
        val key = rawKey?.takeIf { it.isNotBlank() } ?: settings.apiKey()
        if (key.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        return gemini.listModels(key)
    }

    suspend fun testConnection(rawKey: String? = null): AiResult<String> {
        val snapshot = settings.snapshot()
        val key = rawKey?.takeIf { it.isNotBlank() } ?: settings.apiKey()
        if (key.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        return gemini.testConnection(key, snapshot.model)
    }

    // --- IA offline -------------------------------------------------------

    /**
     * Roda no modelo do aparelho. Se ele falhar, o erro aparece como esta: o
     * texto nunca e enviado ao Gemini por conta propria, porque quem escolheu
     * o modo offline pediu justamente para ele nao sair do celular.
     */
    private suspend fun generateOffline(
        snapshot: AppSettings,
        system: String,
        text: String,
        temperature: Float
    ): AiResult<String> {
        val file = if (snapshot.offlineModel.isBlank()) null else offlineStore.fileFor(snapshot.offlineModel)
        if (file == null || !file.isFile) return AiResult.Failure(AiError.OfflineNoModel)
        return offline.generate(
            OfflineCall(
                modelPath = file.path,
                systemInstruction = system,
                userText = text,
                temperature = temperature,
                maxOutputTokens = (text.length / 2 + 128).coerceIn(128, 1024)
            )
        )
    }

    // --- Resiliencia ------------------------------------------------------

    /**
     * Tenta o modelo escolhido e, se a falha for do modelo ou do momento (e nao
     * da chave ou do texto), os proximos da lista. O total de tentativas e
     * pequeno de proposito: na faixa gratuita um 503 tambem pode gastar cota.
     */
    private suspend fun generateResilient(
        call: GeminiCall,
        cachedModels: List<String>
    ): AiResult<String> {
        val chain = buildChain(call.model, cachedModels)
        // Sem alternativa, vale uma segunda chance rapida no mesmo modelo.
        val attemptsPerModel = if (chain.size == 1) 2 else 1
        var worst: AiError? = null

        for ((index, model) in chain.withIndex()) {
            for (attempt in 1..attemptsPerModel) {
                val result = gemini.generate(call.copy(model = model), quick = index > 0)
                if (result is AiResult.Success) {
                    restingUntil.remove(model)
                    return result
                }
                val error = (result as AiResult.Failure).error
                if (!worthTryingNext(error)) return result

                if (worst == null || severity(error) > severity(worst)) worst = error
                val transient = error is AiError.ServiceUnavailable || error is AiError.RateLimited
                if (attempt < attemptsPerModel && transient) {
                    delay(RETRY_DELAY_MS)
                    continue
                }
                rest(model, error)
                break
            }
        }
        return AiResult.Failure(worst ?: AiError.ServiceUnavailable)
    }

    /** Lista de modelos a tentar, sem repetir e sem os que acabaram de falhar. */
    private fun buildChain(primary: String, cachedModels: List<String>): List<String> {
        val now = System.currentTimeMillis()
        val candidates = LinkedHashSet<String>()
        if (ModelRanking.isSuitable(primary)) candidates += primary
        candidates += ModelRanking.rank(cachedModels).filter { ModelRanking.isStable(it) }.take(3)
        if (cachedModels.isEmpty()) candidates += FALLBACK_ALIASES

        val ready = candidates.filter { (restingUntil[it] ?: 0L) <= now }
        return when {
            ready.isNotEmpty() -> ready.take(MAX_MODELS_PER_ACTION)
            candidates.isNotEmpty() -> listOf(candidates.first())
            else -> listOf(primary)
        }
    }

    /** O proximo modelo pode ajudar? Chave invalida ou texto vazio, nao. */
    private fun worthTryingNext(error: AiError): Boolean = when (error) {
        is AiError.ServiceUnavailable,
        is AiError.RateLimited,
        is AiError.QuotaExceeded,
        is AiError.ModelUnavailable,
        is AiError.Timeout,
        is AiError.NetworkFailure,
        is AiError.EmptyResponse,
        is AiError.UnreadableResponse,
        is AiError.BadRequest,
        is AiError.Unknown -> true

        else -> false
    }

    /** Qual erro mostrar quando todos falham: o mais informativo. */
    private fun severity(error: AiError): Int = when (error) {
        is AiError.ServiceUnavailable -> 6
        is AiError.RateLimited -> 5
        is AiError.QuotaExceeded -> 4
        is AiError.Timeout -> 3
        is AiError.NetworkFailure -> 2
        is AiError.ModelUnavailable -> 1
        else -> 0
    }

    /** Poe o modelo "de molho" para os proximos pedidos nao baterem nele de novo. */
    private fun rest(model: String, error: AiError) {
        val millis = when (error) {
            is AiError.ModelUnavailable, is AiError.BadRequest -> 30 * 60_000L
            is AiError.QuotaExceeded -> 10 * 60_000L
            else -> 90_000L
        }
        restingUntil[model] = System.currentTimeMillis() + millis
    }

    private companion object {
        const val MAX_MODELS_PER_ACTION = 3
        const val RETRY_DELAY_MS = 700L

        /** So usados quando o app ainda nao consultou a lista da chave. */
        val FALLBACK_ALIASES = listOf("gemini-flash-lite-latest", "gemini-flash-latest")
    }
}
