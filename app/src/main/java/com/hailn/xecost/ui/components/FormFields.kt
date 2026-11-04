package com.hailn.xecost.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.hailn.xecost.core.formatDate

/**
 * Ô nhập chữ có nhãn. Đặt tên là [LabeledTextField] chứ không phải `TextField`
 * để không trùng tên với `androidx.compose.material3.TextField` — trùng tên thì
 * chỉ cần ai đó thêm `import androidx.compose.material3.*` là lỗi nhập nhằng.
 */
@Composable
fun LabeledTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

/**
 * Ô nhập số. Lọc ký tự ngay khi gõ để state luôn parse được,
 * thay vì để người dùng gõ bậy rồi báo lỗi lúc bấm Lưu.
 */
@Composable
fun NumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    allowDecimal: Boolean = false,
    suffix: String? = null
) {
    // Viết tách ra thay vì `suffix?.let { { Text(it) } }`: lambda lồng lambda mà cả hai cùng
    // dùng `it` thì người đọc phải dừng lại nghĩ, và kiểu của slot @Composable chỉ suy ra
    // được nhờ tham số đích. Khai báo kiểu tường minh thì compiler không phải đoán.
    val suffixSlot: (@Composable () -> Unit)? =
        if (suffix != null) {
            { Text(suffix) }
        } else {
            null
        }

    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val filtered = raw.filter { it.isDigit() || (allowDecimal && (it == '.' || it == ',')) }
                .replace(',', '.')
            // chỉ cho phép một dấu chấm
            val normalized = if (allowDecimal) {
                val first = filtered.indexOf('.')
                if (first == -1) filtered
                else filtered.substring(0, first + 1) + filtered.substring(first + 1).replace(".", "")
            } else filtered
            onValueChange(normalized)
        },
        label = { Text(label) },
        suffix = suffixSlot,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (allowDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateField(
    millis: Long,
    onDateChange: (Long) -> Unit,
    label: String = "Ngày",
    modifier: Modifier = Modifier
) {
    var showPicker by remember { mutableStateOf(false) }

    AssistChip(
        onClick = { showPicker = true },
        label = { Text("$label: ${millis.formatDate()}") },
        modifier = modifier.padding(vertical = 4.dp)
    )

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let(onDateChange)
                    showPicker = false
                }) { Text("Chọn") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Huỷ") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}
