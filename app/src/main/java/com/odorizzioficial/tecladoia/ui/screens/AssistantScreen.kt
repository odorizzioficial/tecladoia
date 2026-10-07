package com.odorizzioficial.tecladoia.ui.screens

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.domain.AiAction
import com.odorizzioficial.tecladoia.service.KeyboardOverlayService
import com.odorizzioficial.tecladoia.ui.MainViewModel
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraBrush
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.ui.components.ErrorBanner
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.components.iconVector
import com.odorizzioficial.tecladoia.ui.theme.PillShape
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import com.odorizzioficial.tecladoia.domain.AppSettings
import com.odorizzioficial.tecladoia.domain.GeminiModels
import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.ui.graphics.vector.ImageVector

/** Aviso de versao nova no topo do Assistente; "Depois" some com ele para essa versao. */
@Composable
private fun UpdateBanner(version: String, onUpdate: () -> Unit, onLater: () -> Unit) {
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(secondary.copy(alpha = 0.14f))
            .border(1.dp, secondary.copy(alpha = 0.5f), shape)
            .padding(14.dp)
    ) {
        Text(
            text = stringResource(R.string.update_banner_title, version),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            PillChip(
                text = stringResource(R.string.update_banner_update),
                onClick = onUpdate,
                selected = true
            )
            Spacer(Modifier.width(10.dp))
            PillChip(text = stringResource(R.string.update_banner_later), onClick = onLater)
        }
    }
}

/** Cor de alerta para motor desligado ou sem configuracao. */
private val WarningAmber = Color(0xFFFFB300)

/**
 * Os dois motores de IA no topo do Assistente. O que esta em uso (e pronto) aparece
 * "Online", em azul; o outro fica apagado e "Offline", em amarelo de alerta. Um motor
 * escolhido mas sem chave ou sem modelo tambem fica "Offline" amarelo, no lugar do
 * aviso vermelho que existia mais abaixo.
 *
 * Toque no motor apagado liga esse motor (ou abre a configuracao dele se faltar algo);
 * toque no que esta em uso abre o menu de IA; segurar abre direto a configuracao.
 */
@Composable
private fun EngineSection(settings: AppSettings, viewModel: MainViewModel) {
    val offlineInUse = settings.aiProvider == AiProvider.OFFLINE
    val geminiReady = settings.hasApiKey
    val offlineReady = settings.offlineModel.isNotBlank()

    Text(
        text = stringResource(R.string.engine_badge_label),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
    EngineCard(
        icon = Icons.Rounded.Cloud,
        title = stringResource(R.string.ai_engine_gemini_title),
        detail = if (geminiReady) {
            GeminiModels.prettyLabel(settings.model)
        } else {
            stringResource(R.string.engine_gemini_no_key)
        },
        active = !offlineInUse,
        ready = geminiReady,
        onClick = {
            when {
                !offlineInUse -> viewModel.requestAiHubScreen()
                geminiReady -> viewModel.setAiProvider(AiProvider.GEMINI)
                else -> viewModel.requestApiKeyScreen()
            }
        },
        onLongClick = viewModel::requestApiKeyScreen
    )
    Spacer(Modifier.height(10.dp))
    EngineCard(
        icon = Icons.Rounded.Memory,
        title = stringResource(R.string.ai_engine_offline_title),
        detail = if (offlineReady) {
            settings.offlineModel.substringBeforeLast('.')
        } else {
            stringResource(R.string.engine_badge_no_model)
        },
        active = offlineInUse,
        ready = offlineReady,
        onClick = {
            when {
                offlineInUse -> viewModel.requestAiHubScreen()
                offlineReady -> viewModel.setAiProvider(AiProvider.OFFLINE)
                else -> viewModel.requestOfflineScreen()
            }
        },
        onLongClick = viewModel::requestOfflineScreen
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text = stringResource(R.string.engine_badge_hint),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun EngineCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    detail: String,
    active: Boolean,
    ready: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(24.dp)

    // Online = em uso e pronto. Todo o resto e Offline, e em uso sem estar pronto pede atencao.
    val online = active && ready
    val alert = active && !ready

    val background = when {
        online -> Brush.linearGradient(listOf(primary.copy(alpha = 0.30f), secondary.copy(alpha = 0.16f)))
        alert -> Brush.linearGradient(listOf(WarningAmber.copy(alpha = 0.20f), WarningAmber.copy(alpha = 0.06f)))
        else -> Brush.linearGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceContainer,
                MaterialTheme.colorScheme.surfaceContainer
            )
        )
    }
    val borderBrush = when {
        online -> Brush.linearGradient(listOf(primary.copy(alpha = 0.75f), secondary.copy(alpha = 0.55f)))
        alert -> Brush.linearGradient(listOf(WarningAmber.copy(alpha = 0.8f), WarningAmber.copy(alpha = 0.5f)))
        else -> Brush.linearGradient(
            listOf(Color.White.copy(alpha = 0.08f), Color.White.copy(alpha = 0.08f))
        )
    }
    // Motor apagado: tudo mais fraco, menos o selo de estado.
    val contentAlpha = if (active) 1f else 0.55f

    val pulse = rememberInfiniteTransition(label = "engine-pulse")
    val dotAlpha by pulse.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "engine-dot"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .pointerInput(onClick, onLongClick) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                )
            }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(PillShape)
                .background(
                    if (online) {
                        AuroraBrush
                    } else {
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.surfaceContainerHighest,
                                MaterialTheme.colorScheme.surfaceContainerHighest
                            )
                        )
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = if (online) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = contentAlpha),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = if (!ready) {
                    WarningAmber
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Spacer(Modifier.width(10.dp))
        val pillColor = if (online) secondary else WarningAmber
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(pillColor.copy(alpha = 0.18f))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(PillShape)
                    .background(pillColor.copy(alpha = if (online) dotAlpha else 1f))
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(
                    if (online) R.string.engine_state_online else R.string.engine_state_offline
                ),
                style = MaterialTheme.typography.labelMedium,
                color = pillColor
            )
        }
    }
}

@Composable
fun AssistantScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val playground by viewModel.playground.collectAsStateWithLifecycle()
    val serviceRunning by KeyboardOverlayService.connected.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current
    val micPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) viewModel.startPlaygroundVoice() else viewModel.notifyPlaygroundMicDenied()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(PillShape)
                    .background(AuroraBrush),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = null,
                    tint = androidx.compose.ui.graphics.Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(16.dp))
        val update by viewModel.update.collectAsStateWithLifecycle()
        val newVersion = update.available?.takeIf { it.version != settings.dismissedUpdate }
        if (newVersion != null) {
            UpdateBanner(
                version = newVersion.version,
                onUpdate = viewModel::requestUpdateScreen,
                onLater = { viewModel.dismissUpdate(newVersion.version) }
            )
            Spacer(Modifier.height(16.dp))
        }
        EngineSection(settings = settings, viewModel = viewModel)

        SectionHeader(title = stringResource(R.string.assistant_service_state), icon = Icons.Rounded.CheckCircle)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                StatusGlow(active = serviceRunning)
                Spacer(Modifier.height(14.dp))
                Text(
                    text = stringResource(
                        if (serviceRunning) {
                            R.string.assistant_active_desc
                        } else {
                            R.string.assistant_inactive_desc
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AuroraButton(
                    text = stringResource(
                        if (serviceRunning) {
                            R.string.assistant_open_service
                        } else {
                            R.string.assistant_activate_service
                        }
                    ),
                    icon = Icons.Rounded.OpenInNew,
                    onClick = {
                        context.startActivity(
                            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                if (!serviceRunning) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.assistant_blocked_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }

        SectionHeader(title = stringResource(R.string.assistant_test_title), icon = Icons.Rounded.AutoAwesome)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = playground.input,
                    onValueChange = viewModel::onPlaygroundInput,
                    label = { Text(stringResource(R.string.assistant_test_label)) },
                    placeholder = { Text(stringResource(R.string.assistant_test_placeholder)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                // Microfone: o mesmo ditado em tempo real da barra, para testar aqui.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PillChip(
                        text = stringResource(
                            if (playground.listening) R.string.assistant_mic_stop else R.string.assistant_mic_start
                        ),
                        icon = if (playground.listening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                        selected = playground.listening,
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            when {
                                playground.listening -> viewModel.stopPlaygroundVoice()
                                granted -> viewModel.startPlaygroundVoice()
                                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    )
                    if (playground.listening) {
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.assistant_mic_listening),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    AiAction.entries.forEach { action ->
                        PillChip(
                            text = stringResource(action.labelRes),
                            icon = action.iconVector(),
                            onClick = { viewModel.runPlayground(action) }
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
                if (prompts.any { it.enabled }) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        prompts.filter { it.enabled }.forEach { prompt ->
                            PillChip(
                                text = prompt.name,
                                leadingEmoji = prompt.icon,
                                onClick = { viewModel.runPlaygroundCustom(prompt) },
                                accent = MaterialTheme.colorScheme.tertiary
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                    }
                }

                playground.error?.let {
                    Spacer(Modifier.height(12.dp))
                    ErrorBanner(message = it, onDismiss = viewModel::clearPlaygroundError)
                }

                if (playground.running) {
                    Spacer(Modifier.height(16.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.common_processing),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (playground.output.isNotBlank()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = playground.output,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(14.dp)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.Start) {
                        PillChip(
                            text = stringResource(R.string.assistant_copy_result),
                            icon = Icons.Rounded.ContentCopy,
                            onClick = {
                                clipboard.setText(AnnotatedString(playground.output))
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Selo grande de estado com brilho: verde quando o servico esta ativo, vermelho
 * quando nao. O brilho e feito com gradientes radiais empilhados, entao funciona
 * em qualquer versao do Android, sem depender de blur.
 */
@Composable
private fun StatusGlow(active: Boolean) {
    val accent = if (active) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.error
    }
    val pulse = rememberInfiniteTransition(label = "statusPulse")
    val alpha by pulse.animateFloat(
        initialValue = 0.28f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "statusAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        contentAlignment = Alignment.Center
    ) {
        // Halo externo
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(96.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(accent.copy(alpha = alpha * 0.45f), Color.Transparent)
                    )
                )
        )
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(accent.copy(alpha = 0.16f))
                .border(width = 1.dp, color = accent.copy(alpha = alpha), shape = PillShape)
                .padding(horizontal = 26.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(PillShape)
                    .background(accent)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(if (active) R.string.assistant_active else R.string.assistant_inactive),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = accent,
                letterSpacing = 2.sp
            )
        }
    }
}
