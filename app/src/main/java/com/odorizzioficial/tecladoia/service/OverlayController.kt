package com.odorizzioficial.tecladoia.service

import android.Manifest
import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.ai.AiEngine
import com.odorizzioficial.tecladoia.data.SettingsRepository
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.domain.Tone
import com.odorizzioficial.tecladoia.ui.MainActivity
import com.odorizzioficial.tecladoia.ui.MicPermissionActivity
import com.odorizzioficial.tecladoia.ui.overlay.OverlayRoot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Dono da janela flutuante. Cria a View somente quando o teclado aparece,
 * mantem a barra ancorada imediatamente acima dele e devolve o foco ao teclado
 * (a janela nao e focavel, entao digitar continua funcionando normalmente).
 */
class OverlayController(
    private val service: AccessibilityService,
    private val bridge: TextFieldBridge,
    private val engine: AiEngine,
    private val settingsRepo: SettingsRepository,
    private val scope: CoroutineScope
) : OverlayActions, VoiceInputController.Callbacks {

    private val windowManager =
        service.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    /** Fica ligado quando o usuario encolhe a barra para ler a tela do app. */
    private var minimized = false

    /** Deslocamento livre aplicado pelo usuario ao arrastar a janela. */
    private var offsetX = 0
    private var offsetY = 0

    /** A posicao salva e lida uma vez por sessao, para nao anular o arrasto. */
    /** Texto que estava no campo quando o ditado comecou. */
    /** Ultima acao executada, para o botao "Tentar novamente". */
    private var lastRun: (suspend (String) -> AiResult<String>)? = null
    private var lastLabel = ""

    private var voiceBase = ""

    /** Ultimo trecho que o ditado escreveu ao vivo no campo. */
    private var liveTranscript = ""

    /** Trecho cru que ja passou pela correcao e o resultado dela. */
    private var polishedFor = ""
    private var polishedText = ""
    private var livePolishJob: Job? = null

    private var offsetsLoaded = false
    private var saveOffsetJob: Job? = null
    private var hideJob: Job? = null

    private val _state = MutableStateFlow(OverlayUiState())
    val state: StateFlow<OverlayUiState> = _state

    private val voice = VoiceInputController(service, this)

    private var rootView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var params: WindowManager.LayoutParams? = null

    private var keyboard = KeyboardState()
    private var undoText: String? = null
    private var timerJob: Job? = null
    private var pendingVoiceRequest = false

    // --- Ciclo de vida / visibilidade ------------------------------------

    fun updateSettings(settings: AppSettings) {
        if (!offsetsLoaded) {
            offsetsLoaded = true
            offsetX = settings.barOffsetX
            offsetY = settings.barOffsetY
            // Cria a janela ja escondida: a primeira abertura do teclado nao
            // precisa montar ComposeView, lifecycle e window de uma vez.
            if (settings.autoBar) {
                ensureBar()
                setBarVisible(false)
            }
        }
        _state.update { it.copy(settings = settings) }
        refreshVisibility()
    }

    fun updatePrompts(prompts: List<CustomPrompt>) {
        _state.update { it.copy(prompts = prompts) }
    }

    fun onKeyboardStateChanged(newState: KeyboardState) {
        if (newState == keyboard && rootView != null) {
            // Eventos de acessibilidade chegam em rajada; sem estado novo nao
            // ha nada para recalcular.
            refreshVisibility()
            return
        }

        /*
         * Teclado descendo: os eventos chegam com alturas intermediarias, e
         * acompanhar cada uma fazia a barra "escorregar" aos pulos atras do
         * teclado. Ao detectar que a altura diminuiu, a barra some de uma vez.
         */
        val closing = keyboard.visible &&
            newState.heightPx < keyboard.heightPx - CLOSING_TOLERANCE_PX
        val heightChanged = newState.heightPx != keyboard.heightPx
        keyboard = newState
        if (closing) {
            setBarVisible(false)
            return
        }
        refreshVisibility()
        if (heightChanged) reposition()
    }

    fun onConfigurationChanged() {
        // A altura do teclado muda com orientacao e redimensionamento de janela.
        reposition()
    }

    fun destroy() {
        hideJob?.cancel()
        livePolishJob?.cancel()
        saveOffsetJob?.cancel()
        timerJob?.cancel()
        voice.release()
        removeBar()
    }

    private fun refreshVisibility() {
        val settings = _state.value.settings
        val shouldShow = when {
            !settings.autoBar -> false
            // Teclado na tela basta: alguns apps (busca, WebView, campos em
            // Compose) nao expoem um no editavel, e antes a barra nem aparecia.
            keyboard.visible -> true
            !settings.hideWithKeyboard -> bridge.hasEditableFocus()
            else -> false
        }
        if (shouldShow) {
            ensureBar()
            setBarVisible(true)
        } else {
            setBarVisible(false)
        }
    }

    /**
     * A janela e criada uma unica vez e depois apenas escondida. Recriar a
     * ComposeView a cada abertura de teclado era o que travava a animacao.
     */
    private fun ensureBar() {
        if (rootView != null) {
            reposition()
            return
        }
        val owner = OverlayLifecycleOwner().apply { onCreate() }
        val view = ComposeView(service).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setContent { OverlayRoot(controller = this@OverlayController) }
        }
        owner.attachTo(view)

        val layout = WindowManager.LayoutParams(
            if (minimized) {
                WindowManager.LayoutParams.WRAP_CONTENT
            } else {
                WindowManager.LayoutParams.MATCH_PARENT
            },
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
            // NOT_FOCUSABLE mantem o foco no teclado e, junto com NOT_TOUCH_MODAL,
            // deixa todo toque fora da janela chegar no app de baixo.
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = offsetX
            y = baseY()
            windowAnimations = 0
        }

        try {
            windowManager.addView(view, layout)
            owner.onResume()
            rootView = view
            lifecycleOwner = owner
            params = layout
        } catch (t: Throwable) {
            Log.e(TAG, "Não foi possível adicionar a barra", t)
            owner.onDestroy()
        }
    }

    /** Esconde sem destruir: o proximo teclado reaproveita a mesma janela. */
    /** Altura utilizavel acima do teclado, em pixels. */
    private fun availableHeightPx(): Int {
        val metrics = service.resources.displayMetrics
        return (metrics.heightPixels - keyboard.heightPx).coerceAtLeast(0)
    }

    private fun setBarVisible(visible: Boolean) {
        val view = rootView ?: return
        hideJob?.cancel()
        if (visible) {
            if (view.visibility != View.VISIBLE) view.visibility = View.VISIBLE
            _state.update {
                it.copy(availableHeightPx = availableHeightPx(), barVisible = true)
            }
            reposition()
            return
        }

        if (!_state.value.barVisible && view.visibility == View.GONE) return
        // Dispara a saida animada e so esconde a janela quando ela termina.
        _state.update { it.copy(barVisible = false) }
        hideJob = scope.launch {
            delay(exitDelayMs())
            rootView?.visibility = View.GONE
            resetTransientState()
        }
    }

    /** Tempo de saida: zero quando o usuario desligou as animacoes. */
    private fun exitDelayMs(): Long =
        if (_state.value.settings.animationStyle == AnimationStyle.NONE) 0L else EXIT_ANIM_MS

    private fun removeBar() {
        val view = rootView ?: return
        rootView = null
        params = null
        runCatching { windowManager.removeViewImmediate(view) }
        lifecycleOwner?.onDestroy()
        lifecycleOwner = null
        resetTransientState()
    }

    /** Altura padrao: logo acima do teclado, com uma folga para nao colar nele. */
    private fun baseY(): Int = (keyboard.heightPx + GAP_PX + offsetY).coerceAtLeast(0)

    private fun reposition() {
        val view = rootView ?: return
        val layout = params ?: return
        val targetWidth = if (minimized) {
            WindowManager.LayoutParams.WRAP_CONTENT
        } else {
            WindowManager.LayoutParams.MATCH_PARENT
        }
        val targetY = baseY()
        if (layout.y == targetY && layout.x == offsetX && layout.width == targetWidth) return
        layout.y = targetY
        layout.x = offsetX
        layout.width = targetWidth
        runCatching { windowManager.updateViewLayout(view, layout) }
    }

    /** Limites de tela para o arrasto nao jogar a barra para fora. */
    private fun clampOffsets() {
        val metrics = service.resources.displayMetrics
        val barWidth = rootView?.width ?: 0
        val barHeight = rootView?.height ?: 0
        val maxX = (metrics.widthPixels - barWidth).coerceAtLeast(0)
        val maxUp = (metrics.heightPixels - barHeight - keyboard.heightPx - GAP_PX)
            .coerceAtLeast(0)
        offsetX = offsetX.coerceIn(0, maxX)
        offsetY = offsetY.coerceIn(-(keyboard.heightPx + GAP_PX), maxUp)
    }

    private fun resetTransientState() {
        timerJob?.cancel()
        voice.cancel()
        _state.update {
            it.copy(
                mode = if (minimized) OverlayMode.MINI else OverlayMode.COLLAPSED,
                customChips = false,
                actionLabel = "",
                busy = false,
                errorMessage = null,
                voice = VoiceUiState()
            )
        }
    }

    private fun haptic() {
        if (!_state.value.settings.haptics) return
        rootView?.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
    }

    // --- Acoes da interface ----------------------------------------------

    override fun onToggleChipPage() {
        haptic()
        val wasMinimized = minimized
        minimized = false
        if (wasMinimized) reposition()
        // Nenhum painel abre: a barra segue no mesmo lugar e apenas troca a
        // lista de atalhos entre as funcoes padrao e as do usuario.
        _state.update {
            it.copy(
                mode = OverlayMode.COLLAPSED,
                customChips = !it.customChips,
                errorMessage = null
            )
        }
    }

    override fun onMinimize() {
        haptic()
        minimized = true
        timerJob?.cancel()
        voice.cancel()
        _state.update {
            it.copy(mode = OverlayMode.MINI, errorMessage = null, voice = VoiceUiState())
        }
        // A janela encolhe junto: em tela cheia ela cobriria o botao de enviar
        // do app de baixo, mesmo com a area transparente.
        reposition()
    }

    override fun onRestoreBar() {
        haptic()
        minimized = false
        _state.update { it.copy(mode = OverlayMode.COLLAPSED, errorMessage = null) }
        reposition()
    }

    override fun onDrag(dx: Float, dy: Float) {
        offsetX += dx.toInt()
        // Na tela, y cresce para baixo; a janela usa gravidade BOTTOM, entao o
        // deslocamento vertical entra invertido.
        offsetY -= dy.toInt()
        clampOffsets()
        reposition()
        persistOffsets()
    }

    override fun onResetPosition() {
        haptic()
        offsetX = 0
        offsetY = 0
        reposition()
        persistOffsets()
    }

    /** Salva a posicao logo depois do arrasto parar, nao a cada pixel. */
    private fun persistOffsets() {
        val x = offsetX
        val y = offsetY
        saveOffsetJob?.cancel()
        saveOffsetJob = scope.launch {
            delay(400)
            settingsRepo.setBarOffset(x, y)
        }
    }

    override fun onCollapse() {
        haptic()
        timerJob?.cancel()
        voice.cancel()
        _state.update {
            it.copy(
                mode = OverlayMode.COLLAPSED,
                errorMessage = null,
                voice = VoiceUiState()
            )
        }
    }

    override fun onAction(action: AiAction) {
        // Enquanto uma acao roda, toques repetidos sao ignorados: um toque
        // acidental nao deve virar tres chamadas a Gemini.
        if (_state.value.busy) return
        haptic()
        if (action == AiAction.TONE) {
            _state.update { it.copy(mode = OverlayMode.TONE, errorMessage = null) }
            return
        }
        val instruction = if (action == AiAction.REWRITE) DEFAULT_REWRITE else ""
        execute(service.getString(action.labelRes)) { text ->
            engine.run(action = action, text = text, instruction = instruction)
        }
    }

    override fun onCustomPrompt(prompt: CustomPrompt) {
        if (_state.value.busy) return
        haptic()
        execute(prompt.name) { text -> engine.runCustom(prompt, text) }
    }

    override fun onRetry() {
        if (_state.value.busy) return
        val again = lastRun ?: return
        haptic()
        execute(lastLabel, again)
    }

    override fun onOpenTonePicker() {
        haptic()
        _state.update { it.copy(mode = OverlayMode.TONE, errorMessage = null) }
    }

    override fun onToneSelected(tone: Tone) {
        haptic()
        _state.update { it.copy(selectedTone = tone) }
        execute(service.getString(R.string.overlay_tone_label, service.getString(tone.labelRes))) { text ->
            engine.run(action = AiAction.TONE, text = text, tone = tone)
        }
    }

    override fun onUndo() {
        haptic()
        val previous = undoText ?: return
        when (bridge.replaceFocusedText(previous)) {
            is AiResult.Success -> {
                undoText = null
                _state.update { it.copy(canUndo = false, errorMessage = null) }
            }

            is AiResult.Failure -> showMessage(service.getString(R.string.overlay_undo_failed))
        }
    }

    override fun onDismissError() {
        _state.update { it.copy(errorMessage = null) }
    }

    override fun onOpenApp() {
        val intent = Intent(service, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { service.startActivity(intent) }
    }

    override fun onOpenAppFunctions() {
        haptic()
        val intent = Intent(service, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            .putExtra(MainActivity.EXTRA_OPEN_TAB, MainActivity.TAB_PROMPTS)
        runCatching { service.startActivity(intent) }
    }

    /**
     * Roda a acao e ja troca o texto do campo. Nenhum painel de confirmacao: o
     * atalho aplica na hora e a barra passa a oferecer Desfazer. O painel so
     * abre quando o usuario toca no chip de IA.
     */
    private fun execute(label: String, block: suspend (String) -> AiResult<String>) {
        lastRun = block
        lastLabel = label
        val startedAt = System.nanoTime()
        when (val read = bridge.readFocusedText()) {
            is AiResult.Failure -> showError(read.error)
            is AiResult.Success -> {
                val source = read.value
                val readAt = System.nanoTime()
                // Feedback imediato: o rotulo da acao e o indicador entram antes
                // de qualquer trabalho de rede.
                _state.update {
                    it.copy(
                        mode = if (it.mode == OverlayMode.MINI) it.mode else OverlayMode.COLLAPSED,
                        actionLabel = label,
                        busy = true,
                        canRetry = false,
                        errorMessage = null
                    )
                }
                scope.launch {
                    when (val result = block(source)) {
                        is AiResult.Success -> {
                            val answeredAt = System.nanoTime()
                            applyResult(source, result.value)
                            logStages(label, startedAt, readAt, answeredAt)
                        }

                        is AiResult.Failure -> showError(result.error)
                    }
                }
            }
        }
    }

    /** Tempo de cada etapa, para achar o gargalo sem adivinhacao. */
    private fun logStages(label: String, startedAt: Long, readAt: Long, answeredAt: Long) {
        val readMs = (readAt - startedAt) / 1_000_000
        val aiMs = (answeredAt - readAt) / 1_000_000
        val applyMs = (System.nanoTime() - answeredAt) / 1_000_000
        Log.d(
            TAG,
            "$label: leitura=${readMs}ms IA=${aiMs}ms aplicação=${applyMs}ms " +
                "total=${readMs + aiMs + applyMs}ms"
        )
    }

    /** Substitui o campo focado pelo texto da IA e habilita o Desfazer. */
    private fun applyResult(original: String, result: String) {
        val clean = result.trim()
        if (clean.isBlank()) {
            showError(AiError.EmptyResponse)
            return
        }
        when (val outcome = bridge.replaceFocusedText(clean)) {
            is AiResult.Success -> {
                undoText = original
                _state.update {
                    it.copy(busy = false, canRetry = false, canUndo = true, errorMessage = null)
                }
                haptic()
            }

            is AiResult.Failure -> showError(
                outcome.error,
                suffix = service.getString(R.string.overlay_clipboard_suffix)
            )
        }
    }

    private fun showError(error: AiError, suffix: String = "") {
        _state.update {
            it.copy(
                busy = false,
                canRetry = error.retryable && lastRun != null,
                errorMessage = error.message(service) + suffix
            )
        }
    }

    /** Mensagens que nao vem de um AiError (permissao de microfone, etc.). */
    private fun showMessage(message: String) {
        _state.update { it.copy(busy = false, canRetry = false, errorMessage = message) }
    }

    // --- Voz --------------------------------------------------------------

    override fun onVoiceStart() {
        haptic()
        val granted = ContextCompat.checkSelfPermission(
            service,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) {
            pendingVoiceRequest = true
            val intent = Intent(service, MicPermissionActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { service.startActivity(intent) }
            showMessage(service.getString(R.string.overlay_mic_grant))
            return
        }
        if (!voice.isAvailable()) {
            showMessage(AiError.VoiceUnavailable.message(service))
            return
        }
        // Base capturada uma vez: o que ja estava escrito no campo. O ditado e
        // acrescentado a ela a cada parcial, entao o texto aparece em tempo real.
        voiceBase = bridge.dictationBaseText()
        liveTranscript = ""
        polishedFor = ""
        polishedText = ""
        livePolishJob?.cancel()
        _state.update {
            it.copy(
                mode = OverlayMode.VOICE,
                errorMessage = null,
                voice = VoiceUiState(stage = VoiceStage.RECORDING)
            )
        }
        voice.start(_state.value.settings.language)
        startTimer()
    }

    override fun onVoiceStop() {
        haptic()
        timerJob?.cancel()
        // Sem tela de "transcrevendo": o texto ja esta no campo, a barra volta
        // ao normal na hora e a correcao roda com o indicador discreto da barra.
        _state.update {
            it.copy(
                mode = OverlayMode.COLLAPSED,
                actionLabel = service.getString(R.string.overlay_correcting),
                busy = true,
                voice = VoiceUiState()
            )
        }
        voice.stop()
    }

    override fun onVoiceCancel() {
        haptic()
        timerJob?.cancel()
        livePolishJob?.cancel()
        voice.cancel()
        // Desfaz o que o ditado tinha escrito ao vivo.
        if (liveTranscript.isNotEmpty()) {
            bridge.replaceFocusedText(voiceBase)
        }
        liveTranscript = ""
        _state.update {
            it.copy(mode = OverlayMode.COLLAPSED, busy = false, voice = VoiceUiState())
        }
    }

    /** Chamado pela Activity transparente depois da caixa de permissao. */
    fun onMicPermissionResult(granted: Boolean) {
        if (!pendingVoiceRequest) return
        pendingVoiceRequest = false
        if (granted) {
            onDismissError()
            onVoiceStart()
        } else {
            showMessage(service.getString(R.string.overlay_mic_denied))
        }
    }

    override fun onRecordingStarted() {
        _state.update { it.copy(voice = it.voice.copy(stage = VoiceStage.RECORDING)) }
    }

    override fun onPartialTranscript(text: String) {
        _state.update { it.copy(voice = it.voice.copy(transcript = text)) }
        writeLive(text)
    }

    /**
     * Escreve o ditado no campo enquanto o usuario fala, ja usando a parte que
     * a IA corrigiu. O trecho novo entra cru e e corrigido logo depois.
     */
    private fun writeLive(text: String) {
        if (text.isBlank() || text == liveTranscript) return
        liveTranscript = text
        bridge.replaceFocusedText(joinWithBase(displayFor(text)))
        scheduleLivePolish(text)
    }

    /** Junta o trecho corrigido com o que ainda esta cru. */
    private fun displayFor(raw: String): String {
        if (polishedText.isEmpty() || !raw.startsWith(polishedFor)) return raw.trim()
        val tail = raw.removePrefix(polishedFor).trim()
        return if (tail.isEmpty()) polishedText else "$polishedText $tail"
    }

    /**
     * Correcao em tempo real: 700 ms depois da ultima fala nova, o texto ate
     * ali vai para a IA e volta pontuado, sem esperar o usuario concluir.
     */
    private fun scheduleLivePolish(raw: String) {
        livePolishJob?.cancel()
        livePolishJob = scope.launch {
            delay(LIVE_POLISH_DELAY_MS)
            if (_state.value.mode != OverlayMode.VOICE) return@launch
            val result = engine.polishTranscript(raw)
            if (result !is AiResult.Success) return@launch
            // Se a fala continuou, o trecho corrigido segue valido como prefixo.
            if (!liveTranscript.startsWith(raw)) return@launch
            polishedFor = raw
            polishedText = result.value.trim()
            bridge.replaceFocusedText(joinWithBase(displayFor(liveTranscript)))
        }
    }

    private fun joinWithBase(addition: String): String {
        if (voiceBase.isEmpty()) return addition
        val separator = if (voiceBase.endsWith(" ") || voiceBase.endsWith("\n")) "" else " "
        return voiceBase + separator + addition
    }

    override fun onFinalTranscript(text: String) {
        timerJob?.cancel()
        livePolishJob?.cancel()
        // O texto ja esta no campo (corrigido ate o penultimo trecho); aqui
        // entra a passada final sobre a frase inteira.
        liveTranscript = text
        bridge.replaceFocusedText(joinWithBase(displayFor(text)))
        scope.launch {
            var polishError: String? = null
            // Duas tentativas de correcao antes de aceitar o texto cru: o
            // polimento dedicado e, se falhar, a acao Corrigir.
            val polished = when (val result = engine.polishTranscript(text)) {
                is AiResult.Success -> result.value
                is AiResult.Failure -> when (
                    val retry = engine.run(action = AiAction.FIX, text = text)
                ) {
                    is AiResult.Success -> retry.value
                    is AiResult.Failure -> {
                        polishError = retry.error.message(service)
                        text
                    }
                }
            }
            when (val outcome = bridge.replaceFocusedText(joinWithBase(polished.trim()))) {
                is AiResult.Success -> {
                    undoText = voiceBase
                    liveTranscript = ""
                    polishedFor = ""
                    polishedText = ""
                    _state.update {
                        it.copy(
                            mode = if (it.mode == OverlayMode.MINI) it.mode else OverlayMode.COLLAPSED,
                            busy = false,
                            canUndo = true,
                            errorMessage = polishError,
                            voice = VoiceUiState(stage = VoiceStage.DONE)
                        )
                    }
                }

                is AiResult.Failure -> showError(
                    outcome.error,
                    suffix = service.getString(R.string.overlay_clipboard_suffix)
                )
            }
        }
    }

    override fun onVoiceError(message: String) {
        timerJob?.cancel()
        _state.update {
            it.copy(
                busy = false,
                voice = it.voice.copy(stage = VoiceStage.ERROR, message = message)
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            var seconds = 0
            while (true) {
                delay(1_000)
                seconds += 1
                _state.update { it.copy(voice = it.voice.copy(seconds = seconds)) }
            }
        }
    }

    private companion object {
        const val TAG = "OverlayController"

        /** Folga entre a barra e o teclado, para a ultima mensagem respirar. */
        const val GAP_PX = 6

        /** Pausa na fala que dispara a correcao do trecho ja dito. */
        const val LIVE_POLISH_DELAY_MS = 700L

        /** Variacao de altura que ja conta como teclado fechando. */
        const val CLOSING_TOLERANCE_PX = 24

        /** Duracao da saida animada da barra, alinhada ao AnimatedVisibility. */
        const val EXIT_ANIM_MS = 220L
        const val DEFAULT_REWRITE =
            "Reescreva o texto com outra construção, mantendo o significado, " +
                "o idioma e um comprimento parecido com o original."
    }
}
