package com.otoki.powersales.external.sap.outbox

/**
 * 도메인 상태 갱신 callback 인터페이스 (Spec #592).
 *
 * [SapOutboxBatchService] 가 SAP 송신 완료/실패 시점에 도메인 상태(`order_request.order_request_status` 등) 를
 * 전이시키기 위해 호출한다. 도메인별 구현체가 자기 도메인의 상태머신을 책임진다.
 *
 * `domainType` 값으로 dispatch 됨 (예: `'ORDER_REQUEST_REGISTER'`).
 */
interface SapOutboxStatusHandler {

    /** 본 핸들러가 처리할 [SapOutbox.domainType] 값. */
    fun supports(): String

    /**
     * SAP 송신 결과를 도메인 상태에 반영한다.
     *
     * @param outbox 송신 outbox row
     * @param success SAP 응답 본문 검증 통과 + `resultCode='S'` 여부
     * @param resultMessage SAP 응답의 `resutlMsg` (오타 레거시 그대로) 또는 실패 사유
     * @param rejected SAP 가 명시적으로 거부(`resultCode` 존재 && ≠ `"S"`)한 확정 실패 여부.
     *   `true` 이면 [resultMessage] 는 SAP 업무 사유 원문이며 재시도 없이 최종 실패로 확정된 상태다.
     *   일시적 장애(EMPTY/HTML/HTTP/NETWORK)로 인한 실패는 `false`.
     */
    fun handle(outbox: SapOutbox, success: Boolean, resultMessage: String?, rejected: Boolean)
}
