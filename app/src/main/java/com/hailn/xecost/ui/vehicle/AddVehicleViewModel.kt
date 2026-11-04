package com.hailn.xecost.ui.vehicle

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.FuelType
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.data.local.entity.VehicleType
import com.hailn.xecost.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddVehicleUiState(
    val vehicleId: Long = 0,
    val name: String = "",
    val type: VehicleType = VehicleType.MOTORBIKE,
    val fuelType: FuelType = FuelType.RON95,
    val plateNumber: String = "",
    val brand: String = "",
    val model: String = "",
    val year: String = "",
    val tankCapacity: String = "",
    val initialOdometer: String = "",
    val purchasePrice: String = "",
    val isDefault: Boolean = false,
    /** Lỗi khi ghi xuống DB. Giữ nguyên form để người dùng không mất cái vừa gõ. */
    val error: String? = null,
    val saved: Boolean = false
) {
    val isEdit: Boolean get() = vehicleId != 0L
    val canSave: Boolean get() = name.isNotBlank()
}

@HiltViewModel
class AddVehicleViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: VehicleRepository,
    private val selectedStore: SelectedVehicleStore
) : ViewModel() {

    private val _state = MutableStateFlow(AddVehicleUiState())
    val state = _state.asStateFlow()

    private val vehicleId: Long = savedStateHandle.get<Long>("vehicleId") ?: 0L

    /** Giữ lại để khi sửa xe không làm mất odo hiện tại và ngày mua. */
    private var original: Vehicle? = null

    /** Chặn bấm Lưu hai lần liên tiếp -> tránh tạo trùng hai xe. */
    private var saving = false

    init {
        if (vehicleId != 0L) {
            viewModelScope.launch {
                repo.getById(vehicleId)?.let { v ->
                    original = v
                    _state.value = AddVehicleUiState(
                        vehicleId = v.id,
                        name = v.name,
                        type = v.type,
                        fuelType = v.fuelType,
                        plateNumber = v.plateNumber.orEmpty(),
                        brand = v.brand.orEmpty(),
                        model = v.model.orEmpty(),
                        year = v.year?.toString().orEmpty(),
                        tankCapacity = v.tankCapacity?.toString().orEmpty(),
                        initialOdometer = v.initialOdometer.toString(),
                        purchasePrice = v.purchasePrice?.toString().orEmpty(),
                        isDefault = v.isDefault
                    )
                }
            }
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setPlate(value: String) = _state.update { it.copy(plateNumber = value) }
    fun setBrand(value: String) = _state.update { it.copy(brand = value) }
    fun setModel(value: String) = _state.update { it.copy(model = value) }
    fun setYear(value: String) = _state.update { it.copy(year = value) }
    fun setTankCapacity(value: String) = _state.update { it.copy(tankCapacity = value) }
    fun setInitialOdometer(value: String) = _state.update { it.copy(initialOdometer = value) }
    fun setPurchasePrice(value: String) = _state.update { it.copy(purchasePrice = value) }
    fun setDefault(value: Boolean) = _state.update { it.copy(isDefault = value) }

    /** Đổi loại xe thì đổi luôn nhiên liệu mặc định cho hợp lý. */
    fun setType(value: VehicleType) = _state.update {
        it.copy(
            type = value,
            fuelType = if (value == VehicleType.MOTORBIKE) FuelType.RON95 else it.fuelType
        )
    }

    fun setFuelType(value: FuelType) = _state.update { it.copy(fuelType = value) }

    fun save() {
        val s = _state.value
        if (!s.canSave || s.saved || saving) return

        // Sửa xe nhưng bản gốc chưa nạp xong: lưu bây giờ sẽ nuốt mất odo hiện tại,
        // ngày mua và createdAt của xe cũ.
        if (s.isEdit && original == null) return

        saving = true
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                val initialOdo = (s.initialOdometer.toIntOrNull() ?: 0).coerceAtLeast(0)
                val vehicle = Vehicle(
                    id = s.vehicleId,
                    name = s.name.trim(),
                    type = s.type,
                    fuelType = s.fuelType,
                    plateNumber = s.plateNumber.takeIf { it.isNotBlank() },
                    brand = s.brand.takeIf { it.isNotBlank() },
                    model = s.model.takeIf { it.isNotBlank() },
                    // Ô trống -> null chứ không phải 0. Thiếu dòng này thì mỗi lần sửa xe
                    // là năm sản xuất bị ghi đè thành null, vì Vehicle() tự điền mặc định.
                    year = s.year.toIntOrNull(),
                    tankCapacity = s.tankCapacity.toDoubleOrNull(),
                    initialOdometer = initialOdo,
                    currentOdometer = maxOf(original?.currentOdometer ?: 0, initialOdo),
                    purchaseDate = original?.purchaseDate,
                    purchasePrice = s.purchasePrice.toLongOrNull(),
                    isActive = original?.isActive ?: true,
                    isDefault = s.isDefault,
                    createdAt = original?.createdAt ?: System.currentTimeMillis()
                )
                val id = repo.save(vehicle)
                selectedStore.select(id)
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                // Không để coroutine văng ra làm sập app và mất hết chữ đã gõ.
                _state.update { it.copy(error = "Không lưu được: ${e.message ?: "lỗi không rõ"}") }
            } finally {
                saving = false
            }
        }
    }
}
