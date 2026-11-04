package com.hailn.xecost.ui.fuel

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hailn.xecost.ui.components.DateField
import com.hailn.xecost.ui.components.NumberField
import com.hailn.xecost.ui.components.LabeledTextField

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFuelScreen(
    onDone: () -> Unit,
    viewModel: AddFuelViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Lưu xong thì đóng màn hình. Dùng LaunchedEffect để chỉ chạy một lần.
    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Sửa lần đổ xăng" else "Ghi đổ xăng") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                // enableEdgeToEdge() đặt decorFitsSystemWindows = false, nên adjustResize
                // trong manifest KHÔNG còn co window lại khi bàn phím hiện. Thiếu imePadding
                // thì các ô ở nửa dưới form bị bàn phím che và không cuộn tới được.
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (state.vehicles.size > 1) {
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.vehicles.forEach { vehicle ->
                        FilterChip(
                            selected = vehicle.id == state.vehicleId,
                            onClick = { viewModel.setVehicle(vehicle.id) },
                            label = { Text(vehicle.name) }
                        )
                    }
                }
            }

            // Lúc mới cài app chưa có xe nào -> không có xe thì nút Lưu luôn mờ.
            // Nói rõ lý do, nếu không người dùng chỉ thấy một cái nút bấm không được.
            if (!state.loading && state.vehicleId == null) {
                Text(
                    text = "Chưa có xe nào. Thêm xe ở tab \"Xe\" trước khi ghi đổ xăng.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            DateField(millis = state.date, onDateChange = viewModel::setDate)

            NumberField(
                value = state.odometer,
                onValueChange = viewModel::setOdometer,
                label = "Số km trên đồng hồ (odo)",
                suffix = "km"
            )
            // Màu phụ chứ không phải màu lỗi: đây là lời nhắc, nút Lưu vẫn bấm được.
            // Dùng màu error cho một thứ không chặn gì sẽ dạy người dùng bỏ qua màu error.
            state.odometerError?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            NumberField(
                value = state.liters,
                onValueChange = viewModel::setLiters,
                label = "Số lít",
                allowDecimal = true,
                suffix = "L"
            )
            NumberField(
                value = state.pricePerLiter,
                onValueChange = viewModel::setPricePerLiter,
                label = "Đơn giá",
                suffix = "₫/L"
            )
            NumberField(
                value = state.totalCost,
                onValueChange = viewModel::setTotalCost,
                label = "Thành tiền",
                suffix = "₫"
            )
            Text(
                text = "Nhập 2 trong 3 ô trên, ô còn lại tự tính.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            SwitchRow(
                title = "Đổ đầy bình",
                subtitle = "Mức tiêu hao chỉ tính được giữa hai lần đổ đầy",
                checked = state.isFullTank,
                onCheckedChange = viewModel::setFullTank
            )
            SwitchRow(
                title = "Lần trước quên ghi",
                subtitle = "Bỏ qua đoạn này khi tính tiêu hao, tránh ra số sai",
                checked = state.missedPrevious,
                onCheckedChange = viewModel::setMissedPrevious
            )

            Spacer(Modifier.height(8.dp))

            LabeledTextField(
                value = state.station,
                onValueChange = viewModel::setStation,
                label = "Trạm xăng (tuỳ chọn)"
            )
            LabeledTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = "Ghi chú (tuỳ chọn)"
            )

            Spacer(Modifier.height(16.dp))

            state.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Button(
                onClick = viewModel::save,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Lưu")
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
