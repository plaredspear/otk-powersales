package com.otoki.powersales.external.sap.auth.audit

object SapInboundAuditEventType {
    const val TOKEN_ISSUED = "TOKEN_ISSUED"
    const val TOKEN_REJECTED = "TOKEN_REJECTED"
    const val REQUEST_ACCEPTED = "REQUEST_ACCEPTED"
    const val REQUEST_REJECTED_AUTH = "REQUEST_REJECTED_AUTH"
    const val REQUEST_REJECTED_SCOPE = "REQUEST_REJECTED_SCOPE"
    const val REQUEST_REJECTED_IP = "REQUEST_REJECTED_IP"
    const val REQUEST_REJECTED_SANITY = "REQUEST_REJECTED_SANITY"

    /** Spec #553: `attend_info` → `team_member_schedule` 변환 처리 결과 카운트 기록 */
    const val SCHEDULE_CONVERSION = "SCHEDULE_CONVERSION"

    /** Spec #553: 변환 처리 트랜잭션이 예외로 실패한 경우 (인바운드 INSERT 는 유지) */
    const val SCHEDULE_CONVERSION_FAILED = "SCHEDULE_CONVERSION_FAILED"

    /** Spec #579: SAP 인바운드 직원 마스터 upsert 가 origin=MANUAL 직원을 보호 차단한 경우 */
    const val MANUAL_ORIGIN_PROTECTED = "MANUAL_ORIGIN_PROTECTED"
}
