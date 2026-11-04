package com.hailn.xecost.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.Expense
import com.hailn.xecost.data.local.entity.ExpenseCategory
import com.hailn.xecost.data.repository.ExpenseRepository
import com.hailn.xecost.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExpenseListUiState(
    /**
     * true cho tới khi flow đầu tiên về. Không có cờ này thì giá trị khởi tạo của [stateIn]
     * (hasVehicle = false) làm màn hình chớp dòng "Chưa có xe nào" ngay cả khi đang có xe.
     */
    val loading: Boolean = true,
    val hasVehicle: Boolean = false,
    val expenses: List<Expense> = emptyList(),
    val filter: ExpenseCategory? = null,
    val total: Long = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ExpenseListViewModel @Inject constructor(
    private val expenseRepo: ExpenseRepository,
    vehicleRepo: VehicleRepository,
    selectedStore: SelectedVehicleStore
) : ViewModel() {

    private val filter = MutableStateFlow<ExpenseCategory?>(null)

    private val currentVehicleId = combine(
        vehicleRepo.observeActive(),
        selectedStore.selectedId
    ) { vehicles, selectedId ->
        vehicles.firstOrNull { it.id == selectedId }?.id
            ?: vehicles.firstOrNull { it.isDefault }?.id
            ?: vehicles.firstOrNull()?.id
    }

    val state = currentVehicleId.flatMapLatest { vehicleId ->
        if (vehicleId == null) {
            flowOf(ExpenseListUiState(loading = false))
        } else {
            combine(expenseRepo.observeByVehicle(vehicleId), filter) { all, category ->
                val visible = if (category == null) all else all.filter { it.category == category }
                ExpenseListUiState(
                    loading = false,
                    hasVehicle = true,
                    expenses = visible,
                    filter = category,
                    total = visible.sumOf { it.amount }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ExpenseListUiState())

    fun setFilter(category: ExpenseCategory?) {
        filter.value = category
    }

    fun delete(expense: Expense) {
        viewModelScope.launch { expenseRepo.delete(expense) }
    }
}
