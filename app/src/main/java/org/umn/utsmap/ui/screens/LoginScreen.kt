package org.umn.utsmap.ui.screens
import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.*
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.umn.utsmap.R
import org.umn.utsmap.ui.auth.AuthValidation
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.theme.BudgetLensTheme
import org.umn.utsmap.ui.theme.Inter
@Composable
fun LoginScreen(auth: AuthViewModel, onBack: () -> Unit) {
    val threshold = with(LocalDensity.current) { 72.dp.toPx() }
    Box(
        modifier = Modifier.fillMaxSize().pointerInput(auth.loading, auth.register) {
            var distance = 0f
            detectHorizontalDragGestures(
                onDragStart = { distance = 0f },
                onHorizontalDrag = { _, amount -> distance += amount },
                onDragEnd = {
                    if (!auth.loading) {
                        if (distance < -threshold) auth.switchMode(true)
                        if (distance > threshold) auth.switchMode(false)
                    }
                },
                onDragCancel = { distance = 0f },
            )
        },
    ) { LoginForm(auth, auth.register, onBack) }
}
@Composable
private fun LoginForm(auth: AuthViewModel, register: Boolean, onBack: () -> Unit) {
    var visible by rememberSaveable(register) { mutableStateOf(false) }
    var confirmVisible by rememberSaveable(register) { mutableStateOf(false) }
    var information by rememberSaveable { mutableStateOf<String?>(null) }
    BackHandler(enabled = auth.loading) { /* Tunggu pengiriman selesai agar sesi tidak tertinggal. */ }
    val nameError = if (auth.submitted || auth.name.isNotEmpty()) AuthValidation.name(auth.name) else null
    val emailError = if (auth.submitted || auth.email.isNotEmpty()) AuthValidation.email(auth.email) else null
    val passwordError = if (auth.submitted || auth.password.isNotEmpty()) AuthValidation.password(auth.password) else null
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    val confirmError = if (auth.submitted || auth.confirmation.isNotEmpty())
        AuthValidation.confirmation(auth.password, auth.confirmation) else null
    Surface(color = MaterialTheme.colorScheme.background) {
      Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxWidth().height(230.dp).background(
            Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .07f), MaterialTheme.colorScheme.background))))
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .padding(horizontal = 24.dp, vertical = 12.dp)) {
            TextButton(onClick = onBack, enabled = !auth.loading,
                contentPadding = PaddingValues(horizontal = 0.dp)) { Text("‹ Kembali", fontFamily = Inter, fontSize = 13.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal, color = Color(0xFF94A3B8)) }
            if(!imeVisible) {
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).background(BrandGradient, MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center) {
                    BrandMark(Modifier.size(22.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(buildAnnotatedString {
                    append("Budget")
                    withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) { append("Lens") }
                }, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.height(24.dp))
            }
            Text(if (register) "Buat Akun Baru" else "Selamat Datang",
                style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            if(!imeVisible) Text(if (register) "Bergabung dan mulai kelola keuanganmu dengan cerdas"
                else "Masuk untuk melanjutkan perjalanan finansialmu",
                modifier=Modifier.heightIn(min=44.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(if(imeVisible) 12.dp else 40.dp))
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant,
                MaterialTheme.shapes.small).padding(4.dp)) {
                listOf("Masuk", "Daftar").forEachIndexed { index, title ->
                    val selected = register == (index == 1)
                    Button(onClick = { auth.switchMode(index == 1) }, enabled = !auth.loading,
                        modifier = Modifier.weight(1f).height(36.dp).then(if (selected) Modifier.background(BrandGradient, MaterialTheme.shapes.extraSmall) else Modifier), shape = MaterialTheme.shapes.extraSmall,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = androidx.compose.ui.graphics.Color.Transparent,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)) {
                        Text(title, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
            AnimatedVisibility(register,enter=fadeIn(tween(250))+expandVertically(tween(300)),exit=fadeOut(tween(150))+shrinkVertically(tween(300))) { Column {
                AuthField("Nama Lengkap", auth.name, { auth.name = it; auth.clearError() },
                    nameError, !auth.loading, KeyboardType.Text, "Masukkan nama lengkap")
                Spacer(Modifier.height(12.dp))
                AuthField("Nomor Telepon (opsional)", auth.phone, { auth.phone = it; auth.clearError() },
                    if (auth.submitted && auth.phone.isNotBlank() && !Regex("62[0-9]{8,13}").matches(AuthViewModel.normalizePhone(auth.phone))) "Nomor telepon tidak valid." else null,
                    !auth.loading, KeyboardType.Phone, "08… atau +62…")
                Spacer(Modifier.height(12.dp))
            } }
            AuthField("Email", auth.email, { auth.email = it; auth.clearError() },
                emailError, !auth.loading, KeyboardType.Email, "nama@email.com")
            Spacer(Modifier.height(12.dp))
            AuthField("Password", auth.password, { auth.password = it; auth.clearError() },
                passwordError, !auth.loading, KeyboardType.Password, "Minimal 8 karakter",
                password = true, visible = visible, onToggle = { visible = !visible },
                last = !register, onDone = auth::submit)
            AnimatedVisibility(register,enter=fadeIn(tween(250))+expandVertically(tween(300)),exit=fadeOut(tween(150))+shrinkVertically(tween(300))) { Column {
                Spacer(Modifier.height(12.dp))
                AuthField("Konfirmasi Password", auth.confirmation, { auth.confirmation = it; auth.clearError() },
                    confirmError, !auth.loading, KeyboardType.Password, "Ulangi password",
                    password = true, visible = confirmVisible, onToggle = { confirmVisible = !confirmVisible },
                    last = true, onDone = auth::submit)
            } }
            AnimatedVisibility(!register,enter=fadeIn(tween(250)),exit=fadeOut(tween(100))) {
                Box(Modifier.fillMaxWidth(),contentAlignment=Alignment.CenterEnd) { TextButton(onClick = { information = "Pemulihan password melalui email belum tersedia pada demo offline. Gunakan akun demo atau buat akun baru melalui tab Daftar." },
                    enabled = !auth.loading) { Text("Lupa password?", fontFamily = Inter, fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Normal) }
            } }
            auth.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 12.dp))
            }
            Spacer(Modifier.height(16.dp))
            GradientButton(onClick = auth::submit, enabled = !auth.loading,
                modifier = Modifier.fillMaxWidth()) {
                if (auth.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.width(10.dp))
                    Text("Memproses…")
                } else Text(if (register) "Buat Akun" else "Masuk ke BudgetLens")
            }
            Spacer(Modifier.height(20.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
                Text("atau lanjutkan dengan", style = MaterialTheme.typography.bodySmall, color = Color(0xFF94A3B8))
                HorizontalDivider(Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Google", "Apple").forEach { provider ->
                    OutlinedButton(onClick = {
                        information = "Masuk dengan $provider belum tersedia. Gunakan email dan password."
                    }, enabled = !auth.loading, modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = MaterialTheme.shapes.small) {
                        FigmaImage(if (provider == "Google") R.drawable.figma_126_3100_imgsvg2 else R.drawable.figma_126_3100_imgsvg3,
                            Modifier.size(if (provider == "Google") 16.dp else 14.dp, 16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(provider, fontFamily = Inter, fontSize = 14.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.Medium, color = if (provider == "Google") Color(0xFFFF3B30) else MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            if (!register) {
                TextButton(onClick = { information = "Email: certified@email.com\nPassword: c3rt1f1ed" },
                    enabled = !auth.loading) { Text("Lihat akun demo") }
            }
            Spacer(Modifier.height(16.dp))
            }
        }
      }
    }
    information?.let { message ->
        AlertDialog(onDismissRequest = { information = null }, title = { Text("Informasi") },
            text = { Text(message) }, confirmButton = {
                TextButton(onClick = { information = null }) { Text("Mengerti") }
            })
    }
}
@Composable
private fun AuthField(
    label: String, value: String, onChange: (String) -> Unit, error: String?,
    enabled: Boolean, keyboard: KeyboardType, hint: String,
    password: Boolean = false, visible: Boolean = false, onToggle: () -> Unit = {},
    last: Boolean = false, onDone: () -> Unit = {},
) {
    BasicTextField(value = value, onValueChange = onChange, enabled = enabled,
        textStyle = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary),
        singleLine = true, modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboard, imeAction = if (last) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { onDone() }),
        visualTransformation = if (password && !visible) PasswordVisualTransformation() else VisualTransformation.None,
        decorationBox = { inner ->
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.fillMaxWidth().heightIn(min = 47.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .border(1.dp, if (error != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                    .padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = .5f))
                        inner()
                    }
                    if (password) IconButton(onClick = onToggle, enabled = enabled, modifier = Modifier.size(24.dp)) {
                        FigmaImage(R.drawable.figma_126_3100_imgsvg1, Modifier.size(18.dp), if (visible) "Tutup" else "Lihat")
                    }
                }
                if (error != null) Text(error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        })
}
@Preview(widthDp = 402, heightDp = 874)
@Preview(widthDp = 402, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun LoginPreview() {
    BudgetLensTheme { LoginScreen(remember { AuthViewModel() }, {}) }
}
