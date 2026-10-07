package com.odorizzioficial.tecladoia.ai

import android.os.SystemClock
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/** Texto final do ditado e, se a correcao falhou, o motivo (o texto vem cru). */
data class DictationResult(val text: String, val error: AiError?)

/**
 * Corrige o ditado enquanto a pessoa fala, trecho por trecho.
 *
 * O texto cru aparece na hora, a cada parcial. A cada pausa na fala, so o que
 * ainda nao foi corrigido vai para a IA e volta pontuado, e o trecho corrigido
 * substitui o cru. Corrigir so o trecho novo (e nao a frase inteira de novo)
 * mantem cada pedido curto, o que importa muito no modelo do aparelho.
 *
 * Uma correcao por vez: se a pessoa continua falando durante uma correcao, o
 * trecho novo espera a vez em vez de empilhar pedidos. Ao concluir, so o que
 * sobrou sem corrigir passa pela IA, entao o fim e quase instantaneo.
 *
 * Tudo roda na thread principal ([scope] deve ser de Dispatchers.Main).
 */
class LiveDictation(
    private val engine: AiEngine,
    private val scope: CoroutineScope,
    /** Recebe o melhor texto do ditado ate agora (sem o que ja existia no campo). */
    private val onDisplay: (String) -> Unit,
    /** Intervalo minimo entre correcoes: protege a cota da nuvem; 0 no aparelho. */
    private val minGapMs: () -> Long = { 0L }
) {
    private var active = false
    private var transcript = ""

    /** Prefixo do texto cru que ja foi corrigido, e o resultado dessa correcao. */
    private var polishedFor = ""
    private var polishedText = ""

    private var pauseJob: Job? = null
    private var polishJob: Job? = null
    private var polishAgain = false
    private var lastPolishEndedAt = 0L

    fun start() {
        cancelJobs()
        transcript = ""
        polishedFor = ""
        polishedText = ""
        lastPolishEndedAt = 0L
        active = true
    }

    /** Ja ha texto ditado nesta sessao? */
    fun hasText(): Boolean = transcript.isNotEmpty()

    fun cancel() {
        active = false
        cancelJobs()
        transcript = ""
        polishedFor = ""
        polishedText = ""
    }

    /** Parcial novo do reconhecedor: mostra na hora e agenda a correcao. */
    fun onPartial(text: String) {
        if (!active || text.isBlank() || text == transcript) return
        transcript = text
        // O reconhecedor revisou palavras de um trecho que ja estava corrigido:
        // aquele pedaco volta ao cru e sera corrigido de novo na proxima pausa.
        if (polishedFor.isNotEmpty() && !text.startsWith(polishedFor)) {
            polishedFor = ""
            polishedText = ""
        }
        onDisplay(display())
        schedule()
    }

    /**
     * Passada final. Espera a correcao em andamento (se houver) e corrige so o
     * que ainda estava cru.
     */
    suspend fun finish(finalText: String): DictationResult {
        pauseJob?.cancel()
        withTimeoutOrNull(FINISH_WAIT_MS) { polishJob?.join() }
        polishJob?.cancel()
        active = false

        if (finalText.isNotBlank()) transcript = finalText
        val raw = transcript
        val reusable = polishedFor.isNotEmpty() && raw.startsWith(polishedFor)
        val baseText = if (reusable) polishedText else ""
        val delta = raw.removePrefix(if (reusable) polishedFor else "").trim()

        if (delta.isEmpty()) {
            return DictationResult(baseText.ifBlank { raw.trim() }, null)
        }

        var error: AiError? = null
        // Duas tentativas antes de aceitar o texto cru: a correcao de fala e,
        // se falhar, a acao Corrigir.
        val piece = when (val result = engine.polishTranscript(delta)) {
            is AiResult.Success -> result.value
            is AiResult.Failure -> when (val retry = engine.run(action = AiAction.FIX, text = delta)) {
                is AiResult.Success -> retry.value
                is AiResult.Failure -> {
                    error = retry.error
                    delta
                }
            }
        }
        return DictationResult(join(baseText, piece.trim()), error)
    }

    // --- Interno --------------------------------------------------------

    private fun cancelJobs() {
        pauseJob?.cancel()
        polishJob?.cancel()
        pauseJob = null
        polishJob = null
        polishAgain = false
    }

    /** Junta o trecho corrigido com o que ainda esta cru. */
    private fun display(): String {
        val raw = transcript
        if (polishedText.isEmpty() || !raw.startsWith(polishedFor)) return raw.trim()
        val tail = raw.removePrefix(polishedFor).trim()
        return if (tail.isEmpty()) polishedText else "$polishedText $tail"
    }

    /** Cada parcial reinicia o relogio da pausa; so pausa de verdade dispara a correcao. */
    private fun schedule() {
        pauseJob?.cancel()
        pauseJob = scope.launch {
            delay(PAUSE_MS)
            pump()
        }
    }

    /** Roda uma correcao por vez; se chegou fala nova durante ela, repete ao terminar. */
    private fun pump() {
        if (polishJob?.isActive == true) {
            polishAgain = true
            return
        }
        polishJob = scope.launch {
            do {
                polishAgain = false
                polishOnce()
            } while (polishAgain && active)
        }
    }

    private suspend fun polishOnce() {
        val gap = minGapMs() - (SystemClock.elapsedRealtime() - lastPolishEndedAt)
        if (gap > 0) delay(gap)
        if (!active) return

        // Foto do texto depois da espera: a pausa garante que o fim esta estavel.
        val raw = transcript
        val reusable = polishedFor.isNotEmpty() && raw.startsWith(polishedFor)
        val baseFor = if (reusable) polishedFor else ""
        val baseText = if (reusable) polishedText else ""
        val delta = raw.removePrefix(baseFor).trim()
        // Trecho muito curto sai pior da IA e gasta pedido: espera mais fala.
        if (delta.split(' ', '\n').count { it.isNotBlank() } < MIN_WORDS) return

        val result = engine.polishTranscript(delta)
        lastPolishEndedAt = SystemClock.elapsedRealtime()
        if (result !is AiResult.Success || !active) return
        // So vale se o comeco do texto nao mudou enquanto a IA trabalhava.
        if (!transcript.startsWith(raw) || polishedFor != baseFor) return

        polishedFor = raw
        polishedText = join(baseText, result.value.trim())
        onDisplay(display())
    }

    private fun join(first: String, second: String): String = when {
        first.isEmpty() -> second
        second.isEmpty() -> first
        else -> "$first $second"
    }

    private companion object {
        /** Pausa na fala que dispara a correcao do trecho ja dito. */
        const val PAUSE_MS = 700L

        /** Menos palavras que isso nao valem uma ida a IA. */
        const val MIN_WORDS = 3

        /** Quanto esperar uma correcao em andamento ao concluir o ditado. */
        const val FINISH_WAIT_MS = 15_000L
    }
}
