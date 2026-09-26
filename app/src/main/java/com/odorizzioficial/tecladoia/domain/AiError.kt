package com.odorizzioficial.tecladoia.domain

import android.content.Context
import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/**
 * Erros estruturados. A mensagem e um recurso resolvido no idioma do app pelo
 * chamador ([message]), nunca um texto fixo: o mesmo erro aparece na barra
 * sobre o teclado e nas telas, e os dois precisam respeitar a escolha de
 * idioma. [retryable] diz se faz sentido oferecer "Tentar novamente".
 */
sealed class AiError(
    @StringRes val messageRes: Int,
    val retryable: Boolean = false,
    val args: List<Any> = emptyList()
) {

    fun message(context: Context): String =
        if (args.isEmpty()) {
            context.getString(messageRes)
        } else {
            context.getString(messageRes, *args.toTypedArray())
        }

    // --- Configuracao ----------------------------------------------------
    data object MissingApiKey : AiError(R.string.err_missing_key)
    data object InvalidApiKey : AiError(R.string.err_invalid_key)
    data object ApiKeyForbidden : AiError(R.string.err_key_forbidden)

    data class ModelUnavailable(val model: String) :
        AiError(R.string.err_model_unavailable, args = listOf(model))

    data object QuotaExceeded : AiError(R.string.err_quota)
    data object RateLimited : AiError(R.string.err_rate_limited, retryable = true)

    // --- Rede ------------------------------------------------------------
    data object NoConnection : AiError(R.string.err_no_connection, retryable = true)
    data object NetworkFailure : AiError(R.string.err_network_failure, retryable = true)
    data object Timeout : AiError(R.string.err_timeout, retryable = true)

    // --- Servidor da Gemini ----------------------------------------------
    data object ServiceUnavailable : AiError(R.string.err_service_unavailable, retryable = true)

    data class BadRequest(val detail: String) :
        AiError(R.string.err_bad_request, args = listOf(detail))

    // --- Resposta --------------------------------------------------------
    data object EmptyResponse : AiError(R.string.err_empty_response, retryable = true)
    data object UnreadableResponse : AiError(R.string.err_unreadable, retryable = true)

    data class Blocked(val reason: String) :
        AiError(R.string.err_blocked, args = listOf(reason))

    data object NoTextModels : AiError(R.string.err_no_text_models)

    // --- Campo de texto e voz --------------------------------------------
    data object EmptyInput : AiError(R.string.err_empty_input)
    data object FieldNotReadable : AiError(R.string.err_field_not_readable)
    data object FieldNotEditable : AiError(R.string.err_field_not_editable)
    data object VoiceUnavailable : AiError(R.string.err_voice_unavailable)
    data object MicPermissionDenied : AiError(R.string.err_mic_permission)

    data class VoiceFailed(@StringRes val detailRes: Int) : AiError(detailRes)

    // --- Fallback --------------------------------------------------------
    data class Unknown(val detail: String) :
        AiError(R.string.err_unknown, retryable = true, args = listOf(detail))

    companion object {
        /**
         * Traduz o status HTTP e o texto de erro da API para uma causa
         * conhecida. O corpo da resposta da Gemini traz um campo "status"
         * (PERMISSION_DENIED, RESOURCE_EXHAUSTED...) mais preciso que o codigo.
         */
        fun fromHttp(code: Int, apiStatus: String, detail: String, model: String): AiError {
            val status = apiStatus.uppercase()
            val text = detail.lowercase()
            return when {
                status == "RESOURCE_EXHAUSTED" || code == 429 ->
                    if (text.contains("quota")) QuotaExceeded else RateLimited

                text.contains("api key not valid") ||
                    text.contains("api_key_invalid") ||
                    code == 401 -> InvalidApiKey

                status == "PERMISSION_DENIED" || code == 403 ->
                    if (text.contains("api key")) InvalidApiKey else ApiKeyForbidden

                status == "NOT_FOUND" || code == 404 -> ModelUnavailable(model)
                status == "UNAVAILABLE" || code == 503 -> ServiceUnavailable
                code == 504 -> Timeout
                code in 500..599 -> ServiceUnavailable

                status == "INVALID_ARGUMENT" || code == 400 ->
                    if (text.contains("model")) ModelUnavailable(model) else BadRequest(detail)

                else -> Unknown("HTTP $code${if (detail.isBlank()) "" else " - $detail"}")
            }
        }
    }
}

/** Resultado de qualquer operacao de IA ou de manipulacao de campo de texto. */
sealed interface AiResult<out T> {
    data class Success<T>(val value: T) : AiResult<T>
    data class Failure(val error: AiError) : AiResult<Nothing>
}
