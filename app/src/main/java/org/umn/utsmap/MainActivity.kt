package org.umn.utsmap

import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.finance.ReceiptScanViewModel
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import org.umn.utsmap.ui.navigation.BudgetLensApp
import org.umn.utsmap.ui.theme.BudgetLensTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val auth = ViewModelProvider(this)[AuthViewModel::class.java]
        val finance = ViewModelProvider(this)[FinanceViewModel::class.java]
        val scan = ViewModelProvider(this)[ReceiptScanViewModel::class.java]
        auth.loadSettings(this)
        auth.loadPreferences(this)
        setContent {
            BudgetLensTheme(darkTheme = when (auth.themeMode) { "dark" -> true; "light" -> false; else -> androidx.compose.foundation.isSystemInDarkTheme() }) { BudgetLensApp(auth, finance, scan) }
        }
    }
}


