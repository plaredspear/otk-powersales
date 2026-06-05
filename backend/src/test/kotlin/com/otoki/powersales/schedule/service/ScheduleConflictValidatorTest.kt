package com.otoki.powersales.schedule.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.LeaderScheduleCategory3ConflictException
import com.otoki.powersales.schedule.exception.LeaderScheduleCategory3LimitExceededException
import com.otoki.powersales.schedule.exception.LeaderScheduleDuplicateLeaveException
import com.otoki.powersales.schedule.exception.LeaderScheduleDuplicateWorkException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("ScheduleConflictValidator 테스트 (spec.md §1.4.1)")
class ScheduleConflictValidatorTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk()
    private val validator = ScheduleConflictValidator(teamMemberScheduleRepository)

    private val targetEmployeeId = 5012L
    private val workingDate = LocalDate.of(2026, 5, 15)
    private val accountId = 90234L

    @Nested
    @DisplayName("충돌 없음")
    inner class NoConflict {
        @Test
        @DisplayName("기존 일정 없음 -> 통과")
        fun noExisting() {
            stubExisting(emptyList())
            assertThatCode {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.doesNotThrowAnyException()
        }

        @Test
        @DisplayName("workingType 이 근무 외 -> 후속 분기 미실행 통과 (본 스펙 범위 외)")
        fun nonWorkType_passesThrough() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.FIXED)))
            assertThatCode {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.ANNUAL_LEAVE, null, null)
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("규칙 #1 - 연차/대휴 충돌 (DUPLICATE_LEAVE_SCHEDULE)")
    inner class Rule1 {
        @Test
        @DisplayName("기존 연차 1건 -> DUPLICATE_LEAVE_SCHEDULE")
        fun annualLeave() {
            stubExisting(listOf(leaveSchedule(WorkingType.ANNUAL_LEAVE)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.isInstanceOf(LeaderScheduleDuplicateLeaveException::class.java)
        }

        @Test
        @DisplayName("기존 대휴 1건 -> DUPLICATE_LEAVE_SCHEDULE")
        fun altHoliday() {
            stubExisting(listOf(leaveSchedule(WorkingType.ALT_HOLIDAY)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.isInstanceOf(LeaderScheduleDuplicateLeaveException::class.java)
        }
    }

    @Nested
    @DisplayName("규칙 #2 - 같은 거래처 근무 중복 (DUPLICATE_WORK_SCHEDULE)")
    inner class Rule2 {
        @Test
        @DisplayName("동일 거래처 근무 1건 -> DUPLICATE_WORK_SCHEDULE")
        fun sameAccount() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.PATROL, accountIdValue = accountId)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.PATROL)
            }.isInstanceOf(LeaderScheduleDuplicateWorkException::class.java)
        }

        @Test
        @DisplayName("다른 거래처 근무 -> 규칙 #2 미발화 (다른 규칙 평가)")
        fun differentAccount_passesRule2() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.PATROL, accountIdValue = 99999)))
            // 카테고리3=순회 신규 + 기존 순회 1건 (다른 거래처) → 규칙 #7 (고정 충돌 아님) 통과 (고정 0건)
            assertThatCode {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.PATROL)
            }.doesNotThrowAnyException()
        }
    }

    @Nested
    @DisplayName("규칙 #3 / #4 - 카테고리3 갯수 초과 (CATEGORY3_LIMIT_EXCEEDED)")
    inner class Rule3And4 {
        @Test
        @DisplayName("기존 고정 1건 + 신규 고정 -> CATEGORY3_LIMIT_EXCEEDED")
        fun fixedLimit() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.FIXED, accountIdValue = 11111)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.isInstanceOf(LeaderScheduleCategory3LimitExceededException::class.java)
        }

        @Test
        @DisplayName("기존 격고 2건 + 신규 격고 -> CATEGORY3_LIMIT_EXCEEDED")
        fun biweeklyLimit() {
            stubExisting(listOf(
                workSchedule(category3 = WorkingCategory3.ALTERNATE, accountIdValue = 11111),
                workSchedule(category3 = WorkingCategory3.ALTERNATE, accountIdValue = 22222)
            ))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.ALTERNATE)
            }.isInstanceOf(LeaderScheduleCategory3LimitExceededException::class.java)
        }
    }

    @Nested
    @DisplayName("규칙 #5/#6/#7 - 카테고리3 간 충돌 (CATEGORY3_CONFLICT)")
    inner class Rule5To7 {
        @Test
        @DisplayName("기존 격고 1건 + 신규 고정 -> CATEGORY3_CONFLICT (#5)")
        fun fixedVsBiweekly() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.ALTERNATE, accountIdValue = 11111)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.isInstanceOf(LeaderScheduleCategory3ConflictException::class.java)
        }

        @Test
        @DisplayName("기존 순회 1건 + 신규 고정 -> CATEGORY3_CONFLICT (#5)")
        fun fixedVsRotating() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.PATROL, accountIdValue = 11111)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.FIXED)
            }.isInstanceOf(LeaderScheduleCategory3ConflictException::class.java)
        }

        @Test
        @DisplayName("기존 고정 1건 + 신규 격고 -> CATEGORY3_CONFLICT (#6)")
        fun biweeklyVsFixed() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.FIXED, accountIdValue = 11111)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.ALTERNATE)
            }.isInstanceOf(LeaderScheduleCategory3ConflictException::class.java)
        }

        @Test
        @DisplayName("기존 고정 1건 + 신규 순회 -> CATEGORY3_CONFLICT (#7)")
        fun rotatingVsFixed() {
            stubExisting(listOf(workSchedule(category3 = WorkingCategory3.FIXED, accountIdValue = 11111)))
            assertThatThrownBy {
                validator.validateConflicts(targetEmployeeId, workingDate, WorkingType.WORK, accountId, WorkingCategory3.PATROL)
            }.isInstanceOf(LeaderScheduleCategory3ConflictException::class.java)
        }
    }

    // ========== Helpers ==========

    private fun stubExisting(schedules: List<TeamMemberSchedule>) {
        every { teamMemberScheduleRepository.findByEmployeeIdAndWorkingDate(any(), any()) } returns schedules
    }

    private fun workSchedule(category3: WorkingCategory3, accountIdValue: Long? = null): TeamMemberSchedule {
        val account = accountIdValue?.let { Account(id = it) }
        return TeamMemberSchedule(
            employee = Employee(id = targetEmployeeId, employeeCode = "20300001", name = "팀원"),
            account = account,
            workingDate = workingDate,
            workingType = WorkingType.WORK,
            workingCategory3 = category3
        )
    }

    private fun leaveSchedule(workingType: WorkingType): TeamMemberSchedule {
        return TeamMemberSchedule(
            employee = Employee(id = targetEmployeeId, employeeCode = "20300001", name = "팀원"),
            workingDate = workingDate,
            workingType = workingType
        )
    }
}
