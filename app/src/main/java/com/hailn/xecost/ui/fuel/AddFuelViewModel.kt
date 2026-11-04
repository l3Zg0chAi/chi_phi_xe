package com.hailn.xecost.ui.fuel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hailn.xecost.data.SelectedVehicleStore
import com.hailn.xecost.data.local.entity.FuelEntry
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.data.repository.FuelRepository
import com.hailn.xecost.data.repository.VehicleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToLong

/** Ba ô tiền/lượng của form đổ xăng. Hai ô bất kỳ suy ra ô thứ ba. */
enum class FuelField { LITERS, PRICE, TOTAL }

data class AddFuelUiState(
    /** Chưa nạp xong danh sách xe / bản ghi đang sửa. */
    val loading: Boolean = true,
    val entryId: Long = 0,
    val vehicles: List<Vehicle> = emptyList(),
    val vehicleId: Long? = null,
    val date: Long = System.currentTimeMillis(),
    val odometer: String = "",
    val liters: String = "",
    val pricePerLiter: String = "",
    val totalCost: String = "",
    val isFullTank: Boolean = true,
    val missedPrevious: Boolean = false,
    val station: String = "",
    val note: String = "",
    val lastOdometer: Int? = null,
    /**
     * Ô mà người dùng vừa tự gõ. Giá trị prefill KHÔNG tính là gõ.
     * Dùng để biết ô nào được phép tính lại — xem [AddFuelViewModel.setLiters].
     */
    val lastEdited: FuelField? = null,
    /**
     * Ô KHÁC mà người dùng gõ gần nhất trước [lastEdited].
     *
     * Phải nhớ hai ô chứ không phải một: gõ nhiều ký tự liên tiếp vào CÙNG một ô cũng gọi
     * hàm set nhiều lần, nếu chỉ có [lastEdited] thì từ ký tự thứ hai trở đi nó đã bằng
     * chính ô đang gõ, nhánh if rẽ sai và tính lại đè lên con số người dùng tự nhập ở ô kia.
     */
    val prevEdited: FuelField? = null,
    /**
     * Ảnh hoá đơn của bản ghi đang sửa. Màn hình chưa cho chụp ảnh, nhưng vẫn phải mang theo:
     * [AddFuelViewModel.save] dựng một [FuelEntry] MỚI rồi ghi đè, nên trường nào không được
     * chép lại sẽ bị xoá âm thầm mỗi lần người dùng sửa bản ghi.
     */
    val photoUri: String? = null,
    /** Lỗi khi ghi xuống DB. Giữ nguyên form để người dùng không mất cái vừa gõ. */
    val error: String? = null,
    val saved: Boolean = false
) {
    val isEdit: Boolean get() = entryId != 0L

    /**
     * Cảnh báo odo nhỏ hơn lần gần nhất. **Cố ý KHÔNG chặn nút Lưu.**
     *
     * Odo lùi không làm hỏng số liệu: [com.hailn.xecost.domain.FuelStats] sort theo odo rồi
     * bỏ qua đoạn có quãng đường <= 0, còn odo hiện tại của xe lấy bằng `MAX` nên cũng không
     * tụt. Trong khi đó người dùng ghi bù một lần đổ cũ là chuyện hoàn toàn bình thường —
     * chặn cứng sẽ khoá mất việc đó. Vì vậy câu chữ phải là lời nhắc, không phải mệnh lệnh.
     */
    val odometerError: String?
        get() {
            val odo = odometer.toIntOrNull() ?: return null
            val last = lastOdometer ?: return null
            return if (odo <= last) {
                "Odo nhỏ hơn lần gần nhất ($last km). Chỉ đúng nếu bạn đang ghi bù một lần đổ cũ."
            } else {
                null
            }
        }

    val canSave: Boolean
        get() = vehicleId != null &&
            odometer.toIntOrNull() != null &&
            (liters.toDoubleOrNull() ?: 0.0) > 0 &&
            (totalCost.toLongOrNull() ?: 0L) > 0
}

@HiltViewModel
class AddFuelViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val fuelRepo: FuelRepository,
    private val vehicleRepo: VehicleRepository,
    private val selectedStore: SelectedVehicleStore
) : ViewModel() {

    private val _state = MutableStateFlow(AddFuelUiState())
    val state = _state.asStateFlow()

    private val entryId: Long = savedStateHandle.get<Long>("entryId") ?: 0L

    /** Chặn bấm Lưu hai lần liên tiếp -> tránh chèn trùng hai bản ghi. */
    private var saving = false

    init {
        viewModelScope.launch {
            val vehicles = vehicleRepo.observeActive().first()
            val existing = if (entryId != 0L) fuelRepo.getById(entryId) else null

            // Xe đang chọn có thể đã bị bán (isActive = false) hoặc đã xoá -> không còn trong
            // observeActive(). Phải bỏ qua, nếu không hàng FilterChip không có ô nào sáng mà
            // bản ghi vẫn âm thầm rơi vào xe đó.
            val vehicleId = existing?.vehicleId
                ?: selectedStore.selectedId.value?.takeIf { id -> vehicles.any { it.id == id } }
                ?: vehicles.firstOrNull { it.isDefault }?.id
                ?: vehicles.firstOrNull()?.id

            _state.update {
                if (existing != null) {
                    it.copy(
                        loading = false,
                        entryId = existing.id,
                        vehicles = vehicles,
                        vehicleId = existing.vehicleId,
                        date = existing.date,
                        odometer = existing.odometer.toString(),
                        liters = existing.liters.toString(),
                        pricePerLiter = existing.pricePerLiter.toString(),
                        totalCost = existing.totalCost.toString(),
                        isFullTank = existing.isFullTank,
                        missedPrevious = existing.missedPrevious,
                        station = existing.station.orEmpty(),
                        note = existing.note.orEmpty(),
                        photoUri = existing.photoUri
                    )
                } else {
                    it.copy(loading = false, vehicles = vehicles, vehicleId = vehicleId)
                }
            }

            vehicleId?.let { prefillFromLast(it) }
        }
    }

    /** Gợi ý đơn giá của lần đổ gần nhất, và ghi nhớ odo lần trước để cảnh báo nhập lùi. */
    private suspend fun prefillFromLast(vehicleId: Long) {
        // Không `?: return` khi xe chưa có bản ghi nào: phải XOÁ mốc odo của xe trước đó,
        // nếu không đổi sang xe mới sẽ báo "Odo phải lớn hơn lần trước" bằng số của xe cũ.
        val last = fuelRepo.getLatest(vehicleId)
        _state.update { s ->
            s.copy(
                lastOdometer = if (s.isEdit) null else last?.odometer,
                pricePerLiter = if (last == null) s.pricePerLiter
                else s.pricePerLiter.ifBlank { last.pricePerLiter.toString() }
            )
        }
    }

    fun setVehicle(id: Long) {
        _state.update { it.copy(vehicleId = id) }
        viewModelScope.launch { prefillFromLast(id) }
    }

    fun setDate(value: Long) = _state.update { it.copy(date = value) }
    fun setOdometer(value: String) = _state.update { it.copy(odometer = value) }
    fun setFullTank(value: Boolean) = _state.update { it.copy(isFullTank = value) }
    fun setMissedPrevious(value: Boolean) = _state.update { it.copy(missedPrevious = value) }
    fun setStation(value: String) = _state.update { it.copy(station = value) }
    fun setNote(value: String) = _state.update { it.copy(note = value) }

    // --- Ba ô lít / đơn giá / thành tiền tự suy ra nhau ---
    //
    // Quy tắc: ô được tính lại là ô mà người dùng ĐỘNG VÀO LÂU NHẤT — tức ô không phải ô vừa
    // gõ, cũng không phải ô gõ trước đó. Hai ô người dùng tự nhập luôn được giữ nguyên.
    //
    // Nếu không theo dõi [AddFuelUiState.lastEdited] thì có lỗi nuốt số: đơn giá được prefill
    // 23.500, người dùng gõ Thành tiền 100.000 (lít tự ra 4,26), rồi sửa đơn giá thành 24.000
    // -> phiên bản cũ tính lại Thành tiền = 102.240, xoá mất đúng con số người dùng vừa gõ.
    //
    // Mỗi KÝ TỰ gõ vào là một lần gọi hàm set, nên "ô vừa gõ" phải được so với
    // [AddFuelUiState.prevEdited] (ô KHÁC gõ gần nhất) chứ không phải với `lastEdited`:
    // gõ đến ký tự thứ hai trong cùng một ô thì `lastEdited` đã là chính ô đó rồi.

    /** Ghi nhận người dùng vừa gõ vào [field]; gõ tiếp cùng một ô thì không đổi gì. */
    private fun AddFuelUiState.markEdited(field: FuelField): AddFuelUiState =
        if (lastEdited == field) this else copy(lastEdited = field, prevEdited = lastEdited)

    fun setLiters(value: String) = _state.update { s ->
        val next = s.markEdited(FuelField.LITERS).copy(liters = value)
        if (next.prevEdited == FuelField.TOTAL) {
            next.copy(pricePerLiter = derivePrice(value, s.totalCost) ?: s.pricePerLiter)
        } else {
            next.copy(totalCost = deriveTotal(value, s.pricePerLiter) ?: s.totalCost)
        }
    }

    fun setPricePerLiter(value: String) = _state.update { s ->
        val next = s.markEdited(FuelField.PRICE).copy(pricePerLiter = value)
        if (next.prevEdited == FuelField.TOTAL) {
            next.copy(liters = deriveLiters(s.totalCost, value) ?: s.liters)
        } else {
            next.copy(totalCost = deriveTotal(s.liters, value) ?: s.totalCost)
        }
    }

    fun setTotalCost(value: String) = _state.update { s ->
        val next = s.markEdited(FuelField.TOTAL).copy(totalCost = value)
        if (next.prevEdited == FuelField.LITERS) {
            next.copy(pricePerLiter = derivePrice(s.liters, value) ?: s.pricePerLiter)
        } else {
            next.copy(liters = deriveLiters(value, s.pricePerLiter) ?: s.liters)
        }
    }

    /** Trả null khi chưa đủ dữ liệu để suy ra — khi đó ô đích được giữ nguyên. */
    private fun deriveTotal(liters: String, price: String): String? {
        val l = liters.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val p = price.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return (l * p).roundToLong().toString()
    }

    private fun deriveLiters(total: String, price: String): String? {
        val t = total.toLongOrNull()?.takeIf { it > 0 } ?: return null
        val p = price.toLongOrNull()?.takeIf { it > 0 } ?: return null
        // Locale.US để luôn ra dấu chấm thập phân — chuỗi này được toDoubleOrNull() đọc lại.
        return String.format(Locale.US, "%.2f", t.toDouble() / p)
    }

    private fun derivePrice(liters: String, total: String): String? {
        val l = liters.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
        val t = total.toLongOrNull()?.takeIf { it > 0 } ?: return null
        return (t / l).roundToLong().toString()
    }

    fun save() {
        val s = _state.value
        val vehicleId = s.vehicleId ?: return
        if (!s.canSave || s.saved || saving) return

        // Parse ở ngoài coroutine: canSave đã bảo đảm parse được, nhưng không dùng
        // toInt()/toDouble()/toLong() trần để một chuỗi quá lớn không làm app văng.
        val odometer = s.odometer.toIntOrNull() ?: return
        val liters = s.liters.toDoubleOrNull() ?: return
        val totalCost = s.totalCost.toLongOrNull() ?: return
        // Người dùng chỉ nhập lít + thành tiền thì tự suy ra đơn giá,
        // nếu không bản ghi sẽ lưu đơn giá 0 và lần sau gợi ý sai.
        val pricePerLiter = s.pricePerLiter.toLongOrNull()?.takeIf { it > 0 }
            ?: (totalCost / liters).roundToLong()

        saving = true
        _state.update { it.copy(error = null) }
        viewModelScope.launch {
            try {
                fuelRepo.save(
                    FuelEntry(
                        id = s.entryId,
                        vehicleId = vehicleId,
                        date = s.date,
                        odometer = odometer,
                        liters = liters,
                        pricePerLiter = pricePerLiter,
                        totalCost = totalCost,
                        isFullTank = s.isFullTank,
                        missedPrevious = s.missedPrevious,
                        station = s.station.takeIf { it.isNotBlank() },
                        note = s.note.takeIf { it.isNotBlank() },
                        photoUri = s.photoUri
                    )
                )
                selectedStore.select(vehicleId)
                _state.update { it.copy(saved = true) }
            } catch (e: Exception) {
                // Không để coroutine văng ra: một exception ở đây (xe vừa bị xoá ở màn khác ->
                // vi phạm khoá ngoại, hoặc lỗi ghi đĩa) sẽ sập app và nuốt sạch cái vừa gõ.
                _state.update { it.copy(error = "Không lưu được: ${e.message ?: "lỗi không rõ"}") }
            } finally {
                saving = false
            }
        }
    }
}
