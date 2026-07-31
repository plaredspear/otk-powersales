package com.otoki.powersales.domain.activity.suggestion.event

/**
 * 제안/물류클레임 등록 완료 이벤트 — SF `/ProposalRegist` 송신 트리거용.
 *
 * admin 등록([com.otoki.powersales.domain.activity.suggestion.service.AdminSuggestionService.create])이
 * 발행하고, [com.otoki.powersales.domain.activity.suggestion.service.SuggestionSfPushDispatcher] 가
 * 커밋 후(AFTER_COMMIT) 비동기로 수신해 SF 릴레이를 수행한다.
 *
 * mobile 등록([com.otoki.powersales.domain.activity.suggestion.service.SuggestionService.create]) 은
 * 레거시 정합상 동기 전송을 유지하므로 이 이벤트를 쓰지 않는다 (클레임의 [com.otoki.powersales.domain.activity.claim.event.ClaimRegisteredEvent] 참고).
 */
data class SuggestionRegisteredEvent(
    val suggestionId: Long,
)
