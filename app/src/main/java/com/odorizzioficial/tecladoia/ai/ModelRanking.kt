package com.odorizzioficial.tecladoia.ai

import com.odorizzioficial.tecladoia.domain.GeminiModels

/**
 * Escolhe e ordena modelos a partir da lista que a propria API devolve para a
 * chave do usuario. Nao existe catalogo fixo aqui: so regras de nome, que
 * continuam valendo quando o Google lanca versoes novas.
 *
 * Para reescrever texto curto o melhor candidato e um modelo estavel da
 * familia "flash-lite" (rapido, barato e com a maior cota gratuita), depois o
 * "flash". Previews e aliases "-latest" ficam para depois: o Google troca o
 * que esta por tras deles e eles tem limites mais apertados.
 */
object ModelRanking {

    /**
     * Itens que a API lista com generateContent mas que nao servem para este
     * app: Gemma recusa a instrucao de sistema (erro 400), e os demais geram
     * audio, imagem, video ou fazem outra tarefa.
     */
    private val UNSUITABLE = listOf(
        "embedding", "aqa", "tts", "image", "imagen", "veo", "lyria", "live",
        "gemma", "robotics", "computer-use", "deep-research", "nano-banana",
        "native-audio", "omni", "learnlm", "customtools", "-exp"
    )

    private val VERSION = Regex("gemini-(\\d+(?:\\.\\d+)?)")

    fun isSuitable(id: String): Boolean =
        !GeminiModels.isRetired(id) && UNSUITABLE.none { id.contains(it, ignoreCase = true) }

    /** Estavel = nao e preview, experimental nem alias que o Google troca. */
    fun isStable(id: String): Boolean =
        !id.contains("preview", ignoreCase = true) &&
            !id.contains("exp", ignoreCase = true) &&
            !id.contains("latest", ignoreCase = true)

    private fun isLite(id: String) = id.contains("flash-lite", ignoreCase = true)
    private fun isFlash(id: String) = id.contains("flash", ignoreCase = true)

    /** Quanto menor, melhor. */
    private fun tier(id: String): Int = when {
        isStable(id) && isLite(id) -> 0
        isStable(id) && isFlash(id) -> 1
        id.contains("latest", ignoreCase = true) && isLite(id) -> 2
        id.contains("latest", ignoreCase = true) && isFlash(id) -> 3
        isFlash(id) -> 4
        isStable(id) -> 5
        else -> 6
    }

    private fun version(id: String): Double =
        VERSION.find(id)?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0

    /** Modelos utilizaveis, do mais indicado para o menos indicado. */
    fun rank(ids: List<String>): List<String> = ids
        .filter { isSuitable(it) }
        .distinct()
        .sortedWith(compareBy<String>({ tier(it) }, { -version(it) }, { it }))

    /** Melhor escolha da lista, sem inventar nome. */
    fun best(ids: List<String>): String? = rank(ids).firstOrNull() ?: ids.firstOrNull()
}
