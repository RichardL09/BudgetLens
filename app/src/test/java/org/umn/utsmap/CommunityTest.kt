package org.umn.utsmap

import org.junit.Test
import org.junit.Assert.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.data.*
import java.time.YearMonth

class CommunityTest {
    @Test fun seededBillsHaveMatchingTransactionsAndBudgets() {
        val vm=FinanceViewModel();vm.activate("ledger")
        val bills=vm.splitBills + vm.groupDetails.flatMap{vm.groupBills(it.id)}
        assertEquals(4,bills.size)
        bills.forEach{bill->val tx=vm.transactions.first{it.splitBillId==bill.id};assertEquals(bill.share("self"),tx.amount);assertEquals(bill.receipt,tx.receipt)}
        assertEquals(965500L,vm.expenses)
        vm.selectBudgetMonth(YearMonth.of(2026,10))
        assertEquals(126000L,vm.budgetSpent(vm.budgets.first{it.sourceCategory=="Makanan"}))
        assertTrue(vm.groupDebt("g1")>0);assertEquals(0L,vm.groupReceivable("g1"))
        assertTrue(vm.groupReceivable("g2")>0);assertEquals(0L,vm.groupDebt("g2"))
    }
    @Test fun groupCreationMembersAndSharesAreValidatedAndEditable() {
        val vm=FinanceViewModel();vm.activate("group-owner")
        assertNull(vm.createGroup(""));val id=vm.createGroup("Belanja Teman")!!
        assertTrue(vm.addGroupMember(id,BillMember("friend","Marcelline","6281230000011")))
        assertFalse(vm.addGroupMember(id,BillMember("again","Marcelline","6281230000011")))
        val receipt=ReceiptDetails(listOf(ReceiptLine("Tisu",3,10000)),30000,0)
        assertNull(vm.saveGroupExpense(id,receipt,"Tisu","2026-10-22","Makanan","missing","self",listOf(mapOf("self" to 1,"friend" to 2)),SplitMethod.ITEMS))
        val billId=vm.saveGroupExpense(id,receipt,"Tisu","2026-10-22","Makanan","friend","self",listOf(mapOf("self" to 1,"friend" to 2)),SplitMethod.ITEMS)!!
        assertEquals(10000L,vm.groupDebt(id))
        assertTrue(vm.updateGroupAllocation(billId,listOf(mapOf("self" to 2,"friend" to 1)),"Lainnya"))
        assertEquals(20000L,vm.transactions.first{it.splitBillId==billId}.amount)
        assertEquals("Lainnya",vm.transactions.first{it.splitBillId==billId}.category)
        assertTrue(vm.requestPayment(billId,"self","self"))
        assertFalse(vm.updateGroupAllocation(billId,listOf(mapOf("self" to 1,"friend" to 2)),"Makanan"))
        assertTrue(vm.confirmPayment(billId,"self","friend"))
        assertEquals(0L,vm.groupDebt(id))
    }
    @Test fun invitationCodesAndNotificationsUseRealState() {
        val vm=FinanceViewModel();vm.activate("owner")
        val id=vm.createGroup("Grup Kode")!!;val code=vm.group(id)!!.code
        vm.activate("joiner");assertNull(vm.group(id));assertEquals(id,vm.joinGroup(code));assertEquals(2,vm.group(id)!!.members.size)
        assertNull(vm.joinGroup("TIDAKADA"))
        val notices=vm.notifications();assertTrue(notices.any{it.target=="example-dinda"})
        vm.readAllNotices();assertTrue(vm.notifications().all{it.read})
        vm.requestPayment("example-dinda","self","self")
        assertTrue(vm.notifications().any{!it.read && it.target=="example-dinda"})
    }
    @Test fun incomeCategoriesDoNotBecomeBudgetSpending() {
        val vm=FinanceViewModel();val old=vm.expenses
        val id=vm.save("Gaji","2000000","Gaji","2026-10-22",true,"")!!
        assertEquals("Gaji",vm.find(id)!!.category);assertEquals(old,vm.expenses)
    }
}
