package org.umn.utsmap

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.auth.DemoUser
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File

class FigmaVisualReviewTest {
    @get:Rule val compose = createComposeRule()
    private fun capture(name: String) {
        compose.waitForIdle()
        val dir = InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)
        File(dir, "figma-$name.png").outputStream().use {
            compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun captureExistingScreensForFigmaComparison() {
        val screen = mutableStateOf(0)
        val dark = mutableStateOf(false)
        val auth = AuthViewModel()
        val finance = FinanceViewModel()
        compose.setContent {
            BudgetLensTheme(darkTheme = dark.value) {
                when (screen.value) {
                    0 -> SplashScreen()
                    1 -> OnboardingScreen {}
                    2 -> LoginScreen(auth) {}
                    3 -> TransactionFormScreen(finance, {}, {})
                    4 -> DashboardScreen(DemoUser("Certified", "certified@email.com"), {}, finance)
                    else -> TransactionDetailScreen(finance.find("t1"), onBack = {})
                }
            }
        }
        capture("splash")
        compose.runOnIdle { screen.value = 1 }
        capture("intro-1")
        repeat(3) { index ->
            compose.onNodeWithText("Lanjut →").performClick()
            capture("intro-${index + 2}")
        }
        compose.runOnIdle { screen.value = 2 }
        capture("login")
        compose.onNodeWithText("Daftar").performClick()
        capture("register")
        compose.runOnIdle { screen.value = 3 }
        capture("form")
        compose.runOnIdle { screen.value = 4 }
        capture("home")
        listOf("Transaksi", "Budget", "Grup", "Profil").forEach {
            compose.onNodeWithText(it, substring = false).performClick()
            capture(it.lowercase())
        }
        compose.runOnIdle { screen.value = 5 }
        capture("detail")
        compose.runOnIdle { screen.value = 3; dark.value = true }
        capture("form-dark")
        compose.onNodeWithText("↑ Pemasukan").performClick()
        capture("income-dark")
    }
}

