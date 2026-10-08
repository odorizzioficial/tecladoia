package com.odorizzioficial.tecladoia.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.odorizzioficial.tecladoia.ui.MainActivity

/**
 * O Android avisa o app logo depois que uma versao nova e instalada por cima dele
 * (ACTION_MY_PACKAGE_REPLACED), ja na versao nova. Trocar o APK encerra o app, e o
 * sistema nao o reabre. Aqui o app tenta voltar sozinho; como o Android bloqueia
 * abrir telas em segundo plano, uma notificacao "atualizado, toque para abrir"
 * garante o caminho de volta.
 */
class AppUpdatedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext

        // O aviso de "nova versao disponivel" deixou de valer.
        UpdateNotifier.cancelAvailable(app)

        // Reabre direto quando o Android permite; em geral ele bloqueia, sem erro.
        runCatching {
            app.startActivity(
                Intent(app, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.onFailure { Log.w(TAG, "Nao foi possivel reabrir o app agora", it) }

        val version = runCatching {
            app.packageManager.getPackageInfo(app.packageName, 0).versionName
        }.getOrNull().orEmpty()
        UpdateNotifier.showUpdated(app, version)
    }

    private companion object {
        const val TAG = "AppUpdatedReceiver"
    }
}
