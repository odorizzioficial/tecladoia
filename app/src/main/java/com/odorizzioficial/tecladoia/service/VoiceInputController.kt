package com.odorizzioficial.tecladoia.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.odorizzioficial.tecladoia.R

/**
 * Grava audio somente enquanto o usuario mantem a gravacao aberta na barra.
 * O audio nunca e persistido: o [SpeechRecognizer] do sistema transcreve e o
 * app recebe apenas texto. Nada e iniciado em segundo plano.
 *
 * O reconhecedor do Android encerra sozinho no primeiro silencio. Aqui a sessao
 * e continua: enquanto o usuario nao toca em parar, o trecho reconhecido e
 * acumulado e a escuta reinicia, entao pausas no meio da fala nao cortam o
 * ditado.
 */
class VoiceInputController(
    private val context: Context,
    private val callbacks: Callbacks
) {

    interface Callbacks {
        fun onPartialTranscript(text: String)
        fun onFinalTranscript(text: String)
        fun onVoiceError(message: String)
        fun onRecordingStarted()
    }

    private val handler = Handler(Looper.getMainLooper())

    private var recognizer: SpeechRecognizer? = null
    private var listening = false

    /** Ligado apenas quando o proprio usuario pede para encerrar. */
    private var userStopped = false
    private var finished = false
    private var languageTag = "pt-BR"

    /** Texto somado de todos os trechos da sessao atual. */
    private val transcript = StringBuilder()

    /** Ultimo parcial recebido: usado para fechar na hora, sem esperar o final. */
    private var lastPartial = ""

    private val finishFallback = Runnable { finishSession(fromFallback = true) }

    val isListening: Boolean get() = listening

    fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    /** Deve ser chamado na thread principal. */
    fun start(languageTag: String) {
        if (listening) return
        if (!isAvailable()) {
            callbacks.onVoiceError(context.getString(R.string.err_voice_unavailable))
            return
        }
        this.languageTag = languageTag
        userStopped = false
        finished = false
        transcript.setLength(0)
        lastPartial = ""
        release()
        listen(notifyStart = true)
    }

    /**
     * Encerra na hora. O texto usado e o que ja foi reconhecido (trechos
     * fechados + ultimo parcial), sem esperar o reconhecedor devolver o
     * resultado final, que costuma levar mais de um segundo.
     */
    fun stop() {
        if (finished) return
        userStopped = true
        appendChunk(lastPartial)
        lastPartial = ""
        runCatching { recognizer?.cancel() }
        finishSession()
    }

    fun cancel() {
        userStopped = true
        finished = true
        listening = false
        transcript.setLength(0)
        lastPartial = ""
        handler.removeCallbacks(finishFallback)
        runCatching { recognizer?.cancel() }
        release()
    }

    fun release() {
        handler.removeCallbacks(finishFallback)
        listening = false
        runCatching { recognizer?.destroy() }
        recognizer = null
    }

    private fun buildIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, languageTag)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
        // Silencio nao encerra o ditado: quem encerra e o usuario.
        putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS,
            SILENCE_MS
        )
        putExtra(
            RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS,
            SILENCE_MS
        )
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, MINIMUM_MS)
    }

    private fun listen(notifyStart: Boolean) {
        val instance = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer = instance
        instance.setRecognitionListener(Listener(notifyStart))

        try {
            instance.startListening(buildIntent())
            listening = true
            if (notifyStart) callbacks.onRecordingStarted()
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao iniciar reconhecimento", t)
            listening = false
            finished = true
            callbacks.onVoiceError(context.getString(R.string.voice_start_failed))
            release()
        }
    }

    /**
     * Reinicia a escuta mantendo o que ja foi transcrito. Reusa a mesma
     * instancia do reconhecedor em vez de destruir e criar outra: era essa
     * troca (e o religamento ao servico do sistema que ela exige) que deixava
     * um vazio sem escuta a cada pausa, atrasando o texto reaparecer.
     */
    private fun restart() {
        if (finished || userStopped) return
        val instance = recognizer
        if (instance == null) {
            listen(notifyStart = false)
            return
        }
        listening = false
        try {
            instance.startListening(buildIntent())
            listening = true
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao reiniciar reconhecimento, recriando", t)
            runCatching { instance.destroy() }
            recognizer = null
            handler.postDelayed({
                if (!finished && !userStopped) listen(notifyStart = false)
            }, RESTART_DELAY_MS)
        }
    }

    private fun appendChunk(chunk: String) {
        val clean = chunk.trim()
        if (clean.isEmpty()) return
        if (transcript.isNotEmpty()) transcript.append(' ')
        transcript.append(clean)
    }

    private fun finishSession(fromFallback: Boolean = false) {
        if (finished) return
        finished = true
        listening = false
        handler.removeCallbacks(finishFallback)
        release()
        val text = transcript.toString().trim()
        if (text.isEmpty()) {
            callbacks.onVoiceError(context.getString(R.string.voice_nothing_heard))
        } else {
            if (fromFallback) Log.d(TAG, "Sessão fechada pelo tempo limite de finalização")
            callbacks.onFinalTranscript(text)
        }
    }

    private inner class Listener(private var notifyStart: Boolean) : RecognitionListener {

        override fun onReadyForSpeech(params: Bundle?) {
            listening = true
            if (notifyStart) {
                callbacks.onRecordingStarted()
                // So a primeira vez desta sessao dispara o aviso: os reinicios
                // internos agora reusam este mesmo listener (sem recriar o
                // reconhecedor), entao sem isso ele soaria a cada reinicio.
                notifyStart = false
            }
        }

        override fun onBeginningOfSpeech() = Unit
        override fun onRmsChanged(rmsdB: Float) = Unit
        override fun onBufferReceived(buffer: ByteArray?) = Unit
        override fun onEndOfSpeech() = Unit
        override fun onEvent(eventType: Int, params: Bundle?) = Unit

        override fun onError(error: Int) {
            if (finished) return
            val silence = error == SpeechRecognizer.ERROR_NO_MATCH ||
                error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT
            when {
                userStopped -> finishSession()
                silence -> restart()
                else -> {
                    listening = false
                    finished = true
                    callbacks.onVoiceError(messageForError(error))
                    release()
                }
            }
        }

        override fun onResults(results: Bundle?) {
            if (finished) return
            val text = results
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            lastPartial = ""
            appendChunk(text)
            if (userStopped) {
                finishSession()
            } else {
                callbacks.onPartialTranscript(transcript.toString())
                restart()
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            if (finished) return
            val text = partialResults
                ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                ?.firstOrNull()
                .orEmpty()
            if (text.isBlank()) return
            lastPartial = text
            val prefix = if (transcript.isEmpty()) "" else "$transcript "
            callbacks.onPartialTranscript(prefix + text)
        }
    }

    private fun messageForError(error: Int): String = context.getString(
        when (error) {
            SpeechRecognizer.ERROR_AUDIO -> R.string.voice_audio_problem
            SpeechRecognizer.ERROR_CLIENT -> R.string.voice_interrupted
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.voice_mic_needed
            SpeechRecognizer.ERROR_NETWORK,
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> R.string.voice_check_connection

            SpeechRecognizer.ERROR_NO_MATCH -> R.string.voice_not_understood
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> R.string.voice_recognizer_busy
            SpeechRecognizer.ERROR_SERVER -> R.string.voice_service_failed
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.voice_nothing_heard
            else -> R.string.voice_generic_failed
        }
    )

    private companion object {
        const val TAG = "VoiceInputController"

        /** Silencio tolerado dentro de um trecho antes do reconhecedor fechar. */
        const val SILENCE_MS = 10_000L
        const val MINIMUM_MS = 60_000L
        const val RESTART_DELAY_MS = 120L
        const val FINISH_FALLBACK_MS = 2_500L
    }
}
