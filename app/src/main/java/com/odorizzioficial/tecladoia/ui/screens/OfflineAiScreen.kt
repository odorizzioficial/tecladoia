package com.odorizzioficial.tecladoia.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.ai.offline.CatalogModel
import com.odorizzioficial.tecladoia.ai.offline.DownloadProgress
import com.odorizzioficial.tecladoia.ai.offline.InstalledModel
import com.odorizzioficial.tecladoia.ai.offline.OfflineCatalog
import com.odorizzioficial.tecladoia.ai.offline.OfflineMessage
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.ui.MainViewModel
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.ui.components.ErrorBanner
import com.odorizzioficial.tecladoia.ui.components.IconBadge
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.components.SettingRow
import java.util.Locale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.draw.clip
import com.odorizzioficial.tecladoia.ui.components.AuroraBrush
import com.odorizzioficial.tecladoia.ui.theme.PillShape

// --- Menu "Inteligencia artificial" ---------------------------------------

/**
 * Pagina que reune os dois motores: escolhe quem responde (Gemini na nuvem ou
 * o modelo do aparelho) e leva para a configuracao de cada um.
 */
@Composable
internal fun AiHubScreen(
    viewModel: MainViewModel,
    modifier: Modifier,
    onOpenGemini: () -> Unit,
    onOpenOffline: () -> Unit,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.ai_hub_title), onBack = onBack)

        SectionHeader(title = stringResource(R.string.ai_hub_engine_section), icon = Icons.Rounded.Bolt)
        AppCard {
            Column {
                EngineRow(
                    title = stringResource(R.string.ai_engine_gemini_title),
                    subtitle = stringResource(R.string.ai_engine_gemini_sub),
                    icon = Icons.Rounded.Cloud,
                    selected = settings.aiProvider == AiProvider.GEMINI,
                    onClick = { viewModel.setAiProvider(AiProvider.GEMINI) }
                )
                HubDivider()
                EngineRow(
                    title = stringResource(R.string.ai_engine_offline_title),
                    subtitle = stringResource(R.string.ai_engine_offline_sub),
                    icon = Icons.Rounded.Memory,
                    selected = settings.aiProvider == AiProvider.OFFLINE,
                    onClick = {
                        if (settings.offlineModel.isBlank()) {
                            // Sem modelo nao ha o que usar: leva direto para instalar um.
                            viewModel.offline.notify(R.string.ai_hub_offline_needs_model)
                            onOpenOffline()
                        } else {
                            viewModel.setAiProvider(AiProvider.OFFLINE)
                        }
                    }
                )
            }
        }

        SectionHeader(title = stringResource(R.string.ai_hub_setup_section), icon = Icons.Rounded.Tune)
        AppCard {
            Column {
                SettingRow(
                    title = stringResource(R.string.ai_hub_gemini_row),
                    subtitle = stringResource(R.string.ai_hub_gemini_row_sub),
                    icon = Icons.Rounded.Cloud,
                    onClick = onOpenGemini
                ) { RowChevron() }
                HubDivider()
                SettingRow(
                    title = stringResource(R.string.ai_hub_offline_row),
                    subtitle = stringResource(R.string.ai_hub_offline_row_sub),
                    icon = Icons.Rounded.Memory,
                    onClick = onOpenOffline
                ) { RowChevron() }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun EngineRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBadge(icon = icon)
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun RowChevron() {
    Icon(
        Icons.Rounded.ChevronRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(22.dp)
    )
}

@Composable
private fun HubDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

// --- Modo IA offline ------------------------------------------------------

@Composable
internal fun OfflineAiScreen(
    viewModel: MainViewModel,
    modifier: Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val controller = viewModel.offline
    val state by controller.state.collectAsStateWithLifecycle()
    var pendingDelete by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { controller.refresh() }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) controller.importModel(uri)
    }

    fun openPage(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.offline_title), onBack = onBack)
        Text(
            text = stringResource(R.string.offline_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 12.dp)
        )

        state.message?.let { message ->
            MessageCard(message = message, onDismiss = controller::clearMessage)
            Spacer(Modifier.height(12.dp))
        }

        // --- Modelos instalados
        SectionHeader(title = stringResource(R.string.offline_installed_section), icon = Icons.Rounded.Memory)
        AppCard {
            if (state.installed.isEmpty()) {
                Text(
                    text = stringResource(R.string.offline_installed_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                Column {
                    state.installed.forEachIndexed { index, model ->
                        if (index > 0) HubDivider()
                        InstalledRow(
                            model = model,
                            selected = settings.offlineModel == model.fileName,
                            active = settings.offlineModel == model.fileName &&
                                settings.aiProvider == AiProvider.OFFLINE,
                            tooBig = controller.totalRamBytes > 0 &&
                                model.sizeBytes * 2 > controller.totalRamBytes,
                            onSelect = { controller.select(model.fileName) },
                            onDelete = { pendingDelete = model.fileName }
                        )
                    }
                }
            }
        }

        // --- Baixar
        SectionHeader(title = stringResource(R.string.offline_download_section), icon = Icons.Rounded.Download)
        Text(
            text = stringResource(R.string.offline_download_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
        )
        OfflineCatalog.ALL.forEach { model ->
            CatalogCard(
                model = model,
                installed = state.installed.any { it.fileName == model.fileName },
                progress = state.downloads[model.fileName],
                onDownload = { controller.download(model) },
                onCancel = { controller.cancelDownload(model.fileName) },
                onOpenPage = { openPage(model.pageUrl) }
            )
            Spacer(Modifier.height(10.dp))
        }

        // --- Importar
        SectionHeader(title = stringResource(R.string.offline_import_section), icon = Icons.Rounded.Upload)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.offline_import_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                val importing = state.importing
                if (importing != null) {
                    if (importing.total > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (importing.copied.toFloat() / importing.total).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.offline_import_progress,
                                formatSize(importing.copied)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            text = stringResource(R.string.offline_download_cancel),
                            onClick = controller::cancelImport
                        )
                    }
                } else {
                    AuroraButton(
                        text = stringResource(R.string.offline_import_button),
                        icon = Icons.Rounded.Upload,
                        onClick = { picker.launch(arrayOf("*/*")) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // --- Temperatura
        SectionHeader(title = stringResource(R.string.offline_temp_title), icon = Icons.Rounded.Thermostat)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.offline_temp_sub),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = String.format(Locale.getDefault(), "%.1f", settings.offlineTemperature),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = settings.offlineTemperature,
                    onValueChange = viewModel::setOfflineTemperature,
                    valueRange = 0f..1f,
                    steps = 9,
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary,
                        inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest
                    )
                )
            }
        }

        // --- Testar
        Spacer(Modifier.height(16.dp))
        AuroraButton(
            text = stringResource(R.string.offline_test_button),
            icon = Icons.Rounded.Bolt,
            enabled = !state.testing && settings.offlineModel.isNotBlank(),
            onClick = controller::test,
            modifier = Modifier.fillMaxWidth()
        )
        if (state.testing) {
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.offline_testing),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    pendingDelete?.let { name ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text(stringResource(R.string.offline_delete_title)) },
            text = { Text(stringResource(R.string.offline_delete_message, name)) },
            confirmButton = {
                TextButton(onClick = {
                    controller.delete(name)
                    pendingDelete = null
                }) { Text(stringResource(R.string.offline_delete_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

@Composable
private fun InstalledRow(
    model: InstalledModel,
    selected: Boolean,
    /** Escolhido e com o modo offline ligado: e o que esta respondendo agora. */
    active: Boolean,
    tooBig: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (active) {
                    // Fundo em degrade e faixa colorida: o modelo em uso salta aos olhos.
                    Modifier
                        .background(
                            Brush.horizontalGradient(
                                listOf(primary.copy(alpha = 0.38f), secondary.copy(alpha = 0.20f))
                            )
                        )
                } else {
                    Modifier
                }
            )
            .clickable(onClick = onSelect)
            .padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (active) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(PillShape)
                    .background(AuroraBrush)
            )
            Spacer(Modifier.width(10.dp))
        }
        IconBadge(
            icon = Icons.Rounded.Memory,
            container = if (active) primary.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHighest,
            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = model.fileName.substringBeforeLast('.'),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = model.fileName.substringAfterLast('.').uppercase() + "  ·  " +
                    formatSize(model.sizeBytes),
                style = MaterialTheme.typography.bodySmall,
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            if (tooBig) {
                Text(
                    text = stringResource(R.string.offline_warn_ram),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        if (active) {
            // Selo "EM USO" com o degrade do app: so o modelo ativo ganha.
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(AuroraBrush)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.offline_model_active).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = stringResource(R.string.offline_model_delete),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CatalogCard(
    model: CatalogModel,
    installed: Boolean,
    progress: DownloadProgress?,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onOpenPage: () -> Unit
) {
    // Instalado = cartao esverdeado/ciano, para ser reconhecido de longe.
    val cardColor = if (installed) {
        MaterialTheme.colorScheme.secondary.copy(alpha = 0.20f)
            .compositeOver(MaterialTheme.colorScheme.surfaceContainer)
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    AppCard(color = cardColor) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = model.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(model.descriptionRes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = model.format.extension.removePrefix(".").uppercase() + "  ·  " +
                        formatSize(model.approxBytes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(12.dp))
            when {
                installed -> Row(
                    modifier = Modifier
                        .clip(PillShape)
                        .background(MaterialTheme.colorScheme.secondary)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.offline_download_installed),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                progress != null -> Column {
                    if (progress.total > 0) {
                        LinearProgressIndicator(
                            progress = {
                                (progress.downloaded.toFloat() / progress.total).coerceIn(0f, 1f)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.offline_download_progress,
                                formatSize(progress.downloaded),
                                formatSize(if (progress.total > 0) progress.total else model.approxBytes)
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        PillChip(
                            text = stringResource(R.string.offline_download_cancel),
                            onClick = onCancel
                        )
                    }
                }

                else -> Row(verticalAlignment = Alignment.CenterVertically) {
                    AuroraButton(
                        text = stringResource(R.string.offline_download_button),
                        icon = Icons.Rounded.Download,
                        onClick = onDownload,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    PillChip(
                        text = stringResource(R.string.offline_open_page),
                        icon = Icons.Rounded.OpenInNew,
                        onClick = onOpenPage,
                        height = 52.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageCard(message: OfflineMessage, onDismiss: () -> Unit) {
    val text = stringResource(message.res, *message.args.toTypedArray())
    if (message.isError) {
        ErrorBanner(
            message = text,
            onDismiss = onDismiss,
            actionLabel = stringResource(R.string.common_ok)
        )
    } else {
        AppCard {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_ok)) }
            }
        }
    }
}

private fun formatSize(bytes: Long): String =
    if (bytes >= 1_000_000_000L) {
        String.format(Locale.getDefault(), "%.2f GB", bytes / 1_000_000_000.0)
    } else {
        String.format(Locale.getDefault(), "%d MB", bytes / 1_000_000L)
    }
