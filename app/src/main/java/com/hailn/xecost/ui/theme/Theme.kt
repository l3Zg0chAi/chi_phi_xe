package com.hailn.xecost.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

// Hai tông màu chính của app: xanh lá (nhiên liệu) và hổ phách (chi phí khác).
private val Green40 = Color(0xFF2E7D32)
private val Green80 = Color(0xFF8ED98F)
private val Amber40 = Color(0xFFB26A00)
private val Amber80 = Color(0xFFFFB95C)

// Các tông phụ. BẮT BUỘC phải khai báo: nếu chỉ đặt `primary`/`secondary` thì mọi vai trò
// còn lại vẫn giữ màu mặc định của Material (tím), mà app lại dùng chúng ở nhiều chỗ:
//   - FilterChip khi được chọn   -> secondaryContainer
//   - thanh tab (NavigationBar)  -> secondaryContainer làm vệt chỉ báo
//   - nút FAB "+"                -> primaryContainer
//   - nút "Lưu" (Button)         -> primary + onPrimary
//   - thẻ xe đang chọn           -> primaryContainer
// Thiếu thì nút xanh lá lại có chữ tím, chip chọn ra màu tím — nhìn như lỗi.
private val GreenOn40 = Color(0xFFFFFFFF)
private val GreenContainerLight = Color(0xFFB5F2B2)
private val GreenOnContainerLight = Color(0xFF002106)
private val GreenOn80 = Color(0xFF00390D)
private val GreenContainerDark = Color(0xFF105318)
private val GreenOnContainerDark = Color(0xFFAAF5A9)

private val AmberOn40 = Color(0xFFFFFFFF)
private val AmberContainerLight = Color(0xFFFFDDB3)
private val AmberOnContainerLight = Color(0xFF2B1700)
private val AmberOn80 = Color(0xFF4A2800)
private val AmberContainerDark = Color(0xFF693C00)
private val AmberOnContainerDark = Color(0xFFFFDDB3)

private val LightColors = lightColorScheme(
    primary = Green40,
    onPrimary = GreenOn40,
    primaryContainer = GreenContainerLight,
    onPrimaryContainer = GreenOnContainerLight,
    secondary = Amber40,
    onSecondary = AmberOn40,
    secondaryContainer = AmberContainerLight,
    onSecondaryContainer = AmberOnContainerLight
)

private val DarkColors = darkColorScheme(
    primary = Green80,
    onPrimary = GreenOn80,
    primaryContainer = GreenContainerDark,
    onPrimaryContainer = GreenOnContainerDark,
    secondary = Amber80,
    onSecondary = AmberOn80,
    secondaryContainer = AmberContainerDark,
    onSecondaryContainer = AmberOnContainerDark
)

/** Màu riêng của app, không nằm trong bảng màu Material. */
object XeCostColors {
    val fuel = Color(0xFF2E7D32)
    val expense = Color(0xFFE07B39)
}

@Composable
fun XeCostTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    /** Android 12+ lấy màu theo hình nền máy người dùng. */
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
