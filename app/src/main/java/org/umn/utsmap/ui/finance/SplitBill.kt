package org.umn.utsmap.ui.finance

import org.umn.utsmap.data.ReceiptDetails
import java.util.UUID

enum class PaymentStatus(val label: String) { UNPAID("Belum Dibayar"), PENDING("Menunggu Konfirmasi"), PAID("Sudah Dibayar") }
data class BillMember(val id: String, val name: String, val phone: String = "", val self: Boolean = false, val paid: Boolean = false, val status: PaymentStatus = if (paid) PaymentStatus.PAID else PaymentStatus.UNPAID)
enum class SplitMethod(val label: String) { EQUAL("Sama Rata"), ITEMS("Per Item"), PERCENT("Persen") }
data class SplitBill(val id: String, val title: String, val date: String, val receipt: ReceiptDetails,
    val members: List<BillMember>, val method: SplitMethod, val allocations: List<Map<String, Long>>,
    val taxes: Map<String, Long>, val transactionId: String? = null, val quantities: List<Map<String, Int>> = emptyList(),
    val creatorId: String = members.first { it.self }.id, val example: Boolean = false, val category: String = "Makanan", val groupId: String? = null, val payerId: String = creatorId) {
    fun subtotal(personId: String) = allocations.sumOf { it[personId] ?: 0L }
    fun share(personId: String) = subtotal(personId) + (taxes[personId] ?: 0L)
}

object BillCalculator {
    /** Largest-remainder allocation. Every rupiah belongs to exactly one member. */
    fun distribute(amount: Long, weights: List<Long>): List<Long> {
        require(amount >= 0 && weights.isNotEmpty() && weights.all { it >= 0 } && weights.sum() > 0)
        val sum = weights.sum()
        val result = weights.map { amount * it / sum }.toMutableList()
        val remainder = amount - result.sum()
        weights.indices.sortedWith(compareByDescending<Int> { amount * weights[it] % sum }.thenBy { it })
            .take(remainder.toInt()).forEach { result[it]++ }
        return result
    }
    fun build(title: String, date: String, receipt: ReceiptDetails, members: List<BillMember>, method: SplitMethod,
        owners: List<Set<String>>, percentages: Map<String, Int>, quantities: List<Map<String, Int>> = emptyList()): SplitBill? {
        if (members.size !in 2..20 || members.count { it.self } != 1 || members.map { it.id }.distinct().size != members.size ||
            members.any { it.name.isBlank() || it.name.length > 80 } || TransactionValidation.title(title) != null ||
            TransactionValidation.date(date) != null || receipt.items.isEmpty() || receipt.items.any { it.quantity !in 1..99 || it.unitPrice !in 1..999999999L } ||
            receipt.subtotal != receipt.items.sumOf { it.total } || receipt.tax < 0) return null
        val ids = members.map { it.id }.toSet()
        if (method == SplitMethod.ITEMS && quantities.isNotEmpty() && (quantities.size != receipt.items.size || quantities.indices.any { index ->
            quantities[index].keys.any { it !in ids } || quantities[index].values.any { it !in 0..receipt.items[index].quantity } || quantities[index].values.sum() != receipt.items[index].quantity })) return null
        if (method == SplitMethod.ITEMS && quantities.isEmpty() && (owners.size != receipt.items.size || owners.any { it.isEmpty() || !ids.containsAll(it) })) return null
        if (method == SplitMethod.PERCENT && (members.any { (percentages[it.id] ?: -1) !in 0..100 } || members.sumOf { percentages[it.id] ?: 0 } != 100)) return null
        val allocations = receipt.items.mapIndexed { index, item ->
            val weights = members.map { member -> when (method) {
                SplitMethod.EQUAL -> 1L
                SplitMethod.ITEMS -> if (quantities.isNotEmpty()) (quantities[index][member.id] ?: 0).toLong() else if (member.id in owners[index]) 1L else 0L
                SplitMethod.PERCENT -> percentages.getValue(member.id).toLong()
            } }
            members.map { it.id }.zip(distribute(item.total, weights)).toMap()
        }
        val taxShares = distribute(receipt.tax, members.map { member -> allocations.sumOf { it[member.id] ?: 0L } })
        return SplitBill(UUID.randomUUID().toString(), title.trim(), date, receipt, members, method, allocations, members.map { it.id }.zip(taxShares).toMap(), quantities = if (method == SplitMethod.ITEMS) quantities else emptyList())
    }
}
