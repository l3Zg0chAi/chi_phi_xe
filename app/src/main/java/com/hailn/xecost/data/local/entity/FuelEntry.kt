package com.hailn.xecost.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "fuel_entries",
    foreignKeys = [
        ForeignKey(
            entity = Vehicle::class,
            parentColumns = ["id"],
            childColumns = ["vehicleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("vehicleId"), Index("date")]
)
data class FuelEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val vehicleId: Long,
    val date: Long,

    /** Bắt buộc — không có odo thì không tính được mức tiêu hao. */
    val odometer: Int,

    val liters: Double,
    /** VND / lít */
    val pricePerLiter: Long,
    /** VND */
    val totalCost: Long,

    /**
     * Đổ đầy bình hay không. Mức tiêu hao chỉ được tính giữa hai lần đổ đầy,
     * nên đây là trường quan trọng nhất của bảng này.
     */
    val isFullTank: Boolean = true,

    /**
     * Người dùng tự đánh dấu "lần đổ trước tôi quên ghi".
     * Đoạn chứa bản ghi này sẽ bị loại khỏi phép tính tiêu hao.
     */
    val missedPrevious: Boolean = false,

    val station: String? = null,
    val note: String? = null,
    val photoUri: String? = null
)
