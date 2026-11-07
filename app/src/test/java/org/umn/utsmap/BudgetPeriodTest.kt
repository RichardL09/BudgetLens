package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.ui.finance.FinanceViewModel
import java.time.LocalDate
import java.time.YearMonth

class BudgetPeriodTest {
    @Test fun budgetTransactionAndCurrentMonthDashboardAreIndependent() {
        val vm = FinanceViewModel()
        val current = YearMonth.now()
        val homeBefore = vm.homeExpenses
        vm.save("Belanja bulan ini", "25000", "Makanan", current.atDay(1).toString(), false, "")
        assertEquals(homeBefore + 25000, vm.homeExpenses)
        vm.selectTransactionMonth(YearMonth.of(2025, 8))
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertEquals(696500L + if (current == YearMonth.of(2026, 10)) 25000L else 0L, vm.budgetExpenses)
        assertEquals(YearMonth.of(2025, 8), vm.month)
        assertEquals(current, vm.homeMonth)
        assertEquals(homeBefore + 25000, vm.homeExpenses)
        vm.selectBudgetDate(LocalDate.of(2026, 10, 15))
        assertEquals(32500L, vm.budgetExpenses)
        vm.selectTransactionDate(LocalDate.of(2026, 10, 14))
        assertEquals(135000L, vm.expenses)
        assertEquals(LocalDate.of(2026, 10, 15), vm.budgetDate)
        vm.selectBudgetMonth(YearMonth.of(2026, 11))
        assertNull(vm.budgetDate)
        assertEquals(LocalDate.of(2026, 10, 14), vm.transactionDate)
        assertEquals(homeBefore + 25000, vm.homeExpenses)
    }

    @Test fun renameAndEditBudgetKeepHistoricalTransactionsLinkedForThatMonth() {
        val vm = FinanceViewModel()
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertTrue(vm.upsertBudget("Makan & Minum", "600000", "Makanan"))
        val renamed = vm.budgets.first { it.sourceCategory == "Makanan" }
        assertEquals(82000L, vm.budgetSpent(renamed))
        assertEquals(3, vm.budgetTransactions(renamed).size)
        assertEquals(600000L, renamed.limit)
        vm.selectBudgetDate(LocalDate.of(2026, 10, 15))
        assertEquals(28000L, vm.budgetSpent(renamed))
        vm.selectBudgetMonth(YearMonth.of(2026, 11))
        assertEquals("Makanan", vm.budgets.first().category)
        assertEquals(400000L, vm.budgets.first().limit)
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertEquals("Makan & Minum", vm.budgets.first().category)
        assertFalse(vm.upsertBudget("Makanan", "10000"))
    }

    @Test fun addBudgetValidatesAndAcceptsTransactionsWithoutChangingOtherAccount() {
        val vm = FinanceViewModel()
        vm.activate("one@example.com")
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertFalse(vm.upsertBudget("", "100000"))
        assertFalse(vm.upsertBudget("Kos", "0"))
        assertFalse(vm.upsertBudget("Kos", "10.000"))
        assertTrue(vm.upsertBudget("Kos", "1000000"))
        assertFalse(vm.upsertBudget(" kos ", "200000"))
        assertNotNull(vm.save("Sewa", "500000", "Kos", "2026-10-15", false, ""))
        assertEquals(500000L, vm.budgetSpent(vm.budgets.first { it.category == "Kos" }))
        vm.activate("two@example.com")
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertFalse(vm.budgets.any { it.category == "Kos" })
        vm.activate("one@example.com")
        vm.selectBudgetMonth(YearMonth.of(2026, 10))
        assertEquals(500000L, vm.budgetSpent(vm.budgets.first { it.category == "Kos" }))
    }
}
