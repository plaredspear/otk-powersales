package com.otoki.powersales.claim.entity

/**
 * 클레임 상태 Enum
 * 클레임의 처리 상태를 나타낸다.
 */
enum class ClaimStatus(val label: String) {
    SUBMITTED("접수"),
    IN_PROGRESS("처리중"),
    RESOLVED("처리완료"),
    REJECTED("반려")
}
