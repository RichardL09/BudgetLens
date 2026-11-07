package org.umn.utsmap.ui.screens
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.rupiah
import java.util.UUID
@Composable
private fun BillPage(title: String, onBack: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppBackButton(onBack)
                Text(title, style = MaterialTheme.typography.titleLarge)
            }
            content()
        }
    }
}
@Composable
private fun BillCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceVariant, contentColor=MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}
@Composable
private fun BillMoney(label: String, amount: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Text(rupiah(amount), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}
@Composable
fun SplitBillListScreen(finance: FinanceViewModel, auth: AuthViewModel, onBack: () -> Unit, onScan: () -> Unit, onDetail: (String) -> Unit, onManual: () -> Unit = {}) {
    BillPage("Split Bill", onBack) {
        Text("Tagihan Bersama", style = MaterialTheme.typography.headlineSmall)
        Text("Lihat pembagian item dan bagian setiap anggota.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        GradientButton(onClick = onScan, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("+ Buat dari Scan Struk") }
        OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("+ Buat Split Bill Manual") }
        if (finance.splitBills.isEmpty()) BillCard { Text("Belum ada split bill"); Text("Scan struk, lalu pilih Split Bill dari Struk Ini.") }
        finance.splitBills.forEach { bill ->
            Surface(onClick = { onDetail(bill.id) }, modifier = Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant, contentColor=MaterialTheme.colorScheme.onSurface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(bill.title, style = MaterialTheme.typography.titleMedium)
                    Text("${bill.date} • ${bill.members.size} anggota • ${bill.receipt.items.size} item", style = MaterialTheme.typography.bodySmall)
                    BillMoney("Total tagihan", bill.receipt.total)
                    BillMoney("Bagianmu", bill.share(bill.members.first { it.self }.id))
                    Text("Lihat rincian ›", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun SplitBillCreateScreen(scan: ReceiptScanViewModel, finance: FinanceViewModel, auth: AuthViewModel, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val members = rememberSaveable(saver = MemberListSaver) { mutableStateListOf(BillMember("self", auth.user?.name ?: "Saya", auth.user?.phone.orEmpty(), self = true, paid = true)) }
    var method by rememberSaveable { mutableStateOf(SplitMethod.ITEMS) }
    var quantities by rememberSaveable(stateSaver = AllocationSaver) { mutableStateOf(scan.items.map { mapOf("self" to it.quantity.toString()) }) }
    var percentages by rememberSaveable(stateSaver = StringMapSaver) { mutableStateOf(mapOf("self" to "100")) }
    var add by rememberSaveable { mutableStateOf(false) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    val counts = quantities.map { row -> row.mapValues { if (it.value.any { digit -> digit !in '0'..'9' }) -1 else it.value.toIntOrNull() ?: -1 } }
    val preview = BillCalculator.build(scan.merchant, scan.date, scan.receiptDetails(), members.toList(), method, emptyList(),
        percentages.mapValues { if (it.value.any { digit -> digit !in '0'..'9' }) -1 else it.value.toIntOrNull() ?: -1 }, counts)
    BillPage("Atur Split Bill", onBack) {
        BillCard { Text(scan.merchant, style = MaterialTheme.typography.titleLarge); BillMoney("Total Tagihan", scan.total); Text("${scan.date} • PPN ${scan.taxPercent}% dibagi proporsional") }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Anggota (${members.size})", style = MaterialTheme.typography.titleMedium)
            TextButton(onClick = { add = true }, enabled = members.size < 20) { Text("+ Tambah") }
        }
        members.forEach { person ->
            BillCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(person.name + if (person.self) " (Kamu)" else "", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                    if (!person.self) TextButton(onClick = {
                        members.remove(person); quantities = quantities.map { it - person.id }; percentages = percentages - person.id
                    }) { Text("Hapus") }
                }
                if (method == SplitMethod.PERCENT) FigmaInput(percentages[person.id].orEmpty(), { percentages = percentages + (person.id to it) }, "Persen ${person.name}", keyboard = KeyboardType.Number,
                    error = if (submitted) numberError(percentages[person.id].orEmpty(), 0, 100, "Persentase") else null)
                preview?.let { BillMoney("Bagian", it.share(person.id)) }
            }
        }
        Text("Kategori Budget", style=MaterialTheme.typography.titleMedium)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            finance.expenseCategories(scan.date).forEach { budget -> FilterChip(scan.category==budget.category || scan.category==budget.sourceCategory,{scan.category=budget.category},label={Text(budget.category)}) }
        }
        Text("Metode Pembagian", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { SplitMethod.entries.forEach { value -> FilterChip(selected = method == value, onClick = { method = value }, label = { Text(value.label) }) } }
        if (method == SplitMethod.PERCENT) Text("Jumlah persentase harus 100%.",
            color = if (submitted && members.sumOf { percentages[it.id]?.toIntOrNull() ?: 0 } != 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
        Text("Rincian Item", style = MaterialTheme.typography.titleMedium)
        scan.items.forEachIndexed { index, item ->
            BillCard {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                BillMoney("${item.quantity} × ${rupiah(item.unitPrice)}", item.total)
                if (method == SplitMethod.ITEMS) {
                    Text("Isi jumlah untuk setiap anggota. Boleh 0.", style = MaterialTheme.typography.bodySmall)
                    members.forEach { person ->
                        FigmaInput(quantities[index][person.id] ?: "0", { value ->
                            quantities = quantities.mapIndexed { i, row -> if (i != index) row else row + (person.id to value.take(3)) }
                        }, "Jumlah ${item.name} - ${person.name}", keyboard = KeyboardType.Number,
                            error = if (submitted) numberError(quantities[index][person.id] ?: "0", 0, item.quantity.toLong(), "Jumlah") else null)
                    }
                    val assigned = counts[index].values.sum()
                    Text("Terbagi $assigned / ${item.quantity}", color = if (assigned == item.quantity && counts[index].values.all { it >= 0 }) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
                } else Text(if (method == SplitMethod.EQUAL) "Dibagi rata ke semua anggota" else "Dibagi sesuai persentase", style = MaterialTheme.typography.bodySmall)
            }
        }
        preview?.let { bill -> BillCard { BillMoney("Subtotal", bill.receipt.subtotal); BillMoney("PPN ${scan.taxPercent}%", bill.receipt.tax); BillMoney("Bagianmu", bill.share("self")) } }
        if (submitted && preview == null) Text("Minimal 2 anggota. Jumlah pembagian tiap item harus tepat sama dengan jumlah barang, atau total persen harus 100%.", color = MaterialTheme.colorScheme.error)
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        GradientButton(onClick = {
            submitted = true
            val id = scan.saveBill(finance, members.toList(), method, emptyList(), percentages.mapValues { if (it.value.any { digit -> digit !in '0'..'9' }) -1 else it.value.toIntOrNull() ?: -1 }, counts)
            if (id != null) onSaved(id) else if (preview != null) error = "Tagihan belum dapat disimpan. Periksa nominal dan kategori transaksi."
        }, modifier = Modifier.fillMaxWidth(), enabled = scan.savedBillId == null && scan.savedId == null) { Text("Simpan Split Bill") }
    }
    if (add) AddBillMemberDialog(auth, members.toList(), onAdd = { member ->
        members.add(member); quantities = quantities.map { it + (member.id to "0") }; percentages = percentages + (member.id to "0"); add = false
    }, onDismiss = { add = false })
}

@Composable
fun ManualBillScreen(scan: ReceiptScanViewModel, onBack: () -> Unit, onNext: () -> Unit, pageTitle: String = "Split Bill Manual") {
    var title by rememberSaveable { mutableStateOf(scan.merchant) }
    var date by rememberSaveable { mutableStateOf(scan.date) }
    var names by rememberSaveable { mutableStateOf(ArrayList(scan.items.map { it.name })) }
    var amounts by rememberSaveable { mutableStateOf(ArrayList(scan.items.map { it.unitPrice.toString() })) }
    var counts by rememberSaveable { mutableStateOf(ArrayList(scan.items.map { it.quantity.toString() })) }
    var tax by rememberSaveable { mutableStateOf(if (scan.merchant.isBlank()) "0" else scan.taxPercent.toString()) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    BillPage(pageTitle, onBack) {
        FigmaInput(title, { title = it; error = null }, "Nama tagihan", error = if (submitted || title.isNotEmpty()) TransactionValidation.title(title) else null)
        FigmaInput(date, { date = it; error = null }, "Tanggal", "YYYY-MM-DD", error = if (submitted) TransactionValidation.date(date) else null)
        FigmaInput(tax, { tax = it; error = null }, "PPN (%)", keyboard = KeyboardType.Number, error = if (submitted) numberError(tax, 0, 100, "PPN") else null)
        names.indices.forEach { index -> BillCard {
            Text("Item ${index + 1}")
            FigmaInput(names[index], { value -> names = ArrayList(names).apply { set(index, value) }; error = null }, "Nama item ${index+1}", error = if (submitted || names[index].isNotEmpty()) itemNameError(names[index]) else null)
            FigmaInput(counts[index], { value -> counts = ArrayList(counts).apply { set(index, value) }; error = null }, "Jumlah item ${index+1}", keyboard = KeyboardType.Number, error = if (submitted) numberError(counts[index], 1, 99, "Jumlah") else null)
            FigmaInput(amounts[index], { value -> amounts = ArrayList(amounts).apply { set(index, value) }; error = null }, "Harga satuan item ${index+1}", keyboard = KeyboardType.Number, error = if (submitted) numberError(amounts[index], 1, 999999999, "Harga satuan") else null)
            if (names.size > 1) TextButton(onClick = {
                names = ArrayList(names).apply { removeAt(index) }; amounts = ArrayList(amounts).apply { removeAt(index) }; counts = ArrayList(counts).apply { removeAt(index) }
            }) { Text("Hapus Item ${index+1}") }
        } }
        OutlinedButton(onClick = { names = ArrayList(names + ""); amounts = ArrayList(amounts + "0"); counts = ArrayList(counts + "1") }, enabled = names.size < 20) { Text("+ Tambah Item") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        GradientButton(onClick = {
            submitted = true
            val revised = names.indices.map { ReceiptItem(names[it].trim(), counts[it].toIntOrNull() ?: 0, amounts[it].toLongOrNull() ?: 0) }
            val percent = tax.toIntOrNull()
            if (numberError(tax, 0, 100, "PPN") != null || names.indices.any { itemNameError(names[it]) != null || numberError(counts[it], 1, 99, "Jumlah") != null || numberError(amounts[it], 1, 999999999, "Harga satuan") != null } || percent == null || !scan.updateReceipt(title, date, revised)) error = "Periksa nama, tanggal, jumlah 1–99, harga 1–999.999.999, dan PPN 0–100%."
            else { scan.taxPercent = percent; onNext() }
        }, modifier = Modifier.fillMaxWidth()) { Text("Lanjut Atur Anggota") }
    }
}
@Composable
private fun AddBillMemberDialog(auth: AuthViewModel, members: List<BillMember>, onAdd: (BillMember) -> Unit, onDismiss: () -> Unit) {
    FriendMemberPicker(auth,members,onAdd,onDismiss)
}
@Composable
fun SplitBillDetailScreen(bill: SplitBill?, finance: FinanceViewModel, onBack: () -> Unit, onEditGroup: () -> Unit = {}) {
    var personId by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmCreator by remember { mutableStateOf<String?>(null) }
    BackHandler(enabled = personId != null) { personId = null }
    BillPage(if (personId == null) "Split Bill" else "Rincian Per Orang", { if (personId != null) personId = null else onBack() }) {
        if (bill == null) { Text("Split bill tidak ditemukan."); return@BillPage }
        val person = bill.members.find { it.id == personId }
        Surface(Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.primary, shape = MaterialTheme.shapes.large) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(if (person == null) "Total Tagihan" else person.name, color = MaterialTheme.colorScheme.onPrimary)
                Text(rupiah(if (person == null) bill.receipt.total else bill.share(person.id)), style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onPrimary)
                Text("${bill.title} • ${bill.date}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        if (person == null) {
            BillCard { Text("BAGIANMU", color = MaterialTheme.colorScheme.primary); BillMoney(bill.members.first { it.self }.name, bill.share(bill.members.first { it.self }.id)) }
            val creator = bill.members.first { it.id == bill.payerId }
            val self = bill.members.first { it.self }
            Text("Metode: ${bill.method.label} • Dibayar oleh: ${creator.name}")
            if(bill.groupId!=null) Text("Ditambahkan oleh: ${bill.members.first{it.id==bill.creatorId}.name}",style=MaterialTheme.typography.bodySmall)

            Text("Anggota (${bill.members.size})", style = MaterialTheme.typography.titleMedium)
            bill.members.forEach { member ->
                BillCard {
                    Row(Modifier.fillMaxWidth().clickable { personId = member.id }, verticalAlignment = Alignment.CenterVertically) {
                        Text(member.name + if (member.self && member.name!="Kamu") " (Kamu)" else "", Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                        Text(rupiah(bill.share(member.id)), color = MaterialTheme.colorScheme.primary)
                    }
                    TextButton(onClick = { personId = member.id }) { Text("Lihat rincian per orang ›") }
                    Text(if (member.id == bill.payerId) "Pembayar awal" else member.status.label, style = MaterialTheme.typography.labelMedium)
                    if (member.id != bill.payerId && bill.share(member.id) > 0) {
                        if (member.status == PaymentStatus.UNPAID && member.self) OutlinedButton(onClick = { finance.requestPayment(bill.id, member.id, member.id) }) { Text("Tandai Sudah Bayar") }
                        if (member.status == PaymentStatus.PENDING && self.id == bill.payerId) OutlinedButton(onClick = { finance.confirmPayment(bill.id, member.id, self.id) }) { Text("Konfirmasi Pembayaran ${member.name}") }
                        if (member.status == PaymentStatus.UNPAID && !member.self && self.id == bill.payerId) TextButton(onClick = { finance.requestPayment(bill.id, member.id, member.id) }) { Text("Catat Pembayaran ${member.name}") }
                        if (member.status == PaymentStatus.PENDING && bill.example && self.id != bill.payerId) TextButton(onClick = { confirmCreator = member.id }) { Text("Konfirmasi Penerimaan ${creator.name}") }
                    }
                }
            }
            BillCard {
                Text("STATUS PEMBAYARAN", style = MaterialTheme.typography.labelMedium)
                BillMoney("Sudah dikonfirmasi", bill.members.filter { it.id != bill.payerId && it.status == PaymentStatus.PAID }.sumOf { bill.share(it.id) })
                BillMoney("Menunggu konfirmasi", bill.members.filter { it.id != bill.payerId && it.status == PaymentStatus.PENDING }.sumOf { bill.share(it.id) })
                BillMoney("Belum dibayar", bill.members.filter { it.id != bill.payerId && it.status == PaymentStatus.UNPAID }.sumOf { bill.share(it.id) })
                Text("Pembayaran dilakukan di luar aplikasi. Tandai bayar tidak langsung membuat status lunas.", style = MaterialTheme.typography.bodySmall)
            }
        }
        if(bill.groupId!=null) OutlinedButton(onClick=onEditGroup,enabled=bill.members.none{it.id!=bill.payerId && it.status!=PaymentStatus.UNPAID}){Text("Edit Pembagian & Kategori")}
        Text("Kategori: ${bill.category}",style=MaterialTheme.typography.labelMedium)
        Text("Rincian Item", style = MaterialTheme.typography.titleMedium)
        bill.receipt.items.forEachIndexed { index, item ->
            val allocation = bill.allocations[index]
            if (person == null || (allocation[person.id] ?: 0L) > 0L) BillCard {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                BillMoney("${item.quantity} × ${rupiah(item.unitPrice)}", item.total)
                if (person == null) bill.members.filter { (allocation[it.id] ?: 0L) > 0 }.forEach { BillMoney(it.name + (bill.quantities.getOrNull(index)?.get(it.id)?.let { count -> " ×$count" } ?: ""), allocation.getValue(it.id)) }
                else BillMoney("Bagian ${person.name}" + (bill.quantities.getOrNull(index)?.get(person.id)?.let { " ×$it" } ?: ""), allocation[person.id] ?: 0)
            }
        }
        BillCard {
            BillMoney("Subtotal", if (person == null) bill.receipt.subtotal else bill.subtotal(person.id))
            BillMoney("PPN", if (person == null) bill.receipt.tax else bill.taxes[person.id] ?: 0)
            BillMoney("Total", if (person == null) bill.receipt.total else bill.share(person.id))
        }
    }
    confirmCreator?.let { memberId -> if (bill != null) AlertDialog(onDismissRequest = { confirmCreator = null }, title = { Text("Konfirmasi Penerimaan") },
        text = { Text("Konfirmasi bahwa ${bill.members.first { it.id == bill.payerId }.name} sudah menerima pembayaran ini.") },
        confirmButton = { TextButton(onClick = { finance.confirmPayment(bill.id, memberId, bill.payerId); confirmCreator = null }) { Text("Konfirmasi Diterima") } },
        dismissButton = { TextButton(onClick = { confirmCreator = null }) { Text("Batal") } }) }

}
