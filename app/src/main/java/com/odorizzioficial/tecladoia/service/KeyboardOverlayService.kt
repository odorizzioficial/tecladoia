package com.odorizzioficial.tecladoia.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.os.SystemClock
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import com.odorizzioficial.tecladoia.AppGraph
import com.odorizzioficial.tecladoia.data.LocaleHelper
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import com.odorizzioficial.tecladoia.data.UpdateChecker
import com.odorizzioficial.tecladoia.data.UpdateError
import com.odorizzioficial.tecladoia.data.UpdateNotifier
import com.odorizzioficial.tecladoia.data.UpdateResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import android.content.BroadcastReceiver
import android.content.IntentFilter
import androidx.core.content.ContextCompat

/**
 * Servico responsavel pela barra flutuante. Ele nao substitui o teclado: o
 * usuario continua com Gboard, Samsung Keyboard ou qualquer outro IME.
 *
 * O servico e usado para tres coisas, todas necessarias para o recurso:
 *  1. saber quando o teclado do sistema esta na tela e qual a altura dele;
 *  2. desenhar a barra em uma janela TYPE_ACCESSIBILITY_OVERLAY acima do teclado;
 *  3. ler e substituir o texto do campo focado quando o usuario pede.
 */
class KeyboardOverlayService : AccessibilityService() {

    override fun attachBaseContext(newBase: Context) {
        // A barra sobre o teclado precisa dos textos no mesmo idioma das telas.
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }


    /**
     * O escopo e criado a cada conexao, nunca reaproveitado: o sistema pode
     * desconectar e reconectar o mesmo servico, e um escopo ja cancelado
     * deixaria a barra viva na tela mas sem receber ajustes nem funcoes.
     */
    private var scope: CoroutineScope? = null
    private var controller: OverlayController? = null
    private var watcher: KeyboardWatcher? = null

    /** Preferencias de protecao, lidas a cada evento sem tocar no DataStore. */
    @Volatile private var protectFinancial = true
    @Volatile private var ignoredApps: Set<String> = emptySet()

    /** Ultimo app (que nao seja teclado nem sistema) visto em primeiro plano. */
    private var foregroundPackage: String? = null
    private var keyboardPackages: Set<String> = emptySet()
    private var keyboardPackagesAt = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Qualquer excecao aqui derruba o processo, e o Android passa a mostrar
        // o servico como "parado" ate o usuario religar. Nunca deixa escapar.
        try {
            connect()
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao conectar o servico", t)
        }
    }

    private fun connect() {
        // Reconexao: derruba o que sobrou da sessao anterior antes de recriar.
        teardown()
        AppGraph.init(this)

        // Excecoes soltas em corrotinas (leitura do campo, rede, janela) caem
        // aqui em vez de matar o app.
        val safety = CoroutineExceptionHandler { _, t ->
            val current = controller
            if (current != null) current.onUnexpectedError(t) else Log.e(TAG, "Erro", t)
        }
        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate + safety)
        scope = sessionScope

        val keyboardWatcher = KeyboardWatcher(this)
        val overlayController = OverlayController(
            service = this,
            bridge = TextFieldBridge(this),
            engine = AppGraph.engine,
            settingsRepo = AppGraph.settings,
            scope = sessionScope
        )
        watcher = keyboardWatcher
        controller = overlayController
        instanceRef = this
        _connected.value = true

        sessionScope.launch { watchForUpdates() }
        registerScreenReceiver()

        sessionScope.launch {
            AppGraph.settings.settings.collect {
                protectFinancial = it.protectFinancialApps
                ignoredApps = it.ignoredApps
                overlayController.updateSettings(it)
                overlayController.setProtectedApp(isForegroundProtected())
            }
        }
        sessionScope.launch {
            AppGraph.prompts.prompts.collect { overlayController.updatePrompts(it) }
        }

        runCatching { overlayController.onKeyboardStateChanged(keyboardWatcher.currentState()) }
            .onFailure { Log.w(TAG, "Falha ao medir o teclado na conexão", it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val watcher = watcher ?: return
        val controller = controller ?: return
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOWS_CHANGED &&
            type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_FOCUSED &&
            type != AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
        ) return

        // Uma excecao aqui derrubaria o servico inteiro e o Android
        // desligaria a acessibilidade do app, por isso o runCatching.
        runCatching {
            trackForegroundApp(event)
            // Em banco ou carteira: barra fora da tela e nenhum conteudo lido.
            val inProtectedApp = isForegroundProtected()
            controller.setProtectedApp(inProtectedApp)
            if (!inProtectedApp) controller.onKeyboardStateChanged(watcher.currentState())
        }.onFailure { Log.w(TAG, "Evento de acessibilidade ignorado", it) }
    }

    private var screenReceiver: BroadcastReceiver? = null

    /**
     * Bloquear e desbloquear a tela pode derrubar a janela da barra. Ao apagar a tela a
     * janela e solta, e ao ligar/desbloquear o servico confere de novo se ha teclado na
     * tela, sem depender de o usuario abrir o app. O app em primeiro plano nao e zerado:
     * a protecao de bancos continua valendo para o que estava aberto.
     */
    private fun registerScreenReceiver() {
        unregisterScreenReceiver()
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    Intent.ACTION_SCREEN_OFF -> runCatching { controller?.onScreenOff() }
                    Intent.ACTION_SCREEN_ON, Intent.ACTION_USER_PRESENT -> scope?.launch {
                        delay(RESYNC_DELAY_MS)
                        runCatching {
                            val current = controller ?: return@launch
                            val keyboardWatcher = watcher ?: return@launch
                            val inProtectedApp = isForegroundProtected()
                            current.setProtectedApp(inProtectedApp)
                            if (!inProtectedApp) current.onKeyboardStateChanged(keyboardWatcher.currentState())
                        }
                    }
                }
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatching {
            ContextCompat.registerReceiver(this, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            screenReceiver = receiver
        }.onFailure { Log.w(TAG, "Nao foi possivel acompanhar a tela", it) }
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let { runCatching { unregisterReceiver(it) } }
        screenReceiver = null
    }

    /**
     * Procura versao nova no GitHub de tempos em tempos e avisa por notificacao, mesmo
     * com o app fechado: o servico de acessibilidade ja fica vivo enquanto o app esta em
     * uso, entao nao precisa de agendador nem de permissao extra. So consulta quando
     * passaram 12 horas da ultima verificacao.
     */
    private suspend fun watchForUpdates() {
        val checker = UpdateChecker(this)
        while (true) {
            try {
                val saved = AppGraph.settings.snapshot()
                val due = System.currentTimeMillis() - saved.lastUpdateCheck > UPDATE_INTERVAL_MS
                if (saved.autoUpdateCheck && due) {
                    val result = checker.check()
                    val offline = result is UpdateResult.Failure && result.error == UpdateError.NETWORK
                    if (!offline) AppGraph.settings.setLastUpdateCheck(System.currentTimeMillis())
                    if (result is UpdateResult.Available) {
                        UpdateNotifier.notifyOnce(this, AppGraph.settings, result.info)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                Log.w(TAG, "Verificacao de atualizacao ignorada", t)
            }
            delay(UPDATE_WATCH_PERIOD_MS)
        }
    }

    /**
     * Descobre qual app esta na frente usando so o nome do pacote que vem no
     * evento, sem abrir o conteudo da tela. Teclado e barras do sistema nao
     * contam: eles aparecem por cima de qualquer app.
     */
    private fun trackForegroundApp(event: AccessibilityEvent) {
        val type = event.eventType
        if (type != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            type != AccessibilityEvent.TYPE_VIEW_FOCUSED
        ) return
        val pkg = event.packageName?.toString()
        if (pkg.isNullOrEmpty() || isSystemOrKeyboard(pkg)) return
        foregroundPackage = pkg
    }

    private fun isForegroundProtected(): Boolean =
        SensitiveApps.isProtected(foregroundPackage, protectFinancial, ignoredApps)

    private fun isSystemOrKeyboard(pkg: String): Boolean {
        if (pkg == "android" || pkg == "com.android.systemui") return true
        val now = SystemClock.elapsedRealtime()
        if (now - keyboardPackagesAt > KEYBOARD_PACKAGES_TTL_MS) {
            keyboardPackages = loadKeyboardPackages()
            keyboardPackagesAt = now
        }
        return pkg in keyboardPackages
    }

    private fun loadKeyboardPackages(): Set<String> = runCatching {
        val manager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        manager.enabledInputMethodList.map { it.packageName }.toSet()
    }.getOrDefault(emptySet())

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        runCatching {
            controller?.onConfigurationChanged()
            watcher?.let { controller?.onKeyboardStateChanged(it.currentState()) }
        }.onFailure { Log.w(TAG, "Falha ao reposicionar após mudança de tela", it) }
    }

    override fun onInterrupt() {
        controller?.onCollapse()
    }

    override fun onUnbind(intent: Intent?): Boolean {
        teardown()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        teardown()
        super.onDestroy()
    }

    /** Recebe o resultado da caixa de permissao do microfone. */
    fun onMicPermissionResult(granted: Boolean) {
        controller?.onMicPermissionResult(granted)
    }

    private fun teardown() {
        unregisterScreenReceiver()
        runCatching { controller?.destroy() }
        controller = null
        watcher = null
        if (instanceRef === this) instanceRef = null
        _connected.value = false
        scope?.cancel()
        scope = null
    }

    companion object {
        private const val TAG = "KeyboardOverlayService"
        private const val KEYBOARD_PACKAGES_TTL_MS = 30_000L
        private const val RESYNC_DELAY_MS = 500L
        private const val UPDATE_INTERVAL_MS = 12 * 60 * 60 * 1000L
        private const val UPDATE_WATCH_PERIOD_MS = 30 * 60 * 1000L

        private val _connected = MutableStateFlow(false)

        /** Observado pela interface para mostrar o estado real do servico. */
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        @Volatile
        private var instanceRef: KeyboardOverlayService? = null

        val instance: KeyboardOverlayService? get() = instanceRef
    }
}
