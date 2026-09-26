package com.odorizzioficial.tecladoia.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda a Gemini API Key cifrada com AES/GCM. A chave de cifragem vive no
 * Android Keystore (nao sai do dispositivo e nao e legivel pelo app); apenas o
 * texto cifrado fica em SharedPreferences privadas.
 */
class SecureKeyStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasApiKey(): Boolean = !prefs.getString(KEY_CIPHERTEXT, null).isNullOrBlank()

    fun saveApiKey(rawKey: String) {
        val trimmed = rawKey.trim()
        if (trimmed.isEmpty()) {
            clearApiKey()
            return
        }
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey())
            val iv = cipher.iv
            val encrypted = cipher.doFinal(trimmed.toByteArray(Charsets.UTF_8))
            val payload = ByteArray(iv.size + encrypted.size)
            System.arraycopy(iv, 0, payload, 0, iv.size)
            System.arraycopy(encrypted, 0, payload, iv.size, encrypted.size)
            prefs.edit()
                .putString(KEY_CIPHERTEXT, Base64.encodeToString(payload, Base64.NO_WRAP))
                .putInt(KEY_IV_LENGTH, iv.size)
                .apply()
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao cifrar a API key", t)
            clearApiKey()
        }
    }

    /** Retorna a chave em claro ou string vazia quando nao houver chave utilizavel. */
    fun readApiKey(): String {
        val stored = prefs.getString(KEY_CIPHERTEXT, null) ?: return ""
        val ivLength = prefs.getInt(KEY_IV_LENGTH, GCM_IV_LENGTH)
        return try {
            val payload = Base64.decode(stored, Base64.NO_WRAP)
            if (payload.size <= ivLength) return ""
            val iv = payload.copyOfRange(0, ivLength)
            val encrypted = payload.copyOfRange(ivLength, payload.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(GCM_TAG_BITS, iv))
            String(cipher.doFinal(encrypted), Charsets.UTF_8)
        } catch (t: Throwable) {
            Log.e(TAG, "Falha ao decifrar a API key", t)
            ""
        }
    }

    fun clearApiKey() {
        prefs.edit().remove(KEY_CIPHERTEXT).remove(KEY_IV_LENGTH).apply()
    }

    /** Mostra apenas os ultimos caracteres, para a interface nunca exibir a chave toda. */
    fun maskedApiKey(): String {
        val key = readApiKey()
        if (key.isBlank()) return ""
        val tail = key.takeLast(4)
        return "\u2022".repeat(16) + tail
    }

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        val existing = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        if (existing != null) return existing.secretKey

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "SecureKeyStore"
        const val PREFS_NAME = "secure_credentials"
        const val KEY_CIPHERTEXT = "gemini_api_key"
        const val KEY_IV_LENGTH = "gemini_api_key_iv_len"
        const val KEY_ALIAS = "ai_keyboard_api_key"
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val GCM_IV_LENGTH = 12
        const val GCM_TAG_BITS = 128
    }
}
