package com.hailn.xecost.domain

import com.hailn.xecost.data.local.entity.FuelEntry

/**
 * Một đoạn giữa hai lần đổ ĐẦY BÌNH liên tiếp — đơn vị nhỏ nhất có thể tính tiêu hao.
 *
 * Bất biến: `toOdometer > fromOdometer`. Mọi đoạn đều do [FuelStats.summarize] tạo ra và
 * hàm đó đã chặn trường hợp odo không tăng, nên [consumption] / [costPerKm] không bao giờ
 * chia cho 0. `require` ở đây chỉ để khoá bất biến lại, phòng khi sau này có chỗ khác tạo
 * `FuelSegment` bằng tay.
 */
data class FuelSegment(
    val fromOdometer: Int,
    val toOdometer: Int,
    val liters: Double,
    val cost: Long,
    val endDate: Long,
    /**
     * Id của bản ghi đóng đoạn. Dùng cái này để gắn mức tiêu hao vào đúng dòng trong danh
     * sách — gắn theo odo là sai vì nhiều bản ghi có thể trùng odo.
     */
    val endEntryId: Long = 0L
) {
    init {
        require(toOdometer > fromOdometer) {
            "Đoạn phải có quãng đường dương: $fromOdometer -> $toOdometer"
        }
    }

    val distance: Int get() = toOdometer - fromOdometer

    /** Lít / 100 km */
    val consumption: Double get() = liters / distance * 100.0

    /** VND / km */
    val costPerKm: Double get() = cost.toDouble() / distance
}

data class FuelSummary(
    val segments: List<FuelSegment>,
    val totalLiters: Double,
    val totalFuelCost: Long,
    val entryCount: Int
) {
    /**
     * Lít / 100 km trung bình, tính trên toàn bộ các đoạn hợp lệ. Null nếu chưa đủ dữ liệu.
     *
     * Chia TỔNG lít cho TỔNG quãng đường — không phải trung bình cộng mức tiêu hao của từng
     * đoạn. Đoạn dài phải có trọng số lớn hơn đoạn ngắn.
     */
    val averageConsumption: Double?
        get() {
            val distance = segments.sumOf { it.distance }
            if (distance <= 0) return null
            return segments.sumOf { it.liters } / distance * 100.0
        }

    val bestConsumption: Double? get() = segments.minOfOrNull { it.consumption }
    val worstConsumption: Double? get() = segments.maxOfOrNull { it.consumption }

    val measuredDistance: Int get() = segments.sumOf { it.distance }
}

/**
 * Tính mức tiêu hao nhiên liệu.
 *
 * Nguyên tắc: chỉ tính giữa HAI LẦN ĐỔ ĐẦY BÌNH.
 * Lượng xăng dùng cho đoạn đó = tổng số lít của mọi lần đổ SAU mốc đầu, tính cả mốc cuối.
 * (Số lít của mốc đầu thuộc về đoạn trước đó, không tính vào đây.)
 *
 * Một đoạn bị loại khi:
 *  - có bản ghi nào trong đoạn được đánh dấu `missedPrevious` (người dùng quên ghi một lần đổ)
 *  - tổng số lít của đoạn bằng 0 (dữ liệu hỏng)
 *
 * Khi odo không tăng thì KHÔNG đóng đoạn và cũng KHÔNG dời mốc: số lít vừa đổ vẫn là xăng
 * đã nạp sau mốc nên phải được cộng tiếp cho đoạn sau, nếu không sẽ bị mất.
 */
object FuelStats {

    fun summarize(entries: List<FuelEntry>): FuelSummary {
        val sorted = entries.sortedWith(compareBy({ it.odometer }, { it.date }))

        val segments = mutableListOf<FuelSegment>()

        // Mốc đầu của đoạn đang xét: lần đổ đầy gần nhất phía trước.
        var anchor: FuelEntry? = null
        var litersSinceAnchor = 0.0
        var costSinceAnchor = 0L
        var missedInSegment = false

        for (entry in sorted) {
            val current = anchor
            if (current == null) {
                // Chưa có mốc nào. Lần đổ đầy đầu tiên trở thành mốc.
                // `missedPrevious` của chính bản ghi này không ảnh hưởng: lần đổ bị quên nằm
                // trước mốc, tức thuộc đoạn chưa tồn tại.
                if (entry.isFullTank) anchor = entry
                continue
            }

            // Lần đổ bị quên nằm giữa bản ghi trước và bản ghi này -> rơi vào đoạn đang xét.
            // Đúng cả khi bản ghi này là bản ghi đầu tiên sau mốc, và cả khi nó là bản ghi
            // đóng đoạn (vừa isFullTank vừa missedPrevious).
            if (entry.missedPrevious) missedInSegment = true

            litersSinceAnchor += entry.liters
            costSinceAnchor += entry.totalCost

            if (!entry.isFullTank) continue

            val distance = entry.odometer - current.odometer
            if (distance <= 0) {
                // Odo trùng / nhập sai: không tính được đoạn, nhưng giữ nguyên mốc cũ và
                // giữ nguyên số lít đã cộng để đoạn sau không bị thiếu nhiên liệu.
                continue
            }

            // Gặp lần đổ đầy ở odo lớn hơn -> đóng đoạn.
            if (!missedInSegment && litersSinceAnchor > 0) {
                segments += FuelSegment(
                    fromOdometer = current.odometer,
                    toOdometer = entry.odometer,
                    liters = litersSinceAnchor,
                    cost = costSinceAnchor,
                    endDate = entry.date,
                    endEntryId = entry.id
                )
            }

            // Dù đoạn có bị loại hay không, lần đổ đầy này vẫn là mốc mới: bình đã đầy trở
            // lại nên lượng xăng còn trong bình đã biết, không còn kéo theo sai số cũ.
            anchor = entry
            litersSinceAnchor = 0.0
            costSinceAnchor = 0L
            missedInSegment = false
        }

        return FuelSummary(
            segments = segments,
            totalLiters = entries.sumOf { it.liters },
            totalFuelCost = entries.sumOf { it.totalCost },
            entryCount = entries.size
        )
    }
}
