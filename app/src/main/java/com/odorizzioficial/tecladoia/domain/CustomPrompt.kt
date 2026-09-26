package com.odorizzioficial.tecladoia.domain

import android.content.Context
import com.odorizzioficial.tecladoia.R
import kotlinx.serialization.Serializable

@Serializable
data class CustomPrompt(
    val id: String,
    val name: String,
    val icon: String,
    val prompt: String,
    val enabled: Boolean = true,
    val order: Int = 0,
    /** Quando true, aparece tambem na barra compacta sobre o teclado. */
    val pinned: Boolean = false
) {
    companion object {
        val AVAILABLE_ICONS = listOf("\uD83D\uDC54", "\uD83D\uDE80", "\uD83D\uDCA1", "\u2696\uFE0F", "\uD83C\uDFAF", "\uD83D\uDE02", "\u2728", "\uD83D\uDCDD")

        /**
         * Funcoes iniciais. Elas sao criadas no idioma do app no primeiro uso e,
         * a partir dai, viram dados do usuario: renomear ou traduzir passa a ser
         * decisao dele, entao nao mudam mais quando o idioma muda.
         */
        fun defaults(context: Context): List<CustomPrompt> = listOf(
            CustomPrompt(
                id = "seed-professional",
                name = context.getString(R.string.seed_professional_name),
                icon = "\uD83D\uDC54",
                prompt = context.getString(R.string.seed_professional_prompt),
                order = 0,
                pinned = true
            ),
            CustomPrompt(
                id = "seed-funny",
                name = context.getString(R.string.seed_funny_name),
                icon = "\uD83D\uDE02",
                prompt = context.getString(R.string.seed_funny_prompt),
                order = 1
            ),
            CustomPrompt(
                id = "seed-short",
                name = context.getString(R.string.seed_short_name),
                icon = "\uD83C\uDFAF",
                prompt = context.getString(R.string.seed_short_prompt),
                order = 2
            )
        )
    }
}
