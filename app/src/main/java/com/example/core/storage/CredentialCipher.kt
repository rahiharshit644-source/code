package com.example.core.storage

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Encrypts small credentials at rest using an AES-GCM key protected by Android Keystore. */
internal object CredentialCipher {
    private const val KEY_ALIAS = "codepilot.credentials.aesgcm.v1"
    private const val PREFIX = "enc:v1:"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val IV_SIZE_BYTES = 12
    private const val TAG_SIZE_BITS = 128

    fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        if (value.startsWith(PREFIX)) return value
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val iv = cipher.iv
        val encrypted = cipher.doFinal(value.toByteArray(StandardCharsets.UTF_8))
        val packed = ByteBuffer.allocate(iv.size + encrypted.size).put(iv).put(encrypted).array()
        return PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP)
    }

    /** Legacy plaintext is returned for migration compatibility and encrypted on the next save. */
    fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        if (!value.startsWith(PREFIX)) return value
        return try {
            val packed = Base64.decode(value.removePrefix(PREFIX), Base64.NO_WRAP)
            if (packed.size <= IV_SIZE_BYTES) return ""
            val iv = packed.copyOfRange(0, IV_SIZE_BYTES)
            val encrypted = packed.copyOfRange(IV_SIZE_BYTES, packed.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
            String(cipher.doFinal(encrypted), StandardCharsets.UTF_8)
        } catch (_: Exception) {
            // Never surface corrupted ciphertext as a usable credential.
            ""
        }
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }
}
