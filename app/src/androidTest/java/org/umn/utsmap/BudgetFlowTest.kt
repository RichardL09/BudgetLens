package org.umn.utsmap

import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.screens.BudgetScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File
import java.time.LocalDate
import java.time.YearMonth

class BudgetFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "budget-$name.png")
        file.outputStream().use {
            InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun pickPeriodOpenCategoryEditAndAddFromSheet() {
        val finance = FinanceViewModel()
        finance.selectTransactionMonth(YearMonth.of(2025, 8))
        compose.setContent { BudgetLensTheme { BudgetScreen(finance, {}) } }
        compose.onNode(hasText("▾", substring = true)).performClick()
        compose.onNodeWithText("Tahun").performTextReplacement("2026")
        compose.onNode(hasText("Okt", substring = false) and SemanticsMatcher.keyIsDefined(SemanticsProperties.Selected)).performClick()
        compose.onNodeWithText("Terapkan").performClick()
        compose.runOnIdle {
            assertEquals(YearMonth.of(2026, 10), finance.budgetMonth)
            assertEquals(YearMonth.of(2025, 8), finance.month)
            assertEquals(YearMonth.now(), finance.homeMonth)
        }
        capture("overview")
        compose.onNodeWithText("Makanan & Minum").performScrollTo().performClick()
        compose.onNodeWithText("Rincian Budget").assertIsDisplayed()
        compose.onNodeWithText("Mie Ayam Geprek").assertExists()
        capture("detail")
        compose.onNodeWithText("Edit", substring = false).performClick()
        compose.onNodeWithText("Nama budget").performTextReplacement("Makan & Minum")
        compose.onNodeWithText("Nominal Budget").performTextReplacement("600000")
        capture("edit-sheet")
        compose.onNodeWithText("Simpan").performScrollTo().performClick()
        compose.waitUntil(5000) { finance.budgets.first().category == "Makan & Minum" }
        compose.onAllNodesWithText("Makan & Minum", substring = false).assertCountEquals(4)
        compose.runOnIdle { finance.selectBudgetDate(LocalDate.of(2026, 10, 15)) }
        compose.onNodeWithText("Kopi Tuku").assertDoesNotExist()
        compose.onNodeWithText("Mie Ayam Geprek").assertExists()
        capture("date-filter")
        compose.onNodeWithContentDescription("Kembali").performClick()
        compose.onNodeWithText("+ Tambah Budget").performClick()
        compose.onNodeWithText("Simpan").performClick()
        compose.onNodeWithText("Nama budget wajib diisi.").assertExists()
        compose.onNodeWithText("Nama budget").performTextInput("Hobi")
        compose.onNodeWithText("Nominal Budget").performTextInput("250000")
        capture("add-sheet")
        compose.onNodeWithText("Simpan").performScrollTo().performClick()
        compose.waitUntil(5000) { finance.budgets.any { it.category == "Hobi" } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Hobi"))
        compose.onNodeWithText("Hobi").performClick()
        compose.onNodeWithText("Belum ada transaksi untuk kategori dan periode ini.").assertExists()
    }
}
