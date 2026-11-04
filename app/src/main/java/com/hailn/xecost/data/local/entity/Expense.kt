package com.hailn.xecost.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

enum class ExpenseCategory(val label: String) {
    MAINTENANCE("Bảo dưỡng"),
    REPAIR("Sửa chữa"),
    INSURANCE("Bảo hiểm"),
    INSPECTION("Đăng kiểm"),
    ROAD_FEE("Phí đường bộ"),
    PARKING("Gửi xe"),
    WASH("Rửa xe"),
    TOLL("Cầu đường / VETC"),
    FINE("Phạt"),
    ACCESSORY("Phụ kiện"),
    OTHER("Khác")
}

@Entity(
    tableName = "expenses",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("date"), Index("category")]
)
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val vehicleId: Long,
    val date: Long,
    val category: ExpenseCategory,
    val title: String,
    /** VND */
    val amount: Long,

    val odometer: Int? = null,
    val place: String? = null,
    val note: String? = null,
    val photoUri: String? = null
)
