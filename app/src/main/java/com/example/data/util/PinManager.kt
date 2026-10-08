package com.example.data.util

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed class PinResult {
    object Success : PinResult()
    data class Wrong(val attemptsLeft: Int) : PinResult()
    object LockedOut : PinResult()
    object NotSet : PinResult()
}

/**
 * Stores a per-user 4-digit quick-sign-in PIN on this device only.
 *
 * - The PIN itself is never stored; only a salted PBKDF2 hash, additionally keyed with a
 *   non-exportable Android Keystore HMAC key (so the hash is useless if copied off the phone).
 * - After [MAX_ATTEMPTS] wrong tries the PIN is removed and the user must sign in with their password.
 */
class PinManager(context: Context) {

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasPin(userId: Long): Boolean = prefs.contains(keyHash(userId))

    fun usersWithPin(): Set<Long> =
        prefs.all.keys
            .filter { it.startsWith("hash_") }
            .mapNotNull { it.removePrefix("hash_").toLongOrNull() }
            .toSet()

    fun setPin(userId: Long, pin: String) {
        require(isFormatValid(pin)) { "PIN must be exactly 4 digits." }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val hash = hashPin(pin, salt)
        prefs.edit()
            .putString(keyHash(userId), Base64.encodeToString(hash, Base64.NO_WRAP))
            .putString(keySalt(userId), Base64.encodeToString(salt, Base64.NO_WRAP))
            .putInt(keyAttempts(userId), 0)
            .remove(keyNever(userId))
            .apply()
    }

    fun verify(userId: Long, pin: String): PinResult {
        val storedHash = prefs.getString(keyHash(userId), null)
        val storedSalt = prefs.getString(keySalt(userId), null)
        if (storedHash == null || storedSalt == null) return PinResult.NotSet

        val expected = Base64.decode(storedHash, Base64.NO_WRAP)
        val actual = hashPin(pin, Base64.decode(storedSalt, Base64.NO_WRAP))

        return if (MessageDigest.isEqual(expected, actual)) {
            prefs.edit().putInt(keyAttempts(userId), 0).apply()
            PinResult.Success
        } else {
            val attempts = prefs.getInt(keyAttempts(userId), 0) + 1
            if (attempts >= MAX_ATTEMPTS) {
                clearPin(userId)
                PinResult.LockedOut
            } else {
                prefs.edit().putInt(keyAttempts(userId), attempts).apply()
                PinResult.Wrong(MAX_ATTEMPTS - attempts)
            }
        }
    }

    fun clearPin(userId: Long) {
        prefs.edit()
            .remove(keyHash(userId))
            .remove(keySalt(userId))
            .remove(keyAttempts(userId))
            .apply()
    }

    /** Removes every PIN (used on logout, because logout wipes this device's bar data). */
    fun clearAll() {
        prefs.edit().clear().apply()
    }

    /** Removes PINs for accounts that no longer exist locally. */
    fun retainOnly(validUserIds: Set<Long>) {
        usersWithPin().filter { it !in validUserIds }.forEach { clearPin(it) }
    }

    fun isPromptSuppressed(userId: Long): Boolean = prefs.getBoolean(keyNever(userId), false)

    fun suppressPrompt(userId: Long) {
        prefs.edit().putBoolean(keyNever(userId), true).apply()
    }

    private fun hashPin(pin: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, 256)
        val derived = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return keystoreMac(derived) ?: derived
    }

    private fun keystoreMac(data: ByteArray): ByteArray? = try {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (!ks.containsAlias(KEY_ALIAS)) {
            val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_HMAC_SHA256, "AndroidKeyStore")
            generator.init(KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN).build())
            generator.generateKey()
        }
        val key = ks.getKey(KEY_ALIAS, null) as SecretKey
        Mac.getInstance("HmacSHA256").apply { init(key) }.doFinal(data)
    } catch (e: Exception) {
        null
    }

    private fun keyHash(id: Long) = "hash_$id"
    private fun keySalt(id: Long) = "salt_$id"
    private fun keyAttempts(id: Long) = "attempts_$id"
    private fun keyNever(id: Long) = "never_$id"

    companion object {
        private const val PREFS_NAME = "quick_pin_prefs"
        private const val KEY_ALIAS = "bar_stock_pin_hmac"
        private const val ITERATIONS = 60_000
        const val MAX_ATTEMPTS = 5
        const val PIN_LENGTH = 4

        fun isFormatValid(pin: String): Boolean =
            pin.length == PIN_LENGTH && pin.all { it.isDigit() }

        /** Rejects trivially guessable PINs like 0000, 1111, 1234, 4321. */
        fun isTooWeak(pin: String): Boolean {
            if (pin.all { it == pin[0] }) return true
            val digits = pin.map { it - '0' }
            val ascending = digits.zipWithNext().all { (a, b) -> b - a == 1 }
            val descending = digits.zipWithNext().all { (a, b) -> a - b == 1 }
            return ascending || descending
        }
    }
}
