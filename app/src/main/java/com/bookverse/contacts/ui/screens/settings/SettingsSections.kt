package com.bookverse.contacts.ui.screens.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AddToHomeScreen
import androidx.compose.material.icons.automirrored.filled.NoteAdd
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AppBlocking
import androidx.compose.material.icons.filled.BlurOn
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.SecurityUpdateGood
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Transform
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.bookverse.contacts.R
import com.composables.icons.lucide.AArrowUp
import com.composables.icons.lucide.AlignJustify
import com.composables.icons.lucide.ArchiveRestore
import com.composables.icons.lucide.BadgeInfo
import com.composables.icons.lucide.Download
import com.composables.icons.lucide.FileInput
import com.composables.icons.lucide.FileOutput
import com.composables.icons.lucide.KeyRound
import com.composables.icons.lucide.Languages
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.PhoneForwarded
import com.composables.icons.lucide.SaveAll
import com.composables.icons.lucide.ShieldBan
import com.composables.icons.lucide.ShieldCheck
import com.composables.icons.lucide.Tags
import com.composables.icons.lucide.Upload
import com.composables.icons.lucide.UsersRound
import com.composables.icons.lucide.Vibrate
import com.composables.icons.lucide.Volume2

internal data class SettingsSectionsState(
    val hasContactsPermission: Boolean,
    val themeValue: String,
    val accentStyleValue: String,
    val accentColorValue: String,
    val fontValue: String,
    val fontScaleValue: String,
    val listDensityValue: String,
    val glassEffectsValue: String,
    val animationSpeedValue: String,
    val transitionValue: String,
    val vibrationDurationValue: String,
    val vibrationPatternValue: String,
    val defaultAccountValue: String,
    val blockedNumbersValue: String,
    val callAnswerStyleValue: String,
    val callSimVisible: Boolean,
    val callSimValue: String,
    val spamFilterValue: String,
    val speedDialValue: String,
    val startupScreenValue: String,
    val languageValue: String,
    val toolsMenuVisible: Boolean,
    val immersiveIncomingCallsEnabled: Boolean,
    val answerWithVolumeUpEnabled: Boolean,
    val dtmfTonesEnabled: Boolean,
    val passwordAuthEnabled: Boolean,
    val passwordManagerEnabled: Boolean,
    val sensitiveSectionsLockEnabled: Boolean,
)

internal data class SettingsSectionsActions(
    val onTheme: () -> Unit,
    val onAccentStyle: () -> Unit,
    val onAccentColor: () -> Unit,
    val onFont: () -> Unit,
    val onFontScale: () -> Unit,
    val onListDensity: () -> Unit,
    val onGlassEffects: () -> Unit,
    val onAnimationSpeed: () -> Unit,
    val onTransition: () -> Unit,
    val onVibrationDuration: () -> Unit,
    val onVibrationPattern: () -> Unit,
    val onToolsMenuVisibleChanged: (Boolean) -> Unit,
    val onCustomizeTools: () -> Unit,
    val onProfile: () -> Unit,
    val onDefaultAccount: () -> Unit,
    val onTagManager: () -> Unit,
    val onDuplicateContacts: () -> Unit,
    val onBlockedNumbers: () -> Unit,
    val onCallAnswerStyle: () -> Unit,
    val onCallSim: () -> Unit,
    val onSpamFilter: () -> Unit,
    val onSpeedDial: () -> Unit,
    val onPhoneStatus: () -> Unit,
    val onImmersiveIncomingCallsChanged: (Boolean) -> Unit,
    val onAnswerWithVolumeChanged: (Boolean) -> Unit,
    val onDtmfTonesChanged: (Boolean) -> Unit,
    val onHomeShortcuts: () -> Unit,
    val onFavoritesWidget: () -> Unit,
    val onNotesWidget: () -> Unit,
    val onPasswordAuthChanged: (Boolean) -> Unit,
    val onPasswordManagerChanged: (Boolean) -> Unit,
    val onSensitiveSectionsLockChanged: (Boolean) -> Unit,
    val onSaveContacts: () -> Unit,
    val onShareContacts: () -> Unit,
    val onRestoreContacts: () -> Unit,
    val onExportVCard: () -> Unit,
    val onImportVCard: () -> Unit,
    val onContactRecycleBin: () -> Unit,
    val onSaveSettings: () -> Unit,
    val onRestoreSettings: () -> Unit,
    val onProtectionBackup: () -> Unit,
    val onStartupScreen: () -> Unit,
    val onLanguage: () -> Unit,
    val onAbout: () -> Unit,
)

internal fun filterSettingsItems(
    sections: List<SettingsSectionModel>,
    query: String,
): List<SettingsItemModel> {
    val items = sections.flatMap(SettingsSectionModel::items)
    if (query.isBlank()) return items

    return items.filter { item ->
        item.title.contains(query, ignoreCase = true) ||
            item.description.contains(query, ignoreCase = true) ||
            item.currentValue?.contains(query, ignoreCase = true) == true
    }
}

@Composable
internal fun buildSettingsSections(
    state: SettingsSectionsState,
    actions: SettingsSectionsActions,
): List<SettingsSectionModel> = listOf(
    SettingsSectionModel(
        key = "appearance",
        title = stringResource(R.string.appearance),
        description = stringResource(R.string.appearance_description),
        items = listOf(
            SettingsItemModel("theme", Icons.Default.DarkMode, stringResource(R.string.theme), stringResource(R.string.theme_description), actions.onTheme, state.themeValue),
            SettingsItemModel("accent_style", Icons.Default.Tune, stringResource(R.string.accent_usage_title), stringResource(R.string.accent_usage_description), actions.onAccentStyle, state.accentStyleValue),
            SettingsItemModel("accent_color", Icons.Default.ColorLens, stringResource(R.string.accent_color), stringResource(R.string.accent_color_description), actions.onAccentColor, state.accentColorValue),
            SettingsItemModel("font", Icons.Default.FontDownload, stringResource(R.string.font_type), stringResource(R.string.font_type_description), actions.onFont, state.fontValue),
            SettingsItemModel("font_scale", Lucide.AArrowUp, stringResource(R.string.font_scale), stringResource(R.string.font_scale_description), actions.onFontScale, state.fontScaleValue),
            SettingsItemModel("list_density", Lucide.AlignJustify, stringResource(R.string.list_density), stringResource(R.string.list_density_description), actions.onListDensity, state.listDensityValue),
            SettingsItemModel("glass_effects", Icons.Default.BlurOn, stringResource(R.string.glass_effects), stringResource(R.string.glass_effects_description), actions.onGlassEffects, state.glassEffectsValue),
        ),
    ),
    SettingsSectionModel(
        key = "motion",
        title = stringResource(R.string.animations_vibration_section),
        description = stringResource(R.string.animations_vibration_section_description),
        items = listOf(
            SettingsItemModel("animation_speed", Icons.Default.Speed, stringResource(R.string.transition_speed), stringResource(R.string.transition_speed_description), actions.onAnimationSpeed, state.animationSpeedValue),
            SettingsItemModel("transition", Icons.Default.Transform, stringResource(R.string.transition_type), stringResource(R.string.transition_type_description), actions.onTransition, state.transitionValue),
            SettingsItemModel("vibration_duration", Icons.Default.Vibration, stringResource(R.string.vibration_duration), stringResource(R.string.vibration_duration_description), actions.onVibrationDuration, state.vibrationDurationValue),
            SettingsItemModel("vibration_pattern", Lucide.Vibrate, stringResource(R.string.vibration_pattern), stringResource(R.string.vibration_pattern_description), actions.onVibrationPattern, state.vibrationPatternValue),
        ),
    ),
    SettingsSectionModel(
        key = "tools",
        title = stringResource(R.string.tools_section_title),
        description = stringResource(R.string.tools_section_description),
        items = listOf(
            SettingsItemModel(
                key = "tools_visible",
                icon = Icons.Default.AppBlocking,
                title = stringResource(R.string.tools_menu_visible_title),
                description = stringResource(R.string.tools_menu_visible_description),
                onClick = {},
                toggle = SettingsToggleModel(state.toolsMenuVisible, actions.onToolsMenuVisibleChanged),
            ),
            SettingsItemModel("tools_customize", Icons.Default.Tune, stringResource(R.string.tools_customize_title), stringResource(R.string.tools_customize_description), actions.onCustomizeTools),
        ),
    ),
    SettingsSectionModel(
        key = "contacts",
        title = stringResource(R.string.contacts_accounts_section),
        description = stringResource(R.string.contacts_accounts_section_description),
        items = buildList {
            if (state.hasContactsPermission) {
                add(SettingsItemModel("profile", Icons.Outlined.PersonOutline, stringResource(R.string.profile_item), stringResource(R.string.profile_description), actions.onProfile))
            }
            add(SettingsItemModel("default_account", Icons.Default.ManageAccounts, stringResource(R.string.default_account_title), stringResource(R.string.default_account_description), actions.onDefaultAccount, state.defaultAccountValue))
            add(SettingsItemModel("tags", Lucide.Tags, stringResource(R.string.tag_manager_title), stringResource(R.string.tag_manager_description), actions.onTagManager))
            add(SettingsItemModel("duplicates", Lucide.UsersRound, stringResource(R.string.duplicate_contacts_title), stringResource(R.string.duplicate_contacts_description), actions.onDuplicateContacts))
            add(SettingsItemModel("contact_recycle_bin", Lucide.ArchiveRestore, stringResource(R.string.contact_recycle_bin_title), stringResource(R.string.contact_recycle_bin_description), actions.onContactRecycleBin))
            add(SettingsItemModel("blocked", Icons.Default.AppBlocking, stringResource(R.string.blocked_numbers_title), stringResource(R.string.blocked_numbers_description), actions.onBlockedNumbers, state.blockedNumbersValue))
        },
    ),
    SettingsSectionModel(
        key = "calls",
        title = stringResource(R.string.calls_section),
        description = stringResource(R.string.calls_section_description),
        items = buildList {
            add(SettingsItemModel("answer_style", Icons.Default.Call, stringResource(R.string.call_answer_style_title), stringResource(R.string.call_answer_style_description), actions.onCallAnswerStyle, state.callAnswerStyleValue))
            if (state.callSimVisible) {
                add(SettingsItemModel("call_sim", Icons.Default.SimCard, stringResource(R.string.call_sim_title), stringResource(R.string.call_sim_description), actions.onCallSim, state.callSimValue))
            }
            add(SettingsItemModel("spam_filter", Lucide.ShieldBan, stringResource(R.string.spam_filter_title), stringResource(R.string.spam_filter_description), actions.onSpamFilter, state.spamFilterValue))
            add(SettingsItemModel("speed_dial", Lucide.PhoneForwarded, stringResource(R.string.speed_dial_title), stringResource(R.string.speed_dial_description), actions.onSpeedDial, state.speedDialValue))
            add(SettingsItemModel("phone_status", Icons.Default.SecurityUpdateGood, stringResource(R.string.status_resume), stringResource(R.string.phone_status_description), actions.onPhoneStatus))
            add(SettingsItemModel("immersive_calls", Icons.Default.Fullscreen, stringResource(R.string.incoming_call_immersive_title), stringResource(R.string.incoming_call_immersive_description), {}, toggle = SettingsToggleModel(state.immersiveIncomingCallsEnabled, actions.onImmersiveIncomingCallsChanged)))
            add(SettingsItemModel("volume_answer", Icons.AutoMirrored.Filled.VolumeUp, stringResource(R.string.answer_with_volume_title), stringResource(R.string.answer_with_volume_description), {}, toggle = SettingsToggleModel(state.answerWithVolumeUpEnabled, actions.onAnswerWithVolumeChanged)))
            add(SettingsItemModel("dtmf", Lucide.Volume2, stringResource(R.string.dtmf_tones_title), stringResource(R.string.dtmf_tones_description), {}, toggle = SettingsToggleModel(state.dtmfTonesEnabled, actions.onDtmfTonesChanged)))
        },
    ),
    SettingsSectionModel(
        key = "home",
        title = stringResource(R.string.home_shortcuts_section_title),
        description = stringResource(R.string.home_shortcuts_section_description),
        items = listOf(
            SettingsItemModel("shortcuts", Icons.AutoMirrored.Filled.AddToHomeScreen, stringResource(R.string.home_shortcuts_card_title), stringResource(R.string.home_shortcuts_card_description), actions.onHomeShortcuts),
            SettingsItemModel("favorites_widget", Icons.Default.Widgets, stringResource(R.string.home_widget_card_title), stringResource(R.string.home_widget_card_description), actions.onFavoritesWidget),
            SettingsItemModel("notes_widget", Icons.AutoMirrored.Filled.NoteAdd, stringResource(R.string.notes_widget_card_title), stringResource(R.string.notes_widget_card_description), actions.onNotesWidget),
        ),
    ),
    SettingsSectionModel(
        key = "security",
        title = stringResource(R.string.security_settings),
        description = stringResource(R.string.security_settings_description),
        items = listOf(
            SettingsItemModel("biometrics", Icons.Default.Fingerprint, stringResource(R.string.security_biometrics), stringResource(R.string.biometrics_protection), {}, toggle = SettingsToggleModel(state.passwordAuthEnabled, actions.onPasswordAuthChanged)),
            SettingsItemModel("passwords", Lucide.KeyRound, stringResource(R.string.passwords_item), stringResource(R.string.password_manager_switch), {}, toggle = SettingsToggleModel(state.passwordManagerEnabled, actions.onPasswordManagerChanged)),
            SettingsItemModel("sensitive_lock", Lucide.ShieldCheck, stringResource(R.string.expenses_lock_title), stringResource(R.string.expenses_lock_description), {}, toggle = SettingsToggleModel(state.sensitiveSectionsLockEnabled, actions.onSensitiveSectionsLockChanged)),
        ),
    ),
    SettingsSectionModel(
        key = "backup",
        title = stringResource(R.string.backup_section),
        description = stringResource(R.string.backup_section_description),
        items = listOf(
            SettingsItemModel("save_contacts", Lucide.SaveAll, stringResource(R.string.extras_save_contacts), stringResource(R.string.save_contacts_description), actions.onSaveContacts),
            SettingsItemModel("share_contacts", Icons.Default.Share, stringResource(R.string.extras_share_contacts), stringResource(R.string.share_contacts_description), actions.onShareContacts),
            SettingsItemModel("restore_contacts", Icons.Default.SettingsBackupRestore, stringResource(R.string.extras_restore_contacts), stringResource(R.string.restore_contacts_description), actions.onRestoreContacts),
            SettingsItemModel("export_vcard", Lucide.FileOutput, stringResource(R.string.vcard_export_title), stringResource(R.string.vcard_export_description), actions.onExportVCard),
            SettingsItemModel("import_vcard", Lucide.FileInput, stringResource(R.string.vcard_import_title), stringResource(R.string.vcard_import_description), actions.onImportVCard),
            SettingsItemModel("save_settings", Lucide.Download, stringResource(R.string.save_settings_title), stringResource(R.string.save_settings_description), actions.onSaveSettings),
            SettingsItemModel("restore_settings", Lucide.Upload, stringResource(R.string.restore_settings_title), stringResource(R.string.restore_settings_description), actions.onRestoreSettings),
            SettingsItemModel("protection_backup", Lucide.ShieldCheck, stringResource(R.string.protection_backup_title), stringResource(R.string.protection_backup_description), actions.onProtectionBackup),
        ),
    ),
    SettingsSectionModel(
        key = "general",
        title = stringResource(R.string.general_section),
        description = stringResource(R.string.general_section_description),
        items = listOf(
            SettingsItemModel("startup", Icons.Default.Home, stringResource(R.string.startup_screen_title), stringResource(R.string.startup_screen_description), actions.onStartupScreen, state.startupScreenValue),
            SettingsItemModel("language", Lucide.Languages, stringResource(R.string.app_language_title), stringResource(R.string.app_language_description), actions.onLanguage, state.languageValue),
            SettingsItemModel("about", Lucide.BadgeInfo, stringResource(R.string.about_title_item), stringResource(R.string.about_description), actions.onAbout),
        ),
    ),
)
