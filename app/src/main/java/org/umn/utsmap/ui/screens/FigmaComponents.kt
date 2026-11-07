package org.umn.utsmap.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.error as semanticError
import org.umn.utsmap.R
import org.umn.utsmap.ui.theme.Outfit

internal val BrandGradient = Brush.linearGradient(listOf(Color(0xFF16A34A), Color(0xFF22C55E)))

/** Render soft CSS-like rounded shadows in software so emulator and device show the same glow. */
internal fun Modifier.figmaCardShadow(accent: Color, radius: Float = 40f) = drawWithCache {
    val padding = 90.dp.toPx()
    val bitmap = android.graphics.Bitmap.createBitmap((size.width + padding * 2).toInt().coerceAtLeast(1),
        (size.height + padding * 2).toInt().coerceAtLeast(1), android.graphics.Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    fun shadow(color: Color, blur: Float, offset: Float) {
        val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color.toArgb()
            maskFilter = android.graphics.BlurMaskFilter(blur.dp.toPx(), android.graphics.BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawRoundRect(padding, padding + offset.dp.toPx(), padding + size.width, padding + size.height + offset.dp.toPx(),
            radius.dp.toPx(), radius.dp.toPx(), paint)
    }
    shadow(Color.Black.copy(alpha = .12f), 30f, 20f)
    shadow(accent.copy(alpha = .13f), 20f, 0f)
    val image = bitmap.asImageBitmap()
    onDrawWithContent {
        drawImage(image, topLeft = Offset(-padding, -padding))
        drawContent()
    }
}

@Composable
internal fun FigmaInput(value: String, onChange: (String) -> Unit, label: String, hint: String = "",
    error: String? = null, supporting: String? = null, readOnly: Boolean = false,
    minLines: Int = 1, maxLines: Int = 1, keyboard: KeyboardType = KeyboardType.Text,
    trailing: (@Composable () -> Unit)? = null) {
    BasicTextField(value, onChange, readOnly = readOnly, singleLine = maxLines == 1, minLines = minLines, maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard), cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        modifier = Modifier.fillMaxWidth().semantics { if (error != null) semanticError(error) },
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .border(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
                        inner()
                    }
                    trailing?.invoke()
                }
                (error ?: supporting)?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant) }
            }
        })
}

@Composable
internal fun GradientButton(onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Button(onClick, enabled = enabled, shape = shape,
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, disabledContainerColor = Color.Transparent),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        modifier = modifier.heightIn(min = 51.dp)
            .shadow(12.dp, shape, ambientColor = Color(0x4016A34A), spotColor = Color(0x4016A34A))
            .background(BrandGradient, shape), content = content)
}

@Composable
internal fun FigmaImage(@DrawableRes resource: Int, modifier: Modifier = Modifier, description: String? = null) {
    Image(painterResource(resource), description, modifier, contentScale = ContentScale.Fit, colorFilter = if (MaterialTheme.colorScheme.background.luminance() < .5f && resource in darkModeWhiteIcons) ColorFilter.tint(Color.White) else null)
}

/** Original exported vector layers retain their Figma geometry inside the 72 dp illustration slot. */
@Composable
internal fun IntroArtwork(page: Int) {
    Box(Modifier.size(72.dp)) {
        when (page) {
            0 -> {
                Layer(R.drawable.figma_99_2910_imgvector, 7f, 15f, 58f, 42f)
                Layer(R.drawable.figma_99_2910_imgvector1, 16f, 24f, 20f, 3f)
                Layer(R.drawable.figma_99_2910_imgvector2, 16f, 31f, 14f, 2f)
                Layer(R.drawable.figma_99_2910_imgvector3, 39.25f, 21.25f, 17.5f, 21.5f)
                TinyText("$", 44.5f, 25f, 11, Color(0xFFF4B942))
                Layer(R.drawable.figma_99_2910_imgvector4, 44f, 38f, 20f, 20f)
                Layer(R.drawable.figma_99_2910_imgvector5, 49f, 44f, 11f, 8f)
            }
            1 -> {
                Layer(R.drawable.figma_107_2911_imgvector, 11.25f, 19.25f, 21.5f, 33.5f)
                Layer(R.drawable.figma_107_2911_imgvector1, 17f, 25f, 14f, 12f)
                Layer(R.drawable.figma_107_2911_imgvector2, 9.25f, 17.25f, 53.5f, 9.5f)
                Layer(R.drawable.figma_107_2911_imgvector3, 46f, 24f, 3f, 28f)
                Layer(R.drawable.figma_107_2911_imgvector4, 52f, 30f, 3f, 22f)
                Layer(R.drawable.figma_107_2911_imgvector5, 58f, 20f, 3f, 32f)
                Layer(R.drawable.figma_107_2911_imgvector6, 44.5f, 16f, 6f, 6f)
                TinyText("!", 46.5f, 16f, 5, Color.White)
            }
            2 -> {
                Layer(R.drawable.figma_113_2912_imgvector, 13f, 13f, 30f, 30f)
                Layer(R.drawable.figma_113_2912_imgvector1, 29f, 13f, 30f, 30f)
                Layer(R.drawable.figma_113_2912_imgvector2, 21f, 29f, 30f, 30f)
                TinyText("A", 24.5f, 23f, 9, Color(0xFF16A34A))
                TinyText("B", 41f, 23f, 9, Color(0xFFF4B942))
                TinyText("C", 32.5f, 39f, 9, Color(0xFFFF6B6B))
                TinyText("÷", 34f, 23f, 7, Color.White)
            }
            else -> {
                Layer(R.drawable.figma_122_2913_imgvector, 11.25f, 19.25f, 49.5f, 33.5f)
                Layer(R.drawable.figma_122_2913_imgvector1, 18.75f, 26.75f, 34.5f, 18.5f)
                Layer(R.drawable.figma_122_2913_imgvector2, 17.5f, 41.5f, 5f, 5f)
                Layer(R.drawable.figma_122_2913_imgvector2, 25.5f, 31.5f, 5f, 5f)
                Layer(R.drawable.figma_122_2913_imgvector3, 33.5f, 37.5f, 5f, 5f)
                Layer(R.drawable.figma_122_2913_imgvector2, 41.5f, 25.5f, 5f, 5f)
                Layer(R.drawable.figma_122_2913_imgvector2, 49.5f, 33.5f, 5f, 5f)
                Layer(R.drawable.figma_122_2913_imgvector4, 13.5f, 13.5f, 17f, 9f)
                TinyText("+15%", 15f, 14f, 6, Color(0xFF16A34A))
            }
        }
    }
}

@Composable
private fun Layer(@DrawableRes resource: Int, x: Float, y: Float, width: Float, height: Float) {
    FigmaImage(resource, Modifier.offset(x.dp, y.dp).size(width.dp, height.dp))
}

@Composable
private fun TinyText(value: String, x: Float, y: Float, size: Int, color: Color) {
    Text(value, Modifier.offset(x.dp, y.dp), color = color, fontFamily = Outfit,
        fontWeight = FontWeight.Bold, fontSize = size.sp, lineHeight = (size + 2).sp)
}

@Composable
internal fun BrandMark(modifier: Modifier = Modifier, large: Boolean = false) {
    Image(painterResource(R.drawable.budgetlens_mark), "Logo BudgetLens", modifier.size(if (large) 48.dp else 24.dp), contentScale = ContentScale.Fit)
}

private val darkModeWhiteIcons = setOf(
    R.drawable.figma_141_3102_imgimage22,
    R.drawable.figma_141_3102_imgimage25,
    R.drawable.figma_141_3102_imgimage27,
    R.drawable.figma_141_3102_imgimage32,
    R.drawable.figma_141_3102_imgimage35,
    R.drawable.figma_141_3102_imgimage36,
    R.drawable.figma_141_3102_imgimage37,
    R.drawable.figma_141_3102_imgimage40,
    R.drawable.figma_145_3103_imgimage35,
    R.drawable.figma_145_3103_imgimage36,
    R.drawable.figma_145_3103_imgimage37,
    R.drawable.figma_145_3103_imgimage40,
    R.drawable.figma_145_3103_imgimage41,
    R.drawable.figma_145_3103_imgimage42,
    R.drawable.figma_145_3103_imgimage47,
    R.drawable.figma_145_3103_imgimage49,
    R.drawable.figma_145_3105_imgimage35,
    R.drawable.figma_145_3105_imgimage36,
    R.drawable.figma_145_3105_imgimage40,
    R.drawable.figma_145_3105_imgimage41,
    R.drawable.figma_145_3105_imgimage42,
    R.drawable.figma_145_3105_imgimage50,
    R.drawable.figma_145_3107_imgimage53,
    R.drawable.figma_145_3109_imgimage11,
    R.drawable.figma_145_3109_imgimage12,
    R.drawable.figma_145_3109_imgimage14,
    R.drawable.figma_145_3109_imgimage15,
    R.drawable.figma_145_3109_imgimage16,
    R.drawable.figma_145_3109_imgimage18,
    R.drawable.figma_145_3109_imgimage19,
    R.drawable.figma_145_3109_imgimage20,
    R.drawable.figma_145_3109_imgimage9,
    R.drawable.figma_145_3111_imgimage,
    R.drawable.figma_145_3111_imgimage35,
    R.drawable.figma_145_3111_imgimage36,
    R.drawable.figma_145_3111_imgimage40,
    R.drawable.figma_145_3111_imgimage41,
    R.drawable.figma_145_3111_imgimage42,
    R.drawable.figma_145_3111_imgimage47,
    R.drawable.figma_145_3111_imgimage49,
    R.drawable.figma_145_3111_imgimage50,
    R.drawable.figma_145_3111_imgimage51,
    R.drawable.figma_145_3111_imgimage52
)

/** Shared navigation control matching the receipt scanner. */
@Composable
internal fun AppBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick,
        modifier = modifier.size(36.dp).semantics { contentDescription = "Kembali" },
        shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Box(contentAlignment = Alignment.Center) {
            Text("‹", fontSize = 25.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
