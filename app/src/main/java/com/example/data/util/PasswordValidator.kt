package com.example.data.util

data class PasswordValidationResult(
    val isValid: Boolean,
    val hasMinLength: Boolean,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigit: Boolean,
    val hasSpecialChar: Boolean,
    val missingRequirementsMessage: String = ""
)

object PasswordValidator {
    fun validate(password: String): PasswordValidationResult {
        val hasMinLength = password.length >= 8
        val hasUppercase = password.any { it.isUpperCase() }
        val hasLowercase = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecialChar = password.any { !it.isLetterOrDigit() }

        val missingList = mutableListOf<String>()
        if (!hasMinLength) missingList.add("8+ chars")
        if (!hasUppercase) missingList.add("1 uppercase letter (A-Z)")
        if (!hasLowercase) missingList.add("1 lowercase letter (a-z)")
        if (!hasDigit) missingList.add("1 digit (0-9)")
        if (!hasSpecialChar) missingList.add("1 special character (!@#$%^&*)")

        val isValid = hasMinLength && hasUppercase && hasLowercase && hasDigit && hasSpecialChar
        val missingMessage = if (missingList.isNotEmpty()) {
            "Requires: " + missingList.joinToString(", ")
        } else {
            "✓ Strong password"
        }

        return PasswordValidationResult(
            isValid = isValid,
            hasMinLength = hasMinLength,
            hasUppercase = hasUppercase,
            hasLowercase = hasLowercase,
            hasDigit = hasDigit,
            hasSpecialChar = hasSpecialChar,
            missingRequirementsMessage = missingMessage
        )
    }
}
