package com.multipos.app.security

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PinHasherTest {

    @Test
    fun hashAndVerifyCorrectPin() {
        val pin = "1234"
        val digest = PinHasher.hash(pin.toCharArray())
        assertTrue(PinHasher.verify(pin.toCharArray(), digest.hash, digest.salt))
    }

    @Test
    fun verifyFailsWithWrongPin() {
        val digest = PinHasher.hash("1234".toCharArray())
        assertFalse(PinHasher.verify("5678".toCharArray(), digest.hash, digest.salt))
    }

    @Test
    fun hashGeneratesDifferentSalts() {
        val digest1 = PinHasher.hash("1234".toCharArray())
        val digest2 = PinHasher.hash("1234".toCharArray())
        assertFalse(digest1.salt == digest2.salt)
        assertFalse(digest1.hash == digest2.hash)
    }

    @Test
    fun hashProducesBase64EncodedStrings() {
        val digest = PinHasher.hash("1234".toCharArray())
        assertTrue(digest.hash.isNotEmpty())
        assertTrue(digest.salt.isNotEmpty())
    }
}
