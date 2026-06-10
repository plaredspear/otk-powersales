package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.employee.entity.Employee

/**
 * 조장의 본인 팀원 목록 응답 DTO (Spec #554 P1-B §3.5.1).
 *
 * 레거시 empSearch parity: 퇴직자는 제외하고 휴직 직원은 포함하며,
 * 클라이언트가 status 필드로 비활성(휴직 등) 처리한다.
 */
data class LeaderTeamMemberListResponse(
    val id: Long,
    val employeeCode: String?,
    val name: String,
    val status: String?,
    val costCenterCode: String?
) {
    companion object {
        fun from(entity: Employee): LeaderTeamMemberListResponse =
            LeaderTeamMemberListResponse(
                id = entity.id,
                employeeCode = entity.employeeCode,
                name = entity.name,
                status = entity.status,
                costCenterCode = entity.costCenterCode
            )
    }
}
