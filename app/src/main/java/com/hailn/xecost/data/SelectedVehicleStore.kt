package com.hailn.xecost.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Xe đang được chọn, dùng chung cho mọi màn hình.
 *
 * Là @Singleton nên đổi xe ở Dashboard thì tab Nhiên liệu và Chi phí cũng đổi theo,
 * không cần truyền tham số qua navigation.
 */
@Singleton
class SelectedVehicleStore @Inject constructor() {

    private val _selectedId = MutableStateFlow<Long?>(null)
    val selectedId: StateFlow<Long?> = _selectedId.asStateFlow()

    fun select(id: Long?) {
        _selectedId.value = id
    }

    /**
     * Gọi khi danh sách xe nạp xong mà chưa có xe nào được chọn.
     *
     * HIỆN CHƯA CÓ CHỖ NÀO GỌI: DashboardViewModel và FuelListViewModel tự lùi về xe mặc
     * định / xe đầu danh sách khi `selectedId` còn null, nên không cần ghi lại lựa chọn.
     * Hoặc là nối hàm này vào ViewModel để lựa chọn được ghi nhớ, hoặc là xoá nó đi.
     */
    fun selectIfEmpty(id: Long) {
        if (_selectedId.value == null) _selectedId.value = id
    }
}
