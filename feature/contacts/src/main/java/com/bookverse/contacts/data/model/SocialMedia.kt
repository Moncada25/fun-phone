package com.bookverse.contacts.data.model

import com.bookverse.contacts.domain.contacts.SocialLinkPolicy
import android.os.Parcelable
import androidx.compose.ui.graphics.Color
import com.bookverse.contacts.core.ui.R
import kotlinx.parcelize.Parcelize

@Parcelize
data class SocialMedia(
    val type: SocialMediaType,
    val username: String,
    val url: String
) : Parcelable

enum class SocialMediaType(
    val socialMediaName: String,
    val domain: String,
    val color: Color,
    val icon: Int
) {
    FACEBOOK(
        "Facebook",
        "facebook.com",
        Color(0xFF1877F2),
        R.drawable.ic_facebook
    ),
    INSTAGRAM(
        "Instagram",
        "instagram.com",
        Color(0xFFE4405F),
        R.drawable.ic_instagram
    ),
    GITHUB(
        "GitHub",
        "github.com",
        Color(0xFF181717),
        R.drawable.ic_github
    ),
    LINKEDIN(
        "LinkedIn",
        "linkedin.com",
        Color(0xFF0077B5),
        R.drawable.ic_linkedin
    );

    companion object {
        const val WEBSITE_ICON = -1

        fun getSocialMediaIcon(url: String): Int {
            return values().firstOrNull { type ->
                SocialLinkPolicy.isDomain(url, type.domain)
            }?.icon ?: WEBSITE_ICON
        }

        fun getSocialMediaName(url: String, unknownLabel: String): String {
            return values().firstOrNull { type ->
                SocialLinkPolicy.isDomain(url, type.domain)
            }?.socialMediaName ?: unknownLabel
        }

        fun getSocialMediaColor(url: String, accentColor: Color): Color {
            return values().firstOrNull { type ->
                SocialLinkPolicy.isDomain(url, type.domain)
            }?.color ?: accentColor
        }
    }
}
