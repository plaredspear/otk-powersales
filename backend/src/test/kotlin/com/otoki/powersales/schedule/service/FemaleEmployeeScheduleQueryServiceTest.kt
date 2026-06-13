package com.otoki.powersales.schedule.service

import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import com.otoki.powersales.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.schedule.entity.AttendanceLog
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.querydsl.jpa.impl.JPAQueryFactory
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("FemaleEmployeeScheduleQueryService (Spec 812)")
class FemaleEmployeeScheduleQueryServiceTest {

    private lateinit var expander: BranchCodeExpander
    private lateinit var employeeRepository: EmployeeRepository
    private lateinit var queryFactory: JPAQueryFactory
    private lateinit var service: FemaleEmployeeScheduleQueryService

    @BeforeEach
    fun setUp() {
        expander = mockk()
        employeeRepository = mockk()
        queryFactory = mockk()
        service = FemaleEmployeeScheduleQueryService(expander, employeeRepository, queryFactory)
    }

    @Nested
    @DisplayName("resolveFetchAllCostCenterCodes — fetchAllShcedule 분기 (D2=(i), D2-a)")
    inner class FetchAllBranch {

        @Test
        @DisplayName("하드코딩 사번 (원미희 외 3명) → FETCH_ALL_HARDCODED_CODES 5개 사용, expander 미호출")
        fun hardcodedSabun() {
            val result = service.resolveFetchAllCostCenterCodes("19951029", "9999")
            assertThat(result).containsExactlyInAnyOrder("3233", "3234", "3235", "3236", "5691")
            verify(exactly = 0) { expander.expand(any()) }
        }

        @Test
        @DisplayName("일반 사용자 → expander.expand([cost_center_code]) 결과 사용")
        fun normalSabun() {
            every { expander.expand(listOf("5849")) } returns setOf("5479", "5849")
            val result = service.resolveFetchAllCostCenterCodes("99999999", "5849")
            assertThat(result).containsExactlyInAnyOrder("5479", "5849")
            verify(exactly = 1) { expander.expand(listOf("5849")) }
        }

        @Test
        @DisplayName("사번 null + cost_center_code null → 빈 결과 (expander 미호출)")
        fun bothNull() {
            val result = service.resolveFetchAllCostCenterCodes(null, null)
            assertThat(result).isEmpty()
            verify(exactly = 0) { expander.expand(any()) }
        }

        @Test
        @DisplayName("사번 null + cost_center_code 존재 → expander 호출 (하드코딩 분기 미진입)")
        fun sabunNullButCostCenterPresent() {
            every { expander.expand(listOf("4148")) } returns setOf("4148")
            val result = service.resolveFetchAllCostCenterCodes(null, "4148")
            assertThat(result).containsExactly("4148")
        }
    }

    @Nested
    @DisplayName("summary — fetchScheduleSummary 분기 (D1=b SF 비대칭, D2-a 6개)")
    inner class SummaryBranch {

        @Test
        @DisplayName("하드코딩 사번 → SUMMARY_HARDCODED_CODES 6개 + role='여사원' 으로 조회")
        fun hardcodedSabun() {
            every {
                employeeRepository.findByCostCenterCodeInAndRole(
                    costCenterCodes = match { it.toSet() == setOf("3234", "3233", "3235", "3236", "5691", "569") },
                    role = AppAuthority.WOMAN,
                )
            } returns emptyList()

            val result = service.summary(
                currentUserSabun = "20060052",
                currentUserCostCenterCode = "9999",
                year = 2026,
                month = 5,
            )

            assertThat(result).isEmpty()
            verify(exactly = 1) {
                employeeRepository.findByCostCenterCodeInAndRole(any(), AppAuthority.WOMAN)
            }
            // BranchMapping 미적용 (D1=b)
            verify(exactly = 0) { expander.expand(any()) }
        }

        @Test
        @DisplayName("일반 사용자 → 본인 cost_center_code 단일 일치 (BranchMapping 미사용, D1=b)")
        fun normalSabun() {
            every {
                employeeRepository.findByCostCenterCodeAndRole("5849", AppAuthority.WOMAN)
            } returns emptyList()

            val result = service.summary(
                currentUserSabun = "99999999",
                currentUserCostCenterCode = "5849",
                year = 2026,
                month = 5,
            )

            assertThat(result).isEmpty()
            verify(exactly = 1) {
                employeeRepository.findByCostCenterCodeAndRole("5849", AppAuthority.WOMAN)
            }
            verify(exactly = 0) { expander.expand(any()) }
        }

        @Test
        @DisplayName("사번 null → 빈 결과 (repository 미호출)")
        fun sabunNull() {
            val result = service.summary(null, "5849", 2026, 5)
            assertThat(result).isEmpty()
            verify(exactly = 0) {
                employeeRepository.findByCostCenterCodeAndRole(any(), any())
                employeeRepository.findByCostCenterCodeInAndRole(any(), any())
            }
        }

        @Test
        @DisplayName("D2-a — fetchAll 5개 vs summary 6개 코드셋이 다름 ('569' 추가)")
        fun fetchAllVsSummaryCodeSetsDiffer() {
            assertThat(FemaleEmployeeScheduleQueryService.FETCH_ALL_HARDCODED_CODES).hasSize(5)
            assertThat(FemaleEmployeeScheduleQueryService.SUMMARY_HARDCODED_CODES).hasSize(6)
            assertThat(FemaleEmployeeScheduleQueryService.SUMMARY_HARDCODED_CODES)
                .containsAll(FemaleEmployeeScheduleQueryService.FETCH_ALL_HARDCODED_CODES)
            assertThat(FemaleEmployeeScheduleQueryService.SUMMARY_HARDCODED_CODES.minus(FemaleEmployeeScheduleQueryService.FETCH_ALL_HARDCODED_CODES))
                .containsExactly("569")
        }
    }

    @Nested
    @DisplayName("aggregateSummary — 일별 카운트 집계 (SF AggregateResult 6벌 동등)")
    inner class AggregateSummary {

        @Test
        @DisplayName("근무 + 비행사 + CommuteLog 존재 → expected + actual 동시 증가")
        fun workNonEventWithCommute() {
            val rows = listOf(
                makeRow(day = 5, workingType = WorkingType.WORK, cat1 = WorkingCategory1.DISPLAY, hasCommute = true),
                makeRow(day = 5, workingType = WorkingType.WORK, cat1 = WorkingCategory1.DISPLAY, hasCommute = false),
            )

            val result = service.aggregateSummary(rows, 2026, 5)

            assertThat(result).hasSize(1)
            assertThat(result[0].day).isEqualTo(5)
            assertThat(result[0].expected).isEqualTo(2)
            assertThat(result[0].actual).isEqualTo(1)
            assertThat(result[0].expectedPromo).isNull()
            assertThat(result[0].holiday).isNull()
            assertThat(result[0].subHoliday).isNull()
        }

        @Test
        @DisplayName("근무 + 행사 → expectedPromo / actualPromo 로 분리")
        fun workEvent() {
            val rows = listOf(
                makeRow(day = 10, workingType = WorkingType.WORK, cat1 = WorkingCategory1.EVENT, hasCommute = true),
                makeRow(day = 10, workingType = WorkingType.WORK, cat1 = WorkingCategory1.EVENT, hasCommute = false),
            )

            val result = service.aggregateSummary(rows, 2026, 5)

            assertThat(result).hasSize(1)
            assertThat(result[0].expectedPromo).isEqualTo(2)
            assertThat(result[0].actualPromo).isEqualTo(1)
            assertThat(result[0].expected).isNull()
            assertThat(result[0].actual).isNull()
        }

        @Test
        @DisplayName("연차 / 대휴 분리")
        fun annualLeaveAndAltHoliday() {
            val rows = listOf(
                makeRow(day = 1, workingType = WorkingType.ANNUAL_LEAVE, cat1 = null, hasCommute = false),
                makeRow(day = 1, workingType = WorkingType.ALT_HOLIDAY, cat1 = null, hasCommute = false),
                makeRow(day = 2, workingType = WorkingType.ALT_HOLIDAY, cat1 = null, hasCommute = false),
            )

            val result = service.aggregateSummary(rows, 2026, 5).associateBy { it.day }

            assertThat(result[1]?.holiday).isEqualTo(1)
            assertThat(result[1]?.subHoliday).isEqualTo(1)
            assertThat(result[2]?.subHoliday).isEqualTo(1)
            assertThat(result[2]?.holiday).isNull()
        }

        @Test
        @DisplayName("빈 입력 → 빈 결과")
        fun emptyInput() {
            assertThat(service.aggregateSummary(emptyList(), 2026, 5)).isEmpty()
        }

        @Test
        @DisplayName("workingDate null 행 → 무시")
        fun nullWorkingDate() {
            val rows = listOf(
                TeamMemberSchedule(workingDate = null, workingType = WorkingType.WORK)
            )
            assertThat(service.aggregateSummary(rows, 2026, 5)).isEmpty()
        }

        private fun makeRow(
            day: Int,
            workingType: WorkingType,
            cat1: WorkingCategory1?,
            hasCommute: Boolean,
        ): TeamMemberSchedule {
            return TeamMemberSchedule(
                workingDate = LocalDate.of(2026, 5, day),
                workingType = workingType,
                workingCategory1 = cat1,
                attendanceLog = if (hasCommute) AttendanceLog() else null,
            )
        }
    }

    @Test
    @DisplayName("HARDCODED_LEADER_SABUNS — SF 1:1 (FullCalendarComponentController.cls:64-65, 75-76, 126)")
    fun hardcodedLeaderSabunsContents() {
        assertThat(FemaleEmployeeScheduleQueryService.HARDCODED_LEADER_SABUNS)
            .containsExactlyInAnyOrder("19951029", "20001013", "20060052", "20050308")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun stubEmployee(id: Long, costCenterCode: String?): Employee =
        mockk(relaxed = true)
}
