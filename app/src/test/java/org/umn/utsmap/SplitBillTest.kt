package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.*

class SplitBillTest {
    private val members = listOf(BillMember("me", "Saya", self = true), BillMember("friend", "Dinda"), BillMember("other", "Fajar"))
    private val receipt = ReceiptDetails(listOf(ReceiptLine("Makanan", 1, 10001), ReceiptLine("Minum", 2, 5000)), 20001, 2000)
    @Test fun equalPreservesEveryRupiah() {
        val bill = BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.EQUAL, emptyList(), emptyMap())!!
        assertEquals(22001L, members.sumOf { bill.share(it.id) })
        receipt.items.indices.forEach { assertEquals(receipt.items[it].total, bill.allocations[it].values.sum()) }
        assertEquals(2000L, bill.taxes.values.sum())
    }
    @Test fun itemOwnershipAndTaxAreProportional() {
        val bill = BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.ITEMS, listOf(setOf("friend"), setOf("me", "other")), emptyMap())!!
        assertEquals(10001L, bill.subtotal("friend"))
        assertEquals(5000L, bill.subtotal("me"))
        assertEquals(1000L, bill.taxes["friend"])
        assertEquals(22001L, members.sumOf { bill.share(it.id) })
        assertNull(BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.ITEMS, listOf(emptySet(), setOf("me")), emptyMap()))
    }
    @Test fun percentageValidatesAndAllocates() {
        assertNull(BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.PERCENT, emptyList(), mapOf("me" to 50, "friend" to 40, "other" to 0)))
        val bill = BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.PERCENT, emptyList(), mapOf("me" to 50, "friend" to 50, "other" to 0))!!
        assertEquals(0L, bill.share("other"))
        assertEquals(22001L, members.sumOf { bill.share(it.id) })
    }
    @Test fun deleteUpdatesSummaryAndKeepsOtherAccountsSeparate() {
        val finance = FinanceViewModel()
        finance.activate("a")
        val id = finance.save("Struk", "22001", "Makanan", "2026-10-22", false, "", receipt = receipt)!!
        assertEquals(receipt, finance.find(id)?.receipt)
        assertEquals(987501L, finance.expenses)
        assertTrue(finance.delete(id))
        assertEquals(965500L, finance.expenses)
        assertNull(finance.find(id))
        val bill = BillCalculator.build("Toko", "2026-10-22", receipt, members, SplitMethod.EQUAL, emptyList(), emptyMap())!!
        finance.addBill(bill)
        finance.activate("b")
        assertNull(finance.bill(bill.id))
        finance.activate("a")
        assertEquals(bill, finance.bill(bill.id))
        assertFalse(finance.confirmPayment(bill.id, "friend", "me"))
        assertTrue(finance.requestPayment(bill.id, "friend", "friend"))
        assertEquals(PaymentStatus.PENDING, finance.bill(bill.id)!!.members[1].status)
        assertFalse(finance.confirmPayment(bill.id, "friend", "friend"))
        assertTrue(finance.confirmPayment(bill.id, "friend", "me"))
        assertTrue(finance.bill(bill.id)!!.members[1].paid)
    }
}
