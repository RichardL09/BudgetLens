package org.umn.utsmap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme

class FormStateRecoveryTest {
    @get:Rule val compose = createAndroidComposeRule<androidx.activity.ComponentActivity>()

    @Test fun splitMembersMethodsPercentagesAndCountsSurviveRestoration() {
        val finance = FinanceViewModel(); val auth = AuthViewModel(); val scan = ReceiptScanViewModel()
        scan.beginManual(); scan.taxPercent = 0; assertTrue(scan.updateReceipt("Belanja Bersama", "2026-10-22", listOf(ReceiptItem("Sabun", 4, 5000))))
        var saved: String? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { BudgetLensTheme { SplitBillCreateScreen(scan, finance, auth, {}, { saved = it }) } }
        compose.onNodeWithText("+ Tambah").performClick()
        compose.onNodeWithText("Marcelline", substring=false).performClick()
        compose.onNodeWithText("Jumlah Sabun - Saya").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Jumlah Sabun - Marcelline").performTextReplacement("3")
        compose.onNodeWithText("Persen", substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Persen Saya").performScrollTo().performTextReplacement("25")
        compose.onNodeWithText("Persen Marcelline").performTextReplacement("75")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Persen", substring=false).assertIsSelected()
        compose.onNodeWithText("Persen Saya").performScrollTo().assertTextContains("25")
        compose.onNodeWithText("Persen Marcelline").assertTextContains("75")
        compose.onNodeWithText("Per Item", substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Jumlah Sabun - Saya").performScrollTo().assertTextContains("1")
        compose.onNodeWithText("Jumlah Sabun - Marcelline").assertTextContains("3")
        compose.onNodeWithText("Simpan Split Bill").performScrollTo().performClick()
        compose.runOnIdle {
            val bill = finance.bill(saved!!)!!
            assertEquals(2, bill.members.size)
            assertEquals(5000L, bill.share("self"))
            assertEquals(4, bill.quantities.single().values.sum())
        }
    }

    @Test fun groupDraftRestoresAndBothBackControlsReturnToManualStep() {
        val finance = FinanceViewModel(); finance.activate("state-recovery")
        var exited = false; var saved: String? = null
        val restoration = StateRestorationTester(compose)
        restoration.setContent { BudgetLensTheme { GroupExpenseScreen("g1", null, finance, { exited=true }, { saved=it }) } }
        compose.onNodeWithText("Nama tagihan").performTextInput("Kebutuhan Kos")
        compose.onNodeWithText("Nama item 1").performScrollTo().performTextInput("Sabun")
        compose.onNodeWithText("Jumlah item 1").performTextReplacement("4")
        compose.onNodeWithText("Harga satuan item 1").performTextReplacement("10000")
        compose.onNodeWithText("PPN (%)").performScrollTo().performTextReplacement("10")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Per Item", substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Jumlah Sabun - Kamu").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Jumlah Sabun - Marcelline").performTextReplacement("1")
        compose.onNodeWithText("Jumlah Sabun - Richard").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Simpan Pengeluaran Grup").performScrollTo().performClick()
        compose.runOnIdle { assertNull(saved) }
        compose.onNodeWithText("Jumlah pembagian harus 4; saat ini 3.").assertExists()
        compose.onNodeWithText("Jumlah Sabun - Richard").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Periksa pembagian jumlah barang",substring=true).assertDoesNotExist()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Kebutuhan Kos").performScrollTo().assertExists()
        compose.onNodeWithText("Jumlah Sabun - Richard").performScrollTo().assertTextContains("2")
        compose.onNodeWithContentDescription("Kembali").performScrollTo().performClick()
        compose.onNodeWithText("Nama tagihan").performScrollTo().assertTextContains("Kebutuhan Kos")
        compose.onNodeWithText("Jumlah item 1").performScrollTo().assertTextContains("4")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Jumlah Sabun - Richard").performScrollTo().assertTextContains("2")
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Nama tagihan").performScrollTo().assertTextContains("Kebutuhan Kos")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Simpan Pengeluaran Grup").performScrollTo().performClick()
        compose.runOnIdle {
            assertFalse(exited)
            assertNotNull(saved)
            assertEquals(11000L, finance.bill(saved!!)!!.share("self"))
        }
    }

    @Test fun manualFieldsIdentifyErrorsAndBlockSubmissionUntilCorrected() {
        val scan = ReceiptScanViewModel(); scan.beginManual(); var continued = false
        compose.setContent { BudgetLensTheme { ManualBillScreen(scan, {}, { continued=true }) } }
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Nama transaksi wajib diisi.").assertExists()
        compose.onNodeWithText("Nama item wajib diisi.").assertExists()
        compose.onNodeWithText("Nama tagihan").performScrollTo().performTextInput("Belanja")
        compose.onNodeWithText("Nama item 1").performScrollTo().performTextInput("Sabun")
        compose.onNodeWithText("Jumlah item 1").performTextReplacement("2x")
        compose.onNodeWithText("Harga satuan item 1").performTextReplacement("abc")
        compose.onNodeWithText("PPN (%)").performScrollTo().performTextReplacement("101")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Jumlah hanya boleh berisi angka.").assertExists()
        compose.onNodeWithText("Harga satuan hanya boleh berisi angka.").assertExists()
        compose.runOnIdle { assertFalse(continued) }
        compose.onNodeWithText("Jumlah item 1").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Harga satuan item 1").performTextReplacement("10000")
        compose.onNodeWithText("PPN (%)").performScrollTo().performTextReplacement("0")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.runOnIdle { assertTrue(continued); assertEquals(20000L, scan.total) }
    }

    @Test fun profileErrorsBelongToTheirFieldsAndClearAfterCorrection() {
        val auth = AuthViewModel(); val finance = FinanceViewModel()
        compose.setContent { BudgetLensTheme { if(auth.user != null) AccountScreen("edit",auth,finance,{}, {}) } }
        compose.runOnIdle { auth.email="certified@email.com"; auth.password="c3rt1f1ed"; auth.submit() }
        compose.waitUntil(3000) { auth.user != null }
        compose.onNodeWithText("Nama lengkap").performTextReplacement("")
        compose.onNodeWithText("Nomor telepon",substring=false).performTextReplacement("12")
        compose.onNodeWithText("Simpan Profil").performScrollTo().performClick()
        compose.onNodeWithText("Nama lengkap wajib diisi.").assertExists()
        compose.onNodeWithText("Nomor telepon tidak valid.",substring=true).assertExists()
        compose.runOnIdle { assertEquals("Certified",auth.user!!.name) }
        compose.onNodeWithText("Nama lengkap").performScrollTo().performTextReplacement("Nama Valid")
        compose.onNodeWithText("Nomor telepon",substring=false).performTextReplacement("081234567890")
        compose.onNodeWithText("Simpan Profil").performScrollTo().performClick()
        compose.onNodeWithText("Profil berhasil diperbarui.").assertExists()
        compose.onNodeWithText("Nama lengkap wajib diisi.").assertDoesNotExist()
        compose.runOnIdle { assertEquals("Nama Valid",auth.user!!.name) }
    }
    @Test fun passwordDraftSurvivesRestorationAndKeepsExistingValidation() {
        val auth = AuthViewModel(); val finance = FinanceViewModel()
        val restoration = StateRestorationTester(compose)
        restoration.setContent { BudgetLensTheme { if(auth.user != null) AccountScreen("password",auth,finance,{}, {}) } }
        compose.runOnIdle { auth.email="certified@email.com"; auth.password="c3rt1f1ed"; auth.submit() }
        compose.waitUntil(3000) { auth.user != null }
        compose.onNodeWithText("Password saat ini").performTextInput("c3rt1f1ed")
        compose.onNodeWithText("Password baru",substring=false).performTextInput("newpass123")
        compose.onNodeWithText("Konfirmasi password baru").performTextInput("newpass123")
        restoration.emulateSavedInstanceStateRestore()
        compose.onNode(hasText("Ganti Password") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Password berhasil diganti.").assertExists()
        compose.runOnIdle { auth.logout(); auth.email="certified@email.com"; auth.password="newpass123"; auth.submit() }
        compose.waitUntil(3000) { auth.user != null }
    }

}
