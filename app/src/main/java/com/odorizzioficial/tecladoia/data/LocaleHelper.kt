package com.odorizzioficial.tecladoia.data

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Aplica o idioma escolhido pelo usuario.
 *
 * No Android 13+ o sistema tem per-app locales e a tela de ajustes usa o
 * LocaleManager. Abaixo disso nao existe esse recurso, entao o proprio app
 * embrulha o contexto com a configuracao correta em [wrap], chamado pela
 * Application, pela Activity e pelo servico de acessibilidade - os tres
 * precisam dos mesmos textos, inclusive a barra sobre o teclado.
 *
 * A escolha fica espelhada em SharedPreferences porque [wrap] roda antes de
 * qualquer corrotina, quando o DataStore ainda nao pode ser lido.
 */
object LocaleHelper {

    private const val PREFS = "locale_prefs"
    private const val KEY_TAG = "app_language"
    const val SYSTEM = "system"

    fun saveTag(context: Context, tag: String) {
        context.applicationContext
            .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_TAG, tag)
            .apply()
    }

    fun savedTag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_TAG, SYSTEM) ?: SYSTEM

    /** Devolve o contexto ja com o idioma do usuario, ou o original. */
    fun wrap(context: Context): Context {
        val tag = runCatching { savedTag(context) }.getOrDefault(SYSTEM)
        if (tag == SYSTEM || tag.isBlank()) return context
        // No 13+ quem manda e o sistema; embrulhar de novo so duplicaria regra.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return context

        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}
