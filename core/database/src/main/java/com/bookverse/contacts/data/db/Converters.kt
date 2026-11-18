package com.bookverse.contacts.data.db

import android.net.Uri
import androidx.room.TypeConverter
import com.bookverse.contacts.data.expenses.ExpenseType
import com.bookverse.contacts.data.model.ContactAccount
import com.bookverse.contacts.data.model.ContactPhone
import com.bookverse.contacts.data.model.RecentType
import com.bookverse.contacts.data.qr.QRType
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.LocalDate

object Converters {
    private val gson = Gson()

    @TypeConverter
    @JvmStatic
    fun fromOperationType(value: RecentType): String = value.name

    @TypeConverter
    @JvmStatic
    fun toOperationType(value: String): RecentType = RecentType.valueOf(value)

    @TypeConverter
    @JvmStatic
    fun fromUri(uri: Uri?): String? = uri?.toString()

    @TypeConverter
    @JvmStatic
    fun toUri(uriString: String?): Uri? = uriString?.let { Uri.parse(it) }

    @TypeConverter
    @JvmStatic
    fun fromContactAccounts(accounts: List<ContactAccount>?): String? =
        accounts?.let { gson.toJson(it) }

    @TypeConverter
    @JvmStatic
    fun toContactAccounts(accountsJson: String?): List<ContactAccount>? = accountsJson?.let {
        gson.fromJson(it, Array<ContactAccount>::class.java)?.toList()
    }

    @TypeConverter
    fun fromPhonesList(phones: List<ContactPhone>): String {
        return Gson().toJson(phones)
    }

    @TypeConverter
    fun toPhonesLis(phonesJson: String): List<ContactPhone> {
        val listType = object : TypeToken<List<ContactPhone>>() {}.type
        return Gson().fromJson(phonesJson, listType)
    }

    @TypeConverter
    fun fromSocialNetworks(socialNetworks: List<String>): String {
        return Gson().toJson(socialNetworks)
    }

    @TypeConverter
    fun toSocialNetworks(socialNetworksJson: String): List<String> {
        val listType = object : TypeToken<List<String>>() {}.type
        return Gson().fromJson(socialNetworksJson, listType) ?: emptyList()
    }

    @TypeConverter
    @JvmStatic
    fun fromExpenseType(value: ExpenseType?): String? = value?.name

    @TypeConverter
    @JvmStatic
    fun toExpenseType(value: String?): ExpenseType? = value?.let { ExpenseType.fromName(it) }

    @TypeConverter
    @JvmStatic
    fun fromLocalDate(date: LocalDate?): Long? = date?.toEpochDay()

    @TypeConverter
    @JvmStatic
    fun toLocalDate(epochDay: Long?): LocalDate? = epochDay?.let { LocalDate.ofEpochDay(it) }

    @TypeConverter
    @JvmStatic
    fun fromQRType(value: QRType): String = value.name

    @TypeConverter
    @JvmStatic
    fun toQRType(value: String): QRType = QRType.valueOf(value)

}
