package org.umn.utsmap.data

import java.text.NumberFormat
import java.util.Locale

data class Transaction(val id: String, val title: String, val category: String, val amount: Long,
    val date: String, val income: Boolean = false, val split: Boolean = false, val note: String = "", val sourceCategory: String = category, val receipt: ReceiptDetails? = null, val splitBillId: String? = null)
data class ReceiptLine(val name: String, val quantity: Int, val unitPrice: Long) { val total get() = quantity * unitPrice }
data class ReceiptDetails(val items: List<ReceiptLine>, val subtotal: Long, val tax: Long) { val total get() = subtotal + tax }

data class Budget(val category: String, val limit: Long, val sourceCategory: String = category)
data class ExpenseGroup(val id: String, val name: String, val members: Int, val balance: Long)

fun rupiah(value: Long): String = "Rp " + NumberFormat.getIntegerInstance(Locale.forLanguageTag("id-ID")).format(value)

/** Seluruh ringkasan dihitung dari sumber data yang sama untuk demo Oktober 2026. */
object DemoFinance {
    val transactions = listOf(
        Transaction("t1", "Mie Ayam Geprek", "Makanan", 28000, "2026-10-15", split = true),
        Transaction("t2", "Trans Jogja", "Transportasi", 4500, "2026-10-15"),
        Transaction("t3", "Uang Saku Bulanan", "Pemasukan", 1500000, "2026-10-15", income = true),
        Transaction("t4", "Buku Algoritma", "Pendidikan", 135000, "2026-10-14"),
        Transaction("t5", "Kopi Tuku", "Makanan", 32000, "2026-10-13"),
        Transaction("t6", "Tiket Konser", "Hiburan", 180000, "2026-10-12"),
        Transaction("t7", "Apotek Kimia Farma", "Kesehatan", 45000, "2026-10-11"),
        Transaction("t8", "Perlengkapan Kos", "Lainnya", 200000, "2026-10-10"),
        Transaction("t9", "Warung Makan Padang", "Makanan", 22000, "2026-10-10"),
        Transaction("t10", "Top Up Kuota", "Lainnya", 50000, "2026-10-09"),
    )
    val budgets = listOf(Budget("Makanan", 400000), Budget("Transportasi", 150000),
        Budget("Hiburan", 200000), Budget("Pendidikan", 300000), Budget("Kesehatan", 100000), Budget("Lainnya", 200000))
    val groups = listOf(ExpenseGroup("g1", "Kos Griya Asri", 4, -75000),
        ExpenseGroup("g2", "Trip Pantai Gunungkidul", 6, 125000),
        ExpenseGroup("g3", "Kelompok Praktikum IF", 5, -25000))
    val income get() = transactions.filter { it.income }.sumOf { it.amount }
    val expenses get() = transactions.filterNot { it.income }.sumOf { it.amount }
    val balance get() = income - expenses
    val debt get() = -groups.filter { it.balance < 0 }.sumOf { it.balance }
    val receivable get() = groups.filter { it.balance > 0 }.sumOf { it.balance }
    fun spent(category: String) = transactions.filter { !it.income && it.category == category }.sumOf { it.amount }
}

