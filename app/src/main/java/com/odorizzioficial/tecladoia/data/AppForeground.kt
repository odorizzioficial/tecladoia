package com.odorizzioficial.tecladoia.data

import android.app.Activity
import android.app.Application
import android.os.Bundle

/**
 * Diz se alguma tela do app esta na frente. A janela de confirmacao da instalacao so
 * abre de verdade com o app aberto: em segundo plano o Android a bloqueia sem avisar,
 * e a tela de atualizacao precisa saber disso para oferecer o botao "Continuar".
 * Tudo roda na thread principal.
 */
object AppForeground {

    @Volatile
    private var started = 0

    fun isForeground(): Boolean = started > 0

    val callbacks = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityStarted(activity: Activity) {
            started++
        }

        override fun onActivityStopped(activity: Activity) {
            if (started > 0) started--
        }

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
        override fun onActivityDestroyed(activity: Activity) = Unit
    }
}
