package com.example.utils

object ValidationUtils {
    fun isValidEmailOrPhone(input: String): Boolean {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) return false
        val isEmail = android.util.Patterns.EMAIL_ADDRESS.matcher(trimmed).matches()
        val digitsOnly = trimmed.filter { it.isDigit() }
        val isPhone = digitsOnly.length in 7..15
        return isEmail || isPhone
    }

    fun isValidPassword(password: String): Boolean = password.length >= 6

    fun doPasswordsMatch(password: String, confirm: String): Boolean =
        password.isNotEmpty() && password == confirm
}
