package com.hailn.xecost.ui.vehicle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import com.hailn.xecost.data.local.entity.FuelType
import com.hailn.xecost.data.local.entity.VehicleType
import com.hailn.xecost.ui.components.NumberField
import com.hailn.xecost.ui.components.LabeledTextField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddVehicleScreen(
    onDone: () -> Unit,
    viewModel: AddVehicleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Sửa xe" else "Thêm xe") },
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
            LabeledTextField(
                value = state.name,
                onValueChange = viewModel::setName,
                label = "Tên gợi nhớ (vd: Wave đi làm)"
            )

            Spacer(Modifier.height(8.dp))
            Text("Loại xe", style = MaterialTheme.typography.labelLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                VehicleType.entries.forEach { type ->
                    FilterChip(
                        selected = type == state.type,
                        onClick = { viewModel.setType(type) },
                        label = { Text(type.label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Nhiên liệu", style = MaterialTheme.typography.labelLarge)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FuelType.entries.forEach { fuel ->
                    FilterChip(
                        selected = fuel == state.fuelType,
                        onClick = { viewModel.setFuelType(fuel) },
                        label = { Text(fuel.label) }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            LabeledTextField(
                value = state.plateNumber,
                onValueChange = viewModel::setPlate,
                label = "Biển số (tuỳ chọn)"
            )
            LabeledTextField(
                value = state.brand,
                onValueChange = viewModel::setBrand,
                label = "Hãng (tuỳ chọn)"
            )
            LabeledTextField(
                value = state.model,
                onValueChange = viewModel::setModel,
                label = "Dòng xe (tuỳ chọn)"
            )
            NumberField(
                value = state.year,
                onValueChange = viewModel::setYear,
                label = "Năm sản xuất (tuỳ chọn)"
            )
            // Ô này ghi vào `initialOdometer` — mốc để tính tổng km đã đi, KHÔNG phải odo
            // mới nhất (odo mới nhất do các lần đổ xăng / chi phí tự đẩy lên).
            // Nhãn phải nói đúng, nếu không lúc sửa xe người dùng sẽ gõ odo hôm nay vào đây
            // và tổng km đã đi tụt về 0.
            NumberField(
                value = state.initialOdometer,
                onValueChange = viewModel::setInitialOdometer,
                label = "Odo lúc bắt đầu dùng app",
                suffix = "km"
            )
            NumberField(
                value = state.tankCapacity,
                onValueChange = viewModel::setTankCapacity,
                label = "Dung tích bình (tuỳ chọn)",
                allowDecimal = true,
                suffix = "L"
            )
            NumberField(
                value = state.purchasePrice,
                onValueChange = viewModel::setPurchasePrice,
                label = "Giá mua (tuỳ chọn)",
                suffix = "₫"
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Đặt làm xe mặc định", Modifier.weight(1f))
                Switch(checked = state.isDefault, onCheckedChange = viewModel::setDefault)
            }

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
