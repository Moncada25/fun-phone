package com.bookverse.contacts.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.BuildConfig
import com.bookverse.contacts.core.ui.R
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.FeatureItem
import com.bookverse.contacts.ui.components.FeaturesGroup
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.composables.icons.lucide.Accessibility
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Ban
import com.composables.icons.lucide.Bug
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.CodeXml
import com.composables.icons.lucide.ContactRound
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.EyeOff
import com.composables.icons.lucide.GitPullRequest
import com.composables.icons.lucide.Globe
import com.composables.icons.lucide.Heart
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Import
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Merge
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.PhoneCall
import com.composables.icons.lucide.PhoneIncoming
import com.composables.icons.lucide.Rocket
import com.composables.icons.lucide.Scale
import com.composables.icons.lucide.Search
import com.composables.icons.lucide.Share2
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Star
import com.composables.icons.lucide.Wrench
import com.composables.icons.lucide.Zap
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Angle
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.Rotation
import nl.dionsegijn.konfetti.core.emitter.Emitter
import nl.dionsegijn.konfetti.core.models.Size
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.milliseconds

private object AboutLinks {
    const val REPOSITORY = "https://github.com/Moncada25/fun-phone"
    const val ISSUES = "$REPOSITORY/issues/new/choose"
    const val RELEASES = "$REPOSITORY/releases"
    const val CONTRIBUTING = "$REPOSITORY/blob/main/CONTRIBUTING.md"
    const val LICENSE = "$REPOSITORY/blob/main/LICENSE"
    const val WEBSITE = "https://moncada25.github.io/fun-phone-web/"
    const val PLAY_STORE = "https://play.google.com/store/apps/details?id=com.bookverse.contacts"
    const val DEVELOPER_GITHUB = "https://github.com/Moncada25"
    const val DEVELOPER_LINKEDIN = "https://www.linkedin.com/in/santiagomoncadavelez/"
}

private const val PLAY_DISTRIBUTION = "play"

private val SEMANTIC_VERSION = Regex("""^\d+(\.\d+)*""")

internal fun displayVersion(versionName: String): String =
    SEMANTIC_VERSION.find(versionName)?.value ?: versionName

@Immutable
private data class AboutLinkItem(
    val key: String,
    val title: String,
    val summary: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

@Immutable
private data class AboutBadge(
    val label: String,
    val icon: ImageVector,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel,
) {
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var confettiKey by remember { mutableIntStateOf(0) }
    val scaleAnimation = remember { Animatable(1f) }

    val version = remember(context) {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "N/A"
        } catch (_: Exception) {
            "N/A"
        }
    }

    val features = listOf(
        FeatureItem(stringResource(R.string.about_feature_dialer), Lucide.PhoneCall),
        FeatureItem(stringResource(R.string.about_feature_t9_search), Lucide.Search),
        FeatureItem(stringResource(R.string.about_feature_call_controls), Lucide.PhoneIncoming),
        FeatureItem(stringResource(R.string.about_feature_call_history), Lucide.History),
        FeatureItem(stringResource(R.string.about_feature_contacts), Lucide.ContactRound),
        FeatureItem(stringResource(R.string.about_feature_favorites), Lucide.Star),
        FeatureItem(stringResource(R.string.about_feature_dual_sim), Lucide.Copy),
        FeatureItem(stringResource(R.string.about_feature_default_phone), Lucide.CheckCheck),
        FeatureItem(stringResource(R.string.about_feature_local_privacy), Lucide.ShieldCheck),
        FeatureItem(stringResource(R.string.about_feature_import_export), Lucide.Import),
        FeatureItem(stringResource(R.string.about_feature_duplicates), Lucide.Merge),
        FeatureItem(stringResource(R.string.about_feature_accessibility), Lucide.Accessibility),
        FeatureItem(stringResource(R.string.about_feature_customization), Lucide.Palette),
        FeatureItem(stringResource(R.string.about_feature_shortcuts), Lucide.Zap),
        FeatureItem(stringResource(R.string.about_feature_widgets), Lucide.LayoutGrid),
        FeatureItem(stringResource(R.string.about_feature_optional_tools), Lucide.Wrench),
    )
    val scrollBehavior = rememberAppBarScrollBehavior()
    val snackbar = rememberAppSnackbarState()

    BackHandler { onBack() }

    fun open(url: String) {
        ContactUtils.openUrl(context, url)
    }

    fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.about_share_message))
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                resources.getString(R.string.about_share_title),
            ),
        )
    }

    fun copyVersion() {
        ContactUtils.copyToClipboard(
            context,
            resources.getString(R.string.about_version, version),
            toast = false,
        )
        scope.launch {
            snackbar.show(
                message = resources.getString(R.string.about_version_copied),
                type = SnackbarType.Info,
            )
        }
    }

    fun handleNameTap() {
        val now = System.currentTimeMillis()
        tapCount = if (now - lastTapTime > 1000) 1 else tapCount + 1
        lastTapTime = now

        scope.launch {
            scaleAnimation.animateTo(0.94f, tween(100))
            scaleAnimation.animateTo(1f, tween(100))
        }

        if (tapCount >= 5) {
            confettiKey++
            tapCount = 0
            scope.launch {
                snackbar.show(
                    message = resources.getString(R.string.easter_egg_toast),
                    type = SnackbarType.Success,
                    withDismissAction = false,
                )
            }
        }
    }

    val badges = listOf(
        AboutBadge(stringResource(R.string.about_badge_no_ads), Lucide.Ban),
        AboutBadge(stringResource(R.string.about_badge_no_trackers), Lucide.EyeOff),
    )

    val openSourceLinks = listOf(
        AboutLinkItem(
            key = "source",
            title = stringResource(R.string.about_source_code),
            summary = stringResource(R.string.about_repository_path),
            icon = Lucide.CodeXml,
            onClick = { open(AboutLinks.REPOSITORY) },
        ),
        AboutLinkItem(
            key = "issues",
            title = stringResource(R.string.about_report_issue),
            summary = stringResource(R.string.about_report_issue_summary),
            icon = Lucide.Bug,
            onClick = { open(AboutLinks.ISSUES) },
        ),
        AboutLinkItem(
            key = "releases",
            title = stringResource(R.string.about_releases),
            summary = stringResource(R.string.about_releases_summary),
            icon = Lucide.Rocket,
            onClick = { open(AboutLinks.RELEASES) },
        ),
        AboutLinkItem(
            key = "contribute",
            title = stringResource(R.string.about_contribute),
            summary = stringResource(R.string.about_contribute_summary),
            icon = Lucide.GitPullRequest,
            onClick = { open(AboutLinks.CONTRIBUTING) },
        ),
        AboutLinkItem(
            key = "license",
            title = stringResource(R.string.about_license),
            summary = stringResource(R.string.about_license_summary),
            icon = Lucide.Scale,
            onClick = { open(AboutLinks.LICENSE) },
        ),
    )

    val supportLinks = buildList {
        add(
            AboutLinkItem(
                key = "share",
                title = stringResource(R.string.share),
                summary = stringResource(R.string.about_share_summary),
                icon = Lucide.Share2,
                onClick = { shareApp() },
            ),
        )
        add(
            AboutLinkItem(
                key = "website",
                title = stringResource(R.string.website),
                summary = stringResource(R.string.about_website_host),
                icon = Lucide.Globe,
                onClick = { open(AboutLinks.WEBSITE) },
            ),
        )
        if (BuildConfig.DISTRIBUTION == PLAY_DISTRIBUTION) {
            add(
                AboutLinkItem(
                    key = "rate",
                    title = stringResource(R.string.about_rate_play),
                    summary = stringResource(R.string.about_rate_play_summary),
                    icon = Lucide.Star,
                    onClick = { open(AboutLinks.PLAY_STORE) },
                ),
            )
        }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.background,
        topBar = {
            SecondaryTopBar(
                expandable = true,
                title = stringResource(R.string.about_app_title),
                onBackClick = onBack,
                appSettingsViewModel = appSettingsViewModel,
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background),
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(28.dp),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 16.dp,
                    bottom = 32.dp,
                    start = 20.dp,
                    end = 20.dp,
                ),
            ) {
                item(key = "hero") {
                    AboutHero(
                        version = version,
                        badges = badges,
                        scale = scaleAnimation.value,
                        onNameTapped = { handleNameTap() },
                        onCopyVersion = { copyVersion() },
                    )
                }

                item(key = "open_source") {
                    AboutSection(
                        title = stringResource(R.string.about_open_source_title),
                        description = stringResource(R.string.about_open_source_description),
                    ) {
                        AboutLinkGroup(items = openSourceLinks)
                        FilledTonalButton(
                            onClick = { open(AboutLinks.REPOSITORY) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp),
                            shape = RoundedCornerShape(16.dp),
                        ) {
                            Icon(
                                imageVector = Lucide.Star,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                text = stringResource(R.string.about_star_repository),
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(start = 10.dp),
                            )
                        }
                    }
                }

                item(key = "features") {
                    AboutSection(
                        title = stringResource(R.string.about_features_title),
                        description = stringResource(R.string.about_features_description),
                    ) {
                        AboutCard {
                            FeaturesGroup(
                                features = features,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 20.dp),
                            )
                        }
                    }
                }

                item(key = "support") {
                    AboutSection(
                        title = stringResource(R.string.about_support_title),
                        description = stringResource(R.string.about_support_description),
                    ) {
                        AboutLinkGroup(items = supportLinks)
                    }
                }

                item(key = "developer") {
                    AboutSection(
                        title = stringResource(R.string.about_developer_title),
                        description = stringResource(R.string.about_developer_description),
                    ) {
                        AboutDeveloperCard(
                            onGithubClick = { open(AboutLinks.DEVELOPER_GITHUB) },
                            onLinkedInClick = { open(AboutLinks.DEVELOPER_LINKEDIN) },
                        )
                    }
                }

                item(key = "footer") {
                    AboutFooter()
                }
            }

            AnimatedVisibility(visible = confettiKey > 0) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = aboutConfettiParties(),
                )
            }

            LaunchedEffect(confettiKey) {
                if (confettiKey > 0) {
                    delay(4000.milliseconds)
                    confettiKey = 0
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AboutHero(
    version: String,
    badges: List<AboutBadge>,
    scale: Float,
    onNameTapped: () -> Unit,
    onCopyVersion: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        color = colorScheme.surface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.22f),
                            colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 32.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Surface(
                    modifier = Modifier.size(104.dp),
                    shape = RoundedCornerShape(30.dp),
                    shadowElevation = 10.dp,
                    color = Color.White,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = stringResource(R.string.about_app_icon_description),
                            modifier = Modifier.size(84.dp),
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.25).sp,
                    ),
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .scale(scale)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onNameTapped, role = Role.Button)
                        .padding(horizontal = 12.dp, vertical = 2.dp),
                )

                Text(
                    text = stringResource(R.string.onboarding_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                )

                Surface(
                    onClick = onCopyVersion,
                    modifier = Modifier.padding(top = 18.dp),
                    shape = CircleShape,
                    color = colorScheme.primary.copy(alpha = 0.12f),
                    contentColor = colorScheme.onSurface,
                ) {
                    Row(
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.about_version_short, displayVersion(version)),
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Icon(
                            imageVector = Lucide.Copy,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    badges.forEach { badge ->
                        AboutBadgePill(badge = badge)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutBadgePill(badge: AboutBadge) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = CircleShape,
        color = colorScheme.surface.copy(alpha = 0.7f),
        contentColor = colorScheme.onSurface,
        border = BorderStroke(1.dp, colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = badge.icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(14.dp),
            )
            Text(
                text = badge.label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun AboutSection(
    title: String,
    description: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        content()
    }
}

@Composable
private fun AboutCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            content()
        }
    }
}

@Composable
private fun AboutLinkGroup(items: List<AboutLinkItem>) {
    AboutCard {
        items.forEachIndexed { index, item ->
            AboutLinkRow(item = item)
            if (index != items.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier.padding(start = 72.dp, end = 16.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun AboutLinkRow(item: AboutLinkItem) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 72.dp)
            .clickable(role = Role.Button, onClick = item.onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(20.dp),
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
            Text(
                text = item.summary,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Icon(
            imageVector = Lucide.ArrowUpRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun AboutDeveloperCard(
    onGithubClick: () -> Unit,
    onLinkedInClick: () -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val name = stringResource(R.string.about_developer_name)
    val initials = remember(name) {
        name.split(" ")
            .filter { it.isNotBlank() }
            .take(2)
            .joinToString("") { it.first().uppercase() }
    }

    AboutCard {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(colorScheme.primary, colorScheme.tertiary),
                            ),
                            shape = CircleShape,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = colorScheme.onPrimary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface,
                    )
                    Text(
                        text = stringResource(R.string.about_developer_role),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AboutSocialButton(
                    label = SocialMediaType.GITHUB.socialMediaName,
                    iconRes = SocialMediaType.GITHUB.icon,
                    iconTint = colorScheme.onSurface,
                    onClick = onGithubClick,
                    modifier = Modifier.weight(1f),
                )
                AboutSocialButton(
                    label = SocialMediaType.LINKEDIN.socialMediaName,
                    iconRes = SocialMediaType.LINKEDIN.icon,
                    iconTint = SocialMediaType.LINKEDIN.color,
                    onClick = onLinkedInClick,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun AboutSocialButton(
    label: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun AboutFooter() {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Lucide.Heart,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = stringResource(R.string.about_footer_title),
                style = MaterialTheme.typography.titleSmall,
                color = colorScheme.onSurface,
            )
        }
        Text(
            text = stringResource(R.string.about_footer_message),
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(
                R.string.about_footer_legal,
                stringResource(R.string.about_copyright),
                stringResource(R.string.about_license_short),
            ),
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

private fun aboutConfettiParties(): List<Party> {
    val accentColors = listOf(
        0xFFFFD700.toInt(),
        0xFFFFA500.toInt(),
        0xFFFF69B4.toInt(),
        0xFF32CD32.toInt(),
        0xFF00BFFF.toInt(),
        0xFFFF1493.toInt(),
    )
    return listOf(
        Party(
            angle = Angle.TOP,
            spread = 60,
            speed = 50f,
            maxSpeed = 80f,
            damping = 0.85f,
            size = listOf(Size.SMALL, Size.MEDIUM, Size.LARGE),
            colors = listOf(
                0xFFE91E63.toInt(),
                0xFF9C27B0.toInt(),
                0xFF673AB7.toInt(),
                0xFF3F51B5.toInt(),
                0xFF2196F3.toInt(),
                0xFF00BCD4.toInt(),
                0xFF009688.toInt(),
                0xFF4CAF50.toInt(),
                0xFF8BC34A.toInt(),
                0xFFCDDC39.toInt(),
                0xFFFFEB3B.toInt(),
                0xFFFFC107.toInt(),
                0xFFFF9800.toInt(),
                0xFFFF5722.toInt(),
                0xFFF44336.toInt(),
            ),
            timeToLive = 3000L,
            fadeOutEnabled = true,
            position = Position.Relative(0.5, 0.1),
            delay = 0,
            rotation = Rotation.enabled(),
            emitter = Emitter(duration = 2, TimeUnit.SECONDS).max(200),
        ),
        Party(
            angle = 225,
            spread = 30,
            speed = 40f,
            maxSpeed = 70f,
            damping = 0.9f,
            size = listOf(Size.SMALL, Size.MEDIUM),
            colors = accentColors,
            timeToLive = 2500L,
            fadeOutEnabled = true,
            position = Position.Relative(0.2, 0.0),
            delay = 300,
            rotation = Rotation.enabled(),
            emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).max(100),
        ),
        Party(
            angle = 315,
            spread = 30,
            speed = 40f,
            maxSpeed = 70f,
            damping = 0.9f,
            size = listOf(Size.SMALL, Size.MEDIUM),
            colors = accentColors,
            timeToLive = 2500L,
            fadeOutEnabled = true,
            position = Position.Relative(0.8, 0.0),
            delay = 300,
            rotation = Rotation.enabled(),
            emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).max(100),
        ),
    )
}
