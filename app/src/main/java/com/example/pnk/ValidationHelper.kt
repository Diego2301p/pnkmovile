package com.example.pnk

import android.util.Patterns

/**
 * Centralized validation utilities used across all forms.
 */
object ValidationHelper {

    fun isEmailValid(email: String): Boolean {
        return email.isNotBlank() &&
               Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    fun isPasswordStrong(password: String): Boolean {
        if (password.length < 8) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { !it.isLetterOrDigit() }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    fun isOnlyLettersAndSpaces(text: String): Boolean {
        return text.all { it.isLetter() || it.isWhitespace() }
    }

    fun passwordsMatch(password: String, confirm: String): Boolean {
        return password == confirm && password.isNotBlank()
    }
}
