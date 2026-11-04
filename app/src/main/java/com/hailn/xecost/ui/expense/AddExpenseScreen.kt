package com.hailn.xecost.ui.expense

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hailn.xecost.data.local.entity.ExpenseCategory
import com.hailn.xecost.ui.components.DateField
import com.hailn.xecost.ui.components.NumberField
import com.hailn.xecost.ui.components.LabeledTextField

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddExpenseScreen(
    onDone: () -> Unit,
    viewModel: AddExpenseViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Sửa khoản chi" else "Thêm khoản chi") },
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

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseCategory.entries.forEach { category ->
                    FilterChip(
                        selected = category == state.category,
                        onClick = { viewModel.setCategory(category) },
                        label = { Text(category.label) }
                    )
                }
            }

            DateField(millis = state.date, onDateChange = viewModel::setDate)

            LabeledTextField(
                value = state.title,
                onValueChange = viewModel::setTitle,
                label = "Nội dung"
            )
            NumberField(
                value = state.amount,
                onValueChange = viewModel::setAmount,
                label = "Số tiền",
                suffix = "₫"
            )
            NumberField(
                value = state.odometer,
                onValueChange = viewModel::setOdometer,
                label = "Odo (tuỳ chọn)",
                suffix = "km"
            )
            LabeledTextField(
                value = state.place,
                onValueChange = viewModel::setPlace,
                label = "Nơi thực hiện (tuỳ chọn)"
            )
            LabeledTextField(
                value = state.note,
                onValueChange = viewModel::setNote,
                label = "Ghi chú (tuỳ chọn)"
            )

            Spacer(Modifier.height(16.dp))

            // Không có xe thì nút Lưu luôn mờ — phải nói rõ lý do, đừng để người dùng
            // gõ xong cả form rồi bấm mãi không được mà không hiểu vì sao.
            if (state.loaded && state.vehicleId == null) {
                Text(
                    text = "Chưa có xe nào. Thêm xe ở tab \"Xe\" trước khi ghi chi phí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
            }

            state.error?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(8.dp))
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
