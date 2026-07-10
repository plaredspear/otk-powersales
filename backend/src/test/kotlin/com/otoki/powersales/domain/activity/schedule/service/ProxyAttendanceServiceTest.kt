package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.activity.schedule.dto.request.ProxyAttendanceRegisterRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.LeaderDailyStatusSummary
import com.otoki.powersales.domain.activity.schedule.enums.AttendanceType
import com.otoki.powersales.domain.activity.schedule.exception.LeaderScheduleTargetEmployeeNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceBranchNotAllowedException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceNotAllowedException
import com.otoki.powersales.domain.activity.schedule.exception.ProxyAttendanceNotBranchMemberException
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional

@DisplayName("ProxyAttendanceService (AccountViewAll 대리출근) 테스트")
class ProxyAttendanceServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()
    private val proxyAttendanceBranchResolver: ProxyAttendanceBranchResolver = mockk()
    private val teamDailyStatusCalculator: TeamDailyStatusCalculator = mockk()
    private val attendanceService: AttendanceService = mockk()

    private val service = ProxyAttendanceService(
        employeeRepository,
        proxyAttendanceBranchResolver,
        teamDailyStatusCalculator,
        attendanceService,
    )

    private val branchCode = "5832" // 원주1지점

    @Nested
    @DisplayName("getBranches")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - AccountViewAll -> 지점 목록 반환")
        fun success() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.resolveBranches() } returns
                listOf(BranchResponse(branchCode, "원주1지점"))

            val result = service.getBranches(1)

            assertThat(result).hasSize(1)
            assertThat(result[0].branchCode).isEqualTo(branchCode)
        }

        @Test
        @DisplayName("실패 - 비 AccountViewAll -> PROXY_ATTENDANCE_NOT_ALLOWED")
        fun notAllowed() {
            val leader = employee(id = 1, role = AppAuthority.LEADER)
            every { employeeRepository.findById(1) } returns Optional.of(leader)

            assertThatThrownBy { service.getBranches(1) }
                .isInstanceOf(ProxyAttendanceNotAllowedException::class.java)
        }
    }

    @Nested
    @DisplayName("getTeamMembers")
    inner class GetTeamMembers {

        @Test
        @DisplayName("성공 - 지점 여사원(조장·지점장 제외, 퇴직 제외, 이름순)")
        fun success() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns true
            val w1 = employee(id = 10, role = AppAuthority.WOMAN, name = "박여사", costCenterCode = branchCode)
            val w2 = employee(id = 11, role = AppAuthority.WOMAN, name = "김여사", costCenterCode = branchCode)
            val resigned = employee(id = 12, role = AppAuthority.WOMAN, name = "이여사", costCenterCode = branchCode, status = "퇴직")
            every {
                employeeRepository.findByCostCenterCodeAndRoleNotIn(
                    branchCode, listOf(AppAuthority.LEADER, AppAuthority.BRANCH_MANAGER)
                )
            } returns listOf(w1, w2, resigned)

            val result = service.getTeamMembers(1, branchCode)

            // 퇴직 제외 + 이름 가나다순
            assertThat(result.map { it.name }).containsExactly("김여사", "박여사")
        }

        @Test
        @DisplayName("실패 - 허용되지 않은 지점(IDOR) -> BRANCH_NOT_ALLOWED")
        fun branchNotAllowed() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns false

            assertThatThrownBy { service.getTeamMembers(1, branchCode) }
                .isInstanceOf(ProxyAttendanceBranchNotAllowedException::class.java)
        }
    }

    @Nested
    @DisplayName("getDailyStatus")
    inner class GetDailyStatus {

        @Test
        @DisplayName("성공 - 선택 지점 인원으로 계산기 위임")
        fun success() {
            val date = LocalDate.of(2026, 6, 10)
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns true
            every { employeeRepository.findByCostCenterCodeIn(listOf(branchCode)) } returns
                listOf(employee(id = 10, role = AppAuthority.WOMAN, costCenterCode = branchCode))
            val expected = emptyStatus(date)
            every { teamDailyStatusCalculator.computeDailyStatus(listOf(10L), date) } returns expected

            val result = service.getDailyStatus(1, branchCode, date)

            assertThat(result).isSameAs(expected)
            verify { teamDailyStatusCalculator.computeDailyStatus(listOf(10L), date) }
        }

        @Test
        @DisplayName("실패 - 허용되지 않은 지점 -> BRANCH_NOT_ALLOWED")
        fun branchNotAllowed() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns false

            assertThatThrownBy { service.getDailyStatus(1, branchCode, LocalDate.of(2026, 6, 10)) }
                .isInstanceOf(ProxyAttendanceBranchNotAllowedException::class.java)
        }
    }

    @Nested
    @DisplayName("registerProxyAttendance")
    inner class Register {

        @Test
        @DisplayName("성공 - 권한/지점/소속 검증 후 registerProxy 위임 (GPS 스킵)")
        fun success() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            val target = employee(id = 10, role = AppAuthority.WOMAN, costCenterCode = branchCode)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns true
            every { employeeRepository.findById(10) } returns Optional.of(target)
            val expected = registerResponse()
            every {
                attendanceService.registerProxy(target, scheduleId = null, displayWorkScheduleId = 500L)
            } returns expected

            val result = service.registerProxyAttendance(
                1,
                ProxyAttendanceRegisterRequest(
                    branchCode = branchCode,
                    targetEmployeeId = 10,
                    displayWorkScheduleId = 500L,
                )
            )

            assertThat(result).isSameAs(expected)
            assertThat(result.gpsSkipped).isTrue()
            verify { attendanceService.registerProxy(target, null, 500L) }
        }

        @Test
        @DisplayName("실패 - 비 AccountViewAll -> NOT_ALLOWED")
        fun notAllowed() {
            val leader = employee(id = 1, role = AppAuthority.LEADER)
            every { employeeRepository.findById(1) } returns Optional.of(leader)

            assertThatThrownBy {
                service.registerProxyAttendance(
                    1, ProxyAttendanceRegisterRequest(branchCode, 10, null, 500L)
                )
            }.isInstanceOf(ProxyAttendanceNotAllowedException::class.java)
        }

        @Test
        @DisplayName("실패 - 대상 여사원이 선택 지점 소속 아님 -> NOT_BRANCH_MEMBER")
        fun notBranchMember() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            val target = employee(id = 10, role = AppAuthority.WOMAN, costCenterCode = "9999")
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns true
            every { employeeRepository.findById(10) } returns Optional.of(target)

            assertThatThrownBy {
                service.registerProxyAttendance(
                    1, ProxyAttendanceRegisterRequest(branchCode, 10, null, 500L)
                )
            }.isInstanceOf(ProxyAttendanceNotBranchMemberException::class.java)
        }

        @Test
        @DisplayName("실패 - 대상 여사원 미존재 -> TARGET_EMPLOYEE_NOT_FOUND")
        fun targetNotFound() {
            val viewer = employee(id = 1, role = AppAuthority.ACCOUNT_VIEW_ALL)
            every { employeeRepository.findById(1) } returns Optional.of(viewer)
            every { proxyAttendanceBranchResolver.isBranchAllowed(branchCode) } returns true
            every { employeeRepository.findById(10) } returns Optional.empty()

            assertThatThrownBy {
                service.registerProxyAttendance(
                    1, ProxyAttendanceRegisterRequest(branchCode, 10, null, 500L)
                )
            }.isInstanceOf(LeaderScheduleTargetEmployeeNotFoundException::class.java)
        }
    }

    // ========== Helpers ==========

    private fun emptyStatus(date: LocalDate) = LeaderDailyStatusResponse(
        date = date.toString(),
        summary = LeaderDailyStatusSummary(0, 0, 0, 0, 0),
        displayWorkers = emptyList(),
        eventWorkers = emptyList(),
        annualLeaveWorkers = emptyList(),
    )

    private fun registerResponse() = AttendanceRegisterResponse(
        scheduleId = 1L,
        accountName = "마트A",
        workType = "근무",
        distanceKm = 0.0,
        totalCount = 1,
        registeredCount = 1,
        gpsSkipped = true,
        attendanceType = AttendanceType.DISPLAY,
        displayWorkScheduleId = 500L,
        scheduleStartDate = null,
        scheduleEndDate = null,
    )

    private fun employee(
        id: Long,
        role: String,
        name: String = "사원$id",
        costCenterCode: String? = "5832",
        status: String? = "활동",
    ): Employee = Employee(
        id = id,
        employeeCode = "E$id",
        name = name,
        password = "encoded",
        role = role,
        costCenterCode = costCenterCode,
        status = status,
    )
}
