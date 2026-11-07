package org.umn.utsmap.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import org.umn.utsmap.ui.finance.ReceiptScanViewModel
import org.umn.utsmap.ui.screens.ReceiptScanScreen
import org.umn.utsmap.ui.screens.*
import org.umn.utsmap.ui.screens.AccountScreen
import org.umn.utsmap.ui.screens.SplitBillListScreen
import org.umn.utsmap.ui.screens.SplitBillCreateScreen
import org.umn.utsmap.ui.screens.SplitBillDetailScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.delay
import org.umn.utsmap.ui.auth.AuthViewModel
import org.umn.utsmap.ui.finance.FinanceViewModel
import org.umn.utsmap.ui.screens.TransactionFormScreen
import org.umn.utsmap.ui.screens.TransactionDetailScreen
import androidx.navigation.NavType
import androidx.navigation.navArgument
import org.umn.utsmap.ui.screens.LoginScreen
import org.umn.utsmap.ui.screens.OnboardingScreen
import org.umn.utsmap.ui.screens.SplashScreen
import org.umn.utsmap.ui.screens.DashboardScreen

@Composable
fun BudgetLensApp(auth: AuthViewModel, finance: FinanceViewModel, scan: ReceiptScanViewModel = remember { ReceiptScanViewModel() }) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            SplashScreen()
            LaunchedEffect(Unit) {
                delay(1800)
                navController.navigate("onboarding") {
                    popUpTo("splash") { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        composable("onboarding") {
            OnboardingScreen(onFinished = {
                navController.navigate("login") { launchSingleTop = true }
            })
        }
        composable("login") {
            LoginScreen(auth = auth, onBack = {
                if (!navController.popBackStack()) {
                    navController.navigate("onboarding") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            })
            LaunchedEffect(auth.user) {
                if (auth.user != null) {
                    finance.activate(auth.user!!.id)
                    navController.navigate("dashboard") {
                        popUpTo(navController.graph.id) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable("dashboard") {
            val user = auth.user
            if (user != null) {
                DashboardScreen(user = user, finance = finance, onEditProfile = auth::updateName, onEditPhone = auth::updatePhone,
                    onAccount = { navController.navigate("account/$it") },
                    onGroup = { navController.navigate("group/$it") },
                    onNotifications = { navController.navigate("notifications") },
                    hideBalances=auth.hideBalances,sortNewest=auth.sortNewest,
                    onAdd = { navController.navigate("transaction/new") { launchSingleTop = true } },
                    onScan = { scan.reset(); navController.navigate("scan") { launchSingleTop = true } },
                    onSplitBills = { navController.navigate("split-bills") { launchSingleTop = true } },
                    onDetail = { id -> navController.navigate("transaction/$id") { launchSingleTop = true } },
                    onLogout = {
                    auth.logout()
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                        launchSingleTop = true
                    }
                })
            } else {
                // Jika proses dihentikan Android, akun memori hilang. Kembali ke login.
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("dashboard") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        composable("transaction/new") {
            TransactionFormScreen(finance, defaultIncome=auth.defaultIncome, onBack = { navController.popBackStack() }, onScan = {
                scan.reset(); navController.navigate("scan") { launchSingleTop = true }
            }, onSaved = { id ->
                navController.navigate("transaction/$id") {
                    popUpTo("transaction/new") { inclusive = true }
                    launchSingleTop = true
                }
            })
        }
        composable("scan") {
            ReceiptScanScreen(scan, finance, onBack = { navController.popBackStack() },
                onSplit = { navController.navigate("split-new") { launchSingleTop = true } }, onSaved = { id ->
                navController.navigate("transaction/$id") {
                    popUpTo("dashboard") { inclusive = false }
                    launchSingleTop = true
                }
            })
        }
        composable("group/{groupId}",arguments=listOf(navArgument("groupId"){type=NavType.StringType})) { entry ->
            val id=entry.arguments?.getString("groupId").orEmpty()
            GroupDetailScreen(id,finance,auth,{navController.popBackStack()},{navController.navigate("group/$id/expense")},{navController.navigate("split/$it")})
        }
        composable("group/{groupId}/expense",arguments=listOf(navArgument("groupId"){type=NavType.StringType})) { entry ->
            GroupExpenseScreen(entry.arguments?.getString("groupId").orEmpty(),null,finance,{navController.popBackStack()},{id->navController.navigate("split/$id"){popUpTo("group/{groupId}/expense"){inclusive=true}}})
        }
        composable("group/{groupId}/edit/{billId}",arguments=listOf(navArgument("groupId"){type=NavType.StringType},navArgument("billId"){type=NavType.StringType})) {entry->
            GroupExpenseScreen(entry.arguments?.getString("groupId").orEmpty(),entry.arguments?.getString("billId"),finance,{navController.popBackStack()},{navController.popBackStack()})
        }
        composable("notifications") {
            NotificationsScreen(finance,{navController.popBackStack()},{navController.navigate("split/$it")},{navController.navigate("budget-overview")},{navController.navigate("account/savings")})
        }
        composable("budget-overview") { BudgetScreen(finance,{navController.navigate("transaction/$it")}) }
        composable("account/{section}", arguments = listOf(navArgument("section") { type = NavType.StringType })) { entry ->
            AccountScreen(entry.arguments?.getString("section").orEmpty(), auth, finance, { navController.popBackStack() }, { navController.navigate("account/$it") })
        }
        composable("split-bills") {
            SplitBillListScreen(finance, auth, { navController.popBackStack() },
                { scan.reset(); navController.navigate("scan") }, { navController.navigate("split/$it") },
                onManual = { scan.beginManual(); navController.navigate("split-manual") })
        }
        composable("split-manual") {
            org.umn.utsmap.ui.screens.ManualBillScreen(scan, { navController.popBackStack() }, { navController.navigate("split-new") })
        }
        composable("split-new") {
            SplitBillCreateScreen(scan, finance, auth, { navController.popBackStack() }, { id ->
                navController.navigate("split/$id") { popUpTo("dashboard") { inclusive = false }; launchSingleTop = true }
            })
        }
        composable("split/{billId}", arguments = listOf(navArgument("billId") { type = NavType.StringType })) { entry ->
            val bill=finance.bill(entry.arguments?.getString("billId").orEmpty())
            SplitBillDetailScreen(bill, finance, { navController.popBackStack() },onEditGroup={ bill?.groupId?.let { groupId -> navController.navigate("group/$groupId/edit/${bill.id}") } })
        }
        composable("transaction/{transactionId}", arguments = listOf(navArgument("transactionId") { type = NavType.StringType })) { entry ->
            val id = entry.arguments?.getString("transactionId").orEmpty()
            TransactionDetailScreen(finance.find(id), onBack = { navController.popBackStack() },
                onDelete = { finance.delete(id); navController.popBackStack() },
                onSplitDetail = { navController.navigate("split/$it") },confirmDeletion=auth.confirmDeletion)
        }
    }
}


