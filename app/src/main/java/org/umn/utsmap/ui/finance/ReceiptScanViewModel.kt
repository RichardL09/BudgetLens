package org.umn.utsmap.ui.finance

import androidx.compose.runtime.*
import org.umn.utsmap.data.ReceiptDetails
import org.umn.utsmap.data.ReceiptLine
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class ScanStage { CAMERA, PROCESSING, RESULT }
data class ReceiptItem(val name: String, val quantity: Int, val unitPrice: Long) {
    val total: Long get() = quantity * unitPrice
}

/** Receives real images from CameraX/gallery; receipt extraction remains a local simulation. */
class ReceiptScanViewModel : ViewModel() {
    var stage by mutableStateOf(ScanStage.CAMERA)
        private set
    var progress by mutableIntStateOf(0)
        private set
    var merchant by mutableStateOf("Indomaret Seturan")
    var date by mutableStateOf("2026-10-15")
    var note by mutableStateOf("")
    var category by mutableStateOf("Makanan")
    var selectedImage by mutableStateOf<String?>(null)
    var flash by mutableStateOf(false)
    var items by mutableStateOf(exampleItems())
        private set
    var savedBillId by mutableStateOf<String?>(null)
        private set
    var savedId by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    private var processing: Job? = null
    val subtotal get() = items.sumOf { it.total }
    var taxPercent by mutableIntStateOf(10)
    val tax get() = (subtotal * taxPercent + 50L) / 100L
    val total get() = subtotal + tax
    val quarterTurns get() = progress / 20

    fun reset() {
        processing?.cancel()
        stage = ScanStage.CAMERA
        progress = 0
        selectedImage = null
        flash = false
        taxPercent = 10
        merchant = "Indomaret Seturan"
        date = "2026-10-15"
        note = ""
        category = "Makanan"
        items = exampleItems()
        savedBillId = null
        savedId = null
        error = null
    }
    fun capture() {
        if (stage != ScanStage.CAMERA) return
        progress = 0
        stage = ScanStage.PROCESSING
        processing = viewModelScope.launch {
            for (percent in 5..100 step 5) {
                delay(150)
                progress = percent
            }
            delay(250)
            stage = ScanStage.RESULT
        }
    }
    fun retake() = reset()
    fun updateReceipt(name: String, dateValue: String, revised: List<ReceiptItem>): Boolean {
        if (TransactionValidation.title(name) != null || TransactionValidation.date(dateValue) != null || revised.isEmpty() ||
            revised.any { it.name.isBlank() || it.name.length > 80 || it.quantity !in 1..99 || it.unitPrice !in 1..999999999L }) return false
        merchant = name.trim()
        date = dateValue
        items = revised
        return true
    }
    fun save(finance: FinanceViewModel, people: Int = 1): String? {
        savedId?.let { return it }
        if (savedBillId != null) return null
        if (stage != ScanStage.RESULT || people !in 1..20 || note.length > 240) { error = "Periksa kembali isian struk."; return null }
        val amount = (total + people - 1) / people
        val description = listOf(note.trim(), "${items.size} item • Termasuk PPN 10%", if (people > 1) "Split $people orang • Total Rp $total" else "")
            .filter { it.isNotEmpty() }.joinToString("\n").take(300)
        val id = finance.save(merchant, amount.toString(), category, date, false, description, split = people > 1, receipt = receiptDetails())
        if (id == null) error = "Transaksi belum dapat disimpan. Periksa kategori dan tanggal struk."
        else { savedId = id; error = null }
        return id
    }
    fun saveBill(finance: FinanceViewModel, members: List<BillMember>, method: SplitMethod, owners: List<Set<String>>, percentages: Map<String, Int>, quantities: List<Map<String, Int>> = emptyList()): String? {
        savedBillId?.let { return it }
        if (stage != ScanStage.RESULT || savedId != null) return null
        val bill = BillCalculator.build(merchant, date, receiptDetails(), members, method, owners, percentages, quantities)?.copy(category=category) ?: return null
        val share = bill.share(members.first { it.self }.id)
        val tx = if (share > 0) finance.save(merchant, share.toString(), category,
            date, false, note.take(240), split = true, receipt = receiptDetails(), splitBillId = bill.id) ?: return null else null
        finance.addBill(bill.copy(transactionId = tx))
        savedId = tx
        savedBillId = bill.id
        return bill.id
    }
    fun beginManual() {
        reset(); merchant = ""; date = java.time.LocalDate.now().toString(); items = listOf(ReceiptItem("", 1, 0)); stage = ScanStage.RESULT
    }
    fun receiptDetails() = ReceiptDetails(items.map { ReceiptLine(it.name, it.quantity, it.unitPrice) }, subtotal, tax)
    fun markSaved(id: String) { savedId = id }
    companion object {
        fun exampleItems() = listOf(ReceiptItem("Aqua 600ml", 2, 5000), ReceiptItem("Indomie Goreng", 3, 3500),
            ReceiptItem("Teh Botol Sosro", 1, 7000), ReceiptItem("Chitato Sapi Panggang", 1, 11500))
    }
}
