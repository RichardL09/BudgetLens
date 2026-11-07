package org.umn.utsmap.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.umn.utsmap.ui.theme.JetBrainsMono
import org.umn.utsmap.R
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.umn.utsmap.data.rupiah
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.finance.TransactionValidation
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TransactionFormScreen(finance: FinanceViewModel, onBack: () -> Unit, onSaved: (String) -> Unit, onScan: () -> Unit = {}, defaultIncome: Boolean = false) {
    var income by rememberSaveable { mutableStateOf(defaultIncome) }
    var amount by rememberSaveable { mutableStateOf("") }
    var title by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(if(defaultIncome) "Gaji" else "Makanan") }
    var date by rememberSaveable { mutableStateOf(finance.month.atDay(15).toString()) }
    var note by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var discard by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val dirty = amount.isNotEmpty() || title.isNotEmpty() || note.isNotEmpty() || income != defaultIncome ||
        category != (if(defaultIncome) "Gaji" else "Makanan") || date != finance.month.atDay(15).toString()
    val back = { if (dirty) discard = true else onBack() }
    BackHandler { back() }
    val amountError = if (submitted || amount.isNotEmpty()) TransactionValidation.amount(amount) else null
    LaunchedEffect(date) {
        if(!income && finance.expenseCategories(date).none{it.category==category || it.sourceCategory==category}) category=finance.expenseCategories(date).first().category
    }
    val titleError = if (submitted || title.isNotEmpty()) TransactionValidation.title(title) else null
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()
            .verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppBackButton(back)
                Text("Tambah Transaksi", style = MaterialTheme.typography.titleLarge, fontSize = 18.sp, modifier = Modifier.weight(1f))
                CompactAction("Scan Struk", onScan)
            }
            Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp)).padding(4.dp)) {
                listOf(false, true).forEach { isIncome ->
                    val active = income == isIncome
                    val gradient = BrandGradient
                    Button(onClick = { income = isIncome; category=if(isIncome) "Gaji" else finance.expenseCategories(date).first().category }, modifier = Modifier.weight(1f).height(40.dp)
                        .then(if (active) Modifier.background(gradient, RoundedCornerShape(11.dp)) else Modifier),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent,
                            contentColor = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant), shape = RoundedCornerShape(11.dp)) {
                        Text(if (isIncome) "↑ Pemasukan" else "↓ Pengeluaran", fontSize = 14.sp)
                    }
                }
            }
            Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    BasicTextField(amount, { amount = it.filter(Char::isDigit).take(12) }, singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.headlineLarge.copy(fontFamily = JetBrainsMono, fontWeight = FontWeight.ExtraBold,
                            fontSize=30.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.primary),
                        decorationBox = { inner ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Jumlah (Rp)", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    if (amount.isEmpty()) Text("0", style = MaterialTheme.typography.headlineLarge, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.primary.copy(alpha = .5f))
                                    inner()
                                }
                            }
                        })
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(10000L, 25000L, 50000L, 100000L).forEach { value ->
                            Surface(onClick = { amount = value.toString() }, modifier = Modifier.weight(1f).height(36.dp), shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.surface, border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                Box(contentAlignment = Alignment.Center) { Text(rupiah(value).removePrefix("Rp "), fontSize = 12.sp, maxLines = 1) }
                            }
                        }
                    }
                    if (amountError != null) Text(amountError, color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
                }
            }
            Text("Kategori", style = MaterialTheme.typography.titleSmall)
            val categoryLabels = if (income) finance.incomeCategories else finance.expenseCategories(date).map { it.category }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                categoryLabels.forEach { label ->
                    FilterChip(selected = category == label, onClick = { category = label },
                        label = { Text(label, fontSize = 12.sp) }, modifier = Modifier.height(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = FilterChipDefaults.filterChipColors(containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = .15f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary))
                }
            }
            Surface(color=MaterialTheme.colorScheme.surfaceVariant,shape=MaterialTheme.shapes.small) {
                Row(Modifier.fillMaxWidth().height(48.dp).padding(horizontal=12.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text("Tanggal",style=MaterialTheme.typography.labelMedium,modifier=Modifier.weight(1f))
                    Text(date,style=MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(8.dp))
                    TextButton(onClick = {
                        val current = LocalDate.parse(date)
                        DatePickerDialog(context, { _, year, month, day ->
                            date = LocalDate.of(year, month + 1, day).toString()
                        }, current.year, current.monthValue - 1, current.dayOfMonth).apply {
                            datePicker.minDate = LocalDate.of(2000, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                            datePicker.maxDate = LocalDate.of(2100, 12, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                        }.show()
                    }) { Text("Pilih",fontSize=12.sp) }
                }
            }
            FigmaInput(title, { title = it }, label = if (income) "Nama / Sumber Pemasukan" else "Nama Toko / Transaksi",
                hint = "Cth: Warung Bu Siti", error = titleError)
            FigmaInput(note, { note = it }, label = "Catatan (opsional)", hint = "Tambah catatan...", minLines = 2, maxLines = 4,
                error = if (note.length > 300) "Catatan maksimal 300 karakter." else null, supporting = "${note.length}/300 karakter")
            Button(onClick = {
                submitted = true
                if (!saving) {
                    val id = finance.save(title, amount, category, date, income, note)
                    if (id != null) { saving = true; onSaved(id) }
                }
            }, enabled = !saving, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = MaterialTheme.shapes.small) { Text("Simpan Transaksi") }
        }
    }
    if (discard) AlertDialog(onDismissRequest = { discard = false }, title = { Text("Batalkan transaksi?") },
        text = { Text("Isian yang belum disimpan akan dibuang.") },
        confirmButton = { TextButton(onClick = { discard = false; onBack() }) { Text("Buang Isian") } },
        dismissButton = { TextButton(onClick = { discard = false }) { Text("Lanjut Isi") } })
}


