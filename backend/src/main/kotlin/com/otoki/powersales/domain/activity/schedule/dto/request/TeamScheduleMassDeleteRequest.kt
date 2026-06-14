package com.otoki.powersales.domain.activity.schedule.dto.request

import jakarta.validation.constraints.NotEmpty

/**
 * 여사원 일정 다건 삭제 요청 (Spec #691 P1-B).
 *
 * legacy `MassDeleteTmScheduleController.doMassDelete` (VF `@RemoteAction` 100건 제한) 의 신규 대응 endpoint.
 * 100건 상한은 service 측 `TeamScheduleMassDeleteRowLimitExceededException` 으로 강제 (#691 Q1 옵션 1).
 */
data class TeamScheduleMassDeleteRequest(
    @field:NotEmpty(message = "ids는 1건 이상이어야 합니다")
    val ids: List<Long>
)
