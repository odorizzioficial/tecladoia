package com.odorizzioficial.tecladoia.domain

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/**
 * Tons oferecidos pelo atalho "Mudar Tom".
 *
 * [labelRes] e [descriptionRes] sao o que o usuario le, traduzidos. Ja
 * [promptName] e [promptHint] entram no prompt enviado a IA e ficam fixos: sao
 * instrucao de modelo, nao texto de interface (o idioma da resposta e definido
 * separadamente nos ajustes).
 */
enum class Tone(
    @StringRes val labelRes: Int,
    @StringRes val descriptionRes: Int,
    val promptName: String,
    val promptHint: String
) {
    PROFESSIONAL(
        R.string.tone_professional,
        R.string.tone_professional_desc,
        "professional",
        "corporate objectivity, no slang, clear deadlines"
    ),
    FRIENDLY(
        R.string.tone_friendly,
        R.string.tone_friendly_desc,
        "friendly",
        "warm and approachable, without losing the information"
    ),
    CASUAL(
        R.string.tone_casual,
        R.string.tone_casual_desc,
        "casual",
        "light and conversational, like everyday talk"
    ),
    FORMAL(
        R.string.tone_formal,
        R.string.tone_formal_desc,
        "formal",
        "ceremonious address and careful structure"
    ),
    ROMANTIC(
        R.string.tone_romantic,
        R.string.tone_romantic_desc,
        "romantic",
        "affection and gentleness without overdoing it"
    ),
    DIRECT(
        R.string.tone_direct,
        R.string.tone_direct_desc,
        "direct",
        "no beating around the bush, to the point in few words"
    ),
    POLITE(
        R.string.tone_polite,
        R.string.tone_polite_desc,
        "polite",
        "requests and refusals made with courtesy"
    ),
    CREATIVE(
        R.string.tone_creative,
        R.string.tone_creative_desc,
        "creative",
        "personality and imagery, without changing the meaning"
    )
}
