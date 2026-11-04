package com.hailn.xecost.data.repository

import androidx.room.withTransaction
import com.hailn.xecost.data.local.AppDatabase
import com.hailn.xecost.data.local.dao.ExpenseDao
import com.hailn.xecost.data.local.entity.Expense
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepository @Inject constructor(
    private val db: AppDatabase,
    private val expenseDao: ExpenseDao,
    private val odometerSync: OdometerSync
) {
    fun observeByVehicle(vehicleId: Long): Flow<List<Expense>> =
        expenseDao.observeByVehicle(vehicleId)

    suspend fun getById(id: Long): Expense? = expenseDao.getById(id)

    /**
     * Odo của chi phí là tuỳ chọn. Vẫn gọi [OdometerSync] vô điều kiện vì người dùng có thể
     * XOÁ odo khỏi một bản ghi đang giữ mức cao nhất — khi đó odo của xe phải tụt xuống.
     *
     * Màn hình sửa vẫn cho đổi xe, nên một khoản chi có thể CHUYỂN sang xe khác. Khi đó phải
     * tính lại odo cho cả xe cũ, nếu không xe cũ giữ mãi odo của bản ghi vừa bị chuyển đi.
     */
    suspend fun save(expense: Expense): Long = db.withTransaction {
        // Đọc xe cũ TRƯỚC khi ghi đè. Chỉ cần đọc khi đang sửa.
        val previousVehicleId =
            if (expense.id == 0L) null else expenseDao.getById(expense.id)?.vehicleId

        val id = if (expense.id == 0L) expenseDao.insert(expense) else {
            expenseDao.update(expense); expense.id
        }
        odometerSync.recalculate(expense.vehicleId)
        if (previousVehicleId != null && previousVehicleId != expense.vehicleId) {
            odometerSync.recalculate(previousVehicleId)
        }
        id
    }

    suspend fun delete(expense: Expense) = db.withTransaction {
        expenseDao.delete(expense)
        odometerSync.recalculate(expense.vehicleId)
    }
}
