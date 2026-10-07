package com.odorizzioficial.tecladoia.service

import android.accessibilityservice.AccessibilityService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.odorizzioficial.tecladoia.domain.AiError
import com.odorizzioficial.tecladoia.domain.AiResult

/**
 * Acesso ao campo de texto em foco usando somente APIs publicas de
 * acessibilidade. Nenhum app e obrigado a expor um campo editavel, por isso
 * toda operacao aqui retorna erro estruturado em vez de lancar excecao: uma
 * excecao solta derrubaria o servico e o Android o desligaria.
 *
 * O campo e procurado em camadas, porque apps como YouTube, Instagram e
 * navegadores nem sempre marcam o foco do jeito que o Android espera:
 *  1. o foco de entrada do sistema;
 *  2. a janela ativa;
 *  3. as demais janelas do app (comentarios e dialogos abrem em janela propria).
 *
 * Para escrever, tenta ACTION_SET_TEXT e, se o app nao aceitar, seleciona tudo
 * e cola. Campo de senha nunca e lido nem alterado.
 */
class TextFieldBridge(private val service: AccessibilityService) {

    /**
     * Resumo tecnico da ultima busca/escrita, mostrado junto do erro quando um
     * app nao deixa o TecladoIA mexer no campo. Serve para entender o caso
     * sem precisar de acesso ao aparelho de quem relatou.
     */
    @Volatile
    private var lastDiagnostic: String = ""

    fun diagnostic(): String = lastDiagnostic

    /** Texto do campo focado, ou erro quando nao ha campo acessivel. */
    fun readFocusedText(): AiResult<String> = safely(AiResult.Failure(AiError.FieldNotReadable)) {
        val node = findInputNode() ?: return@safely AiResult.Failure(AiError.FieldNotReadable)
        try {
            val text = realText(node)
            if (text.isNotBlank()) return@safely AiResult.Success(text)

            lastDiagnostic = describe(node)
            val caret = caretOf(node)
            if (caret <= 0) return@safely AiResult.Failure(AiError.EmptyInput)

            // O cursor esta depois de algum texto, mas o app nao o entrega: ele so
            // esta "vazio" para o servico. Ultimo recurso: copiar o campo.
            val copied = readViaClipboard(node, caret)
            if (copied.isNotBlank()) AiResult.Success(copied) else AiResult.Failure(AiError.FieldTextHidden)
        } finally {
            node.recycleCompat()
        }
    }

    /**
     * Substitui o conteudo do campo focado. Quando nada funciona, o texto vai
     * para a area de transferencia e [AiError.FieldNotEditable] e devolvido
     * para a barra avisar o usuario.
     */
    fun replaceFocusedText(newText: String): AiResult<Unit> = try {
        replaceInternal(newText)
    } catch (t: Throwable) {
        Log.w(TAG, "Falha ao escrever no campo", t)
        copyToClipboard(newText)
        AiResult.Failure(AiError.FieldNotEditable)
    }

    private fun replaceInternal(newText: String): AiResult<Unit> {
        val node = findInputNode() ?: run {
            copyToClipboard(newText)
            return AiResult.Failure(AiError.FieldNotEditable)
        }
        try {
            val setTextSupported = supportsSetText(node)
            var setTextOk = false
            // 1) O caminho normal: ACTION_SET_TEXT.
            if (setTextSupported) {
                val args = Bundle().apply {
                    putCharSequence(
                        AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE,
                        newText
                    )
                }
                val applied = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args)
                setTextOk = applied
                // Em campo que nao se declara editavel, "true" nem sempre quer
                // dizer que o texto mudou: confere antes de aceitar.
                if (applied && (node.isEditable || textLanded(node, newText))) {
                    moveCursorToEnd(node, newText.length)
                    return AiResult.Success(Unit)
                }
            }

            // 2) Plano B: seleciona tudo e cola.
            val pasted = pasteInto(node, newText)
            if (pasted) return AiResult.Success(Unit)

            lastDiagnostic = "classe=${node.className?.toString()?.substringAfterLast('.') ?: "-"} " +
                "editavel=${if (node.isEditable) "sim" else "nao"} " +
                "settext=${if (!setTextSupported) "ausente" else if (setTextOk) "sem-efeito" else "recusado"} " +
                "colar=recusado"
            copyToClipboard(newText)
            return AiResult.Failure(AiError.FieldNotEditable)
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
        val caret = caretOf(node)

        if (text.isEmpty()) {
            // Alguns editores personalizados so entregam o texto digitado na
            // descricao. O cursor depois de algum caractere confirma que e texto.
            return if (caret > 0 && description.isNotEmpty() && description != hint) description else ""
        }
        // Texto igual a dica ou a descricao e o placeholder do campo vazio... a
        // menos que o cursor mostre que ha texto de verdade (apps que repetem o
        // texto digitado na descricao).
        val looksLikePlaceholder =
            (hint.isNotEmpty() && text == hint) || (description.isNotEmpty() && text == description)
        if (looksLikePlaceholder && caret <= 0) return ""
        return text
    }

    /** Posicao do cursor no campo; 0 ou -1 quando o app nao informa. */
    private fun caretOf(node: AccessibilityNodeInfo): Int =
        maxOf(node.textSelectionStart, node.textSelectionEnd)

    /** Resumo tecnico do campo, para o aviso de erro e para diagnosticar um app. */
    private fun describe(node: AccessibilityNodeInfo): String {
        val bounds = Rect()
        node.getBoundsInScreen(bounds)
        return "classe=${node.className?.toString()?.substringAfterLast('.') ?: "-"} " +
            "editavel=${if (node.isEditable) "sim" else "nao"} " +
            "texto=${node.text?.length ?: 0} desc=${node.contentDescription?.length ?: 0} " +
            "dica=${node.hintText?.length ?: 0} cursor=${caretOf(node)} base=${bounds.bottom}"
    }

    /**
     * Para campos que escondem o texto (o cursor diz que ha, a leitura vem vazia):
     * seleciona ate o cursor, copia e le a area de transferencia. Devolve o que
     * a pessoa tinha copiado antes quando o Android deixa ler.
     */
    private fun readViaClipboard(node: AccessibilityNodeInfo, caret: Int): String = try {
        val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        if (clipboard == null) {
            ""
        } else {
            val previous = runCatching { clipboard.primaryClip }.getOrNull()
            clipboard.setPrimaryClip(ClipData.newPlainText("TecladoIA", CLIP_MARKER))
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, caret)
            }
            val selected = node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
            val copied = selected && node.performAction(AccessibilityNodeInfo.ACTION_COPY)
            val text = if (copied) {
                runCatching {
                    clipboard.primaryClip?.getItemAt(0)?.coerceToText(service)?.toString()
                }.getOrNull().orEmpty()
            } else {
                ""
            }
            moveCursorToEnd(node, caret)
            // Devolve a area de transferencia ao que a pessoa tinha.
            if (previous != null) {
                runCatching { clipboard.setPrimaryClip(previous) }
            } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                runCatching { clipboard.clearPrimaryClip() }
            }
            if (text == CLIP_MARKER) "" else text
        }
    } catch (t: Throwable) {
        Log.w(TAG, "Falha ao ler o campo pela area de transferencia", t)
        ""
    }

    /**
     * O campo que o sistema diz ter o foco de entrada vale mesmo sem se declarar
     * editavel: editores personalizados (como o de comentarios do YouTube) so
     * mostram que aceitam texto pelas acoes de selecionar e colar.
     */
    private fun usableFocused(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return false
        if (usable(node)) return true
        val actions = node.actionList.map { it.id }
        return AccessibilityNodeInfo.ACTION_SET_SELECTION in actions ||
            AccessibilityNodeInfo.ACTION_PASTE in actions ||
            (node.isFocused && node.text?.isNotEmpty() == true)
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
    fun dictationBaseText(): String = safely("") {
        val node = findInputNode() ?: return@safely ""
        try {
            val text = realText(node)
            if (text.isEmpty()) return@safely ""
            val caret = maxOf(node.textSelectionStart, node.textSelectionEnd)
            if (caret <= 0) "" else text
        } finally {
            node.recycleCompat()
        }
    }

    fun hasEditableFocus(): Boolean = safely(false) {
        val node = findInputNode() ?: return@safely false
        node.recycleCompat()
        true
    }

    fun copyToClipboard(text: String) {
        runCatching {
            val clipboard = service.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            clipboard?.setPrimaryClip(ClipData.newPlainText("TecladoIA", text))
        }
    }

    // --- Busca do campo ---------------------------------------------------

    /**
     * Devolve o campo de texto em foco, ou null. Quem recebe o no e quem o
     * libera ([recycleCompat]).
     */
    private fun findInputNode(): AccessibilityNodeInfo? {
        // 1) Foco de entrada do sistema.
        val focused = service.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) {
            if (usableFocused(focused)) return focused
            focused.recycleCompat()
        }

        // 2) Janela ativa e 3) as outras janelas do app, da de cima para baixo.
        val roots = ArrayList<AccessibilityNodeInfo>()
        service.rootInActiveWindow?.let { roots += it }
        runCatching {
            service.windows.orEmpty().forEach { window ->
                if (window.type == AccessibilityWindowInfo.TYPE_INPUT_METHOD) return@forEach
                window.root?.let { roots += it }
            }
        }

        var found: AccessibilityNodeInfo? = null
        for (root in roots) {
            if (found == null) found = searchRoot(root)
        }

        // 4) Apps que nao informam qual campo tem o foco (editores personalizados):
        // pega o campo de texto visivel mais perto do teclado, que e onde a
        // pessoa esta digitando.
        var fallbackUsed = false
        if (found == null) {
            found = pickByPosition(roots)
            fallbackUsed = found != null
        }
        lastDiagnostic = "janelas=${roots.size} foco=${if (found != null && !fallbackUsed) "sim" else "nao"} " +
            "posicao=${if (fallbackUsed) "sim" else "nao"} " +
            "classe=${found?.className?.toString()?.substringAfterLast('.') ?: "-"}"

        // Os nos raiz nao devolvidos sao liberados aqui.
        roots.forEach { it.recycleCompat() }
        return found
    }

    /** Campo de texto visivel mais baixo na tela entre todas as janelas lidas. */
    private fun pickByPosition(roots: List<AccessibilityNodeInfo>): AccessibilityNodeInfo? {
        var best: AccessibilityNodeInfo? = null
        var bestBottom = Int.MIN_VALUE
        val bounds = Rect()
        val budget = intArrayOf(MAX_NODES_VISITED)
        for (root in roots) {
            collectEditable(root, budget) { candidate ->
                candidate.getBoundsInScreen(bounds)
                if (bounds.bottom > bestBottom) {
                    bestBottom = bounds.bottom
                    best = candidate
                }
            }
        }
        return best
    }

    private fun collectEditable(
        node: AccessibilityNodeInfo,
        budget: IntArray,
        onFound: (AccessibilityNodeInfo) -> Unit
    ) {
        if (budget[0] <= 0) return
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            budget[0]--
            if (child.isVisibleToUser && usable(child)) onFound(child)
            collectEditable(child, budget, onFound)
            if (budget[0] <= 0) return
        }
    }

    /** Procura dentro de uma janela: foco de entrada, depois campo editavel focado. */
    private fun searchRoot(root: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val focused = root.findFocus(AccessibilityNodeInfo.FOCUS_INPUT)
        if (focused != null) {
            if (usableFocused(focused)) return focused
            focused.recycleCompat()
        }
        val budget = intArrayOf(MAX_NODES_VISITED)
        return findFocusedEditable(root, budget)
    }

    private fun findFocusedEditable(
        node: AccessibilityNodeInfo,
        budget: IntArray
    ): AccessibilityNodeInfo? {
        if (budget[0] <= 0) return null
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            budget[0]--
            if (child.isFocused && usable(child)) return child
            val deeper = findFocusedEditable(child, budget)
            child.recycleCompat()
            if (deeper != null) return deeper
            if (budget[0] <= 0) return null
        }
        return null
    }

    /** E um campo de texto que o app pode alterar, e nao e de senha? */
    private fun usable(node: AccessibilityNodeInfo): Boolean {
        if (node.isPassword) return false
        return node.isEditable ||
            node.className?.contains("EditText") == true ||
            supportsSetText(node)
    }

    private fun supportsSetText(node: AccessibilityNodeInfo): Boolean =
        node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_SET_TEXT }

    // --- Escrita ----------------------------------------------------------

    /** O texto realmente mudou? Alguns apps respondem true e nao fazem nada. */
    private fun textLanded(node: AccessibilityNodeInfo, expected: String): Boolean {
        node.refresh()
        val now = realText(node).filterNot { it.isWhitespace() }
        return now == expected.filterNot { it.isWhitespace() }
    }

    /** Seleciona o conteudo atual e cola o texto novo por cima. */
    private fun pasteInto(node: AccessibilityNodeInfo, text: String): Boolean {
        val canPaste = node.isEditable ||
            node.actionList.any { it.id == AccessibilityNodeInfo.ACTION_PASTE }
        if (!canPaste) return false

        copyToClipboard(text)
        node.performAction(AccessibilityNodeInfo.ACTION_FOCUS)

        val currentLength = realText(node).length
        if (currentLength > 0) {
            val args = Bundle().apply {
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, 0)
                putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, currentLength)
            }
            // Sem conseguir selecionar, colar duplicaria o texto: melhor desistir.
            if (!node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)) return false
        }
        return node.performAction(AccessibilityNodeInfo.ACTION_PASTE)
    }

    private fun moveCursorToEnd(node: AccessibilityNodeInfo, length: Int) {
        val args = Bundle().apply {
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_START_INT, length)
            putInt(AccessibilityNodeInfo.ACTION_ARGUMENT_SELECTION_END_INT, length)
        }
        node.performAction(AccessibilityNodeInfo.ACTION_SET_SELECTION, args)
    }

    private inline fun <T> safely(fallback: T, block: () -> T): T = try {
        block()
    } catch (t: Throwable) {
        Log.w(TAG, "Falha ao acessar o campo de texto", t)
        fallback
    }

    /** recycle() e obsoleto desde a API 33 e no-op; encapsulado em um lugar so. */
    @Suppress("DEPRECATION")
    private fun AccessibilityNodeInfo.recycleCompat() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            recycle()
        }
    }

    private companion object {
        const val TAG = "TextFieldBridge"

        /** Marca colocada na area de transferencia para saber se a copia aconteceu. */
        const val CLIP_MARKER = "\u200BTecladoIA\u200B"

        /** Teto de nos visitados na varredura, para nao pesar em telas enormes. */
        const val MAX_NODES_VISITED = 600
    }
}
