package com.odorizzioficial.tecladoia

import android.app.Application
import android.content.Context
import com.odorizzioficial.tecladoia.ai.AiEngine
import com.odorizzioficial.tecladoia.ai.GeminiService
import com.odorizzioficial.tecladoia.ai.offline.OfflineLlm
import com.odorizzioficial.tecladoia.ai.offline.OfflineModelStore
import com.odorizzioficial.tecladoia.data.LocaleHelper
import com.odorizzioficial.tecladoia.data.PromptRepository
import com.odorizzioficial.tecladoia.data.SettingsRepository

class AiKeyboardApp : Application() {

    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        AppGraph.init(this)
    }
}

/**
 * Localizador de dependencias minimo. Evita uma biblioteca de injecao para um
 * grafo com quatro objetos, e e inicializado tanto pela Application quanto pelo
 * AccessibilityService (que pode subir antes de qualquer Activity).
 */
object AppGraph {

    @Volatile
    private var initialized = false

    lateinit var appContext: Context
        private set
    lateinit var settings: SettingsRepository
        private set
    lateinit var prompts: PromptRepository
        private set
    lateinit var gemini: GeminiService
        private set
    lateinit var engine: AiEngine
        private set
    lateinit var offlineStore: OfflineModelStore
        private set
    lateinit var offline: OfflineLlm
        private set

    @Synchronized
    fun init(context: Context) {
        if (initialized) return
        val app = context.applicationContext
        appContext = app
        settings = SettingsRepository(app)
        prompts = PromptRepository(app)
        gemini = GeminiService()
        offlineStore = OfflineModelStore(app)
        offline = OfflineLlm(app)
        engine = AiEngine(settings, gemini, offline, offlineStore)
        initialized = true
    }
}
