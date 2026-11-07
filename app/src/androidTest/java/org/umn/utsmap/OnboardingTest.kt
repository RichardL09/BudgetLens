package org.umn.utsmap

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.assertIsDisplayed
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.screens.OnboardingScreen
import org.umn.utsmap.ui.theme.BudgetLensTheme

class OnboardingTest {
    @get:Rule val compose = createComposeRule()

    @Test fun nextVisitsAllPagesAndFinishesOnlyAtTheEnd() {
        var completions = 0
        compose.setContent { BudgetLensTheme { OnboardingScreen { completions++ } } }
        compose.onNodeWithText("Catat Setiap Transaksi").assertIsDisplayed()
        listOf("Atur Budget Pribadimu", "Split Bill Anti-Ribet", "Scan Struk Instan").forEach { title ->
            compose.onNodeWithText("Lanjut →").performClick()
            compose.waitForIdle()
            compose.onNodeWithText(title).assertIsDisplayed()
        }
        compose.runOnIdle { assertEquals(0, completions) }
        compose.onNodeWithText("Mulai Sekarang").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
    }

    @Test fun skipFinishesFromFirstPage() {
        var completions = 0
        compose.setContent { BudgetLensTheme { OnboardingScreen { completions++ } } }
        compose.onNodeWithText("Lewati").performClick()
        compose.runOnIdle { assertEquals(1, completions) }
    }
}
