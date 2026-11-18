package com.bookverse.contacts.domain.passwords

import java.nio.ByteBuffer
import java.util.Locale
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

enum class TotpAlgorithm(val macName: String) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
}

data class TotpConfig(
    val secret: String,
    val digits: Int = Totp.DEFAULT_DIGITS,
    val periodSeconds: Int = Totp.DEFAULT_PERIOD_SECONDS,
    val algorithm: TotpAlgorithm = TotpAlgorithm.SHA1,
    val issuer: String? = null,
    val account: String? = null,
)

object Totp {
    const val DEFAULT_DIGITS = 6
    const val DEFAULT_PERIOD_SECONDS = 30
    private const val SCHEME = "otpauth://"
    private const val BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
    private val DIGIT_RANGE = 6..8
    private val PERIOD_RANGE = 1..300

    fun parse(input: String?): TotpConfig? {
        val value = input?.trim().orEmpty()
        if (value.isEmpty()) return null
        return if (value.startsWith(SCHEME, ignoreCase = true)) parseUri(value) else parseSecret(value)
    }

    fun toUri(config: TotpConfig): String {
        val label = listOfNotNull(
            config.issuer?.takeIf { it.isNotBlank() },
            config.account?.takeIf { it.isNotBlank() },
        ).joinToString(":") { percentEncode(it) }
        val query = buildList {
            add("secret=${config.secret}")
            config.issuer?.takeIf { it.isNotBlank() }?.let { add("issuer=${percentEncode(it)}") }
            add("algorithm=${config.algorithm.name}")
            add("digits=${config.digits}")
            add("period=${config.periodSeconds}")
        }.joinToString("&")
        return "${SCHEME}totp/$label?$query"
    }

    fun code(config: TotpConfig, epochSeconds: Long): String {
        val key = decodeBase32(config.secret) ?: return ""
        val counter = Math.floorDiv(epochSeconds, config.periodSeconds.toLong())
        val mac = Mac.getInstance(config.algorithm.macName)
        mac.init(SecretKeySpec(key, config.algorithm.macName))
        val hash = mac.doFinal(ByteBuffer.allocate(Long.SIZE_BYTES).putLong(counter).array())
        val offset = hash.last().toInt() and 0x0F
        val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
            ((hash[offset + 1].toInt() and 0xFF) shl 16) or
            ((hash[offset + 2].toInt() and 0xFF) shl 8) or
            (hash[offset + 3].toInt() and 0xFF)
        var modulus = 1
        repeat(config.digits) { modulus *= 10 }
        return (binary % modulus).toString().padStart(config.digits, '0')
    }

    fun remainingSeconds(config: TotpConfig, epochSeconds: Long): Int {
        val period = config.periodSeconds.toLong()
        return (period - Math.floorMod(epochSeconds, period)).toInt()
    }

    internal fun decodeBase32(value: String): ByteArray? {
        val cleaned = normalizeSecret(value)
        if (cleaned.isEmpty()) return null
        val output = ByteArray(cleaned.length * 5 / 8)
        var buffer = 0
        var bits = 0
        var index = 0
        for (char in cleaned) {
            val digit = BASE32_ALPHABET.indexOf(char)
            if (digit < 0) return null
            buffer = (buffer shl 5) or digit
            bits += 5
            if (bits >= 8) {
                bits -= 8
                output[index++] = ((buffer shr bits) and 0xFF).toByte()
            }
        }
        return output.copyOf(index).takeIf { it.isNotEmpty() }
    }

    private fun normalizeSecret(value: String): String =
        value.filterNot { it.isWhitespace() || it == '-' || it == '=' }.uppercase(Locale.ROOT)

    private fun parseSecret(value: String): TotpConfig? {
        val secret = normalizeSecret(value)
        return if (decodeBase32(secret) == null) null else TotpConfig(secret = secret)
    }

    private fun parseUri(value: String): TotpConfig? {
        val rest = value.substring(SCHEME.length)
        val typeEnd = rest.indexOf('/')
        if (typeEnd < 0 || !rest.substring(0, typeEnd).equals("totp", ignoreCase = true)) return null
        val afterType = rest.substring(typeEnd + 1)
        val queryStart = afterType.indexOf('?')
        if (queryStart < 0) return null
        val label = percentDecode(afterType.substring(0, queryStart))
        val parameters = afterType.substring(queryStart + 1)
            .substringBefore('#')
            .split('&')
            .mapNotNull { part ->
                val separator = part.indexOf('=')
                if (separator <= 0) null
                else part.substring(0, separator).lowercase(Locale.ROOT) to percentDecode(part.substring(separator + 1))
            }
            .toMap()
        val secret = normalizeSecret(parameters["secret"].orEmpty())
        if (decodeBase32(secret) == null) return null
        val digits = parameters["digits"]?.let { it.toIntOrNull() ?: return null } ?: DEFAULT_DIGITS
        val period = parameters["period"]?.let { it.toIntOrNull() ?: return null } ?: DEFAULT_PERIOD_SECONDS
        if (digits !in DIGIT_RANGE || period !in PERIOD_RANGE) return null
        val algorithm = parameters["algorithm"]?.let { name ->
            TotpAlgorithm.entries.firstOrNull { it.name.equals(name.replace("-", ""), ignoreCase = true) }
                ?: return null
        } ?: TotpAlgorithm.SHA1
        val labelIssuer = label.substringBefore(':', "").trim()
        val account = label.substringAfter(':', label).trim()
        return TotpConfig(
            secret = secret,
            digits = digits,
            periodSeconds = period,
            algorithm = algorithm,
            issuer = (parameters["issuer"]?.trim() ?: labelIssuer).takeIf { it.isNotEmpty() },
            account = account.takeIf { it.isNotEmpty() },
        )
    }

    private fun percentDecode(value: String): String {
        if ('%' !in value) return value
        val bytes = ArrayList<Byte>(value.length)
        var index = 0
        while (index < value.length) {
            val char = value[index]
            val hex = if (char == '%' && index + 2 <= value.lastIndex) {
                value.substring(index + 1, index + 3).toIntOrNull(16)
            } else {
                null
            }
            if (hex != null) {
                bytes += hex.toByte()
                index += 3
            } else {
                char.toString().toByteArray(Charsets.UTF_8).forEach { bytes += it }
                index++
            }
        }
        return String(bytes.toByteArray(), Charsets.UTF_8)
    }

    private fun percentEncode(value: String): String = buildString {
        for (byte in value.toByteArray(Charsets.UTF_8)) {
            val char = (byte.toInt() and 0xFF).toChar()
            if (char.isLetterOrDigit() && char.code < 128 || char in "-._~") {
                append(char)
            } else {
                append('%').append("%02X".format(Locale.ROOT, byte.toInt() and 0xFF))
            }
        }
    }
}
