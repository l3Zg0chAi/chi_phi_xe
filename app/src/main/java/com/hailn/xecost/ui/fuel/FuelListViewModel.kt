package com.hailn.xecost.ui.fuel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.FuelEntry
import com.hailn.xecost.data.repository.FuelRepository
import com.hailn.xecost.data.repository.VehicleRepository
import com.hailn.xecost.domain.FuelSegment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FuelRow(
    val entry: FuelEntry,
    /** Mức tiêu hao của đoạn kết thúc tại bản ghi này, null nếu không tính được. */
    val consumption: Double?
)

data class FuelListUiState(
    /** Chưa có emit nào từ DB. Phân biệt với "đã nạp xong và đúng là chưa có xe". */
    val loading: Boolean = true,
    val hasVehicle: Boolean = false,
    val rows: List<FuelRow> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class FuelListViewModel @Inject constructor(
    private val fuelRepo: FuelRepository,
    vehicleRepo: VehicleRepository,
    selectedStore: SelectedVehicleStore
) : ViewModel() {

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
            flowOf(FuelListUiState(loading = false))
        } else {
            combine(
                fuelRepo.observeByVehicle(vehicleId),
                fuelRepo.observeSummary(vehicleId)
            ) { entries, summary ->
                // Gắn mức tiêu hao vào đúng bản ghi đóng đoạn.
                // Khoá theo ID chứ KHÔNG theo odo: hai lần đổ có thể trùng odo (đổ hai lần
                // cùng mốc, hoặc gõ trùng), khi đó khoá theo odo sẽ gắn nhãn L/100km lên
                // bản ghi không phải mốc đóng đoạn.
                val byEndEntry: Map<Long, FuelSegment> =
                    summary.segments.associateBy { it.endEntryId }

                FuelListUiState(
                    loading = false,
                    hasVehicle = true,
                    rows = entries.map { entry ->
                        FuelRow(
                            entry = entry,
                            consumption = byEndEntry[entry.id]?.consumption
                        )
                    }
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FuelListUiState())

    fun delete(entry: FuelEntry) {
        viewModelScope.launch { fuelRepo.delete(entry) }
    }
}
