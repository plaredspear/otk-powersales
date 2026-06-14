package com.otoki.powersales.domain.activity.suggestion.dto.response

/**
 * SF 물류 클레임 등록(ProposalRegist) 전송 테스트 응답 (개발자 도구 — 외부 API 테스트).
 *
 * SF 전송 API 정보 미확보 단계 — 실제 SF POST 없이 **모바일 입력으로 구성한 전송 payload(apiMap)
 * 미리보기만** 반환한다. [success]/[resultCode]/[resultMsg]/[rawResponse] 는 SF 호출이 추가되면
 * 채워질 자리로, 현재는 항상 미전송(success=false, resultMsg=[note]) 상태이다.
 */
data class AdminLogisticsClaimRegistTestResponse(
    val success: Boolean,
    val resultCode: String?,
    val resultMsg: String?,
    val rawResponse: String?,
    /** SF `IF_REST_MOBILE_ProposalRegist` 로 전송될 apiMap JSON (Input 클래스 key 셋 정합). */
    val requestPayload: String,
    /** 현재 단계 안내 문구 (SF 전송 미구현 — 미리보기 전용). */
    val note: String,
)
