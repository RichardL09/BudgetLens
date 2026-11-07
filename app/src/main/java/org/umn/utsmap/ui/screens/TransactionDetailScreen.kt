package org.umn.utsmap.ui.screens



import androidx.compose.foundation.layout.*

import androidx.compose.foundation.rememberScrollState

import androidx.compose.foundation.verticalScroll

import androidx.compose.material3.*

import androidx.compose.runtime.*

import androidx.compose.runtime.saveable.rememberSaveable

import androidx.compose.ui.Modifier

import androidx.compose.ui.unit.dp

import org.umn.utsmap.data.Transaction

import org.umn.utsmap.data.rupiah



@Composable

fun TransactionDetailScreen(transaction: Transaction?, onBack: () -> Unit, onDelete: () -> Unit = {}, onSplitDetail: (String) -> Unit = {}, confirmDeletion: Boolean = true) {

    var confirmDelete by rememberSaveable { mutableStateOf(false) }

    Surface(color = MaterialTheme.colorScheme.background) {

        Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(20.dp),

            verticalArrangement = Arrangement.spacedBy(20.dp)) {

            AppBackButton(onBack)

            Text("Detail Transaksi", style = MaterialTheme.typography.headlineSmall)

            if (transaction == null) {

                Text("Transaksi tidak ditemukan.")

            } else {

                val color = if (transaction.income) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error

                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = MaterialTheme.shapes.large) {

                    Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        Text(if (transaction.income) "Pemasukan" else "Pengeluaran", color = color)

                        Text(rupiah(transaction.amount), style = MaterialTheme.typography.headlineLarge, color = color)

                        Text(transaction.title, style = MaterialTheme.typography.titleLarge)

                    }

                }

                DetailRow("Kategori", transaction.category)

                DetailRow("Tanggal", transaction.date)

                DetailRow("Catatan", transaction.note.ifBlank { "Tidak ada catatan" })

                transaction.receipt?.let { receipt ->

                    Text("Rincian Struk", style = MaterialTheme.typography.titleLarge)

                    receipt.items.forEach { item ->

                        DetailRow(item.name + " ×" + item.quantity, "${rupiah(item.unitPrice)} / item • ${rupiah(item.total)}")

                    }

                    DetailRow("Subtotal struk", rupiah(receipt.subtotal))

                    DetailRow("PPN", rupiah(receipt.tax))

                    DetailRow("Total struk", rupiah(receipt.total))

                    if (transaction.split) DetailRow("Bagian pengeluaranmu", rupiah(transaction.amount))

                }

                transaction.splitBillId?.let { id -> OutlinedButton(onClick = { onSplitDetail(id) }, modifier = Modifier.fillMaxWidth()) { Text("Lihat Rincian Split Bill") } }

                OutlinedButton(onClick = { if(confirmDeletion) confirmDelete = true else onDelete() }, modifier = Modifier.fillMaxWidth(),

                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) { Text("Hapus Transaksi") }

                if (transaction.split) Text("Bagian pribadi dari transaksi bersama.", color = MaterialTheme.colorScheme.primary)

            }

        }

    }

    if (confirmDelete && transaction != null) AlertDialog(onDismissRequest = { confirmDelete = false }, title = { Text("Hapus transaksi?") },

        text = { Text("${transaction.title} akan dihapus dan ringkasan keuangan diperbarui. Catatan split bill tetap tersedia.") },

        confirmButton = { TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Hapus") } },

        dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Batal") } })

}



@Composable

private fun DetailRow(label: String, value: String) {

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)

        Text(value, style = MaterialTheme.typography.bodyLarge)

        HorizontalDivider(Modifier.padding(top = 12.dp))

    }

}



