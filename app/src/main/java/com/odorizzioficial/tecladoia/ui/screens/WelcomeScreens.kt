package com.odorizzioficial.tecladoia.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.domain.Changelog
import com.odorizzioficial.tecladoia.domain.ReleaseNotes
import com.odorizzioficial.tecladoia.ui.components.AppCard
import com.odorizzioficial.tecladoia.ui.components.AuroraBrush
import com.odorizzioficial.tecladoia.ui.components.AuroraButton
import com.odorizzioficial.tecladoia.ui.components.PermissionsCard
import com.odorizzioficial.tecladoia.ui.components.SectionHeader
import com.odorizzioficial.tecladoia.ui.theme.PillShape

/**
 * Primeira abertura: o usuario libera microfone, bateria e acessibilidade antes
 * de entrar no app. Nenhuma permissao e pedida sozinha, cada uma sai de um
 * toque dele.
 */
@Composable
fun OnboardingScreen(onContinue: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(PillShape)
                .background(AuroraBrush),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                Icons.Rounded.AutoAwesome,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.welcome_title),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.welcome_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SectionHeader(title = stringResource(R.string.settings_permissions_section), icon = Icons.Rounded.Check)
        PermissionsCard()

        Spacer(Modifier.height(20.dp))
        AuroraButton(
            text = stringResource(R.string.welcome_continue),
            icon = Icons.Rounded.Check,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.welcome_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(28.dp))
    }
}

/**
 * Novidades da versao atual e das anteriores.
 *
 * [scrollable] existe porque esta tela tambem aparece dentro de Ajustes >
 * Sobre, que ja rola. Duas rolagens verticais aninhadas fazem o Compose medir
 * com altura infinita e derrubar o app - era esse o travamento ao abrir
 * Novidades por ali.
 */
@Composable
fun WhatsNewScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    continueLabel: String? = null,
    scrollable: Boolean = true
) {
    Column(
        modifier = modifier
            .then(
                if (scrollable) {
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                } else {
                    Modifier.fillMaxWidth()
                }
            )
            .padding(horizontal = if (scrollable) 20.dp else 0.dp)
    ) {
        Spacer(Modifier.height(28.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(PillShape)
                    .background(AuroraBrush),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.material3.Icon(
                    Icons.Rounded.NewReleases,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = stringResource(R.string.news_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.news_version, Changelog.CURRENT),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Changelog.ALL.forEach { release ->
            ReleaseCard(release)
        }

        Spacer(Modifier.height(18.dp))
        AuroraButton(
            text = continueLabel ?: stringResource(R.string.news_start),
            icon = Icons.Rounded.Check,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun ReleaseCard(release: ReleaseNotes) {
    SectionHeader(
        title = if (release.sinceVersion != null) {
            stringResource(R.string.news_release_since, release.sinceVersion)
        } else {
            stringResource(R.string.news_release, release.version, stringResource(release.dateRes))
        },
        icon = Icons.Rounded.NewReleases
    )
    AppCard {
        Column(modifier = Modifier.padding(16.dp)) {
            release.highlights.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row {
                    Text(
                        text = "\u2022",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = stringResource(item),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
