package org.umn.utsmap

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme
import org.umn.utsmap.data.*
import java.io.File

class AccountSettingsTest {
    @get:Rule val compose = createComposeRule()
    private fun login(auth: AuthViewModel) {
        compose.runOnIdle { auth.email = "certified@email.com"; auth.password = "c3rt1f1ed"; auth.submit() }
        compose.waitUntil(3000) { auth.user != null }
    }
    private fun screenshot(name: String) {
        val root = File(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null), "$name.png")
        root.outputStream().use { compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it) }
    }
    @Test fun profileEmailAndPasswordKeepFinancialAccount() {
        val auth = AuthViewModel(); val finance = FinanceViewModel()
        var section by mutableStateOf("info")
        compose.setContent { BudgetLensTheme { if (auth.user == null) androidx.compose.material3.Text("Masuk") else AccountScreen(section, auth, finance, {}, { section = it }) } }
        login(auth)
        compose.runOnIdle { finance.activate(auth.user!!.id) }
        compose.onNodeWithText("Password", substring=false).assertExists()
        compose.onNodeWithText("Edit Foto, Nama & Nomor Telepon").performScrollTo().performClick()
        compose.onNodeWithText("Nama lengkap").performTextReplacement("Nama Baru")
        compose.onNodeWithText("Nomor telepon", substring=false).performTextReplacement("081234009999")
        compose.onNodeWithText("Simpan Profil").performScrollTo().performClick()
        compose.onNodeWithText("Profil berhasil diperbarui.").assertExists()
        screenshot("account-edit")
        compose.runOnIdle { section = "email" }
        compose.onNodeWithText("Email", substring=false).performTextReplacement("baru@example.com")
        compose.onNodeWithText("Password saat ini").performTextInput("c3rt1f1ed")
        compose.onNodeWithText("Minta Kode Verifikasi").performScrollTo().performClick()
        compose.onNodeWithText("Kode verifikasi", substring=false).performTextInput("000000")
        compose.onNodeWithText("Verifikasi Email", substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Kode verifikasi salah.").assertExists()
        compose.onNodeWithText("Kode verifikasi", substring=false).performTextReplacement("123456")
        compose.onNodeWithText("Verifikasi Email", substring=false).performScrollTo().performClick()
        compose.runOnIdle {
            assertEquals("baru@example.com", auth.user!!.email); assertTrue(auth.user!!.verified)
            finance.activate(auth.user!!.id); assertEquals(14,finance.transactions.size)
            section = "password"
        }
        compose.onNodeWithText("Password saat ini").performTextReplacement("salah")
        compose.onNodeWithText("Password baru",substring=false).performTextInput("password-baru")
        compose.onNodeWithText("Konfirmasi password baru").performTextInput("password-baru")
        compose.onNode(hasText("Ganti Password") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Password saat ini salah.").assertExists()
        compose.onNodeWithText("Password saat ini").performTextReplacement("c3rt1f1ed")
        compose.onNode(hasText("Ganti Password") and hasClickAction()).performScrollTo().performClick()
        compose.onNodeWithText("Password berhasil diganti.").assertExists()
        compose.runOnIdle { auth.logout(); auth.email="baru@example.com"; auth.password="password-baru"; auth.submit() }
        compose.waitUntil(3000) { auth.user != null }
        assertEquals("Nama Baru",auth.user!!.name)
    }
    @Test fun pdfExportProducesPaginatedReadableDocument() {
        compose.setContent { androidx.compose.material3.Text("PDF") }
        val root=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)!!
        val file=File(root,"budgetlens-report.pdf")
        val entries=(1..80).map { DemoFinance.transactions[(it-1)%10].copy(id="export-$it",title="Transaksi $it dengan catatan panjang untuk memeriksa tata letak laporan") }
        file.outputStream().use { FinancePdf.write(it,"Certified","Oktober 2026",entries) }
        assertTrue(file.length()>1000)
        assertEquals("%PDF",file.inputStream().use { String(it.readNBytes(4)) })
        PdfRenderer(ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY)).use { renderer ->
            assertTrue(renderer.pageCount>1)
            renderer.openPage(0).use { page ->
                val bitmap=Bitmap.createBitmap(1190,1684,Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap,null,null,PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                File(root,"pdf-report-preview.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG,100,it) }
            }
            renderer.openPage(renderer.pageCount-1).use { page -> assertTrue(page.width>0) }
        }
    }
    @Test fun settingsNotificationsAndTransactionDirection() {
        val auth=AuthViewModel(); val finance=FinanceViewModel(); var section by mutableStateOf("settings")
        val context=InstrumentationRegistry.getInstrumentation().targetContext
        compose.setContent { BudgetLensTheme(darkTheme=auth.themeMode=="dark") { if(auth.user==null) androidx.compose.material3.Text("Masuk") else AccountScreen(section,auth,finance,{}, { section=it }) } }
        login(auth)
        compose.runOnIdle { auth.changeTheme("dark",context) }
        assertEquals("dark",auth.themeMode)
        screenshot("settings-dark")
        compose.runOnIdle { section="notifications" }
        compose.onAllNodes(isToggleable()).onFirst().performClick()
        assertFalse(finance.budgetAlerts)
        screenshot("notifications-page")
        compose.runOnIdle { auth.changeTheme("system",context) }
    }
}
