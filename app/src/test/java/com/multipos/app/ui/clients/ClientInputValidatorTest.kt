package com.multipos.app.ui.clients

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClientInputValidatorTest {

    @Test
    fun `validate with valid inputs returns success`() {
        val result = ClientInputValidator.validate("  John Doe  ", "  12345  ")
        assertTrue(result is ClientInputValidator.ValidationResult.Success)
        val success = result as ClientInputValidator.ValidationResult.Success
        assertEquals("John Doe", success.normalizedName)
        assertEquals("12345", success.normalizedDocument)
    }

    @Test
    fun `validate with spaces only name returns empty name error`() {
        val result = ClientInputValidator.validate("   ", "12345")
        assertTrue(result is ClientInputValidator.ValidationResult.EmptyName)
    }

    @Test
    fun `validate with empty name returns empty name error`() {
        val result = ClientInputValidator.validate("", "12345")
        assertTrue(result is ClientInputValidator.ValidationResult.EmptyName)
    }

    @Test
    fun `validate with spaces only document returns empty document error`() {
        val result = ClientInputValidator.validate("John Doe", "   ")
        assertTrue(result is ClientInputValidator.ValidationResult.EmptyDocument)
    }

    @Test
    fun `validate with empty document returns empty document error`() {
        val result = ClientInputValidator.validate("John Doe", "")
        assertTrue(result is ClientInputValidator.ValidationResult.EmptyDocument)
    }

    @Test
    fun `validate preserves internal spaces`() {
        val result = ClientInputValidator.validate("  John   Doe  ", "  123 45  ")
        assertTrue(result is ClientInputValidator.ValidationResult.Success)
        val success = result as ClientInputValidator.ValidationResult.Success
        assertEquals("John   Doe", success.normalizedName)
        assertEquals("123 45", success.normalizedDocument)
    }
}