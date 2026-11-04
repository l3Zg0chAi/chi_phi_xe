package com.hailn.xecost.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class VehicleType(val label: String) {
    MOTORBIKE("Xe máy"),
    CAR("Ô tô")
}

enum class FuelType(val label: String) {
    RON95("Xăng RON 95"),
    E5("Xăng E5 RON 92"),
    DIESEL("Dầu DO"),
    ELECTRIC("Điện")
}

@Entity(tableName = "vehicles")
data class Vehicle(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,
    val type: VehicleType,
    val fuelType: FuelType,

    val plateNumber: String? = null,
    val brand: String? = null,
    val model: String? = null,
    val year: Int? = null,
    val tankCapacity: Double? = null,

    /** Odo lúc bắt đầu dùng app. Dùng làm mốc tính tổng km đã đi. */
    val initialOdometer: Int = 0,
    /** Odo mới nhất, cập nhật mỗi khi ghi một bản ghi có odo lớn hơn. */
    val currentOdometer: Int = 0,

    val purchaseDate: Long? = null,
    /** VND */
    val purchasePrice: Long? = null,

    /** false = đã bán. Vẫn giữ lịch sử nhưng ẩn khỏi danh sách chính. */
    val isActive: Boolean = true,
    val isDefault: Boolean = false,

    val createdAt: Long = System.currentTimeMillis()
) {
    // Hai property dưới đây CHỈ có getter nên không sinh backing field -> Room/KSP không coi
    // là cột, không cần @Ignore (và @Ignore cũng không đặt được lên property không có field).
    // Nếu sau này thêm property có giá trị khởi tạo (vd `val x = f()`) vào thân class thì nó
    // SẼ có backing field và KSP sẽ báo lỗi — khi đó phải dùng @Ignore.

    val displayName: String
        get() = if (plateNumber.isNullOrBlank()) name else "$name · $plateNumber"

    /** Tổng km đã đi kể từ khi dùng app. */
    val distanceTravelled: Int
        get() = (currentOdometer - initialOdometer).coerceAtLeast(0)
}
