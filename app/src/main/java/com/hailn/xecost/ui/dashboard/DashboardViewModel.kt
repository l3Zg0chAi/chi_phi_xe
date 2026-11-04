package com.hailn.xecost.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.core.toYearMonth
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.Expense
import com.hailn.xecost.data.local.entity.FuelEntry
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.data.repository.ExpenseRepository
import com.hailn.xecost.data.repository.FuelRepository
import com.hailn.xecost.data.repository.VehicleRepository
import com.hailn.xecost.domain.FuelSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.YearMonth
import javax.inject.Inject

data class MonthBucket(
    val month: YearMonth,
    val fuelCost: Long,
    val otherCost: Long
) {
    val total: Long get() = fuelCost + otherCost
}

sealed interface ActivityItem {
    val date: Long
    val amount: Long

    data class Fuel(val entry: FuelEntry) : ActivityItem {
        override val date get() = entry.date
        override val amount get() = entry.totalCost
    }

    data class Other(val expense: Expense) : ActivityItem {
        override val date get() = expense.date
        override val amount get() = expense.amount
    }
}

data class DashboardUiState(
    val loading: Boolean = true,
    val vehicles: List<Vehicle> = emptyList(),
    val selected: Vehicle? = null,
    val fuelSummary: FuelSummary? = null,
    val monthCost: Long = 0,
    val previousMonthCost: Long = 0,
    val totalCost: Long = 0,
    val buckets: List<MonthBucket> = emptyList(),
    val recent: List<ActivityItem> = emptyList()
) {
    /** VND / km — gộp cả nhiên liệu và chi phí khác. */
    val costPerKm: Double?
        get() {
            val km = selected?.distanceTravelled ?: return null
            if (km <= 0) return null
            return totalCost.toDouble() / km
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    vehicleRepo: VehicleRepository,
    fuelRepo: FuelRepository,
    expenseRepo: ExpenseRepository,
    private val selectedStore: SelectedVehicleStore
) : ViewModel() {

    private val vehiclesFlow = vehicleRepo.observeActive()

    val state = combine(
        vehiclesFlow,
        selectedStore.selectedId
    ) { vehicles, selectedId ->
        // Chưa chọn xe nào -> lấy xe mặc định, không có thì lấy xe đầu tiên.
        val selected = vehicles.firstOrNull { it.id == selectedId }
            ?: vehicles.firstOrNull { it.isDefault }
            ?: vehicles.firstOrNull()
        vehicles to selected
    }.flatMapLatest { (vehicles, selected) ->
        if (selected == null) {
            flowOf(DashboardUiState(loading = false, vehicles = vehicles))
        } else {
            combine(
                fuelRepo.observeByVehicle(selected.id),
                fuelRepo.observeSummary(selected.id),
                expenseRepo.observeByVehicle(selected.id)
            ) { fuels, summary, expenses ->
                build(vehicles, selected, fuels, summary, expenses)
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DashboardUiState()
    )

    fun selectVehicle(id: Long) = selectedStore.select(id)

    private fun build(
        vehicles: List<Vehicle>,
        selected: Vehicle,
        fuels: List<FuelEntry>,
        summary: FuelSummary,
        expenses: List<Expense>
    ): DashboardUiState {
        val thisMonth = YearMonth.now()
        val lastMonth = thisMonth.minusMonths(1)

        val months = (5 downTo 0).map { thisMonth.minusMonths(it.toLong()) }
        val fuelByMonth = fuels.groupBy { it.date.toYearMonth() }
        val expenseByMonth = expenses.groupBy { it.date.toYearMonth() }

        val buckets = months.map { m ->
            MonthBucket(
                month = m,
                fuelCost = fuelByMonth[m]?.sumOf { it.totalCost } ?: 0L,
                otherCost = expenseByMonth[m]?.sumOf { it.amount } ?: 0L
            )
        }

        val recent = (fuels.map { ActivityItem.Fuel(it) } + expenses.map { ActivityItem.Other(it) })
            .sortedByDescending { it.date }
            .take(5)

        return DashboardUiState(
            loading = false,
            vehicles = vehicles,
            selected = selected,
            fuelSummary = summary,
            monthCost = buckets.firstOrNull { it.month == thisMonth }?.total ?: 0L,
            previousMonthCost = buckets.firstOrNull { it.month == lastMonth }?.total ?: 0L,
            totalCost = fuels.sumOf { it.totalCost } + expenses.sumOf { it.amount },
            buckets = buckets,
            recent = recent
        )
    }
}
