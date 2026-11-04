package com.hailn.xecost.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun StatCard(
    label: String,
    value: String,
    hint: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge
            )
            if (hint != null) {
                Text(
                    text = hint,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = modifier.padding(vertical = 8.dp)
    )
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

data class StackedBar(
    val label: String,
    val lower: Float,
    val upper: Float
)

/**
 * Biểu đồ cột chồng, vẽ bằng Canvas — không cần thư viện ngoài.
 * Khi cần tương tác (chạm vào cột để xem số) thì thay bằng Vico.
 */
@Composable
fun StackedBarChart(
    bars: List<StackedBar>,
    lowerColor: Color,
    upperColor: Color,
    modifier: Modifier = Modifier
) {
    // Kẹp về 0 trước khi tính: số âm (dữ liệu nhập sai) sẽ cho `Size` chiều cao âm,
    // Canvas bỏ qua không vẽ gì và `max` cũng bị kéo lệch -> các cột còn lại sai tỉ lệ.
    // `takeIf { it > 0f }` lo nốt hai trường hợp danh sách rỗng và tất cả đều bằng 0
    // (nếu không sẽ chia cho 0 ở dưới).
    val max = bars
        .maxOfOrNull { it.lower.coerceAtLeast(0f) + it.upper.coerceAtLeast(0f) }
        ?.takeIf { it > 0f } ?: 1f

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
        ) {
            if (bars.isEmpty()) return@Canvas
            // Mỗi cột chiếm một ô rộng bằng nhau, cột nằm giữa ô.
            // Hàng nhãn bên dưới chia ô y hệt (weight 1f) nên nhãn luôn thẳng cột.
            val slot = size.width / bars.size
            val barWidth = slot * 0.6f

            bars.forEachIndexed { index, bar ->
                val x = index * slot + (slot - barWidth) / 2f
                val lowerH = size.height * (bar.lower.coerceAtLeast(0f) / max)
                val upperH = size.height * (bar.upper.coerceAtLeast(0f) / max)

                drawRect(
                    color = lowerColor,
                    topLeft = Offset(x, size.height - lowerH),
                    size = Size(barWidth, lowerH)
                )
                drawRect(
                    color = upperColor,
                    topLeft = Offset(x, size.height - lowerH - upperH),
                    size = Size(barWidth, upperH)
                )
            }
        }

        Row(Modifier.fillMaxWidth()) {
            bars.forEach {
                Text(
                    text = it.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Canvas(Modifier.width(10.dp).height(10.dp)) { drawRect(color) }
        Text(
            text = "  $label",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
