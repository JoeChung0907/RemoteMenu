package com.remotemenu.model

/**
 * CustomOption
 * 메뉴에 부가적으로 선택할 수 있는 옵션(예: 굽기 정도, 토핑 등) 정보를 담는 데이터 클래스.
 * @property id 옵션 고유 식별자
 * @property label 옵션 표시 이름
 */
data class CustomOption(
    val id: Int,
    val label: String
)