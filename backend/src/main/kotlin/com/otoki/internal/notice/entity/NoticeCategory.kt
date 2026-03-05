package com.otoki.internal.notice.entity

/**
 * 공지사항 분류 Enum
 */
enum class NoticeCategory(
    val displayName: String,
    val dbValue: String
) {
    COMPANY("전체공지", "ALL"),
    BRANCH("지점공지", "BRANCH");

    companion object {
        fun fromString(category: String): NoticeCategory {
            return entries.find { it.name == category }
                ?: throw IllegalArgumentException("Invalid category: $category")
        }
    }
}
