package com.otoki.internal.entity

/**
 * 클레임 상태 Enum
 * 클레임의 처리 상태를 나타낸다.
 */
enum class ClaimStatus {
    SUBMITTED,   // 등록됨
    IN_PROGRESS, // 처리 중
    RESOLVED,    // 처리 완료
    REJECTED     // 반려
}
