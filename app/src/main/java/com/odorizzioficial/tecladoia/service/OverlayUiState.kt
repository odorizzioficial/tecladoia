package com.odorizzioficial.tecladoia.service

import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.domain.Tone

enum class OverlayMode {
    /** Barra compacta: chip de IA + atalhos + microfone. */
    COLLAPSED,

    /**
     * Barra recolhida em um botao pequeno no canto. Serve para liberar a
     * ultima mensagem do app quando a barra estaria cobrindo o conteudo.
     */
    MINI,

    /** Seletor de tom. */
    TONE,

    /** Gravacao e transcricao de voz, mostradas dentro da propria barra. */
    VOICE
}

enum class VoiceStage { READY, RECORDING, PROCESSING, DONE, ERROR }

data class VoiceUiState(
    val stage: VoiceStage = VoiceStage.READY,
    val seconds: Int = 0,
    val transcript: String = "",
    val message: String = ""
)

data class OverlayUiState(
    val mode: OverlayMode = OverlayMode.COLLAPSED,
    val settings: AppSettings = AppSettings(),
    val prompts: List<CustomPrompt> = emptyList(),
    val actionLabel: String = "",
    /** Chamada a IA em andamento: a barra mostra o progresso sem abrir painel. */
    val busy: Boolean = false,
    /** Verdadeiro quando o ultimo erro admite "Tentar novamente". */
    val canRetry: Boolean = false,
    /**
     * Comanda a entrada e a saida animadas da barra. A janela continua na tela
     * ate a animacao de saida terminar; so depois ela e escondida de fato.
     */
    val barVisible: Boolean = false,
    /** Espaco livre acima do teclado: limita a altura do seletor de tom. */
    val availableHeightPx: Int = 0,
    /**
     * Conjunto de atalhos visivel na barra: falso mostra as funcoes padrao da
     * IA, verdadeiro mostra as funcoes criadas pelo usuario. A barra continua
     * do mesmo tamanho; muda apenas o que ela lista.
     */
    val customChips: Boolean = false,
    val selectedTone: Tone = Tone.PROFESSIONAL,
    val errorMessage: String? = null,
    val canUndo: Boolean = false,
    val voice: VoiceUiState = VoiceUiState()
) {
    val pinnedPrompts: List<CustomPrompt>
        get() = prompts.filter { it.enabled && it.pinned }

    val enabledPrompts: List<CustomPrompt>
        get() = prompts.filter { it.enabled }
}

/** Contrato entre a interface da barra e o controlador que vive no servico. */
interface OverlayActions {
    /** Alterna entre as funcoes padrao e as funcoes do usuario na barra. */
    fun onToggleChipPage()
    fun onCollapse()
    fun onMinimize()
    fun onRestoreBar()

    /** Arrasto da janela flutuante, em pixels de tela. */
    fun onDrag(dx: Float, dy: Float)

    /** Volta a barra para a posicao padrao, colada acima do teclado. */
    fun onResetPosition()
    fun onAction(action: com.odorizzioficial.tecladoia.domain.AiAction)
    fun onCustomPrompt(prompt: CustomPrompt)
    fun onToneSelected(tone: Tone)
    fun onOpenTonePicker()
    fun onUndo()
    fun onDismissError()

    /** Repete a ultima acao quando o erro permite nova tentativa. */
    fun onRetry()
    fun onVoiceStart()
    fun onVoiceStop()
    fun onVoiceCancel()
    fun onOpenApp()

    /** Segurar o chip de IA por alguns segundos abre o app direto na aba Funções. */
    fun onOpenAppFunctions()
}
