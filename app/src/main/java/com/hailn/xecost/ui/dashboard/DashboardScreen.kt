package com.hailn.xecost.ui.dashboard

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hailn.xecost.core.format1
import com.hailn.xecost.core.format2
import com.hailn.xecost.core.formatDate
import com.hailn.xecost.core.formatShort
import com.hailn.xecost.core.toKm
import com.hailn.xecost.core.toVnd
import com.hailn.xecost.core.toVndShort
import com.hailn.xecost.ui.components.EmptyState
import com.hailn.xecost.ui.components.LegendDot
import com.hailn.xecost.ui.components.SectionTitle
import com.hailn.xecost.ui.components.StackedBar
import com.hailn.xecost.ui.components.StackedBarChart
import com.hailn.xecost.ui.components.StatCard
import com.hailn.xecost.ui.theme.XeCostColors

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Chưa có xe thì không dựng dashboard. Lưu ý phải chặn CẢ lúc `loading` còn true:
    // state khởi tạo của ViewModel là `DashboardUiState()` — loading = true, vehicles rỗng —
    // nên nếu chỉ chặn khi `!loading`, mỗi lần mở app người dùng sẽ thấy nhoáng một dashboard
    // giả (0 ₫, "0 km", "Chưa có khoản chi nào") rồi mới nhảy sang màn đúng.
    if (state.vehicles.isEmpty()) {
        if (!state.loading) {
            EmptyState("Chưa có xe nào.\nVào tab \"Xe\" để thêm chiếc đầu tiên.")
        }
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // --- Chọn xe ---
        Row(
            Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            state.vehicles.forEach { vehicle ->
                FilterChip(
                    selected = vehicle.id == state.selected?.id,
                    onClick = { viewModel.selectVehicle(vehicle.id) },
                    label = { Text(vehicle.name) }
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // --- Ô số liệu ---
        val summary = state.fuelSummary
        val delta = state.monthCost - state.previousMonthCost
        val deltaHint = when {
            state.previousMonthCost == 0L -> null
            delta > 0 -> "▲ ${delta.toVndShort()} so với tháng trước"
            delta < 0 -> "▼ ${(-delta).toVndShort()} so với tháng trước"
            else -> "không đổi"
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Chi phí tháng này",
                value = state.monthCost.toVndShort(),
                hint = deltaHint,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Mức tiêu hao",
                value = summary?.averageConsumption?.let { "${it.format1()} L" } ?: "—",
                hint = if (summary?.averageConsumption != null) "/100 km" else "cần 2 lần đổ đầy",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(
                label = "Chi phí mỗi km",
                value = state.costPerKm?.let { "${it.format2()} ₫" } ?: "—",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Odo hiện tại",
                value = (state.selected?.currentOdometer ?: 0).toKm(),
                hint = "đã đi ${(state.selected?.distanceTravelled ?: 0).toKm()}",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(20.dp))

        // --- Biểu đồ 6 tháng ---
        SectionTitle("Chi phí 6 tháng gần nhất")
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendDot(XeCostColors.fuel, "Nhiên liệu")
            LegendDot(XeCostColors.expense, "Chi phí khác")
        }
        Spacer(Modifier.height(8.dp))
        StackedBarChart(
            bars = state.buckets.map {
                StackedBar(
                    label = it.month.formatShort(),
                    lower = it.fuelCost.toFloat(),
                    upper = it.otherCost.toFloat()
                )
            },
            lowerColor = XeCostColors.fuel,
            upperColor = XeCostColors.expense
        )

        Spacer(Modifier.height(20.dp))

        // --- Gần đây ---
        SectionTitle("Gần đây")
        if (state.recent.isEmpty()) {
            EmptyState("Chưa có khoản chi nào được ghi.")
        } else {
            Card(Modifier.fillMaxWidth()) {
                state.recent.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider()
                    ActivityRow(item)
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun ActivityRow(item: ActivityItem) {
    val (title, subtitle) = when (item) {
        is ActivityItem.Fuel -> {
            "Đổ ${item.entry.liters.format1()} L" to
                "${item.entry.date.formatDate()} · ${item.entry.odometer.toKm()}"
        }
        is ActivityItem.Other -> {
            item.expense.title to
                "${item.expense.date.formatDate()} · ${item.expense.category.label}"
        }
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(8.dp))
        Text(item.amount.toVnd(), style = MaterialTheme.typography.bodyLarge)
    }
}
