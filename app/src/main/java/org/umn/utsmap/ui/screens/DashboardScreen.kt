package org.umn.utsmap.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.selection.selectable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import org.umn.utsmap.ui.theme.Inter
import androidx.compose.foundation.shape.RoundedCornerShape
import org.umn.utsmap.ui.theme.JetBrainsMono
import kotlinx.coroutines.launch
import org.umn.utsmap.R
import org.umn.utsmap.data.*
import org.umn.utsmap.ui.auth.DemoUser
import org.umn.utsmap.ui.finance.FinanceViewModel
import androidx.compose.foundation.lazy.LazyRow

@Composable
fun DashboardScreen(user: DemoUser, onLogout: () -> Unit,
    finance: FinanceViewModel = remember { FinanceViewModel() },
    onAdd: () -> Unit = {}, onDetail: (String) -> Unit = {},
    onEditProfile: (String) -> Unit = {}, onScan: () -> Unit = {}, onSplitBills: () -> Unit = {}, onEditPhone: (String) -> String? = { null }, onAccount: (String) -> Unit = {}, onGroup: (String) -> Unit = {}, onNotifications: () -> Unit = {}, hideBalances: Boolean = false, sortNewest: Boolean = true) {
    var direction by rememberSaveable { mutableStateOf("BERSIH") }
    var category by rememberSaveable { mutableStateOf("Semua") }
    var selected by rememberSaveable { mutableIntStateOf(0) }
    var search by rememberSaveable { mutableStateOf("") }
    var showNotifications by rememberSaveable { mutableStateOf(false) }
    var read by rememberSaveable { mutableStateOf(false) }
    var confirmLogout by rememberSaveable { mutableStateOf(false) }
    val listState = key(selected) { androidx.compose.foundation.lazy.rememberLazyListState() }
    val snack = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val tabs = listOf("Beranda", "Transaksi", "Budget", "Grup", "Profil")
    val icons = listOf(R.drawable.figma_145_3103_imgsvg3, R.drawable.figma_141_3102_imgsvg6,
        R.drawable.figma_141_3102_imgsvg7, R.drawable.figma_141_3102_imgsvg8, R.drawable.figma_141_3102_imgsvg9)
    val activeIcons = listOf(R.drawable.figma_141_3102_imgsvg5, R.drawable.figma_145_3103_imgsvg4,
        R.drawable.figma_145_3105_imgsvg5, R.drawable.figma_145_3107_imgsvg6, R.drawable.figma_145_3109_imgsvg8)
    var selectMonth by rememberSaveable { mutableStateOf(false) }
    val pending: () -> Unit = { scope.launch { snack.showSnackbar("Fitur ini belum tersedia.") }; Unit }
    BackHandler(enabled = selected != 0) { selected = 0 }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snack) },
        bottomBar = {
            Surface(color = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
                Column {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                    Row(Modifier.fillMaxWidth().navigationBarsPadding()) {
                        tabs.forEachIndexed { index, title ->
                            val active = selected == index
                            val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            Column(Modifier.weight(1f).selectable(selected = active, role = Role.Tab, onClick = { selected = index })
                                .padding(bottom = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(Modifier.width(32.dp).height(2.dp).background(if (active) tint else Color.Transparent))
                                Spacer(Modifier.height(8.dp))
                                FigmaImage(if (active) activeIcons[index] else icons[index], Modifier.size(22.dp))
                                Spacer(Modifier.height(4.dp))
                                Text(title, color = tint, style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        },
    ) { padding ->
        if (selected == 2) {
            BudgetScreen(finance, onDetail, Modifier.padding(padding))
        } else if(selected==3) {
            Box(Modifier.padding(padding)) { GroupListScreen(finance,{selected=0},onGroup,user.name,user.phone) }
        } else {
        LazyColumn(Modifier.fillMaxSize().padding(padding).then(if (selected == 4) Modifier.background(
            Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary.copy(alpha = .06f), MaterialTheme.colorScheme.background), endY = 700f)) else Modifier), state = listState,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        if (selected == 0) Text("Selamat datang,", color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall)
                        Text(if (selected == 0) user.name else if (selected == 3) "Grup Saya" else tabs[selected],
                            style = MaterialTheme.typography.titleLarge, fontSize = if (selected == 0) 20.sp else 22.sp)
                    }
                    if (selected == 1) Surface(onClick = onAdd, modifier = Modifier.size(92.dp, 35.dp),
                        shape = MaterialTheme.shapes.small, color = Color.Transparent) {
                        Row(Modifier.fillMaxSize().background(BrandGradient), verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center) {
                            Text("+ Tambah", fontFamily = Inter, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                    if (selected == 4) FilledTonalButton(onClick = { onAccount("edit") }, shape = MaterialTheme.shapes.small) { Text("Edit") }
                    if (selected == 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(38.dp)) {
                                Surface(onClick = onNotifications, modifier = Modifier.fillMaxSize(),
                                    color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                                    Box(contentAlignment = Alignment.Center) {
                                        FigmaImage(R.drawable.figma_141_3102_imgsvg2, Modifier.size(18.dp), "Notifikasi")
                                    }
                                }
                                if (finance.notifications().any{!it.read}) Box(Modifier.align(Alignment.TopEnd).offset(x = 2.dp, y = (-2).dp)
                                    .size(8.dp).background(MaterialTheme.colorScheme.error, CircleShape))
                            }
                            Surface(onClick = { selected = 4 }, modifier = Modifier.size(38.dp).semantics { contentDescription = "Buka profil" },
                                color = MaterialTheme.colorScheme.primary.copy(alpha = .1f), shape = MaterialTheme.shapes.small,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .2f))) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(user.name.take(1).uppercase(), fontFamily = Inter, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                                        color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            }
            if (selected == 0) {
                item {
                    Box(Modifier.fillMaxWidth().background(
                        Brush.linearGradient(listOf(Color(0xFF16A34A), Color(0xFF15803D))),
                        MaterialTheme.shapes.large).padding(24.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Saldo Bersih Bulan Ini", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                Text(finance.label(finance.homeMonth), color = Color.White, style = MaterialTheme.typography.labelSmall)
                            }
                            Text(if(hideBalances) "••••••" else rupiah(finance.homeBalance), color = Color.White,
                                style = MaterialTheme.typography.headlineLarge, fontFamily = JetBrainsMono)
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("Pemasukan", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    Text(rupiah(finance.homeIncome), color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                                Column(Modifier.weight(1f)) {
                                    Text("Pengeluaran", color = Color.White, style = MaterialTheme.typography.bodySmall)
                                    Text(rupiah(finance.homeExpenses), color = Color.White, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                item { DebtSummary(finance) }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Tambah", "Scan", "Split Bill", "Grup").forEachIndexed { i, label ->
                            Surface(onClick = { when (i) { 0 -> onAdd(); 1 -> onScan(); 2 -> onSplitBills(); 3 -> selected = 3; else -> pending() } },
                                modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.surfaceVariant) {
                                Column(Modifier.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    FigmaImage(listOf(R.drawable.figma_141_3102_imgimage22, R.drawable.figma_141_3102_imgimage25,
                                        R.drawable.figma_141_3102_imgimage27, R.drawable.figma_141_3102_imgimage32)[i], Modifier.size(22.dp))
                                    Spacer(Modifier.height(6.dp))
                                    Text(label, style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
                item { SectionHeader("Budget Bulan Ini") { selected = 2 } }
                item { HomeBudgetSummary(finance) }
                item { SectionHeader("Transaksi Terbaru") { selected = 1 } }
                items(finance.homeTransactions.take(5), key = { it.id }) { TransactionCard(it) { onDetail(it.id) } }
            }
            if (selected == 1) {
                item { TransactionTotals(finance, direction) { direction = it; category = "Semua" } }
                if (direction != "BERSIH") item { Text(if (direction == "MASUK") "Menampilkan pemasukan" else "Menampilkan pengeluaran", color = MaterialTheme.colorScheme.primary) }
                item {
                    BasicTextField(search, { search = it }, singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth().height(40.dp).semantics { contentDescription = "Cari transaksi" },
                        decorationBox = { inner ->
                            Row(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, MaterialTheme.shapes.small)
                                .padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FigmaImage(R.drawable.figma_145_3103_imgsvg2, Modifier.size(16.dp))
                                Box(Modifier.weight(1f)) {
                                    if (search.isEmpty()) Text("Cari transaksi...", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    inner()
                                }
                            }
                        })
                }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items((listOf("Semua") + (if(direction=="MASUK") emptyList() else finance.transactionBudgets.map { it.category }) + (if(direction=="KELUAR") emptyList() else finance.incomeCategories + "Pemasukan")).distinct()) { label ->
                            FilterChip(selected = category == label, onClick = { category = label }, label = { Text(label) },
                                shape = CircleShape, colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary))
                        }
                    }
                }
                val filtered = (if(sortNewest) finance.transactions else finance.transactions.reversed()).filter {
                    (direction == "BERSIH" || (direction == "MASUK" && it.income) || (direction == "KELUAR" && !it.income)) && (category == "Semua" || it.category == category) && (it.title.contains(search.trim(), true) || it.category.contains(search.trim(), true))
                }
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("${finance.monthLabel.uppercase()} · ${filtered.size} transaksi", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                        TextButton(onClick = { selectMonth = true }, contentPadding = PaddingValues(0.dp), modifier = Modifier.height(24.dp)) { Text("‹ Bulan ›", fontSize = 10.sp) }
                    }
                }
                if (filtered.isEmpty()) item { Text("Tidak ada transaksi yang cocok.") }
                items(filtered, key = { it.id }) { TransactionCard(it) { onDetail(it.id) } }
            }
            if (selected == 3) {
                item { GroupTotals(finance) }
                items(finance.groups, key = { it.id }) { group ->
                    GroupSummaryCard(group)
                }
            }
            if (selected == 4) {
                item { ProfileOverview(user, finance) { onAccount("savings") } }
                items(listOf("Informasi Profil" to "info", "Teman" to "friends", "Pengaturan Notifikasi" to "notifications", "Tingkat Tabungan" to "savings", "Ekspor Data" to "export", "Pengaturan" to "settings")) { (title, route) ->
                    ProfileMenu(title, finance) { onAccount(route) }
                }
                item {
                    OutlinedButton(onClick = { confirmLogout = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("Keluar dari Akun", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
    }
    if (selectMonth) AlertDialog(onDismissRequest = { selectMonth = false }, title = { Text("Pilih bulan") },
        text = { PeriodControls(finance.month, finance.transactionDate, finance::selectTransactionMonth, finance::selectTransactionDate) }, confirmButton = { TextButton(onClick = { selectMonth = false }) { Text("Selesai") } })
    if (confirmLogout) {
        AlertDialog(onDismissRequest = { confirmLogout = false }, title = { Text("Keluar dari akun?") },
            text = { Text("Kamu dapat masuk kembali dengan akunmu.") },
            confirmButton = { TextButton(onClick = { confirmLogout = false; onLogout() }) { Text("Keluar") } },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Batal") } })
    }
}

@Composable
private fun SectionHeader(title: String, onAll: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = onAll, modifier = Modifier.height(24.dp), contentPadding = PaddingValues(0.dp)) { Text("Lihat semua", style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun DebtSummary(finance: FinanceViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(12.dp)) {
                Text("KAMU BERHUTANG", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                Text(rupiah(finance.debt), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                Text("Grup dan split bill", style = MaterialTheme.typography.bodySmall)
            }
        }
        Surface(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.small) {
            Column(Modifier.padding(12.dp)) {
                Text("KAMU DIUTANGI", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(rupiah(finance.receivable), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                Text("Grup dan split bill", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, subtitle: String, detail: String) {
    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.medium) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            Text(detail, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodySmall)
        }
    }
}



