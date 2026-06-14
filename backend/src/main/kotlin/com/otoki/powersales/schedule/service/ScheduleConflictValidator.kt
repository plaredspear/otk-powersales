package com.otoki.powersales.schedule.service

import com.otoki.powersales.platform.common.enums.WorkingCategory3
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.schedule.exception.LeaderScheduleCategory3ConflictException
import com.otoki.powersales.schedule.exception.LeaderScheduleCategory3LimitExceededException
import com.otoki.powersales.schedule.exception.LeaderScheduleDuplicateLeaveException
import com.otoki.powersales.schedule.exception.LeaderScheduleDuplicateWorkException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import org.springframework.stereotype.Component
import java.time.LocalDate

/**
 * 일정 충돌 검증기 (Spec #554 P1-B §5.3).
 *
 * spec.md §1.4.1 의 7개 충돌 규칙을 (대상 직원 + 일자) 기준으로 평가하여, 첫 매칭 시 즉시 예외를 throw 한다.
 * 향후 본인 직접 등록 등 후속 스펙에서 재사용 가능하도록 분리했다.
 */
@Component
class ScheduleConflictValidator(
    private val teamMemberScheduleRepository: TeamMemberScheduleRepository
) {

    companion object {
        private val WORKING_TYPE_WORK = WorkingType.WORK
        private val WORKING_TYPE_ANNUAL_LEAVE = WorkingType.ANNUAL_LEAVE
        private val WORKING_TYPE_ALT_HOLIDAY = WorkingType.ALT_HOLIDAY
        private val CATEGORY3_FIXED = WorkingCategory3.FIXED
        private val CATEGORY3_BIWEEKLY = WorkingCategory3.ALTERNATE
        private val CATEGORY3_ROTATING = WorkingCategory3.PATROL
    }

    /**
     * 동일 (employeeId, workingDate) 의 기존 일정과 충돌하는지 검증한다.
     *
     * 본 스펙 (조장 대리 등록) 진입점에서는 항상 workingType="근무" 로 호출된다.
     * 후속 스펙(본인 직접 등록 등) 에서 연차/대휴 등록을 추가할 때 분기를 확장한다.
     *
     * @throws LeaderScheduleDuplicateLeaveException §1.4.1 #1
     * @throws LeaderScheduleDuplicateWorkException §1.4.1 #2
     * @throws LeaderScheduleCategory3LimitExceededException §1.4.1 #3, #4
     * @throws LeaderScheduleCategory3ConflictException §1.4.1 #5, #6, #7
     *
     * @param excludeScheduleId 수정(P7) 시 자기 자신 일정을 충돌 후보에서 제외한다 (등록 시 null).
     */
    fun validateConflicts(
        employeeId: Long,
        workingDate: LocalDate,
        workingType: WorkingType,
        accountId: Long?,
        workingCategory3: WorkingCategory3?,
        excludeScheduleId: Long? = null
    ) {
        val existing = teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(employeeId, workingDate)
            .filter { excludeScheduleId == null || it.id != excludeScheduleId }
        if (existing.isEmpty()) return

        if (workingType != WORKING_TYPE_WORK) {
            // 본 스펙 범위 외. 후속 스펙에서 분기 확장.
            return
        }

        // 규칙 #1: 동일 일자 연차/대휴 일정 존재 시 거부
        val hasLeave = existing.any { it.workingType == WORKING_TYPE_ANNUAL_LEAVE || it.workingType == WORKING_TYPE_ALT_HOLIDAY }
        if (hasLeave) throw LeaderScheduleDuplicateLeaveException()

        // 규칙 #2: 같은 (사원, 일자, 거래처) 근무 중복
        if (accountId != null) {
            val hasSameAccountWork = existing.any {
                it.workingType == WORKING_TYPE_WORK && it.account?.id == accountId
            }
            if (hasSameAccountWork) throw LeaderScheduleDuplicateWorkException()
        }

        // 카테고리3 갯수 / 충돌 평가 (#3 ~ #7)
        val workSchedules = existing.filter { it.workingType == WORKING_TYPE_WORK }
        val fixedCount = workSchedules.count { it.workingCategory3 == CATEGORY3_FIXED }
        val biweeklyCount = workSchedules.count { it.workingCategory3 == CATEGORY3_BIWEEKLY }
        val rotatingCount = workSchedules.count { it.workingCategory3 == CATEGORY3_ROTATING }

        when (workingCategory3) {
            CATEGORY3_FIXED -> {
                // 규칙 #3: 같은 일자에 고정 1건 이상 존재
                if (fixedCount >= 1) throw LeaderScheduleCategory3LimitExceededException()
                // 규칙 #5: 같은 일자에 격고/순회 1건 이상 존재 (고정과 양립 불가)
                if (biweeklyCount >= 1 || rotatingCount >= 1) throw LeaderScheduleCategory3ConflictException()
            }

            CATEGORY3_BIWEEKLY -> {
                // 규칙 #4: 같은 일자에 격고 2건 이상 존재
                if (biweeklyCount >= 2) throw LeaderScheduleCategory3LimitExceededException()
                // 규칙 #6: 같은 일자에 고정 1건 이상 존재
                if (fixedCount >= 1) throw LeaderScheduleCategory3ConflictException()
            }

            CATEGORY3_ROTATING -> {
                // 규칙 #7: 같은 일자에 고정 1건 이상 존재
                if (fixedCount >= 1) throw LeaderScheduleCategory3ConflictException()
            }
            else -> Unit
        }
    }
}
