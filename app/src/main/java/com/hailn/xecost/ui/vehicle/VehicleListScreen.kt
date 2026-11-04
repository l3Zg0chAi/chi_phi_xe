package com.hailn.xecost.ui.vehicle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hailn.xecost.core.toKm
import com.hailn.xecost.data.local.entity.Vehicle
import com.hailn.xecost.ui.components.EmptyState

@Composable
fun VehicleListScreen(
    onEdit: (Long) -> Unit,
    viewModel: VehicleListViewModel = hiltViewModel()
) {
    val vehicles by viewModel.vehicles.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedId.collectAsStateWithLifecycle()

    if (vehicles.isEmpty()) {
        EmptyState(
            text = "Chưa có xe nào.\nBấm nút + ở góc dưới bên phải để thêm chiếc đầu tiên.",
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(vehicles, key = { it.id }) { vehicle ->
            VehicleCard(
                vehicle = vehicle,
                isSelected = vehicle.id == selectedId,
                onClick = { viewModel.select(vehicle) },
                onEdit = { onEdit(vehicle.id) },
                onSetDefault = { viewModel.setDefault(vehicle) },
                onToggleActive = { viewModel.toggleActive(vehicle) },
                onDelete = { viewModel.delete(vehicle) }
            )
        }
    }
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onSetDefault: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = if (isSelected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(vehicle.displayName, style = MaterialTheme.typography.titleMedium)
                }
                Text(
                    text = "${vehicle.type.label} · ${vehicle.fuelType.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Odo ${vehicle.currentOdometer.toKm()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // Hai nhãn dưới đây chỉ để xem, KHÔNG bấm được: trước đây chúng là AssistChip
                // nên chạm vào nhãn "Đã bán" là âm thầm bật lại xe mà người dùng không hề
                // định làm. Muốn đổi thì vào menu ba chấm.
                if (vehicle.isDefault || !vehicle.isActive) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (vehicle.isDefault) {
                            Text(
                                text = "Mặc định",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (!vehicle.isActive) {
                            Text(
                                text = "Đã bán",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }

            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Tuỳ chọn")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Sửa") },
                        onClick = { menuOpen = false; onEdit() }
                    )
                    // Xe đã bán không được làm xe mặc định: observeActive() không trả về nó,
                    // nên các màn hình khác sẽ tìm xe mặc định mà không thấy.
                    if (vehicle.isActive && !vehicle.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Đặt làm mặc định") },
                            onClick = { menuOpen = false; onSetDefault() }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(if (vehicle.isActive) "Đánh dấu đã bán" else "Dùng lại") },
                        onClick = { menuOpen = false; onToggleActive() }
                    )
                    DropdownMenuItem(
                        text = { Text("Xoá xe và toàn bộ dữ liệu") },
                        onClick = { menuOpen = false; confirmDelete = true }
                    )
                }
            }
        }
    }

    // Xoá xe kéo theo CASCADE toàn bộ lần đổ xăng / chi phí / nhắc nhở,
    // không hoàn tác được -> bắt buộc hỏi lại.
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Xoá xe này?") },
            text = {
                Text(
                    "Xoá \"${vehicle.displayName}\" sẽ xoá luôn toàn bộ lần đổ xăng, " +
                        "khoản chi và nhắc nhở của xe. Không thể hoàn tác.\n\n" +
                        "Nếu chỉ muốn cất xe đi, hãy chọn \"Đánh dấu đã bán\"."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    onDelete()
                }) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("Huỷ") }
            }
        )
    }
}
