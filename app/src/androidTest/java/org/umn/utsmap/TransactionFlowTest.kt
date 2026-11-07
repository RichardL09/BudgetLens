package org.umn.utsmap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.navigation.BudgetLensApp
import org.umn.utsmap.ui.theme.BudgetLensTheme

class TransactionFlowTest {
    @get:Rule val compose = createComposeRule()
    @Test fun createThenOpenDifferentTransactionIds() {
        val auth = AuthViewModel()
        val finance = FinanceViewModel()
        compose.setContent { BudgetLensTheme { BudgetLensApp(auth, finance) } }
        compose.waitUntil(6000) { compose.onAllNodesWithText("Lewati").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Lewati").performClick()
        compose.runOnIdle {
            auth.email = "certified@email.com"
            auth.password = "c3rt1f1ed"
            auth.submit()
        }
        compose.waitUntil(6000) { compose.onAllNodesWithText("Saldo Bersih Bulan Ini").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Transaksi", substring = false).performClick()
        compose.onNodeWithText("+ Tambah").performScrollTo().performClick()
        compose.onNodeWithText("Simpan Transaksi").performScrollTo().performClick()
        compose.onNodeWithText("Nominal wajib diisi.").assertExists()
        compose.onNodeWithText("Jumlah (Rp)").performScrollTo().performTextInput("25000")
        compose.onNodeWithText("Nama Toko / Transaksi").performScrollTo().performTextInput("Kantin UTS")
        compose.onNodeWithText("Simpan Transaksi").performScrollTo().performClick()
        compose.onNodeWithText("Detail Transaksi").assertIsDisplayed()
        compose.onNodeWithText("Kantin UTS").assertIsDisplayed()
        compose.runOnIdle { assertEquals(990500L, finance.expenses) }
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithText("Mie Ayam Geprek").performScrollTo().performClick()
        compose.onNodeWithText("Detail Transaksi").assertIsDisplayed()
        compose.onNodeWithText("Rp 28.000").assertIsDisplayed()
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithText("Trans Jogja").performScrollTo().performClick()
        compose.onNodeWithText("Rp 4.500").assertIsDisplayed()
        compose.onNodeWithText("Mie Ayam Geprek").assertDoesNotExist()
    }
}

