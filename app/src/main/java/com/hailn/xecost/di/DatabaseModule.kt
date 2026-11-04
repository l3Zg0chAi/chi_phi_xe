package com.hailn.xecost.di

import android.content.Context
import androidx.room.Room
import com.hailn.xecost.data.local.AppDatabase
import com.hailn.xecost.data.local.dao.ExpenseDao
import com.hailn.xecost.data.local.dao.FuelDao
import com.hailn.xecost.data.local.dao.ReminderDao
import com.hailn.xecost.data.local.dao.VehicleDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt tạo MỘT bản AppDatabase duy nhất cho cả app và "tiêm" vào nơi nào cần.
 * Không chỗ nào được tự gọi Room.databaseBuilder lần nữa.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME)
            // Chưa có migration vì mới version 1. Khi đổi schema: thêm .addMigrations(...)
            // KHÔNG dùng fallbackToDestructiveMigration() — nó xoá sạch dữ liệu người dùng.
            .build()

    // DAO cũng để @Singleton: bản thân Room đã cache mỗi DAO trong AppDatabase nên không đổi
    // hành vi, nhưng nhờ vậy Hilt không gọi lại db.vehicleDao() ở mỗi chỗ tiêm, và vòng đời
    // của DAO khớp hẳn với vòng đời của AppDatabase.
    @Provides
    @Singleton
    fun provideVehicleDao(db: AppDatabase): VehicleDao = db.vehicleDao()

    @Provides
    @Singleton
    fun provideFuelDao(db: AppDatabase): FuelDao = db.fuelDao()

    @Provides
    @Singleton
    fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()

    // Chưa có màn hình nhắc lịch nên chưa chỗ nào tiêm ReminderDao. Hilt không báo lỗi với
    // một @Provides không ai dùng, cứ để sẵn cho tính năng Reminder.
    @Provides
    @Singleton
    fun provideReminderDao(db: AppDatabase): ReminderDao = db.reminderDao()
}
