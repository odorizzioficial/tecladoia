package com.odorizzioficial.tecladoia.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Recebe do Android o andamento da instalacao da atualizacao e repassa ao [UpdateInstaller]. */
class UpdateInstallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION) UpdateInstaller.onInstallResult(context, intent)
    }

    companion object {
        const val ACTION = "com.odorizzioficial.tecladoia.UPDATE_INSTALL_RESULT"
    }
}
