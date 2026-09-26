package com.odorizzioficial.tecladoia.service

import android.accessibilityservice.AccessibilityService
import android.graphics.Rect
import android.view.accessibility.AccessibilityWindowInfo

/** Estado observado do teclado do sistema. */
data class KeyboardState(
    val visible: Boolean = false,
    /** Altura ocupada pelo teclado, medida da base da tela, em pixels. */
    val heightPx: Int = 0
)

/**
 * Descobre se o IME esta na tela e qual altura ele ocupa, lendo a lista de
 * janelas do sistema (AccessibilityWindowInfo.TYPE_INPUT_METHOD). Esta e a
 * unica forma publica de saber a altura real de um teclado de terceiros
 * estando fora do processo dele.
 */
class KeyboardWatcher(private val service: AccessibilityService) {

    fun currentState(): KeyboardState {
        val windows = try {
            service.windows
        } catch (t: Throwable) {
            return KeyboardState()
        }
        if (windows.isNullOrEmpty()) return KeyboardState()

        val bounds = Rect()
        var imeTop = Int.MAX_VALUE
        var found = false

        for (window in windows) {
            if (window == null) continue
            if (window.type != AccessibilityWindowInfo.TYPE_INPUT_METHOD) continue
            window.getBoundsInScreen(bounds)
            if (bounds.height() <= 0) continue
            found = true
            if (bounds.top < imeTop) imeTop = bounds.top
        }

        if (!found) return KeyboardState()

        val displayHeight = service.resources.displayMetrics.heightPixels
        val height = (displayHeight - imeTop).coerceIn(0, displayHeight)
        // Teclados muito baixos normalmente sao apenas a barra de gestos do IME.
        if (height < MIN_KEYBOARD_HEIGHT_PX) return KeyboardState()
        return KeyboardState(visible = true, heightPx = height)
    }

    private companion object {
        const val MIN_KEYBOARD_HEIGHT_PX = 160
    }
}
