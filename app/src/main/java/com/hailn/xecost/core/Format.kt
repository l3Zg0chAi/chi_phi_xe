package com.hailn.xecost.core

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs

private val VI_VN: Locale = Locale("vi", "VN")

private fun vnSymbols() = DecimalFormatSymbols(VI_VN).apply {
    groupingSeparator = '.'
    decimalSeparator = ','
}

// DecimalFormat KHÔNG thread-safe (nó giữ state khi format). Các hàm dưới đây được gọi từ
// Compose (recomposition có thể chạy trên nhiều luồng) lẫn từ coroutine nền, nên không được
// dùng chung một instance — mỗi luồng giữ một bản riêng.
// Dùng `object : ThreadLocal<...>()` thay cho ThreadLocal.withInitial để khỏi phụ thuộc
// java.util.function.Supplier.
private val moneyFormat = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue() = DecimalFormat("#,##0", vnSymbols())
}
private val decimal1 = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue() = DecimalFormat("#,##0.0", vnSymbols())
}
private val decimal2 = object : ThreadLocal<DecimalFormat>() {
    override fun initialValue() = DecimalFormat("#,##0.00", vnSymbols())
}

/** 1250000 -> "1.250.000 ₫" */
fun Long.toVnd(): String = "${moneyFormat.get().format(this)} ₫"

// Ngưỡng nâng đơn vị KHÔNG phải số tròn. 999.999 ₫ chia cho 1.000 rồi làm tròn 1 chữ số thập
// phân ra "1.000,0k" — dài hơn cả số gốc và dễ đọc nhầm. Mọi giá trị mà sau khi làm tròn sẽ
// thành 1.000 ở đơn vị nhỏ đều được đẩy lên đơn vị lớn hơn (999.999 -> "1,0 tr").
private const val BILLION_THRESHOLD = 999_950_000L
private const val MILLION_THRESHOLD = 999_950L
private const val THOUSAND_THRESHOLD = 1_000L

/**
 * 1250000 -> "1,25 tr" — dùng cho ô số liệu nhỏ trên dashboard.
 *
 * Xử lý cả số âm (chênh lệch chi phí giữa hai tháng có thể âm): rút gọn theo trị tuyệt đối
 * rồi thêm dấu trừ, nếu không -1.250.000 sẽ rơi hết xuống nhánh cuối và in ra đủ 7 chữ số.
 */
fun Long.toVndShort(): String {
    // abs(Long.MIN_VALUE) tràn số và trả về chính nó -> kẹp lại. Số tiền thật không bao giờ
    // tới mức này, nhưng để vậy thì nhánh dưới sẽ chọn sai.
    val value = if (this == Long.MIN_VALUE) Long.MAX_VALUE else abs(this)
    val sign = if (this < 0) "-" else ""
    val short = when {
        value >= BILLION_THRESHOLD -> "${decimal1.get().format(value / 1_000_000_000.0)} tỷ"
        value >= MILLION_THRESHOLD -> "${decimal1.get().format(value / 1_000_000.0)} tr"
        value >= THOUSAND_THRESHOLD -> "${decimal1.get().format(value / 1_000.0)}k"
        else -> moneyFormat.get().format(value)
    }
    return sign + short
}

fun Double.format1(): String = decimal1.get().format(this)
fun Double.format2(): String = decimal2.get().format(this)

// toLong() tường minh để chọn đúng overload NumberFormat.format(long) — Kotlin không tự nới
// Int thành Long khi gọi hàm Java, để nguyên Int sẽ rơi vào format(Object).
fun Int.toKm(): String = "${moneyFormat.get().format(this.toLong())} km"

// DateTimeFormatter là immutable và thread-safe nên dùng chung được.
// Chỉ định Locale rõ ràng để máy đang đặt ngôn ngữ dùng hệ chữ số khác (vd: ar) không in ra
// chữ số Ả Rập.
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", VI_VN)
private val monthFormatter = DateTimeFormatter.ofPattern("MM/yyyy", VI_VN)
private val shortMonthFormatter = DateTimeFormatter.ofPattern("MM", VI_VN)

fun Long.toLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

fun LocalDate.toEpochMillis(): Long =
    atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

fun Long.formatDate(): String = toLocalDate().format(dateFormatter)

fun YearMonth.formatMonth(): String = atDay(1).format(monthFormatter)

fun YearMonth.formatShort(): String = "T${atDay(1).format(shortMonthFormatter)}"

fun Long.toYearMonth(): YearMonth = YearMonth.from(toLocalDate())
