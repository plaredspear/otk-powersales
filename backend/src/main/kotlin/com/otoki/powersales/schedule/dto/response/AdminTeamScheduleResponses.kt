package com.otoki.powersales.schedule.dto.response

import com.otoki.powersales.common.dto.response.BranchResponse
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.schedule.entity.TeamMemberSchedule

data class TeamMemberDto(
    val employeeId: Long,
    val employeeCode: String?,
    val name: String
) {
    companion object {
        fun from(employee: Employee): TeamMemberDto = TeamMemberDto(
            employeeId = employee.id,
            employeeCode = employee.employeeCode,
            name = employee.name
        )
    }
}

data class TeamScheduleAccountDto(
    val accountId: Long,
    val externalKey: String,
    val name: String
) {
    companion object {
        fun from(account: Account): TeamScheduleAccountDto = TeamScheduleAccountDto(
            accountId = account.id,
            externalKey = account.externalKey ?: "",
            name = account.name ?: ""
        )
    }
}

data class TeamScheduleDto(
    val id: Long,
    val employeeCode: String,
    val employeeName: String,
    val workingDate: String,
    val workingType: String,
    val workingCategory1: String?,
    val workingCategory2: String?,
    val workingCategory3: String?,
    val accountId: Long?,
    val accountName: String?,
    val accountExternalKey: String?,
    // SF 목록(FullCalendarComponentController.fetchAllShcedule) title 정합 — 거래처 유형/지점명.
    // accountType: SF `AccountType__c = TEXT(AccountId__r.Type)` 대응 (account.accountType).
    // accountBranchName: SF `AccountId__r.BranchName__c` 대응.
    val accountType: String?,
    val accountBranchName: String?,
    val isClockIn: Boolean,
    val promotionId: Long?
) {
    companion object {
        fun from(schedule: TeamMemberSchedule): TeamScheduleDto {
            return TeamScheduleDto(
                id = schedule.id,
                employeeCode = schedule.employee?.employeeCode ?: "",
                employeeName = schedule.employee?.name ?: "",
                workingDate = schedule.workingDate?.toString() ?: "",
                workingType = schedule.workingType?.displayName ?: "",
                workingCategory1 = schedule.workingCategory1?.displayName,
                workingCategory2 = schedule.workingCategory2?.displayName,
                workingCategory3 = schedule.workingCategory3?.displayName,
                accountId = schedule.account?.id,
                accountName = schedule.account?.name,
                accountExternalKey = schedule.account?.externalKey,
                accountType = schedule.account?.accountType?.displayName,
                accountBranchName = schedule.account?.branchName,
                isClockIn = schedule.attendanceLog != null,
                promotionId = schedule.promotionEmployee?.promotionId
            )
        }
    }
}

data class DailySummaryDto(
    val date: String,
    val displayExpected: Int,
    val displayActual: Int,
    val promotionExpected: Int,
    val promotionActual: Int,
    val annualLeave: Int,
    val compensatoryLeave: Int
)

data class MonthlyScheduleWithSummaryDto(
    val schedules: List<TeamScheduleDto>,
    val dailySummary: List<DailySummaryDto>
)

data class TeamScheduleCreateResultDto(
    val id: Long
)

/**
 * 여사원 일정 다건 삭제 응답 (Spec #691 P1-B, Q5 옵션 1 — 전체 rollback 정책).
 * 1건이라도 가드 fail 시 도메인 예외 throw → @Transactional 전체 rollback → 본 DTO 미반환.
 */
data class TeamScheduleMassDeleteResponse(
    val deletedCount: Int
)

/**
 * 여사원 일정관리 화면 초기 로드 통합 응답.
 *
 * 기존 4개 round-trip (`/branches`, `/members`, `/accounts`, `/professional-promotion-teams`) 을
 * 1회 호출로 합쳐 초기 렌더 latency 축소 + waterfall 제거.
 *
 * `accounts` 채움 조건:
 * - 클라이언트가 `branchCode` 쿼리 파라미터로 지정 → 해당 지점 거래처 (다중지점 사용자의 지점 선택 시점)
 * - 미지정 + 단일지점 사용자 → 본인 지점 거래처 자동 채움
 * - 미지정 + 다중지점 사용자 → 빈 리스트 (지점 선택 전 상태)
 */
data class TeamScheduleFormDto(
    val branches: List<BranchResponse>,
    val members: List<TeamMemberDto>,
    val professionalPromotionTeams: List<String>,
    val accounts: List<TeamScheduleAccountDto>,
    /**
     * 현재 월 + 내 거래처 전체 기준 일별 요약 (진열/행사 expected/actual + 연차/대휴).
     *
     * SF 레거시 정합 — 마운트 시 사용자 선택 없이도 캘린더에 요약이 즉시 노출되도록 form 응답에 포함.
     * 일정 개별 칩 (schedules) 은 본 응답에 포함하지 않음 — 사용자가 거래처 선택 + 조회 시점에만 fetch.
     */
    val dailySummary: List<DailySummaryDto>
)
