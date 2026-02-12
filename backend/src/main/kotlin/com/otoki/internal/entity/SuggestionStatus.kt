package com.otoki.internal.entity

/**
 * 제안 상태 Enum
 * 제안의 처리 상태를 나타낸다.
 */
enum class SuggestionStatus {
    SUBMITTED,  // 등록됨
    IN_REVIEW,  // 검토 중
    ACCEPTED,   // 채택됨
    REJECTED    // 반려
}
