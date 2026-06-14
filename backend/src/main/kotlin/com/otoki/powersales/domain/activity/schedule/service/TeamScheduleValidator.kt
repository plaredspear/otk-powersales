package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.exception.TeamScheduleConflictException
import com.otoki.powersales.domain.activity.schedule.exception.TeamScheduleDisplayMasterLinkException
import com.otoki.powersales.domain.activity.schedule.exception.TeamScheduleEmployeeOnLeaveException
import com.otoki.powersales.domain.activity.schedule.exception.TeamScheduleEmployeeResignedException
import com.otoki.powersales.domain.activity.schedule.repository.DisplayWorkScheduleRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.domain.org.employee.entity.Employee
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 일정 변경에서 공유하는 검증 헬퍼.
 *
 * 기존 `AdminTeamScheduleService` 내부 private validator 를 추출하여 동일 동작을 제공한다.
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

    /**
     * UC-13 양방향 잠금 — 여사원일정이 진열마스터에 lookup 으로 연결된 상태에서
     * 수정·삭제 시도 시 차단 (ADMIN_GRADE 예외).
     * 레거시 SF TeamMemberScheduleTrigger 의 chain 1-hop 차단 동등.
     *
     * 매칭 키: `schedule.displayWorkSchedule` (FK) — 레거시 SF lookup FK 매칭과 동등.
     * (이전 값 매칭 — 사원·거래처·날짜 조합 — 은 false positive/negative 위험으로 FK 매칭으로 전환.
     *  UC-06 단건 삭제 차단과 일관된 정책. FK 백필은 AttendanceService 진열 출근 경로에서 자동 채움)
     */
    fun validateDisplayMasterLink(actorIsAdminGrade: Boolean, schedule: TeamMemberSchedule) {
        if (actorIsAdminGrade) return
        if (schedule.workingCategory1 != WorkingCategory1.DISPLAY) return

        if (schedule.displayWorkSchedule != null) {
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
