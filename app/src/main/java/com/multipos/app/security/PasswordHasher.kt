package com.multipos.app.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_LENGTH_BYTES = 16

    data class PasswordDigest(val hash: String, val salt: String)

    fun hash(password: CharArray): PasswordDigest {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val hash = derive(password, salt)
        return PasswordDigest(hash.toBase64(), salt.toBase64())
    }

    fun verify(password: CharArray, expectedHash: String, encodedSalt: String): Boolean {
        val salt = Base64.decode(encodedSalt, Base64.NO_WRAP)
        val actual = derive(password, salt)
        val expected = Base64.decode(expectedHash, Base64.NO_WRAP)
        return MessageDigest.isEqual(actual, expected)
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(password, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            password.fill('\u0000')
        }
    }

    private fun ByteArray.toBase64(): String = Base64.encodeToString(this, Base64.NO_WRAP)
}
