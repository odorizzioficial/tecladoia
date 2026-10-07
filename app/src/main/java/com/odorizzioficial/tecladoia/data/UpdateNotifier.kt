package com.odorizzioficial.tecladoia.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.odorizzioficial.tecladoia.R
import com.odorizzioficial.tecladoia.ui.MainActivity

/**
 * Avisa por notificacao quando sai uma versao nova. Cada versao avisa uma vez so
 * (a ultima avisada fica guardada), e sem a permissao de notificacoes nada e
 * mostrado: a tentativa se repete na proxima verificacao, depois que a pessoa
 * permitir. Tocar na notificacao abre a tela de atualizacao do app.
 */
object UpdateNotifier {

    private const val TAG = "UpdateNotifier"
    private const val CHANNEL_ID = "app_updates"
    private const val NOTIFICATION_ID = 4107

    /** Notificacoes liberadas (no Android 13+ isso inclui a permissao). */
    fun canNotify(context: Context): Boolean =
        runCatching { NotificationManagerCompat.from(context).areNotificationsEnabled() }
            .getOrDefault(false)

    suspend fun notifyOnce(context: Context, settings: SettingsRepository, info: UpdateInfo) {
        val saved = settings.snapshot()
        if (!saved.updateNotify || saved.notifiedUpdate == info.version) return
        if (!canNotify(context)) return
        if (show(context, info)) settings.setNotifiedUpdate(info.version)
    }

    private fun show(context: Context, info: UpdateInfo): Boolean = try {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                context.getString(R.string.update_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = context.getString(R.string.update_channel_desc) }
        )

        val open = Intent(context, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            .putExtra(MainActivity.EXTRA_OPEN_UPDATE, true)
        val pending = PendingIntent.getActivity(
            context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val title = context.getString(R.string.update_notif_title, info.version)
        val text = context.getString(R.string.update_notif_text)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_update)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        manager.notify(NOTIFICATION_ID, notification)
        true
    } catch (t: Throwable) {
        Log.w(TAG, "Falha ao mostrar a notificacao de atualizacao", t)
        false
    }
}
