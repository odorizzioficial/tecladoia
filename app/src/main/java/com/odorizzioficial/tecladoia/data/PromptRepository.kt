package com.odorizzioficial.tecladoia.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

private val Context.promptsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "ai_keyboard_prompts"
)

/** CRUD das funcoes personalizadas, persistidas como JSON no DataStore. */
class PromptRepository(context: Context) {

    private val appContext = context.applicationContext

    private val store = context.applicationContext.promptsDataStore
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val prompts: Flow<List<CustomPrompt>> = store.data.map { prefs ->
        val raw = prefs[KEY_PROMPTS] ?: return@map CustomPrompt.defaults(appContext)
        runCatching { json.decodeFromString<List<CustomPrompt>>(raw) }
            .getOrDefault(CustomPrompt.defaults(appContext))
            .sortedBy { it.order }
    }

    suspend fun snapshot(): List<CustomPrompt> = prompts.first()

    suspend fun add(name: String, icon: String, prompt: String, pinned: Boolean) {
        val current = snapshot()
        val item = CustomPrompt(
            id = UUID.randomUUID().toString(),
            name = name.trim(),
            icon = icon,
            prompt = prompt.trim(),
            enabled = true,
            order = (current.maxOfOrNull { it.order } ?: -1) + 1,
            pinned = pinned
        )
        persist(current + item)
    }

    suspend fun update(item: CustomPrompt) {
        persist(snapshot().map { if (it.id == item.id) item else it })
    }

    suspend fun delete(id: String) {
        persist(snapshot().filterNot { it.id == id }.reindexed())
    }

    suspend fun duplicate(id: String) {
        val current = snapshot()
        val source = current.firstOrNull { it.id == id } ?: return
        val copy = source.copy(
            id = UUID.randomUUID().toString(),
            name = "${source.name} (cópia)",
            order = (current.maxOfOrNull { it.order } ?: -1) + 1
        )
        persist(current + copy)
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        persist(snapshot().map { if (it.id == id) it.copy(enabled = enabled) else it })
    }

    suspend fun setPinned(id: String, pinned: Boolean) {
        persist(snapshot().map { if (it.id == id) it.copy(pinned = pinned) else it })
    }

    /** Move um item uma posicao para cima (-1) ou para baixo (+1). */
    suspend fun move(id: String, delta: Int) {
        val current = snapshot().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return
        val target = index + delta
        if (target < 0 || target >= current.size) return
        val item = current.removeAt(index)
        current.add(target, item)
        persist(current.reindexed())
    }

    suspend fun restoreDefaults() = persist(CustomPrompt.defaults(appContext))

    /** Substitui todas as funcoes pelas de um arquivo de backup restaurado. */
    suspend fun replaceAll(items: List<CustomPrompt>) = persist(items)

    private suspend fun persist(items: List<CustomPrompt>) {
        val normalized = items.reindexed()
        store.edit { prefs -> prefs[KEY_PROMPTS] = json.encodeToString(normalized) }
    }

    private fun List<CustomPrompt>.reindexed(): List<CustomPrompt> =
        mapIndexed { index, item -> item.copy(order = index) }

    private companion object {
        val KEY_PROMPTS = stringPreferencesKey("custom_prompts")
    }
}
