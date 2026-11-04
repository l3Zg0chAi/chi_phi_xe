package com.hailn.xecost.data.repository

import androidx.room.withTransaction
import com.hailn.xecost.data.local.AppDatabase
import com.hailn.xecost.data.local.dao.FuelDao
import com.hailn.xecost.data.local.entity.FuelEntry
import com.hailn.xecost.domain.FuelStats
import com.hailn.xecost.domain.FuelSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FuelRepository @Inject constructor(
    private val db: AppDatabase,
    private val fuelDao: FuelDao,
    private val odometerSync: OdometerSync
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelEntry>> =
        fuelDao.observeByVehicle(vehicleId)

    /** Tóm tắt tiêu hao, tự tính lại mỗi khi dữ liệu đổi. */
    fun observeSummary(vehicleId: Long): Flow<FuelSummary> =
        fuelDao.observeByVehicleChronological(vehicleId).map { FuelStats.summarize(it) }

    suspend fun getById(id: Long): FuelEntry? = fuelDao.getById(id)

    suspend fun getLatest(vehicleId: Long): FuelEntry? = fuelDao.getLatest(vehicleId)

    /**
     * Lưu bản ghi rồi tính lại odo của xe từ dữ liệu thật — xem [OdometerSync].
     *
     * Màn hình sửa vẫn cho đổi xe (chip chọn xe hiện cả khi đang sửa), nên một bản ghi có thể
     * CHUYỂN từ xe này sang xe khác. Khi đó phải tính lại odo cho CẢ HAI xe: nếu chỉ tính cho
     * xe mới thì xe cũ giữ mãi odo của bản ghi vừa bị chuyển đi, và không có thao tác nào sau
     * này gỡ được — đúng kiểu sai vĩnh viễn mà [OdometerSync] sinh ra để chặn.
     */
    suspend fun save(entry: FuelEntry): Long = db.withTransaction {
        // Đọc xe cũ TRƯỚC khi ghi đè. Chỉ cần đọc khi đang sửa.
        val previousVehicleId =
            if (entry.id == 0L) null else fuelDao.getById(entry.id)?.vehicleId

        val id = if (entry.id == 0L) fuelDao.insert(entry) else {
            fuelDao.update(entry); entry.id
        }
        odometerSync.recalculate(entry.vehicleId)
        if (previousVehicleId != null && previousVehicleId != entry.vehicleId) {
            odometerSync.recalculate(previousVehicleId)
        }
        id
    }

    /** Xoá cũng phải tính lại odo: bản ghi vừa xoá có thể đang giữ odo cao nhất. */
    suspend fun delete(entry: FuelEntry) = db.withTransaction {
        fuelDao.delete(entry)
        odometerSync.recalculate(entry.vehicleId)
    }
}
