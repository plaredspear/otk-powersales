package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 여사원 일정 조회 요청 body.
 *
 * 종전 `GET /api/v1/admin/team-schedule` 의 쿼리 파라미터 (`from`/`to`/`employeeIds`/`accountIds`/
 * `promotionTeams`/`branchCode`) 를 그대로 옮긴 POST body. 거래처 전체선택(549건) 시 `accountIds` 가
 * 수 KB 쿼리스트링이 되어 GET URL 길이 한도를 초과하며 차단되던 문제를 해소하기 위해 조회를 POST 로 전환.
 *
 * `from`/`to` 는 ISO-8601 (예: `2026-06-01`) 로 파싱된다 (Jackson JavaTime 모듈).
 *
 * `employeeIds` / `accountIds` 는 SF 레거시 XOR 분기 입력 — 여사원 우선, 없으면 거래처 IN 절 사용.
 * 둘 다 비면 일별 요약만 반환 ([com.otoki.powersales.schedule.service.AdminTeamScheduleService.getSchedulesWithSummary]).
 */
data class TeamScheduleSearchRequest(
    @field:NotNull(message = "조회 시작일은 필수입니다")
    val from: LocalDate?,

    @field:NotNull(message = "조회 종료일은 필수입니다")
    val to: LocalDate?,

    val employeeIds: List<Long>? = null,
    val accountIds: List<Long>? = null,
    val promotionTeams: List<String>? = null,
    val branchCode: String? = null,
)
