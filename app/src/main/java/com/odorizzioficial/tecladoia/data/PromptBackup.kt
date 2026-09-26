package com.odorizzioficial.tecladoia.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Resultado de uma exportacao de backup. */
sealed class BackupResult {
    data class Success(val fileName: String, val count: Int) : BackupResult()
    data class Failure(val message: String) : BackupResult()
}

/**
 * Exporta e importa as funcoes personalizadas como um unico arquivo JSON na
 * pasta Downloads do aparelho. E so um espelho local do que ja esta no
 * DataStore (PromptRepository): nao existe servidor nem conta na nuvem.
 */
object PromptBackup {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }
    private const val MIME_TYPE = "application/json"

    private fun newFileName(): String {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        return "tecladoia-funcoes-$stamp.json"
    }

    /**
     * Grava o JSON das funcoes em Downloads. Em Android 10+ usa o MediaStore,
     * sem pedir permissao. Abaixo disso, escreve direto na pasta publica (o
     * chamador e responsavel por garantir WRITE_EXTERNAL_STORAGE ali).
     */
    fun export(context: Context, prompts: List<CustomPrompt>): BackupResult {
        val fileName = newFileName()
        val content = json.encodeToString(prompts)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, MIME_TYPE)
                    put(MediaStore.Downloads.IS_PENDING, 1)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: return BackupResult.Failure("insert")
                resolver.openOutputStream(uri)?.use { stream ->
                    stream.write(content.toByteArray(Charsets.UTF_8))
                } ?: return BackupResult.Failure("stream")
                values.clear()
                values.put(MediaStore.Downloads.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            } else {
                @Suppress("DEPRECATION")
                val downloads =
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloads.exists()) downloads.mkdirs()
                val file = File(downloads, fileName)
                FileOutputStream(file).use { it.write(content.toByteArray(Charsets.UTF_8)) }
            }
            BackupResult.Success(fileName, prompts.size)
        } catch (e: Exception) {
            BackupResult.Failure(e.message ?: "unknown")
        }
    }

    /** Le um arquivo de backup escolhido pelo usuario e devolve a lista de funcoes. */
    fun import(context: Context, uri: Uri): List<CustomPrompt>? = try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val text = stream.bufferedReader(Charsets.UTF_8).readText()
            json.decodeFromString<List<CustomPrompt>>(text).takeIf { it.isNotEmpty() }
        }
    } catch (e: Exception) {
        null
    }
}
