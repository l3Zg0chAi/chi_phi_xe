package com.hailn.xecost.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Insights
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val DASHBOARD = "dashboard"
    const val FUEL = "fuel"
    const val EXPENSE = "expense"
    const val VEHICLE = "vehicle"

    const val ADD_FUEL = "add_fuel"
    const val ADD_EXPENSE = "add_expense"
    const val ADD_VEHICLE = "add_vehicle"

    /** id = 0 nghĩa là thêm mới; khác 0 là sửa bản ghi đó. */
    fun addFuel(entryId: Long = 0) = "$ADD_FUEL?entryId=$entryId"
    fun addExpense(expenseId: Long = 0) = "$ADD_EXPENSE?expenseId=$expenseId"
    fun addVehicle(vehicleId: Long = 0) = "$ADD_VEHICLE?vehicleId=$vehicleId"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector
) {
    DASHBOARD(Routes.DASHBOARD, "Tổng quan", Icons.Default.Insights),
    FUEL(Routes.FUEL, "Nhiên liệu", Icons.Default.LocalGasStation),
    EXPENSE(Routes.EXPENSE, "Chi phí", Icons.Default.Receipt),
    VEHICLE(Routes.VEHICLE, "Xe", Icons.Default.DirectionsCar)
}
