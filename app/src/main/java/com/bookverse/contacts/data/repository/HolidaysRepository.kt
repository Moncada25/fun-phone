package com.bookverse.contacts.data.repository

import com.bookverse.contacts.data.model.PublicHoliday
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.URL
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.HttpsURLConnection

class HolidaysRepository private constructor(
    private val remoteSource: HolidaysRemoteSource,
    private val nowMillis: () -> Long,
    private val gson: Gson,
) {
    constructor() : this(
        remoteSource = NagerDateHolidayRemoteSource(),
        nowMillis = System::currentTimeMillis,
        gson = Gson(),
    )

    internal constructor(
        remoteSource: HolidaysRemoteSource,
        nowMillis: () -> Long = System::currentTimeMillis,
    ) : this(remoteSource, nowMillis, Gson())

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    suspend fun getHolidays(year: Int, countryCode: String): Result<List<PublicHoliday>> {
        val normalizedCountry = countryCode.uppercase(Locale.ROOT)
        if (!COUNTRY_CODE.matches(normalizedCountry)) {
            return Result.failure(HolidaysRepositoryException.ClientError(400))
        }

        val cacheKey = "${normalizedCountry}_$year"
        cache[cacheKey]?.let { entry ->
            if (nowMillis() - entry.timestamp < cacheTtl(year)) {
                return Result.success(entry.data)
            }
            cache.remove(cacheKey)
        }

        return try {
            val response = remoteSource.fetch(year, normalizedCountry)
            when (response.statusCode) {
                in 200..299 -> {
                    val type = object : TypeToken<List<PublicHoliday>>() {}.type
                    val holidays: List<PublicHoliday> =
                        gson.fromJson(response.body.ifBlank { "[]" }, type) ?: emptyList()
                    cache[cacheKey] = CacheEntry(holidays, nowMillis())
                    Result.success(holidays)
                }

                in 400..499 ->
                    Result.failure(HolidaysRepositoryException.ClientError(response.statusCode))

                in 500..599 ->
                    Result.failure(HolidaysRepositoryException.ServerError(response.statusCode))

                else -> Result.failure(
                    HolidaysRepositoryException.Unknown(
                        detail = "Unexpected response code: ${response.statusCode}",
                    ),
                )
            }
        } catch (exception: HolidaysRepositoryException) {
            Result.failure(exception)
        } catch (exception: Exception) {
            Result.failure(HolidaysRepositoryException.Unknown(original = exception))
        }
    }

    private fun cacheTtl(year: Int): Long {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        return if (year < currentYear) TTL_PAST_YEAR_MS else TTL_CURRENT_YEAR_MS
    }

    private data class CacheEntry(
        val data: List<PublicHoliday>,
        val timestamp: Long,
    )

    private companion object {
        val COUNTRY_CODE = Regex("^[A-Z]{2}$")
        const val TTL_PAST_YEAR_MS = 30L * 24 * 60 * 60 * 1000L
        const val TTL_CURRENT_YEAR_MS = 7L * 24 * 60 * 60 * 1000L
    }
}

internal fun interface HolidaysRemoteSource {
    suspend fun fetch(year: Int, countryCode: String): HolidaysHttpResponse
}

internal data class HolidaysHttpResponse(
    val statusCode: Int,
    val body: String,
)

private class NagerDateHolidayRemoteSource : HolidaysRemoteSource {
    override suspend fun fetch(year: Int, countryCode: String): HolidaysHttpResponse =
        withContext(Dispatchers.IO) {
            val url = URL("$BASE_URL/PublicHolidays/$year/$countryCode")
            val connection = url.openConnection() as HttpsURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MS
                connection.readTimeout = READ_TIMEOUT_MS
                connection.instanceFollowRedirects = false
                connection.setRequestProperty("Accept", "application/json")

                val statusCode = connection.responseCode
                val stream = if (statusCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                HolidaysHttpResponse(
                    statusCode = statusCode,
                    body = stream?.use { it.readUtf8Limited(MAX_RESPONSE_BYTES) }.orEmpty(),
                )
            } finally {
                connection.disconnect()
            }
        }

    private fun InputStream.readUtf8Limited(maxBytes: Int): String {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var total = 0
        while (true) {
            val count = read(buffer)
            if (count < 0) break
            total += count
            if (total > maxBytes) {
                throw IOException("Holiday response exceeds the allowed size")
            }
            output.write(buffer, 0, count)
        }
        return output.toString(Charsets.UTF_8.name())
    }

    private companion object {
        const val BASE_URL = "https://date.nager.at/api/v3"
        const val CONNECT_TIMEOUT_MS = 10_000
        const val READ_TIMEOUT_MS = 15_000
        const val MAX_RESPONSE_BYTES = 2 * 1024 * 1024
    }
}
