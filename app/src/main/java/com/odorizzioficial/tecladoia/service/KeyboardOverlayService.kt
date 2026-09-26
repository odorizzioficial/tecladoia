package com.odorizzioficial.tecladoia.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.odorizzioficial.tecladoia.AppGraph
import com.odorizzioficial.tecladoia.data.LocaleHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

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

    override fun onServiceConnected() {
        super.onServiceConnected()
        // Reconexao: derruba o que sobrou da sessao anterior antes de recriar.
        teardown()
        AppGraph.init(this)

        val sessionScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
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

        sessionScope.launch {
            AppGraph.settings.settings.collect { overlayController.updateSettings(it) }
        }
        sessionScope.launch {
            AppGraph.prompts.prompts.collect { overlayController.updatePrompts(it) }
        }

        runCatching { overlayController.onKeyboardStateChanged(keyboardWatcher.currentState()) }
            .onFailure { Log.w(TAG, "Falha ao medir o teclado na conexão", it) }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val watcher = watcher ?: return
        val controller = controller ?: return
        when (event?.eventType) {
            AccessibilityEvent.TYPE_WINDOWS_CHANGED,
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED,
            AccessibilityEvent.TYPE_VIEW_FOCUSED,
            AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED ->
                // Uma excecao aqui derrubaria o servico inteiro e o Android
                // desligaria a acessibilidade do app, por isso o runCatching.
                runCatching { controller.onKeyboardStateChanged(watcher.currentState()) }
                    .onFailure { Log.w(TAG, "Evento de acessibilidade ignorado", it) }

            else -> Unit
        }
    }

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

        private val _connected = MutableStateFlow(false)

        /** Observado pela interface para mostrar o estado real do servico. */
        val connected: StateFlow<Boolean> = _connected.asStateFlow()

        @Volatile
        private var instanceRef: KeyboardOverlayService? = null

        val instance: KeyboardOverlayService? get() = instanceRef
    }
}
