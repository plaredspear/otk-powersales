package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.platform.common.enums.WorkingType
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.org.employee.repository.EmployeeRepository
import com.otoki.powersales.external.sap.inbound.dto.attendance.ScheduleConversionSummary
import com.otoki.powersales.external.sap.inbound.service.AttendInfoToScheduleConverter
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminAttendInfoUpdateRequest
import com.otoki.powersales.domain.activity.schedule.entity.AttendInfo
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.exception.AttendInfoNotFoundException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoDateException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoDateRangeException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoStatusException
import com.otoki.powersales.domain.activity.schedule.exception.InvalidAttendInfoTypeException
import com.otoki.powersales.domain.activity.schedule.repository.AttendInfoRepository
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.activity.schedule.service.AdminAttendInfoService
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.Optional
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

@DisplayName("AdminAttendInfoService 테스트")
class AdminAttendInfoServiceTest {

    private val attendInfoRepository: AttendInfoRepository = mockk(relaxUnitFun = true)

    private val employeeRepository: EmployeeRepository = mockk(relaxUnitFun = true)

    private val teamMemberScheduleRepository: TeamMemberScheduleRepository = mockk(relaxUnitFun = true)

    private val attendInfoToScheduleConverter: AttendInfoToScheduleConverter = mockk(relaxUnitFun = true)

    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk(relaxUnitFun = true)

    private val branchCodeExpander: BranchCodeExpander = mockk(relaxUnitFun = true)

    private val service = AdminAttendInfoService(
        attendInfoRepository,
        employeeRepository,
        teamMemberScheduleRepository,
        attendInfoToScheduleConverter,
        womenScheduleBranchResolver,
        branchCodeExpander,
    )

    @Nested
    @DisplayName("getMembers - 근무기간 조회 여사원 목록")
    inner class GetMembersTests {

        @Test
        @DisplayName("본인 costCenterCode 스코프 + 비활성(퇴사/휴직) 포함 + status 매핑")
        fun getMembers_includesInactive() {
            val principal = principalOf(employeeCode = "20030001", costCenterCode = "1234")
            val active = Employee(id = 2L, employeeCode = "20030002", name = "김영희").apply {
                role = AppAuthority.WOMAN; orgName = "강북유통지점"; jikwee = "사원"; status = "재직"
            }
            val resigned = Employee(id = 3L, employeeCode = "20030003", name = "이수진").apply {
                role = AppAuthority.WOMAN; status = "퇴직"
            }
            // 근무기간 조회는 appLoginActive 무관 — findWomenByCostCenterCodes 사용.
            every { employeeRepository.findWomenByCostCenterCodes(listOf("1234")) } returns listOf(active, resigned)

            val result = service.getMembers(principal)

            assertThat(result).hasSize(2)
            assertThat(result[0].status).isEqualTo("재직")
            assertThat(result[0].orgName).isEqualTo("강북유통지점")
            assertThat(result[0].jikwee).isEqualTo("사원")
            assertThat(result[1].status).isEqualTo("퇴직")
            verify(exactly = 1) { employeeRepository.findWomenByCostCenterCodes(listOf("1234")) }
        }

        @Test
        @DisplayName("costCenterCode 가 비어 있으면 빈 목록 (조회 미수행)")
        fun getMembers_blankCostCenter_returnsEmpty() {
            val principal = principalOf(employeeCode = "20030001", costCenterCode = null)

            val result = service.getMembers(principal)

            assertThat(result).isEmpty()
            verify(exactly = 0) { employeeRepository.findWomenByCostCenterCodes(any()) }
        }

        @Test
        @DisplayName("branchCode 지정 + 권한 허용 → 해당 지점(매핑 확장) 여사원 조회")
        fun getMembers_withAllowedBranch() {
            val principal = principalOf(employeeCode = "99990001", costCenterCode = "9999")
            every { womenScheduleBranchResolver.isBranchAllowed(principal, "5694") } returns true
            every { branchCodeExpander.expand(setOf("5694")) } returns setOf("5694", "5691")
            val woman = Employee(id = 5L, employeeCode = "20030005", name = "박지점").apply {
                role = AppAuthority.WOMAN; status = "재직"
            }
            every { employeeRepository.findWomenByCostCenterCodes(listOf("5694", "5691")) } returns listOf(woman)

            val result = service.getMembers(principal, branchCode = "5694")

            assertThat(result).hasSize(1)
            assertThat(result[0].employeeCode).isEqualTo("20030005")
            verify { employeeRepository.findWomenByCostCenterCodes(listOf("5694", "5691")) }
        }

        @Test
        @DisplayName("branchCode 지정 + 권한 밖 지점 → 빈 목록 (IDOR 차단, 조회 미수행)")
        fun getMembers_withDisallowedBranch_blocked() {
            val principal = principalOf(employeeCode = "20030001", costCenterCode = "1234")
            every { womenScheduleBranchResolver.isBranchAllowed(principal, "9999") } returns false

            val result = service.getMembers(principal, branchCode = "9999")

            assertThat(result).isEmpty()
            verify(exactly = 0) { employeeRepository.findWomenByCostCenterCodes(any()) }
        }
    }

    @Nested
    @DisplayName("getBranches - 권한별 지점")
    inner class GetBranchesTests {

        @Test
        @DisplayName("resolver 결과를 그대로 반환")
        fun getBranches_delegatesToResolver() {
            val principal = principalOf(employeeCode = "20030001", costCenterCode = "1234")
            val branches = listOf(BranchResponse("1234", "강북유통지점"))
            every { womenScheduleBranchResolver.resolveBranches(principal) } returns branches

            val result = service.getBranches(principal)

            assertThat(result).isEqualTo(branches)
        }
    }

    @Nested
    @DisplayName("create - 보정 등록")
    inner class CreateTests {

        @Test
        @DisplayName("정상 등록 + Converter 호출")
        fun create_success_callsConverter() {
            val employee = newEmployee()
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(employee)
            every { attendInfoRepository.save(any<AttendInfo>()) } answers { invocation ->
                firstArg<AttendInfo>()
            }
            every { attendInfoToScheduleConverter.convert(any()) } returns ScheduleConversionSummary(3, 0, 0, 0, 0, 0)
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    any(), any(), any(), any()
                )
} returns emptyList()
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
                reason = "SAP 적재 누락 보정 등록",
            )

            val result = service.create(request)

            assertThat(result.employeeCode).isEqualTo("E001")
            assertThat(result.attendType).isEqualTo("14")
            assertThat(result.attendTypeName).isEqualTo("연차")
            assertThat(result.conversionSummary?.convertedScheduleCount).isEqualTo(3)
            verify { attendInfoToScheduleConverter.convert(any()) }
        }

        @Test
        @DisplayName("status 무효 값 - 차단")
        fun create_invalidStatus_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "Z",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoStatusException::class.java)
        }

        @Test
        @DisplayName("attendType 무효 코드 - 차단")
        fun create_invalidAttendType_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "99",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoTypeException::class.java)
        }

        @Test
        @DisplayName("날짜 형식 오류 - 차단")
        fun create_invalidDateFormat_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "2026-05-18",
                endDate = "20260522",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoDateException::class.java)
        }

        @Test
        @DisplayName("종료일 < 시작일 - 차단")
        fun create_endBeforeStart_throws() {
            val request = AdminAttendInfoCreateRequest(
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260522",
                endDate = "20260518",
                status = "N",
                reason = "테스트 사유",
            )
            assertThatThrownBy { service.create(request) }
                .isInstanceOf(InvalidAttendInfoDateRangeException::class.java)
        }
    }

    @Nested
    @DisplayName("update - 수정 + cascade")
    inner class UpdateTests {

        @Test
        @DisplayName("수정 시 기존 연차 일정 cascade 삭제 후 신규 INSERT")
        fun update_cascadesDeleteAndInsert() {
            val employee = newEmployee()
            val existing = AttendInfo(
                id = 1L,
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            every { attendInfoRepository.findById(1L) } returns Optional.of(existing)
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(employee)
            val oldSchedules = listOf(
                newSchedule(101L, employee, LocalDate.of(2026, 5, 18)),
                newSchedule(102L, employee, LocalDate.of(2026, 5, 19)),
            )
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 22), WorkingType.ANNUAL_LEAVE
                )
} returns oldSchedules
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 25), LocalDate.of(2026, 5, 27), WorkingType.ANNUAL_LEAVE
                )
} returns emptyList()
            every { attendInfoToScheduleConverter.convert(any()) } returns ScheduleConversionSummary(3, 0, 0, 0, 0, 0)

            val request = AdminAttendInfoUpdateRequest(
                startDate = "20260525",
                endDate = "20260527",
                reason = "기간 변경 보정",
            )

            val result = service.update(1L, request)

            assertThat(result.startDate).isEqualTo("20260525")
            assertThat(result.endDate).isEqualTo("20260527")
            verify { teamMemberScheduleRepository.deleteAll(oldSchedules) }
            verify { attendInfoToScheduleConverter.convert(any()) }
            assertThat(result.conversionSummary?.deletedScheduleCount).isEqualTo(2)
            assertThat(result.conversionSummary?.convertedScheduleCount).isEqualTo(3)
        }

        @Test
        @DisplayName("미존재 id - 404")
        fun update_notFound_throws() {
            every { attendInfoRepository.findById(999L) } returns Optional.empty()
            assertThatThrownBy {
                service.update(999L, AdminAttendInfoUpdateRequest(status = "Y"))
            }.isInstanceOf(AttendInfoNotFoundException::class.java)
        }
    }

    @Nested
    @DisplayName("delete - 삭제 + cascade")
    inner class DeleteTests {

        @Test
        @DisplayName("삭제 시 연결 연차 일정 cascade 삭제")
        fun delete_cascadesAnnualLeaveSchedules() {
            val employee = newEmployee()
            val existing = AttendInfo(
                id = 5L,
                employeeCode = "E001",
                attendType = "14",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            every { attendInfoRepository.findById(5L) } returns Optional.of(existing)
            every { employeeRepository.findByEmployeeCode("E001") } returns Optional.of(employee)
            val oldSchedules = listOf(
                newSchedule(201L, employee, LocalDate.of(2026, 5, 18)),
                newSchedule(202L, employee, LocalDate.of(2026, 5, 19)),
                newSchedule(203L, employee, LocalDate.of(2026, 5, 20)),
            )
            every {
                teamMemberScheduleRepository.findAllByEmployeeAndWorkingDateBetweenAndWorkingType(
                    employee, LocalDate.of(2026, 5, 18), LocalDate.of(2026, 5, 22), WorkingType.ANNUAL_LEAVE
                )
} returns oldSchedules
            val result = service.delete(5L)

            assertThat(result.deletedScheduleCount).isEqualTo(3)
            verify { teamMemberScheduleRepository.deleteAll(oldSchedules) }
            verify { attendInfoRepository.delete(existing) }
        }

        @Test
        @DisplayName("연차류 외 코드 - cascade 0건")
        fun delete_nonAnnualLeave_noCascade() {
            val existing = AttendInfo(
                id = 6L,
                employeeCode = "E002",
                attendType = "99",
                startDate = "20260518",
                endDate = "20260522",
                status = "N",
            )
            every { attendInfoRepository.findById(6L) } returns Optional.of(existing)

            val result = service.delete(6L)

            assertThat(result.deletedScheduleCount).isEqualTo(0)
            verify(exactly = 0) { teamMemberScheduleRepository.deleteAll(any<List<TeamMemberSchedule>>()) }
            verify { attendInfoRepository.delete(existing) }
        }
    }

    private fun newEmployee() = Employee(
        id = 100L,
        employeeCode = "E001",
        name = "홍길동",
    ).apply {
        jobCode = "판촉직"
    }

    private fun principalOf(employeeCode: String, costCenterCode: String?): WebUserPrincipal =
        WebUserPrincipal(
            userId = 1L,
            usernameValue = employeeCode,
            employeeCode = employeeCode,
            employeeId = 1L,
            role = null,
            costCenterCode = costCenterCode,
            profileName = "9. Staff",
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true,
        )

    private fun newSchedule(id: Long, employee: Employee, date: LocalDate): TeamMemberSchedule {
        val schedule = TeamMemberSchedule(id = id)
        schedule.employee = employee
        schedule.workingDate = date
        schedule.workingType = WorkingType.ANNUAL_LEAVE
        return schedule
    }
}
