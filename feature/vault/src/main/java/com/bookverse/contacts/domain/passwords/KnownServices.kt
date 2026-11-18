package com.bookverse.contacts.domain.passwords

import androidx.compose.runtime.Immutable
import java.text.Normalizer
import java.util.Locale

enum class KnownServiceCategory { STREAMING, EMAIL_SOCIAL, SHOPPING, GAMING_DEV }

@Immutable
data class KnownService(
    val id: String,
    val name: String,
    val url: String,
    val category: KnownServiceCategory,
    val domains: List<String>,
    val packages: List<String> = emptyList(),
    val aliases: List<String> = emptyList(),
) {
    val host: String get() = CredentialMatchPolicy.hostOf(url).orEmpty()
}

object KnownServices {
    val all: List<KnownService> = listOf(
        KnownService("netflix", "Netflix", "https://www.netflix.com", KnownServiceCategory.STREAMING, listOf("netflix.com"), listOf("com.netflix.mediaclient")),
        KnownService("disney_plus", "Disney+", "https://www.disneyplus.com", KnownServiceCategory.STREAMING, listOf("disneyplus.com"), listOf("com.disney.disneyplus"), listOf("disney")),
        KnownService("prime_video", "Prime Video", "https://www.primevideo.com", KnownServiceCategory.STREAMING, listOf("primevideo.com", "amazon.com"), listOf("com.amazon.avod.thirdpartyclient"), listOf("amazon")),
        KnownService("max", "HBO Max", "https://www.hbomax.com", KnownServiceCategory.STREAMING, listOf("hbomax.com", "max.com"), listOf("com.wbd.stream"), listOf("max", "hbo")),
        KnownService("spotify", "Spotify", "https://accounts.spotify.com", KnownServiceCategory.STREAMING, listOf("spotify.com"), listOf("com.spotify.music")),
        KnownService("youtube", "YouTube", "https://www.youtube.com", KnownServiceCategory.STREAMING, listOf("youtube.com", "google.com"), listOf("com.google.android.youtube")),
        KnownService("apple_tv", "Apple TV+", "https://tv.apple.com", KnownServiceCategory.STREAMING, listOf("tv.apple.com", "apple.com")),
        KnownService("paramount_plus", "Paramount+", "https://www.paramountplus.com", KnownServiceCategory.STREAMING, listOf("paramountplus.com"), listOf("com.cbs.app")),
        KnownService("crunchyroll", "Crunchyroll", "https://www.crunchyroll.com", KnownServiceCategory.STREAMING, listOf("crunchyroll.com"), listOf("com.crunchyroll.crunchyroid")),
        KnownService("twitch", "Twitch", "https://www.twitch.tv", KnownServiceCategory.STREAMING, listOf("twitch.tv"), listOf("tv.twitch.android.app")),
        KnownService("google", "Google", "https://accounts.google.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("google.com"), aliases = listOf("gmail")),
        KnownService("microsoft", "Microsoft", "https://login.live.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("live.com", "microsoft.com", "microsoftonline.com", "outlook.com"), listOf("com.microsoft.office.outlook"), listOf("outlook", "hotmail")),
        KnownService("apple_id", "Apple ID", "https://account.apple.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("apple.com", "icloud.com"), aliases = listOf("apple", "icloud")),
        KnownService("facebook", "Facebook", "https://www.facebook.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("facebook.com"), listOf("com.facebook.katana", "com.facebook.lite"), listOf("meta")),
        KnownService("instagram", "Instagram", "https://www.instagram.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("instagram.com"), listOf("com.instagram.android")),
        KnownService("x", "X", "https://x.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("x.com", "twitter.com"), listOf("com.twitter.android"), listOf("twitter")),
        KnownService("tiktok", "TikTok", "https://www.tiktok.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("tiktok.com"), listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")),
        KnownService("whatsapp", "WhatsApp", "https://web.whatsapp.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("whatsapp.com"), listOf("com.whatsapp")),
        KnownService("linkedin", "LinkedIn", "https://www.linkedin.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("linkedin.com"), listOf("com.linkedin.android")),
        KnownService("reddit", "Reddit", "https://www.reddit.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("reddit.com"), listOf("com.reddit.frontpage")),
        KnownService("discord", "Discord", "https://discord.com", KnownServiceCategory.EMAIL_SOCIAL, listOf("discord.com"), listOf("com.discord")),
        KnownService("amazon", "Amazon", "https://www.amazon.com", KnownServiceCategory.SHOPPING, listOf("amazon.com"), listOf("com.amazon.mShop.android.shopping")),
        KnownService("mercado_libre", "Mercado Libre", "https://www.mercadolibre.com", KnownServiceCategory.SHOPPING, listOf("mercadolibre.com"), listOf("com.mercadolibre"), listOf("mercadolibre")),
        KnownService("paypal", "PayPal", "https://www.paypal.com", KnownServiceCategory.SHOPPING, listOf("paypal.com"), listOf("com.paypal.android.p2pmobile")),
        KnownService("dropbox", "Dropbox", "https://www.dropbox.com", KnownServiceCategory.GAMING_DEV, listOf("dropbox.com"), listOf("com.dropbox.android")),
        KnownService("github", "GitHub", "https://github.com", KnownServiceCategory.GAMING_DEV, listOf("github.com"), listOf("com.github.android")),
        KnownService("steam", "Steam", "https://store.steampowered.com", KnownServiceCategory.GAMING_DEV, listOf("steampowered.com", "steamcommunity.com"), listOf("com.valvesoftware.android.steam.community")),
        KnownService("playstation", "PlayStation", "https://www.playstation.com", KnownServiceCategory.GAMING_DEV, listOf("playstation.com", "account.sony.com"), listOf("com.scee.psxandroid"), listOf("psn", "sony")),
        KnownService("xbox", "Xbox", "https://www.xbox.com", KnownServiceCategory.GAMING_DEV, listOf("xbox.com", "live.com"), listOf("com.microsoft.xboxone.smartglass")),
        KnownService("nintendo", "Nintendo", "https://accounts.nintendo.com", KnownServiceCategory.GAMING_DEV, listOf("nintendo.com")),
        KnownService("epic_games", "Epic Games", "https://www.epicgames.com", KnownServiceCategory.GAMING_DEV, listOf("epicgames.com"), aliases = listOf("fortnite")),
    )

    fun forHost(host: String?): KnownService? {
        val value = host?.lowercase(Locale.ROOT)?.takeIf { it.isNotEmpty() } ?: return null
        return all.mapNotNull { service ->
            service.domains
                .withIndex()
                .filter { (_, domain) -> value == domain || value.endsWith(".$domain") }
                .maxWithOrNull(compareBy<IndexedValue<String>> { it.value.length }.thenByDescending { it.index })
                ?.let { match -> Triple(service, match.value.length, match.index == 0) }
        }.maxWithOrNull(compareBy<Triple<KnownService, Int, Boolean>> { it.second }.thenBy { it.third })?.first
    }

    fun forTarget(target: CredentialTarget): KnownService? =
        CredentialMatchPolicy.hostOf(target.webDomain)?.let(::forHost)
            ?: all.firstOrNull { target.webDomain == null && target.packageName in it.packages }

    fun coversHost(service: KnownService, host: String): Boolean =
        service.domains.any { domain -> host == domain || host.endsWith(".$domain") }

    fun search(query: String): List<KnownService> {
        val needle = normalize(query)
        if (needle.isEmpty()) return all
        return all.filter { service ->
            normalize(service.name).contains(needle) ||
                service.host.contains(needle) ||
                service.aliases.any { normalize(it).contains(needle) }
        }
    }

    private fun normalize(value: String): String =
        Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
            .replace(Regex("\\p{Mn}+"), "")
            .lowercase(Locale.ROOT)
}
