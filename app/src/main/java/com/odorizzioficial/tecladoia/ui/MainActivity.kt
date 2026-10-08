package com.odorizzioficial.tecladoia.ui

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Keyboard
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.odorizzioficial.tecladoia.AppGraph
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.data.LocaleHelper
import com.odorizzioficial.tecladoia.domain.AnimationStyle
import com.odorizzioficial.tecladoia.domain.Changelog
import com.odorizzioficial.tecladoia.ui.screens.AssistantScreen
import com.odorizzioficial.tecladoia.ui.screens.OnboardingScreen
import com.odorizzioficial.tecladoia.ui.screens.PromptsScreen
import com.odorizzioficial.tecladoia.ui.screens.SettingsScreen
import com.odorizzioficial.tecladoia.ui.screens.WhatsNewScreen
import com.odorizzioficial.tecladoia.ui.theme.AiKeyboardTheme
import com.odorizzioficial.tecladoia.data.UpdateNotifier

class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }


    private val viewModel: MainViewModel by viewModels { MainViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppGraph.init(this)
        // Abriu o app: o aviso "TecladoIA atualizado" ja cumpriu o papel.
        UpdateNotifier.cancelUpdated(this)
        enableEdgeToEdge()
        // Atalho da barra sobre o teclado: segurar o chip de IA abre direto na
        // aba Funções, em vez de cair sempre no Assistente.
        val initialTab = when (intent?.getStringExtra(EXTRA_OPEN_TAB)) {
            TAB_PROMPTS -> Tab.PROMPTS
            TAB_SETTINGS -> Tab.SETTINGS
            else -> Tab.ASSISTANT
        }
        if (intent?.getBooleanExtra(EXTRA_OPEN_UPDATE, false) == true) {
            viewModel.requestUpdateScreen()
        }
        setContent {
            val settings by viewModel.settings.collectAsStateWithLifecycle()
            // Fluxo da primeira abertura: permissoes, novidades e depois o app.
            var welcomeStep by rememberSaveable { mutableStateOf<String?>(null) }
            val step = welcomeStep ?: when {
                !settings.onboardingDone -> STEP_PERMISSIONS
                settings.lastSeenVersion != Changelog.CURRENT -> STEP_NEWS
                else -> STEP_APP
            }

            AiKeyboardTheme(themeMode = settings.themeMode) {
                when (step) {
                    STEP_PERMISSIONS -> WelcomeContainer {
                        OnboardingScreen(
                            onContinue = {
                                viewModel.completeOnboarding()
                                welcomeStep = STEP_NEWS
                            }
                        )
                    }

                    STEP_NEWS -> WelcomeContainer {
                        WhatsNewScreen(
                            onContinue = {
                                viewModel.markVersionSeen(Changelog.CURRENT)
                                welcomeStep = STEP_APP
                            }
                        )
                    }

                    // AppShell traz o proprio Scaffold com a barra de abas.
                    else -> AppShell(viewModel = viewModel, initialTab = initialTab)
                }
            }
        }
    }

    companion object {
        /** Extra do intent que diz em qual aba o app deve abrir. */
        const val EXTRA_OPEN_TAB = "open_tab"

        /** A notificacao de versao nova abre direto a tela de atualizacao. */
        const val EXTRA_OPEN_UPDATE = "open_update"
        const val TAB_PROMPTS = "prompts"
        const val TAB_SETTINGS = "settings"
    }
}

/** Moldura das telas de boas-vindas: so insets, sem barra de abas. */
@Composable
private fun WelcomeContainer(content: @Composable () -> Unit) {
    Scaffold(containerColor = MaterialTheme.colorScheme.background) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            content()
        }
    }
}

private const val STEP_PERMISSIONS = "permissions"
private const val STEP_NEWS = "news"
private const val STEP_APP = "app"

private enum class Tab(@androidx.annotation.StringRes val labelRes: Int, val icon: ImageVector) {
    ASSISTANT(R.string.tab_assistant, Icons.Rounded.Keyboard),
    PROMPTS(R.string.tab_prompts, Icons.Rounded.ViewAgenda),
    SETTINGS(R.string.tab_settings, Icons.Rounded.Tune)
}

/** Duracao da troca de aba, no mesmo espirito da animacao 2 do catalogo skydoves. */
private const val TAB_ANIM_DURATION_MS = 220

@Composable
private fun AppShell(viewModel: MainViewModel, initialTab: Tab = Tab.ASSISTANT) {
    var current by rememberSaveable { mutableStateOf(initialTab) }
    val apiKeyRequest by viewModel.openApiKeyRequest.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    // O aviso "Configure sua Gemini API Key" abre a aba de ajustes, e a propria
    // tela de ajustes segue para a pagina da chave.
    LaunchedEffect(apiKeyRequest) {
        if (apiKeyRequest) current = Tab.SETTINGS
    }
    val offlineRequest by viewModel.openOfflineRequest.collectAsStateWithLifecycle()
    LaunchedEffect(offlineRequest) {
        if (offlineRequest) current = Tab.SETTINGS
    }
    val aiHubRequest by viewModel.openAiHubRequest.collectAsStateWithLifecycle()
    LaunchedEffect(aiHubRequest) {
        if (aiHubRequest) current = Tab.SETTINGS
    }
    val updateRequest by viewModel.openUpdateRequest.collectAsStateWithLifecycle()
    LaunchedEffect(updateRequest) {
        if (updateRequest) current = Tab.SETTINGS
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLow) {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab,
                        onClick = { current = tab },
                        icon = { Icon(tab.icon, contentDescription = stringResource(tab.labelRes)) },
                        label = { Text(stringResource(tab.labelRes)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSurface,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
        ) {
            // Troca de aba com deslize + esmaecimento (animacao 2 do catalogo
            // skydoves), na direcao de quem foi tocado na barra inferior.
            AnimatedContent(
                targetState = current,
                transitionSpec = {
                    val forward = targetState.ordinal > initialState.ordinal
                    when (settings.animationStyle) {
                        AnimationStyle.NONE ->
                            EnterTransition.None togetherWith ExitTransition.None

                        AnimationStyle.SIMPLE ->
                            fadeIn(tween(TAB_ANIM_DURATION_MS)) togetherWith
                                fadeOut(tween(TAB_ANIM_DURATION_MS))

                        AnimationStyle.FULL -> {
                            val enter = slideInHorizontally(
                                animationSpec = tween(TAB_ANIM_DURATION_MS, easing = LinearOutSlowInEasing)
                            ) { width -> if (forward) width / 6 else -width / 6 } +
                                fadeIn(tween(TAB_ANIM_DURATION_MS))
                            val exit = slideOutHorizontally(
                                animationSpec = tween(TAB_ANIM_DURATION_MS, easing = FastOutLinearInEasing)
                            ) { width -> if (forward) -width / 6 else width / 6 } +
                                fadeOut(tween(TAB_ANIM_DURATION_MS))
                            enter togetherWith exit
                        }
                    }
                },
                label = "main-tab"
            ) { tab ->
                when (tab) {
                    Tab.ASSISTANT -> AssistantScreen(viewModel = viewModel)
                    Tab.PROMPTS -> PromptsScreen(viewModel = viewModel)
                    Tab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                }
            }
        }
    }
}
