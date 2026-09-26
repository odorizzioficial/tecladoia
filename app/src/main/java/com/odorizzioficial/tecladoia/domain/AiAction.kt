package com.odorizzioficial.tecladoia.domain

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/**
 * Acoes fixas da barra de IA. Os rotulos sao recursos: a barra roda dentro do
 * servico de acessibilidade e resolve o texto no idioma escolhido pelo usuario.
 * Funcoes do usuario chegam via [CustomPrompt] e nunca sao traduzidas.
 */
enum class AiAction(
    @StringRes val labelRes: Int,
    @StringRes val subtitleRes: Int
) {
    FIX(R.string.action_fix, R.string.action_fix_sub),
    IMPROVE(R.string.action_improve, R.string.action_improve_sub),
    TRANSLATE(R.string.action_translate, R.string.action_translate_sub),
    TONE(R.string.action_tone, R.string.action_tone_sub),
    SUMMARIZE(R.string.action_summarize, R.string.action_summarize_sub),
    REWRITE(R.string.action_rewrite, R.string.action_rewrite_sub)
}
