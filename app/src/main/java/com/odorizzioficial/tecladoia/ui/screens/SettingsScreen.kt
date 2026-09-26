package com.odorizzioficial.tecladoia.ui.screens

import android.Manifest
import android.app.Activity
import android.app.LocaleManager
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
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.odorizzioficial.tecladoia.ui.components.PermissionsCard
import com.odorizzioficial.tecladoia.ui.components.PillChip
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.components.SettingRow
import com.odorizzioficial.tecladoia.ui.components.ToggleRow
import com.odorizzioficial.tecladoia.ui.theme.PillShape
import kotlinx.coroutines.delay

/** Link oficial para criar a chave da API Gemini. */
private const val AI_STUDIO_URL = "https://aistudio.google.com/apikey"

/** Canal do autor do app. */
private const val YOUTUBE_URL = "https://www.youtube.com/@odorizzioficial"

/** Paginas internas da aba Ajustes. */
private enum class SettingsPage { LIST, GEMINI, APPEARANCE, PERMISSIONS, BACKUP, ABOUT }

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
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val style = settings.animationStyle

    LaunchedEffect(apiKeyRequest) {
        if (apiKeyRequest) {
            page = SettingsPage.GEMINI
            viewModel.consumeApiKeyRequest()
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
                SettingsPage.GEMINI -> GeminiSettingsScreen(
                    viewModel = viewModel,
                    scopes = scopes,
                    onBack = { page = SettingsPage.LIST }
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
                    onOpenGemini = { page = SettingsPage.GEMINI },
                    onOpenAppearance = { page = SettingsPage.APPEARANCE },
                    onOpenPermissions = { page = SettingsPage.PERMISSIONS },
                    onOpenBackup = { page = SettingsPage.BACKUP },
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
    onOpenGemini: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPermissions: () -> Unit,
    onOpenBackup: () -> Unit,
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
        AppCard(modifier = Modifier.sharedPage(scopes, "page-gemini")) {
            SettingRow(
                title = stringResource(R.string.settings_gemini_key),
                subtitle = if (settings.hasApiKey) {
                    stringResource(
                        R.string.settings_key_saved_with_model,
                        GeminiModels.prettyLabel(settings.model)
                    )
                } else {
                    stringResource(R.string.settings_key_sub)
                },
                icon = Icons.Rounded.VpnKey,
                onClick = onOpenGemini
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
private fun SubPageHeader(title: String, onBack: () -> Unit) {
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
        Spacer(Modifier.height(28.dp))
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
