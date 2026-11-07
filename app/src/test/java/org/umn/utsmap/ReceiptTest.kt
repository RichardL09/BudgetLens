package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.ui.finance.ReceiptScanViewModel
import org.umn.utsmap.data.DemoFinance
import java.time.YearMonth
import java.time.LocalDate

class ReceiptTest {
    @Test fun receiptTotalsAndEditsAreConsistent() {
        val scan = ReceiptScanViewModel()
        assertEquals(39000L, scan.subtotal)
        assertEquals(3900L, scan.tax)
        assertEquals(42900L, scan.total)
        val revised = scan.items.mapIndexed { index, item -> if (index == 0) item.copy(quantity = 3) else item }
        assertTrue(scan.updateReceipt("Warung Kampus", "2026-10-16", revised))
        assertEquals(44000L, scan.subtotal)
        assertEquals(48400L, scan.total)
        assertFalse(scan.updateReceipt("", "2026-10-16", revised))
        assertFalse(scan.updateReceipt("Toko", "2026-02-30", revised))
        assertFalse(scan.updateReceipt("Toko", "2026-10-16", revised.map { it.copy(quantity = 0) }))
    }
    @Test fun allDemoTransactionsBelongToOctober2026() {
        assertEquals(10, DemoFinance.transactions.size)
        assertTrue(DemoFinance.transactions.all { YearMonth.from(LocalDate.parse(it.date)) == YearMonth.of(2026, 10) })
        assertEquals(1500000L, DemoFinance.income)
        assertEquals(696500L, DemoFinance.expenses)
    }
}
