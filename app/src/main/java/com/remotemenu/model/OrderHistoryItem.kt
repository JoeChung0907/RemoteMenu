package com.remotemenu.model

/**
 * OrderHistoryItem
 * 확정된 주문의 영수증 텍스트와 시간 정보를 담는 데이터 클래스.
 * @property id 기록 고유 식별자
 * @property timestamp 주문 확정 시간 (밀리초 단위)
 * @property printedText 출력된 영수증의 전문 텍스트
 */
data class OrderHistoryItem(
    val id: Int,
    val timestamp: Long,
    val printedText: String
)