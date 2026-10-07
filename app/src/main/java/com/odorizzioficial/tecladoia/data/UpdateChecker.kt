package com.odorizzioficial.tecladoia.data

import android.content.Context
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Uma versao nova encontrada no GitHub. */
data class UpdateInfo(
    val version: String,
    val notes: String,
    /** Endereco do APK anexado ao release, ou a pagina do release se nao houver APK. */
    val downloadUrl: String,
    val pageUrl: String,
    val sizeBytes: Long
)

enum class UpdateError { NETWORK, RATE_LIMIT, NO_RELEASE, INVALID }

sealed interface UpdateResult {
    data object UpToDate : UpdateResult
    data class Available(val info: UpdateInfo) : UpdateResult
    data class Failure(val error: UpdateError) : UpdateResult
}

@Serializable
private data class GhAsset(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
    val size: Long = 0
)

@Serializable
private data class GhRelease(
    @SerialName("tag_name") val tag: String = "",
    val body: String? = null,
    @SerialName("html_url") val pageUrl: String = "",
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GhAsset> = emptyList()
)

/**
 * Procura uma versao nova no GitHub, sem servidor nosso e sem custo: a API
 * publica de releases do repositorio responde a quem pergunta (ate 60 consultas
 * por hora por rede, muito mais do que o app faz). So le o release mais recente
 * marcado como "Latest"; o app nao instala nada sozinho, quem baixa e instala
 * e o navegador, e o Android so aceita o APK se vier assinado com a mesma chave.
 */
class UpdateChecker(context: Context) {

    private val appContext = context.applicationContext
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Versao instalada, como o Android a conhece (versionName do build). */
    fun installedVersion(): String = runCatching {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName
    }.getOrNull().orEmpty()

    suspend fun check(): UpdateResult = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_URL)
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "TecladoIA/${installedVersion()}")
            .build()
        try {
            client.newCall(request).execute().use { response ->
                when {
                    response.code == 404 -> UpdateResult.Failure(UpdateError.NO_RELEASE)
                    response.code == 403 || response.code == 429 ->
                        UpdateResult.Failure(UpdateError.RATE_LIMIT)

                    !response.isSuccessful -> UpdateResult.Failure(UpdateError.NETWORK)
                    else -> parse(response.body?.string().orEmpty())
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            UpdateResult.Failure(UpdateError.NETWORK)
        } catch (e: Exception) {
            UpdateResult.Failure(UpdateError.INVALID)
        }
    }

    private fun parse(body: String): UpdateResult {
        val release = runCatching { json.decodeFromString<GhRelease>(body) }.getOrNull()
            ?: return UpdateResult.Failure(UpdateError.INVALID)
        if (release.draft || release.prerelease || release.tag.isBlank()) {
            return UpdateResult.Failure(UpdateError.NO_RELEASE)
        }
        if (!isNewer(release.tag, installedVersion())) return UpdateResult.UpToDate

        // Prefere o APK de release; qualquer .apk serve se nao houver esse.
        val apks = release.assets.filter { it.name.endsWith(".apk", ignoreCase = true) }
        val apk = apks.firstOrNull { it.name.contains("release", ignoreCase = true) } ?: apks.firstOrNull()
        val page = release.pageUrl.takeIf { isTrusted(it) } ?: RELEASES_URL
        val download = apk?.url?.takeIf { isTrusted(it) } ?: page

        return UpdateResult.Available(
            UpdateInfo(
                version = release.tag.removePrefix("v").removePrefix("V"),
                notes = release.body.orEmpty().trim(),
                downloadUrl = download,
                pageUrl = page,
                sizeBytes = apk?.size ?: 0L
            )
        )
    }

    companion object {
        private const val OWNER = "odorizzioficial"
        private const val REPO = "tecladoia"
        private const val LATEST_URL = "https://api.github.com/repos/$OWNER/$REPO/releases/latest"
        const val REPO_URL = "https://github.com/$OWNER/$REPO"
        const val RELEASES_URL = "$REPO_URL/releases"

        /** So abre enderecos https do proprio GitHub, mesmo que a resposta traga outra coisa. */
        fun isTrusted(url: String): Boolean {
            if (!url.startsWith("https://")) return false
            val host = url.removePrefix("https://").substringBefore('/').lowercase()
            return host == "github.com" || host.endsWith(".github.com") ||
                host.endsWith(".githubusercontent.com")
        }

        /** "v1.3.10" e mais novo que "1.3.9"? Compara numero a numero, ignorando sufixos. */
        fun isNewer(latest: String, installed: String): Boolean {
            val a = numbers(latest)
            val b = numbers(installed)
            if (a.isEmpty()) return false
            for (index in 0 until maxOf(a.size, b.size)) {
                val x = a.getOrElse(index) { 0 }
                val y = b.getOrElse(index) { 0 }
                if (x != y) return x > y
            }
            return false
        }

        private fun numbers(version: String): List<Int> =
            Regex("\\d+").findAll(version.substringBefore('-')).mapNotNull { it.value.toIntOrNull() }.toList()
    }
}
