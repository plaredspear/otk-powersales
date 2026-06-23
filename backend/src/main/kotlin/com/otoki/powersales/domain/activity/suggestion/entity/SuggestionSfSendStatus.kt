package com.otoki.powersales.domain.activity.suggestion.entity

/**
 * 제안/물류클레임 SF outbound 전송상태 Enum.
 *
 * `SuggestionService.create()` 의 dual-write(DB INSERT → SF Apex `IF_REST_MOBILE_ProposalRegist`
 * 직접 호출) 전송 lifecycle 추적 전용 — SF 매핑 없음(backend 내부 상태).
 * 클레임 등록(`ClaimStatus` 의 SF_PENDING/SENT/SEND_FAILED) 패턴과 정합.
 *
 * 리뷰 lifecycle 인 [SuggestionStatus] 와는 별개 차원이다(과적재 금지).
 */
enum class SuggestionSfSendStatus {
    /** 전송대기 — DB INSERT 완료, SF 전송 시도 전/중. */
    PENDING,

    /** 전송완료 — SF push 성공(RESULT_CODE=200). */
    SENT,

    /** 전송실패 — SF push 실패(오류/예외). 재전송 대상. */
    SEND_FAILED,
}
