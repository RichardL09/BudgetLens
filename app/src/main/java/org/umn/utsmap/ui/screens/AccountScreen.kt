package org.umn.utsmap.ui.screens

import android.content.Intent
import android.net.Uri
import android.graphics.BitmapFactory
import android.widget.ImageView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.umn.utsmap.ui.auth.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.*

@Composable
internal fun AccountAvatar(photo: String, name: String, modifier: Modifier = Modifier.size(72.dp)) {
    val context = LocalContext.current
    val bitmap = remember(photo) {
        if (photo.isEmpty()) null else try {
            val uri = Uri.parse(photo)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 256) sample *= 2
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample }) }
        } catch (_: Exception) { null }
    }
    Surface(modifier, shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.primaryContainer) {
        if (bitmap == null) Box(contentAlignment = Alignment.Center) { Text(name.take(1).uppercase(), style = MaterialTheme.typography.headlineMedium) }
        else AndroidView(factory = { ImageView(it).apply { scaleType = ImageView.ScaleType.CENTER_CROP } }, update = { it.setImageBitmap(bitmap) })
    }
}
@Composable
fun AccountScreen(section: String, auth: AuthViewModel, finance: FinanceViewModel, onBack: () -> Unit, onOpen: (String) -> Unit) {
    if(section=="friends") { FriendsScreen(auth,onBack);return }
    val user = auth.user ?: return
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var message by rememberSaveable(section) { mutableStateOf<String?>(null) }
    var editName by rememberSaveable(user.id, section) { mutableStateOf(user.name) }
    var editPhone by rememberSaveable(user.id, section) { mutableStateOf(user.phone) }
    var photo by rememberSaveable(user.id, section) { mutableStateOf(user.photo) }
    var address by rememberSaveable(user.id, section) { mutableStateOf(user.email) }
    val owner = requireNotNull(androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner.current)
    val secrets = remember(owner, user.id, section) {
        androidx.lifecycle.ViewModelProvider(owner).get("account-form:${user.id}:$section", AccountFormViewModel::class.java)
    }
    var old by secrets::old
    var next by secrets::next
    var confirm by secrets::confirmation
    var code by rememberSaveable(user.id, section) { mutableStateOf("") }
    var exporting by remember { mutableStateOf(false) }
    var exported by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable(user.id, section) { mutableStateOf(false) }
    var codeSubmitted by rememberSaveable(user.id, section) { mutableStateOf(false) }
    var fieldErrors by rememberSaveable(user.id, section, stateSaver = StringMapSaver) { mutableStateOf(emptyMap<String, String>()) }
    var messageIsError by rememberSaveable(section) { mutableStateOf(false) }
    val nameError = AuthValidation.name(editName) ?: if (editName.trim().length > 80) "Nama maksimal 80 karakter." else null
    val phoneError = if (!Regex("62[0-9]{8,13}").matches(AuthViewModel.normalizePhone(editPhone))) "Nomor telepon tidak valid. Gunakan 08… atau +62…." else null
    fun changed(field: String) { fieldErrors = fieldErrors - field; message = null }
    fun feedback(error: String?, success: String) { messageIsError = error != null; message = error ?: success }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { try { context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION); photo = it.toString() } catch (_: Exception) { feedback("Foto tidak dapat dibuka.", "") } }
    }
    val document = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) {
            val rows = finance.periodEntries(); val period = finance.monthLabel; val owner = user.name
            exporting = true
            scope.launch {
                val result = try {
                    withContext(Dispatchers.IO) { context.contentResolver.openOutputStream(uri, "wt")?.use { FinancePdf.write(it, owner, period, rows) } ?: error("File tidak tersedia") }
                    exported = uri.toString(); null
                } catch (_: Exception) { "PDF tidak dapat disimpan. Pilih lokasi lain." }
                feedback(result, "PDF berhasil disimpan.")
                exporting = false
            }
        }
    }
    val title = when(section) { "info" -> "Informasi Profil"; "edit" -> "Edit Profil"; "email" -> "Email & Verifikasi"; "password" -> "Ganti Password"; "notifications" -> "Pengaturan Notifikasi"; "export" -> "Ekspor PDF"; "savings" -> "Tingkat Tabungan"; else -> "Pengaturan" }
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            AppBackButton(onBack)
            Text(title, style = MaterialTheme.typography.titleLarge)
            when(section) {
                "info" -> {
                    AccountAvatar(user.photo, user.name)
                    AccountValue("Nama Lengkap", user.name)
                    AccountValue("Email", user.email + if (user.verified) " • Terverifikasi" else " • Belum diverifikasi")
                    AccountValue("Nomor Telepon", user.phone.ifBlank { "Belum diisi" })
                    AccountValue("Password", "••••••••")
                    AccountLink("Edit Foto, Nama & Nomor Telepon") { onOpen("edit") }
                    AccountLink("Ubah / Verifikasi Email") { onOpen("email") }
                    AccountLink("Ganti Password") { onOpen("password") }
                }
                "edit" -> {
                    AccountAvatar(photo, editName)
                    OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) { Text("Ganti Foto Profil") }
                    if (photo.isNotEmpty()) TextButton(onClick = { photo = "" }) { Text("Hapus Foto") }
                    FigmaInput(editName, { editName = it; changed("name") }, "Nama lengkap", error = if (submitted) nameError ?: fieldErrors["name"] else null)
                    FigmaInput(editPhone, { editPhone = it; changed("phone") }, "Nomor telepon", keyboard = KeyboardType.Phone, error = if (submitted) phoneError ?: fieldErrors["phone"] else null)
                    GradientButton(onClick = {
                        submitted = true
                        if (nameError == null && phoneError == null) {
                            val error = auth.editProfile(editName, editPhone, photo)
                            fieldErrors = if (error != null) mapOf("phone" to error) else emptyMap()
                            feedback(error, "Profil berhasil diperbarui.")
                            if (error == null) submitted = false
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Profil") }
                }
                "password" -> {
                    SecretField(old, { old = it; changed("old") }, "Password saat ini", if (submitted) (if(old.isBlank()) "Password saat ini wajib diisi." else fieldErrors["old"]) else null)
                    SecretField(next, { next = it; changed("next") }, "Password baru", if (submitted) AuthValidation.password(next) ?: fieldErrors["next"] else null)
                    SecretField(confirm, { confirm = it; changed("confirm") }, "Konfirmasi password baru", if (submitted) AuthValidation.confirmation(next, confirm) else null)
                    Text("Minimal 8 karakter. Password asli tidak pernah ditampilkan.", style = MaterialTheme.typography.bodySmall)
                    GradientButton(onClick = {
                        submitted = true
                        if (old.isNotBlank() && AuthValidation.password(next) == null && AuthValidation.confirmation(next, confirm) == null) {
                            val error = auth.changePassword(old, next, confirm)
                            fieldErrors = if(error == null) emptyMap() else mapOf((if(error == "Password saat ini salah.") "old" else "next") to error)
                            feedback(error, "Password berhasil diganti.")
                            if (error == null) { submitted = false; old = ""; next = ""; confirm = "" }
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Ganti Password") }
                }
                "email" -> {
                    FigmaInput(address, { address = it; changed("email") }, "Email", keyboard = KeyboardType.Email, error = if (submitted) AuthValidation.email(address) ?: fieldErrors["email"] else null)
                    SecretField(old, { old = it; changed("old") }, "Password saat ini", if (submitted) (if(old.isBlank()) "Password saat ini wajib diisi." else fieldErrors["old"]) else null)
                    OutlinedButton(onClick = {
                        submitted = true
                        if (AuthValidation.email(address) == null && old.isNotBlank()) {
                            val error = auth.requestEmail(address, old)
                            fieldErrors = if(error == null) emptyMap() else mapOf((if(error == "Password saat ini salah.") "old" else "email") to error)
                            feedback(error, "Kode verifikasi disiapkan.")
                            if(error == null) { submitted = false; old = "" }
                        }
                    }) { Text("Minta Kode Verifikasi") }
                    auth.pendingEmail?.let {
                        Text("Verifikasi $it")
                        Text("Kode verifikasi: 123456 (berlaku 5 menit).", style = MaterialTheme.typography.bodySmall)
                        val codeError = if (code.length != 6 || code.any { it !in '0'..'9' }) "Kode verifikasi harus 6 angka." else null
                        FigmaInput(code, { code = it.take(6); changed("code") }, "Kode verifikasi", keyboard = KeyboardType.Number, error = if(codeSubmitted) codeError ?: fieldErrors["code"] else null)
                        GradientButton(onClick = {
                            codeSubmitted = true
                            if(codeError == null) {
                                val error = auth.verifyEmail(code)
                                fieldErrors = if(error == null) emptyMap() else mapOf("code" to error)
                                feedback(error, "Email berhasil diverifikasi.")
                                if(error == null) { codeSubmitted = false; code = "" }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Verifikasi Email") }
                    }
                }
                "notifications" -> {
                    Text("Atur informasi yang ingin kamu lihat di aplikasi.")
                    AccountSwitch("Peringatan budget", "Tampilkan indikator saat pemakaian budget tinggi", finance.budgetAlerts) { finance.budgetAlerts = it }
                    AccountSwitch("Pengingat pembayaran", "Pengingat tagihan Split Bill yang belum lunas", finance.paymentReminders) { finance.paymentReminders = it }
                    AccountSwitch("Pembaruan Split Bill", "Pembayaran menunggu konfirmasi dan status lunas", finance.splitUpdates) { finance.splitUpdates = it }
                    AccountSwitch("Ringkasan mingguan", "Preferensi ringkasan keuangan", finance.weeklySummary) { finance.weeklySummary = it }
                    AccountSwitch("Jam tenang", "Preferensi tenang pukul 22.00–07.00", finance.quietHours) { finance.quietHours = it }
                }
                "export" -> {
                    Text("Laporan PDF", style = MaterialTheme.typography.headlineSmall)
                    PeriodControls(finance.month, finance.transactionDate, finance::selectTransactionMonth, finance::selectTransactionDate)
                    Text("${finance.transactions.size} transaksi • ${finance.monthLabel}")
                    Text("Laporan berisi ringkasan pemasukan, pengeluaran, serta rincian transaksi dan item struk. Filter tanggal ikut diterapkan.")
                    GradientButton(onClick = { document.launch("BudgetLens-${finance.month}.pdf") }, enabled = !exporting, modifier = Modifier.fillMaxWidth()) { Text(if (exporting) "Membuat PDF…" else "Simpan PDF") }
                    exported?.let { path -> OutlinedButton(onClick = {
                        try { context.startActivity(Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(path), "application/pdf").addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)) }
                        catch (_: Exception) { feedback("PDF sudah disimpan. Buka melalui aplikasi Files atau pembaca PDF.", "") }
                    }) { Text("Buka PDF") } }
                }
                "savings" -> {
                    val income = finance.homeIncome; val expense = finance.homeExpenses
                    val rate = if (income > 0) (income-expense) * 100.0 / income else null
                    Text(finance.label(finance.homeMonth), style = MaterialTheme.typography.titleMedium)
                    AccountValue("Pemasukan", rupiah(income)); AccountValue("Pengeluaran", rupiah(expense)); AccountValue("Sisa bersih", rupiah(income-expense))
                    Text(if (rate == null) "Belum dapat dihitung" else String.format(java.util.Locale.forLanguageTag("id-ID"), "%.1f%%", rate), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                    Text("Rumus: (pemasukan − pengeluaran) ÷ pemasukan × 100%.")
                    Text("Angka ini menunjukkan bagian pemasukan yang belum terpakai. Gunakan untuk mengevaluasi pola pengeluaran, bukan sebagai bukti saldo rekening atau tabungan yang benar-benar disetor.")
                    Text(if (income == 0L) "Tambahkan pemasukan agar rasio dapat dihitung." else if (expense > income) "Rasio negatif: pengeluaran melebihi pemasukan bulan ini." else "Rasio positif: masih ada sisa pemasukan bulan ini.")
                    Text("Tidak ada target atau pemindahan uang. Nilai berubah mengikuti catatan bulan berjalan.", style = MaterialTheme.typography.bodySmall)
                }
                else -> {
                    Text("Tampilan", style = MaterialTheme.typography.titleMedium)
                    listOf("system" to "Ikuti Sistem", "light" to "Terang", "dark" to "Gelap").forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(auth.themeMode == value, { auth.changeTheme(value, context) }); Text(label) }
                    }
                    Text("Privasi & Penggunaan",style=MaterialTheme.typography.titleMedium)
                    AccountSwitch("Sembunyikan saldo beranda","Samarkan angka saldo pada ringkasan beranda",auth.hideBalances){auth.setPreference("hide",it,context)}
                    AccountSwitch("Konfirmasi hapus transaksi","Minta persetujuan sebelum menghapus catatan",auth.confirmDeletion){auth.setPreference("confirm",it,context)}
                    AccountSwitch("Pemasukan sebagai pilihan awal","Buka formulir transaksi pada jenis pemasukan",auth.defaultIncome){auth.setPreference("defaultIncome",it,context)}
                    AccountSwitch("Transaksi terbaru di atas","Matikan untuk menampilkan urutan terlama dahulu",auth.sortNewest){auth.setPreference("newest",it,context)}
                    AccountLink("Kelola Teman"){onOpen("friends")}
                    AccountLink("Pengaturan Notifikasi") { onOpen("notifications") }
                    AccountLink("Keamanan Akun") { onOpen("password") }
                    AccountLink("Ekspor Laporan PDF") { onOpen("export") }
                    Text("Tentang BudgetLens", style = MaterialTheme.typography.titleMedium)
                    Text("Pencatat keuangan pribadi dan pembagian tagihan. Pembayaran dilakukan di luar aplikasi.")
                }
            }
            message?.let { if (it !in fieldErrors.values) Text(it, color = if(messageIsError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary) }
        }
    }
}
@Composable private fun SecretField(value: String, onChange: (String) -> Unit, label: String, error: String? = null) {
    OutlinedTextField(value, onChange, label = { Text(label) }, isError = error != null, supportingText = error?.let { text -> { Text(text) } }, singleLine = true, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Password))
}
@Composable private fun AccountValue(label: String, value: String) { Column(verticalArrangement = Arrangement.spacedBy(6.dp)) { Text(label, style = MaterialTheme.typography.labelMedium); Text(value); HorizontalDivider() } }
@Composable private fun AccountLink(label: String, action: () -> Unit) { OutlinedButton(onClick = action, modifier = Modifier.fillMaxWidth()) { Text(label) } }
@Composable private fun AccountSwitch(title: String, subtitle: String, checked: Boolean, action: (Boolean) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(title); Text(subtitle, style = MaterialTheme.typography.bodySmall) }; Switch(checked, action) }
    }
}
