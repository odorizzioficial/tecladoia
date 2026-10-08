package com.odorizzioficial.tecladoia.ui.screens

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
import android.content.pm.ApplicationInfo
import android.content.Intent
import android.os.Build
import android.os.LocaleList
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.VpnKey
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.produceState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.data.LocaleHelper
import com.odorizzioficial.tecladoia.domain.AiProvider
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.AppLanguage
import com.odorizzioficial.tecladoia.domain.AppLanguages
import com.odorizzioficial.tecladoia.domain.BarHeight
import com.odorizzioficial.tecladoia.domain.Changelog
import com.odorizzioficial.tecladoia.domain.GeminiModels
import com.odorizzioficial.tecladoia.domain.Languages
import com.odorizzioficial.tecladoia.domain.ThemeMode
import com.odorizzioficial.tecladoia.ui.ConnectionStage
import com.odorizzioficial.tecladoia.ui.BackupUiState
import com.odorizzioficial.tecladoia.ui.MainViewModel
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.ui.components.ErrorBanner
import com.odorizzioficial.tecladoia.ui.components.IconBadge
import com.odorizzioficial.tecladoia.ui.components.OemHelp
import com.odorizzioficial.tecladoia.ui.components.PermissionsCard
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.components.SettingRow
import com.odorizzioficial.tecladoia.ui.components.ToggleRow
import com.odorizzioficial.tecladoia.ui.theme.PillShape
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.ui.text.style.TextOverflow
import com.odorizzioficial.tecladoia.service.KeyboardOverlayService
import com.odorizzioficial.tecladoia.service.SensitiveApps
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.odorizzioficial.tecladoia.data.UpdateChecker
import com.odorizzioficial.tecladoia.data.UpdateError
import android.content.pm.PackageManager
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Notifications
import com.odorizzioficial.tecladoia.ui.components.AuroraBrush
import com.odorizzioficial.tecladoia.data.InstallFailure
import com.odorizzioficial.tecladoia.data.InstallState
import com.odorizzioficial.tecladoia.data.UpdateInstaller
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.text.style.TextAlign

/** Link oficial para criar a chave da API Gemini. */
private const val AI_STUDIO_URL = "https://aistudio.google.com/apikey"

/** Canal do autor do app. */
private const val YOUTUBE_URL = "https://www.youtube.com/@odorizzioficial"

/** Paginas internas da aba Ajustes. */
private enum class SettingsPage { LIST, AI_HUB, GEMINI, OFFLINE, OVERLAY, APPEARANCE, PERMISSIONS, BACKUP, PROTECTED, UPDATE, ABOUT }

/** Assuntos do menu Sobre, cada um com a sua propria pagina. */
private enum class AboutTopic(@androidx.annotation.StringRes val titleRes: Int) {
    NONE(R.string.about_title),
    APP(R.string.about_topic_app),
    PERMISSION(R.string.about_topic_permission),
    DATA(R.string.about_topic_data),
    NEWS(R.string.about_topic_news),
    BLOCKED(R.string.about_topic_blocked)
}

/**
 * Constantes da animação 12 do catálogo de animações do skydoves
 * (Shared Bounds Expansion): o card do menu vira a página, com os limites
 * interpolados entre os dois estados.
 */
private const val BOUNDS_DURATION_MS = 420
private val BOUNDS_EASING = FastOutSlowInEasing

/** Duracao da troca entre a lista do Sobre e cada topico dela. */
private const val ABOUT_TOPIC_ANIM_MS = 220

/** Depois disto sem a instalacao acontecer, a tela de espera oferece tentar de novo. */
private const val INSTALL_STUCK_AFTER_MS = 60_000L

/** Duracao da entrada/saida do preview de altura da barra, e por quanto tempo ele fica visivel. */
private const val BAR_HEIGHT_PREVIEW_ANIM_MS = 260
private const val BAR_HEIGHT_PREVIEW_MS = 2_500L

/**
 * Escopos da transição compartilhada. Nulo quando o usuário escolheu um modo
 * de animação mais leve: aí as páginas trocam sem correlacionar limites.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
private class SharedPageScopes(
    val shared: SharedTransitionScope,
    val visibility: AnimatedVisibilityScope
)

/** Liga o card do menu e a página pela mesma chave. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun Modifier.sharedPage(scopes: SharedPageScopes?, key: String): Modifier {
    if (scopes == null) return this
    return with(scopes.shared) {
        this@sharedPage.sharedBounds(
            sharedContentState = rememberSharedContentState(key = key),
            animatedVisibilityScope = scopes.visibility,
            boundsTransform = { _, _ ->
                tween(durationMillis = BOUNDS_DURATION_MS, easing = BOUNDS_EASING)
            }
        )
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun SettingsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    var page by rememberSaveable { mutableStateOf(SettingsPage.LIST) }
    var topic by rememberSaveable { mutableStateOf(AboutTopic.NONE) }
    val apiKeyRequest by viewModel.openApiKeyRequest.collectAsStateWithLifecycle()
    val offlineRequest by viewModel.openOfflineRequest.collectAsStateWithLifecycle()
    val aiHubRequest by viewModel.openAiHubRequest.collectAsStateWithLifecycle()
    val updateRequest by viewModel.openUpdateRequest.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val style = settings.animationStyle

    LaunchedEffect(apiKeyRequest) {
        if (apiKeyRequest) {
            page = SettingsPage.GEMINI
            viewModel.consumeApiKeyRequest()
        }
    }
    LaunchedEffect(offlineRequest) {
        if (offlineRequest) {
            page = SettingsPage.OFFLINE
            viewModel.consumeOfflineRequest()
        }
    }
    LaunchedEffect(aiHubRequest) {
        if (aiHubRequest) {
            page = SettingsPage.AI_HUB
            viewModel.consumeAiHubRequest()
        }
    }
    LaunchedEffect(updateRequest) {
        if (updateRequest) {
            page = SettingsPage.UPDATE
            viewModel.consumeUpdateRequest()
        }
    }

    SharedTransitionLayout(modifier = modifier) {
        AnimatedContent(
            targetState = page,
            transitionSpec = {
                when (style) {
                    AnimationStyle.NONE -> EnterTransition.None togetherWith ExitTransition.None
                    else -> fadeIn(tween(BOUNDS_DURATION_MS, easing = BOUNDS_EASING)) togetherWith
                        fadeOut(tween(BOUNDS_DURATION_MS, easing = BOUNDS_EASING))
                }
            },
            label = "settings-page"
        ) { current ->
            val scopes = if (style == AnimationStyle.FULL) {
                SharedPageScopes(this@SharedTransitionLayout, this@AnimatedContent)
            } else {
                null
            }

            when (current) {
                SettingsPage.AI_HUB -> AiHubScreen(
                    viewModel = viewModel,
                    modifier = Modifier.sharedPage(scopes, "page-ai-hub"),
                    onOpenGemini = { page = SettingsPage.GEMINI },
                    onOpenOffline = { page = SettingsPage.OFFLINE },
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.OFFLINE -> OfflineAiScreen(
                    viewModel = viewModel,
                    modifier = Modifier.sharedPage(scopes, "page-offline"),
                    onBack = { page = SettingsPage.AI_HUB }
                )

                SettingsPage.UPDATE -> UpdateScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.OVERLAY -> OverlayBehaviorScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.GEMINI -> GeminiSettingsScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.AI_HUB }
                )

                SettingsPage.APPEARANCE -> AppearanceScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.PERMISSIONS -> PermissionsScreen(
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.PROTECTED -> ProtectedAppsScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.BACKUP -> BackupScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
                )

                SettingsPage.ABOUT -> AboutScreen(
                    topic = topic,
                    style = style,
                    scopes = scopes,
                    onOpenTopic = { topic = it },
                    onBack = {
                        if (topic == AboutTopic.NONE) {
                            page = SettingsPage.LIST
                        } else {
                            topic = AboutTopic.NONE
                        }
                    }
                )

                SettingsPage.LIST -> SettingsList(
                    viewModel = viewModel,
                    scopes = scopes,
                    onOpenAi = { page = SettingsPage.AI_HUB },
                    onOpenOverlay = { page = SettingsPage.OVERLAY },
                    onOpenUpdate = { page = SettingsPage.UPDATE },
                    onOpenAppearance = { page = SettingsPage.APPEARANCE },
                    onOpenPermissions = { page = SettingsPage.PERMISSIONS },
                    onOpenBackup = { page = SettingsPage.BACKUP },
                    onOpenProtected = { page = SettingsPage.PROTECTED },
                    onOpenAbout = {
                        topic = AboutTopic.NONE
                        page = SettingsPage.ABOUT
                    }
                )
            }
        }
    }
}

@Composable
private fun SettingsList(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onOpenAi: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenUpdate: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenProtected: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 16.dp)
        )

        // --- Menu da IA --------------------------------------------------
        SectionHeader(title = stringResource(R.string.settings_ai_section), icon = Icons.Rounded.Bolt)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-ai-hub")) {
            SettingRow(
                title = stringResource(R.string.settings_ai_hub_row),
                subtitle = if (settings.aiProvider == AiProvider.OFFLINE) {
                    stringResource(
                        R.string.settings_ai_hub_row_sub_offline,
                        settings.offlineModel.substringBeforeLast('.')
                    )
                } else {
                    stringResource(R.string.settings_ai_hub_row_sub_gemini)
                },
                icon = Icons.Rounded.Bolt,
                onClick = onOpenAi
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --- Idiomas -----------------------------------------------------
        SectionHeader(title = stringResource(R.string.settings_ai_langs), icon = Icons.Rounded.Language)
        AppCard {
            Column {
                DropdownRow(
                    title = stringResource(R.string.settings_response_lang),
                    subtitle = stringResource(R.string.settings_response_lang_sub),
                    icon = Icons.Rounded.Language,
                    selectedLabel = stringResource(Languages.labelResFor(settings.language)),
                    options = Languages.ALL.map { it.code to stringResource(it.labelRes) },
                    onSelect = viewModel::setLanguage
                )
                Divider()
                DropdownRow(
                    title = stringResource(R.string.settings_translate_to),
                    subtitle = stringResource(R.string.settings_translate_to_sub),
                    icon = Icons.Rounded.Translate,
                    selectedLabel = stringResource(Languages.labelResFor(settings.translateTarget)),
                    options = Languages.ALL.map { it.code to stringResource(it.labelRes) },
                    onSelect = viewModel::setTranslateTarget
                )
            }
        }

        // --- Comportamento do overlay ------------------------------------
        SectionHeader(title = stringResource(R.string.settings_overlay_section), icon = Icons.Rounded.Tune)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-overlay")) {
            SettingRow(
                title = stringResource(R.string.settings_overlay_row),
                subtitle = stringResource(R.string.settings_overlay_row_sub),
                icon = Icons.Rounded.Keyboard,
                onClick = onOpenOverlay
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --- Aparencia ---------------------------------------------------
        SectionHeader(title = stringResource(R.string.settings_appearance), icon = Icons.Rounded.Palette)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-appearance")) {
            SettingRow(
                title = stringResource(R.string.settings_appearance_row),
                subtitle = stringResource(
                    R.string.settings_appearance_row_sub,
                    stringResource(themeLabelRes(settings.themeMode)),
                    stringResource(settings.barHeight.labelRes).lowercase(),
                    appLanguageLabel(settings.appLanguage)
                ),
                icon = Icons.Rounded.Palette,
                onClick = onOpenAppearance
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --- Permissoes --------------------------------------------------
        SectionHeader(title = stringResource(R.string.settings_permissions_section), icon = Icons.Rounded.Shield)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-permissions")) {
            SettingRow(
                title = stringResource(R.string.settings_permissions_row),
                subtitle = stringResource(R.string.settings_permissions_row_sub),
                icon = Icons.Rounded.Shield,
                onClick = onOpenPermissions
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --- Backup --------------------------------------------------------
        SectionHeader(title = stringResource(R.string.settings_privacy_section), icon = Icons.Rounded.Shield)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-protected")) {
            SettingRow(
                title = stringResource(R.string.settings_protected_row),
                subtitle = stringResource(R.string.settings_protected_row_sub),
                icon = Icons.Rounded.Shield,
                onClick = onOpenProtected
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        SectionHeader(title = stringResource(R.string.settings_backup_section), icon = Icons.Rounded.Download)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-backup")) {
            SettingRow(
                title = stringResource(R.string.settings_backup_row),
                subtitle = stringResource(R.string.settings_backup_row_sub),
                icon = Icons.Rounded.Download,
                onClick = onOpenBackup
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        // --- Sobre -------------------------------------------------------
        // --- Atualizacoes ----------------------------------------------------
        val updateState by viewModel.update.collectAsStateWithLifecycle()
        SectionHeader(title = stringResource(R.string.settings_update_section), icon = Icons.Rounded.NewReleases)
        UpdateHighlightRow(
            subtitle = updateState.available?.let {
                stringResource(R.string.settings_update_row_sub_new, it.version)
            } ?: stringResource(R.string.settings_update_row_sub_current, viewModel.installedVersion()),
            hasUpdate = updateState.available != null,
            onClick = onOpenUpdate,
            modifier = Modifier.sharedPage(scopes, "page-update")
        )

        SectionHeader(title = stringResource(R.string.settings_about_section), icon = Icons.Rounded.Info)
        AppCard(modifier = Modifier.sharedPage(scopes, "page-about")) {
            SettingRow(
                title = stringResource(R.string.settings_about_row),
                subtitle = stringResource(R.string.settings_about_row_sub),
                icon = Icons.Rounded.Info,
                onClick = onOpenAbout
            ) {
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(Modifier.height(24.dp))
        CreditFooter(context = LocalContext.current)
        Spacer(Modifier.height(28.dp))
    }
}

/** Submenu com tudo da Gemini: link da chave, chave, modelo e temperatura. */
@Composable
private fun GeminiSettingsScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val connection by viewModel.connection.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()

    var apiKeyInput by remember { mutableStateOf("") }
    var keyVisible by remember { mutableStateOf(false) }

    // A lista vem da API: modelos em cache para esta chave ou o resultado da
    // ultima busca. Nada de nomes fixos no app.
    val cacheValid = settings.cachedModelsKey == viewModel.currentKeyFingerprint()
    val modelOptions = when {
        models.available.isNotEmpty() -> models.available
        cacheValid -> settings.cachedModels
        else -> emptyList()
    }.map { it to GeminiModels.prettyLabel(it) }

    LaunchedEffect(settings.cachedModelsKey) {
        if (cacheValid) viewModel.primeCachedModels()
    }

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-gemini")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.common_back),
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.settings_gemini_key),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // --- Link direto para criar a chave ------------------------------
        SectionHeader(title = stringResource(R.string.gemini_create_section), icon = Icons.Rounded.OpenInNew)
        AppCard {
            Column {
                SettingRow(
                    title = stringResource(R.string.gemini_open_studio),
                    subtitle = stringResource(R.string.gemini_open_studio_sub),
                    icon = Icons.Rounded.OpenInNew,
                    onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(AI_STUDIO_URL))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                ) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Divider()
                Text(
                    text = stringResource(R.string.gemini_studio_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        // --- Chave -------------------------------------------------------
        SectionHeader(title = stringResource(R.string.gemini_your_key), icon = Icons.Rounded.VpnKey)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(icon = Icons.Rounded.VpnKey)
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.gemini_key_field),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (settings.hasApiKey) {
                                stringResource(R.string.gemini_key_saved, viewModel.maskedApiKey())
                            } else {
                                stringResource(R.string.gemini_no_key)
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.hasApiKey) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text(stringResource(R.string.gemini_key_placeholder)) },
                    singleLine = true,
                    visualTransformation = if (keyVisible) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(PillShape)
                                .clickable { keyVisible = !keyVisible },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                if (keyVisible) {
                                    Icons.Rounded.VisibilityOff
                                } else {
                                    Icons.Rounded.Visibility
                                },
                                contentDescription = stringResource(R.string.gemini_toggle_visibility),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AuroraButton(
                        text = stringResource(R.string.gemini_save_key),
                        onClick = {
                            val typed = apiKeyInput
                            viewModel.saveApiKey(typed)
                            viewModel.loadAvailableModels(typed)
                            apiKeyInput = ""
                        },
                        enabled = apiKeyInput.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(10.dp))
                    PillChip(
                        text = stringResource(R.string.gemini_test),
                        icon = Icons.Rounded.Bolt,
                        onClick = {
                            viewModel.testConnection(apiKeyInput.takeIf { it.isNotBlank() })
                        }
                    )
                }
                if (connection.message.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    StatusLine(stage = connection.stage, message = connection.message)
                }
                if (settings.hasApiKey) {
                    Spacer(Modifier.height(10.dp))
                    PillChip(
                        text = stringResource(R.string.gemini_remove_key),
                        icon = Icons.Rounded.DeleteOutline,
                        onClick = viewModel::clearApiKey,
                        accent = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // --- Modelo ------------------------------------------------------
        SectionHeader(title = stringResource(R.string.gemini_model_section), icon = Icons.Rounded.Dashboard)
        AppCard {
            Column {
                if (modelOptions.isEmpty()) {
                    SettingRow(
                        title = stringResource(R.string.gemini_model_section),
                        subtitle = stringResource(R.string.gemini_model_empty_sub),
                        icon = Icons.Rounded.Dashboard,
                        onClick = { viewModel.loadAvailableModels(null) }
                    ) {
                        Text(
                            text = GeminiModels.prettyLabel(settings.model),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    DropdownRow(
                        title = stringResource(R.string.gemini_model_section),
                        subtitle = stringResource(R.string.gemini_model_sub),
                        icon = Icons.Rounded.Dashboard,
                        selectedLabel = GeminiModels.prettyLabel(settings.model),
                        options = modelOptions,
                        onSelect = viewModel::setModel
                    )
                }
                Divider()
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        PillChip(
                            text = stringResource(
                                if (models.loading) {
                                    R.string.common_searching
                                } else {
                                    R.string.gemini_fetch_models
                                }
                            ),
                            icon = Icons.Rounded.Refresh,
                            onClick = { viewModel.loadAvailableModels(null) }
                        )
                        if (models.loading) {
                            Spacer(Modifier.width(10.dp))
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = models.message.ifBlank {
                            stringResource(R.string.gemini_models_hint)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // --- Temperatura -------------------------------------------------
        SectionHeader(title = stringResource(R.string.gemini_creativity), icon = Icons.Rounded.Thermostat)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Rounded.Thermostat,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.gemini_temperature),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.gemini_temperature_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = String.format("%.1f", settings.temperature),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Slider(
                    value = settings.temperature,
                    onValueChange = viewModel::setTemperature,
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

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun StatusLine(stage: ConnectionStage, message: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        when (stage) {
            ConnectionStage.TESTING -> CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )

            ConnectionStage.OK -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(16.dp)
            )

            ConnectionStage.FAILED -> Icon(
                Icons.Rounded.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp)
            )

            ConnectionStage.IDLE -> Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = when (stage) {
                ConnectionStage.OK -> MaterialTheme.colorScheme.secondary
                ConnectionStage.FAILED -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f))
    )
}

@Composable
private fun DropdownRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    selectedLabel: String,
    options: List<Pair<String, String>>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        SettingRow(
            title = title,
            subtitle = subtitle,
            icon = icon,
            onClick = { expanded = true }
        ) {
            Row(
                modifier = Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                    .padding(start = 14.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedLabel,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        onSelect(value)
                        expanded = false
                    }
                )
            }
        }
    }
}

// --- Sobre ----------------------------------------------------------------

@Composable
private fun AboutScreen(
    topic: AboutTopic,
    style: AnimationStyle,
    scopes: SharedPageScopes?,
    onOpenTopic: (AboutTopic) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-about")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(topic.titleRes), onBack = onBack)

        // Animacao 2 do catalogo (Animated Visibility): o menu do Sobre e cada
        // topico dele entram deslizando de um lado e saem para o outro, com
        // esmaecimento junto - a mesma linguagem usada na troca de chips da
        // barra sobre o teclado.
        AnimatedContent(
            targetState = topic,
            transitionSpec = {
                val toDetail = targetState != AboutTopic.NONE && initialState == AboutTopic.NONE
                val toList = targetState == AboutTopic.NONE && initialState != AboutTopic.NONE
                val direction = if (toList) -1 else 1
                when (style) {
                    AnimationStyle.NONE -> EnterTransition.None togetherWith ExitTransition.None
                    AnimationStyle.SIMPLE ->
                        fadeIn(tween(ABOUT_TOPIC_ANIM_MS)) togetherWith
                            fadeOut(tween(ABOUT_TOPIC_ANIM_MS))

                    AnimationStyle.FULL -> {
                        val enter = slideInHorizontally(
                            animationSpec = tween(ABOUT_TOPIC_ANIM_MS, easing = LinearOutSlowInEasing)
                        ) { width -> direction * width / 5 } + fadeIn(tween(ABOUT_TOPIC_ANIM_MS))
                        val exit = slideOutHorizontally(
                            animationSpec = tween(ABOUT_TOPIC_ANIM_MS, easing = FastOutLinearInEasing)
                        ) { width -> -direction * width / 5 } + fadeOut(tween(ABOUT_TOPIC_ANIM_MS))
                        enter togetherWith exit
                    }
                }.let { if (toDetail || toList) it else EnterTransition.None togetherWith ExitTransition.None }
            },
            label = "about-topic"
        ) { current ->
        when (current) {
            AboutTopic.NONE -> Column {
                AppCard {
                    Column {
                        AboutTopic.entries
                            .filter { it != AboutTopic.NONE && it != AboutTopic.NEWS }
                            .forEachIndexed { index, item ->
                                if (index > 0) Divider()
                                SettingRow(
                                    title = stringResource(item.titleRes),
                                    subtitle = when (item) {
                                        AboutTopic.APP ->
                                            stringResource(R.string.about_topic_app_sub)

                                        AboutTopic.PERMISSION ->
                                            stringResource(R.string.about_topic_permission_sub)

                                        AboutTopic.DATA ->
                                            stringResource(R.string.about_topic_data_sub)

                                        AboutTopic.NEWS -> stringResource(
                                            R.string.about_topic_news_sub,
                                            Changelog.CURRENT
                                        )

                                        AboutTopic.BLOCKED ->
                                            stringResource(R.string.about_topic_blocked_sub)

                                        AboutTopic.NONE -> ""
                                    },
                                    icon = when (item) {
                                        AboutTopic.APP -> Icons.Rounded.Info
                                        AboutTopic.PERMISSION -> Icons.Rounded.Shield
                                        AboutTopic.DATA -> Icons.Rounded.Lock
                                        AboutTopic.NEWS -> Icons.Rounded.NewReleases
                                        AboutTopic.BLOCKED -> Icons.Rounded.Warning
                                        AboutTopic.NONE -> Icons.Rounded.Info
                                    },
                                    onClick = { onOpenTopic(item) }
                                ) {
                                    Icon(
                                        Icons.Rounded.ChevronRight,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                    }
                }
                Spacer(Modifier.height(16.dp))
                CreditFooter(context = context)
            }

            AboutTopic.APP -> AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutParagraph(
                        title = stringResource(R.string.about_app_1_title),
                        detail = stringResource(R.string.about_app_1_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_app_2_title),
                        detail = stringResource(R.string.about_app_2_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_app_3_title),
                        detail = stringResource(R.string.about_app_3_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_app_4_title),
                        detail = stringResource(R.string.about_app_4_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_app_5_title),
                        detail = stringResource(R.string.about_app_5_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_app_6_title),
                        detail = stringResource(R.string.about_app_6_body)
                    )
                }
            }

            AboutTopic.PERMISSION -> AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutParagraph(
                        title = stringResource(R.string.about_perm_1_title),
                        detail = stringResource(R.string.about_perm_1_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_perm_2_title),
                        detail = stringResource(R.string.about_perm_2_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_perm_3_title),
                        detail = stringResource(R.string.about_perm_3_body)
                    )
                    Text(
                        text = stringResource(R.string.about_perm_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }

            AboutTopic.DATA -> AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutParagraph(
                        title = stringResource(R.string.about_data_1_title),
                        detail = stringResource(R.string.about_data_1_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_2_title),
                        detail = stringResource(R.string.about_data_2_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_3_title),
                        detail = stringResource(R.string.about_data_3_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_4_title),
                        detail = stringResource(R.string.about_data_4_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_5_title),
                        detail = stringResource(R.string.about_data_5_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_6_title),
                        detail = stringResource(R.string.about_data_6_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_data_7_title),
                        detail = stringResource(R.string.about_data_7_body)
                    )
                }
            }

            AboutTopic.NEWS -> WhatsNewScreen(
                onContinue = onBack,
                continueLabel = stringResource(R.string.common_back),
                // A pagina Sobre ja rola: aqui a tela entra sem rolagem propria.
                scrollable = false
            )

            AboutTopic.BLOCKED -> AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    AboutParagraph(
                        title = stringResource(R.string.about_blocked_1_title),
                        detail = stringResource(R.string.about_blocked_1_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_blocked_2_title),
                        detail = stringResource(R.string.about_blocked_2_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_blocked_3_title),
                        detail = stringResource(R.string.about_blocked_3_body)
                    )
                    AboutParagraph(
                        title = stringResource(R.string.about_blocked_4_title),
                        detail = stringResource(R.string.about_blocked_4_body)
                    )
                    AuroraButton(
                        text = stringResource(R.string.about_open_app_info),
                        icon = Icons.Rounded.OpenInNew,
                        onClick = {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        }

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun AboutParagraph(title: String, detail: String) {
    Column(modifier = Modifier.padding(bottom = 14.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = detail,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Cabecalho com seta de voltar usado nas subpaginas dos Ajustes. */
@Composable
internal fun SubPageHeader(title: String, onBack: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.common_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

// --- Aparencia ------------------------------------------------------------

@androidx.annotation.StringRes
private fun themeLabelRes(mode: ThemeMode): Int = when (mode) {
    ThemeMode.DARK -> R.string.theme_dark_lower
    ThemeMode.LIGHT -> R.string.theme_light_lower
    ThemeMode.SYSTEM -> R.string.theme_system_lower
}

/**
 * Nome do idioma do app. Cada idioma aparece escrito nele mesmo; so a opcao
 * "padrao do sistema" e traduzida.
 */
@Composable
private fun appLanguageLabel(tag: String): String =
    if (tag == AppLanguages.DEFAULT) {
        stringResource(R.string.language_system)
    } else {
        AppLanguages.labelFor(tag)
    }

/** Aplica o idioma escolhido usando os per-app locales do Android 13+. */
private fun applyAppLanguage(context: android.content.Context, tag: String) {
    // Espelho lido pela Application, pela Activity e pelo servico.
    LocaleHelper.saveTag(context, tag)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val manager = context.getSystemService(LocaleManager::class.java)
        manager?.applicationLocales = if (tag == AppLanguages.DEFAULT) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
        return
    }

    // Abaixo do 13 o sistema nao recria nada: a Activity se refaz com o
    // contexto novo para a troca valer na hora.
    (context as? Activity)?.recreate()
}

@Composable
private fun AppearanceScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // O preview so aparece quando o usuario mexe na altura, com uma entrada e
    // saida animadas, e some sozinho depois de um tempo.
    var previewTrigger by remember { mutableStateOf(0) }
    var previewVisible by remember { mutableStateOf(false) }
    LaunchedEffect(previewTrigger) {
        if (previewTrigger == 0) return@LaunchedEffect
        previewVisible = true
        delay(BAR_HEIGHT_PREVIEW_MS)
        previewVisible = false
    }

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-appearance")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.settings_appearance), onBack = onBack)

        SectionHeader(title = stringResource(R.string.appearance_theme), icon = Icons.Rounded.Palette)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    ThemeMode.entries.forEach { mode ->
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) {
                            PillChip(
                                text = stringResource(
                                    when (mode) {
                                        ThemeMode.DARK -> R.string.theme_dark
                                        ThemeMode.LIGHT -> R.string.theme_light
                                        ThemeMode.SYSTEM -> R.string.theme_system
                                    }
                                ),
                                onClick = { viewModel.setThemeMode(mode) },
                                selected = settings.themeMode == mode,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = stringResource(R.string.appearance_bar_height), icon = Icons.Rounded.Height)
        AppCard {
            Column {
                // Mesma linha de lista usada no idioma: nome + marca de
                // selecionado, sem o "NNdp" solto no texto.
                BarHeight.entries.forEachIndexed { index, height ->
                    if (index > 0) Divider()
                    BarHeightRow(
                        height = height,
                        selected = settings.barHeight == height,
                        onSelect = {
                            viewModel.setBarHeight(height)
                            previewTrigger++
                        }
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = previewVisible,
            enter = when (settings.animationStyle) {
                AnimationStyle.NONE -> EnterTransition.None
                AnimationStyle.SIMPLE -> fadeIn(tween(BAR_HEIGHT_PREVIEW_ANIM_MS))
                else -> fadeIn(tween(BAR_HEIGHT_PREVIEW_ANIM_MS)) +
                    expandVertically(
                        animationSpec = tween(BAR_HEIGHT_PREVIEW_ANIM_MS, easing = LinearOutSlowInEasing)
                    )
            },
            exit = when (settings.animationStyle) {
                AnimationStyle.NONE -> ExitTransition.None
                AnimationStyle.SIMPLE -> fadeOut(tween(BAR_HEIGHT_PREVIEW_ANIM_MS))
                else -> fadeOut(tween(BAR_HEIGHT_PREVIEW_ANIM_MS)) +
                    shrinkVertically(
                        animationSpec = tween(BAR_HEIGHT_PREVIEW_ANIM_MS, easing = FastOutLinearInEasing)
                    )
            }
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.appearance_bar_height_preview_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                BarHeightPreview(height = settings.barHeight)
            }
        }

        SectionHeader(title = stringResource(R.string.appearance_animation), icon = Icons.Rounded.Animation)
        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    AnimationStyle.entries.forEach { style ->
                        Box(modifier = Modifier.weight(1f).padding(horizontal = 3.dp)) {
                            PillChip(
                                text = stringResource(style.labelRes),
                                onClick = { viewModel.setAnimationStyle(style) },
                                selected = settings.animationStyle == style,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = stringResource(R.string.appearance_animation_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SectionHeader(title = stringResource(R.string.appearance_language), icon = Icons.Rounded.Translate)
        AppCard {
            Column {
                AppLanguages.ALL.forEachIndexed { index, language ->
                    if (index > 0) Divider()
                    LanguageRow(
                        language = language,
                        selected = settings.appLanguage == language.tag,
                        onSelect = {
                            viewModel.setAppLanguage(language.tag)
                            applyAppLanguage(context, language.tag)
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.appearance_language_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun LanguageRow(
    language: AppLanguage,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = language.flag, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = appLanguageLabel(language.tag),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/** Linha de opcao de altura da barra: so o nome, sem "NNdp" no texto. */
@Composable
private fun BarHeightRow(
    height: BarHeight,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Rounded.Height,
            contentDescription = null,
            tint = if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(height.labelRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = stringResource(R.string.common_selected),
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

/**
 * Mostra a barra flutuante no tamanho de verdade da opcao escolhida, para o
 * usuario ver como fica antes de sair da tela de Ajustes.
 */
@Composable
private fun BarHeightPreview(height: BarHeight) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = height.heightDp.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.22f))
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = stringResource(R.string.overlay_ai),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// --- Atualizacoes -----------------------------------------------------

/**
 * Linha de "Atualizacoes" com o mesmo destaque do modelo offline em uso: degrade,
 * borda colorida e icone com o degrade do app. Com versao nova, ganha o selo NOVA.
 */
@Composable
private fun UpdateHighlightRow(
    subtitle: String,
    hasUpdate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(24.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.linearGradient(listOf(primary.copy(alpha = 0.34f), secondary.copy(alpha = 0.18f))))
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(primary.copy(alpha = 0.8f), secondary.copy(alpha = 0.6f))),
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(PillShape)
                .background(AuroraBrush),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.Download,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_update_row),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = if (hasUpdate) secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (hasUpdate) FontWeight.SemiBold else FontWeight.Normal
            )
        }
        Spacer(Modifier.width(10.dp))
        if (hasUpdate) {
            Box(
                modifier = Modifier
                    .clip(PillShape)
                    .background(AuroraBrush)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.update_new_pill),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

/** Cartao do repositorio no GitHub, no fim da tela de atualizacao. */
@Composable
private fun GithubCard() {
    val context = LocalContext.current
    val primary = MaterialTheme.colorScheme.primary
    val secondary = MaterialTheme.colorScheme.secondary
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(primary.copy(alpha = 0.55f), secondary.copy(alpha = 0.35f))),
                shape = shape
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(icon = Icons.Rounded.Code)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.github_card_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = stringResource(R.string.github_card_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            text = UpdateChecker.REPO_URL.removePrefix("https://"),
            style = MaterialTheme.typography.bodyMedium,
            color = secondary,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(PillShape)
                .clickable { openGithubUrl(context, UpdateChecker.REPO_URL) }
                .background(secondary.copy(alpha = 0.12f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(14.dp))
        AuroraButton(
            text = stringResource(R.string.github_open_repo),
            icon = Icons.Rounded.OpenInNew,
            onClick = { openGithubUrl(context, UpdateChecker.REPO_URL) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            PillChip(
                text = stringResource(R.string.github_open_releases),
                icon = Icons.Rounded.NewReleases,
                onClick = { openGithubUrl(context, UpdateChecker.RELEASES_URL) },
                modifier = Modifier.weight(1f),
                height = 48.dp
            )
            Spacer(Modifier.width(8.dp))
            PillChip(
                text = stringResource(R.string.github_share),
                icon = Icons.Rounded.Share,
                onClick = {
                    val send = Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(
                            Intent.EXTRA_TEXT,
                            context.getString(R.string.github_share_text, UpdateChecker.REPO_URL)
                        )
                    runCatching {
                        context.startActivity(
                            Intent.createChooser(send, context.getString(R.string.github_share_title))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                },
                modifier = Modifier.weight(1f),
                height = 48.dp
            )
        }
    }
}

/**
 * Painel de "aguarde" enquanto o Android instala. O app fecha e abre sozinho ao terminar.
 * Se passar de um minuto sem acontecer nada, oferece limpar o estado e tentar de novo.
 */
@Composable
private fun InstallWaitingPanel(onRetry: () -> Unit) {
    var stuck by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(INSTALL_STUCK_AFTER_MS)
        stuck = true
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(40.dp),
            color = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.update_install_waiting_title),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.update_install_waiting_sub),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (stuck) {
            Spacer(Modifier.height(12.dp))
            PillChip(text = stringResource(R.string.update_install_retry), onClick = onRetry)
        }
    }
}

/** Tela do Android onde se permite que o TecladoIA instale apps (so na primeira vez). */
private fun openInstallPermissionSettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

/** Abre um endereco do GitHub no navegador (alternativa a instalar dentro do app). */
private fun openGithubUrl(context: android.content.Context, url: String) {
    if (!UpdateChecker.isTrusted(url)) return
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

@Composable
private fun UpdateScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val state by viewModel.update.collectAsStateWithLifecycle()
    val installed = viewModel.installedVersion()
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* sem a permissao o aviso simplesmente nao aparece */ }
    val install by UpdateInstaller.state.collectAsStateWithLifecycle()
    var showInstallHint by remember { mutableStateOf(false) }

    // Mensagem de cada falha da instalacao, ja traduzida.
    @Composable
    fun installErrorText(failed: InstallState.Failed): String = when (failed.kind) {
        InstallFailure.NETWORK -> stringResource(R.string.update_install_error_network)
        InstallFailure.STORAGE -> stringResource(R.string.update_install_error_storage)
        InstallFailure.SIGNATURE -> stringResource(R.string.update_install_error_signature)
        InstallFailure.BLOCKED -> stringResource(R.string.update_install_error_blocked)
        InstallFailure.NO_APK -> stringResource(R.string.update_error_no_release)
        InstallFailure.OTHER -> stringResource(R.string.update_install_error_other, failed.detail.ifBlank { "-" })
    }

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-update")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.update_title), onBack = onBack)
        Spacer(Modifier.height(8.dp))

        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.update_installed),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = installed.ifBlank { "-" },
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(14.dp))
                AuroraButton(
                    text = stringResource(
                        if (state.checking) R.string.update_checking else R.string.update_check_button
                    ),
                    icon = Icons.Rounded.Refresh,
                    enabled = !state.checking,
                    onClick = { viewModel.checkForUpdate(manual = true) },
                    modifier = Modifier.fillMaxWidth()
                )
                if (state.upToDate) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.update_up_to_date),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                state.error?.let { error ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(
                            when (error) {
                                UpdateError.NETWORK, UpdateError.INVALID -> R.string.update_error_network
                                UpdateError.RATE_LIMIT -> R.string.update_error_limit
                                UpdateError.NO_RELEASE -> R.string.update_error_no_release
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        state.available?.let { info ->
            Spacer(Modifier.height(16.dp))
            AppCard {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.update_available, info.version),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (info.notes.isNotBlank()) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.update_notes_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            // Tira as marcas do Markdown dos releases; o texto fica limpo.
                            text = info.notes.replace(Regex("[#*`>]"), "").trim().take(900),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    when (val progress = install) {
                        is InstallState.Downloading -> {
                            val percent = if (progress.total > 0) {
                                ((progress.downloaded * 100) / progress.total).toInt().coerceIn(0, 100)
                            } else {
                                0
                            }
                            if (progress.total > 0) {
                                LinearProgressIndicator(
                                    progress = { percent / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.update_install_downloading, percent),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(1f)
                                )
                                PillChip(
                                    text = stringResource(R.string.update_install_cancel),
                                    onClick = viewModel::cancelUpdateInstall
                                )
                            }
                        }

                        is InstallState.AwaitingConfirmation -> {
                            if (progress.shown) {
                                // Instalando: so aguardar. Nada de botao para instalar de novo.
                                InstallWaitingPanel(onRetry = UpdateInstaller::reset)
                            } else {
                                Text(
                                    text = stringResource(R.string.update_install_awaiting),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.height(10.dp))
                                AuroraButton(
                                    text = stringResource(R.string.update_install_continue),
                                    icon = Icons.Rounded.Download,
                                    onClick = { UpdateInstaller.resumeConfirmation(context) },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        else -> {
                            AuroraButton(
                                text = stringResource(R.string.update_install_button),
                                icon = Icons.Rounded.Download,
                                onClick = {
                                    when {
                                        // Sem APK anexado, so a pagina do release pode ser aberta.
                                        !info.canInstallInApp -> openGithubUrl(context, info.pageUrl)
                                        !UpdateInstaller.canInstall(context) -> {
                                            showInstallHint = true
                                            openInstallPermissionSettings(context)
                                        }

                                        else -> {
                                            showInstallHint = false
                                            viewModel.installUpdate(info)
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (progress is InstallState.Failed) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = installErrorText(progress),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            if (showInstallHint) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = stringResource(R.string.update_install_permission_hint),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                    if (install !is InstallState.Downloading && install !is InstallState.AwaitingConfirmation) {
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            PillChip(
                                text = stringResource(R.string.update_install_browser),
                                icon = Icons.Rounded.OpenInNew,
                                onClick = { openGithubUrl(context, info.downloadUrl) },
                                modifier = Modifier.weight(1f),
                                height = 48.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            PillChip(
                                text = stringResource(R.string.update_open_page),
                                icon = Icons.Rounded.NewReleases,
                                onClick = { openGithubUrl(context, info.pageUrl) },
                                modifier = Modifier.weight(1f),
                                height = 48.dp
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = stringResource(R.string.update_install_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        AppCard {
            Column {
                ToggleRow(
                    title = stringResource(R.string.update_auto_title),
                    subtitle = stringResource(R.string.update_auto_sub),
                    icon = Icons.Rounded.Refresh,
                    checked = settings.autoUpdateCheck,
                    onCheckedChange = viewModel::setAutoUpdateCheck
                )
                Divider()
                ToggleRow(
                    title = stringResource(R.string.update_notify_title),
                    subtitle = stringResource(R.string.update_notify_sub),
                    icon = Icons.Rounded.Notifications,
                    checked = settings.updateNotify,
                    onCheckedChange = { on ->
                        viewModel.setUpdateNotify(on)
                        // Android 13+: ligar o aviso pede a permissao de notificacoes.
                        val missing = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                            PackageManager.PERMISSION_GRANTED
                        if (on && missing) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                )
            }
        }

        Spacer(Modifier.height(16.dp))
        GithubCard()
        Spacer(Modifier.height(28.dp))
    }
}

// --- Comportamento do overlay -----------------------------------------

/** As tres chaves que antes ficavam soltas na lista de Ajustes. */
@Composable
private fun OverlayBehaviorScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-overlay")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.settings_overlay_section), onBack = onBack)
        Spacer(Modifier.height(8.dp))
        AppCard {
            Column {
                ToggleRow(
                    title = stringResource(R.string.settings_auto_bar),
                    subtitle = stringResource(R.string.settings_auto_bar_sub),
                    icon = Icons.Rounded.Keyboard,
                    checked = settings.autoBar,
                    onCheckedChange = viewModel::setAutoBar
                )
                Divider()
                ToggleRow(
                    title = stringResource(R.string.settings_hide_with_keyboard),
                    subtitle = stringResource(R.string.settings_hide_with_keyboard_sub),
                    icon = Icons.Rounded.VisibilityOff,
                    checked = settings.hideWithKeyboard,
                    onCheckedChange = viewModel::setHideWithKeyboard
                )
                Divider()
                ToggleRow(
                    title = stringResource(R.string.settings_haptics),
                    subtitle = stringResource(R.string.settings_haptics_sub),
                    icon = Icons.Rounded.Vibration,
                    checked = settings.haptics,
                    onCheckedChange = viewModel::setHaptics
                )
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

// --- Backup -----------------------------------------------------------

@Composable
private fun BackupScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val prompts by viewModel.prompts.collectAsStateWithLifecycle()
    val backup by viewModel.backup.collectAsStateWithLifecycle()
    var pendingImportUri by rememberSaveable { mutableStateOf<String?>(null) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> if (uri != null) pendingImportUri = uri.toString() }

    // So entra em jogo em Android 9 ou anterior: no 10+ o MediaStore ja
    // escreve em Downloads sem pedir nada.
    val legacyPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) viewModel.exportPromptsBackup() }

    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-backup")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.backup_title), onBack = onBack)

        backup.message?.let { message ->
            Spacer(Modifier.height(4.dp))
            ErrorBanner(
                message = message,
                onDismiss = viewModel::clearBackupMessage,
                actionLabel = stringResource(R.string.common_ok)
            )
        }

        Spacer(Modifier.height(12.dp))

        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.backup_export_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.backup_export_sub, prompts.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AuroraButton(
                    text = stringResource(R.string.backup_export_button),
                    icon = Icons.Rounded.Download,
                    enabled = !backup.busy && prompts.isNotEmpty(),
                    onClick = {
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                            legacyPermission.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.exportPromptsBackup()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        AppCard {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = stringResource(R.string.backup_import_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.backup_import_sub),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(14.dp))
                AuroraButton(
                    text = stringResource(R.string.backup_import_button),
                    icon = Icons.Rounded.Upload,
                    enabled = !backup.busy,
                    onClick = { importLauncher.launch(arrayOf("application/json")) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(Modifier.height(28.dp))
    }

    // Restaurar substitui tudo: confirma antes, ja que nao tem volta.
    pendingImportUri?.let { uriText ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.backup_import_confirm_title)) },
            text = { Text(stringResource(R.string.backup_import_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.importPromptsBackup(Uri.parse(uriText))
                    pendingImportUri = null
                }) {
                    Text(stringResource(R.string.backup_import_confirm_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) {
                    Text(stringResource(R.string.common_cancel))
                }
            }
        )
    }
}

// --- Permissoes -----------------------------------------------------------

@Composable
private fun PermissionsScreen(scopes: SharedPageScopes?, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .sharedPage(scopes, "page-permissions")
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        SubPageHeader(title = stringResource(R.string.settings_permissions_section), onBack = onBack)
        Text(
            text = stringResource(R.string.perm_intro),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 12.dp, start = 4.dp)
        )
        PermissionsCard()
        Spacer(Modifier.height(16.dp))
        OemHelpCard()
        Spacer(Modifier.height(16.dp))
        BankHelpCard()
        Spacer(Modifier.height(28.dp))
    }
}

/** Ajuda para aparelhos que desligam o servico sozinhos (Xiaomi, Oppo e familia). */
@Composable
private fun OemHelpCard() {
    val context = LocalContext.current
    AppCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.Bolt)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.oem_help_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.oem_help_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            if (OemHelp.hasAutostartScreen()) {
                AuroraButton(
                    text = stringResource(R.string.oem_open_autostart),
                    icon = Icons.Rounded.OpenInNew,
                    onClick = { OemHelp.openAutostart(context) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
            AuroraButton(
                text = stringResource(R.string.oem_open_battery),
                icon = Icons.Rounded.OpenInNew,
                onClick = { OemHelp.openBattery(context) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Alguns bancos recusam abrir com qualquer servico de acessibilidade de fora
 * da Play Store ligado. O app nao tem como esconder isso do banco, entao a
 * saida honesta e permitir desligar o servico na hora e religar depois.
 */
@Composable
private fun BankHelpCard() {
    val context = LocalContext.current
    AppCard {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconBadge(icon = Icons.Rounded.Shield)
                Spacer(Modifier.width(12.dp))
                Text(
                    text = stringResource(R.string.protected_bank_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.protected_bank_body),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            AuroraButton(
                text = stringResource(R.string.protected_disable_now),
                icon = Icons.Rounded.VisibilityOff,
                onClick = {
                    val service = KeyboardOverlayService.instance
                    if (service != null) {
                        runCatching { service.disableSelf() }
                    } else {
                        openAccessibilitySettings(context)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            AuroraButton(
                text = stringResource(R.string.protected_reenable),
                icon = Icons.Rounded.OpenInNew,
                onClick = { openAccessibilitySettings(context) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private fun openAccessibilitySettings(context: android.content.Context) {
    runCatching {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

// --- Apps ignorados ---------------------------------------------------

private data class LaunchableApp(
    val packageName: String,
    val label: String,
    /** Veio com o aparelho e nao foi atualizado pelo usuario (telefone, ajustes...). */
    val isSystem: Boolean
)

/**
 * Todos os apps instalados, inclusive os do sistema e os sem icone na tela
 * inicial. Precisa da permissao QUERY_ALL_PACKAGES; sem ela o Android esconde
 * os apps que o TecladoIA nao tem motivo declarado para enxergar.
 */
@Suppress("DEPRECATION")
private fun loadInstalledApps(context: android.content.Context): List<LaunchableApp> {
    val pm = context.packageManager
    return pm.getInstalledApplications(0)
        .asSequence()
        .filter { it.packageName != context.packageName && it.enabled }
        .map { info ->
            val flags: Int = info.flags
            // Apps de fabrica que a pessoa atualizou (YouTube, Chrome...) contam como dela.
            val system = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            LaunchableApp(
                packageName = info.packageName,
                label = runCatching { pm.getApplicationLabel(info).toString() }
                    .getOrDefault(info.packageName),
                isSystem = system
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
        .toList()
}

/** Apps com icone na tela inicial, sem precisar da permissao de listar todos os apps. */
private fun loadLaunchableApps(context: android.content.Context): List<LaunchableApp> {
    val pm = context.packageManager
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    return pm.queryIntentActivities(intent, 0)
        .map {
            val flags: Int = it.activityInfo.applicationInfo.flags
            // Apps de fabrica que a pessoa atualizou (YouTube, Chrome...) contam como dela.
            val system = (flags and ApplicationInfo.FLAG_SYSTEM) != 0 &&
                (flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0
            LaunchableApp(it.activityInfo.packageName, it.loadLabel(pm).toString(), system)
        }
        .filter { it.packageName != context.packageName }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun ProtectedAppsScreen(
    viewModel: MainViewModel,
    scopes: SharedPageScopes?,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showSystem by rememberSaveable { mutableStateOf(false) }
    val apps by produceState(initialValue = emptyList<LaunchableApp>(), context) {
        value = withContext(Dispatchers.IO) {
            // Se a lista completa falhar, cai nos apps com icone, que sempre aparecem.
            runCatching { loadInstalledApps(context) }
                .getOrElse { runCatching { loadLaunchableApps(context) }.getOrDefault(emptyList()) }
        }
    }
    val shown = remember(apps, query, showSystem, settings.ignoredApps) {
        apps.filter { app ->
            // Apps do sistema so entram na lista com a chave ligada, exceto os
            // que ja estao desligados: esses nunca somem da vista.
            (showSystem || !app.isSystem || app.packageName in settings.ignoredApps) &&
                (query.isBlank() ||
                    app.label.contains(query, ignoreCase = true) ||
                    app.packageName.contains(query, ignoreCase = true))
        }
    }

    LazyColumn(
        modifier = Modifier
            .sharedPage(scopes, "page-protected")
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item { SubPageHeader(title = stringResource(R.string.protected_title), onBack = onBack) }
        // Cada item da lista e uma coluna so: varios componentes soltos no
        // mesmo item ficariam um por cima do outro.
        item {
            Column {
                Spacer(Modifier.height(8.dp))
                AppCard {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        IconBadge(icon = Icons.Rounded.Info)
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.protected_notice),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                AppCard {
                    ToggleRow(
                        title = stringResource(R.string.protected_default_title),
                        subtitle = stringResource(R.string.protected_default_sub),
                        icon = Icons.Rounded.Shield,
                        checked = settings.protectFinancialApps,
                        onCheckedChange = viewModel::setProtectFinancialApps
                    )
                }
                Spacer(Modifier.height(12.dp))
                AppCard {
                    ToggleRow(
                        title = stringResource(R.string.protected_system_title),
                        subtitle = stringResource(R.string.protected_system_sub),
                        icon = Icons.Rounded.Dashboard,
                        checked = showSystem,
                        onCheckedChange = { showSystem = it }
                    )
                }
            }
        }
        item {
            Column {
                SectionHeader(
                    title = stringResource(R.string.protected_custom_title),
                    icon = Icons.Rounded.Shield
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.protected_search_hint)) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
            }
        }
        items(shown, key = { it.packageName }) { app ->
            val auto = settings.protectFinancialApps && SensitiveApps.isFinancial(app.packageName)
            ProtectedAppRow(
                app = app,
                checked = auto || app.packageName in settings.ignoredApps,
                locked = auto,
                onChange = { viewModel.setAppIgnored(app.packageName, it) }
            )
        }
        item { Spacer(Modifier.height(28.dp)) }
    }
}

@Composable
private fun ProtectedAppRow(
    app: LaunchableApp,
    checked: Boolean,
    locked: Boolean,
    onChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(96, 96)
                .asImageBitmap()
        }.getOrNull()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !locked) { onChange(!checked) }
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(36.dp))
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = app.label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = if (locked) null else onChange,
            enabled = !locked
        )
    }
}

/** Assinatura do app: versao e link do canal. */
@Composable
private fun CreditFooter(context: android.content.Context) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.settings_footer_version, Changelog.CURRENT),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse(YOUTUBE_URL))
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    )
                }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_credit),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.PlayCircle,
                contentDescription = stringResource(R.string.settings_credit_desc),
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
