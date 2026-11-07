package org.umn.utsmap

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.screens.LoginScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme

class AuthFlowTest {
    @get:Rule val compose = createComposeRule()

    @Test fun invalidFormIsBlockedAndDemoLoginSucceeds() {
        val auth = AuthViewModel()
        compose.setContent { BudgetLensTheme { LoginScreen(auth, {}) } }
        compose.onNodeWithText("Masuk ke BudgetLens").performScrollTo().performClick()
        compose.onNodeWithText("Email wajib diisi.").assertExists()
        compose.onNodeWithText("Password wajib diisi.").assertExists()
        compose.runOnIdle { assertNull(auth.user) }
        compose.onNodeWithText("Email").performScrollTo().performTextInput("certified@email.com")
        compose.onNode(hasSetTextAction() and hasText("Password")).performScrollTo().performTextInput("c3rt1f1ed")
        compose.onNodeWithText("Masuk ke BudgetLens").performScrollTo().performClick()
        compose.waitUntil(5000) { auth.user != null }
        compose.runOnIdle { assertEquals("Certified", auth.user?.name) }
    }

    @Test fun registerRejectsMismatchAndAccountCanLoginAfterLogout() {
        val auth = AuthViewModel()
        compose.setContent { BudgetLensTheme { LoginScreen(auth, {}) } }
        compose.onNodeWithText("Daftar").performClick()
        compose.onNodeWithText("Nama Lengkap").performScrollTo().performTextInput("Michael")
        compose.onNodeWithText("Email").performScrollTo().performTextInput("michael@example.com")
        compose.onNode(hasSetTextAction() and hasText("Password")).performScrollTo().performTextInput("password123")
        compose.onNodeWithText("Konfirmasi Password").performScrollTo().performTextInput("different123")
        compose.onNodeWithText("Buat Akun", substring = false).performScrollTo().performClick()
        compose.onNodeWithText("Konfirmasi password belum sama.").assertExists()
        compose.runOnIdle { assertNull(auth.user) }
        compose.onNodeWithText("Konfirmasi Password").performScrollTo().performTextReplacement("password123")
        compose.onNodeWithText("Buat Akun", substring = false).performScrollTo().performClick()
        compose.waitUntil(5000) { auth.user != null }
        compose.runOnIdle {
            assertEquals("Michael", auth.user?.name)
            auth.logout()
            auth.email = "michael@example.com"
            auth.password = "wrong123"
            auth.submit()
        }
        compose.waitUntil(5000) { !auth.loading }
        compose.runOnIdle {
            assertNull(auth.user)
            assertEquals("Email atau password salah.", auth.error)
            auth.password = "password123"
            auth.submit()
        }
        compose.waitUntil(5000) { auth.user != null }
        compose.runOnIdle { assertEquals("Michael", auth.user?.name) }
    }
}

