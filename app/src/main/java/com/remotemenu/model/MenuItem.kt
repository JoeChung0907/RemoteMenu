package com.remotemenu.model

/**
 * MenuItem
 * 매장에서 판매하는 메뉴의 정보를 담는 데이터 클래스.
 * @property id 메뉴 고유 식별자
 * @property name 메뉴 이름
 * @property price 메뉴 가격 (소수점 지원)
 * @property allergy 포함된 알러지 정보 문자열
 * @property customOptions 선택 가능한 추가 옵션 리스트
 */
data class MenuItem(
    val id: Int,
    val name: String,
    val price: Double,
    val allergy: String,
    val customOptions: List<CustomOption>
)
