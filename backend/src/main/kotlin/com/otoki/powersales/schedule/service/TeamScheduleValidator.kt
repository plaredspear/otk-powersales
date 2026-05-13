package com.otoki.powersales.schedule.service

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.entity.WorkingCategory1
import com.otoki.powersales.common.entity.WorkingCategory3
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.TeamScheduleConflictException
import com.otoki.powersales.schedule.exception.TeamScheduleDisplayMasterLinkException
import com.otoki.powersales.schedule.exception.TeamScheduleEmployeeOnLeaveException
import com.otoki.powersales.schedule.exception.TeamScheduleEmployeeResignedException
import com.otoki.powersales.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 단건/일괄 일정 변경에서 공유하는 검증 헬퍼.
 *
 * 기존 `AdminTeamScheduleService` 내부 private validator (스펙 #571 P1-B 추출 대상)
 * 와 동일 동작을 제공하며, 행사 단위 일괄 API(`AdminPromotionScheduleService`)에서도 재사용된다.
 */
@Component
class TeamScheduleValidator(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository,
    private val displayWorkScheduleRepository: DisplayWorkScheduleRepository
) {

    fun validateEmployeeStatus(employee: Employee) {
        when (employee.status) {
            "휴직" -> throw TeamScheduleEmployeeOnLeaveException()
            "퇴직" -> throw TeamScheduleEmployeeResignedException()
        }
    }

    fun validateDisplayMasterLink(currentEmployee: Employee, schedule: TeamMemberSchedule) {
        if (currentEmployee.role in UserRole.ADMIN_GRADE) return
        if (schedule.workingCategory1 != WorkingCategory1.DISPLAY) return

        val employeeId = schedule.employee?.id ?: return
        val accountId = schedule.account?.id ?: return
        val workingDate = schedule.workingDate ?: return

        if (displayWorkScheduleRepository.existsConfirmedByEmployeeAndAccountAndDate(employeeId, accountId, workingDate)) {
            throw TeamScheduleDisplayMasterLinkException()
        }
    }

    fun validateScheduleConflict(
        employeeId: Long,
        workingDate: LocalDate,
        workingCategory3: WorkingCategory3?,
        excludeId: Long?
    ) {
        validateScheduleConflict(employeeId, workingDate, workingCategory3, excludeIds = excludeId?.let { setOf(it) } ?: emptySet())
    }

    /**
     * 일괄 변경에서 동일 요청 안에서 변경 대상 schedule_id 들을 한꺼번에 제외해야 하므로
     * 다중 excludeIds 를 받는 오버로드를 제공한다. excludeIds 에 포함된 일정은 충돌 비교에서 제외한다.
     */
    fun validateScheduleConflict(
        employeeId: Long,
        workingDate: LocalDate,
        workingCategory3: WorkingCategory3?,
        excludeIds: Set<Long>
    ) {
        val existing = teamMemberScheduleRepository.findActiveByEmployeeIdAndDate(employeeId, workingDate)
            .filter { it.id !in excludeIds }

        if (existing.isEmpty()) return

        when (workingCategory3) {
            WorkingCategory3.FIXED -> {
                if (existing.any { it.workingCategory3 == WorkingCategory3.FIXED }) {
                    throw TeamScheduleConflictException("해당 날짜에 고정 일정이 이미 존재합니다")
                }
                if (existing.isNotEmpty()) {
                    throw TeamScheduleConflictException("다른 유형의 일정이 있는 날짜에 고정을 추가할 수 없습니다")
                }
            }
            WorkingCategory3.ALTERNATE -> {
                if (existing.any { it.workingCategory3 == WorkingCategory3.FIXED }) {
                    throw TeamScheduleConflictException("고정 일정이 있는 날짜에는 다른 유형을 추가할 수 없습니다")
                }
                val alternateCount = existing.count { it.workingCategory3 == WorkingCategory3.ALTERNATE }
                if (alternateCount >= 2) {
                    throw TeamScheduleConflictException("해당 날짜에 격고 일정이 이미 2건 존재합니다")
                }
                if (existing.any { it.workingCategory3 == WorkingCategory3.PATROL } && alternateCount >= 1) {
                    throw TeamScheduleConflictException("순회 일정이 존재하므로 격고는 1건만 등록 가능합니다")
                }
            }
            WorkingCategory3.PATROL -> {
                if (existing.any { it.workingCategory3 == WorkingCategory3.FIXED }) {
                    throw TeamScheduleConflictException("고정 일정이 있는 날짜에는 다른 유형을 추가할 수 없습니다")
                }
            }
            null -> Unit
        }
    }
}
