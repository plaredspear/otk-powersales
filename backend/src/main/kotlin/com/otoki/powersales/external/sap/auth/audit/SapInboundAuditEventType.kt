package com.otoki.powersales.external.sap.auth.audit

object SapInboundAuditEventType {
    const val TOKEN_ISSUED = "TOKEN_ISSUED"
    const val TOKEN_REJECTED = "TOKEN_REJECTED"
    const val REQUEST_ACCEPTED = "REQUEST_ACCEPTED"
    const val REQUEST_REJECTED_AUTH = "REQUEST_REJECTED_AUTH"
    const val REQUEST_REJECTED_SCOPE = "REQUEST_REJECTED_SCOPE"
    const val REQUEST_REJECTED_IP = "REQUEST_REJECTED_IP"
    const val REQUEST_REJECTED_SANITY = "REQUEST_REJECTED_SANITY"

    /** endpoint 가 비활성 상태라 적재 처리를 생략하고 정상 응답만 반환한 경우 (SAP 인바운드 토글). */
    const val REQUEST_SKIPPED = "REQUEST_SKIPPED"

    /** Spec #553: `attend_info` → `team_member_schedule` 변환 처리 결과 카운트 기록 */
    const val SCHEDULE_CONVERSION = "SCHEDULE_CONVERSION"

    /** Spec #553: 변환 처리 트랜잭션이 예외로 실패한 경우 (인바운드 INSERT 는 유지) */
    const val SCHEDULE_CONVERSION_FAILED = "SCHEDULE_CONVERSION_FAILED"

    /**
     * (deprecated — 더 이상 발행하지 않음) Spec #579 에서 도입했던 origin=MANUAL 보호 차단 이벤트.
     * 레거시 IF_REST_SAP_EmployeeMaster 정합으로 보호 게이트를 제거하여 신규 기록은 발생하지 않으나,
     * 과거 audit row 의 event_type 조회 호환을 위해 상수는 보존한다.
     */
    const val MANUAL_ORIGIN_PROTECTED = "MANUAL_ORIGIN_PROTECTED"
}
