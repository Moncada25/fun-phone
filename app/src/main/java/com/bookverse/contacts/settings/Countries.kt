package com.bookverse.contacts.settings

import android.content.Context
import android.telephony.TelephonyManager
import java.util.Locale

class Countries {
    companion object {
        fun getCountryList(context: Context? = null): List<Country> {
            val selectedLocale = context?.let(LocaleController::effectiveLocale) ?: Locale.ENGLISH

            return COUNTRY_ENTRIES.map { entry ->
                val locale = Locale.Builder().setRegion(entry.iso).build()
                val localizedName =
                    locale.getDisplayCountry(selectedLocale).takeIf { it.isNotBlank() }

                Country(
                    name = localizedName ?: entry.defaultName,
                    code = entry.dialCode,
                    flag = entry.iso.toFlagEmoji(),
                    countryCode = entry.iso
                )
            }
        }

        val popularCountryCodes: List<String>
            get() = POPULAR_COUNTRY_CODES

        fun getDefaultCountry(
            context: Context? = null
        ): Country? {
            val countries = getCountryList(context)
            val signals = mutableListOf<CountrySignal>()

            if (context != null) {
                val telephonyManager =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
                val simIso = telephonyManager.safeCountryIso { simCountryIso }
                val networkIso = telephonyManager.safeCountryIso { networkCountryIso }
                val deviceIso = resolveDeviceIso(context)

                simIso?.let { signals += CountrySignal(CountrySignal.Source.USER, it) }
                networkIso?.let { signals += CountrySignal(CountrySignal.Source.NETWORK, it) }
                deviceIso?.let { signals += CountrySignal(CountrySignal.Source.DEVICE, it) }
            }

            (context?.let(LocaleController::effectiveLocale) ?: Locale.getDefault()).country
                .takeIf { it.isNotBlank() }
                ?.let { signals += CountrySignal(CountrySignal.Source.FALLBACK, it) }

            val normalizedSignals = signals.mapNotNull { signal ->
                signal.iso.normalizeIso()?.let { normalized ->
                    signal.copy(iso = normalized)
                }
            }

            val consensus = normalizedSignals
                .groupingBy { it.iso }
                .eachCount()
                .maxByOrNull { it.value }
                ?.key

            val resolvedIso = consensus ?: normalizedSignals.minByOrNull { it.source.priority }?.iso

            return resolvedIso?.let { iso ->
                countries.firstOrNull { it.countryCode.equals(iso, ignoreCase = true) }
            } ?: countries.firstOrNull()
        }

        fun removeCountryCodeFromPhoneNumber(phoneNumber: String, country: Country): String {
            val cleanNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
            return if (cleanNumber.startsWith(country.code)) {
                cleanNumber.removePrefix(country.code)
            } else {
                phoneNumber
            }
        }

        fun detectCountryFromPhoneNumber(phoneNumber: String): Country? {
            val cleanNumber = phoneNumber.replace(Regex("[^+\\d]"), "")
            if (!cleanNumber.startsWith("+")) return null

            val countries = getCountryList()
            return countries
                .sortedByDescending { it.code.length }
                .firstOrNull { cleanNumber.startsWith(it.code) }
        }

        private fun TelephonyManager?.safeCountryIso(reader: TelephonyManager.() -> String?): String? {
            if (this == null) return null
            return try {
                reader()?.takeIf { it.isNotBlank() }
            } catch (_: SecurityException) {
                null
            } catch (_: Exception) {
                null
            }
        }

        private fun resolveDeviceIso(context: Context): String? {
            val configuration = context.resources.configuration
            val locale = configuration.locales.takeIf { it.size() > 0 }?.get(0)
            return locale?.country
        }

        private fun String?.normalizeIso(): String? {
            return this
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.uppercase(Locale.US)
        }

        private fun String.toFlagEmoji(): String {
            val normalized = this
                .trim()
                .takeIf { it.length == 2 }
                ?.uppercase(Locale.US)
                ?: return "🏳️"

            val base = 0x1F1E6
            val offset = 'A'.code

            return buildString(2) {
                normalized.forEach { char ->
                    val codePoint = base + (char.code - offset)
                    append(String(Character.toChars(codePoint)))
                }
            }
        }

        private val SPANISH_LOCALE: Locale = Locale.forLanguageTag("es")
        private val ENGLISH_LOCALE: Locale = Locale.ENGLISH

        private data class CountryEntry(
            val iso: String,
            val dialCode: String,
            val defaultName: String
        )

        private val POPULAR_COUNTRY_CODES = listOf(
            "CO",
            "MX",
            "AR",
            "US",
            "ES",
            "BR",
            "CL",
            "PE",
            "VE",
            "EC",
            "FR",
            "IT",
            "DE",
            "GB",
            "CA",
            "UY",
            "BO",
            "PY",
            "GT",
            "PR"
        )

        private val COUNTRY_ENTRIES = listOf(
            CountryEntry("AF", "+93", "Afghanistan"),
            CountryEntry("AX", "+358", "Aland Islands"),
            CountryEntry("AL", "+355", "Albania"),
            CountryEntry("DZ", "+213", "Algeria"),
            CountryEntry("AS", "+1684", "American Samoa"),
            CountryEntry("AD", "+376", "Andorra"),
            CountryEntry("AO", "+244", "Angola"),
            CountryEntry("AI", "+1264", "Anguilla"),
            CountryEntry("AQ", "+672", "Antarctica"),
            CountryEntry("AG", "+1268", "Antigua and Barbuda"),
            CountryEntry("AR", "+54", "Argentina"),
            CountryEntry("AM", "+374", "Armenia"),
            CountryEntry("AW", "+297", "Aruba"),
            CountryEntry("AU", "+61", "Australia"),
            CountryEntry("AT", "+43", "Austria"),
            CountryEntry("AZ", "+994", "Azerbaijan"),
            CountryEntry("BS", "+1242", "Bahamas"),
            CountryEntry("BH", "+973", "Bahrain"),
            CountryEntry("BD", "+880", "Bangladesh"),
            CountryEntry("BB", "+1246", "Barbados"),
            CountryEntry("BY", "+375", "Belarus"),
            CountryEntry("BE", "+32", "Belgium"),
            CountryEntry("BZ", "+501", "Belize"),
            CountryEntry("BJ", "+229", "Benin"),
            CountryEntry("BM", "+1441", "Bermuda"),
            CountryEntry("BT", "+975", "Bhutan"),
            CountryEntry("BO", "+591", "Bolivia"),
            CountryEntry("BQ", "+599", "Bonaire, Sint Eustatius and Saba"),
            CountryEntry("BA", "+387", "Bosnia and Herzegovina"),
            CountryEntry("BW", "+267", "Botswana"),
            CountryEntry("BR", "+55", "Brazil"),
            CountryEntry("IO", "+246", "British Indian Ocean Territory"),
            CountryEntry("VG", "+1284", "British Virgin Islands"),
            CountryEntry("BN", "+673", "Brunei"),
            CountryEntry("BG", "+359", "Bulgaria"),
            CountryEntry("BF", "+226", "Burkina Faso"),
            CountryEntry("BI", "+257", "Burundi"),
            CountryEntry("KH", "+855", "Cambodia"),
            CountryEntry("CM", "+237", "Cameroon"),
            CountryEntry("CA", "+1", "Canada"),
            CountryEntry("CV", "+238", "Cape Verde"),
            CountryEntry("KY", "+1345", "Cayman Islands"),
            CountryEntry("CF", "+236", "Central African Republic"),
            CountryEntry("TD", "+235", "Chad"),
            CountryEntry("CL", "+56", "Chile"),
            CountryEntry("CN", "+86", "China"),
            CountryEntry("CX", "+61", "Christmas Island"),
            CountryEntry("CC", "+61", "Cocos (Keeling) Islands"),
            CountryEntry("CO", "+57", "Colombia"),
            CountryEntry("KM", "+269", "Comoros"),
            CountryEntry("CG", "+242", "Congo"),
            CountryEntry("CD", "+243", "Congo, Democratic Republic of the"),
            CountryEntry("CK", "+682", "Cook Islands"),
            CountryEntry("CR", "+506", "Costa Rica"),
            CountryEntry("CI", "+225", "Cote d'Ivoire"),
            CountryEntry("HR", "+385", "Croatia"),
            CountryEntry("CU", "+53", "Cuba"),
            CountryEntry("CW", "+599", "Curacao"),
            CountryEntry("CY", "+357", "Cyprus"),
            CountryEntry("CZ", "+420", "Czechia"),
            CountryEntry("DK", "+45", "Denmark"),
            CountryEntry("DJ", "+253", "Djibouti"),
            CountryEntry("DM", "+1767", "Dominica"),
            CountryEntry("DO", "+1", "Dominican Republic"),
            CountryEntry("EC", "+593", "Ecuador"),
            CountryEntry("EG", "+20", "Egypt"),
            CountryEntry("SV", "+503", "El Salvador"),
            CountryEntry("GQ", "+240", "Equatorial Guinea"),
            CountryEntry("ER", "+291", "Eritrea"),
            CountryEntry("EE", "+372", "Estonia"),
            CountryEntry("SZ", "+268", "Eswatini"),
            CountryEntry("ET", "+251", "Ethiopia"),
            CountryEntry("FK", "+500", "Falkland Islands"),
            CountryEntry("FO", "+298", "Faroe Islands"),
            CountryEntry("FJ", "+679", "Fiji"),
            CountryEntry("FI", "+358", "Finland"),
            CountryEntry("FR", "+33", "France"),
            CountryEntry("GF", "+594", "French Guiana"),
            CountryEntry("PF", "+689", "French Polynesia"),
            CountryEntry("TF", "+262", "French Southern Territories"),
            CountryEntry("GA", "+241", "Gabon"),
            CountryEntry("GM", "+220", "Gambia"),
            CountryEntry("GE", "+995", "Georgia"),
            CountryEntry("DE", "+49", "Germany"),
            CountryEntry("GH", "+233", "Ghana"),
            CountryEntry("GI", "+350", "Gibraltar"),
            CountryEntry("GR", "+30", "Greece"),
            CountryEntry("GL", "+299", "Greenland"),
            CountryEntry("GD", "+1473", "Grenada"),
            CountryEntry("GP", "+590", "Guadeloupe"),
            CountryEntry("GU", "+1671", "Guam"),
            CountryEntry("GT", "+502", "Guatemala"),
            CountryEntry("GG", "+44", "Guernsey"),
            CountryEntry("GN", "+224", "Guinea"),
            CountryEntry("GW", "+245", "Guinea-Bissau"),
            CountryEntry("GY", "+592", "Guyana"),
            CountryEntry("HT", "+509", "Haiti"),
            CountryEntry("HM", "+672", "Heard and McDonald Islands"),
            CountryEntry("HN", "+504", "Honduras"),
            CountryEntry("HK", "+852", "Hong Kong"),
            CountryEntry("HU", "+36", "Hungary"),
            CountryEntry("IS", "+354", "Iceland"),
            CountryEntry("IN", "+91", "India"),
            CountryEntry("ID", "+62", "Indonesia"),
            CountryEntry("IR", "+98", "Iran"),
            CountryEntry("IQ", "+964", "Iraq"),
            CountryEntry("IE", "+353", "Ireland"),
            CountryEntry("IM", "+44", "Isle of Man"),
            CountryEntry("IL", "+972", "Israel"),
            CountryEntry("IT", "+39", "Italy"),
            CountryEntry("JM", "+1876", "Jamaica"),
            CountryEntry("JP", "+81", "Japan"),
            CountryEntry("JE", "+44", "Jersey"),
            CountryEntry("JO", "+962", "Jordan"),
            CountryEntry("KZ", "+7", "Kazakhstan"),
            CountryEntry("KE", "+254", "Kenya"),
            CountryEntry("KI", "+686", "Kiribati"),
            CountryEntry("KP", "+850", "Korea, North"),
            CountryEntry("KR", "+82", "Korea, South"),
            CountryEntry("KW", "+965", "Kuwait"),
            CountryEntry("KG", "+996", "Kyrgyzstan"),
            CountryEntry("LA", "+856", "Laos"),
            CountryEntry("LV", "+371", "Latvia"),
            CountryEntry("LB", "+961", "Lebanon"),
            CountryEntry("LS", "+266", "Lesotho"),
            CountryEntry("LR", "+231", "Liberia"),
            CountryEntry("LY", "+218", "Libya"),
            CountryEntry("LI", "+423", "Liechtenstein"),
            CountryEntry("LT", "+370", "Lithuania"),
            CountryEntry("LU", "+352", "Luxembourg"),
            CountryEntry("MO", "+853", "Macau"),
            CountryEntry("MG", "+261", "Madagascar"),
            CountryEntry("MW", "+265", "Malawi"),
            CountryEntry("MY", "+60", "Malaysia"),
            CountryEntry("MV", "+960", "Maldives"),
            CountryEntry("ML", "+223", "Mali"),
            CountryEntry("MT", "+356", "Malta"),
            CountryEntry("MH", "+692", "Marshall Islands"),
            CountryEntry("MQ", "+596", "Martinique"),
            CountryEntry("MR", "+222", "Mauritania"),
            CountryEntry("MU", "+230", "Mauritius"),
            CountryEntry("YT", "+262", "Mayotte"),
            CountryEntry("MX", "+52", "Mexico"),
            CountryEntry("FM", "+691", "Micronesia"),
            CountryEntry("MD", "+373", "Moldova"),
            CountryEntry("MC", "+377", "Monaco"),
            CountryEntry("MN", "+976", "Mongolia"),
            CountryEntry("ME", "+382", "Montenegro"),
            CountryEntry("MS", "+1664", "Montserrat"),
            CountryEntry("MA", "+212", "Morocco"),
            CountryEntry("MZ", "+258", "Mozambique"),
            CountryEntry("MM", "+95", "Myanmar"),
            CountryEntry("NA", "+264", "Namibia"),
            CountryEntry("NR", "+674", "Nauru"),
            CountryEntry("NP", "+977", "Nepal"),
            CountryEntry("NL", "+31", "Netherlands"),
            CountryEntry("NC", "+687", "New Caledonia"),
            CountryEntry("NZ", "+64", "New Zealand"),
            CountryEntry("NI", "+505", "Nicaragua"),
            CountryEntry("NE", "+227", "Niger"),
            CountryEntry("NG", "+234", "Nigeria"),
            CountryEntry("NU", "+683", "Niue"),
            CountryEntry("NF", "+672", "Norfolk Island"),
            CountryEntry("MK", "+389", "North Macedonia"),
            CountryEntry("MP", "+1670", "Northern Mariana Islands"),
            CountryEntry("NO", "+47", "Norway"),
            CountryEntry("OM", "+968", "Oman"),
            CountryEntry("PK", "+92", "Pakistan"),
            CountryEntry("PW", "+680", "Palau"),
            CountryEntry("PS", "+970", "Palestine"),
            CountryEntry("PA", "+507", "Panama"),
            CountryEntry("PG", "+675", "Papua New Guinea"),
            CountryEntry("PY", "+595", "Paraguay"),
            CountryEntry("PE", "+51", "Peru"),
            CountryEntry("PH", "+63", "Philippines"),
            CountryEntry("PN", "+870", "Pitcairn Islands"),
            CountryEntry("PL", "+48", "Poland"),
            CountryEntry("PT", "+351", "Portugal"),
            CountryEntry("PR", "+1", "Puerto Rico"),
            CountryEntry("QA", "+974", "Qatar"),
            CountryEntry("RE", "+262", "Reunion"),
            CountryEntry("RO", "+40", "Romania"),
            CountryEntry("RU", "+7", "Russia"),
            CountryEntry("RW", "+250", "Rwanda"),
            CountryEntry("BL", "+590", "Saint Barthelemy"),
            CountryEntry("SH", "+290", "Saint Helena"),
            CountryEntry("KN", "+1869", "Saint Kitts and Nevis"),
            CountryEntry("LC", "+1758", "Saint Lucia"),
            CountryEntry("MF", "+590", "Saint Martin"),
            CountryEntry("PM", "+508", "Saint Pierre and Miquelon"),
            CountryEntry("VC", "+1784", "Saint Vincent and the Grenadines"),
            CountryEntry("WS", "+685", "Samoa"),
            CountryEntry("SM", "+378", "San Marino"),
            CountryEntry("ST", "+239", "Sao Tome and Principe"),
            CountryEntry("SA", "+966", "Saudi Arabia"),
            CountryEntry("SN", "+221", "Senegal"),
            CountryEntry("RS", "+381", "Serbia"),
            CountryEntry("SC", "+248", "Seychelles"),
            CountryEntry("SL", "+232", "Sierra Leone"),
            CountryEntry("SG", "+65", "Singapore"),
            CountryEntry("SX", "+1721", "Sint Maarten"),
            CountryEntry("SK", "+421", "Slovakia"),
            CountryEntry("SI", "+386", "Slovenia"),
            CountryEntry("SB", "+677", "Solomon Islands"),
            CountryEntry("SO", "+252", "Somalia"),
            CountryEntry("ZA", "+27", "South Africa"),
            CountryEntry("GS", "+500", "South Georgia and the South Sandwich Islands"),
            CountryEntry("SS", "+211", "South Sudan"),
            CountryEntry("ES", "+34", "Spain"),
            CountryEntry("LK", "+94", "Sri Lanka"),
            CountryEntry("SD", "+249", "Sudan"),
            CountryEntry("SR", "+597", "Suriname"),
            CountryEntry("SJ", "+47", "Svalbard and Jan Mayen"),
            CountryEntry("SE", "+46", "Sweden"),
            CountryEntry("CH", "+41", "Switzerland"),
            CountryEntry("SY", "+963", "Syria"),
            CountryEntry("TW", "+886", "Taiwan"),
            CountryEntry("TJ", "+992", "Tajikistan"),
            CountryEntry("TZ", "+255", "Tanzania"),
            CountryEntry("TH", "+66", "Thailand"),
            CountryEntry("TL", "+670", "Timor-Leste"),
            CountryEntry("TG", "+228", "Togo"),
            CountryEntry("TK", "+690", "Tokelau"),
            CountryEntry("TO", "+676", "Tonga"),
            CountryEntry("TT", "+1868", "Trinidad and Tobago"),
            CountryEntry("TN", "+216", "Tunisia"),
            CountryEntry("TR", "+90", "Turkey"),
            CountryEntry("TM", "+993", "Turkmenistan"),
            CountryEntry("TC", "+1649", "Turks and Caicos Islands"),
            CountryEntry("TV", "+688", "Tuvalu"),
            CountryEntry("UG", "+256", "Uganda"),
            CountryEntry("UA", "+380", "Ukraine"),
            CountryEntry("AE", "+971", "United Arab Emirates"),
            CountryEntry("GB", "+44", "United Kingdom"),
            CountryEntry("US", "+1", "United States"),
            CountryEntry("UM", "+1", "United States Minor Outlying Islands"),
            CountryEntry("UY", "+598", "Uruguay"),
            CountryEntry("UZ", "+998", "Uzbekistan"),
            CountryEntry("VU", "+678", "Vanuatu"),
            CountryEntry("VA", "+379", "Vatican City"),
            CountryEntry("VE", "+58", "Venezuela"),
            CountryEntry("VN", "+84", "Vietnam"),
            CountryEntry("WF", "+681", "Wallis and Futuna"),
            CountryEntry("EH", "+212", "Western Sahara"),
            CountryEntry("YE", "+967", "Yemen"),
            CountryEntry("ZM", "+260", "Zambia"),
            CountryEntry("ZW", "+263", "Zimbabwe"),
            CountryEntry("XK", "+383", "Kosovo")
        )
    }
}

private data class CountrySignal(
    val source: Source,
    val iso: String
) {
    enum class Source(val priority: Int) {
        USER(0),
        NETWORK(1),
        DEVICE(2),
        FALLBACK(3)
    }
}

data class Country(val name: String, val code: String, val flag: String, val countryCode: String)
