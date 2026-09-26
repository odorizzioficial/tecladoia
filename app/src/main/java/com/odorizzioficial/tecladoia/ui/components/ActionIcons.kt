package com.odorizzioficial.tecladoia.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.Spellcheck
import androidx.compose.material.icons.rounded.Summarize
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.ui.graphics.vector.ImageVector
import com.odorizzioficial.tecladoia.domain.AiAction

/** Mapeia as acoes do dominio para icones Material, sem sujar a camada de dominio. */
fun AiAction.iconVector(): ImageVector = when (this) {
    AiAction.FIX -> Icons.Rounded.Spellcheck
    AiAction.IMPROVE -> Icons.Rounded.Bolt
    AiAction.TRANSLATE -> Icons.Rounded.Translate
    AiAction.TONE -> Icons.Rounded.Mood
    AiAction.SUMMARIZE -> Icons.Rounded.Summarize
    AiAction.REWRITE -> Icons.Rounded.EditNote
}
