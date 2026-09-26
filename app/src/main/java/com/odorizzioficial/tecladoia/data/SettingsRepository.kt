package com.odorizzioficial.tecladoia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.AppLanguages
import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.BarHeight
import com.odorizzioficial.tecladoia.domain.GeminiModels
import com.odorizzioficial.tecladoia.domain.Languages
import com.odorizzioficial.tecladoia.domain.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_keyboard_settings"
)

class SettingsRepository(context: Context) {

    private val appContext = context.applicationContext
    private val store = appContext.settingsDataStore
    private val secure = SecureKeyStore(appContext)

    val settings: Flow<AppSettings> = store.data.map { prefs -> prefs.toSettings() }

    suspend fun snapshot(): AppSettings = settings.first()

    // --- API key ---------------------------------------------------------

    /**
     * Chave em memoria para nao repetir a decifragem AES em cada toque na
     * barra. O cache e invalidado quando a chave e salva ou removida.
     */
    @Volatile
    private var cachedKey: String? = null

    fun apiKey(): String {
        cachedKey?.let { return it }
        val value = secure.readApiKey()
        cachedKey = value
        return value
    }

    fun maskedApiKey(): String = secure.maskedApiKey()

    suspend fun saveApiKey(value: String) {
        secure.saveApiKey(value)
        cachedKey = value.trim()
        // Toque no DataStore para que os coletores recebam o novo estado.
        store.edit { it[Keys.API_KEY_STAMP] = System.currentTimeMillis().toString() }
    }

    suspend fun clearApiKey() {
        secure.clearApiKey()
        cachedKey = ""
        store.edit { it[Keys.API_KEY_STAMP] = System.currentTimeMillis().toString() }
    }

    // --- Preferencias ----------------------------------------------------

    suspend fun setModel(model: String) = store.edit {
        it[Keys.MODEL] = model.removePrefix("models/")
    }

    suspend fun setTemperature(value: Float) =
        store.edit { it[Keys.TEMPERATURE] = value.coerceIn(0f, 1f) }

    suspend fun setLanguage(code: String) = store.edit { it[Keys.LANGUAGE] = code }

    suspend fun setTranslateTarget(code: String) = store.edit { it[Keys.TRANSLATE_TARGET] = code }

    suspend fun setAutoBar(value: Boolean) = store.edit { it[Keys.AUTO_BAR] = value }

    suspend fun setHideWithKeyboard(value: Boolean) =
        store.edit { it[Keys.HIDE_WITH_KEYBOARD] = value }

    suspend fun setPinnedShortcuts(value: Boolean) =
        store.edit { it[Keys.PINNED_SHORTCUTS] = value }

    suspend fun setCustomPromptsFirst(value: Boolean) =
        store.edit { it[Keys.CUSTOM_FIRST] = value }

    suspend fun setHaptics(value: Boolean) = store.edit { it[Keys.HAPTICS] = value }

    suspend fun setFluidAnimations(value: Boolean) = store.edit { it[Keys.ANIMATIONS] = value }

    suspend fun setThemeMode(mode: ThemeMode) = store.edit { it[Keys.THEME] = mode.name }

    suspend fun setBarHeight(height: BarHeight) = store.edit { it[Keys.BAR_HEIGHT] = height.name }

    suspend fun setHistoryEnabled(value: Boolean) = store.edit { it[Keys.HISTORY] = value }

    /** Guarda onde o usuario deixou a barra flutuante. */
    /** Guarda os modelos que a chave aceita, com a impressao digital da chave. */
    suspend fun setCachedModels(models: List<String>, keyFingerprint: String) = store.edit {
        it[Keys.MODEL_CACHE] = models.joinToString(",")
        it[Keys.MODEL_CACHE_KEY] = keyFingerprint
    }

    suspend fun setOnboardingDone(value: Boolean) =
        store.edit { it[Keys.ONBOARDING_DONE] = value }

    suspend fun setLastSeenVersion(version: String) =
        store.edit { it[Keys.LAST_SEEN_VERSION] = version }

    suspend fun setAnimationStyle(style: AnimationStyle) =
        store.edit { it[Keys.ANIMATION_STYLE] = style.name }

    suspend fun setAppLanguage(tag: String) = store.edit { it[Keys.APP_LANGUAGE] = tag }

    suspend fun setBarOffset(x: Int, y: Int) = store.edit {
        it[Keys.BAR_OFFSET_X] = x
        it[Keys.BAR_OFFSET_Y] = y
    }

    private fun Preferences.toSettings(): AppSettings = AppSettings(
        hasApiKey = secure.hasApiKey(),
        model = GeminiModels.sanitize(this[Keys.MODEL]),
        temperature = this[Keys.TEMPERATURE] ?: 0.4f,
        language = this[Keys.LANGUAGE] ?: Languages.DEFAULT,
        translateTarget = this[Keys.TRANSLATE_TARGET] ?: "en-US",
        autoBar = this[Keys.AUTO_BAR] ?: true,
        hideWithKeyboard = this[Keys.HIDE_WITH_KEYBOARD] ?: true,
        pinnedShortcuts = this[Keys.PINNED_SHORTCUTS] ?: true,
        customPromptsFirst = this[Keys.CUSTOM_FIRST] ?: false,
        haptics = this[Keys.HAPTICS] ?: true,
        fluidAnimations = this[Keys.ANIMATIONS] ?: true,
        themeMode = runCatching { ThemeMode.valueOf(this[Keys.THEME] ?: ThemeMode.DARK.name) }
            .getOrDefault(ThemeMode.DARK),
        barHeight = runCatching { BarHeight.valueOf(this[Keys.BAR_HEIGHT] ?: BarHeight.NORMAL.name) }
            .getOrDefault(BarHeight.NORMAL),
        historyEnabled = this[Keys.HISTORY] ?: false,
        animationStyle = this[Keys.ANIMATION_STYLE]
            ?.let { runCatching { AnimationStyle.valueOf(it) }.getOrNull() }
            ?: AnimationStyle.FULL,
        appLanguage = this[Keys.APP_LANGUAGE] ?: AppLanguages.DEFAULT,
        cachedModels = this[Keys.MODEL_CACHE]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?: emptyList(),
        cachedModelsKey = this[Keys.MODEL_CACHE_KEY] ?: "",
        onboardingDone = this[Keys.ONBOARDING_DONE] ?: false,
        lastSeenVersion = this[Keys.LAST_SEEN_VERSION] ?: "",
        barOffsetX = this[Keys.BAR_OFFSET_X] ?: 0,
        barOffsetY = this[Keys.BAR_OFFSET_Y] ?: 0
    )

    private object Keys {
        val MODEL = stringPreferencesKey("model")
        val TEMPERATURE = floatPreferencesKey("temperature")
        val LANGUAGE = stringPreferencesKey("language")
        val TRANSLATE_TARGET = stringPreferencesKey("translate_target")
        val AUTO_BAR = booleanPreferencesKey("auto_bar")
        val HIDE_WITH_KEYBOARD = booleanPreferencesKey("hide_with_keyboard")
        val PINNED_SHORTCUTS = booleanPreferencesKey("pinned_shortcuts")
        val CUSTOM_FIRST = booleanPreferencesKey("custom_prompts_first")
        val HAPTICS = booleanPreferencesKey("haptics")
        val ANIMATIONS = booleanPreferencesKey("animations")
        val THEME = stringPreferencesKey("theme_mode")
        val BAR_HEIGHT = stringPreferencesKey("bar_height")
        val HISTORY = booleanPreferencesKey("history_enabled")
        val API_KEY_STAMP = stringPreferencesKey("api_key_stamp")
        val ANIMATION_STYLE = stringPreferencesKey("animation_style")
        val APP_LANGUAGE = stringPreferencesKey("app_language")
        val MODEL_CACHE = stringPreferencesKey("model_cache")
        val MODEL_CACHE_KEY = stringPreferencesKey("model_cache_key")
        val ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val LAST_SEEN_VERSION = stringPreferencesKey("last_seen_version")
        val BAR_OFFSET_X = intPreferencesKey("bar_offset_x")
        val BAR_OFFSET_Y = intPreferencesKey("bar_offset_y")
    }
}
