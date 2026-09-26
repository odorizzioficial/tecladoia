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
import com.odorizzioficial.tecladoia.ui.components.ErrorBanner
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.components.iconVector
import com.odorizzioficial.tecladoia.ui.theme.PillShape

@Composable
fun AssistantScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val playground by viewModel.playground.collectAsStateWithLifecycle()
    val serviceRunning by KeyboardOverlayService.connected.collectAsStateWithLifecycle()
    val clipboard = LocalClipboardManager.current

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

        if (!settings.hasApiKey) {
            Spacer(Modifier.height(14.dp))
            ErrorBanner(
                message = stringResource(R.string.err_missing_key),
                onDismiss = {},
                actionLabel = stringResource(R.string.assistant_configure_now),
                onAction = viewModel::requestApiKeyScreen,
                showDismiss = false
            )
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
