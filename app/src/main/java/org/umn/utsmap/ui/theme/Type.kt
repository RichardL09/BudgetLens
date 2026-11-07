package org.umn.utsmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.ExperimentalTextApi
import org.umn.utsmap.R
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalTextApi::class)
private fun variableFamily(resource: Int) = FontFamily(
    listOf(400, 500, 600, 700, 800).map { weight ->
        Font(resource, weight = FontWeight(weight), variationSettings = FontVariation.Settings(FontVariation.weight(weight)))
    }
)
val Outfit = variableFamily(R.font.outfit)
val Inter = variableFamily(R.font.inter)
val JetBrainsMono = variableFamily(R.font.jetbrains_mono)

private fun budgetText(size: Int, height: Int, weight: FontWeight = FontWeight.Normal, family: FontFamily = Inter) =
    TextStyle(fontFamily = family, fontWeight = weight,
        fontSize = size.sp, lineHeight = height.sp, letterSpacing = 0.sp)

val BudgetLensTypography = Typography(
    displayLarge = budgetText(52, 60, FontWeight.ExtraBold),
    displayMedium = budgetText(44, 52, FontWeight.ExtraBold),
    displaySmall = budgetText(36, 44, FontWeight.ExtraBold),
    headlineLarge = budgetText(32, 40, FontWeight.ExtraBold, Outfit),
    headlineMedium = budgetText(28, 36, FontWeight.ExtraBold, Outfit),
    headlineSmall = budgetText(26, 32, FontWeight.ExtraBold, Outfit),
    titleLarge = budgetText(22, 28, FontWeight.ExtraBold, Outfit),
    titleMedium = budgetText(16, 24, FontWeight.Bold, Outfit),
    titleSmall = budgetText(14, 20, FontWeight.SemiBold),
    bodyLarge = budgetText(16, 24),
    bodyMedium = budgetText(14, 24),
    bodySmall = budgetText(12, 18),
    labelLarge = budgetText(15, 20, FontWeight.Bold, Outfit),
    labelMedium = budgetText(12, 16, FontWeight.Medium),
    labelSmall = budgetText(10, 16, FontWeight.Medium),
)
