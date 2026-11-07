package org.umn.utsmap.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.umn.utsmap.ui.auth.DemoUser

/** Halaman akhir sementara untuk mengecek hasil login sebelum dashboard dibuat. */
@Composable
fun WelcomeScreen(user: DemoUser, onLogout: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Text("BudgetLens", style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(24.dp))
            Text("Selamat datang, ${user.name}", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))
            Text(user.email, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(16.dp))
            Text("Berhasil masuk", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(32.dp))
            OutlinedButton(onClick = onLogout) { Text("Keluar dari Akun") }
        }
    }
}

