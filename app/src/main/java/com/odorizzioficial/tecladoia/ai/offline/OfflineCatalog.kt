package com.odorizzioficial.tecladoia.ai.offline

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

/**
 * Modelo pronto para baixar, no Hugging Face, com licenca aberta e sem exigir
 * login. A lista segue o que o app oficial de IA no aparelho da Google
 * (AI Edge Gallery) oferece, mais um modelo em .gguf. Se algum dia o Hugging
 * Face pedir login, o app avisa e a pessoa baixa pelo navegador e usa Importar.
 */
data class CatalogModel(
    val title: String,
    @StringRes val descriptionRes: Int,
    val format: ModelFormat,
    val repo: String,
    val fileName: String,
    /** Versao fixa do repositorio: o arquivo baixado e sempre o que foi testado. */
    val revision: String,
    /** Tamanho aproximado, so para mostrar e conferir o espaco livre. */
    val approxBytes: Long
) {
    val downloadUrl: String
        get() = "https://huggingface.co/$repo/resolve/$revision/$fileName?download=true"

    val pageUrl: String
        get() = "https://huggingface.co/$repo"
}

object OfflineCatalog {
    val ALL: List<CatalogModel> = listOf(
        CatalogModel(
            title = "Gemma 3 1B (GGUF)",
            descriptionRes = R.string.offline_cat_gemma3_desc,
            format = ModelFormat.GGUF,
            repo = "unsloth/gemma-3-1b-it-GGUF",
            fileName = "gemma-3-1b-it-Q4_K_M.gguf",
            revision = "main",
            approxBytes = 806_000_000L
        ),
        CatalogModel(
            title = "Gemma 4 E2B",
            descriptionRes = R.string.offline_cat_gemma4_desc,
            format = ModelFormat.LITERTLM,
            repo = "litert-community/gemma-4-E2B-it-litert-lm",
            fileName = "gemma-4-E2B-it.litertlm",
            revision = "6e5c4f1e395deb959c494953478fa5cec4b8008f",
            approxBytes = 2_588_147_712L
        ),
        CatalogModel(
            title = "Gemma 4 E4B",
            descriptionRes = R.string.offline_cat_gemma4_e4b_desc,
            format = ModelFormat.LITERTLM,
            repo = "litert-community/gemma-4-E4B-it-litert-lm",
            fileName = "gemma-4-E4B-it.litertlm",
            revision = "28299f30ee4d43294517a4ac93abd6163412f07f",
            approxBytes = 3_659_530_240L
        )
    )
}
