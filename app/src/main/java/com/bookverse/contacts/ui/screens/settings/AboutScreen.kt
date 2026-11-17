package com.bookverse.contacts.ui.screens.settings

import android.content.Intent
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import com.bookverse.contacts.ui.components.AppScreenScaffold as Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bookverse.contacts.R
import com.bookverse.contacts.data.model.SocialMediaType
import com.bookverse.contacts.ui.components.AppSnackbarHost
import com.bookverse.contacts.ui.components.FeatureItem
import com.bookverse.contacts.ui.components.FeaturesGroup
import com.bookverse.contacts.ui.components.SecondaryTopBar
import com.bookverse.contacts.ui.components.SnackbarType
import com.bookverse.contacts.ui.components.rememberAppSnackbarState
import com.bookverse.contacts.ui.viewmodel.AppSettingsViewModel
import com.bookverse.contacts.utils.ContactUtils
import com.composables.icons.lucide.Accessibility
import com.composables.icons.lucide.CheckCheck
import com.composables.icons.lucide.ContactRound
import com.composables.icons.lucide.Copy
import com.composables.icons.lucide.History
import com.composables.icons.lucide.Import
import com.composables.icons.lucide.LayoutGrid
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Merge
import com.composables.icons.lucide.Palette
import com.composables.icons.lucide.PhoneCall
import com.composables.icons.lucide.PhoneIncoming
import com.composables.icons.lucide.Search
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    onBack: () -> Unit,
    appSettingsViewModel: AppSettingsViewModel
) {
    val accentColor = MaterialTheme.colorScheme.primary
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current
    val resources = LocalResources.current
    val scope = rememberCoroutineScope()

    var tapCount by remember { mutableIntStateOf(0) }
    var lastTapTime by remember { mutableLongStateOf(0L) }
    var confettiKey by remember { mutableIntStateOf(0) }
    val scaleAnimation = remember { Animatable(1f) }

    val version = try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "N/A"
    } catch (_: Exception) {
        "N/A"
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
    val scrollBehavior = com.bookverse.contacts.ui.components.rememberAppBarScrollBehavior()
    val snackbar = rememberAppSnackbarState()

    BackHandler { onBack() }

    fun shareApp() {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, resources.getString(R.string.app_name))
            putExtra(Intent.EXTRA_TEXT, resources.getString(R.string.about_share_message))
        }
        context.startActivity(
            Intent.createChooser(
                shareIntent,
                resources.getString(R.string.about_share_title)
            )
        )
    }

    fun openWebsite() {
        ContactUtils.openUrl(
            context,
            "https://moncada25.github.io/fun-phone-web/"
        )
    }

    fun openFeedback() {
        ContactUtils.openUrl(
            context,
            "https://play.google.com/store/apps/details?id=com.bookverse.contacts"
        )
    }

    fun copyVersion() {
        ContactUtils.copyToClipboard(
            context,
            resources.getString(R.string.about_version, version),
            toast = false
        )
        scope.launch {
            snackbar.show(
                message = resources.getString(R.string.about_version_copied),
                type = SnackbarType.Info
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
                    withDismissAction = false
                )
            }
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
                scrollBehavior = scrollBehavior
            )
        },
        snackbarHost = { AppSnackbarHost(snackbar.hostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding() + 24.dp,
                    bottom = 24.dp,
                    start = 24.dp,
                    end = 24.dp
                )
            ) {
                item {
                    AboutHeroCard(
                        accentColor = accentColor,
                        version = version,
                        scale = scaleAnimation.value,
                        onNameTapped = { handleNameTap() },
                        onCopyVersion = { copyVersion() }
                    )
                }

                item {
                    AboutFeaturesCard(
                        features = features,
                        accentColor = accentColor
                    )
                }

                item {
                    AboutCommunityCard(
                        accentColor = accentColor,
                        onShare = { shareApp() },
                        onVisitWebsite = { openWebsite() },
                        onFeedback = { openFeedback() }
                    )
                }

                item {
                    AboutDeveloperCard(
                        accentColor = accentColor,
                        onGithubClick = {
                            ContactUtils.openUrl(
                                context,
                                "https://github.com/Moncada25"
                            )
                        },
                        onLinkedInClick = {
                            ContactUtils.openUrl(
                                context,
                                "https://www.linkedin.com/in/santiagomoncadavelez/"
                            )
                        }
                    )
                }

                item {
                    AboutFooterCard(accentColor = accentColor)
                }
            }

            AnimatedVisibility(visible = confettiKey > 0) {
                KonfettiView(
                    modifier = Modifier.fillMaxSize(),
                    parties = listOf(
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
                                0xFFF44336.toInt()
                            ),
                            timeToLive = 3000L,
                            fadeOutEnabled = true,
                            position = Position.Relative(0.5, 0.1),
                            delay = 0,
                            rotation = Rotation.enabled(),
                            emitter = Emitter(duration = 2, TimeUnit.SECONDS).max(200)
                        ),
                        Party(
                            angle = 225,
                            spread = 30,
                            speed = 40f,
                            maxSpeed = 70f,
                            damping = 0.9f,
                            size = listOf(Size.SMALL, Size.MEDIUM),
                            colors = listOf(
                                0xFFFFD700.toInt(),
                                0xFFFFA500.toInt(),
                                0xFFFF69B4.toInt(),
                                0xFF32CD32.toInt(),
                                0xFF00BFFF.toInt(),
                                0xFFFF1493.toInt()
                            ),
                            timeToLive = 2500L,
                            fadeOutEnabled = true,
                            position = Position.Relative(0.2, 0.0),
                            delay = 300,
                            rotation = Rotation.enabled(),
                            emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).max(100)
                        ),
                        Party(
                            angle = 315,
                            spread = 30,
                            speed = 40f,
                            maxSpeed = 70f,
                            damping = 0.9f,
                            size = listOf(Size.SMALL, Size.MEDIUM),
                            colors = listOf(
                                0xFFFFD700.toInt(),
                                0xFFFFA500.toInt(),
                                0xFFFF69B4.toInt(),
                                0xFF32CD32.toInt(),
                                0xFF00BFFF.toInt(),
                                0xFFFF1493.toInt()
                            ),
                            timeToLive = 2500L,
                            fadeOutEnabled = true,
                            position = Position.Relative(0.8, 0.0),
                            delay = 300,
                            rotation = Rotation.enabled(),
                            emitter = Emitter(duration = 1500, TimeUnit.MILLISECONDS).max(100)
                        )
                    )
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

@Composable
private fun AboutHeroCard(
    accentColor: Color,
    version: String,
    scale: Float,
    onNameTapped: () -> Unit,
    onCopyVersion: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
    ) {
        Box(
            modifier = Modifier
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.35f),
                            accentColor.copy(alpha = 0.08f),
                            colorScheme.surface
                        )
                    )
                )
                .padding(horizontal = 28.dp, vertical = 32.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    tonalElevation = 12.dp,
                    shadowElevation = 12.dp,
                    color = Color.White.copy(alpha = 0.95f)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = stringResource(R.string.about_app_icon_description),
                            modifier = Modifier.size(96.dp)
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .scale(scale)
                        .clickable(
                            onClick = onNameTapped,
                            role = Role.Button
                        )
                )

                Text(
                    text = stringResource(R.string.onboarding_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                AssistChip(
                    onClick = onCopyVersion,
                    label = {
                        Text(
                            text = stringResource(R.string.about_version, version),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium)
                        )
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = accentColor.copy(alpha = 0.15f),
                        labelColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun AboutSectionSurface(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.(AboutSurfacePalette) -> Unit
) {
    val palette = aboutSurfacePalette(accentColor)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = palette.containerColor,
        contentColor = palette.contentColor,
        tonalElevation = 0.dp,
        border = BorderStroke(1.dp, palette.outlineColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            content(palette)
        }
    }
}

@Composable
private fun AboutSectionHeader(
    title: String,
    description: String,
    palette: AboutSurfacePalette
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = palette.accentColor,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = palette.supportingColor
        )
    }
}

@Composable
private fun AboutDeveloperCard(
    accentColor: Color,
    onGithubClick: () -> Unit,
    onLinkedInClick: () -> Unit
) {
    AboutSectionSurface(
        accentColor = accentColor
    ) { palette ->
        AboutSectionHeader(
            title = stringResource(R.string.about_developer_title),
            description = stringResource(R.string.about_developer_contact),
            palette = palette
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutActionRow(
                iconPainterRes = SocialMediaType.GITHUB.icon,
                label = SocialMediaType.GITHUB.socialMediaName,
                palette = palette,
                leadingContainerColor = Color.Black,
                leadingContentColor = Color.White,
                onClick = onGithubClick
            )
            AboutActionRow(
                iconPainterRes = SocialMediaType.LINKEDIN.icon,
                label = SocialMediaType.LINKEDIN.socialMediaName,
                palette = palette,
                leadingContainerColor = SocialMediaType.LINKEDIN.color,
                leadingContentColor = contentColorFor(SocialMediaType.LINKEDIN.color),
                onClick = onLinkedInClick
            )
        }
    }
}

@Composable
private fun AboutFeaturesCard(
    features: List<FeatureItem>,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.about_features_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = accentColor,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = stringResource(R.string.about_features_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }

            FeaturesGroup(
                features = features,
            )
        }
    }
}

@Composable
private fun AboutCommunityCard(
    accentColor: Color,
    onShare: () -> Unit,
    onVisitWebsite: () -> Unit,
    onFeedback: () -> Unit
) {
    AboutSectionSurface(
        accentColor = accentColor
    ) { palette ->
        AboutSectionHeader(
            title = stringResource(R.string.about_actions_title),
            description = stringResource(R.string.about_actions_description),
            palette = palette
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            AboutActionRow(
                icon = Icons.Outlined.Share,
                label = stringResource(R.string.share),
                palette = palette,
                onClick = onShare
            )
            AboutActionRow(
                icon = Icons.Filled.Public,
                label = stringResource(R.string.website),
                palette = palette,
                onClick = onVisitWebsite
            )
            AboutActionRow(
                icon = Icons.Outlined.Feedback,
                label = stringResource(R.string.about_feedback),
                palette = palette,
                onClick = onFeedback
            )
        }
    }
}

@Composable
private fun AboutActionRow(
    label: String,
    palette: AboutSurfacePalette,
    onClick: () -> Unit,
    icon: ImageVector? = null,
    iconPainterRes: Int? = null,
    leadingContainerColor: Color = palette.iconContainerColor,
    leadingContentColor: Color = palette.iconContentColor
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(
                role = Role.Button,
                onClick = onClick
            ),
        shape = RoundedCornerShape(16.dp),
        color = palette.rowContainerColor,
        contentColor = palette.contentColor,
        border = BorderStroke(1.dp, palette.outlineColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = leadingContainerColor,
                contentColor = leadingContentColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    when {
                        icon != null -> {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        iconPainterRes != null -> {
                            Icon(
                                painter = painterResource(id = iconPainterRes),
                                contentDescription = null,
                                tint = leadingContentColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }

            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                color = palette.contentColor,
                modifier = Modifier.weight(1f)
            )

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = palette.supportingColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun AboutFooterCard(
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    tint = accentColor
                )
                Text(
                    text = stringResource(R.string.about_footer_title),
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Medium),
                    color = colorScheme.onSurface
                )
            }

            Text(
                text = stringResource(R.string.about_footer_message),
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Text(
                text = stringResource(R.string.about_copyright),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                color = accentColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

private data class AboutSurfacePalette(
    val containerColor: Color,
    val contentColor: Color,
    val supportingColor: Color,
    val accentColor: Color,
    val outlineColor: Color,
    val rowContainerColor: Color,
    val iconContainerColor: Color,
    val iconContentColor: Color
)

@Composable
private fun aboutSurfacePalette(
    accentColor: Color
): AboutSurfacePalette {
    val colorScheme = MaterialTheme.colorScheme

    return AboutSurfacePalette(
        containerColor = colorScheme.surface,
        contentColor = colorScheme.onSurface,
        supportingColor = colorScheme.onSurface.copy(alpha = 0.78f),
        accentColor = accentColor,
        outlineColor = colorScheme.onSurface.copy(alpha = 0.14f),
        rowContainerColor = colorScheme.surface,
        iconContainerColor = accentColor.copy(alpha = 0.12f),
        iconContentColor = accentColor
    )
}
