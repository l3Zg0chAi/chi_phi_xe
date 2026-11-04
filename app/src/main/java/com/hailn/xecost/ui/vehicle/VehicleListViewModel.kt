package com.hailn.xecost.ui.vehicle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VehicleListViewModel @Inject constructor(
    private val repo: VehicleRepository,
    private val selectedStore: SelectedVehicleStore
) : ViewModel() {

    val vehicles = repo.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val selectedId = selectedStore.selectedId

    fun select(vehicle: Vehicle) = selectedStore.select(vehicle.id)

    /**
     * Chốt chặn thứ hai cho "xe đã bán không được là mặc định".
     * `repo.setDefault()` ghi thẳng cờ mà không kiểm tra `isActive`, nên nếu chỉ dựa vào
     * việc UI ẩn mục menu thì một lần gọi sót là cờ mặc định nằm trên xe mà
     * `observeActive()` không trả về -> màn khác tìm xe mặc định không thấy.
     */
    fun setDefault(vehicle: Vehicle) {
        if (!vehicle.isActive) return
        viewModelScope.launch { repo.setDefault(vehicle.id) }
    }

    /** Đánh dấu đã bán thay vì xoá — giữ lại lịch sử chi phí. */
    fun toggleActive(vehicle: Vehicle) {
        viewModelScope.launch { repo.save(vehicle.copy(isActive = !vehicle.isActive)) }
    }

    fun delete(vehicle: Vehicle) {
        viewModelScope.launch {
            repo.delete(vehicle)
            // Xoá đúng chiếc đang được chọn mà không bỏ chọn thì SelectedVehicleStore giữ
            // lại một id không còn tồn tại. Nó khác null nên Dashboard / Nhiên liệu / Chi phí
            // không rơi về xe mặc định được, cả ba màn hình trống trơn cho tới khi
            // người dùng tự bấm chọn xe khác.
            if (selectedStore.selectedId.value == vehicle.id) selectedStore.select(null)
        }
    }
}
