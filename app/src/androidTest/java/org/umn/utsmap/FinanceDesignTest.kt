package org.umn.utsmap

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.DemoUser
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.screens.DashboardScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File

class FinanceDesignTest {
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name: String) {
        compose.waitForIdle()
        val file = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "$name.png")
        file.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun budgetAndProfileChangesUpdateUi() {
        val finance = FinanceViewModel()
        val user = mutableStateOf(DemoUser("Certified", "certified@email.com"))
        val dark = mutableStateOf(false)
        compose.setContent { BudgetLensTheme(darkTheme = dark.value) { DashboardScreen(user.value, {}, finance, onEditProfile = { user.value = user.value.copy(name = it) }) } }
        compose.onNodeWithText("Budget", substring = false).performClick()
        compose.onNodeWithText("Total Terpakai").assertIsDisplayed()
        screenshot("budget-light")
        compose.onNodeWithText("+ Tambah Budget").performClick()
        compose.onNodeWithText("Simpan").performClick()
        compose.onNodeWithText("Nominal wajib diisi.").assertExists()
        compose.onNodeWithText("Nama budget").performTextInput("Belanja")
        compose.onNodeWithText("Nominal Budget").performTextInput("500000")
        compose.onNodeWithText("Simpan").performClick()
        compose.runOnIdle { assertEquals(500000L, finance.budgets.first { it.category == "Belanja" }.limit) }
        compose.onNodeWithText("Transaksi", substring = false).performClick()
        compose.onNodeWithText("BERSIH").assertIsDisplayed()
        screenshot("transactions-light")
        compose.onNodeWithText("Profil", substring = false).performClick()
        screenshot("profile-light")
        compose.onNodeWithText("Target Tabungan", substring = false).assertDoesNotExist()
        compose.onNodeWithText("Metode Pembayaran", substring = false).assertDoesNotExist()
        compose.runOnIdle { dark.value = true }
        compose.onNodeWithText("Transaksi", substring = false).performClick()
        screenshot("transactions-dark")
        compose.onNodeWithText("Budget", substring = false).performClick()
        screenshot("budget-dark")
        compose.onNodeWithText("Profil", substring = false).performClick()
        screenshot("profile-dark")
    }
}
