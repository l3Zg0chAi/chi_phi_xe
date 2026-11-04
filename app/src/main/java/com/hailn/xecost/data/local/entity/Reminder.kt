package com.hailn.xecost.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ReminderType(val label: String) {
    OIL_CHANGE("Thay nhớt"),
    INSPECTION("Đăng kiểm"),
    INSURANCE("Bảo hiểm"),
    ROAD_FEE("Phí đường bộ"),
    TIRE("Thay lốp"),
    CUSTOM("Khác")
}

/**
 * Bảng này chưa được dùng ở MVP nhưng tạo sẵn từ đầu, để khi thêm tính năng nhắc nhở
 * không phải viết migration cho người dùng đã cài app.
 */
@Entity(
    tableName = "reminders",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId")]
)
data class Reminder(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val vehicleId: Long,
    val type: ReminderType,
    val title: String,

    /** Hạn theo thời gian. */
    val dueDate: Long? = null,
    /** Hạn theo số km trên odo. */
    val dueOdometer: Int? = null,

    /** Chu kỳ lặp lại sau khi hoàn thành. */
    val intervalDays: Int? = null,
    val intervalKm: Int? = null,

    val lastDoneDate: Long? = null,
    val lastDoneOdometer: Int? = null,

    val isEnabled: Boolean = true
)
