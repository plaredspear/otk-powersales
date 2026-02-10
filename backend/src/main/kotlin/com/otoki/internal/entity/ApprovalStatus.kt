package com.otoki.internal.entity

/**
 * 주문 승인상태 Enum
 */
enum class ApprovalStatus {
    APPROVED,      // 승인완료
    PENDING,       // 승인대기
    SEND_FAILED,   // 전송실패
    RESEND         // 재전송
}
