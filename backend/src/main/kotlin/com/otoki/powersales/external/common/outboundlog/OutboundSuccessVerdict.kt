package com.otoki.powersales.external.common.outboundlog

/**
 * 응답 본문 기반 성공/실패 판정 결과 — [ExternalApiLogInterceptor] 의 `responseSuccessResolver` 반환 타입.
 *
 * 일부 대상 시스템(예: SF Apex REST)은 도메인 실패도 HTTP 200 으로 응답하고, 실제 성공 여부를 응답 본문의
 * 코드 필드(SF `RESULT_CODE`)로 전달한다. 이 경우 HTTP status 만으로는 `external_api_log.success` 를 옳게
 * 판정할 수 없어, 대상별 resolver 가 본문을 해석해 최종 판정을 override 한다.
 *
 * resolver 가 판정할 수 없는 응답(비-등록 응답, 파싱 실패 등)에는 이 타입 대신 `null` 을 반환해 기존 HTTP
 * status 판정을 그대로 유지한다.
 *
 * @param success 도메인 성공 여부.
 * @param errorMessage 실패 시 `error_detail` 에 남길 사유(성공이면 무시). SF 는 `RESULT_MSG`.
 */
data class OutboundSuccessVerdict(
    val success: Boolean,
    val errorMessage: String? = null,
)
