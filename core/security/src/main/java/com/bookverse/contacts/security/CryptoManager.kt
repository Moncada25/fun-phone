@file:Suppress("DEPRECATION")

package com.bookverse.contacts.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.bookverse.contacts.diagnostics.DiagnosticEvent
import com.bookverse.contacts.diagnostics.DiagnosticLog
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object CryptoManager {
    private const val KEY_SIZE_BITS = 256
    private const val GCM_TAG_BITS = 128
    private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
    private const val KEY_ALIAS_SUFFIX = "pm_master_key"
    private const val WRAPPED_KEY_PREFS = "crypto_manager_prefs"
    private const val WRAPPED_KEY_KEY = "wrapped_dek"
    private const val LEGACY_PREFS_NAME = "secure_prefs"
    private const val LEGACY_KEY_NAME = "pm_dek"
    private const val LEGACY_KEY_SIZE_BYTES = 32

    private val secureRandom = SecureRandom()
    @Volatile
    private var cachedDek: ByteArray? = null

    private fun buildKeyAlias(context: Context): String {
        return "${context.packageName}.$KEY_ALIAS_SUFFIX"
    }

    private fun getOrCreateSecretKey(context: Context): SecretKey {
        return try {
            val alias = buildKeyAlias(context)
            val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
            (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }

            val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
            val parameterSpec = KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                .build()

            keyGenerator.init(parameterSpec)
            keyGenerator.generateKey()
        } catch (exception: GeneralSecurityException) {
            DiagnosticLog.error(DiagnosticEvent.CRYPTO_MASTER_KEY_ACCESS_FAILED)
            throw IllegalStateException("Unable to access encryption key", exception)
        } catch (exception: IOException) {
            DiagnosticLog.error(DiagnosticEvent.CRYPTO_KEYSTORE_LOAD_FAILED)
            throw IllegalStateException("Unable to access encryption key", exception)
        }
    }

    private fun getDek(context: Context): ByteArray {
        cachedDek?.let { return it }

        synchronized(this) {
            cachedDek?.let { return it }

            val wrapped = readWrappedDek(context)
            val dek = if (wrapped != null) {
                try {
                    unwrapDek(context, wrapped)
                } catch (exception: GeneralSecurityException) {
                    DiagnosticLog.error(DiagnosticEvent.CRYPTO_KEY_UNWRAP_FAILED)
                    migrateFromLegacy(context) ?: throw IllegalStateException(
                        "Unable to recover encryption key",
                        exception
                    )
                }
            } else {
                migrateFromLegacy(context) ?: createAndStoreDek(context)
            }

            cachedDek = dek
            return dek
        }
    }

    private fun createAndStoreDek(context: Context): ByteArray {
        val newDek = ByteArray(LEGACY_KEY_SIZE_BYTES).also { secureRandom.nextBytes(it) }
        storeWrappedDek(context, newDek)
        return newDek
    }

    private fun readWrappedDek(context: Context): ByteArray? {
        val prefs = context.getSharedPreferences(WRAPPED_KEY_PREFS, Context.MODE_PRIVATE)
        val stored = prefs.getString(WRAPPED_KEY_KEY, null) ?: return null
        return try {
            Base64.decode(stored, Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            DiagnosticLog.error(DiagnosticEvent.CRYPTO_WRAPPED_KEY_INVALID)
            null
        }
    }

    private fun storeWrappedDek(context: Context, dek: ByteArray) {
        val wrapped = wrapDek(context, dek)
        val prefs = context.getSharedPreferences(WRAPPED_KEY_PREFS, Context.MODE_PRIVATE)
        prefs.edit {
            putString(WRAPPED_KEY_KEY, Base64.encodeToString(wrapped, Base64.NO_WRAP))
        }
    }

    private fun wrapDek(context: Context, dek: ByteArray): ByteArray {
        val secretKey = getOrCreateSecretKey(context)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(dek)
        val wrapped = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, wrapped, 0, iv.size)
        System.arraycopy(cipherBytes, 0, wrapped, iv.size, cipherBytes.size)
        return wrapped
    }

    private fun unwrapDek(context: Context, wrapped: ByteArray): ByteArray {
        if (wrapped.size <= 12) throw GeneralSecurityException("Wrapped key too short")
        val secretKey = getOrCreateSecretKey(context)
        val iv = wrapped.copyOfRange(0, 12)
        val cipherBytes = wrapped.copyOfRange(12, wrapped.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }

    @Suppress("DEPRECATION")
    private fun migrateFromLegacy(context: Context): ByteArray? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val prefs = EncryptedSharedPreferences.create(
                context,
                LEGACY_PREFS_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val encoded = prefs.getString(LEGACY_KEY_NAME, null) ?: return null
            val dek = try {
                Base64.decode(encoded, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                DiagnosticLog.error(DiagnosticEvent.CRYPTO_LEGACY_KEY_DECODE_FAILED)
                null
            }

            if (dek == null || dek.size != LEGACY_KEY_SIZE_BYTES) {
                DiagnosticLog.warning(DiagnosticEvent.CRYPTO_LEGACY_KEY_LENGTH_INVALID)
                return null
            }

            storeWrappedDek(context, dek)
            prefs.edit { remove(LEGACY_KEY_NAME) }
            dek
        } catch (_: Exception) {
            DiagnosticLog.warning(DiagnosticEvent.CRYPTO_LEGACY_KEY_UNAVAILABLE)
            null
        }
    }

    fun encrypt(context: Context, data: ByteArray?): ByteArray? {
        if (data == null) return null
        val keyBytes = getDek(context)
        val key = SecretKeySpec(keyBytes, "AES")
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv
        val cipherBytes = cipher.doFinal(data)
        val out = ByteArray(iv.size + cipherBytes.size)
        System.arraycopy(iv, 0, out, 0, iv.size)
        System.arraycopy(cipherBytes, 0, out, iv.size, cipherBytes.size)
        return out
    }

    fun decrypt(context: Context, blob: ByteArray?): ByteArray? {
        if (blob == null || blob.size < 12) return null
        val keyBytes = getDek(context)
        val key = SecretKeySpec(keyBytes, "AES")
        val iv = blob.copyOfRange(0, 12)
        val cipherBytes = blob.copyOfRange(12, blob.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(GCM_TAG_BITS, iv))
        return cipher.doFinal(cipherBytes)
    }
}
