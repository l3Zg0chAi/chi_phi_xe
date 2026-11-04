package com.hailn.xecost.data.local

import androidx.room.TypeConverter
import com.hailn.xecost.data.local.entity.ExpenseCategory
import com.hailn.xecost.data.local.entity.FuelType
import com.hailn.xecost.data.local.entity.ReminderType
import com.hailn.xecost.data.local.entity.VehicleType

/**
 * Room chỉ lưu được kiểu nguyên thuỷ, nên enum phải chuyển thành String.
 * Lưu theo TÊN enum (không phải ordinal) — đổi thứ tự enum sau này sẽ không làm hỏng dữ liệu.
 */
class Converters {

    @TypeConverter
    fun vehicleTypeToString(value: VehicleType): String = value.name

    @TypeConverter
    fun stringToVehicleType(value: String): VehicleType = VehicleType.valueOf(value)

    @TypeConverter
    fun fuelTypeToString(value: FuelType): String = value.name

    @TypeConverter
    fun stringToFuelType(value: String): FuelType = FuelType.valueOf(value)

    @TypeConverter
    fun expenseCategoryToString(value: ExpenseCategory): String = value.name

    @TypeConverter
    fun stringToExpenseCategory(value: String): ExpenseCategory = ExpenseCategory.valueOf(value)

    @TypeConverter
    fun reminderTypeToString(value: ReminderType): String = value.name

    @TypeConverter
    fun stringToReminderType(value: String): ReminderType = ReminderType.valueOf(value)
}
