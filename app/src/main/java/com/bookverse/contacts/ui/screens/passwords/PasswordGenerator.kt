package com.bookverse.contacts.ui.screens.passwords

import java.security.SecureRandom
import java.time.LocalDate
import java.util.regex.Pattern

data class PasswordPolicy(
    val length: Int = 16,
    val upper: Boolean = true,
    val lower: Boolean = true,
    val digits: Boolean = true,
    val symbols: Boolean = true,
    val avoidAmbiguous: Boolean = true,
    val minClasses: Int = 3
)

object PasswordGenerator {
    private const val UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private const val LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz"
    private const val DIGIT_CHARS = "0123456789"
    private const val SYMBOL_CHARS = "!@#%^&*()-_=+[]{}:,.?"
    private val ambiguous = setOf('O','0','l','1','I')
    private val random = SecureRandom()

    private val commonPasswords = setOf(
        "password", "contraseña", "123456", "12345678", "123456789", "1234567890",
        "qwerty", "abc123", "password123", "admin", "letmein", "welcome",
        "monkey", "dragon", "master", "shadow", "sunshine", "princess",
        "azerty", "trustno1", "000000", "login", "hello", "guest",
        "admin123", "root", "toor", "pass", "test", "user", "demo",
        "secret", "security", "system", "computer", "internet", "email",
        "facebook", "google", "twitter", "instagram", "whatsapp", "android",
        "iphone", "samsung", "windows", "linux", "apple", "microsoft"
    )

    private val commonSpanishWords = setOf(
        "amor", "casa", "perro", "gato", "familia", "amigo", "trabajo",
        "escuela", "universidad", "colegio", "nombre", "apellido", "fecha",
        "cumpleanos", "cumpleaños", "navidad", "año", "mes", "dia", "día",
        "enero", "febrero", "marzo", "abril", "mayo", "junio", "julio",
        "agosto", "septiembre", "octubre", "noviembre", "diciembre",
        "lunes", "martes", "miercoles", "miércoles", "jueves", "viernes",
        "sabado", "sábado", "domingo", "mexico", "méxico", "colombia",
        "argentina", "españa", "chile", "peru", "perú", "venezuela"
    )

    private val keyboardPatterns = listOf(
        "qwerty", "asdf", "zxcv", "qwertyuiop", "asdfghjkl", "zxcvbnm",
        "1234567890", "qwer", "asdf", "zxcv", "123", "456", "789"
    )

    fun generate(policy: PasswordPolicy): String {
        val pools = mutableListOf<String>()
        if (policy.upper) pools += UPPER_CHARS
        if (policy.lower) pools += LOWER_CHARS
        if (policy.digits) pools += DIGIT_CHARS
        if (policy.symbols) pools += SYMBOL_CHARS
        val filteredPools = pools.map { p -> if (policy.avoidAmbiguous) p.filter { it !in ambiguous } else p }
        if (filteredPools.isEmpty()) return ""
        var lastPassword = ""
        var attempt = 0
        val maxAttempts = 20
        while (attempt < maxAttempts) {
            attempt++
            val chars = mutableListOf<Char>()
            filteredPools.forEach { pool -> chars += pool[random.nextInt(pool.length)] }
            while (chars.size < policy.length) {
                val pool = filteredPools[random.nextInt(filteredPools.size)]
                chars += pool[random.nextInt(pool.length)]
            }
            chars.shuffle(random)
            if (policy.minClasses > 0) {
                var satisfied = countClasses(chars.joinToString(""))
                var retries = 0
                while (satisfied < policy.minClasses && retries < 20) {
                    retries++
                    val pool = filteredPools[random.nextInt(filteredPools.size)]
                    chars[random.nextInt(chars.size)] = pool[random.nextInt(pool.length)]
                    satisfied = countClasses(chars.joinToString(""))
                }
            }
            lastPassword = chars.joinToString("")
            if (strength(lastPassword) >= 7) return lastPassword
        }
        return lastPassword
    }

    private fun countClasses(p: String): Int {
        var c = 0
        if (p.any { it.isUpperCase() }) c++
        if (p.any { it.isLowerCase() }) c++
        if (p.any { it.isDigit() }) c++
        if (p.any { !it.isLetterOrDigit() }) c++
        return c
    }

    fun strength(p: String): Int {
        if (p.isEmpty()) return 0

        val lowerPassword = p.lowercase()

        if (isBlacklisted(lowerPassword)) return 0

        if (hasCommonPatterns(lowerPassword)) return 1

        if (isDatePattern(p)) return 1

        if (hasKeyboardPattern(lowerPassword)) return 2

        if (isSimplePattern(p)) return 2

        val variety = countClasses(p)
        val lengthScore = when {
            p.length >= 20 -> 5
            p.length >= 16 -> 4
            p.length >= 12 -> 3
            p.length >= 8 -> 2
            p.length >= 6 -> 1
            else -> 0
        }

        val complexityBonus = when {
            variety >= 4 && p.length >= 12 -> 2
            variety >= 3 && p.length >= 10 -> 1
            else -> 0
        }

        val entropyBonus = if (hasHighEntropy(p)) 1 else 0

        return (variety + lengthScore + complexityBonus + entropyBonus).coerceIn(3, 10)
    }

    private fun isBlacklisted(password: String): Boolean {
        if (password in commonPasswords) return true
        if (password in commonSpanishWords) return true

        commonPasswords.forEach { common ->
            if (password.contains(common) && password.length <= common.length + 3) return true
        }

        return false
    }

    private fun hasCommonPatterns(password: String): Boolean {
        val consecutiveChars = "abcdefghijklmnopqrstuvwxyz"
        val consecutiveNumbers = "0123456789"

        for (i in 0..consecutiveChars.length - 4) {
            val pattern = consecutiveChars.substring(i, i + 4)
            if (password.contains(pattern) || password.contains(pattern.reversed())) return true
        }

        for (i in 0..consecutiveNumbers.length - 4) {
            val pattern = consecutiveNumbers.substring(i, i + 4)
            if (password.contains(pattern) || password.contains(pattern.reversed())) return true
        }

        return false
    }

    private fun isDatePattern(password: String): Boolean {
        val datePatterns = listOf(
            "\\d{1,2}/\\d{1,2}/\\d{2,4}",
            "\\d{1,2}-\\d{1,2}-\\d{2,4}",
            "\\d{4}",
            "\\d{2}/\\d{2}",
            "\\d{2}-\\d{2}"
        )

        datePatterns.forEach { pattern ->
            if (Pattern.compile(pattern).matcher(password).find()) {
                return true
            }
        }

        val currentYear = LocalDate.now().year
        for (year in (currentYear - 50)..(currentYear + 10)) {
            if (password.contains(year.toString())) return true
        }

        return false
    }

    private fun hasKeyboardPattern(password: String): Boolean {
        keyboardPatterns.forEach { pattern ->
            if (password.contains(pattern) || password.contains(pattern.reversed())) return true
        }
        return false
    }

    private fun isSimplePattern(password: String): Boolean {
        if (password.all { it.isDigit() }) return true
        if (password.all { it.isLetter() }) return true

        val repeatedPattern = Regex("(.)\\1{2,}")
        if (repeatedPattern.containsMatchIn(password)) return true

        val alternatingPattern = Regex("(..+?)\\1+")
        return alternatingPattern.containsMatchIn(password) && password.length >= 6
    }

    private fun hasHighEntropy(password: String): Boolean {
        val charFrequency = password.groupingBy { it }.eachCount()
        val totalChars = password.length.toDouble()

        var entropy = 0.0
        charFrequency.values.forEach { count ->
            val probability = count / totalChars
            entropy -= probability * kotlin.math.log2(probability)
        }

        return entropy >= 3.5
    }
}
