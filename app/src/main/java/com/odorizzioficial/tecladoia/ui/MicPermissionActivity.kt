package com.odorizzioficial.tecladoia.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.odorizzioficial.tecladoia.service.KeyboardOverlayService

/**
 * Activity invisivel usada apenas para pedir RECORD_AUDIO no momento em que o
 * usuario toca no microfone da barra. Um Service nao pode abrir a caixa de
 * permissao por conta propria.
 */
class MicPermissionActivity : ComponentActivity() {

    private val requestPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        KeyboardOverlayService.instance?.onMicPermissionResult(granted)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val alreadyGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        if (alreadyGranted) {
            KeyboardOverlayService.instance?.onMicPermissionResult(true)
            finish()
        } else {
            requestPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
}
