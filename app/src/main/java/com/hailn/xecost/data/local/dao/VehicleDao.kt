package com.hailn.xecost.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hailn.xecost.data.local.entity.Vehicle
import kotlinx.coroutines.flow.Flow

@Dao
interface VehicleDao {

    @Query("SELECT * FROM vehicles WHERE isActive = 1 ORDER BY isDefault DESC, createdAt ASC")
    fun observeActive(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles ORDER BY isActive DESC, isDefault DESC, createdAt ASC")
    fun observeAll(): Flow<List<Vehicle>>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    fun observeById(id: Long): Flow<Vehicle?>

    @Query("SELECT * FROM vehicles WHERE id = :id")
    suspend fun getById(id: Long): Vehicle?

    @Insert
    suspend fun insert(vehicle: Vehicle): Long

    @Update
    suspend fun update(vehicle: Vehicle)

    @Delete
    suspend fun delete(vehicle: Vehicle)

    @Query("UPDATE vehicles SET isDefault = 0")
    suspend fun clearDefault()

    @Query("UPDATE vehicles SET isDefault = 1 WHERE id = :id")
    suspend fun markDefault(id: Long)

    /**
     * Đặt thẳng odo hiện tại. Giá trị truyền vào luôn được tính lại từ dữ liệu thật
     * (xem [com.hailn.xecost.data.repository.OdometerSync]), nên đây là phép gán chứ không
     * phải phép "kéo lên". Phiên bản cũ chỉ tăng-không-giảm làm odo kẹt ở số sai vĩnh viễn
     * khi người dùng sửa lại một bản ghi nhập nhầm hoặc xoá bản ghi có odo cao nhất.
     */
    @Query("UPDATE vehicles SET currentOdometer = :odometer WHERE id = :id")
    suspend fun setOdometer(id: Long, odometer: Int)
}
