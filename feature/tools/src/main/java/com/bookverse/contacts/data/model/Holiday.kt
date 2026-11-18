package com.bookverse.contacts.data.model

import com.google.gson.annotations.SerializedName

data class PublicHoliday(
    @SerializedName("date")
    val date: String,
    @SerializedName("localName")
    val localName: String,
    @SerializedName("name")
    val name: String,
    @SerializedName("countryCode")
    val countryCode: String,
    @SerializedName("fixed")
    val fixed: Boolean,
    @SerializedName("global")
    val global: Boolean,
    @SerializedName("counties")
    val counties: List<String>?,
    @SerializedName("launchYear")
    val launchYear: Int?,
    @SerializedName("types")
    val types: List<String>
)
