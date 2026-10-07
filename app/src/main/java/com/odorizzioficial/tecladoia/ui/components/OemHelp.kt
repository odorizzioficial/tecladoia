package com.odorizzioficial.tecladoia.ui.components

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Atalhos para as telas de "inicio automatico" e bateria dos fabricantes que
 * encerram apps em segundo plano (Xiaomi, Oppo e familia). Essas telas nao sao
 * API publica e mudam entre versoes: cada tentativa e isolada e, se nenhuma
 * abrir, cai na tela de informacoes do app.
 */
object OemHelp {

    private enum class Family { XIAOMI, OPPO, VIVO, HUAWEI, OTHER }

    private data class Target(val pkg: String, val cls: String)

    private fun family(): Family {
        val brand = "${Build.MANUFACTURER} ${Build.BRAND}".lowercase()
        return when {
            listOf("xiaomi", "redmi", "poco").any { brand.contains(it) } -> Family.XIAOMI
            listOf("oppo", "realme", "oneplus").any { brand.contains(it) } -> Family.OPPO
            listOf("vivo", "iqoo").any { brand.contains(it) } -> Family.VIVO
            listOf("huawei", "honor").any { brand.contains(it) } -> Family.HUAWEI
            else -> Family.OTHER
        }
    }

    /** O fabricante tem uma tela propria de inicio automatico? */
    fun hasAutostartScreen(): Boolean = family() != Family.OTHER

    fun openAutostart(context: Context) {
        val targets = when (family()) {
            Family.XIAOMI -> listOf(
                Target("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            )
            Family.OPPO -> listOf(
                Target("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                Target("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
                Target("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity")
            )
            Family.VIVO -> listOf(
                Target("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                Target("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity")
            )
            Family.HUAWEI -> listOf(
                Target("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                Target("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            )
            Family.OTHER -> emptyList()
        }
        openFirst(context, targets)
    }

    fun openBattery(context: Context) {
        val targets = when (family()) {
            Family.XIAOMI -> listOf(
                Target("com.miui.powerkeeper", "com.miui.powerkeeper.ui.HiddenAppsConfigActivity")
            )
            else -> emptyList()
        }
        if (openFirst(context, targets, fallback = false)) return
        val direct = Intent(
            Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (!tryStart(context, direct)) openAppDetails(context)
    }

    /** Tenta cada destino; se nenhum abrir, mostra as informacoes do app. */
    private fun openFirst(
        context: Context,
        targets: List<Target>,
        fallback: Boolean = true
    ): Boolean {
        for (target in targets) {
            val intent = Intent()
                .setComponent(ComponentName(target.pkg, target.cls))
                .putExtra("package_name", context.packageName)
                .putExtra("package_label", "TecladoIA")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (tryStart(context, intent)) return true
        }
        if (fallback) openAppDetails(context)
        return false
    }

    private fun openAppDetails(context: Context) {
        tryStart(
            context,
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.fromParts("package", context.packageName, null))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun tryStart(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (t: Throwable) {
        false
    }
}
