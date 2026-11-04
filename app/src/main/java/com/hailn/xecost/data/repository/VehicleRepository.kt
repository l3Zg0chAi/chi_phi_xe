package com.hailn.xecost.data.repository

import androidx.room.withTransaction
import com.hailn.xecost.data.local.AppDatabase
import com.hailn.xecost.data.local.dao.VehicleDao
import com.hailn.xecost.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRepository @Inject constructor(
    private val db: AppDatabase,
    private val dao: VehicleDao,
    private val odometerSync: OdometerSync
) {
    fun observeActive(): Flow<List<Vehicle>> = dao.observeActive()

    fun observeAll(): Flow<List<Vehicle>> = dao.observeAll()

    fun observeById(id: Long): Flow<Vehicle?> = dao.observeById(id)

    suspend fun getById(id: Long): Vehicle? = dao.getById(id)

    /**
     * Lưu xe. Trả về id (id vừa sinh nếu là xe mới).
     *
     * Ba điều phải đúng thứ tự:
     *  1. Xe đánh dấu đã bán thì bỏ luôn cờ mặc định. Nếu không, cờ `isDefault` vẫn nằm trên
     *     một xe mà `observeActive()` không trả về, nên nhánh "lấy xe mặc định" ở các
     *     ViewModel không bao giờ khớp và app âm thầm rơi xuống xe đầu danh sách.
     *  2. `clearDefault()` / `markDefault()` chạy SAU `insert`/`update`, vì hai lệnh ghi đó
     *     cũng ghi đè cột `isDefault` của chính hàng này.
     *  3. Tính lại odo sau cùng: người dùng có thể vừa sửa `initialOdometer`.
     *
     * Lưu ý về hành vi: bỏ tick "xe mặc định" ở xe đang là mặc định thì app không còn xe mặc
     * định nào. Đây không phải lỗi — UI tự lùi về xe đầu danh sách.
     */
    suspend fun save(vehicle: Vehicle): Long = db.withTransaction {
        val toSave = if (vehicle.isActive) vehicle else vehicle.copy(isDefault = false)

        val id = if (toSave.id == 0L) dao.insert(toSave) else {
            dao.update(toSave); toSave.id
        }

        if (toSave.isDefault) {
            dao.clearDefault()
            dao.markDefault(id)
        }

        odometerSync.recalculate(id)
        id
    }

    suspend fun delete(vehicle: Vehicle) = dao.delete(vehicle)

    /**
     * Đặt xe mặc định từ menu ngoài danh sách.
     *
     * Phải kiểm tra `isActive` y như [save] làm, nếu không cờ mặc định rơi vào một xe đã bán —
     * xe mà `observeActive()` không trả về — nên nhánh "lấy xe mặc định" ở các ViewModel không
     * bao giờ khớp, app âm thầm rơi xuống xe đầu danh sách và người dùng không hiểu vì sao
     * lựa chọn của mình không có tác dụng. Chặn ở đây chứ không chỉ ở UI: UI có thể quên.
     */
    suspend fun setDefault(id: Long) = db.withTransaction {
        val vehicle = dao.getById(id)
        if (vehicle != null && vehicle.isActive) {
            dao.clearDefault()
            dao.markDefault(id)
        }
    }
}
