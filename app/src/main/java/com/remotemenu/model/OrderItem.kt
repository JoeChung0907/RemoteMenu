package com.remotemenu.model

data class OrderItem(
    val id: Int,
    val tableNumber: Int,
    val menuItem: MenuItem,
    val quantity: Int,
    val selectedOptions: List<CustomOption>,
    val isCustom: Boolean
)