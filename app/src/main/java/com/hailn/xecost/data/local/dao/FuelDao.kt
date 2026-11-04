package com.hailn.xecost.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hailn.xecost.data.local.entity.FuelEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface FuelDao {

    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<FuelEntry>>

    /** Sắp xếp tăng dần theo odo — dạng mà thuật toán tính tiêu hao cần. */
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY odometer ASC, date ASC")
    fun observeByVehicleChronological(vehicleId: Long): Flow<List<FuelEntry>>

    @Query("SELECT * FROM fuel_entries WHERE id = :id")
    suspend fun getById(id: Long): FuelEntry?

    /**
     * Lần đổ gần nhất — dùng để gợi ý đơn giá và lấy mốc odo cảnh báo nhập lùi.
     * Phải phá hoà bằng odometer giống [observeByVehicle]: nhiều bản ghi cùng ngày mà chỉ
     * ORDER BY date thì SQLite trả về bản nào cũng được, mốc odo sẽ sai.
     */
    @Query("SELECT * FROM fuel_entries WHERE vehicleId = :vehicleId ORDER BY date DESC, odometer DESC LIMIT 1")
    suspend fun getLatest(vehicleId: Long): FuelEntry?

    /**
     * Odo lớn nhất đã ghi cho xe này; null khi chưa có bản ghi nào.
     * Dùng để TÍNH LẠI odo hiện tại của xe sau mỗi lần thêm/sửa/xoá, thay vì chỉ "kéo lên".
     */
    @Query("SELECT MAX(odometer) FROM fuel_entries WHERE vehicleId = :vehicleId")
    suspend fun maxOdometer(vehicleId: Long): Int?

    @Insert
    suspend fun insert(entry: FuelEntry): Long

    @Update
    suspend fun update(entry: FuelEntry)

    @Delete
    suspend fun delete(entry: FuelEntry)
}
