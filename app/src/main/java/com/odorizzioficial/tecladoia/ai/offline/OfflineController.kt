package com.odorizzioficial.tecladoia.ai.offline

import android.app.ActivityManager
import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.data.SettingsRepository
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.domain.AiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Progresso de um download em andamento. */
data class DownloadProgress(val downloaded: Long, val total: Long, val paused: Boolean)

/** Progresso da copia de um arquivo importado. */
data class ImportProgress(val copied: Long, val total: Long)

/** Aviso mostrado na tela: um recurso de texto (traduzido) e seus argumentos. */
data class OfflineMessage(
    @StringRes val res: Int,
    val args: List<Any> = emptyList(),
    val isError: Boolean = false
)

data class OfflineUiState(
    val installed: List<InstalledModel> = emptyList(),
    /** Chave: nome do arquivo do modelo do catalogo. */
    val downloads: Map<String, DownloadProgress> = emptyMap(),
    val importing: ImportProgress? = null,
    val testing: Boolean = false,
    val message: OfflineMessage? = null
)

/**
 * Tudo o que a tela do modo offline faz: baixar (pelo Download Manager do
 * Android, que continua em segundo plano), importar, escolher, apagar e testar.
 */
class OfflineController(
    context: Context,
    private val settingsRepo: SettingsRepository,
    private val store: OfflineModelStore,
    private val llm: OfflineLlm,
    private val scope: CoroutineScope
) {
    private val appContext = context.applicationContext
    private val downloadManager =
        appContext.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
    private val prefs = appContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(OfflineUiState())
    val state: StateFlow<OfflineUiState> = _state.asStateFlow()

    private var pollJob: Job? = null
    private var importJob: Job? = null

    /** Memoria total do aparelho, para avisar quando o modelo e grande demais. */
    val totalRamBytes: Long = runCatching {
        val manager = appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val info = ActivityManager.MemoryInfo()
        manager.getMemoryInfo(info)
        info.totalMem
    }.getOrDefault(0L)

    // --- Estado -----------------------------------------------------------

    /** Relê os modelos da pasta e retoma o acompanhamento de downloads. */
    fun refresh() {
        publishInstalled()
        if (pending().isNotEmpty()) startPolling()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun notify(@StringRes res: Int, isError: Boolean = true) =
        _state.update { it.copy(message = OfflineMessage(res, isError = isError)) }

    private fun publishInstalled() {
        _state.update { it.copy(installed = store.list(pending().keys)) }
    }

    private fun say(@StringRes res: Int, vararg args: Any, isError: Boolean = false) =
        _state.update { it.copy(message = OfflineMessage(res, args.toList(), isError)) }

    // --- Escolher e apagar ------------------------------------------------

    /**
     * Escolher um modelo ja liga o modo offline na barra inteira: quem pediu
     * para usar um modelo do aparelho nao quer continuar no Gemini. Voltar ao
     * Gemini e feito no menu Inteligencia artificial.
     */
    fun select(fileName: String) {
        scope.launch {
            settingsRepo.setOfflineModel(fileName)
            settingsRepo.setAiProvider(AiProvider.OFFLINE)
            llm.release()
        }
    }

    fun delete(fileName: String) {
        scope.launch {
            llm.release()
            withContext(Dispatchers.IO) { store.delete(fileName) }
            val snapshot = settingsRepo.snapshot()
            if (snapshot.offlineModel == fileName) {
                settingsRepo.setOfflineModel("")
                // Sem modelo nao ha o que usar: volta para o Gemini.
                if (snapshot.aiProvider == AiProvider.OFFLINE) {
                    settingsRepo.setAiProvider(AiProvider.GEMINI)
                }
            }
            publishInstalled()
        }
    }

    // --- Importar ---------------------------------------------------------

    fun importModel(uri: Uri) {
        if (importJob?.isActive == true) return
        importJob = scope.launch {
            _state.update { it.copy(importing = ImportProgress(0, -1), message = null) }
            val result = store.importFrom(uri) { copied, total ->
                _state.update { it.copy(importing = ImportProgress(copied, total)) }
            }
            _state.update { it.copy(importing = null) }
            when (result) {
                is ImportResult.Success -> {
                    selectIfNone(result.fileName)
                    say(R.string.offline_import_done, result.fileName)
                }

                ImportResult.UnsupportedFormat -> say(R.string.offline_import_bad_format, isError = true)
                ImportResult.NoSpace -> say(R.string.offline_import_no_space, isError = true)
                ImportResult.Failed -> say(R.string.offline_import_failed, isError = true)
            }
            publishInstalled()
        }
    }

    fun cancelImport() {
        importJob?.cancel()
        _state.update { it.copy(importing = null) }
    }

    // --- Baixar -----------------------------------------------------------

    fun download(model: CatalogModel) {
        if (!store.canDownload) {
            say(R.string.offline_no_storage, isError = true)
            return
        }
        if (pending().containsKey(model.fileName)) return
        if (store.freeBytes() < model.approxBytes + SPACE_MARGIN) {
            say(R.string.offline_import_no_space, isError = true)
            return
        }
        try {
            // Se sobrou um arquivo antigo com o mesmo nome, o Download Manager
            // criaria outro com sufixo: apaga antes.
            store.delete(model.fileName)
            val request = DownloadManager.Request(Uri.parse(model.downloadUrl))
                .setTitle(model.title)
                .setDescription(appContext.getString(R.string.app_name))
                .setMimeType("application/octet-stream")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE)
                .setAllowedOverMetered(true)
                .setDestinationInExternalFilesDir(
                    appContext,
                    OfflineModelStore.DIR_NAME,
                    model.fileName
                )
            val id = downloadManager.enqueue(request)
            prefs.edit().putLong(model.fileName, id).apply()
            _state.update { it.copy(message = null) }
            startPolling()
        } catch (t: Throwable) {
            say(R.string.offline_download_failed, isError = true)
        }
    }

    fun cancelDownload(fileName: String) {
        val id = pending()[fileName] ?: return
        runCatching { downloadManager.remove(id) }
        prefs.edit().remove(fileName).apply()
        store.delete(fileName)
        publishInstalled()
        _state.update { it.copy(downloads = it.downloads - fileName) }
    }

    private fun pending(): Map<String, Long> =
        prefs.all.mapNotNull { (key, value) -> (value as? Long)?.let { key to it } }.toMap()

    private fun startPolling() {
        if (pollJob?.isActive == true) return
        pollJob = scope.launch {
            while (isActive) {
                val keepGoing = withContext(Dispatchers.IO) { pollOnce() }
                if (!keepGoing) break
                delay(POLL_MS)
            }
        }
    }

    /** Verdadeiro enquanto ainda ha download em andamento. */
    private suspend fun pollOnce(): Boolean {
        val progress = HashMap<String, DownloadProgress>()
        for ((fileName, id) in pending()) {
            val status = queryStatus(id)
            when {
                status == null -> {
                    // Removido de fora (pelo usuario nas notificacoes, por exemplo).
                    prefs.edit().remove(fileName).apply()
                    store.delete(fileName)
                }

                status.status == DownloadManager.STATUS_SUCCESSFUL -> finishDownload(fileName)
                status.status == DownloadManager.STATUS_FAILED -> failDownload(fileName, id, status.reason)
                else -> progress[fileName] = DownloadProgress(
                    downloaded = status.downloaded,
                    total = status.total,
                    paused = status.status == DownloadManager.STATUS_PAUSED
                )
            }
        }
        _state.update { it.copy(downloads = progress, installed = store.list(pending().keys)) }
        return progress.isNotEmpty()
    }

    private suspend fun finishDownload(fileName: String) {
        prefs.edit().remove(fileName).apply()
        val file = store.fileFor(fileName)
        if (store.detect(file) == null) {
            file.delete()
            say(R.string.offline_download_invalid, isError = true)
            return
        }
        selectIfNone(fileName)
        say(R.string.offline_download_done, fileName)
    }

    private fun failDownload(fileName: String, id: Long, reason: Int) {
        prefs.edit().remove(fileName).apply()
        runCatching { downloadManager.remove(id) }
        store.delete(fileName)
        // Quando o motivo e um erro HTTP, o Download Manager devolve o proprio codigo.
        if (reason == 401 || reason == 403) {
            say(R.string.offline_download_denied, isError = true)
        } else {
            say(R.string.offline_download_failed, isError = true)
        }
    }

    private class DownloadStatus(val status: Int, val downloaded: Long, val total: Long, val reason: Int)

    private fun queryStatus(id: Long): DownloadStatus? {
        val cursor = downloadManager.query(DownloadManager.Query().setFilterById(id)) ?: return null
        return cursor.use { c ->
            if (!c.moveToFirst()) {
                null
            } else {
                DownloadStatus(
                    status = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)),
                    downloaded = c.getLong(
                        c.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    ),
                    total = c.getLong(c.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)),
                    reason = c.getInt(c.getColumnIndexOrThrow(DownloadManager.COLUMN_REASON))
                )
            }
        }
    }

    // --- Testar -----------------------------------------------------------

    /** Carrega o modelo escolhido e faz uma pergunta simples, medindo o tempo. */
    fun test() {
        if (_state.value.testing) return
        scope.launch {
            _state.update { it.copy(testing = true, message = null) }
            val snapshot = settingsRepo.snapshot()
            val file = if (snapshot.offlineModel.isBlank()) null else store.fileFor(snapshot.offlineModel)
            if (file == null || !file.isFile) {
                _state.update { it.copy(testing = false) }
                say(R.string.err_offline_no_model, isError = true)
                return@launch
            }
            val startedAt = System.nanoTime()
            val result = llm.generate(
                OfflineCall(
                    modelPath = file.path,
                    systemInstruction = "Responda exatamente com a palavra OK.",
                    userText = "ping",
                    temperature = 0f,
                    maxOutputTokens = 16
                )
            )
            val seconds = (System.nanoTime() - startedAt) / 1_000_000_000.0
            _state.update { it.copy(testing = false) }
            when (result) {
                is AiResult.Success -> say(
                    R.string.offline_test_ok,
                    String.format(java.util.Locale.getDefault(), "%.1f", seconds),
                    result.value.take(40)
                )

                is AiResult.Failure -> _state.update {
                    it.copy(
                        message = OfflineMessage(
                            res = result.error.messageRes,
                            args = result.error.args,
                            isError = true
                        )
                    )
                }
            }
        }
    }

    /** Primeiro modelo que a pessoa instala ja fica escolhido e ligado. */
    private suspend fun selectIfNone(fileName: String) {
        if (settingsRepo.snapshot().offlineModel.isBlank()) {
            settingsRepo.setOfflineModel(fileName)
            settingsRepo.setAiProvider(AiProvider.OFFLINE)
        }
    }

    private companion object {
        const val PREFS = "offline_downloads"
        const val POLL_MS = 800L
        const val SPACE_MARGIN = 128L * 1024 * 1024
    }
}
