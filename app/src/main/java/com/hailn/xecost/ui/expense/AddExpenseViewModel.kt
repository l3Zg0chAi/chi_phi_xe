package com.hailn.xecost.ui.expense

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.Expense
import com.hailn.xecost.data.local.entity.ExpenseCategory
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.data.repository.ExpenseRepository
import com.hailn.xecost.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddExpenseUiState(
    val expenseId: Long = 0,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val category: ExpenseCategory = ExpenseCategory.MAINTENANCE,
    val title: String = "",
    val amount: String = "",
    val odometer: String = "",
    val place: String = "",
    val note: String = "",
    /**
     * Ảnh hoá đơn của bản ghi đang sửa. Màn hình chưa cho chụp ảnh, nhưng vẫn phải mang theo:
     * [AddExpenseViewModel.save] dựng một [Expense] MỚI rồi ghi đè, nên trường nào không được
     * chép lại sẽ bị xoá âm thầm mỗi lần người dùng sửa bản ghi.
     */
    val photoUri: String? = null,
    /** false cho tới khi nạp xong danh sách xe / bản ghi đang sửa. */
    val loaded: Boolean = false,
    val saved: Boolean = false,
    /** Lỗi khi ghi xuống DB. Giữ form lại để người dùng không mất những gì vừa gõ. */
    val error: String? = null
) {
    val isEdit: Boolean get() = expenseId != 0L

    val canSave: Boolean
        get() = vehicleId != null &&
            title.isNotBlank() &&
            (amount.toLongOrNull() ?: 0L) > 0
}

@HiltViewModel
class AddExpenseViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val expenseRepo: ExpenseRepository,
    private val vehicleRepo: VehicleRepository,
    private val selectedStore: SelectedVehicleStore
) : ViewModel() {

    private val _state = MutableStateFlow(AddExpenseUiState())
    val state = _state.asStateFlow()

    private val expenseId: Long = savedStateHandle.get<Long>("expenseId") ?: 0L

    /** Chặn bấm Lưu hai lần liên tiếp -> tránh chèn trùng hai bản ghi. */
    private var saving = false

    init {
        viewModelScope.launch {
            val vehicles = vehicleRepo.observeActive().first()
            val existing = if (expenseId != 0L) expenseRepo.getById(expenseId) else null

            _state.update {
                if (existing != null) {
                    it.copy(
                        expenseId = existing.id,
                        vehicles = vehicles,
                        vehicleId = existing.vehicleId,
                        date = existing.date,
                        category = existing.category,
                        title = existing.title,
                        amount = existing.amount.toString(),
                        odometer = existing.odometer?.toString().orEmpty(),
                        place = existing.place.orEmpty(),
                        note = existing.note.orEmpty(),
                        photoUri = existing.photoUri,
                        loaded = true
                    )
                } else {
                    // Chỉ nhận xe đang chọn khi nó còn trong danh sách xe đang hoạt động:
                    // xe đã bán vẫn nằm trong SelectedVehicleStore nhưng không còn hiện trên
                    // hàng chip, khi đó form sẽ ghi vào một xe người dùng không nhìn thấy.
                    val selected = selectedStore.selectedId.value
                    val vehicleId = vehicles.firstOrNull { v -> v.id == selected }?.id
                        ?: vehicles.firstOrNull { v -> v.isDefault }?.id
                        ?: vehicles.firstOrNull()?.id
                    it.copy(vehicles = vehicles, vehicleId = vehicleId, loaded = true)
                }
            }
        }
    }

    fun setVehicle(id: Long) = _state.update { it.copy(vehicleId = id) }
    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setAmount(value: String) = _state.update { it.copy(amount = value) }
    fun setOdometer(value: String) = _state.update { it.copy(odometer = value) }
    fun setPlace(value: String) = _state.update { it.copy(place = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    /**
     * Đổi danh mục thì gợi ý luôn tiêu đề, nếu người dùng chưa tự gõ.
     *
     * Chỉ coi là gợi ý khi tiêu đề đúng bằng nhãn của danh mục ĐANG chọn — so với nhãn
     * của mọi danh mục sẽ xoá nhầm tiêu đề do người dùng tự gõ (vd gõ "Rửa xe" trong
     * danh mục Sửa chữa).
     */
    fun setCategory(value: ExpenseCategory) = _state.update {
        val titleWasSuggestion = it.title.isBlank() || it.title == it.category.label
        it.copy(
            category = value,
            title = if (titleWasSuggestion) value.label else it.title
        )
    }

    fun save() {
        val s = _state.value
        val vehicleId = s.vehicleId ?: return
        if (!s.canSave || s.saved || saving) return

        val amount = s.amount.toLongOrNull() ?: return

        saving = true
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                expenseRepo.save(
                    Expense(
                        id = s.expenseId,
                        vehicleId = vehicleId,
                        date = s.date,
                        category = s.category,
                        title = s.title.trim(),
                        amount = amount,
                        odometer = s.odometer.toIntOrNull(),
                        place = s.place.takeIf { it.isNotBlank() },
                        note = s.note.takeIf { it.isNotBlank() },
                        photoUri = s.photoUri
                    )
                )
                selectedStore.select(vehicleId)
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                // Ghi hỏng (vd xe vừa bị xoá ở màn khác) thì báo lỗi và giữ nguyên form,
                // tuyệt đối không để coroutine văng ra làm sập app và mất hết chữ đã gõ.
                _state.update { it.copy(error = e.message ?: "Không lưu được khoản chi.") }
            } finally {
                saving = false
            }
        }
    }
}
