package org.umn.utsmap

import org.junit.Test
import org.junit.Assert.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.*

class QuantitySplitTest {
    private val members = listOf(BillMember("self", "Saya", self = true, paid = true), BillMember("a", "Dinda"), BillMember("b", "Fajar"))
    private val receipt = ReceiptDetails(listOf(ReceiptLine("Tisu", 3, 10000), ReceiptLine("Sabun", 4, 15000)), 90000, 9000)
    @Test fun exactQuantitiesAllowZeroAndRejectMismatch() {
        val counts = listOf(mapOf("self" to 1, "a" to 2, "b" to 0), mapOf("self" to 2, "a" to 1, "b" to 1))
        val bill = BillCalculator.build("Kos", "2026-10-22", receipt, members, SplitMethod.ITEMS, emptyList(), emptyMap(), counts)!!
        assertEquals(44000L, bill.share("self")); assertEquals(38500L, bill.share("a")); assertEquals(16500L, bill.share("b"))
        assertEquals(99000L, members.sumOf { bill.share(it.id) })
        assertEquals(0L, bill.allocations[0]["b"])
        assertNull(BillCalculator.build("Kos", "2026-10-22", receipt, members, SplitMethod.ITEMS, emptyList(), emptyMap(), counts.map { it + ("self" to 0) }))
        assertNull(BillCalculator.build("Kos", "2026-10-22", receipt, members, SplitMethod.ITEMS, emptyList(), emptyMap(), listOf(mapOf("self" to -1, "a" to 4), counts[1])))
        assertNull(BillCalculator.build("Kos", "2026-10-22", receipt, members, SplitMethod.ITEMS, emptyList(), emptyMap(), listOf(mapOf("unknown" to 3), counts[1])))
    }
    @Test fun paymentRequiresClaimAndCreatorAndCannotRepeat() {
        val finance = FinanceViewModel(); finance.activate("account")
        val bill = finance.bill("example-dinda")!!
        assertEquals(44000L, bill.share("self"))
        assertFalse(finance.confirmPayment(bill.id, "self", "dinda"))
        assertFalse(finance.requestPayment(bill.id, "self", "fajar"))
        assertTrue(finance.requestPayment(bill.id, "self", "self"))
        assertFalse(finance.requestPayment(bill.id, "self", "self"))
        assertEquals(PaymentStatus.PENDING, finance.bill(bill.id)!!.members.first { it.self }.status)
        assertFalse(finance.confirmPayment(bill.id, "self", "self"))
        assertTrue(finance.confirmPayment(bill.id, "self", "dinda"))
        assertFalse(finance.confirmPayment(bill.id, "self", "dinda"))
        assertEquals(PaymentStatus.PAID, finance.bill(bill.id)!!.members.first { it.self }.status)
    }
}
