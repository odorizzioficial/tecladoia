package com.odorizzioficial.tecladoia.data

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

/** Em que ponto esta a instalacao da atualizacao feita dentro do app. */
sealed interface InstallState {
    data object Idle : InstallState
    data class Downloading(val downloaded: Long, val total: Long) : InstallState

    /** APK entregue ao Android; falta a pessoa confirmar na janela do sistema. */
    data object AwaitingConfirmation : InstallState
    data class Failed(val kind: InstallFailure, val detail: String = "") : InstallState
}

enum class InstallFailure { NETWORK, STORAGE, SIGNATURE, BLOCKED, NO_APK, OTHER }

/**
 * Baixa o APK do release e o entrega ao instalador do Android (PackageInstaller),
 * sem passar pelo navegador. O APK vai direto da internet para a sessao de
 * instalacao: nao ha arquivo solto no aparelho.
 *
 * O Android nao deixa um app comum instalar escondido: a pessoa ainda confirma na
 * janela do sistema, e o sistema so aceita o APK se vier assinado com a mesma
 * chave do app instalado e com versao maior. Na primeira vez a pessoa precisa
 * permitir "instalar apps desconhecidos" para o TecladoIA.
 */
object UpdateInstaller {

    private const val TAG = "UpdateInstaller"
    private const val APK_NAME = "TecladoIA-update.apk"
    private const val REPORT_EVERY_MS = 200L

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _state = MutableStateFlow<InstallState>(InstallState.Idle)
    val state: StateFlow<InstallState> = _state.asStateFlow()

    /** Janela de confirmacao do Android que nao pode abrir (app em segundo plano). */
    @Volatile
    private var pendingConfirm: Intent? = null

    fun reset() {
        pendingConfirm = null
        _state.value = InstallState.Idle
    }

    /** O Android ja deixou o app instalar pacotes? */
    fun canInstall(context: Context): Boolean =
        runCatching { context.packageManager.canRequestPackageInstalls() }.getOrDefault(false)

    /** Reabre a janela de confirmacao quando ela nao chegou a aparecer. */
    fun resumeConfirmation(context: Context): Boolean {
        val intent = pendingConfirm ?: return false
        return try {
            context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            true
        } catch (t: Throwable) {
            Log.w(TAG, "Nao foi possivel reabrir a confirmacao", t)
            false
        }
    }

    suspend fun install(context: Context, info: UpdateInfo) {
        val app = context.applicationContext
        val current = _state.value
        if (current is InstallState.Downloading || current is InstallState.AwaitingConfirmation) return
        if (!info.canInstallInApp) {
            _state.value = InstallState.Failed(InstallFailure.NO_APK)
            return
        }
        pendingConfirm = null
        _state.value = InstallState.Downloading(0L, info.sizeBytes)
        try {
            withContext(Dispatchers.IO) { runInstall(app, info) }
            // O resultado do Android pode chegar antes deste ponto: so troca se nada mudou.
            _state.update { if (it is InstallState.Downloading) InstallState.AwaitingConfirmation else it }
        } catch (e: CancellationException) {
            _state.value = InstallState.Idle
            throw e
        } catch (e: IOException) {
            val noSpace = e.message.orEmpty().contains("ENOSPC") ||
                e.message.orEmpty().contains("No space", ignoreCase = true)
            _state.value = InstallState.Failed(
                if (noSpace) InstallFailure.STORAGE else InstallFailure.NETWORK,
                e.message.orEmpty()
            )
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao instalar a atualizacao", t)
            _state.value = InstallState.Failed(InstallFailure.OTHER, (t.message ?: t.javaClass.simpleName).take(120))
        }
    }

    private suspend fun runInstall(app: Context, info: UpdateInfo) {
        val installer = app.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(app.packageName)
            if (info.sizeBytes > 0) setSize(info.sizeBytes)
        }
        val sessionId = installer.createSession(params)
        val session = installer.openSession(sessionId)
        try {
            download(info, session)
            val callback = Intent(app, UpdateInstallReceiver::class.java)
                .setAction(UpdateInstallReceiver.ACTION)
                .setPackage(app.packageName)
            // O sistema acrescenta o resultado a este intent, por isso precisa ser mutavel.
            val flags = PendingIntent.FLAG_UPDATE_CURRENT or
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val pending = PendingIntent.getBroadcast(app, sessionId, callback, flags)
            session.commit(pending.intentSender)
        } catch (t: Throwable) {
            runCatching { session.abandon() }
            throw t
        } finally {
            runCatching { session.close() }
        }
    }

    /** Copia o APK da internet direto para a sessao de instalacao, contando o progresso. */
    private suspend fun download(info: UpdateInfo, session: PackageInstaller.Session) {
        val request = Request.Builder()
            .url(info.downloadUrl)
            .header("User-Agent", "TecladoIA")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            val body = response.body ?: throw IOException("resposta vazia")
            val total = body.contentLength().takeIf { it > 0 } ?: info.sizeBytes
            session.openWrite(APK_NAME, 0, if (total > 0) total else -1).use { out ->
                body.byteStream().use { input ->
                    val buffer = ByteArray(64 * 1024)
                    var copied = 0L
                    var lastReport = 0L
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val read = input.read(buffer)
                        if (read < 0) break
                        out.write(buffer, 0, read)
                        copied += read
                        val now = System.currentTimeMillis()
                        if (now - lastReport >= REPORT_EVERY_MS) {
                            lastReport = now
                            _state.value = InstallState.Downloading(copied, total)
                        }
                    }
                    session.fsync(out)
                }
            }
        }
    }

    /** Resposta do Android sobre a instalacao (chamada pelo [UpdateInstallReceiver]). */
    fun onInstallResult(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE).orEmpty()
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_INTENT)
                }
                if (confirm != null) {
                    pendingConfirm = confirm
                    _state.value = InstallState.AwaitingConfirmation
                    // Com o app aberto a janela aparece na hora; em segundo plano o botao
                    // "Continuar instalacao" da tela de atualizacao a reabre.
                    runCatching {
                        context.startActivity(Intent(confirm).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.onFailure { Log.w(TAG, "Confirmacao nao abriu agora", it) }
                }
            }

            PackageInstaller.STATUS_SUCCESS -> reset()
            PackageInstaller.STATUS_FAILURE_ABORTED -> reset() // a pessoa cancelou
            PackageInstaller.STATUS_FAILURE_STORAGE ->
                _state.value = InstallState.Failed(InstallFailure.STORAGE, message)

            PackageInstaller.STATUS_FAILURE_INVALID,
            PackageInstaller.STATUS_FAILURE_CONFLICT,
            PackageInstaller.STATUS_FAILURE_INCOMPATIBLE ->
                _state.value = InstallState.Failed(InstallFailure.SIGNATURE, message)

            PackageInstaller.STATUS_FAILURE_BLOCKED ->
                _state.value = InstallState.Failed(InstallFailure.BLOCKED, message)

            else -> _state.value = InstallState.Failed(InstallFailure.OTHER, message)
        }
    }
}
