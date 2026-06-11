package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import java.time.LocalDate

/**
 * 조장 진열 일정(마스터) 상세/변경 응답.
 *
 * 모바일 편집 시트가 기존값을 선조회(GET)할 때, 그리고 생성/변경 결과 반환에 공용으로 사용.
 */
data class LeaderDisplayScheduleResponse(
    val displayWorkScheduleId: Long,
    val employeeId: Long?,
    val employeeName: String?,
    val accountId: Long?,
    val accountName: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val typeOfWork3: String?,
    val typeOfWork4: String?,
    val typeOfWork5: String?,
) {
    companion object {
        fun from(entity: DisplayWorkSchedule): LeaderDisplayScheduleResponse =
            LeaderDisplayScheduleResponse(
                displayWorkScheduleId = entity.id,
                employeeId = entity.employee?.id,
                employeeName = entity.employee?.name,
                accountId = entity.account?.id,
                accountName = entity.account?.name,
                startDate = entity.startDate,
                endDate = entity.endDate,
                typeOfWork3 = entity.typeOfWork3?.displayName,
                typeOfWork4 = entity.typeOfWork4?.displayName,
                typeOfWork5 = entity.typeOfWork5?.displayName,
            )
    }
}
