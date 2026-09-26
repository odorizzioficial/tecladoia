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
        "profissional",
        "objetividade corporativa, sem girias, prazos claros"
    ),
    FRIENDLY(
        R.string.tone_friendly,
        R.string.tone_friendly_desc,
        "amigavel",
        "proximo e simpatico, sem perder a informacao"
    ),
    CASUAL(
        R.string.tone_casual,
        R.string.tone_casual_desc,
        "casual",
        "leve e conversado, como no dia a dia"
    ),
    FORMAL(
        R.string.tone_formal,
        R.string.tone_formal_desc,
        "formal",
        "tratamento cerimonioso e estrutura cuidadosa"
    ),
    ROMANTIC(
        R.string.tone_romantic,
        R.string.tone_romantic_desc,
        "romantico",
        "afeto e delicadeza sem exagero"
    ),
    DIRECT(
        R.string.tone_direct,
        R.string.tone_direct_desc,
        "direto",
        "sem rodeios, ao ponto em poucas palavras"
    ),
    POLITE(
        R.string.tone_polite,
        R.string.tone_polite_desc,
        "educado",
        "pedidos e recusas com cortesia"
    ),
    CREATIVE(
        R.string.tone_creative,
        R.string.tone_creative_desc,
        "criativo",
        "personalidade e imagens, sem mudar o sentido"
    )
}
