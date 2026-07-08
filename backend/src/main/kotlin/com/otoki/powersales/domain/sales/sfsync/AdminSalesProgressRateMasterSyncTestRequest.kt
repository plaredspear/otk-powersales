package com.otoki.powersales.domain.sales.sfsync

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

/**
 * SF `IF_salesprogresssend` 거래처목표등록마스터 조회 테스트 요청 (개발자 도구 — 외부 API 테스트).
 *
 * SF → PWS 방향의 거래처 목표 마스터 조회 인터페이스. 기준 일자([modDt]) 로 SF 를 호출하면
 * SF 가 해당 일자 기준으로 변경된 거래처목표등록마스터 목록을 응답한다 ("알라딘 거래처목표 마스터 API" 문서 정합).
 *
 * @property modDt 조회 기준 일자 (YYYYMMDD, 예: `20260410`). SF Request Body 의 `MOD_DT` 로 전송.
 * @property save  `true` 면 SF 응답을 주기 sync 와 동일 경로(ExternalKey upsert)로 신규 DB 에 저장한다.
 *                 기본 `false` — 조회 전용 (DB 변경 없음).
 */
data class AdminSalesProgressRateMasterSyncTestRequest(
    @field:NotBlank(message = "조회 기준 일자(MOD_DT)는 필수입니다")
    @field:Pattern(regexp = "\\d{8}", message = "조회 기준 일자는 YYYYMMDD 8자리 숫자여야 합니다")
    val modDt: String,

    val save: Boolean = false,
)
