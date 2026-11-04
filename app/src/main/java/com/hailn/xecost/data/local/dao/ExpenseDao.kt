package com.hailn.xecost.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.hailn.xecost.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {

    @Query("SELECT * FROM expenses WHERE vehicleId = :vehicleId ORDER BY date DESC")
    fun observeByVehicle(vehicleId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getById(id: Long): Expense?

    /**
     * Odo lớn nhất đã ghi cho xe này (cột odo của chi phí là tuỳ chọn nên có thể null).
     * Dùng để tính lại odo hiện tại của xe — xem [com.hailn.xecost.data.repository.OdometerSync].
     */
    @Query("SELECT MAX(odometer) FROM expenses WHERE vehicleId = :vehicleId")
    suspend fun maxOdometer(vehicleId: Long): Int?

    @Insert
    suspend fun insert(expense: Expense): Long

    @Update
    suspend fun update(expense: Expense)

    @Delete
    suspend fun delete(expense: Expense)
}
