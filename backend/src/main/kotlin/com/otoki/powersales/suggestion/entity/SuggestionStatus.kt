package com.otoki.powersales.suggestion.entity

/**
 * 제안 상태 Enum.
 *
 * 초기 활성화 단계 — 등록 시점 SUBMITTED 기본값. 상태 전이 흐름 (REVIEWED / APPROVED / REJECTED) 은 별 스펙 후속.
 */
enum class SuggestionStatus {
    SUBMITTED,
    IN_REVIEW,
    ACCEPTED,
    REJECTED
}
