package com.bookverse.contacts.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.bookverse.contacts.ui.viewmodel.MenstrualPhaseType

data class MenstrualPhaseColors(
    val container: Color,
    val content: Color,
    val indicator: Color,
)

@Composable
fun menstrualPhaseColors(type: MenstrualPhaseType): MenstrualPhaseColors {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    return if (isLightTheme) {
        when (type) {
            MenstrualPhaseType.MENSTRUATION -> MenstrualPhaseColors(
                container = Color(0xFFFBE1E7),
                content = Color(0xFF571827),
                indicator = Color(0xFFB2254A),
            )
            MenstrualPhaseType.FOLLICULAR -> MenstrualPhaseColors(
                container = Color(0xFFDDF3E5),
                content = Color(0xFF123B27),
                indicator = Color(0xFF2E7D4F),
            )
            MenstrualPhaseType.OVULATION -> MenstrualPhaseColors(
                container = Color(0xFFFFEEC7),
                content = Color(0xFF4A3100),
                indicator = Color(0xFF976900),
            )
            MenstrualPhaseType.LUTEAL -> MenstrualPhaseColors(
                container = Color(0xFFEDE3FB),
                content = Color(0xFF38205B),
                indicator = Color(0xFF7046A1),
            )
        }
    } else {
        when (type) {
            MenstrualPhaseType.MENSTRUATION -> MenstrualPhaseColors(
                container = Color(0xFF542334),
                content = Color(0xFFFFD9E1),
                indicator = Color(0xFFFFB0C1),
            )
            MenstrualPhaseType.FOLLICULAR -> MenstrualPhaseColors(
                container = Color(0xFF1D4932),
                content = Color(0xFFC2F8D4),
                indicator = Color(0xFF8BE5AA),
            )
            MenstrualPhaseType.OVULATION -> MenstrualPhaseColors(
                container = Color(0xFF513D12),
                content = Color(0xFFFFE7A1),
                indicator = Color(0xFFFFD768),
            )
            MenstrualPhaseType.LUTEAL -> MenstrualPhaseColors(
                container = Color(0xFF412B61),
                content = Color(0xFFE9D9FF),
                indicator = Color(0xFFD5B7FF),
            )
        }
    }
}
