package org.umn.utsmap

import androidx.compose.runtime.*
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.test.platform.app.InstrumentationRegistry
import android.graphics.Bitmap
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.umn.utsmap.ui.auth.*
import org.umn.utsmap.ui.finance.*
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.theme.BudgetLensTheme
import java.io.File

class CommunityFlowTest {
    @get:Rule val compose=createComposeRule()
    private fun shot(name:String){val dir=InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null);File(dir,"$name.png").outputStream().use{compose.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG,100,it)}}
    @Test fun groupsOpenMembersCanBeAddedAndDebtChoosesExpense() {
        val vm=FinanceViewModel();vm.activate("groups-ui");val auth=AuthViewModel();var groupId by mutableStateOf<String?>(null);var opened:String?=null
        compose.setContent{BudgetLensTheme{if(groupId==null)GroupListScreen(vm,{}, {groupId=it}) else GroupDetailScreen(groupId!!,vm,auth,{}, {},{opened=it})}}
        shot("groups-list-new")
        compose.onNodeWithText("Kos Griya Asri").performClick()
        compose.onNodeWithText("Ingatkan Anggota untuk Bayar").assertDoesNotExist()
        compose.onNodeWithText("+ Anggota").performClick()
        compose.onNodeWithText("Michael",substring=false).performClick()
        assertTrue(vm.group("g1")!!.members.any{it.name=="Michael"})
        shot("group-detail-new")
        compose.onNodeWithText("Bayar Pengeluaran Grup").performScrollTo().performClick()
        compose.onNodeWithText("Belanja kebutuhan kos • Rp 75.000").performClick()
        assertNotNull(opened)
        compose.runOnIdle{groupId="g2"}
        compose.onNodeWithText("Bayar Pengeluaran Grup").assertDoesNotExist()
        compose.onNodeWithText("Ingatkan Anggota untuk Bayar").performScrollTo().assertIsDisplayed()
    }
    @Test fun friendRequestsRequireAcceptanceAndPickerShowsSeededFriends() {
        val auth=AuthViewModel()
        compose.setContent{BudgetLensTheme{FriendsScreen(auth,{})}}
        compose.runOnIdle{auth.switchMode(true);auth.name="Teman Baru";auth.email="teman@example.com";auth.phone="081998887777";auth.password="password1";auth.confirmation="password1";auth.submit()}
        compose.waitUntil(3000){auth.user!=null}
        compose.runOnIdle{auth.logout();auth.email="certified@email.com";auth.password="c3rt1f1ed";auth.submit()}
        compose.waitUntil(3000){auth.user!=null}
        compose.onNodeWithText("Cari nomor telepon").performTextInput("081998887777")
        compose.onNodeWithText("Kirim Permintaan Pertemanan").performClick()
        compose.runOnIdle{assertEquals(1,auth.sentRequests.size);assertFalse(auth.friends.any{it.name=="Teman Baru"});auth.logout();auth.email="teman@example.com";auth.password="password1";auth.submit()}
        compose.waitUntil(3000){auth.user?.email=="teman@example.com"}
        compose.onNodeWithText("Terima",substring=false).performScrollTo().performClick()
        assertTrue(auth.friends.any{it.name=="Certified"})
        assertTrue(auth.friends.any{it.name=="Marcelline"})
        shot("friends-new")
    }
    @Test fun notificationsCanBeMarkedReadAndOpenRealBill() {
        val vm=FinanceViewModel();vm.activate("notices");var opened:String?=null
        compose.setContent{BudgetLensTheme{NotificationsScreen(vm,{}, {opened=it},{},{})}}
        shot("notifications-new")
        compose.onNodeWithText("Tandai semua").performClick()
        assertTrue(vm.notifications().all{it.read})
        compose.onAllNodesWithText("Dinda minta bayar",substring=false).onFirst().performScrollTo().performClick()
        assertEquals("example-dinda",opened)
    }
    @Test fun newGroupExpenseCanSplitQuantitiesAndRecordBudgetCategory() {
        val vm=FinanceViewModel();vm.activate("create-expense")
        var saved:String?=null
        compose.setContent{BudgetLensTheme{GroupExpenseScreen("g1",null,vm,{}, {saved=it})}}
        compose.onNodeWithText("Nama tagihan").performTextInput("Sabun Kos")
        compose.onNodeWithText("Nama item 1").performScrollTo().performTextInput("Sabun")
        compose.onNodeWithText("Jumlah item 1").performTextReplacement("4")
        compose.onNodeWithText("Harga satuan item 1").performTextReplacement("10000")
        compose.onNodeWithText("Lanjut Atur Anggota").performScrollTo().performClick()
        compose.onNodeWithText("Per Item",substring=false).performScrollTo().performClick()
        compose.onNodeWithText("Jumlah Sabun - Kamu").performScrollTo().performTextReplacement("1")
        compose.onNodeWithText("Jumlah Sabun - Marcelline").performTextReplacement("1")
        compose.onNodeWithText("Jumlah Sabun - Richard").performScrollTo().performTextReplacement("2")
        compose.onNodeWithText("Simpan Pengeluaran Grup").performScrollTo().performClick()
        compose.runOnIdle{
            val bill=vm.bill(saved!!)!!
            assertEquals("g1",bill.groupId);assertEquals("self",bill.payerId)
            assertEquals(10000L,bill.share("self"))
            val tx=vm.transactions.first{it.splitBillId==bill.id}
            assertEquals("Lainnya",tx.category);assertEquals(10000L,tx.amount)
        }
    }

}
