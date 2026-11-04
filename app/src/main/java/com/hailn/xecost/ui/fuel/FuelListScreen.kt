package com.hailn.xecost.ui.fuel

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.hailn.xecost.core.formatDate
import com.hailn.xecost.core.toKm
import com.hailn.xecost.core.toVnd
import com.hailn.xecost.ui.components.EmptyState

@Composable
fun FuelListScreen(
    onEdit: (Long) -> Unit,
    viewModel: FuelListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Khung còn trống lúc DB chưa emit. Không chặn ở đây thì frame đầu tiên của tab luôn
    // loé lên câu "Chưa có xe nào" dù người dùng đã có xe.
    if (state.loading) return

    if (!state.hasVehicle) {
        EmptyState("Chưa có xe nào. Thêm xe ở tab \"Xe\" trước.")
        return
    }
    if (state.rows.isEmpty()) {
        EmptyState("Chưa có lần đổ xăng nào.\nBấm nút + để ghi lần đầu tiên.")
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.rows, key = { it.entry.id }) { row ->
            FuelCard(
                row = row,
                onClick = { onEdit(row.entry.id) },
                onDelete = { viewModel.delete(row.entry) }
            )
        }
    }
}

@Composable
private fun FuelCard(row: FuelRow, onClick: () -> Unit, onDelete: () -> Unit) {
    val e = row.entry

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("${e.liters.format1()} L", style = MaterialTheme.typography.titleMedium)
                    if (!e.isFullTank) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "đổ một phần",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    text = "${e.date.formatDate()} · ${e.odometer.toKm()} · ${e.pricePerLiter.toVnd()}/L",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (row.consumption != null) {
                    Text(
                        text = "${row.consumption.format1()} L/100km",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(e.totalCost.toVnd(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xoá")
                }
            }
        }
    }
}
