package org.umn.utsmap.ui.screens

import android.content.res.Configuration
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.utsmap.R
import org.umn.utsmap.ui.theme.*

@Composable
fun SplashScreen(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val light = colors.background == Color.White
    Box(modifier.fillMaxSize().background(Brush.linearGradient(listOf(if (light) Color(0xFFEFF9F3) else colors.surface, colors.background)))) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * .5f, size.height * .277f)
            drawCircle(Brush.radialGradient(0f to BudgetGreen.copy(alpha = .12f), .7f to Color.Transparent, center = center, radius = 150.dp.toPx()), 150.dp.toPx(), center)
            val lower = Offset(size.width * .643f, size.height * .731f)
            drawCircle(Brush.radialGradient(0f to Color(0x140099CC), .7f to Color.Transparent, center = lower, radius = 100.dp.toPx()), 100.dp.toPx(), lower)
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxWidth().padding(top = maxHeight * .38f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(104.dp).border(2.dp, BudgetGreen.copy(alpha = .3f), RoundedCornerShape(36.dp)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(88.dp).figmaCardShadow(BudgetGreen, radius = 28f)
                        .background(BrandGradient, RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) { BrandMark(Modifier.size(48.dp), large = true) }
                }
                Spacer(Modifier.height(16.dp))
                Text(buildAnnotatedString {
                    append("Budget")
                    withStyle(SpanStyle(color = BudgetGreen)) { append("Lens") }
                }, fontFamily = Outfit, fontWeight = FontWeight.ExtraBold, fontSize = 36.sp,
                    letterSpacing = (-.5).sp, lineHeight = 45.sp, color = colors.onBackground)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.brand_tagline), fontSize = 13.sp, fontWeight = FontWeight.Medium,
                    letterSpacing = .78.sp, color = Color(0xFF94A3B8))
            }
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = maxHeight * .105f).offset(x = (-6).dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.size(6.dp).background(BudgetGreen, CircleShape))
                Box(Modifier.size(20.dp, 6.dp).background(Color(0x6694A3B8), CircleShape))
                Box(Modifier.size(6.dp).background(Color(0x6694A3B8), CircleShape))
            }
        }
    }
}

@Preview(widthDp = 402, heightDp = 874)
@Preview(widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SplashPreview() { BudgetLensTheme { SplashScreen() } }
