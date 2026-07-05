package com.otoki.powersales.admin.dto.response

/**
 * FCM push 발송 테스트 응답 (개발자 도구 > 외부 API 테스트).
 *
 * @property employeeCode 대상 사번
 * @property employeeName 대상 사원명 (조회된 경우)
 * @property tokenRegistered 해당 사번에 FCM 토큰이 등록되어 있었는지 여부
 * @property maskedToken 발송에 사용된 FCM 토큰의 마스킹 표기 (앞 8자 + …, 미등록 시 null)
 * @property successCount FcmSender 집계 성공 건수
 * @property failureCount FcmSender 집계 실패 건수
 * @property message 발송 시도 결과에 대한 사람이 읽는 요약 문구
 */
data class AdminPushTestResponse(
    val employeeCode: String,
    val employeeName: String?,
    val tokenRegistered: Boolean,
    val maskedToken: String?,
    val successCount: Int,
    val failureCount: Int,
    val message: String,
)
