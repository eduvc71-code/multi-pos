package com.multipos.app.security

import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object PinHasher {
    private const val ITERATIONS = 100_000
    private const val KEY_LENGTH_BITS = 128
    private const val SALT_LENGTH_BYTES = 16

    data class PinDigest(val hash: String, val salt: String)

    fun hash(pin: CharArray): PinDigest {
        val salt = ByteArray(SALT_LENGTH_BYTES).also(SecureRandom()::nextBytes)
        val hash = derive(pin, salt)
        return PinDigest(hash.toBase64(), salt.toBase64())
    }

    fun verify(pin: CharArray, expectedHash: String, encodedSalt: String): Boolean {
        val salt = Base64Codec.decode(encodedSalt)
        val actual = derive(pin, salt)
        val expected = Base64Codec.decode(expectedHash)
        return java.security.MessageDigest.isEqual(actual, expected)
    }

    private fun derive(pin: CharArray, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(pin, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            pin.fill('\u0000')
        }
    }

    private fun ByteArray.toBase64(): String = Base64Codec.encode(this)
}

private object Base64Codec {
    private const val ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"

    fun encode(bytes: ByteArray): String {
        val result = StringBuilder(((bytes.size + 2) / 3) * 4)
        var index = 0
        while (index < bytes.size) {
            val first = bytes[index++].toInt() and 0xff
            val hasSecond = index < bytes.size
            val second = if (hasSecond) bytes[index++].toInt() and 0xff else 0
            val hasThird = index < bytes.size
            val third = if (hasThird) bytes[index++].toInt() and 0xff else 0
            val combined = (first shl 16) or (second shl 8) or third
            result.append(ALPHABET[(combined ushr 18) and 0x3f])
            result.append(ALPHABET[(combined ushr 12) and 0x3f])
            result.append(if (hasSecond) ALPHABET[(combined ushr 6) and 0x3f] else '=')
            result.append(if (hasThird) ALPHABET[combined and 0x3f] else '=')
        }
        return result.toString()
    }

    fun decode(value: String): ByteArray {
        require(value.length % 4 == 0) { "Base64 inválido" }
        val padding = when {
            value.endsWith("==") -> 2
            value.endsWith("=") -> 1
            else -> 0
        }
        val result = ByteArray((value.length / 4) * 3 - padding)
        var inputIndex = 0
        var outputIndex = 0
        while (inputIndex < value.length) {
            val first = sextet(value[inputIndex++])
            val second = sextet(value[inputIndex++])
            val thirdChar = value[inputIndex++]
            val fourthChar = value[inputIndex++]
            val third = if (thirdChar == '=') 0 else sextet(thirdChar)
            val fourth = if (fourthChar == '=') 0 else sextet(fourthChar)
            val combined = (first shl 18) or (second shl 12) or (third shl 6) or fourth
            if (outputIndex < result.size) result[outputIndex++] = (combined ushr 16).toByte()
            if (outputIndex < result.size) result[outputIndex++] = (combined ushr 8).toByte()
            if (outputIndex < result.size) result[outputIndex++] = combined.toByte()
        }
        return result
    }

    private fun sextet(character: Char): Int {
        val index = ALPHABET.indexOf(character)
        require(index >= 0) { "Base64 inválido" }
        return index
    }
}
