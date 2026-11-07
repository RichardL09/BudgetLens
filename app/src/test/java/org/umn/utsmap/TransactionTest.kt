package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.finance.TransactionValidation

class TransactionTest {
    @Test fun invalidAmountsAndDatesAreRejected() {
        listOf("", "0", "-1", "1.5", "10,000", "abc", "1000000000000", "999999999999999999999").forEach {
            assertNotNull(it, TransactionValidation.amount(it))
        }
        assertNull(TransactionValidation.amount("25000"))
        assertNotNull(TransactionValidation.date("2025-02-30"))
        assertNotNull(TransactionValidation.title("   "))
    }
    @Test fun savedExpenseUpdatesAllSummariesAndIncomeDoesNotConsumeBudget() {
        val vm = FinanceViewModel()
        val id = vm.save("Makan Siang", "25000", "Makanan", "2026-10-16", false, "Kantin")!!
        assertEquals("Kantin", vm.find(id)?.note)
        assertEquals(107000L, vm.spent("Makanan"))
        assertEquals(721500L, vm.expenses)
        assertEquals(778500L, vm.balance)
        vm.save("Bonus", "100000", "Makanan", "2026-10-16", true, "")
        assertEquals(1600000L, vm.income)
        assertEquals(107000L, vm.spent("Makanan"))
        assertEquals(878500L, vm.balance)
    }
    @Test fun monthsAndAccountsAreIsolated() {
        val vm = FinanceViewModel()
        vm.activate("a@example.com")
        val id = vm.save("Oktober", "50000", "Makanan", "2026-11-01", false, "")!!
        assertEquals(50000L, vm.expenses)
        vm.shiftMonth(-1)
        assertEquals(965500L, vm.expenses)
        vm.activate("b@example.com")
        assertNull(vm.find(id))
        vm.activate("a@example.com")
        assertNotNull(vm.find(id))
        val size = vm.transactions.size
        assertNull(vm.save("invalid", "0", "Makanan", "2026-10-16", false, ""))
        assertEquals(size, vm.transactions.size)
    }
}
