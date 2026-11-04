package com.hailn.xecost.domain

import com.hailn.xecost.data.local.entity.FuelEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Logic tính tiêu hao nằm trong Kotlin thuần nên test chạy trên JVM, không cần máy ảo.
 * Chạy: ./gradlew test
 */
class FuelStatsTest {

    private var nextId = 1L

    /**
     * `date` mặc định lấy theo odo chỉ để các bản ghi có thứ tự thời gian trùng với thứ tự
     * odo — thuật toán sắp xếp theo (odo, date) nên giá trị cụ thể không ảnh hưởng kết quả.
     * Khi cần phân biệt thứ tự của hai bản ghi TRÙNG odo thì truyền `date` tường minh.
     */
    private fun entry(
        odometer: Int,
        liters: Double,
        full: Boolean = true,
        missed: Boolean = false,
        cost: Long = (liters * 24_000).toLong(),
        date: Long = odometer.toLong()
    ) = FuelEntry(
        id = nextId++,
        vehicleId = 1,
        date = date,
        odometer = odometer,
        liters = liters,
        pricePerLiter = 24_000,
        totalCost = cost,
        isFullTank = full,
        missedPrevious = missed
    )

    // --- Trường hợp cơ bản ---

    @Test
    fun `danh sach rong`() {
        val summary = FuelStats.summarize(emptyList())
        assertTrue(summary.segments.isEmpty())
        assertNull(summary.averageConsumption)
        assertNull(summary.bestConsumption)
        assertEquals(0.0, summary.totalLiters, 0.001)
        assertEquals(0L, summary.totalFuelCost)
        assertEquals(0, summary.entryCount)
        assertEquals(0, summary.measuredDistance)
    }

    @Test
    fun `chua du hai lan do day thi khong tinh duoc`() {
        val summary = FuelStats.summarize(listOf(entry(10_000, 4.0)))
        assertNull(summary.averageConsumption)
        assertTrue(summary.segments.isEmpty())
    }

    @Test
    fun `khong co lan nao do day thi khong co doan nao`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, full = false),
                entry(10_200, 3.0, full = false),
                entry(10_400, 3.0, full = false)
            )
        )
        assertTrue(summary.segments.isEmpty())
        assertNull(summary.averageConsumption)
        // Tổng lít / tổng tiền vẫn tính trên mọi bản ghi.
        assertEquals(10.0, summary.totalLiters, 0.001)
        assertEquals(3, summary.entryCount)
    }

    @Test
    fun `hai lan do day lien tiep`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0)
            )
        )
        // 3 L cho 200 km -> 1,5 L/100km. Lít của mốc đầu không được tính.
        assertEquals(1, summary.segments.size)
        assertEquals(1.5, summary.averageConsumption!!, 0.001)
    }

    @Test
    fun `do mot phan o giua van duoc cong vao`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_150, 2.0, full = false),
                entry(10_300, 3.5)
            )
        )
        // (2.0 + 3.5) L cho 300 km
        assertEquals(1, summary.segments.size)
        assertEquals(5.5 / 300 * 100, summary.averageConsumption!!, 0.001)
    }

    @Test
    fun `lan do le o cuoi danh sach khong dong doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0),
                entry(10_300, 1.0, full = false)
            )
        )
        // Chỉ đoạn 10.000 -> 10.200 được tính; 1 L cuối chưa biết đi được bao xa.
        assertEquals(1, summary.segments.size)
        assertEquals(200, summary.measuredDistance)
        assertEquals(3.0, summary.segments[0].liters, 0.001)
        assertEquals(8.0, summary.totalLiters, 0.001)
    }

    // --- missedPrevious ---

    @Test
    fun `doan co lan quen ghi bi loai bo`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0, missed = true),  // đoạn này bỏ
                entry(10_400, 2.0)                  // đoạn này tính: 2 L / 200 km
            )
        )
        assertEquals(1, summary.segments.size)
        assertEquals(1.0, summary.averageConsumption!!, 0.001)
        assertEquals(10_200, summary.segments[0].fromOdometer)
    }

    @Test
    fun `ban ghi vua do day vua quen ghi thi khong con doan nao`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0, missed = true)
            )
        )
        assertTrue(summary.segments.isEmpty())
        assertNull(summary.averageConsumption)
    }

    @Test
    fun `quen ghi o ban ghi dau tien sau moc cung loai ca doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_100, 2.0, full = false, missed = true), // nằm trong đoạn -> loại
                entry(10_300, 3.0),                              // đoạn 10.000->10.300 bị bỏ
                entry(10_500, 2.5)                               // đoạn này sạch: 2,5 L / 200 km
            )
        )
        assertEquals(1, summary.segments.size)
        assertEquals(10_300, summary.segments[0].fromOdometer)
        assertEquals(1.25, summary.averageConsumption!!, 0.001)
        // Quãng đường của đoạn bị loại không được tính vào measuredDistance.
        assertEquals(200, summary.measuredDistance)
    }

    @Test
    fun `quen ghi khong lan sang doan ke tiep`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0, missed = true),
                entry(10_400, 2.0),
                entry(10_600, 3.0)
            )
        )
        // Hai đoạn sau mốc 10.200 đều hợp lệ: 2 L/200 km và 3 L/200 km.
        assertEquals(2, summary.segments.size)
        assertEquals(5.0 / 400 * 100, summary.averageConsumption!!, 0.001)
    }

    // --- Dữ liệu odo bất thường ---

    @Test
    fun `odo khong tang thi bo qua doan do`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_000, 3.0)
            )
        )
        assertTrue(summary.segments.isEmpty())
        assertNull(summary.averageConsumption)
    }

    @Test
    fun `odo trung nhau khong lam mat so lit cua doan sau`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, date = 1),
                entry(10_000, 3.0, date = 2),   // odo không nhúc nhích -> không đóng đoạn
                entry(10_300, 5.0, date = 3)
            )
        )
        // 3 L kia vẫn là xăng đổ sau mốc 10.000 nên phải tính cho đoạn 10.000 -> 10.300.
        assertEquals(1, summary.segments.size)
        assertEquals(8.0, summary.segments[0].liters, 0.001)
        assertEquals(300, summary.segments[0].distance)
        assertEquals(8.0 / 300 * 100, summary.averageConsumption!!, 0.001)
    }

    @Test
    fun `nhap lan xan thu tu van cho ket qua dung`() {
        // Người dùng nhập bản ghi odo cao trước, odo thấp sau (hoặc sửa odo lùi lại).
        val summary = FuelStats.summarize(
            listOf(
                entry(10_300, 3.5),
                entry(10_000, 4.0),
                entry(10_150, 2.0, full = false)
            )
        )
        // Thuật toán tự sắp xếp theo odo nên kết quả giống hệt khi nhập đúng thứ tự.
        val segment = summary.segments.single()
        assertEquals(10_000, segment.fromOdometer)
        assertEquals(10_300, segment.toOdometer)
        assertEquals(5.5, segment.liters, 0.001)
        assertEquals(5.5 / 300 * 100, summary.averageConsumption!!, 0.001)
    }

    @Test
    fun `doan khong co lit nao bi bo nhung van doi moc`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 0.0, cost = 0),   // dữ liệu hỏng: đổ đầy mà 0 lít
                entry(10_400, 2.0)
            )
        )
        assertEquals(1, summary.segments.size)
        assertEquals(10_200, summary.segments[0].fromOdometer)
        assertEquals(1.0, summary.averageConsumption!!, 0.001)
    }

    // --- Tổng hợp ---

    @Test
    fun `trung binh tinh tren tong quang duong chu khong phai trung binh cong`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_100, 2.0),   // 2 L / 100 km  -> 2,0 L/100km
                entry(10_500, 4.0)    // 4 L / 400 km  -> 1,0 L/100km
            )
        )
        // Tổng: 6 L / 500 km = 1,2 — không phải (2,0 + 1,0) / 2 = 1,5
        assertEquals(2, summary.segments.size)
        assertEquals(1.2, summary.averageConsumption!!, 0.001)
        assertEquals(500, summary.measuredDistance)
    }

    @Test
    fun `best va worst lay theo tung doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_100, 2.0),   // 2,0 L/100km
                entry(10_500, 4.0)    // 1,0 L/100km
            )
        )
        assertEquals(1.0, summary.bestConsumption!!, 0.001)
        assertEquals(2.0, summary.worstConsumption!!, 0.001)
    }

    @Test
    fun `chi phi cua doan khong tinh tien cua moc dau`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, cost = 100_000),
                entry(10_200, 3.0, cost = 75_000)
            )
        )
        val segment = summary.segments.single()
        assertEquals(75_000L, segment.cost)
        assertEquals(375.0, segment.costPerKm, 0.001)
    }

    @Test
    fun `doan ghi lai id cua ban ghi dong doan`() {
        val first = entry(10_000, 4.0)
        val second = entry(10_200, 3.0)
        val summary = FuelStats.summarize(listOf(first, second))
        assertEquals(second.id, summary.segments.single().endEntryId)
    }

    @Test
    fun `tong so lit va tong tien tinh tren moi ban ghi`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, cost = 100_000),
                entry(10_200, 3.0, cost = 75_000)
            )
        )
        assertEquals(7.0, summary.totalLiters, 0.001)
        assertEquals(175_000L, summary.totalFuelCost)
        assertEquals(2, summary.entryCount)
    }

    // --- Biên bổ sung ---

    @Test
    fun `cac lan do truoc moc dau tien khong tinh vao doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 2.0, full = false),  // chưa có mốc nào -> xăng này không thuộc đoạn nào
                entry(10_100, 4.0),                // mốc đầu
                entry(10_300, 3.0)                 // đóng đoạn: 3 L / 200 km
            )
        )
        val segment = summary.segments.single()
        assertEquals(10_100, segment.fromOdometer)
        assertEquals(3.0, segment.liters, 0.001)
        assertEquals(1.5, summary.averageConsumption!!, 0.001)
        // Tổng vẫn đếm cả 2 L đổ trước mốc.
        assertEquals(9.0, summary.totalLiters, 0.001)
    }

    @Test
    fun `quen ghi ngay o ban ghi lam moc dau tien khong loai doan sau no`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, missed = true),  // lần quên nằm TRƯỚC mốc -> không ảnh hưởng
                entry(10_200, 3.0)
            )
        )
        assertEquals(1, summary.segments.size)
        assertEquals(1.5, summary.averageConsumption!!, 0.001)
    }

    @Test
    fun `odo trung nhau khong lam mat tien cua doan sau`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, cost = 100_000, date = 1),
                entry(10_000, 3.0, cost = 75_000, date = 2),   // odo không nhúc nhích
                entry(10_300, 5.0, cost = 125_000, date = 3)
            )
        )
        // Tiền phải cộng dồn y như số lít: 75.000 + 125.000.
        val segment = summary.segments.single()
        assertEquals(200_000L, segment.cost)
        assertEquals(200_000.0 / 300, segment.costPerKm, 0.001)
    }

    @Test
    fun `tien cua doan bi loai khong lan sang doan sau`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, cost = 100_000),
                entry(10_200, 3.0, cost = 75_000, missed = true),  // đoạn này bị loại
                entry(10_400, 2.0, cost = 50_000)
            )
        )
        val segment = summary.segments.single()
        assertEquals(2.0, segment.liters, 0.001)
        assertEquals(50_000L, segment.cost)
    }

    @Test
    fun `doan ghi lai ngay cua ban ghi dong doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, date = 1_000),
                entry(10_200, 3.0, date = 2_000)
            )
        )
        assertEquals(2_000L, summary.segments.single().endDate)
    }

    @Test
    fun `lan do day trung odo o cuoi danh sach khong tao them doan`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0, date = 1),
                entry(10_300, 3.0, date = 2),
                entry(10_300, 2.0, date = 3)   // trùng odo của mốc mới -> không đóng thêm đoạn
            )
        )
        assertEquals(1, summary.segments.size)
        assertEquals(300, summary.measuredDistance)
        assertEquals(9.0, summary.totalLiters, 0.001)
    }

    @Test
    fun `chi co mot doan thi best bang worst`() {
        val summary = FuelStats.summarize(
            listOf(
                entry(10_000, 4.0),
                entry(10_200, 3.0)
            )
        )
        assertEquals(1.5, summary.bestConsumption!!, 0.001)
        assertEquals(1.5, summary.worstConsumption!!, 0.001)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `tao segment voi odo khong tang thi nem loi`() {
        // Khoá bất biến của FuelSegment: summarize không bao giờ tạo ra đoạn như thế này,
        // nhưng nếu sau này có chỗ khác dựng bằng tay thì phải chết ngay chứ không chia cho 0.
        FuelSegment(
            fromOdometer = 10_000,
            toOdometer = 10_000,
            liters = 1.0,
            cost = 0,
            endDate = 0
        )
    }
}
