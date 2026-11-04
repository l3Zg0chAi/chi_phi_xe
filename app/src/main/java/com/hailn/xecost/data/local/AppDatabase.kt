package com.hailn.xecost.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.hailn.xecost.data.local.dao.ExpenseDao
import com.hailn.xecost.data.local.dao.FuelDao
import com.hailn.xecost.data.local.dao.ReminderDao
import com.hailn.xecost.data.local.dao.VehicleDao
import com.hailn.xecost.data.local.entity.Expense
import com.hailn.xecost.data.local.entity.FuelEntry
import com.hailn.xecost.data.local.entity.Reminder
import com.hailn.xecost.data.local.entity.Vehicle

@Database(
    entities = [Vehicle::class, FuelEntry::class, Expense::class, Reminder::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun vehicleDao(): VehicleDao
    abstract fun fuelDao(): FuelDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun reminderDao(): ReminderDao

    companion object {
        const val NAME = "xecost.db"
    }
}
