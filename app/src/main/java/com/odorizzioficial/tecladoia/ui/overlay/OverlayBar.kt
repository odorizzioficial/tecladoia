package com.odorizzioficial.tecladoia.ui.overlay

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Mood
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Undo
import androidx.compose.material.icons.rounded.VerticalAlignBottom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.GeminiModels
import com.odorizzioficial.tecladoia.domain.Tone
import com.odorizzioficial.tecladoia.service.OverlayActions
import com.odorizzioficial.tecladoia.service.OverlayController
import com.odorizzioficial.tecladoia.service.OverlayMode
import com.odorizzioficial.tecladoia.service.OverlayUiState
import com.odorizzioficial.tecladoia.service.VoiceStage
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraBrush
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.ui.components.ErrorBanner
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.iconVector
import com.odorizzioficial.tecladoia.ui.theme.AiKeyboardTheme
import com.odorizzioficial.tecladoia.ui.theme.PillShape
import kotlinx.coroutines.withTimeoutOrNull

/** Raiz da janela flutuante desenhada pelo AccessibilityService. */
@Composable
fun OverlayRoot(controller: OverlayController) {
    val state by controller.state.collectAsState()
    AiKeyboardTheme(themeMode = state.settings.themeMode) {
        AiOverlay(state = state, actions = controller)
    }
}

/**
 * Barra + painel. O mesmo composable e reutilizado pela tela "Prévia" dentro do
 * app, por isso ele nao conhece WindowManager nem o servico.
 */
@Composable
fun AiOverlay(
    state: OverlayUiState,
    actions: OverlayActions,
    modifier: Modifier = Modifier
) {
    if (state.mode == OverlayMode.MINI) {
        MiniBar(actions = actions, modifier = modifier)
        return
    }
    if (state.mode == OverlayMode.VOICE) {
        VoiceBar(state = state, actions = actions, modifier = modifier)
        return
    }
    Column(modifier = modifier.fillMaxWidth()) {
        state.errorMessage?.takeIf { state.mode == OverlayMode.COLLAPSED }?.let { message ->
            ErrorBanner(
                message = message,
                onDismiss = actions::onDismissError,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                actionLabel = stringResource(if (state.canRetry) R.string.common_retry else R.string.common_ok),
                onAction = if (state.canRetry) actions::onRetry else null
            )
        }
        // A janela do overlay e WRAP_CONTENT: animar a ALTURA do painel faria o
        // WindowManager refazer o layout da janela a cada frame, e era isso que
        // deixava a abertura e o fechamento aos pulos. Aqui a altura muda uma
        // unica vez e so a opacidade/escala e animada, dentro da GPU.
        if (state.mode == OverlayMode.TONE) {
            val instant = state.settings.animationStyle == AnimationStyle.NONE
            val reveal = remember(state.mode) { Animatable(if (instant) 1f else 0f) }
            LaunchedEffect(state.mode, instant) {
                if (!instant) reveal.animateTo(1f, tween(110, easing = LinearOutSlowInEasing))
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .graphicsLayer {
                        alpha = reveal.value
                        scaleX = 0.97f + 0.03f * reveal.value
                        scaleY = 0.97f + 0.03f * reveal.value
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
            ) {
                TonePanel(state, actions)
            }
        }
        // Animacao 2 do catalogo (Animated Visibility): slideIn + fadeIn +
        // scaleIn combinados na entrada e o inverso na saida, quando o teclado
        // abre e fecha. Sem expandIn/shrinkOut de proposito: a barra ja nasce
        // com a altura final, entao a janela do overlay nao e remedida a cada
        // frame - era isso que engasgava a animacao.
        AnimatedVisibility(
            visible = state.barVisible,
            enter = when (state.settings.animationStyle) {
                AnimationStyle.NONE -> EnterTransition.None
                AnimationStyle.SIMPLE -> fadeIn(tween(BAR_ANIM_DURATION_MS))
                AnimationStyle.FULL ->
                    slideInVertically(
                        animationSpec = tween(BAR_ANIM_DURATION_MS, easing = LinearOutSlowInEasing)
                    ) { height -> height / 2 } +
                        fadeIn(tween(BAR_ANIM_DURATION_MS)) +
                        scaleIn(
                            animationSpec = tween(BAR_ANIM_DURATION_MS, easing = LinearOutSlowInEasing),
                            initialScale = BAR_ANIM_INITIAL_SCALE
                        )
            },
            exit = when (state.settings.animationStyle) {
                AnimationStyle.NONE -> ExitTransition.None
                AnimationStyle.SIMPLE -> fadeOut(tween(BAR_ANIM_DURATION_MS))
                AnimationStyle.FULL ->
                    slideOutVertically(
                        animationSpec = tween(BAR_ANIM_DURATION_MS, easing = FastOutLinearInEasing)
                    ) { height -> height / 2 } +
                        fadeOut(tween(BAR_ANIM_DURATION_MS)) +
                        scaleOut(
                            animationSpec = tween(BAR_ANIM_DURATION_MS, easing = FastOutLinearInEasing),
                            targetScale = BAR_ANIM_INITIAL_SCALE
                        )
            },
            label = "bar-visibility"
        ) {
            CompactBar(state = state, actions = actions)
        }
    }
}

// --- Barra compacta -------------------------------------------------------

@Composable
private fun CompactBar(state: OverlayUiState, actions: OverlayActions) {
    val defaultScroll = rememberScrollState()
    val customScroll = rememberScrollState()
    val custom = state.customChips
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.98f))
            .heightIn(min = state.settings.barHeight.heightDp.dp)
            // Puxar para baixo troca os atalhos padrao pelos do usuario; para
            // cima volta. A barra nao muda de tamanho nem abre painel, e a
            // rolagem horizontal dos chips consome apenas o movimento lateral,
            // entao os dois gestos convivem sem mexer no teclado.
            .pointerInput(custom) {
                var travelled = 0f
                var fired = false
                detectVerticalDragGestures(
                    onDragStart = {
                        travelled = 0f
                        fired = false
                    },
                    onDragEnd = { fired = false },
                    onDragCancel = { fired = false }
                ) { _, delta ->
                    travelled += delta
                    if (!fired) {
                        if (travelled > PULL_THRESHOLD_PX && !custom) {
                            fired = true
                            actions.onToggleChipPage()
                        } else if (travelled < -PULL_THRESHOLD_PX && custom) {
                            fired = true
                            actions.onToggleChipPage()
                        }
                    }
                }
            }
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DragHandle(actions = actions)
        // O chip de IA volta (ou vai) para as funcoes padrao da IA. Segurar
        // por 2 segundos abre o app direto na aba Funcoes.
        AiToggleChip(
            expanded = custom,
            onClick = actions::onToggleChipPage,
            onLongHold = actions::onOpenAppFunctions
        )
        Spacer(Modifier.width(8.dp))
        if (state.busy) {
            Row(
                modifier = Modifier.weight(1f).padding(start = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.actionLabel.ifBlank { stringResource(R.string.overlay_ai) } + "…",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
        // Troca de pagina: os chips atuais saem deslizando para um lado e os
        // novos entram do outro, com um fade curto. O tamanho nao e animado
        // (snap), para a barra nao mudar de altura nem de posicao.
        AnimatedContent(
            targetState = custom,
            transitionSpec = {
                val toCustom = targetState && !initialState
                val direction = if (toCustom) 1 else -1
                val enter = slideInHorizontally(
                    animationSpec = tween(200, easing = LinearOutSlowInEasing)
                ) { width -> direction * width / 5 } + fadeIn(tween(170))
                val exit = slideOutHorizontally(
                    animationSpec = tween(180, easing = FastOutLinearInEasing)
                ) { width -> -direction * width / 5 } + fadeOut(tween(120))
                enter togetherWith exit using SizeTransform(clip = false) { _, _ -> snap() }
            },
            label = "chipPage",
            modifier = Modifier.weight(1f)
        ) { showCustom ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(if (showCustom) customScroll else defaultScroll),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (state.canUndo) {
                    PillChip(
                        text = stringResource(R.string.common_undo),
                        icon = Icons.Rounded.Undo,
                        onClick = actions::onUndo,
                        accent = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                if (showCustom) {
                    // Funcoes do usuario: emoji + nome, nada mais. Executam o
                    // prompt salvo, igual ao que ja acontecia.
                    val prompts = state.enabledPrompts.sortedByDescending { it.pinned }
                    if (prompts.isEmpty()) {
                        PillChip(
                            text = stringResource(R.string.overlay_create_prompts),
                            onClick = actions::onOpenApp,
                            accent = MaterialTheme.colorScheme.tertiary
                        )
                    } else {
                        prompts.forEach { prompt ->
                            PillChip(
                                text = prompt.name,
                                leadingEmoji = prompt.icon,
                                onClick = { actions.onCustomPrompt(prompt) },
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                } else {
                    // Funcoes padrao da IA, em ciano.
                    AiAction.entries.forEach { action ->
                        PillChip(
                            text = stringResource(action.labelRes),
                            icon = action.iconVector(),
                            onClick = { actions.onAction(action) },
                            accent = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
        }
        Spacer(Modifier.width(4.dp))
        if (state.busy) {
            Box(
                modifier = Modifier.size(40.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        } else {
            RoundIconButton(
                icon = Icons.Rounded.Mic,
                contentDescription = stringResource(R.string.overlay_dictate),
                onClick = actions::onVoiceStart,
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        RoundIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = stringResource(R.string.overlay_minimize),
            onClick = actions::onMinimize,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Estado minimizado: so um botao pequeno, alinhado a esquerda, com o resto da
 * linha transparente para o app de baixo continuar legivel.
 */
@Composable
private fun MiniBar(actions: OverlayActions, modifier: Modifier = Modifier) {
    // A janela do overlay fica em WRAP_CONTENT neste modo, entao o botao ocupa
    // apenas o proprio tamanho e todo o resto da tela volta para o app.
    Row(
        modifier = modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.95f))
                .clickable(onClick = actions::onRestoreBar)
                .pointerInput(Unit) {
                    detectDragGestures { _, drag -> actions.onDrag(drag.x, drag.y) }
                }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = stringResource(R.string.overlay_show_bar),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.overlay_ai),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/** Alca de arrasto: leva a janela para qualquer ponto da tela. */
@Composable
private fun DragHandle(actions: OverlayActions) {
    Box(
        modifier = Modifier
            .size(28.dp, 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .pointerInput(Unit) {
                detectDragGestures { _, drag -> actions.onDrag(drag.x, drag.y) }
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            Icons.Rounded.DragIndicator,
            contentDescription = stringResource(R.string.overlay_drag_bar),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun AiToggleChip(expanded: Boolean, onClick: () -> Unit, onLongHold: () -> Unit) {
    val background: Brush = if (expanded) {
        AuroraBrush
    } else {
        SolidColor(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
    }
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(PillShape)
            .background(background)
            // Toque curto alterna os atalhos padrao/personalizados (onClick).
            // Segurar por AI_CHIP_HOLD_MS abre o app direto na aba Funcoes,
            // sem disparar o toque curto quando o tempo se esgota.
            .pointerInput(onClick, onLongHold) {
                awaitEachGesture {
                    awaitFirstDown()
                    val releasedEarly = withTimeoutOrNull(AI_CHIP_HOLD_MS) {
                        waitForUpOrCancellation()
                    }
                    if (releasedEarly != null) {
                        onClick()
                    } else {
                        onLongHold()
                        waitForUpOrCancellation()
                    }
                }
            }
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.AutoAwesome,
            contentDescription = stringResource(R.string.overlay_open_actions),
            tint = if (expanded) Color.White else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = stringResource(R.string.overlay_ai),
            style = MaterialTheme.typography.labelLarge,
            color = if (expanded) Color.White else MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun RoundIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    container: Color = Color.Transparent
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(PillShape)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// --- Paineis --------------------------------------------------------------

@Composable
private fun PanelHeader(
    title: String,
    subtitle: String,
    actions: OverlayActions,
    showSettings: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(PillShape)
                .background(AuroraBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.secondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (showSettings) {
            RoundIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.overlay_open_settings),
                onClick = actions::onOpenApp
            )
        }
        RoundIconButton(
            icon = Icons.Rounded.VerticalAlignBottom,
            contentDescription = stringResource(R.string.overlay_reset_position),
            onClick = actions::onResetPosition
        )
        RoundIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = stringResource(R.string.common_close),
            onClick = actions::onCollapse
        )
    }
}

@Composable
private fun TonePanel(state: OverlayUiState, actions: OverlayActions) {
    AppCard {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            PanelHeader(
                title = stringResource(R.string.overlay_tone_title),
                subtitle = stringResource(R.string.overlay_tone_subtitle, Tone.entries.size),
                actions = actions,
                showSettings = false
            )
            state.errorMessage?.let {
                ErrorBanner(
                    message = it,
                    onDismiss = actions::onDismissError,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 4.dp)
                )
            }
            Column(modifier = Modifier.padding(horizontal = 14.dp)) {
                Tone.entries.chunked(3).forEach { row ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        row.forEach { tone ->
                            Box(modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) {
                                PillChip(
                                    text = stringResource(tone.labelRes),
                                    onClick = { actions.onToneSelected(tone) },
                                    selected = tone == state.selectedTone,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        if (row.size < 3) {
                            repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(
                        R.string.overlay_tone_selected,
                        stringResource(state.selectedTone.labelRes),
                        stringResource(state.selectedTone.descriptionRes)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .padding(12.dp)
                )
            }
        }
    }
}

/**
 * Ditado dentro da propria barra: onda, cronometro, previa do texto e apenas
 * dois botoes. O reconhecimento nao para no silencio, so quando o usuario toca
 * em Concluir.
 */
@Composable
private fun VoiceBar(
    state: OverlayUiState,
    actions: OverlayActions,
    modifier: Modifier = Modifier
) {
    val voice = state.voice
    val recording = voice.stage == VoiceStage.RECORDING
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.98f))
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(PillShape)
                    .background(
                        if (recording) {
                            MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Mic,
                    contentDescription = null,
                    tint = if (recording) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Box(modifier = Modifier.weight(1f)) {
                if (recording) {
                    Waveform(enabled = state.settings.animationStyle != AnimationStyle.NONE)
                } else {
                    Text(
                        text = when (voice.stage) {
                            VoiceStage.PROCESSING -> stringResource(R.string.overlay_voice_transcribing)
                            VoiceStage.ERROR -> voice.message.ifBlank { stringResource(R.string.overlay_voice_failed) }
                            else -> stringResource(R.string.overlay_voice_title)
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = formatSeconds(voice.seconds),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.width(8.dp))
            RoundIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = stringResource(R.string.overlay_voice_cancel),
                onClick = actions::onVoiceCancel,
                tint = MaterialTheme.colorScheme.error
            )
            RoundIconButton(
                icon = Icons.Rounded.Check,
                contentDescription = stringResource(R.string.overlay_voice_done),
                onClick = actions::onVoiceStop,
                tint = MaterialTheme.colorScheme.secondary,
                container = MaterialTheme.colorScheme.secondary.copy(alpha = 0.18f)
            )
        }
        if (voice.transcript.isNotBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(
                text = voice.transcript,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun Waveform(enabled: Boolean) {
    val transition = rememberInfiniteTransition(label = "waveform")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(vertical = 18.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(WAVE_BARS) { index ->
            val scale by transition.animateFloat(
                initialValue = 0.35f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 520 + (index % 5) * 90),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "bar$index"
            )
            val height = if (enabled) (12 + (scale * 34)).dp else 26.dp
            Box(
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .width(4.dp)
                    .height(height)
                    .clip(PillShape)
                    .background(
                        if (index % 2 == 0) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.tertiary
                        }
                    )
            )
        }
    }
}

private fun formatSeconds(total: Int): String {
    val minutes = total / 60
    val seconds = total % 60
    return "%02d:%02d".format(minutes, seconds)
}

private const val WAVE_BARS = 15

/** Acoes que aparecem direto na barra compacta. */
/** Duracao da entrada e da saida da barra, em milissegundos. */
private const val BAR_ANIM_DURATION_MS = 220

/** Escala inicial da barra ao entrar (e final ao sair). */
private const val BAR_ANIM_INITIAL_SCALE = 0.92f

/** Deslocamento vertical que confirma a puxada da barra, em pixels. */
private const val PULL_THRESHOLD_PX = 34f

/** Tempo segurando o chip de IA para abrir o app direto na aba Funcoes. */
private const val AI_CHIP_HOLD_MS = 2000L
