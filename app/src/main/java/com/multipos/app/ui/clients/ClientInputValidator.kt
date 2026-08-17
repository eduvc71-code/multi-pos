package com.multipos.app.ui.clients

object ClientInputValidator {

    sealed class ValidationResult {
        data class Success(val normalizedName: String, val normalizedDocument: String) : ValidationResult()
        object EmptyName : ValidationResult()
        object EmptyDocument : ValidationResult()
    }

    fun validate(name: String, document: String): ValidationResult {
        val normalizedName = name.trim()
        val normalizedDocument = document.trim()

        if (normalizedName.isEmpty()) {
            return ValidationResult.EmptyName
        }
        if (normalizedDocument.isEmpty()) {
            return ValidationResult.EmptyDocument
        }
        return ValidationResult.Success(normalizedName, normalizedDocument)
    }
}