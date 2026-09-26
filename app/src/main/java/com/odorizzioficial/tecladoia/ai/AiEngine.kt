package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.data.SettingsRepository
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.domain.Tone

/**
 * Junta preferencias do usuario + biblioteca de prompts + [GeminiService].
 * Nenhum texto sai do aparelho sem que o chamador (barra ou tela) tenha
 * recebido um toque explicito do usuario.
 */
class AiEngine(
    private val settings: SettingsRepository,
    private val gemini: GeminiService
) {

    suspend fun run(
        action: AiAction,
        text: String,
        tone: Tone = Tone.PROFESSIONAL,
        instruction: String = "",
        translateTargetOverride: String? = null
    ): AiResult<String> {
        val snapshot = settings.snapshot()
        val apiKey = settings.apiKey()
        if (apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        if (text.isBlank()) return AiResult.Failure(AiError.EmptyInput)

        val system = PromptLibrary.forAction(
            action = action,
            language = snapshot.language,
            translateTarget = translateTargetOverride ?: snapshot.translateTarget,
            tone = tone,
            instruction = instruction
        )

        return gemini.generate(
            GeminiCall(
                apiKey = apiKey,
                model = snapshot.model,
                systemInstruction = system,
                userText = text,
                temperature = snapshot.temperature,
                language = snapshot.language
            )
        )
    }

    suspend fun runCustom(prompt: CustomPrompt, text: String): AiResult<String> {
        val snapshot = settings.snapshot()
        val apiKey = settings.apiKey()
        if (apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)
        if (text.isBlank()) return AiResult.Failure(AiError.EmptyInput)

        return gemini.generate(
            GeminiCall(
                apiKey = apiKey,
                model = snapshot.model,
                systemInstruction = PromptLibrary.rewrite(prompt.prompt),
                userText = text,
                temperature = snapshot.temperature,
                language = snapshot.language
            )
        )
    }

    /**
     * Corrige gramatica, acentuacao e pontuacao de uma transcricao de voz antes
     * de inserir no campo. O erro sobe para quem chamou: a barra ainda insere o
     * texto cru, mas o usuario fica sabendo que a correcao nao rodou.
     */
    suspend fun polishTranscript(text: String): AiResult<String> {
        val snapshot = settings.snapshot()
        val apiKey = settings.apiKey()
        if (text.isBlank()) return AiResult.Success(text)
        if (apiKey.isBlank()) return AiResult.Failure(AiError.MissingApiKey)

        return when (
            val result = gemini.generate(
                GeminiCall(
                    apiKey = apiKey,
                    model = snapshot.model,
                    systemInstruction = PromptLibrary.voicePolish(snapshot.language),
                    userText = text,
                    temperature = 0.2f,
                    language = snapshot.language
                )
            )
        ) {
            is AiResult.Success -> result
            is AiResult.Failure -> result
        }
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
}
