package org.umn.utsmap.ui.screens

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.utsmap.data.Budget
import org.umn.utsmap.data.rupiah
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.finance.TransactionValidation
import org.umn.utsmap.ui.theme.Inter
import org.umn.utsmap.ui.theme.JetBrainsMono
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val BudgetLocale = Locale.forLanguageTag("id-ID")

@Composable
internal fun CompactAction(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(onClick = onClick, shape = MaterialTheme.shapes.small, color = Color.Transparent,
        modifier = modifier.height(32.dp)) {
        Box(Modifier.background(BrandGradient).padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
            Text(label, color = Color.White, fontFamily = Inter, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        }
    }
}

@Composable
internal fun BudgetScreen(finance: FinanceViewModel, onTransaction: (String) -> Unit, modifier: Modifier = Modifier) {
    var selectedSource by rememberSaveable { mutableStateOf<String?>(null) }
    var showEditor by rememberSaveable { mutableStateOf(false) }
    var editingSource by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = finance.budgets.find { it.sourceCategory == selectedSource }
    BackHandler(enabled = selected != null && !showEditor) { selectedSource = null }
    LazyColumn(modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp, 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Row(Modifier.fillMaxWidth().heightIn(min = 38.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (selected != null) AppBackButton(onClick = { selectedSource = null })
                Text(if (selected == null) "Budget" else "Rincian Budget", modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge)
                CompactAction(if (selected == null) "+ Tambah Budget" else "Edit", {
                    editingSource = selected?.sourceCategory
                    showEditor = true
                })
            }
        }
        item {
            PeriodControls(finance.budgetMonth, finance.budgetDate, finance::selectBudgetMonth, finance::selectBudgetDate)
        }
        if (selected == null) {
            item { BudgetOverview(finance) }
            items(finance.budgets, key = { it.sourceCategory }) { budget ->
                BudgetCard(budget, finance) { selectedSource = budget.sourceCategory }
            }
        } else {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface, shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            CategoryFormIcon(selected.sourceCategory)
                            Text(selected.category, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        }
                        Text("Total Terpakai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(rupiah(finance.budgetSpent(selected)), fontFamily = JetBrainsMono, fontSize = 26.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text("dari ${rupiah(selected.limit)}", style = MaterialTheme.typography.bodySmall)
                        val ratio = finance.budgetSpent(selected).toFloat() / selected.limit
                        LinearProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth(),
                            gapSize = 0.dp, drawStopIndicator = {}, color = if (ratio >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
                    }
                }
            }
            val transactions = finance.budgetTransactions(selected)
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Transaksi ${selected.category}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    Text("${transactions.size} transaksi", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (transactions.isEmpty()) item {
                Text("Belum ada transaksi untuk kategori dan periode ini.", modifier = Modifier.padding(vertical = 20.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
            }
            items(transactions, key = { it.id }) { transaction -> TransactionCard(transaction) { onTransaction(transaction.id) } }
        }
    }
    if (showEditor) BudgetEditorSheet(finance, finance.budgets.find { it.sourceCategory == editingSource }) {
        showEditor = false
        editingSource = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BudgetEditorSheet(finance: FinanceViewModel, editing: Budget?, onDismiss: () -> Unit) {
    var name by rememberSaveable(editing?.sourceCategory) { mutableStateOf(editing?.category.orEmpty()) }
    var amount by rememberSaveable(editing?.sourceCategory) { mutableStateOf(editing?.limit?.toString().orEmpty()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val nameError = if (submitted || name.isNotEmpty()) finance.budgetNameError(name, editing?.sourceCategory) else null
    val amountError = if (submitted || amount.isNotEmpty()) TransactionValidation.amount(amount) else null
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.extraLarge) {
        Column(Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(if (editing == null) "Tambah Budget" else "Edit Budget", style = MaterialTheme.typography.headlineSmall)
            Text(finance.label(finance.budgetMonth), color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium)
            Text("Tetapkan batas pengeluaran untuk kategori pilihanmu.", style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            FigmaInput(name, { name = it }, "Nama budget", "Contoh: Makanan", error = nameError)
            FigmaInput(amount, { amount = it }, "Nominal Budget", "Contoh: 500000", error = amountError,
                supporting = "Masukkan nominal dalam rupiah tanpa titik atau koma.", keyboard = KeyboardType.Number)
            GradientButton(onClick = {
                submitted = true
                if (finance.upsertBudget(name, amount, editing?.sourceCategory)) onDismiss()
            }, modifier = Modifier.fillMaxWidth()) { Text("Simpan") }
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Batal") }
        }
    }
}

@Composable
internal fun PeriodControls(period: YearMonth, day: LocalDate?, onMonth: (YearMonth) -> Unit, onDate: (LocalDate?) -> Unit) {
    var monthPicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            listOf(-1L, 0L, 1L).forEach { offset ->
                val label = period.plusMonths(offset).format(DateTimeFormatter.ofPattern(if (offset == 0L) "MMM yyyy ▾" else "MMM", BudgetLocale))
                Surface(onClick = { if (offset == 0L) monthPicker = true else onMonth(period.plusMonths(offset)) },
                    shape = CircleShape, color = if (offset == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                    border = if (offset == 0L) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Text(label, Modifier.padding(horizontal = 18.dp, vertical = 8.dp), fontSize = 12.sp,
                        color = if (offset == 0L) Color.White else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = {
                val date = day ?: period.atDay(1)
                DatePickerDialog(context, { _, year, month, dayOfMonth -> onDate(LocalDate.of(year, month + 1, dayOfMonth)) },
                    date.year, date.monthValue - 1, date.dayOfMonth).apply {
                    datePicker.minDate = LocalDate.of(2000, 1, 1).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    datePicker.maxDate = LocalDate.of(2100, 12, 31).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                }.show()
            }, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), shape = MaterialTheme.shapes.small) {
                Text(day?.format(DateTimeFormatter.ofPattern("d MMM yyyy", BudgetLocale)) ?: "Pilih tanggal", fontSize = 12.sp)
            }
            if (day != null) TextButton(onClick = { onDate(null) }) { Text("Semua tanggal", fontSize = 12.sp) }
        }
    }
    if (monthPicker) MonthYearPicker(period, onDismiss = { monthPicker = false }, onSelected = {
        onMonth(it)
        monthPicker = false
    })
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun MonthYearPicker(initial: YearMonth, onDismiss: () -> Unit, onSelected: (YearMonth) -> Unit) {
    var year by rememberSaveable { mutableStateOf(initial.year.toString()) }
    var month by rememberSaveable { mutableIntStateOf(initial.monthValue) }
    val validYear = year.toIntOrNull()?.takeIf { it in 2000..2100 }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Pilih bulan dan tahun") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                FigmaInput(year, { year = it }, "Tahun", keyboard = KeyboardType.Number,
                    error = if (validYear == null) "Masukkan tahun 2000–2100." else null)
                (1..12).chunked(3).forEach { months ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        months.forEach { number ->
                            FilterChip(selected = month == number, onClick = { month = number }, modifier = Modifier.weight(1f),
                                label = { Text(YearMonth.of(2026, number).format(DateTimeFormatter.ofPattern("MMM", BudgetLocale)), fontSize = 12.sp) })
                        }
                    }
                }
            }
        }, confirmButton = { TextButton(onClick = { validYear?.let { onSelected(YearMonth.of(it, month)) } }, enabled = validYear != null) { Text("Terapkan") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } })
}
