package com.hailn.xecost.ui.navigation

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.hailn.xecost.ui.dashboard.DashboardScreen
import com.hailn.xecost.ui.expense.AddExpenseScreen
import com.hailn.xecost.ui.expense.ExpenseListScreen
import com.hailn.xecost.ui.fuel.AddFuelScreen
import com.hailn.xecost.ui.fuel.FuelListScreen
import com.hailn.xecost.ui.vehicle.AddVehicleScreen
import com.hailn.xecost.ui.vehicle.VehicleListScreen

/**
 * Khung của app: thanh tab dưới + NavHost.
 * Các màn hình form (thêm/sửa) nằm ngoài tab, nên khi mở thì thanh tab và FAB ẩn đi.
 */
// ExperimentalLayoutApi khai phòng thủ cho consumeWindowInsets(PaddingValues): theo tài liệu
// nó đã stable từ foundation 1.6, nhưng @OptIn thừa chỉ sinh cảnh báo, còn thiếu là lỗi biên
// dịch — mà đây là lần build đầu tiên nên đổi một cảnh báo lấy chắc chắn là đáng.
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AppRoot() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val topLevel = TopLevelDestination.entries.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }

    Scaffold(
        topBar = {
            // Màn hình form tự dựng TopAppBar riêng của chúng.
            if (topLevel != null) {
                TopAppBar(title = { Text(topLevel.label) })
            }
        },
        bottomBar = {
            if (topLevel != null) {
                NavigationBar {
                    TopLevelDestination.entries.forEach { dest ->
                        NavigationBarItem(
                            selected = dest == topLevel,
                            onClick = {
                                navController.navigate(dest.route) {
                                    // Không chồng chất back stack khi bấm qua lại giữa các tab.
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(dest.icon, contentDescription = dest.label) },
                            label = { Text(dest.label) }
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            val target = when (topLevel) {
                TopLevelDestination.DASHBOARD, TopLevelDestination.FUEL -> Routes.addFuel()
                TopLevelDestination.EXPENSE -> Routes.addExpense()
                TopLevelDestination.VEHICLE -> Routes.addVehicle()
                null -> null
            }
            if (target != null) {
                FloatingActionButton(onClick = { navController.openForm(target) }) {
                    Icon(Icons.Default.Add, contentDescription = "Thêm")
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                // Scaffold ngoài đã trừ status bar / navigation bar rồi.
                // Không "tiêu thụ" insets ở đây thì Scaffold + TopAppBar của các màn
                // hình form bên trong sẽ trừ thêm một lần nữa -> thừa lề trên/dưới.
                .consumeWindowInsets(padding)
        ) {
            composable(Routes.DASHBOARD) {
                DashboardScreen()
            }

            composable(Routes.FUEL) {
                FuelListScreen(onEdit = { navController.openForm(Routes.addFuel(it)) })
            }

            composable(Routes.EXPENSE) {
                ExpenseListScreen(onEdit = { navController.openForm(Routes.addExpense(it)) })
            }

            composable(Routes.VEHICLE) {
                VehicleListScreen(onEdit = { navController.openForm(Routes.addVehicle(it)) })
            }

            composable(
                route = "${Routes.ADD_FUEL}?entryId={entryId}",
                arguments = listOf(
                    navArgument("entryId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                AddFuelScreen(onDone = { navController.popOnce(entry) })
            }

            composable(
                route = "${Routes.ADD_EXPENSE}?expenseId={expenseId}",
                arguments = listOf(
                    navArgument("expenseId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                AddExpenseScreen(onDone = { navController.popOnce(entry) })
            }

            composable(
                route = "${Routes.ADD_VEHICLE}?vehicleId={vehicleId}",
                arguments = listOf(
                    navArgument("vehicleId") { type = NavType.LongType; defaultValue = 0L }
                )
            ) { entry ->
                AddVehicleScreen(onDone = { navController.popOnce(entry) })
            }
        }
    }
}

/**
 * Chỉ pop khi màn hình gọi vẫn đang là màn hình trên cùng (RESUMED).
 *
 * Không có chốt này thì bấm nhanh hai lần nút "Quay lại" (hoặc `LaunchedEffect`
 * chạy lại sau khi đã lưu) sẽ pop hai lần, văng luôn cả màn hình phía dưới.
 *
 * PHẢI so với RESUMED chứ không phải `isAtLeast(STARTED)`: ngay khi pop lần đầu, màn
 * hình này rời khỏi back stack nhưng VẪN hiển thị trong lúc chạy animation thoát, và
 * lifecycle của nó lúc đó chưa tụt xuống dưới STARTED. Dùng `isAtLeast(STARTED)` thì
 * cú bấm thứ hai vẫn lọt qua -> pop quá một màn hình. Đây đúng là mẫu khử trùng lặp
 * `lifecycleIsResumed()` trong các sample Navigation Compose chính thức.
 */
private fun NavController.popOnce(entry: NavBackStackEntry) {
    if (entry.lifecycle.currentState == Lifecycle.State.RESUMED) {
        popBackStack()
    }
}

/**
 * Mở một màn hình form (thêm/sửa).
 *
 * `launchSingleTop` là mặt đối xứng của [popOnce]: bấm nhanh hai lần nút + (hoặc hai lần
 * vào cùng một dòng của danh sách) sẽ đẩy HAI bản sao cùng màn hình lên back stack —
 * lưu xong, pop một lần, người dùng lại thấy đúng cái form đó hiện ra lần nữa.
 */
private fun NavController.openForm(route: String) {
    navigate(route) { launchSingleTop = true }
}
