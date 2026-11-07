package org.umn.utsmap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import android.view.KeyEvent
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File

class SplitFlowTest {
    @get:Rule val compose = createComposeRule()
    private fun screenshot(name: String) {
        val instrument = InstrumentationRegistry.getInstrumentation()
        File(instrument.targetContext.getExternalFilesDir(null), "$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun uploadOpensPickerAndCancelDoesNotProcess() {
        val scan = ReceiptScanViewModel()
        compose.setContent { BudgetLensTheme { ReceiptScanScreen(scan, FinanceViewModel(), {}, {}) } }
        compose.onNodeWithContentDescription("Upload gambar struk").performClick()
        val instrument = InstrumentationRegistry.getInstrumentation()
        compose.waitUntil(8000) {
            val activePackage = instrument.uiAutomation.rootInActiveWindow?.packageName?.toString().orEmpty()
            activePackage.contains("photopicker") || activePackage.contains("documentsui")
        }
        File(instrument.targetContext.getExternalFilesDir(null), "upload-picker.png").outputStream().use {
            instrument.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        assertEquals(ScanStage.CAMERA, scan.stage)
        assertEquals(0, scan.progress)
        InstrumentationRegistry.getInstrumentation().sendKeyDownUpSync(KeyEvent.KEYCODE_BACK)
        android.os.SystemClock.sleep(800)
        File(instrument.targetContext.getExternalFilesDir(null), "upload-return.png").outputStream().use {
            instrument.uiAutomation.takeScreenshot().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        compose.waitUntil(4000) {
            compose.onAllNodesWithContentDescription("Foto struk").fetchSemanticsNodes(atLeastOneRootRequired = false).isNotEmpty()
        }
        compose.onNodeWithContentDescription("Foto struk").assertExists()
        assertNull(scan.selectedImage)
    }
    @Test fun itemDetailsDeleteWithConfirmation() {
        val finance = FinanceViewModel()
        val receipt = ReceiptScanViewModel().receiptDetails()
        val id = finance.save("Indomaret", "42900", "Makanan", "2026-10-22", false, "", receipt = receipt)!!
        var deleted = false
        compose.setContent { BudgetLensTheme { TransactionDetailScreen(finance.find(id), {}, { deleted = finance.delete(id) }) } }
        compose.onNodeWithText("Rincian Struk").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Aqua 600ml ×2").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Hapus Transaksi").performScrollTo().performClick()
        compose.onNodeWithText("Batal").performClick()
        assertNotNull(finance.find(id))
        compose.onNodeWithText("Hapus Transaksi").performScrollTo().performClick()
        compose.onNodeWithText("Hapus", substring = false).performClick()
        assertTrue(deleted)
        assertNull(finance.find(id))
        assertEquals(696500L, finance.expenses)
    }
    @Test fun listShowsItemAndPersonBreakdown() {
        val finance = FinanceViewModel()
        val members = listOf(BillMember("self", "Rizky", self = true, paid = true), BillMember("dinda", "Dinda"))
        val receipt = ReceiptScanViewModel().receiptDetails()
        val bill = BillCalculator.build("Indomaret Seturan", "2026-10-22", receipt, members, SplitMethod.ITEMS,
            listOf(setOf("self"), setOf("dinda"), setOf("self", "dinda"), setOf("dinda")), emptyMap())!!
        finance.addBill(bill)
        var selected by mutableStateOf<String?>(null)
        compose.setContent { BudgetLensTheme {
            if (selected == null) SplitBillListScreen(finance, AuthViewModel(), {}, {}, { selected = it })
            else SplitBillDetailScreen(finance.bill(selected!!), finance, { selected = null })
        } }
        compose.onNodeWithText("Indomaret Seturan").performScrollTo().performClick()
        screenshot("split-detail")
        compose.onAllNodesWithText("Dinda", substring = false).onFirst().performScrollTo().performClick()
        compose.onNodeWithText("Rincian Per Orang").assertExists()
        compose.onNodeWithText("Indomie Goreng").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Aqua 600ml").assertDoesNotExist()
        screenshot("split-person")
    }
    @Test fun registeredPhoneLookupRejectsUnknownAndDuplicates() {
        val auth = AuthViewModel()
        compose.setContent { androidx.compose.material3.Text("Akun") }
        compose.runOnIdle {
            auth.switchMode(true); auth.name = "Dinda"; auth.email = "dinda@example.com"; auth.phone = "081234567890"
            auth.password = "password1"; auth.confirmation = "password1"; auth.submit()
        }
        compose.waitUntil(3000) { auth.user != null }
        compose.runOnIdle {
            auth.logout(); auth.email = "certified@email.com"; auth.password = "c3rt1f1ed"; auth.submit()
        }
        compose.waitUntil(3000) { auth.user != null }
        compose.runOnIdle {
            assertEquals("Dinda", auth.findByPhone("+62 812 3456 7890")?.name)
            assertNull(auth.findByPhone("081111111111"))
            assertNotNull(auth.updatePhone("081234567890"))
            assertNull(auth.updatePhone("081999999999"))
            assertNull(auth.findByPhone("081999999999"))
        }
    }
}
