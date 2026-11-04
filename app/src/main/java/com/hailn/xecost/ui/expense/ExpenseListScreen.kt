package com.hailn.xecost.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import com.hailn.xecost.core.formatDate
import com.hailn.xecost.core.toVnd
import com.hailn.xecost.data.local.entity.Expense
import com.hailn.xecost.data.local.entity.ExpenseCategory
import com.hailn.xecost.ui.components.EmptyState

@Composable
fun ExpenseListScreen(
    onEdit: (Long) -> Unit,
    viewModel: ExpenseListViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Khoản chi đang chờ xác nhận xoá. Xoá là thao tác không hoàn tác được mà nút rác lại
    // nằm ngay trên thẻ bấm được, nên phải hỏi lại trước khi xoá.
    var pendingDelete by remember { mutableStateOf<Expense?>(null) }

    // Chưa có dữ liệu thì không vẽ gì, tránh chớp nhầm thông báo "chưa có xe".
    if (state.loading) return

    if (!state.hasVehicle) {
        EmptyState("Chưa có xe nào. Thêm xe ở tab \"Xe\" trước.")
        return
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = state.filter == null,
                onClick = { viewModel.setFilter(null) },
                label = { Text("Tất cả") }
            )
            ExpenseCategory.entries.forEach { category ->
                FilterChip(
                    selected = state.filter == category,
                    onClick = { viewModel.setFilter(category) },
                    label = { Text(category.label) }
                )
            }
        }

        Text(
            text = "Tổng: ${state.total.toVnd()}",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
        )

        if (state.expenses.isEmpty()) {
            EmptyState("Chưa có khoản chi nào.\nBấm nút + để thêm.")
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.expenses, key = { it.id }) { expense ->
                    ExpenseCard(
                        expense = expense,
                        onClick = { onEdit(expense.id) },
                        onDelete = { pendingDelete = expense }
                    )
                }
            }
        }
    }

    val target = pendingDelete
    if (target != null) {
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Xoá khoản chi?") },
            text = { Text("${target.title} · ${target.amount.toVnd()}") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(target)
                    pendingDelete = null
                }) { Text("Xoá") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Huỷ") }
            }
        )
    }
}

@Composable
private fun ExpenseCard(expense: Expense, onClick: () -> Unit, onDelete: () -> Unit) {
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
                Text(expense.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${expense.date.formatDate()} · ${expense.category.label}" +
                        (expense.place?.let { " · $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(expense.amount.toVnd(), style = MaterialTheme.typography.titleMedium)
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xoá")
                }
            }
        }
    }
}
