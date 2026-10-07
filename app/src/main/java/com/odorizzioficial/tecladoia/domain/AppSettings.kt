package com.odorizzioficial.tecladoia.domain

import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R

enum class ThemeMode { DARK, LIGHT, SYSTEM }

/**
 * Intensidade das animacoes do app.
 *
 * FULL usa expansao de limites compartilhados (SharedTransitionLayout) nos
 * menus e entrada deslizante na barra sobre o teclado; SIMPLE reduz tudo a
 * esmaecimento; NONE troca sem animacao, para aparelhos mais fracos.
 */
enum class AnimationStyle(@StringRes val labelRes: Int) {
    FULL(R.string.anim_full),
    SIMPLE(R.string.anim_simple),
    NONE(R.string.anim_none)
}

enum class BarHeight(@StringRes val labelRes: Int, val heightDp: Int) {
    COMPACT(R.string.bar_height_compact, 48),
    NORMAL(R.string.bar_height_normal, 56),
    EXPANDED(R.string.bar_height_expanded, 64)
}

/** Quem responde aos pedidos de IA da barra e do assistente. */
enum class AiProvider { GEMINI, OFFLINE }

data class GeminiModel(val id: String, val label: String)

object GeminiModels {
    /**
     * Valor inicial, usado somente antes de o app consultar a API. A lista de
     * modelos oferecida ao usuario e sempre a que a API devolve para a chave
     * dele: o app nao mantem catalogo proprio nem inventa nomes.
     */
    const val DEFAULT = "gemini-flash-lite-latest"

    /** Prefixos desligados pelo Google: qualquer chave responde 404 neles. */
    private val RETIRED_PREFIXES = listOf(
        "gemini-1.0", "gemini-1.5", "gemini-2.0", "gemini-pro", "models/gemini-1", "text-bison"
    )

    fun isRetired(id: String): Boolean = RETIRED_PREFIXES.any { id.startsWith(it) }

    /** Troca um modelo desligado pelo padrao atual, preservando escolhas validas. */
    fun sanitize(id: String?): String = when {
        id.isNullOrBlank() -> DEFAULT
        isRetired(id) -> DEFAULT
        else -> id
    }

    /** Nome legivel a partir do identificador devolvido pela API. */
    fun prettyLabel(id: String): String = id
        .removePrefix("models/")
        .split('-')
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercase() } }
}

/** Idiomas oferecidos para a interface do app. */
data class AppLanguage(val tag: String, val label: String, val flag: String)

object AppLanguages {
    val ALL = listOf(
        // O nome do sistema e o unico traduzido; os outros aparecem no
        // proprio idioma, como manda a convencao de seletores de idioma.
        AppLanguage("system", "", "\uD83D\uDCF1"),
        AppLanguage("pt-BR", "Português (Brasil)", "\uD83C\uDDE7\uD83C\uDDF7"),
        AppLanguage("pt-PT", "Português (Portugal)", "\uD83C\uDDF5\uD83C\uDDF9"),
        AppLanguage("en", "English", "\uD83C\uDDFA\uD83C\uDDF8"),
        AppLanguage("es", "Español", "\uD83C\uDDEA\uD83C\uDDF8"),
        AppLanguage("ro", "Română", "\uD83C\uDDF7\uD83C\uDDF4"),
        AppLanguage("fr", "Français", "\uD83C\uDDEB\uD83C\uDDF7"),
        AppLanguage("zh", "\u4E2D\u6587", "\uD83C\uDDE8\uD83C\uDDF3"),
        AppLanguage("ja", "\u65E5\u672C\u8A9E", "\uD83C\uDDEF\uD83C\uDDF5"),
        AppLanguage("ko", "\uD55C\uAD6D\uC5B4", "\uD83C\uDDF0\uD83C\uDDF7"),
        AppLanguage("hi", "\u0939\u093F\u0928\u094D\u0926\u0940", "\uD83C\uDDEE\uD83C\uDDF3")
    )

    const val DEFAULT = "system"

    fun labelFor(tag: String): String = ALL.firstOrNull { it.tag == tag }?.label ?: tag

    fun flagFor(tag: String): String = ALL.firstOrNull { it.tag == tag }?.flag.orEmpty()
}

/**
 * Idioma usado pela IA.
 *
 * [labelRes] e o nome traduzido que o usuario le; [promptName] e o nome fixo
 * que entra na instrucao enviada ao modelo, e por isso nao depende do idioma
 * da interface.
 */
data class LanguageOption(
    val code: String,
    @StringRes val labelRes: Int,
    val badge: String,
    val promptName: String
)

object Languages {
    val ALL = listOf(
        LanguageOption("pt-BR", R.string.ai_lang_pt_br, "BR", "portugues do Brasil"),
        LanguageOption("pt-PT", R.string.ai_lang_pt_pt, "PT", "portugues europeu"),
        LanguageOption("en-US", R.string.ai_lang_en_us, "EN", "ingles dos Estados Unidos"),
        LanguageOption("es-ES", R.string.ai_lang_es, "ES", "espanhol"),
        LanguageOption("fr-FR", R.string.ai_lang_fr, "FR", "frances"),
        LanguageOption("de-DE", R.string.ai_lang_de, "DE", "alemao"),
        LanguageOption("it-IT", R.string.ai_lang_it, "IT", "italiano"),
        LanguageOption("ja-JP", R.string.ai_lang_ja, "JA", "japones")
    )
    const val DEFAULT = "pt-BR"

    @StringRes
    fun labelResFor(code: String): Int =
        ALL.firstOrNull { it.code == code }?.labelRes ?: R.string.ai_lang_pt_br

    /** Nome do idioma para os prompts, nunca para a interface. */
    fun promptNameFor(code: String): String =
        ALL.firstOrNull { it.code == code }?.promptName ?: code

    /** Nome em ingles, para os prompts (que sao escritos em ingles). */
    fun englishNameFor(code: String): String = when (code) {
        "pt-BR" -> "Brazilian Portuguese"
        "pt-PT" -> "European Portuguese"
        "en-US" -> "American English"
        "es-ES" -> "Spanish"
        "fr-FR" -> "French"
        "de-DE" -> "German"
        "it-IT" -> "Italian"
        "ja-JP" -> "Japanese"
        else -> code
    }

    fun badgeFor(code: String): String =
        ALL.firstOrNull { it.code == code }?.badge ?: code.take(2).uppercase()
}

/** Snapshot completo das preferencias do app. */
data class AppSettings(
    val hasApiKey: Boolean = false,
    val model: String = GeminiModels.DEFAULT,
    val temperature: Float = 0.4f,
    val language: String = Languages.DEFAULT,
    val translateTarget: String = "en-US",
    val autoBar: Boolean = true,
    val hideWithKeyboard: Boolean = true,
    val pinnedShortcuts: Boolean = true,
    /** No painel do teclado, mostrar as funcoes do usuario antes dos atalhos padrao. */
    val customPromptsFirst: Boolean = false,
    val haptics: Boolean = true,
    val fluidAnimations: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.DARK,
    val animationStyle: AnimationStyle = AnimationStyle.FULL,
    /** Idioma da interface: "system" segue o aparelho. */
    val appLanguage: String = "system",
    val barHeight: BarHeight = BarHeight.NORMAL,
    /** Historico desativado por padrao, conforme requisito de privacidade. */
    val historyEnabled: Boolean = false,
    /** Modelos que a chave atual aceita, conforme a ultima consulta a API. */
    val cachedModels: List<String> = emptyList(),
    /** Impressao digital da chave a que a lista acima pertence. */
    val cachedModelsKey: String = "",
    /** Falso enquanto o usuario nao passou pela tela inicial de permissoes. */
    val onboardingDone: Boolean = false,
    /** Ultima versao cujas novidades o usuario ja viu. */
    val lastSeenVersion: String = "",
    /** Posicao da barra escolhida no arrasto, em pixels, mantida entre sessoes. */
    val barOffsetX: Int = 0,
    val barOffsetY: Int = 0,
    /** Barra desligada em bancos, carteiras digitais e corretoras conhecidos. */
    /** Procura versao nova no GitHub ao abrir o app (no maximo a cada 12 horas). */
    val autoUpdateCheck: Boolean = true,
    val lastUpdateCheck: Long = 0L,
    /** Notificacao quando sai versao nova (precisa da permissao de notificacoes no Android 13+). */
    val updateNotify: Boolean = true,
    /** Ultima versao ja avisada por notificacao: cada versao avisa uma vez so. */
    val notifiedUpdate: String = "",
    /** Versao nova que a pessoa dispensou no aviso do Assistente. */
    val dismissedUpdate: String = "",
    /** Gemini (nuvem) ou o modelo que roda no proprio aparelho. */
    val aiProvider: AiProvider = AiProvider.GEMINI,
    /** Nome do arquivo (.litertlm) do modelo offline escolhido. Vazio = nenhum. */
    val offlineModel: String = "",
    val offlineTemperature: Float = 0.3f,
    val protectFinancialApps: Boolean = true,
    /** Pacotes que o usuario escolheu ignorar, alem da lista padrao. */
    val ignoredApps: Set<String> = emptySet()
)
