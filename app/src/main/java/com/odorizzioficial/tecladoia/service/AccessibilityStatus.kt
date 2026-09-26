package com.odorizzioficial.tecladoia.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.text.TextUtils

/** Consulta, sem privilegios especiais, se o servico esta habilitado nos Ajustes. */
object AccessibilityStatus {

    fun isEnabled(context: Context): Boolean {
        val expected = ComponentName(
            context.packageName,
            KeyboardOverlayService::class.java.name
        ).flattenToString()

        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        )
        if (TextUtils.isEmpty(enabledServices)) return false

        return enabledServices.split(':').any { entry ->
            entry.equals(expected, ignoreCase = true) ||
                entry.equals(
                    ComponentName(
                        context.packageName,
                        KeyboardOverlayService::class.java.name
                    ).flattenToShortString(),
                    ignoreCase = true
                )
        }
    }
}
