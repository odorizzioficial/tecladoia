package com.odorizzioficial.tecladoia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.EmojiEmotions
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.domain.CustomPrompt
import com.odorizzioficial.tecladoia.ui.MainViewModel
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.theme.PillShape
import androidx.compose.ui.draw.alpha
import java.text.BreakIterator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PromptsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<CustomPrompt?>(null) }
    var creating by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.prompts_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = stringResource(R.string.prompts_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 6.dp)
        )

        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AuroraButton(
                text = stringResource(R.string.prompts_create),
                icon = Icons.Rounded.Add,
                onClick = { creating = true },
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(10.dp))
            PillChip(
                text = stringResource(R.string.prompts_defaults),
                icon = Icons.Rounded.Restore,
                onClick = viewModel::restoreDefaultPrompts,
                modifier = Modifier.weight(1f),
                height = 52.dp
            )
        }

        SectionHeader(
            title = stringResource(R.string.prompts_count, prompts.size),
            icon = Icons.Rounded.ViewAgenda
        )

        if (prompts.size > 1) {
            Text(
                text = stringResource(R.string.prompts_reorder_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
            )
        }

        if (prompts.isEmpty()) {
            AppCard {
                Text(
                    text = stringResource(R.string.prompts_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        prompts.forEachIndexed { index, prompt ->
            PromptCard(
                prompt = prompt,
                index = index,
                total = prompts.size,
                canReorder = prompts.size > 1,
                onToggleEnabled = { viewModel.setPromptEnabled(prompt.id, it) },
                onTogglePinned = { viewModel.setPromptPinned(prompt.id, !prompt.pinned) },
                onEdit = { editing = prompt },
                onDuplicate = { viewModel.duplicatePrompt(prompt.id) },
                onDelete = { viewModel.deletePrompt(prompt.id) },
                onMoveBy = { delta -> viewModel.movePrompt(prompt.id, delta) }
            )
            Spacer(Modifier.height(LIST_GAP))
        }

        Spacer(Modifier.height(28.dp))
    }

    if (creating || editing != null) {
        ModalBottomSheet(
            onDismissRequest = {
                creating = false
                editing = null
            },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) {
            PromptEditor(
                initial = editing,
                onCancel = {
                    creating = false
                    editing = null
                },
                onSave = { name, icon, prompt, pinned ->
                    val current = editing
                    if (current == null) {
                        viewModel.addPrompt(name, icon, prompt, pinned)
                    } else {
                        viewModel.updatePrompt(
                            current.copy(
                                name = name,
                                icon = icon,
                                prompt = prompt,
                                pinned = pinned
                            )
                        )
                    }
                    creating = false
                    editing = null
                }
            )
        }
    }
}

@Composable
private fun PromptCard(
    prompt: CustomPrompt,
    index: Int,
    total: Int,
    canReorder: Boolean,
    onToggleEnabled: (Boolean) -> Unit,
    onTogglePinned: () -> Unit,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit,
    onMoveBy: (Int) -> Unit
) {
    // Altura real do card + o espaco entre cards da lista: e o passo de uma
    // posicao no reordenamento.
    var cardHeight by remember { mutableStateOf(0) }
    val gapPx = with(LocalDensity.current) { LIST_GAP.toPx() }
    val stepPx = (if (cardHeight > 0) cardHeight.toFloat() else 200f) + gapPx

    var dragged by remember { mutableStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val settle = remember { Animatable(0f) }
    val position by rememberUpdatedState(index)
    val count by rememberUpdatedState(total)
    val move by rememberUpdatedState(onMoveBy)

    /**
     * Durante o arrasto o card segue o dedo e a lista fica parada: e isso que
     * evita o efeito de blocos se sobrepondo, que acontecia quando a ordem
     * mudava no meio do gesto e as alturas (diferentes de card para card)
     * deixavam o item fora de lugar.
     */
    fun onDragDelta(delta: Float) {
        val minOffset = -position * stepPx
        val maxOffset = (count - 1 - position) * stepPx
        dragged = (dragged + delta).coerceIn(minOffset, maxOffset)
    }

    /** Ao soltar, a ordem muda de uma vez e o card encaixa na nova posicao. */
    fun endDrag() {
        val travelled = dragged
        val shift = (travelled / stepPx).roundToInt()
            .coerceIn(-position, count - 1 - position)
        dragging = false
        dragged = 0f
        if (shift != 0) move(shift)
        scope.launch {
            // Sobra apenas a diferenca entre onde o dedo parou e o encaixe.
            settle.snapTo(travelled - shift * stepPx)
            settle.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val lift by animateFloatAsState(
        targetValue = if (dragging) 1.02f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "reorderLift"
    )

    AppCard(
        modifier = Modifier
            .onSizeChanged { cardHeight = it.height }
            .zIndex(if (dragging) 1f else 0f)
            .graphicsLayer {
                translationY = if (dragging) dragged else settle.value
                scaleX = lift
                scaleY = lift
                shadowElevation = if (dragging) 20f else 0f
            }
            // Segurar em qualquer ponto do card tambem arrasta.
            .pointerInput(prompt.id, canReorder) {
                if (!canReorder) return@pointerInput
                detectDragGesturesAfterLongPress(
                    onDragStart = { dragging = true },
                    onDragEnd = { endDrag() },
                    onDragCancel = { endDrag() }
                ) { _, drag -> onDragDelta(drag.y) }
            }
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (canReorder) {
                    Box(
                        modifier = Modifier
                            .size(width = 26.dp, height = 44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .pointerInput(prompt.id) {
                                detectDragGestures(
                                    onDragStart = { dragging = true },
                                    onDragEnd = { endDrag() },
                                    onDragCancel = { endDrag() }
                                ) { _, drag -> onDragDelta(drag.y) }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.DragIndicator,
                            contentDescription = stringResource(R.string.prompts_drag_handle),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                }
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = prompt.icon, style = MaterialTheme.typography.titleMedium)
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = prompt.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(
                            if (prompt.pinned) R.string.prompts_pinned else R.string.prompts_unpinned
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                Switch(
                    checked = prompt.enabled,
                    onCheckedChange = onToggleEnabled,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                        checkedTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = prompt.prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(12.dp))
            // Quatro acoes divididas em partes iguais: nada de rolar de lado.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SmallAction(
                    icon = if (prompt.pinned) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                    label = stringResource(R.string.prompts_pin),
                    onClick = onTogglePinned,
                    highlighted = prompt.pinned,
                    modifier = Modifier.weight(1f)
                )
                SmallAction(
                    icon = Icons.Rounded.Edit,
                    label = stringResource(R.string.prompts_edit),
                    onClick = onEdit,
                    modifier = Modifier.weight(1f)
                )
                SmallAction(
                    icon = Icons.Rounded.ContentCopy,
                    label = stringResource(R.string.prompts_duplicate),
                    onClick = onDuplicate,
                    modifier = Modifier.weight(1f)
                )
                SmallAction(
                    icon = Icons.Rounded.Delete,
                    label = stringResource(R.string.prompts_delete),
                    onClick = onDelete,
                    destructive = true,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun SmallAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    highlighted: Boolean = false,
    destructive: Boolean = false,
    modifier: Modifier = Modifier
) {
    val tint = when {
        destructive -> MaterialTheme.colorScheme.error
        highlighted -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Column(
        modifier = modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 9.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun PromptEditor(
    initial: CustomPrompt?,
    onCancel: () -> Unit,
    onSave: (String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var icon by remember { mutableStateOf(initial?.icon ?: CustomPrompt.AVAILABLE_ICONS.first()) }
    var body by remember { mutableStateOf(initial?.prompt ?: "") }
    var pinned by remember { mutableStateOf(initial?.pinned ?: false) }
    val emojiFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    // Focar e abrir o teclado no mesmo clique nao funciona direito: o foco so
    // "pega" de verdade um frame depois. Por isso o pedido vira um estado e o
    // show() roda num LaunchedEffect, com uma folga pequena pro foco assentar.
    var openKeyboardRequest by remember { mutableStateOf(0) }
    LaunchedEffect(openKeyboardRequest) {
        if (openKeyboardRequest > 0) {
            emojiFocus.requestFocus()
            delay(80)
            keyboard?.show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 620.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(
                if (initial == null) R.string.editor_create_title else R.string.editor_edit_title
            ),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onCancel),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.common_close),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.editor_icon),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                contentAlignment = Alignment.Center
            ) {
                // Campo vazio mostra o icone padrao esmaecido, como previa de que
                // o atalho ficara com ele se a pessoa nao escolher outro.
                Text(
                    text = icon.ifEmpty { CustomPrompt.AVAILABLE_ICONS.first() },
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.alpha(if (icon.isEmpty()) 0.4f else 1f)
                )
            }
            Spacer(Modifier.width(12.dp))
            OutlinedTextField(
                value = icon,
                onValueChange = { typed -> icon = pickEmoji(typed, icon) },
                label = { Text(stringResource(R.string.editor_emoji_field)) },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(emojiFocus)
            )
        }
        Spacer(Modifier.height(10.dp))
        EmojiPicker(
            selected = icon,
            onSelect = { icon = it },
            onOpenKeyboard = { openKeyboardRequest++ }
        )

        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.editor_name)) },
            placeholder = { Text(stringResource(R.string.editor_name_placeholder)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text(stringResource(R.string.editor_prompt)) },
            placeholder = {
                Text(stringResource(R.string.editor_prompt_placeholder))
            },
            minLines = 4,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable { pinned = !pinned }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.editor_pin),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.editor_pin_desc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = pinned,
                onCheckedChange = { pinned = it },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = MaterialTheme.colorScheme.primary
                )
            )
        }

        Spacer(Modifier.height(20.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(
                onClick = onCancel,
                shape = PillShape,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            ) {
                Text(text = stringResource(R.string.common_cancel), style = MaterialTheme.typography.labelLarge)
            }
            Spacer(Modifier.width(10.dp))
            AuroraButton(
                text = stringResource(R.string.editor_save),
                onClick = {
                    onSave(
                        name,
                        icon.ifBlank { CustomPrompt.AVAILABLE_ICONS.first() },
                        body,
                        pinned
                    )
                },
                enabled = name.isNotBlank() && body.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp)
            )
        }
    }
}

/**
 * Resultado de digitar ou colar no campo de emoji.
 *
 * O texto do campo e sempre so o icone atual, entao o que a pessoa digita vem
 * colado depois dele. Fica o ultimo "caractere" visivel (um emoji pode ter
 * varios codigos, como bandeiras e familias), o que permite trocar por outro
 * emoji. Apagar tudo deixa o campo vazio. Letras e numeros comuns sao
 * ignorados: nao servem de icone e quase sempre sao toque errado.
 */
private fun pickEmoji(typed: String, current: String): String {
    if (typed.isEmpty()) return ""
    val last = lastGrapheme(typed)
    if (last.isBlank()) return current
    return if (last.any { it.code > 0x7F }) last else current
}

/** Ultimo caractere visivel do texto (emoji composto conta como um so). */
private fun lastGrapheme(text: String): String {
    val breaks = BreakIterator.getCharacterInstance()
    breaks.setText(text)
    val end = breaks.last()
    val start = breaks.previous()
    return if (start == BreakIterator.DONE || start < 0) text else text.substring(start, end)
}

/** Espaco entre os cards da lista; entra na conta do arrasto. */
private val LIST_GAP = 10.dp

/** Categorias de emoji oferecidas no editor de funcoes. */
private val EMOJI_CATEGORIES: List<Pair<Int, List<String>>> = listOf(
    R.string.emoji_cat_suggested to listOf(
        "\uD83D\uDCBC", "\u2728", "\uD83D\uDE04", "\uD83D\uDCA1", "\uD83C\uDFAF", "\uD83D\uDD25",
        "\uD83D\uDCDD", "\uD83E\uDDE0", "\uD83D\uDE80", "\u2705", "\uD83D\uDCA0", "\uD83C\uDF10"
    ),
    R.string.emoji_cat_faces to listOf(
        "\uD83D\uDE00", "\uD83D\uDE02", "\uD83D\uDE0A", "\uD83D\uDE07", "\uD83D\uDE0E", "\uD83D\uDE09",
        "\uD83E\uDD29", "\uD83D\uDE18", "\uD83D\uDE14", "\uD83D\uDE21", "\uD83D\uDE31", "\uD83E\uDD14",
        "\uD83D\uDE34", "\uD83E\uDD2D", "\uD83D\uDE0F", "\uD83D\uDE05"
    ),
    R.string.emoji_cat_work to listOf(
        "\uD83D\uDCC8", "\uD83D\uDCCA", "\uD83D\uDCC5", "\uD83D\uDCCE", "\uD83D\uDD8A", "\uD83D\uDCD1",
        "\uD83D\uDCE7", "\uD83D\uDCDE", "\uD83D\uDCB0", "\uD83E\uDD1D", "\u2696", "\uD83D\uDD0E"
    ),
    R.string.emoji_cat_objects to listOf(
        "\uD83D\uDCF1", "\uD83D\uDCBB", "\u2328", "\uD83C\uDFA4", "\uD83C\uDFA7", "\uD83D\uDCF7",
        "\uD83C\uDFAE", "\uD83D\uDD10", "\uD83D\uDD14", "\u23F0", "\uD83D\uDCA3", "\uD83E\uDDF2"
    ),
    R.string.emoji_cat_nature to listOf(
        "\uD83D\uDC36", "\uD83D\uDC31", "\uD83E\uDD8A", "\uD83D\uDC26", "\uD83C\uDF3F", "\uD83C\uDF38",
        "\uD83C\uDF1E", "\uD83C\uDF19", "\u2601", "\u26A1", "\uD83C\uDF0A", "\uD83C\uDF08"
    ),
    R.string.emoji_cat_food to listOf(
        "\u2615", "\uD83C\uDF55", "\uD83C\uDF54", "\uD83C\uDF7F", "\uD83C\uDF70", "\uD83C\uDF7B",
        "\uD83C\uDF89", "\uD83C\uDF81", "\u2764", "\u2B50", "\uD83C\uDFC6", "\uD83D\uDC4D"
    )
)

/** Seletor de emoji com categorias, para qualquer icone caber na funcao. */
@Composable
private fun EmojiPicker(
    selected: String,
    onSelect: (String) -> Unit,
    onOpenKeyboard: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(0) }

    PillChip(
        text = stringResource(if (open) R.string.emoji_close else R.string.emoji_open),
        icon = if (open) Icons.Rounded.Close else Icons.Rounded.EmojiEmotions,
        onClick = { open = !open }
    )

    if (!open) return

    Spacer(Modifier.height(10.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
    ) {
        EMOJI_CATEGORIES.forEachIndexed { index, pair ->
            Box(modifier = Modifier.padding(end = 8.dp)) {
                PillChip(
                    text = stringResource(pair.first),
                    onClick = { category = index },
                    selected = index == category
                )
            }
        }
        // "Outros" abre o teclado de emoji do proprio aparelho: de lá o
        // usuario escolhe qualquer emoji que exista no sistema.
        PillChip(
            text = stringResource(R.string.emoji_others),
            icon = Icons.Rounded.Apps,
            onClick = onOpenKeyboard
        )
    }
    Spacer(Modifier.height(10.dp))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 190.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .verticalScroll(rememberScrollState())
            .padding(10.dp)
    ) {
        EMOJI_CATEGORIES[category].second.chunked(6).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth()) {
                row.forEach { candidate ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .height(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (candidate == selected) {
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHighest
                                }
                            )
                            .clickable { onSelect(candidate) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = candidate, style = MaterialTheme.typography.titleMedium)
                    }
                }
                repeat(6 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
