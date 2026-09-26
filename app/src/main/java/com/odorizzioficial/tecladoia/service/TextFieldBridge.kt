package com.odorizzioficial.tecladoia.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.accessibility.AccessibilityNodeInfo
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult

/**
 * Acesso ao campo de texto em foco usando somente APIs publicas de
 * acessibilidade. Nenhum app e obrigado a expor um campo editavel, por isso
 * toda operacao aqui retorna erro estruturado em vez de lancar excecao.
 */
class TextFieldBridge(private val service: AccessibilityService) {

    /** Texto do campo focado, ou erro quando nao ha campo acessivel. */
    fun readFocusedText(): AiResult<String> {
        val node = focusedEditable() ?: return AiResult.Failure(AiError.FieldNotReadable)
        return try {
            val text = realText(node)
            if (text.isBlank()) AiResult.Failure(AiError.EmptyInput) else AiResult.Success(text)
        } finally {
            node.recycleCompat()
        }
    }

    /**
     * Substitui o conteudo do campo focado. Quando o app de destino nao aceita
     * ACTION_SET_TEXT, o texto vai para a area de transferencia e o erro
     * [AiError.FieldNotEditable] e devolvido para a barra avisar o usuario.
     */
    fun replaceFocusedText(newText: String): AiResult<Unit> {
        val node = focusedEditable() ?: run {
            copyToClipboard(newText)
            return AiResult.Failure(AiError.FieldNotEditable)
        }
        return try {
            val supportsSetText = node.actionList.any {
                it.id == AccessibilityNodeInfo.ACTION_SET_TEXT
            }
            if (!node.isEditable || !supportsSetText) {
                copyToClipboard(newText)
                return AiResult.Failure(AiError.FieldNotEditable)
            }
            val args = Bundle().apply {
                putCharSequence(
                    AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                    newText
                )
            }
            val applied = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
            if (!applied) {
                copyToClipboard(newText)
                AiResult.Failure(AiError.FieldNotEditable)
            } else {
                moveCursorToEnd(node, newText.length)
                AiResult.Success(Unit)
            }
        } finally {
            node.recycleCompat()
        }
    }

    /** Acrescenta texto ao final do campo (usado pela transcricao de voz). */
    fun appendToFocusedText(addition: String): AiResult<Unit> {
        val current = when (val read = readFocusedText()) {
            is AiResult.Success -> read.value
            is AiResult.Failure -> if (read.error is AiError.EmptyInput) "" else {
                copyToClipboard(addition)
                return AiResult.Failure(AiError.FieldNotEditable)
            }
        }
        val separator = when {
            current.isEmpty() -> ""
            current.endsWith(" ") || current.endsWith("\n") -> ""
            else -> " "
        }
        return replaceFocusedText(current + separator + addition)
    }

    /**
     * Campo vazio devolve o placeholder ("Mensagem", "Pesquisar"...) em
     * node.text. Sem esse filtro o ditado colava a fala depois da dica e o
     * resultado saia como "Mensagem bom dia".
     */
    private fun realText(node: AccessibilityNodeInfo): String {
        if (node.isShowingHintText) return ""
        val text = node.text?.toString().orEmpty()
        val hint = node.hintText?.toString().orEmpty()
        val description = node.contentDescription?.toString().orEmpty()
        if (hint.isNotEmpty() && text == hint) return ""
        if (description.isNotEmpty() && text == description) return ""
        return text
    }

    /**
     * Texto que a transcricao deve preservar antes de acrescentar a fala.
     *
     * Alguns apps (o WhatsApp entre eles) devolvem o proprio placeholder em
     * node.text sem marcar isShowingHintText, e era isso que colava "Mensagem"
     * na frente do ditado. A checagem extra e o cursor: em texto digitado de
     * verdade o caret fica depois do primeiro caractere; num placeholder ele
     * nao existe (0 ou -1).
     */
    fun dictationBaseText(): String {
        val node = focusedEditable() ?: return ""
        return try {
            val text = realText(node)
            if (text.isEmpty()) return ""
            val caret = maxOf(node.textSelectionStart, node.textSelectionEnd)
            if (caret <= 0) "" else text
        } finally {
            node.recycleCompat()
        }
    }

    fun hasEditableFocus(): Boolean {
        val node = focusedEditable() ?: return false
        node.recycleCompat()
        return true
    }

    fun copyToClipboard(text: String) {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        clipboard?.setPrimaryClip(ClipData.newPlainText("IA no seu Teclado", text))
    }

    private fun focusedEditable(): AccessibilityNodeInfo? {
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT) ?: return null
        if (focused.isEditable || focused.className?.contains("EditText") == true) return focused
        focused.recycleCompat()
        return null
    }

    private fun moveCursorToEnd(node: AccessibilityNodeInfo, length: Int) {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, length)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, length)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    /** recycle() e obsoleto desde a API 33 e no-op; encapsulado em um lugar so. */
    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleCompat() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            recycle()
        }
    }
}
