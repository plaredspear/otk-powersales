package com.otoki.internal.entity

/**
 * 공지사항 분류 Enum
 */
enum class NoticeCategory(
    val displayName: String
) {
    /**
     * 회사공지
     */
    COMPANY("회사공지"),

    /**
     * 지점공지
     */
    BRANCH("지점공지");

    companion object {
        /**
         * 문자열을 NoticeCategory enum으로 변환
         * @throws IllegalArgumentException 유효하지 않은 카테고리 문자열인 경우
         */
        fun fromString(category: String): NoticeCategory {
            return entries.find { it.name == category }
                ?: throw IllegalArgumentException("Invalid category: $category")
        }
    }
}
