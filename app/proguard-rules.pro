# ============================================================================
# XeCost — R8 / ProGuard
#
# CẢNH BÁO RỦI RO (ghi lại theo yêu cầu review):
# buildTypes.release đang bật isMinifyEnabled = true. Bản DEBUG (assembleDebug)
# KHÔNG dùng file này nên không ảnh hưởng tới lần build đầu, nhưng bản release
# chưa từng được build/chạy thử lần nào. Những rule dưới đây là phòng thủ; phải
# chạy thử một bản release thật (installRelease) trước khi phát hành, vì lỗi do
# R8 cắt nhầm lớp chỉ lộ ra lúc runtime chứ không làm fail lúc compile.
#
# Ghi chú: Room, Hilt/Dagger, Compose và kotlinx-coroutines đều đã tự đóng gói
# "consumer proguard rules" trong artifact của chúng, nên phần lớn trường hợp
# R8 đã xử lý đúng. Các rule dưới đây chỉ bổ sung những chỗ dự án này thực sự
# có rủi ro (phản chiếu / nạp lớp theo tên).
# ============================================================================

# ---------------------------------------------------------------------------
# Room
# ---------------------------------------------------------------------------
# Entity: giữ nguyên tên lớp + field. Room sinh mã lúc compile nên về lý thuyết
# đổi tên vẫn chạy, nhưng schema JSON đã export lại ghi theo tên gốc; giữ lại
# cho khỏi lệch khi viết migration và khi debug dữ liệu.
-keep class com.hailn.xecost.data.local.entity.** { *; }

# TypeConverters được Room gọi qua lớp sinh ra, nhưng giữ lại cho chắc vì lớp
# Converters không bị code nào tham chiếu trực tiếp ngoài annotation.
-keep class com.hailn.xecost.data.local.Converters { *; }

# Lớp *_Impl do Room sinh ra được nạp bằng Class.forName(tên + "_Impl").
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class **_Impl { *; }
-dontwarn androidx.room.paging.**

# ---------------------------------------------------------------------------
# Hilt / Dagger
# ---------------------------------------------------------------------------
# Hilt nạp một số lớp sinh ra theo tên (Hilt_*, *_HiltModules, *_Factory...).
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper
-keepclasseswithmembers class * {
    @javax.inject.Inject <init>(...);
}
# ViewModel có @HiltViewModel được tra cứu qua tên lớp khi tạo bằng hiltViewModel().
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }

# ---------------------------------------------------------------------------
# Lifecycle / ViewModel
# ---------------------------------------------------------------------------
-keep class * extends androidx.lifecycle.ViewModel { <init>(...); }

# ---------------------------------------------------------------------------
# Compose / Navigation
# ---------------------------------------------------------------------------
# Compose compiler tự chèn rule; chỉ cần dọn cảnh báo của các API tuỳ chọn.
-dontwarn androidx.compose.**
# Navigation ở dự án này dùng route dạng chuỗi (navArgument + NavType), KHÔNG
# dùng type-safe route bằng kotlinx-serialization, nên không cần giữ @Serializable.

# ---------------------------------------------------------------------------
# Kotlin
# ---------------------------------------------------------------------------
-keepattributes *Annotation*, InnerClasses, Signature, Exceptions
-dontwarn kotlinx.coroutines.**
-dontwarn kotlin.Unit
