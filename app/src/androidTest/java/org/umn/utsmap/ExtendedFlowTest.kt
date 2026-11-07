package org.umn.utsmap

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme

class ExtendedFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun manualBillAllocatesItemCountsAndValidatesTotal() {
        val finance=FinanceViewModel(); val scan=ReceiptScanViewModel(); val auth=AuthViewModel()
        scan.beginManual()
        var step by mutableStateOf(0); var billId: String? = null
        compose.setContent { BudgetLensTheme {
            when(step) {
                0 -> ManualBillScreen(scan,{}, { step=1 })
                1 -> SplitBillCreateScreen(scan,finance,auth,{}, { billId=it;step=2 })
                else -> SplitBillDetailScreen(finance.bill(billId!!),finance,{})
            }
        } }
        compose.onNodeWithText("Nama tagihan").performTextInput("Belanja Kos")
        compose.onNodeWithText("PPN (%)").performTextReplacement("10")
        compose.onNodeWithText("Nama item 1").performTextInput("Tisu")
        compose.onNodeWithText("Jumlah item 1").performTextReplacement("3")
        compose.onNodeWithText("Harga satuan item 1").performTextReplacement("10000")
        compose.onNodeWithText("+ Tambah Item").performScrollTo().performClick()
        compose.onNodeWithText("Nama item 2").performScrollTo().performTextInput("Sabun")
        compose.onNodeWithText("Jumlah item 2").performTextReplacement("4")
        compose.onNodeWithText("Harga satuan item 2").performTextReplacement("15000")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        listOf("Dinda","Fajar").forEach { name ->
            compose.onNodeWithText("+ Tambah").performScrollTo().performClick()
            compose.onNodeWithText("Nama Manual").performClick()
        compose.onNodeWithText("Nama anggota").performTextInput(name)
            compose.onNodeWithText("Tambah",substring=false).performClick()
        }
        compose.onNodeWithText("Jumlah Tisu - Saya").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Jumlah Tisu - Dinda").performTextReplacement("2")
        compose.onNodeWithText("Jumlah Sabun - Saya").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Jumlah Sabun - Dinda").performTextReplacement("1")
        compose.onNodeWithText("Simpan Split Bill").performScrollTo().performClick()
        assertNull(billId)
        compose.onNodeWithText("Jumlah Sabun - Fajar").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Simpan Split Bill").performScrollTo().performClick()
        compose.runOnIdle {
            val bill=finance.bill(billId!!)!!
            assertEquals(44000L,bill.share("self")); assertEquals(99000L,bill.members.sumOf { bill.share(it.id) })
            assertEquals(4,bill.quantities[1].values.sum())
        }
    }
    @Test fun externalCreatorPaymentWaitsForConfirmation() {
        val finance=FinanceViewModel(); finance.activate("payment-user")
        compose.setContent { BudgetLensTheme { SplitBillDetailScreen(finance.bill("example-dinda"),finance,{}) } }
        compose.onNodeWithText("Tandai Sudah Bayar").performScrollTo().performClick()
        compose.runOnIdle { assertEquals(PaymentStatus.PENDING,finance.bill("example-dinda")!!.members.first { it.self }.status) }
        compose.onNodeWithText("Konfirmasi Penerimaan Dinda").performScrollTo().performClick()
        compose.onNodeWithText("Konfirmasi Diterima").performClick()
        compose.runOnIdle { assertEquals(PaymentStatus.PAID,finance.bill("example-dinda")!!.members.first { it.self }.status) }
        compose.onNodeWithText("Tandai Sudah Bayar").assertDoesNotExist()
    }
    @Test fun transactionSummaryCardsFilterIncomeAndExpense() {
        val finance=FinanceViewModel()
        compose.setContent { BudgetLensTheme { DashboardScreen(DemoUser("Certified","certified@email.com"),{},finance) } }
        compose.onNodeWithText("Transaksi",substring=false).performClick()
        compose.onNodeWithText("MASUK",substring=false).performClick()
        compose.onNodeWithText("Menampilkan pemasukan").assertExists()
        compose.onNodeWithText("Uang Saku Bulanan").assertExists()
        compose.onNodeWithText("Mie Ayam Geprek").assertDoesNotExist()
        compose.onNodeWithText("KELUAR",substring=false).performClick()
        compose.onNodeWithText("Uang Saku Bulanan").assertDoesNotExist()
        compose.onNodeWithText("Mie Ayam Geprek").assertExists()
        compose.onNodeWithText("BERSIH",substring=false).performClick()
        compose.onNodeWithText("Menampilkan pengeluaran").assertDoesNotExist()
    }
}
