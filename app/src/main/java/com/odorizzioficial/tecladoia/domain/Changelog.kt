package com.odorizzioficial.tecladoia.domain

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/** Novidades de uma ou mais versoes, com textos localizados. */
data class ReleaseNotes(
    val version: String,
    @StringRes val dateRes: Int,
    @StringRes val highlights: List<Int>,
    /** Quando preenchido, o titulo diz "tudo o que mudou desde a versao X" em vez de uma versao so. */
    val sinceVersion: String? = null
)

/**
 * Novidades mostradas na primeira abertura depois de atualizar (logo apos as
 * permissoes). Ficam numa lista so, reunindo tudo desde a versao [SINCE]: quem
 * atualizou pula varias versoes e le tudo de uma vez, sem cartao por versao.
 */
object Changelog {

    /** Versao atual: comparada com a ultima vista para abrir as novidades. */
    const val CURRENT = "1.4.0"

    /** Primeira versao coberta pela lista de novidades. */
    const val SINCE = "1.2.1"

    val ALL = listOf(
        ReleaseNotes(
            version = CURRENT,
            dateRes = R.string.news_date_oct_2026,
            sinceVersion = SINCE,
            highlights = listOf(
                R.string.news_all_1,
                R.string.news_all_2,
                R.string.news_all_3,
                R.string.news_all_4,
                R.string.news_all_5,
                R.string.news_all_6,
                R.string.news_all_7,
                R.string.news_all_8,
                R.string.news_all_9,
                R.string.news_all_10,
                R.string.news_all_11,
                R.string.news_all_12,
                R.string.news_all_13,
                R.string.news_all_14,
                R.string.news_all_15,
                R.string.news_all_16
            )
        )
    )
}
