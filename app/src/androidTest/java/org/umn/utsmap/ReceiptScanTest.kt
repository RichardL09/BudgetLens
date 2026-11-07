package org.umn.utsmap

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.ui.screens.ReceiptScanScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File

class ReceiptScanTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val instrument = InstrumentationRegistry.getInstrumentation()
        File(instrument.targetContext.getExternalFilesDir(null), "scan-$name.png").outputStream().use {
            val bitmap = requireNotNull(instrument.uiAutomation.takeScreenshot())
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            bitmap.recycle()
        }
    }
    @Test fun captureProcessEditAndSaveOnce() {
        val instrument = InstrumentationRegistry.getInstrumentation()
        instrument.uiAutomation.executeShellCommand("pm grant ${instrument.targetContext.packageName} android.permission.CAMERA").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        val scan = ReceiptScanViewModel()
        val finance = FinanceViewModel()
        var saved: String? = null
        compose.setContent { BudgetLensTheme { ReceiptScanScreen(scan, finance, {}, { saved = it }) } }
        capture("camera")
        compose.runOnIdle {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            val photo = File(context.cacheDir, "test-receipt.png")
            photo.outputStream().use { Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, it) }
            scan.selectedImage = android.net.Uri.fromFile(photo).toString()
        }
        compose.onNodeWithContentDescription("Foto struk").performClick()
        compose.onNodeWithText("Memproses Struk...").assertIsDisplayed()
        compose.waitUntil(5000) { scan.progress >= 60 }
        capture("processing")
        compose.waitUntil(6000) { scan.stage == ScanStage.RESULT }
        compose.onNodeWithText("Indomaret Seturan").assertExists()
        capture("result")
        compose.onNodeWithText("Edit", substring = false).performClick()
        compose.onNodeWithText("Nama toko").performTextReplacement("Indomaret Kampus")
        compose.onNodeWithText("Tanggal struk").performTextReplacement("2026-10-16")
        compose.onNodeWithText("Terapkan Perubahan").performScrollTo().performClick()
        compose.onNodeWithText("Simpan Transaksi").performScrollTo().performClick()
        compose.runOnIdle {
            assertNotNull(saved)
            assertEquals(42900L, finance.find(saved!!)?.amount)
            assertEquals("Indomaret Kampus", finance.find(saved!!)?.title)
            assertEquals("2026-10-16", finance.find(saved!!)?.date)
            assertEquals(739400L, finance.expenses)
            assertEquals(saved, scan.save(finance))
            assertEquals(11, finance.transactions.size)
            assertEquals(4, finance.find(saved!!)!!.receipt!!.items.size)
        }
    }
    @Test fun embeddedCameraCapturesWithoutOpeningAnotherApp() {
        val instrument = InstrumentationRegistry.getInstrumentation()
        instrument.uiAutomation.executeShellCommand("pm grant ${instrument.targetContext.packageName} android.permission.CAMERA").use {
            android.os.ParcelFileDescriptor.AutoCloseInputStream(it).readBytes()
        }
        val monitor = instrument.addMonitor(android.content.IntentFilter(android.provider.MediaStore.ACTION_IMAGE_CAPTURE),
            android.app.Instrumentation.ActivityResult(android.app.Activity.RESULT_CANCELED, null), true)
        try {
            val scan = ReceiptScanViewModel()
            val finance = FinanceViewModel()
            compose.setContent { BudgetLensTheme { ReceiptScanScreen(scan, finance, {}, {}) } }
            compose.waitUntil(20000) {
                compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Siap"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
            capture("live-preview")
            compose.onNodeWithContentDescription("Foto struk").performClick()
            compose.waitUntil(20000) { scan.selectedImage != null && scan.stage != ScanStage.CAMERA }
            compose.runOnIdle {
                assertEquals(0, monitor.hits)
                val uri = android.net.Uri.parse(scan.selectedImage!!)
                val bytes = instrument.targetContext.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
                assertTrue(bytes.size > 1000)
                val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                assertNotNull(bitmap)
                assertTrue(bitmap.width > 0 && bitmap.height > 0)
                scan.retake()
                assertNull(scan.selectedImage)
                assertEquals(ScanStage.CAMERA, scan.stage)
            }
            compose.waitUntil(20000) {
                compose.onAllNodes(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.StateDescription, "Siap"))
                    .fetchSemanticsNodes().isNotEmpty()
            }
        } finally { instrument.removeMonitor(monitor) }
    }
    @Test fun perItemBillHasListAndPersonDetails() {
        val scan = ReceiptScanViewModel()
        val finance = FinanceViewModel()
        val auth = org.umn.utsmap.ui.auth.AuthViewModel()
        var saved: String? = null
        compose.setContent { BudgetLensTheme {
            if (scan.stage == ScanStage.RESULT) org.umn.utsmap.ui.screens.SplitBillCreateScreen(scan, finance, auth, {}, { saved = it })
            else androidx.compose.material3.Text("Menyiapkan struk")
        } }
        compose.runOnIdle { scan.capture() }
        compose.waitUntil(6000) { scan.stage == ScanStage.RESULT }
        compose.onNodeWithText("+ Tambah").performClick()
        compose.onNodeWithText("Nama Manual").performClick()
        compose.onNodeWithText("Nama anggota").performTextInput("Dinda")
        compose.onNodeWithText("Tambah", substring = false).performClick()
        compose.onNodeWithText("Jumlah Aqua 600ml - Saya").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Jumlah Aqua 600ml - Dinda").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Simpan Split Bill").performScrollTo().performClick()
        compose.runOnIdle {
            val bill = finance.bill(saved!!)!!
            assertEquals(31900L, bill.share("self"))
            assertEquals(4, bill.allocations.size)
            assertEquals(31900L, finance.find(bill.transactionId!!)!!.amount)
            assertEquals(saved, scan.saveBill(finance, bill.members, bill.method, emptyList(), emptyMap()))
            assertEquals(1, finance.splitBills.size)
        }
    }
}
