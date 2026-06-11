package com.otoki.powersales.schedule.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

/**
 * 조장 진열 일정(마스터) 변경 요청 (레거시 `scheduleChange` 진열 거래처/유형 변경 동등).
 *
 * 진열 마스터의 거래처/근무유형/기간을 갱신한다 (담당 여사원은 변경 대상 아님 — 진열 변경 화면 정합).
 * 모바일은 마스터 상세(GET)로 기존값을 선조회해 전체 필드를 채워 보낸다. (JSON 키 camelCase)
 */
data class LeaderDisplayScheduleUpdateRequest(
    @field:NotNull(message = "거래처 ID는 필수입니다")
    val accountId: Long?,

    @field:NotNull(message = "시작일은 필수입니다")
    val startDate: LocalDate?,

    val endDate: LocalDate? = null,

    @field:NotBlank(message = "근무유형(고정/격고/순회)은 필수입니다")
    val typeOfWork3: String?,

    @field:NotBlank(message = "근무유형(상온/냉동냉장)은 필수입니다")
    val typeOfWork4: String?,

    @field:NotBlank(message = "근무유형(상시/임시)은 필수입니다")
    val typeOfWork5: String?,
)
