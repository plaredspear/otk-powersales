package com.otoki.powersales.domain.activity.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * AccountViewAll 대리출근 등록 요청 DTO.
 *
 * 조장 대리출근([LeaderProxyAttendanceRequest])과 달리 [branchCode](선택 지점)를 필수로 받아,
 * 대상 여사원이 그 지점 소속인지 서버에서 재검증한다. 진열 거래처는 [displayWorkScheduleId]
 * (진열 마스터 ID), 행사·기배정 거래처는 [scheduleId](team_member_schedule ID) 중 하나 전달.
 * [workingDate] 는 소급 등록할 근무일 — 생략 시 오늘. 미래일은 서버에서 거부한다.
 * (JSON 키는 프로젝트 공통 camelCase — `workingDate` = "YYYY-MM-DD")
 */
data class ProxyAttendanceRegisterRequest(
    @field:NotBlank(message = "지점을 선택해야 합니다")
    val branchCode: String?,

    @field:NotNull(message = "대상 직원 ID는 필수입니다")
    val targetEmployeeId: Long?,

    val scheduleId: Long? = null,

    val displayWorkScheduleId: Long? = null,

    /** 등록 대상 근무일 (과거일 소급 등록 지원). null = 오늘. 미래일은 거부. */
    val workingDate: LocalDate? = null,
)
