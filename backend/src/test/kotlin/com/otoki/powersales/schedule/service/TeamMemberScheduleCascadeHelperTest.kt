package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.exception.TeamScheduleDisplayMasterLinkException
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth

@DisplayName("TeamMemberScheduleCascadeHelper — cascade delete + MFEIS batch refresh")
class TeamMemberScheduleCascadeHelperTest {

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)
    private val teamScheduleValidator: TeamScheduleValidator = mockk(relaxUnitFun = true)
    private val adminMonthlyIntegrationService: AdminMonthlyIntegrationService = mockk(relaxUnitFun = true)

    private val helper = TeamMemberScheduleCascadeHelper(
        teamMemberScheduleRepository,
        teamScheduleValidator,
        adminMonthlyIntegrationService
    )

    private val regularPrincipal: WebUserPrincipal = mockk<WebUserPrincipal>(relaxed = true).also {
        every { it.isSalesSupport } returns false
        every { it.profileName } returns "일반관리자"
    }

    private val adminPrincipal: WebUserPrincipal = mockk<WebUserPrincipal>(relaxed = true).also {
        every { it.isSalesSupport } returns false
        every { it.profileName } returns "시스템 관리자"
    }

    @BeforeEach
    fun resetCommonStubs() {
        every { teamScheduleValidator.validateDisplayMasterLink(any(), any()) } returns Unit
    }

    @Test
    @DisplayName("scheduleIds 빈 list — no-op (findAllById/validateDisplayMasterLink/deleteAll/refresh 호출 0회)")
    fun emptyIdsNoOp() {
        helper.cascadeDeleteByIds(regularPrincipal, emptyList())

        verify(exactly = 0) { teamMemberScheduleRepository.findAllById(any<List<Long>>()) }
        verify(exactly = 0) { teamScheduleValidator.validateDisplayMasterLink(any(), any()) }
        verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any()) }
        verify(exactly = 0) { adminMonthlyIntegrationService.refreshIntegration(any(), any(), any()) }
    }

    @Test
    @DisplayName("schedules 빈 list — no-op (validateDisplayMasterLink/deleteAll/refresh 호출 0회)")
    fun emptySchedulesNoOp() {
        helper.cascadeDeleteSchedules(regularPrincipal, emptyList())

        verify(exactly = 0) { teamScheduleValidator.validateDisplayMasterLink(any(), any()) }
        verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any()) }
        verify(exactly = 0) { adminMonthlyIntegrationService.refreshIntegration(any(), any(), any()) }
    }

    @Test
    @DisplayName("정상 cascade — 가드 통과 + deleteAll + MFEIS refresh (단일 schedule)")
    fun cascadeDeleteSingle() {
        val schedule = makeSchedule(id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 15))
        every { teamMemberScheduleRepository.findAllById(listOf(1L)) } returns listOf(schedule)

        helper.cascadeDeleteByIds(regularPrincipal, listOf(1L))

        verify(exactly = 1) { teamScheduleValidator.validateDisplayMasterLink(false, schedule) }
        verify(exactly = 1) { teamMemberScheduleRepository.deleteAll(listOf(schedule)) }
        verify(exactly = 1) {
            adminMonthlyIntegrationService.refreshIntegration(10L, 100, YearMonth.of(2026, 5))
        }
    }

    @Test
    @DisplayName("MFEIS batch refresh — (employeeId × accountId × YearMonth) distinct 그룹 당 1회")
    fun mfeisBatchRefreshDistinct() {
        val s1 = makeSchedule(id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 1))
        val s2 = makeSchedule(id = 2L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 10)) // 동일 (emp,acc,YM)
        val s3 = makeSchedule(id = 3L, empId = 20L, accId = 100, date = LocalDate.of(2026, 5, 1))  // 다른 emp
        val s4 = makeSchedule(id = 4L, empId = 10L, accId = 100, date = LocalDate.of(2026, 6, 1))  // 다른 month

        helper.cascadeDeleteSchedules(regularPrincipal, listOf(s1, s2, s3, s4))

        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(10L, 100, YearMonth.of(2026, 5)) }
        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(20L, 100, YearMonth.of(2026, 5)) }
        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(10L, 100, YearMonth.of(2026, 6)) }
    }

    @Test
    @DisplayName("workingType != WORK 인 schedule 은 MFEIS refresh 제외")
    fun mfeisExcludesNonWorkType() {
        val workSchedule = makeSchedule(id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 1), workingType = WorkingType.WORK)
        val leaveSchedule = makeSchedule(id = 2L, empId = 20L, accId = 100, date = LocalDate.of(2026, 5, 1), workingType = WorkingType.ANNUAL_LEAVE)

        helper.cascadeDeleteSchedules(regularPrincipal, listOf(workSchedule, leaveSchedule))

        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(10L, 100, YearMonth.of(2026, 5)) }
        verify(exactly = 0) { adminMonthlyIntegrationService.refreshIntegration(20L, 100, any()) }
    }

    @Test
    @DisplayName("validateDisplayMasterLink 위반 시 throw → 호출 측 트랜잭션 rollback (deleteAll/refresh 호출 0회)")
    fun guardViolationThrows() {
        val displayLinkedSchedule = makeSchedule(
            id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 15),
            workingCategory1 = WorkingCategory1.DISPLAY, displayWorkSchedule = mockk()
        )
        every {
            teamScheduleValidator.validateDisplayMasterLink(false, displayLinkedSchedule)
        } throws TeamScheduleDisplayMasterLinkException()

        assertThatThrownBy { helper.cascadeDeleteSchedules(regularPrincipal, listOf(displayLinkedSchedule)) }
            .isInstanceOf(TeamScheduleDisplayMasterLinkException::class.java)

        verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any()) }
        verify(exactly = 0) { adminMonthlyIntegrationService.refreshIntegration(any(), any(), any()) }
    }

    @Test
    @DisplayName("시스템 관리자 — actorIsAdminGrade=true 로 validator 호출 (uncertain DISPLAY 차단 우회)")
    fun adminGradeBypassesGuard() {
        val schedule = makeSchedule(id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 15))

        helper.cascadeDeleteSchedules(adminPrincipal, listOf(schedule))

        verify(exactly = 1) { teamScheduleValidator.validateDisplayMasterLink(true, schedule) }
    }

    @Test
    @DisplayName("isSalesSupport=true — actorIsAdminGrade=true (영업지원 우회)")
    fun salesSupportBypassesGuard() {
        val salesSupportPrincipal: WebUserPrincipal = mockk<WebUserPrincipal>(relaxed = true).also {
            every { it.isSalesSupport } returns true
            every { it.profileName } returns "영업지원"
        }
        val schedule = makeSchedule(id = 1L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 15))

        helper.cascadeDeleteSchedules(salesSupportPrincipal, listOf(schedule))

        verify(exactly = 1) { teamScheduleValidator.validateDisplayMasterLink(true, schedule) }
    }

    @Test
    @DisplayName("필수 필드 (employeeId / accountId / workingDate) 중 하나라도 null 이면 해당 schedule 만 MFEIS refresh 제외")
    fun mfeisExcludesNullFields() {
        val noEmployee = TeamMemberSchedule(
            id = 1L, employee = null, account = accountFor(100),
            workingDate = LocalDate.of(2026, 5, 1), workingType = WorkingType.WORK
        )
        val validSchedule = makeSchedule(id = 2L, empId = 10L, accId = 100, date = LocalDate.of(2026, 5, 1))

        helper.cascadeDeleteSchedules(regularPrincipal, listOf(noEmployee, validSchedule))

        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(10L, 100, YearMonth.of(2026, 5)) }
        // null employee schedule 은 refresh 호출 0회 — 위 verify 의 (10, 100, 5월) 외 다른 호출 없음
        verify(exactly = 1) { adminMonthlyIntegrationService.refreshIntegration(any(), any(), any()) }
    }

    // --- helpers ---

    private fun makeSchedule(
        id: Long,
        empId: Long,
        accId: Long,
        date: LocalDate,
        workingType: WorkingType? = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.EVENT,
        displayWorkSchedule: DisplayWorkSchedule? = null
    ): TeamMemberSchedule = TeamMemberSchedule(
        id = id,
        employee = Employee(id = empId, employeeCode = String.format("%08d", empId), name = "사원$empId"),
        account = accountFor(accId),
        workingDate = date,
        workingType = workingType,
        workingCategory1 = workingCategory1,
        displayWorkSchedule = displayWorkSchedule
    )

    private fun accountFor(id: Long): Account = Account(id = id, name = "거래처$id", externalKey = "EXT$id")
}
