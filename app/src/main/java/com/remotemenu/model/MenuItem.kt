package com.remotemenu.model

data class MenuItem(
    val id: Int,
    val name: String,
    val price: Int,
    val allergy: String,
    val customOptions: List<CustomOption>
)
