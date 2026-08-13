package com.remotemenu.model

/**
 * MenuItem
 * 매장에서 판매하는 메뉴의 정보를 담는 데이터 클래스.
 */
data class MenuItem(
    val id: Int,
    val category: String,
    val name: String,
    val price: Double,
    val allergy: String,
    val customOptions: List<CustomOption>
)
