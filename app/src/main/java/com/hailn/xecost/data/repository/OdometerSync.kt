package com.hailn.xecost.data.repository

import com.hailn.xecost.data.local.dao.ExpenseDao
import com.hailn.xecost.data.local.dao.FuelDao
import com.hailn.xecost.data.local.dao.VehicleDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Giữ `Vehicle.currentOdometer` luôn khớp với dữ liệu thật.
 *
 * Trước đây mỗi lần lưu chỉ "kéo" odo lên (`UPDATE ... WHERE currentOdometer < :odometer`).
 * Cách đó sai trong hai tình huống rất dễ gặp:
 *  - nhập nhầm odo 20.000 thay vì 10.200 rồi vào sửa lại cho đúng
 *  - xoá bản ghi đang giữ odo cao nhất
 * Cả hai đều để lại `currentOdometer` kẹt ở số cao, kéo theo `distanceTravelled` và ô
 * "chi phí mỗi km" trên Dashboard sai vĩnh viễn, mà không có cách nào tự khỏi.
 *
 * Nay odo được TÍNH LẠI từ đầu sau mỗi lần thêm / sửa / xoá:
 * `max(odo ban đầu của xe, MAX(odo) của fuel_entries, MAX(odo) của expenses)`.
 *
 * Luôn gọi bên trong `db.withTransaction { }` của repository gọi nó, để phép đọc MAX và phép
 * ghi odo nằm cùng một giao dịch với chính thao tác vừa làm thay đổi dữ liệu.
 */
@Singleton
class OdometerSync @Inject constructor(
    private val vehicleDao: VehicleDao,
    private val fuelDao: FuelDao,
    private val expenseDao: ExpenseDao
) {
    suspend fun recalculate(vehicleId: Long) {
        val vehicle = vehicleDao.getById(vehicleId) ?: return
        val maxFuel = fuelDao.maxOdometer(vehicleId) ?: 0
        val maxExpense = expenseDao.maxOdometer(vehicleId) ?: 0

        // Odo ban đầu là sàn: xoá hết bản ghi thì odo quay về mốc lúc bắt đầu dùng app,
        // chứ không về 0.
        val odometer = maxOf(vehicle.initialOdometer, maxFuel, maxExpense)
        if (odometer != vehicle.currentOdometer) {
            vehicleDao.setOdometer(vehicleId, odometer)
        }
    }
}
