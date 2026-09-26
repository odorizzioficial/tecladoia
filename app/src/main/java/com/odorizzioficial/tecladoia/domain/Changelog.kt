package com.odorizzioficial.tecladoia.domain

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/** Uma versao do app e o que mudou nela, com textos localizados. */
data class ReleaseNotes(
    val version: String,
    @StringRes val dateRes: Int,
    @StringRes val highlights: List<Int>
)

/**
 * Historico de novidades mostrado na primeira abertura depois de atualizar e
 * disponivel em Ajustes > Sobre > Novidades.
 */
object Changelog {

    /** Versao atual: comparada com a ultima vista para abrir as novidades. */
    const val CURRENT = "1.2.0"

    val ALL = listOf(
        ReleaseNotes(
            version = "1.2.0",
            dateRes = R.string.news_date_sep_2026,
            highlights = listOf(
                R.string.cl_120_1,
                R.string.cl_120_2,
                R.string.cl_120_3,
                R.string.cl_120_4,
                R.string.cl_120_5,
                R.string.cl_120_6
            )
        ),
        ReleaseNotes(
            version = "1.1.0",
            dateRes = R.string.news_date_sep_2026,
            highlights = listOf(
                R.string.cl_110_1,
                R.string.cl_110_2,
                R.string.cl_110_3,
                R.string.cl_110_4,
                R.string.cl_110_5,
                R.string.cl_110_6
            )
        ),
        ReleaseNotes(
            version = "1.0.0",
            dateRes = R.string.news_date_sep_2026,
            highlights = listOf(
                R.string.cl_100_1,
                R.string.cl_100_2,
                R.string.cl_100_3,
                R.string.cl_100_4
            )
        )
    )
}
