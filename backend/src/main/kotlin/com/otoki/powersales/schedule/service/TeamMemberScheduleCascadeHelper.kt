package com.otoki.powersales.schedule.service

import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Component
import java.time.YearMonth

/**
 * Spec #693 — cascade 경로의 TeamMemberSchedule hard delete 공통 helper.
 *
 * Promotion / PromotionEmployee / PromotionSchedule 도메인의 cascade delete 5종에서 호출.
 * (1) `validateDisplayMasterLink` 가드 — legacy `checkDisplayMaster:1205-1232` 동등
 * (2) MFEIS batch refresh — legacy `updateMonthlyFemaleEmployeeIntegrationSchedule:608-818` 동등
 *
 * 단건 `AdminTeamScheduleService.deleteSchedule` / 다건 `massDelete` 의 `validateDeleteGuards` helper 는
 * `attendanceLog` 가드 (legacy `deleteblock`) 도 포함하나, cascade 경로의 schedule 은 행사조원 schedule
 * (`workingCategory1 == DISPLAY` 진열 schedule 위주) 이라 출근완료 가드는 도메인 의미상 적용하지 않는다.
 * 본 helper 는 cascade 전용 게이트만 포함.
 *
 * BRANCH_MANAGER 차단 등 cascade 진입 자체에 대한 정책 가드는 호출 측 책임 (예: Promotion 삭제 가드 /
 * PE 마감 가드 / bulk delete promoCloseByTm 가드).
 */
@Component
class TeamMemberScheduleCascadeHelper(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val teamScheduleValidator: TeamScheduleValidator,
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService
) {

    /**
     * cascade hard delete — schedule id 목록 입력. 내부에서 `findAllById` 로 엔티티 조회 후 위임.
     *
     * 1건이라도 `validateDisplayMasterLink` 위반 시 `TeamScheduleDisplayMasterLinkException` throw →
     * 호출 측 `@Transactional` 전체 rollback (Q1 옵션 1 — legacy Trigger before delete 게이트 동등).
     *
     * MFEIS refresh 는 `workingType == WORK` schedule 의 `(employeeId, accountId, YearMonth)` distinct
     * 그룹 당 1회 호출 (Q2 옵션 1 — DML 후 호출 + #691 batch 패턴).
     *
     * @param principal cascade 진입 사용자 — `actorIsAdminGrade` 분기에 사용
     * @param scheduleIds cascade 대상 schedule id 목록. 빈 list 면 no-op.
     */
    fun cascadeDeleteByIds(principal: WebUserPrincipal, scheduleIds: List<Long>) {
        if (scheduleIds.isEmpty()) return

        val schedules = teamMemberScheduleRepository.findAllById(scheduleIds)
        cascadeDeleteSchedules(principal, schedules)
    }

    /**
     * cascade hard delete — schedule 엔티티 list 입력 (id 오버로드의 본체).
     *
     * `findAllById` 재조회 비용 회피용. cascade 호출처가 PE 조회 → schedule 수집 흐름에서
     * 이미 schedule 엔티티를 보유한 경우 본 메소드 사용.
     */
    fun cascadeDeleteSchedules(principal: WebUserPrincipal, schedules: List<TeamMemberSchedule>) {
        if (schedules.isEmpty()) return

        val actorIsAdminGrade = actorIsAdminGrade(principal)

        // Q1 옵션 1 — 1건 가드 fail 시 throw → 호출 측 트랜잭션 전체 rollback (legacy allOrNone=true 동등)
        schedules.forEach { schedule ->
            teamScheduleValidator.validateDisplayMasterLink(actorIsAdminGrade, schedule)
        }

        val refreshTargets = schedules
            .filter { it.workingType == WorkingType.WORK }
            .mapNotNull { schedule ->
                val empId = schedule.employee?.id
                val accId = schedule.account?.id
                val date = schedule.workingDate
                if (empId != null && accId != null && date != null) {
                    Triple(empId, accId, YearMonth.from(date))
                } else null
            }
            .distinct()

        teamMemberScheduleRepository.deleteAll(schedules)

        // Q2 옵션 1 — DML 후 호출. (employeeId × accountId × YearMonth) groupBy 후 그룹 당 1회 refresh
        for ((empId, accId, yearMonth) in refreshTargets) {
            adminMonthlyIntegrationService.refreshIntegration(empId, accId, yearMonth)
        }
    }

    /**
     * SF ADMIN_GRADE 동등 — Profile.Name == "시스템 관리자" OR User.isSalesSupport.
     * `AdminTeamScheduleService.actorIsAdminGrade` 와 동일 정책.
     */
    private fun actorIsAdminGrade(principal: WebUserPrincipal): Boolean =
        principal.isSalesSupport || principal.profileName == SYSTEM_ADMIN_PROFILE_NAME

    companion object {
        /** SF 시스템 관리자 Profile.Name ([SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME] 와 동일 값). */
        private const val SYSTEM_ADMIN_PROFILE_NAME = "시스템 관리자"
    }
}
