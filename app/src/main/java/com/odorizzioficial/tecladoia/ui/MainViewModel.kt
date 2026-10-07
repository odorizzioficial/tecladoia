package com.odorizzioficial.tecladoia.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.odorizzioficial.tecladoia.AppGraph
import com.odorizzioficial.tecladoia.ai.AiEngine
import com.odorizzioficial.tecladoia.data.PromptRepository
import android.content.Context
import android.net.Uri
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.ai.offline.OfflineController
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.ai.ModelRanking
import com.odorizzioficial.tecladoia.data.BackupResult
import com.odorizzioficial.tecladoia.data.PromptBackup
import com.odorizzioficial.tecladoia.data.SettingsRepository
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AiResult
import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.BarHeight
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.domain.GeminiModels
import com.odorizzioficial.tecladoia.domain.ThemeMode
import com.odorizzioficial.tecladoia.domain.Tone
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.odorizzioficial.tecladoia.ai.LiveDictation
import com.odorizzioficial.tecladoia.service.VoiceInputController
import com.odorizzioficial.tecladoia.data.UpdateChecker
import com.odorizzioficial.tecladoia.data.UpdateError
import com.odorizzioficial.tecladoia.data.UpdateInfo
import com.odorizzioficial.tecladoia.data.UpdateResult
import kotlinx.coroutines.delay
import com.odorizzioficial.tecladoia.data.UpdateNotifier

enum class ConnectionStage { IDLE, TESTING, OK, FAILED }

data class ConnectionState(
    val stage: ConnectionStage = ConnectionStage.IDLE,
    val message: String = ""
)

/** Modelos descobertos na chave do usuario. */
data class ModelsState(
    val loading: Boolean = false,
    val available: List<String> = emptyList(),
    val message: String = ""
)

/** Estado do "playground" da tela Assistente, onde o usuario testa a IA digitando. */
data class PlaygroundState(
    val input: String = "",
    val output: String = "",
    val running: Boolean = false,
    /** Microfone aberto: o texto dito entra no campo e e corrigido ao vivo. */
    val listening: Boolean = false,
    val error: String? = null,
    val tone: Tone = Tone.PROFESSIONAL,
    val instruction: String = ""
)

/** Estado da busca por versao nova do app. */
data class UpdateUiState(
    val checking: Boolean = false,
    val available: UpdateInfo? = null,
    val upToDate: Boolean = false,
    val error: UpdateError? = null
)

/** Estado da tela de backup das funcoes personalizadas. */
data class BackupUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

class MainViewModel(
    private val context: Context,
    private val settingsRepo: SettingsRepository,
    private val promptRepo: PromptRepository,
    private val engine: AiEngine
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepo.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val prompts: StateFlow<List<CustomPrompt>> = promptRepo.prompts
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _connection = MutableStateFlow(ConnectionState())
    val connection: StateFlow<ConnectionState> = _connection.asStateFlow()

    /** Pedido interno para abrir Ajustes > Gemini API Key. */
    private val _openApiKeyRequest = MutableStateFlow(false)
    val openApiKeyRequest: StateFlow<Boolean> = _openApiKeyRequest.asStateFlow()

    private val _models = MutableStateFlow(ModelsState())
    val models: StateFlow<ModelsState> = _models.asStateFlow()

    private val _playground = MutableStateFlow(PlaygroundState())
    val playground: StateFlow<PlaygroundState> = _playground.asStateFlow()

    // --- Microfone do teste: o mesmo ditado em tempo real da barra ---------
    private var playgroundBase = ""

    private val playgroundDictation = LiveDictation(
        engine = engine,
        scope = viewModelScope,
        onDisplay = { shown ->
            _playground.update { it.copy(input = joinSpoken(playgroundBase, shown)) }
        },
        minGapMs = {
            if (settings.value.aiProvider == AiProvider.OFFLINE) 0L else 1_200L
        }
    )

    private val playgroundVoice = VoiceInputController(
        context,
        object : VoiceInputController.Callbacks {
            override fun onRecordingStarted() = Unit

            override fun onPartialTranscript(text: String) {
                playgroundDictation.onPartial(text)
            }

            override fun onFinalTranscript(text: String) {
                finishPlaygroundDictation(text)
            }

            override fun onVoiceError(message: String) {
                playgroundDictation.cancel()
                _playground.update { it.copy(listening = false, running = false, error = message) }
            }
        }
    )

    /** Modelos offline: baixar, importar, escolher, apagar e testar. */
    val offline = OfflineController(
        context = context,
        settingsRepo = settingsRepo,
        store = AppGraph.offlineStore,
        llm = AppGraph.offline,
        scope = viewModelScope
    )

    private val _openOfflineRequest = MutableStateFlow(false)
    val openOfflineRequest: StateFlow<Boolean> = _openOfflineRequest.asStateFlow()

    private val _openAiHubRequest = MutableStateFlow(false)
    val openAiHubRequest: StateFlow<Boolean> = _openAiHubRequest.asStateFlow()

    private val _backup = MutableStateFlow(BackupUiState())
    val backup: StateFlow<BackupUiState> = _backup.asStateFlow()

    // --- Atualizacao do app, pelos releases do GitHub ---------------------
    private val updateChecker = UpdateChecker(context)

    private val _update = MutableStateFlow(UpdateUiState())
    val update: StateFlow<UpdateUiState> = _update.asStateFlow()

    private val _openUpdateRequest = MutableStateFlow(false)
    val openUpdateRequest: StateFlow<Boolean> = _openUpdateRequest.asStateFlow()

    init {
        // Ao abrir o app, procura versao nova em silencio, no maximo a cada 12 horas.
        viewModelScope.launch {
            val saved = settingsRepo.snapshot()
            val due = System.currentTimeMillis() - saved.lastUpdateCheck > UPDATE_CHECK_INTERVAL_MS
            if (saved.autoUpdateCheck && due) {
                delay(UPDATE_CHECK_DELAY_MS)
                checkForUpdate(manual = false)
            }
        }
    }

    fun maskedApiKey(): String = settingsRepo.maskedApiKey()

    // --- Ajustes ---------------------------------------------------------

    fun saveApiKey(value: String) = viewModelScope.launch {
        settingsRepo.saveApiKey(value)
        _connection.value = ConnectionState(ConnectionStage.IDLE, context.getString(R.string.key_saved_message))
    }

    fun clearApiKey() = viewModelScope.launch {
        settingsRepo.clearApiKey()
        _connection.value = ConnectionState(ConnectionStage.IDLE, context.getString(R.string.key_removed_message))
    }

    /**
     * Valida a chave na propria API. A checagem usa a listagem de modelos, que
     * confirma chave e permissoes sem gastar uma geracao, e ja diz se o modelo
     * escolhido continua liberado.
     */
    fun testConnection(rawKey: String?) = viewModelScope.launch {
        _connection.value = ConnectionState(ConnectionStage.TESTING, context.getString(R.string.conn_testing))
        when (val result = engine.availableModels(rawKey)) {
            is AiResult.Success -> {
                val ids = result.value
                settingsRepo.setCachedModels(ids, fingerprint(rawKey))
                _models.value = ModelsState(available = ids)
                val selected = settings.value.model
                _connection.value = if (selected in ids) {
                    ConnectionState(
                        ConnectionStage.OK,
                        context.getString(R.string.conn_valid, ids.size)
                    )
                } else {
                    ConnectionState(
                        ConnectionStage.OK,
                        context.getString(R.string.conn_valid_model_missing, selected)
                    )
                }
            }

            is AiResult.Failure -> _connection.value =
                ConnectionState(ConnectionStage.FAILED, result.error.message(context))
        }
    }

    fun setModel(model: String) = viewModelScope.launch { settingsRepo.setModel(model) }

    /**
     * Consulta a API os modelos daquela chave, salva o resultado em cache e
     * corrige a selecao se o modelo escolhido nao estiver mais na lista.
     */
    fun loadAvailableModels(rawKey: String? = null) = viewModelScope.launch {
        _models.value = ModelsState(loading = true, available = _models.value.available)
        when (val result = engine.availableModels(rawKey)) {
            is AiResult.Success -> {
                val ids = result.value
                val selected = settings.value.model
                val stillThere = selected in ids
                _models.value = ModelsState(
                    loading = false,
                    available = ids,
                    message = if (stillThere) {
                        context.getString(R.string.models_available, ids.size)
                    } else {
                        context.getString(R.string.models_available_pick, ids.size)
                    }
                )
                settingsRepo.setCachedModels(ids, fingerprint(rawKey))
                if (!stillThere) {
                    // Sem inventar nome: o novo padrao vem da propria lista.
                    val fallback = ModelRanking.best(ids) ?: ids.first()
                    settingsRepo.setModel(fallback)
                }
            }

            is AiResult.Failure -> _models.value = ModelsState(
                loading = false,
                available = _models.value.available,
                message = result.error.message(context)
            )
        }
    }

    /** Carrega o cache salvo, sem rede, ao abrir a tela de modelos. */
    fun primeCachedModels() {
        if (_models.value.available.isNotEmpty() || _models.value.loading) return
        val snapshot = settings.value
        if (snapshot.cachedModels.isNotEmpty()) {
            _models.value = ModelsState(available = snapshot.cachedModels)
        }
    }

    /**
     * Identificador curto da chave usado apenas para saber se o cache pertence
     * a chave atual. Nunca e a chave, nem parte utilizavel dela.
     */
    /** Impressao digital da chave atual, para validar o cache de modelos. */
    fun currentKeyFingerprint(): String = fingerprint(null)

    private fun fingerprint(rawKey: String?): String {
        val key = rawKey?.takeIf { it.isNotBlank() } ?: settingsRepo.apiKey()
        return key.hashCode().toString()
    }
    fun setProtectFinancialApps(value: Boolean) =
        viewModelScope.launch { settingsRepo.setProtectFinancialApps(value) }

    fun setAppIgnored(packageName: String, ignored: Boolean) =
        viewModelScope.launch { settingsRepo.setAppIgnored(packageName, ignored) }

    fun setTemperature(value: Float) = viewModelScope.launch { settingsRepo.setTemperature(value) }
    fun setLanguage(code: String) = viewModelScope.launch { settingsRepo.setLanguage(code) }
    fun setTranslateTarget(code: String) =
        viewModelScope.launch { settingsRepo.setTranslateTarget(code) }

    fun setAutoBar(value: Boolean) = viewModelScope.launch { settingsRepo.setAutoBar(value) }
    fun setHideWithKeyboard(value: Boolean) =
        viewModelScope.launch { settingsRepo.setHideWithKeyboard(value) }

    fun setPinnedShortcuts(value: Boolean) =
        viewModelScope.launch { settingsRepo.setPinnedShortcuts(value) }

    fun requestApiKeyScreen() {
        _openApiKeyRequest.value = true
    }

    fun consumeApiKeyRequest() {
        _openApiKeyRequest.value = false
    }

    fun requestOfflineScreen() {
        _openOfflineRequest.value = true
    }

    fun requestUpdateScreen() {
        _openUpdateRequest.value = true
    }

    fun consumeUpdateRequest() {
        _openUpdateRequest.value = false
    }

    fun requestAiHubScreen() {
        _openAiHubRequest.value = true
    }

    fun consumeAiHubRequest() {
        _openAiHubRequest.value = false
    }

    fun consumeOfflineRequest() {
        _openOfflineRequest.value = false
    }

    fun setAiProvider(provider: AiProvider) =
        viewModelScope.launch { settingsRepo.setAiProvider(provider) }

    fun setOfflineTemperature(value: Float) =
        viewModelScope.launch { settingsRepo.setOfflineTemperature(value) }

    fun completeOnboarding() = viewModelScope.launch { settingsRepo.setOnboardingDone(true) }

    fun markVersionSeen(version: String) =
        viewModelScope.launch { settingsRepo.setLastSeenVersion(version) }

    fun setAnimationStyle(style: AnimationStyle) =
        viewModelScope.launch { settingsRepo.setAnimationStyle(style) }

    fun setAppLanguage(tag: String) = viewModelScope.launch { settingsRepo.setAppLanguage(tag) }

    fun setCustomPromptsFirst(value: Boolean) =
        viewModelScope.launch { settingsRepo.setCustomPromptsFirst(value) }

    fun setHaptics(value: Boolean) = viewModelScope.launch { settingsRepo.setHaptics(value) }
    fun setAnimations(value: Boolean) =
        viewModelScope.launch { settingsRepo.setFluidAnimations(value) }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepo.setThemeMode(mode) }
    fun setBarHeight(height: BarHeight) = viewModelScope.launch { settingsRepo.setBarHeight(height) }
    fun setHistoryEnabled(value: Boolean) =
        viewModelScope.launch { settingsRepo.setHistoryEnabled(value) }

    // --- Funcoes personalizadas -----------------------------------------

    fun addPrompt(name: String, icon: String, prompt: String, pinned: Boolean) =
        viewModelScope.launch { promptRepo.add(name, icon, prompt, pinned) }

    fun updatePrompt(item: CustomPrompt) = viewModelScope.launch { promptRepo.update(item) }
    fun deletePrompt(id: String) = viewModelScope.launch { promptRepo.delete(id) }
    fun duplicatePrompt(id: String) = viewModelScope.launch { promptRepo.duplicate(id) }
    fun setPromptEnabled(id: String, enabled: Boolean) =
        viewModelScope.launch { promptRepo.setEnabled(id, enabled) }

    fun setPromptPinned(id: String, pinned: Boolean) =
        viewModelScope.launch { promptRepo.setPinned(id, pinned) }

    fun movePrompt(id: String, delta: Int) = viewModelScope.launch { promptRepo.move(id, delta) }
    fun restoreDefaultPrompts() = viewModelScope.launch { promptRepo.restoreDefaults() }

    // --- Backup das funcoes personalizadas --------------------------------

    /** Exporta as funcoes atuais para um arquivo JSON na pasta Downloads. */
    fun exportPromptsBackup() = viewModelScope.launch {
        _backup.update { it.copy(busy = true, message = null) }
        when (val result = PromptBackup.export(context, prompts.value)) {
            is BackupResult.Success -> _backup.update {
                it.copy(
                    busy = false,
                    isError = false,
                    message = context.getString(
                        R.string.backup_export_success,
                        result.fileName,
                        result.count
                    )
                )
            }

            is BackupResult.Failure -> _backup.update {
                it.copy(
                    busy = false,
                    isError = true,
                    message = context.getString(R.string.backup_export_error)
                )
            }
        }
    }

    /** Le o arquivo escolhido pelo usuario e substitui as funcoes atuais por ele. */
    fun importPromptsBackup(uri: Uri) = viewModelScope.launch {
        _backup.update { it.copy(busy = true, message = null) }
        val items = PromptBackup.import(context, uri)
        if (items == null) {
            _backup.update {
                it.copy(
                    busy = false,
                    isError = true,
                    message = context.getString(R.string.backup_import_error)
                )
            }
            return@launch
        }
        promptRepo.replaceAll(items)
        _backup.update {
            it.copy(
                busy = false,
                isError = false,
                message = context.getString(R.string.backup_import_success, items.size)
            )
        }
    }

    fun clearBackupMessage() = _backup.update { it.copy(message = null) }

    fun installedVersion(): String = updateChecker.installedVersion()

    /**
     * Procura uma versao nova. Na verificacao manual qualquer resultado aparece na
     * tela; na automatica so aparece o que for versao nova (falha de rede nao incomoda).
     */
    fun checkForUpdate(manual: Boolean = true) {
        if (_update.value.checking) return
        viewModelScope.launch {
            _update.update { it.copy(checking = true, error = null, upToDate = false) }
            val result = updateChecker.check()
            // Sem internet nao conta como verificado: tenta de novo na proxima abertura.
            val offline = result is UpdateResult.Failure && result.error == UpdateError.NETWORK
            if (!offline) settingsRepo.setLastUpdateCheck(System.currentTimeMillis())
            if (!manual && result is UpdateResult.Available) {
                UpdateNotifier.notifyOnce(context, settingsRepo, result.info)
            }
            _update.update { state ->
                when (result) {
                    is UpdateResult.Available ->
                        state.copy(checking = false, available = result.info, upToDate = false)

                    UpdateResult.UpToDate ->
                        state.copy(checking = false, available = null, upToDate = manual)

                    is UpdateResult.Failure ->
                        state.copy(checking = false, error = if (manual) result.error else null)
                }
            }
        }
    }

    /** "Depois" no aviso: nao mostra de novo o aviso dessa versao. */
    fun dismissUpdate(version: String) = viewModelScope.launch { settingsRepo.setDismissedUpdate(version) }

    fun setAutoUpdateCheck(value: Boolean) = viewModelScope.launch { settingsRepo.setAutoUpdateCheck(value) }

    fun setUpdateNotify(value: Boolean) = viewModelScope.launch { settingsRepo.setUpdateNotify(value) }

    // --- Playground ------------------------------------------------------

    fun onPlaygroundInput(value: String) = _playground.update { it.copy(input = value) }

    /** Abre o microfone; o que for dito entra no fim do texto que ja estava no campo. */
    fun startPlaygroundVoice() {
        if (_playground.value.listening) return
        playgroundBase = _playground.value.input.trim()
        _playground.update { it.copy(listening = true, error = null) }
        playgroundDictation.start()
        playgroundVoice.start(settings.value.language)
    }

    /** Fecha o microfone; o texto final chega por [finishPlaygroundDictation]. */
    fun stopPlaygroundVoice() {
        if (!_playground.value.listening) return
        playgroundVoice.stop()
    }

    fun notifyPlaygroundMicDenied() =
        _playground.update { it.copy(error = context.getString(R.string.overlay_mic_denied)) }

    private fun finishPlaygroundDictation(text: String) {
        playgroundDictation.onPartial(text)
        viewModelScope.launch {
            _playground.update { it.copy(running = true) }
            val done = playgroundDictation.finish(text)
            _playground.update {
                it.copy(
                    input = joinSpoken(playgroundBase, done.text),
                    listening = false,
                    running = false,
                    error = done.error?.message(context)
                )
            }
        }
    }

    private fun joinSpoken(base: String, spoken: String): String = when {
        base.isEmpty() -> spoken
        spoken.isEmpty() -> base
        base.endsWith(" ") || base.endsWith("\n") -> base + spoken
        else -> "$base $spoken"
    }

    override fun onCleared() {
        playgroundDictation.cancel()
        playgroundVoice.cancel()
        super.onCleared()
    }
    fun onPlaygroundInstruction(value: String) = _playground.update { it.copy(instruction = value) }
    fun onPlaygroundTone(tone: Tone) = _playground.update { it.copy(tone = tone) }

    fun runPlayground(action: AiAction) = viewModelScope.launch {
        val current = _playground.value
        if (current.input.isBlank()) {
            _playground.update { it.copy(error = context.getString(R.string.playground_empty)) }
            return@launch
        }
        _playground.update { it.copy(running = true, error = null, output = "") }
        val result = engine.run(
            action = action,
            text = current.input,
            tone = current.tone,
            instruction = current.instruction.ifBlank {
                "Reescreva o texto mantendo o significado."
            }
        )
        _playground.update {
            when (result) {
                is AiResult.Success -> it.copy(running = false, output = result.value)
                is AiResult.Failure -> it.copy(running = false, error = result.error.message(context))
            }
        }
    }

    fun runPlaygroundCustom(prompt: CustomPrompt) = viewModelScope.launch {
        val current = _playground.value
        if (current.input.isBlank()) {
            _playground.update { it.copy(error = context.getString(R.string.playground_empty)) }
            return@launch
        }
        _playground.update { it.copy(running = true, error = null, output = "") }
        val result = engine.runCustom(prompt, current.input)
        _playground.update {
            when (result) {
                is AiResult.Success -> it.copy(running = false, output = result.value)
                is AiResult.Failure -> it.copy(running = false, error = result.error.message(context))
            }
        }
    }

    fun clearPlaygroundError() = _playground.update { it.copy(error = null) }

    companion object {
        private const val UPDATE_CHECK_INTERVAL_MS = 12 * 60 * 60 * 1000L
        private const val UPDATE_CHECK_DELAY_MS = 3_000L

        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    context = AppGraph.appContext,
                    settingsRepo = AppGraph.settings,
                    promptRepo = AppGraph.prompts,
                    engine = AppGraph.engine
                ) as T
            }
        }
    }
}
