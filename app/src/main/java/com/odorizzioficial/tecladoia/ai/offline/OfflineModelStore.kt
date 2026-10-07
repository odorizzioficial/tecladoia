package com.odorizzioficial.tecladoia.ai.offline

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException

/** Um modelo ja guardado no aparelho. */
data class InstalledModel(val fileName: String, val sizeBytes: Long)

/** Formatos de modelo que o app sabe rodar, cada um com o seu motor. */
enum class ModelFormat(val extension: String) {
    /** Motor LiteRT-LM da Google (Gemma, Qwen...). */
    LITERTLM(".litertlm"),

    /** Motor llama.cpp: o formato mais comum de modelo aberto na internet. */
    GGUF(".gguf")
}

/** Resultado de importar um arquivo escolhido pela pessoa. */
sealed interface ImportResult {
    data class Success(val fileName: String) : ImportResult
    data object UnsupportedFormat : ImportResult
    data object NoSpace : ImportResult
    data object Failed : ImportResult
}

/**
 * Pasta e regras dos modelos de IA offline. Os arquivos ficam no armazenamento
 * proprio do app (sem pedir permissao nenhuma) e somem junto com ele ao
 * desinstalar. Aceita .litertlm (motor LiteRT-LM) e .gguf (motor llama.cpp). O
 * formato e reconhecido pelo cabecalho do arquivo, nao pelo nome: um arquivo
 * renomeado ou sem extensao tambem funciona se o conteudo for valido.
 */
class OfflineModelStore(context: Context) {

    private val appContext = context.applicationContext
    private val external: File? = appContext.getExternalFilesDir(DIR_NAME)

    /** Pasta dos modelos: cartao/armazenamento do app, ou o interno se nao houver. */
    val dir: File = (external ?: File(appContext.filesDir, DIR_NAME)).also { it.mkdirs() }

    /** O Android Download Manager so escreve no armazenamento externo do app. */
    val canDownload: Boolean get() = external != null

    fun fileFor(name: String): File = File(dir, sanitize(name))

    /** Modelos prontos, sem os que ainda estao sendo baixados. */
    fun list(downloading: Set<String>): List<InstalledModel> =
        dir.listFiles().orEmpty()
            .filter { file ->
                file.isFile && ModelFormat.entries.any {
                    file.name.endsWith(it.extension, ignoreCase = true)
                }
            }
            .filter { it.name !in downloading }
            .map { InstalledModel(it.name, it.length()) }
            .sortedBy { it.fileName.lowercase() }

    fun delete(name: String): Boolean = fileFor(name).delete()

    fun freeBytes(): Long = runCatching { StatFs(dir.path).availableBytes }.getOrDefault(Long.MAX_VALUE)

    /** Formato do arquivo pelo cabecalho, ou nulo se nao for um modelo conhecido. */
    fun detect(file: File): ModelFormat? = try {
        file.inputStream().use { input ->
            val header = ByteArray(MAGIC_LITERTLM.size)
            val read = readFully(input, header)
            formatOf(header, read)
        }
    } catch (t: Throwable) {
        null
    }

    private fun formatOf(header: ByteArray, read: Int): ModelFormat? = when {
        read >= MAGIC_LITERTLM.size &&
            header.copyOf(MAGIC_LITERTLM.size).contentEquals(MAGIC_LITERTLM) -> ModelFormat.LITERTLM

        read >= MAGIC_GGUF.size &&
            header.copyOf(MAGIC_GGUF.size).contentEquals(MAGIC_GGUF) -> ModelFormat.GGUF

        else -> null
    }

    /**
     * Copia o arquivo escolhido para a pasta do app. Confere o cabecalho antes
     * de gastar espaco, e so renomeia para o nome final quando a copia termina.
     */
    suspend fun importFrom(uri: Uri, onProgress: (copied: Long, total: Long) -> Unit): ImportResult =
        withContext(Dispatchers.IO) {
            val resolver = appContext.contentResolver
            var displayName = "model"
            var total = -1L
            runCatching {
                val cursor = resolver.query(
                    uri,
                    arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
                    null, null, null
                )
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        c.getString(0)?.let { displayName = it }
                        if (!c.isNull(1)) total = c.getLong(1)
                    }
                }
            }
            if (total > 0 && total + SPACE_MARGIN > freeBytes()) return@withContext ImportResult.NoSpace

            val temp = File(dir, "import-${System.nanoTime()}.part")
            try {
                val source = resolver.openInputStream(uri) ?: return@withContext ImportResult.Failed
                var format: ModelFormat? = null
                source.use { input ->
                    val header = ByteArray(MAGIC_LITERTLM.size)
                    val read = readFully(input, header)
                    format = formatOf(header, read)
                    if (format != null) {
                        temp.outputStream().use { out ->
                            out.write(header, 0, read)
                            var copied = read.toLong()
                            val buffer = ByteArray(BUFFER_SIZE)
                            while (true) {
                                ensureActive()
                                val read = input.read(buffer)
                                if (read < 0) break
                                out.write(buffer, 0, read)
                                copied += read
                                onProgress(copied, total)
                            }
                        }
                    }
                }
                val detected = format ?: return@withContext ImportResult.UnsupportedFormat
                val target = uniqueTarget(displayName, detected)
                if (!temp.renameTo(target)) return@withContext ImportResult.Failed
                ImportResult.Success(target.name)
            } catch (e: CancellationException) {
                throw e
            } catch (e: IOException) {
                if (e.message.orEmpty().contains("ENOSPC") ||
                    e.message.orEmpty().contains("No space", ignoreCase = true)
                ) ImportResult.NoSpace else ImportResult.Failed
            } catch (t: Throwable) {
                ImportResult.Failed
            } finally {
                temp.delete()
            }
        }

    /** Nome seguro, com a extensao do formato detectado, sem sobrescrever outro modelo. */
    private fun uniqueTarget(displayName: String, format: ModelFormat): File {
        var base = sanitize(displayName)
        ModelFormat.entries.forEach { base = base.removeSuffix(it.extension).removeSuffix(it.extension.uppercase()) }
        if (base.isBlank()) base = "model"
        var candidate = File(dir, base + format.extension)
        var index = 2
        while (candidate.exists()) {
            candidate = File(dir, "$base-$index${format.extension}")
            index++
        }
        return candidate
    }

    private fun sanitize(name: String): String =
        name.replace(Regex("[^A-Za-z0-9._-]"), "_").trimStart('.')

    private fun readFully(input: java.io.InputStream, target: ByteArray): Int {
        var total = 0
        while (total < target.size) {
            val read = input.read(target, total, target.size - total)
            if (read < 0) break
            total += read
        }
        return total
    }

    companion object {
        const val DIR_NAME = "models"
        private val MAGIC_LITERTLM = "LITERTLM".toByteArray(Charsets.US_ASCII)
        private val MAGIC_GGUF = "GGUF".toByteArray(Charsets.US_ASCII)
        private const val BUFFER_SIZE = 256 * 1024
        private const val SPACE_MARGIN = 64L * 1024 * 1024
    }
}
