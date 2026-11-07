package org.umn.utsmap.ui.screens

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.umn.utsmap.ui.theme.JetBrainsMono
import org.umn.utsmap.R
import org.umn.utsmap.data.*
import org.umn.utsmap.ui.auth.*
import org.umn.utsmap.ui.finance.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

private val Indonesian = Locale.forLanguageTag("id-ID")

@Composable
internal fun HomeBudgetSummary(finance: FinanceViewModel) {
    SoftCard {
        finance.homeBudgets.take(3).forEach { budget ->
            val ratio = finance.homeSpent(budget).toFloat() / budget.limit
            val color = when { ratio >= .9f -> MaterialTheme.colorScheme.error
                budget.category == "Transportasi" -> Color(0xFFF4B942)
                else -> MaterialTheme.colorScheme.primary }
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(budget.category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    Text("${rupiah(finance.homeSpent(budget))} / ${rupiah(budget.limit).removePrefix("Rp ")}",
                        fontFamily = JetBrainsMono, fontSize = 11.sp, color = color)
                }
                LinearProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, gapSize = 0.dp, drawStopIndicator = {},
                    modifier = Modifier.fillMaxWidth().height(6.dp), color = color, trackColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .6f))
                if (ratio >= .9f) Text(if (ratio >= 1f) "⚠ Melebihi budget!" else "⚠ Hampir habis!", fontSize = 10.sp, color = color)
            }
        }
    }
}

@Composable
internal fun GroupTotals(finance: FinanceViewModel) {
    SoftCard {
        Text("RINGKASAN SEMUA GRUP", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            listOf(Triple("Total Hutangmu", finance.debt, MaterialTheme.colorScheme.error),
                Triple("Total Piutangmu", finance.receivable, MaterialTheme.colorScheme.primary)).forEach { (label, amount, color) ->
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(label, fontSize = 12.sp, color = color)
                    Text(rupiah(amount), fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, fontFamily = JetBrainsMono, color = color)
                }
            }
        }
    }
}

@Composable
internal fun GroupSummaryCard(group: ExpenseGroup, onClick: () -> Unit = {}) {
    val color = if (group.balance < 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    SoftCard(Modifier.clickable(onClick=onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(color = color.copy(alpha = .07f), shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, color.copy(alpha = .2f))) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) { FigmaImage(R.drawable.figma_145_3107_imgimage53, Modifier.size(22.dp)) }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(group.name, style = MaterialTheme.typography.titleSmall)
                Text("${group.members} anggota", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FigmaImage(R.drawable.figma_145_3107_imgsvg2, Modifier.size(16.dp))
        }
        Surface(color = Color.Transparent, shape = MaterialTheme.shapes.extraSmall, border = BorderStroke(1.dp, color.copy(alpha = .3f))) {
            Text((if (group.balance < 0) "Hutang " else "Piutang ") + rupiah(kotlin.math.abs(group.balance)),
                Modifier.padding(horizontal = 10.dp, vertical = 8.dp), fontSize = 11.sp, color = color)
        }
    }
}

@Composable
internal fun MonthSelector(finance: FinanceViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        listOf(-1L, 0L, 1L).forEach { offset ->
            val label = finance.month.plusMonths(offset).format(DateTimeFormatter.ofPattern(if (offset == 0L) "MMM yyyy" else "MMM", Indonesian))
            Surface(onClick = { finance.shiftMonth(offset) }, shape = CircleShape,
                color = if (offset == 0L) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                contentColor = if (offset == 0L) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                border = if (offset == 0L) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Text(label, Modifier.padding(horizontal = 18.dp, vertical = 8.dp), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

@Composable
private fun SoftCard(modifier: Modifier = Modifier, border: Color = MaterialTheme.colorScheme.outlineVariant, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface, border = BorderStroke(1.dp, border)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun BudgetOverview(finance: FinanceViewModel) {
    val limit = finance.budgets.sumOf { it.limit }
    val ratio = if (limit > 0) finance.budgetExpenses.toFloat() / limit else 0f
    val color = if (ratio >= 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
    SoftCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Total Terpakai", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(rupiah(finance.budgetExpenses), style = MaterialTheme.typography.headlineSmall, fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold)
                Text("dari ${rupiah(limit)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxSize(), strokeWidth = 8.dp,
                    color = color, trackColor = MaterialTheme.colorScheme.outlineVariant)
                Text("${(ratio * 100).roundToInt()}%", style = MaterialTheme.typography.titleSmall)
            }
        }
        LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(if (finance.budgetExpenses <= limit) "Sisa ${rupiah(limit - finance.budgetExpenses)}" else "Lebih ${rupiah(finance.budgetExpenses - limit)}", style = MaterialTheme.typography.labelSmall, color = color)
            val today = LocalDate.now()
            val end = finance.budgetMonth.atEndOfMonth()
            Text(if (end < today) "Periode selesai" else if (finance.budgetMonth.atDay(1) > today) "Periode mendatang" else "${end.dayOfMonth - today.dayOfMonth} hari tersisa",
                style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun categoryIcon(category: String): Int = when (category) {
    "Makanan" -> R.drawable.figma_145_3103_imgimage35
    "Transportasi" -> R.drawable.figma_145_3103_imgimage36
    "Pendidikan" -> R.drawable.figma_145_3103_imgimage40
    "Hiburan" -> R.drawable.figma_145_3103_imgimage41
    "Kesehatan" -> R.drawable.figma_145_3103_imgimage42
    "Pemasukan" -> R.drawable.figma_145_3103_imgimage37
    else -> R.drawable.figma_145_3103_imgimage49
}

@Composable
internal fun CategoryFormIcon(category: String) { FigmaImage(categoryIcon(category), Modifier.size(12.dp)) }

@Composable
private fun CategoryTile(category: String, tint: Color = MaterialTheme.colorScheme.primary) {
    Box(Modifier.size(40.dp).background(tint.copy(alpha = .07f), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
        FigmaImage(categoryIcon(category), Modifier.size(20.dp))
    }
}

@Composable
internal fun BudgetCard(budget: Budget, finance: FinanceViewModel, onClick: () -> Unit = {}) {
    val used = finance.budgetSpent(budget)
    val ratio = used.toFloat() / budget.limit
    val color = when {
        ratio >= .9f -> MaterialTheme.colorScheme.error
        budget.category == "Transportasi" -> Color(0xFFECAE26)
        budget.category == "Pendidikan" -> Color(0xFF8971EB)
        else -> MaterialTheme.colorScheme.primary
    }
    SoftCard(modifier = Modifier.clickable(onClick = onClick), border = if (ratio >= .9f) MaterialTheme.colorScheme.error.copy(alpha = .3f) else MaterialTheme.colorScheme.outlineVariant) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            CategoryTile(budget.sourceCategory, color)
            Column(Modifier.weight(1f)) {
                Text(if (budget.category == "Makanan") "Makanan & Minum" else budget.category, style = MaterialTheme.typography.titleSmall)
                Text("${finance.budgetTransactions(budget).size} transaksi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column(horizontalAlignment = Alignment.End) {
                if (ratio >= .9f) Text(if (ratio >= 1f) "Melebihi budget!" else "Hampir habis!", style = MaterialTheme.typography.labelSmall, color = color)
                Text("${(ratio * 100).roundToInt()}%", style = MaterialTheme.typography.labelMedium, color = color)
            }
        }
        LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { ratio.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp), color = color)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(rupiah(used), style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(rupiah(budget.limit), style = MaterialTheme.typography.labelSmall, fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun TransactionTotals(finance: FinanceViewModel, selected: String = "BERSIH", onSelect: (String) -> Unit = {}) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(Triple("MASUK", finance.income, MaterialTheme.colorScheme.primary),
            Triple("KELUAR", finance.expenses, MaterialTheme.colorScheme.error),
            Triple("BERSIH", finance.balance, MaterialTheme.colorScheme.onSurface)).forEach { (title, amount, color) ->
            Surface(onClick = { onSelect(title) }, modifier = Modifier.weight(1f), shape = MaterialTheme.shapes.small, color = if (selected == title) color.copy(alpha = .1f) else MaterialTheme.colorScheme.surfaceVariant,
                border = BorderStroke(1.dp, color.copy(alpha = if (selected == title) .6f else .12f))) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(title, style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, lineHeight = 14.sp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text((if (title == "MASUK") "+" else if (title == "KELUAR") "−" else "") + rupiah(amount).removePrefix("Rp "),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp, lineHeight = 18.sp), fontFamily = JetBrainsMono, fontWeight = FontWeight.Bold, color = color)
                }
            }
        }
    }
}

@Composable
internal fun TransactionCard(transaction: Transaction, onClick: () -> Unit) {
    Surface(onClick = onClick, color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface, shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            CategoryTile(transaction.sourceCategory)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(transaction.title, style = MaterialTheme.typography.titleSmall.copy(fontSize = 13.sp), maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, fill = false))
                    if (transaction.split) Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary.copy(alpha = .1f)) {
                        Text("SPLIT", Modifier.padding(horizontal = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp), color = MaterialTheme.colorScheme.primary)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(LocalDate.parse(transaction.date).format(DateTimeFormatter.ofPattern("d MMM", Indonesian)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Surface(shape = CircleShape, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .4f)) {
                        Text(transaction.category, Modifier.padding(horizontal = 5.dp), style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
            Text((if (transaction.income) "+" else "") + rupiah(transaction.amount), fontFamily = JetBrainsMono,
                color = if (transaction.income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun ProfileOverview(user: DemoUser, finance: FinanceViewModel, onSavings: () -> Unit = {}) {
    val savings = if (finance.homeIncome > 0) finance.homeBalance.toFloat() / finance.homeIncome else 0f
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            AccountAvatar(user.photo, user.name, Modifier.size(64.dp))
            Column {
                Text(user.name, style = MaterialTheme.typography.titleMedium, fontSize = 18.sp)
                Text(user.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(finance.homeTransactions.size.toString() to "Transaksi\nperiode ini", "${(savings * 100).roundToInt()}%" to "Tabungan\ndari pemasukan", finance.groups.size.toString() to "Grup aktif").forEach { (value, caption) ->
                Surface(Modifier.weight(1f).height(100.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.surfaceVariant,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                    Column(Modifier.fillMaxSize().padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
                        Text(value, style = MaterialTheme.typography.titleLarge, fontFamily = JetBrainsMono, fontSize = 18.sp, color = MaterialTheme.colorScheme.primary)
                        Text(caption, minLines=2, maxLines=2, style = MaterialTheme.typography.labelSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        SoftCard {
            Text(finance.label(finance.homeMonth).uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Total Pemasukan", style = MaterialTheme.typography.labelSmall)
                    Text(rupiah(finance.homeIncome), fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleSmall)
                }
                Column(Modifier.weight(1f)) {
                    Text("Total Pengeluaran", style = MaterialTheme.typography.labelSmall)
                    Text(rupiah(finance.homeExpenses), fontFamily = JetBrainsMono, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.titleSmall)
                }
            }
            Row(Modifier.fillMaxWidth(), verticalAlignment=Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = onSavings, modifier=Modifier.height(32.dp), contentPadding = PaddingValues(0.dp)) { Text("Tingkat Tabungan ›", style = MaterialTheme.typography.labelSmall) }
                Text(String.format(Indonesian, "%.1f%%", savings * 100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
            LinearProgressIndicator(gapSize = 0.dp, drawStopIndicator = {}, progress = { savings.coerceIn(0f, 1f) }, modifier = Modifier.fillMaxWidth().height(5.dp))
        }
    }
}

@Composable
internal fun ProfileMenu(title: String, finance: FinanceViewModel, onClick: () -> Unit) {
    val (icon, subtitle) = when (title) {
        "Informasi Profil" -> R.drawable.figma_145_3109_imgimage9 to "Nama dan email akun"
        "Pengaturan Notifikasi" -> R.drawable.figma_145_3109_imgimage11 to "Budget alert, pengingat pembayaran"
        "Tingkat Tabungan" -> R.drawable.figma_145_3109_imgimage12 to "Rasio sisa pemasukan bulan berjalan"
        "Teman" -> R.drawable.figma_145_3109_imgimage9 to "Daftar teman dan permintaan pertemanan"
        "Pengaturan" -> R.drawable.figma_145_3109_imgimage14 to "Tema, keamanan, dan preferensi"
        else -> R.drawable.figma_145_3109_imgimage15 to "Simpan laporan transaksi sebagai PDF"
    }
    Surface(onClick = onClick, shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(38.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = .04f), MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                FigmaImage(icon, Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(subtitle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FigmaImage(R.drawable.figma_145_3109_imgsvg3, Modifier.size(16.dp))
        }
    }
}

