package org.umn.utsmap

import org.junit.Assert.*
import org.junit.Test
import org.umn.utsmap.data.DemoFinance

class DemoFinanceTest {
    @Test fun summaryAndCategoryTotalsReconcile() {
        assertEquals(1500000L, DemoFinance.income)
        assertEquals(696500L, DemoFinance.expenses)
        assertEquals(803500L, DemoFinance.balance)
        assertEquals(DemoFinance.expenses, DemoFinance.budgets.sumOf { DemoFinance.spent(it.category) })
        assertEquals(100000L, DemoFinance.debt)
        assertEquals(125000L, DemoFinance.receivable)
        assertEquals(DemoFinance.transactions.size, DemoFinance.transactions.map { it.id }.distinct().size)
    }
}
