package com.otoki.powersales.domain.activity.claim.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * SF `IF_SendClaimToPWS` 클레임 마스터 조회 테스트 요청 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 클레임 마스터 조회 인터페이스. 요청 body 는 기준 일자([modDt]) 하나뿐이며,
 * SF 가 해당 일자 기준으로 변경된 클레임 마스터 목록을 응답한다.
 *
 * @property modDt 조회 기준 일자 (YYYYMMDD, 예: `20260410`). SF Request Body 의 `MOD_DT` 로 전송.
 */
data class AdminClaimMasterSyncTestRequest(
    @field:NotBlank(message = "조회 기준 일자(MOD_DT)는 필수입니다")
    @field:Pattern(regexp = "\\d{8}", message = "조회 기준 일자는 YYYYMMDD 8자리 숫자여야 합니다")
    val modDt: String,
)
