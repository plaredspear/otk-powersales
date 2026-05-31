package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotNull

/**
 * 조장 진열 일정 수정 요청 DTO (P7 — 레거시 `changeProc` InterfaceType=M 동등).
 *
 * 레거시 M 경로는 실질적으로 **거래처(account)만** 변경 가능했다(근무유형3은 hidden 고정,
 * 근무유형1은 읽기전용). 따라서 본 수정도 거래처 변경만 지원한다.
 * JSON 키는 기본 Jackson(camelCase) — `accountId` (create 요청과 동일 컨벤션).
 */
data class LeaderScheduleUpdateRequest(
    @field:NotNull(message = "거래처를 선택해야 합니다")
    val accountId: Int?
)
