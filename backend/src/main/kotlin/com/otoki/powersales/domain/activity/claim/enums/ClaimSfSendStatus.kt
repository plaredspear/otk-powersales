package com.otoki.powersales.domain.activity.claim.enums

/**
 * 제품클레임 SF outbound 전송상태 Enum.
 *
 * 클레임 등록(web admin / mobile) 의 dual-write(DB INSERT → SF Apex `/ClaimRegist` 직접 호출) 전송
 * lifecycle 추적 전용 — **SF 매핑 없음(backend 내부 상태)**. 물류클레임 [com.otoki.powersales.domain.activity.suggestion.entity.SuggestionSfSendStatus]
 * 패턴과 정합한다.
 *
 * 기존 [ClaimStatus] (SF `DKRetail__Status__c`) 와는 **완전히 다른 차원**이다:
 *   - [ClaimStatus] : SF → 외부(코스모스) 전송상태. 마이그레이션 시 SF 값 그대로 적재(표시 전용).
 *   - [ClaimSfSendStatus] : **신규 시스템 → SF** 전송상태. 신규 등록 건에서만 세팅.
 *
 * 이 분리로 SF origin 마이그레이션 데이터(= sf_send_status NULL)는 재전송 대상에서 자연히 제외된다.
 */
enum class ClaimSfSendStatus(val displayName: String) {
    /** 전송대기 — DB INSERT 완료, SF 전송 시도 전/중. */
    PENDING("전송대기"),

    /** 전송완료 — SF push 성공(RESULT_CODE=200). */
    SENT("전송완료"),

    /** 전송실패 — SF push 실패(오류/예외). 재전송 대상. */
    SEND_FAILED("전송실패"),
}
