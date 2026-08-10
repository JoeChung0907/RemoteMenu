package com.remotemenu.model

/**
 * OrderItem
 * 사용자가 선택하여 장바구니에 담은 개별 주문 항목 정보를 담는 데이터 클래스.
 * @property id 항목 고유 식별자
 * @property tableNumber 주문이 발생한 테이블 번호
 * @property menuItem 선택한 메뉴 정보
 * @property quantity 주문 수량
 * @property selectedOptions 사용자가 선택한 추가 옵션 리스트
 * @property isCustom 커스텀 옵션 포함 여부
 */
data class OrderItem(
    val id: Int,
    val tableNumber: Int,
    val menuItem: MenuItem,
    val quantity: Int,
    val selectedOptions: List<CustomOption>,
    val isCustom: Boolean
)