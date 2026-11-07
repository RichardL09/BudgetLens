package org.umn.utsmap

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.auth.DemoUser
import org.umn.utsmap.ui.screens.LoginScreen
import org.umn.utsmap.ui.screens.DashboardScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme

class DashboardAndSwipeTest {
    @get:Rule val compose = createComposeRule()
    @Test fun swipeChangesAuthModeBothWays() {
        val auth = AuthViewModel()
        compose.setContent { BudgetLensTheme { LoginScreen(auth, {}) } }
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.85f, height * 0.2f), Offset(width * 0.15f, height * 0.2f))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertTrue(auth.register) }
        compose.onNodeWithText("Buat Akun Baru").assertIsDisplayed()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.15f, height * 0.2f), Offset(width * 0.85f, height * 0.2f))
        }
        compose.waitForIdle()
        compose.runOnIdle { assertFalse(auth.register) }
        compose.onNodeWithText("Selamat Datang").assertIsDisplayed()
    }
    @Test fun dashboardNavigationSearchAndLogoutWork() {
        var loggedOut = false
        compose.setContent { BudgetLensTheme { DashboardScreen(DemoUser("Certified", "certified@email.com"), { loggedOut = true }) } }
        compose.onNodeWithText("Saldo Bersih Bulan Ini").assertIsDisplayed()
        compose.onNodeWithText("Transaksi", substring = false).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("tidakada")
        compose.onNodeWithText("Tidak ada transaksi yang cocok.").assertIsDisplayed()
        compose.onNode(hasSetTextAction()).performTextReplacement("Kopi")
        compose.onNodeWithText("Kopi Tuku").assertIsDisplayed()
        compose.onNodeWithText("Profil", substring = false).performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Keluar dari Akun"))
        compose.onNodeWithText("Keluar dari Akun").performClick()
        compose.onNodeWithText("Batal").performClick()
        compose.runOnIdle { assertFalse(loggedOut) }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Keluar dari Akun"))
        compose.onNodeWithText("Keluar dari Akun").performClick()
        compose.onNodeWithText("Keluar", substring = false).performClick()
        compose.runOnIdle { assertTrue(loggedOut) }
    }
    @Test fun authTabsRemainAtSamePositionWhenModeChanges() {
        val auth=AuthViewModel()
        compose.setContent{BudgetLensTheme{LoginScreen(auth,{})}}
        val before=compose.onNodeWithText("Masuk",substring=false).fetchSemanticsNode().boundsInRoot
        compose.onNodeWithText("Daftar",substring=false).performClick()
        compose.waitForIdle()
        val after=compose.onNodeWithText("Masuk",substring=false).fetchSemanticsNode().boundsInRoot
        assertEquals(before.top,after.top,1f);assertEquals(before.left,after.left,1f)
        compose.onNodeWithText("Masuk",substring=false).performClick()
        compose.waitForIdle()
        assertEquals(before.top,compose.onNodeWithText("Masuk",substring=false).fetchSemanticsNode().boundsInRoot.top,1f)
    }

}
