package com.otoki.powersales.domain.activity.claim.event

/**
 * 클레임 등록 SF 송신(`/ClaimRegist`) 트리거 이벤트.
 *
 * 발행: 등록 진입점([com.otoki.powersales.domain.activity.claim.service.MobileClaimService] ·
 *       [com.otoki.powersales.domain.activity.claim.service.AdminClaimCreateService]) 이
 *       claim + photo INSERT(sfSendStatus=PENDING) 직후 발행.
 * 수신: [com.otoki.powersales.domain.activity.claim.service.ClaimSfPushDispatcher]
 *       (`@TransactionalEventListener(AFTER_COMMIT) + @Async`).
 *
 * 의미: 등록 트랜잭션 커밋 후 별도 스레드 + 별도 트랜잭션에서 SF push 를 수행한다. 진입점(모바일/web)은
 * "등록됨(sfSendStatus=PENDING)" 만 동기로 응답받고, SF 전송 성공/실패(SENT/SEND_FAILED) 는 커밋 후 비동기로 전이된다.
 * SF 송신에 필요한 입력·이미지는 [com.otoki.powersales.domain.activity.claim.service.ClaimSfDispatchService]
 * 가 claimId 로 DB + S3 에서 복원하므로 본 이벤트는 claimId 만 운반한다.
 */
data class ClaimRegisteredEvent(
    val claimId: Long,
)
