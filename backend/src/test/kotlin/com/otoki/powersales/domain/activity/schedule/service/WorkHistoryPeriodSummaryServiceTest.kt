package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingType
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("WorkHistoryPeriodSummaryService 테스트")
class WorkHistoryPeriodSummaryServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = WorkHistoryPeriodSummaryService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(
        code: String = "20230016",
        name: String = "홍길동",
        orgName: String? = "강남지점",
        jikwee: String? = "사원",
    ): Employee {
        val emp = Employee(employeeCode = code, name = name)
        emp.orgName = orgName
        emp.jikwee = jikwee
        return emp
    }

    private fun account(id: Long): Account = Account(id = id, externalKey = "B$id")

    private fun schedule(
        emp: Employee,
        acc: Account? = account(1),
        workingDate: LocalDate = LocalDate.of(2026, 5, 12),
        workingType: WorkingType = WorkingType.WORK,
        workingCategory1: WorkingCategory1? = WorkingCategory1.DISPLAY,
    ): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            name = "schedule",
            workingDate = workingDate,
            workingType = workingType,
            workingCategory1 = workingCategory1,
        )
        s.employee = emp
        s.account = acc
        return s
    }

    @Nested
    @DisplayName("여사원별 집계")
    inner class Aggregate {

        @Test
        @DisplayName("같은 사번의 여러 일정을 1행으로 합산한다")
        fun aggregatesByEmployee() {
            val emp = employee()
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(emp, account(1), LocalDate.of(2026, 5, 1), WorkingType.WORK, WorkingCategory1.DISPLAY),
                schedule(emp, account(1), LocalDate.of(2026, 5, 2), WorkingType.WORK, WorkingCategory1.EVENT),
                schedule(emp, account(2), LocalDate.of(2026, 6, 3), WorkingType.ANNUAL_LEAVE, WorkingCategory1.DISPLAY),
                schedule(emp, account(2), LocalDate.of(2026, 6, 4), WorkingType.ALT_HOLIDAY, null),
            )

            val res = service.getSummary(allScope, "2026-05", "2026-06", emptyList(), null)

            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.orgName).isEqualTo("강남지점")
            assertThat(item.title).isEqualTo("사원")
            assertThat(item.totalWorkingDays).isEqualTo(4)
            assertThat(item.workingAccountCount).isEqualTo(2)
            assertThat(item.displayDays).isEqualTo(2)
            assertThat(item.eventDays).isEqualTo(1)
            assertThat(item.workDays).isEqualTo(2)
            assertThat(item.annualLeaveDays).isEqualTo(1)
            assertThat(item.altHolidayDays).isEqualTo(1)
        }

        @Test
        @DisplayName("서로 다른 사번은 별도 행으로 분리한다")
        fun separatesEmployees() {
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(employee("A1", "갑")),
                schedule(employee("A2", "을")),
                schedule(employee("A2", "을")),
            )

            val res = service.getSummary(allScope, "2026-05", "2026-05", emptyList(), null)

            assertThat(res.items).hasSize(2)
            assertThat(res.totalCount).isEqualTo(2)
            assertThat(res.items.map { it.employeeCode }).containsExactlyInAnyOrder("A1", "A2")
        }

        @Test
        @DisplayName("2개월 이상 조회 시 월별 분해(monthlyBreakdown)를 yyyy-MM 오름차순으로 채운다")
        fun fillsMonthlyBreakdown() {
            val emp = employee()
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(emp, account(1), LocalDate.of(2026, 6, 3), WorkingType.WORK, WorkingCategory1.EVENT),
                schedule(emp, account(1), LocalDate.of(2026, 5, 1), WorkingType.WORK, WorkingCategory1.DISPLAY),
                schedule(emp, account(2), LocalDate.of(2026, 5, 2), WorkingType.ANNUAL_LEAVE, WorkingCategory1.DISPLAY),
            )

            val res = service.getSummary(allScope, "2026-05", "2026-06", emptyList(), null)

            val item = res.items.single()
            assertThat(item.monthlyBreakdown).hasSize(2)
            val may = item.monthlyBreakdown[0]
            assertThat(may.yearMonth).isEqualTo("2026-05")
            assertThat(may.totalWorkingDays).isEqualTo(2)
            assertThat(may.displayDays).isEqualTo(2)
            assertThat(may.annualLeaveDays).isEqualTo(1)
            val jun = item.monthlyBreakdown[1]
            assertThat(jun.yearMonth).isEqualTo("2026-06")
            assertThat(jun.totalWorkingDays).isEqualTo(1)
            assertThat(jun.eventDays).isEqualTo(1)
        }

        @Test
        @DisplayName("단일 월 조회 시 monthlyBreakdown 은 비어 있다")
        fun noBreakdownForSingleMonth() {
            val emp = employee()
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(emp, account(1), LocalDate.of(2026, 5, 1)),
            )

            val res = service.getSummary(allScope, "2026-05", "2026-05", emptyList(), null)

            assertThat(res.items.single().monthlyBreakdown).isEmpty()
        }

        @Test
        @DisplayName("사번이 없는 행은 집계 대상에서 제외한다")
        fun excludesBlankEmployeeCode() {
            val noCode = Employee(employeeCode = null, name = "사번없음")
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(noCode),
            )

            val res = service.getSummary(allScope, "2026-05", "2026-05", emptyList(), null)

            assertThat(res.items).isEmpty()
        }
    }

    @Nested
    @DisplayName("기간 환산 + 파라미터 전달")
    inner class PeriodParams {

        @Test
        @DisplayName("시작년월 1일 ~ 종료년월 말일로 환산해 repository 에 전달한다")
        fun convertsRange() {
            val fromSlot = slot<LocalDate>()
            val toSlot = slot<LocalDate>()
            every {
                repository.findWorkHistoryForPeriod(capture(fromSlot), capture(toSlot), any(), any())
            } returns emptyList()

            service.getSummary(allScope, "2026-02", "2026-04", emptyList(), null)

            assertThat(fromSlot.captured).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(toSlot.captured).isEqualTo(LocalDate.of(2026, 4, 30))
        }

        @Test
        @DisplayName("keyword 공백은 null 로 정규화해 전달한다")
        fun normalizesKeyword() {
            val keywordSlot = slot<String?>()
            every {
                repository.findWorkHistoryForPeriod(any(), any(), any(), captureNullable(keywordSlot))
            } returns emptyList()

            service.getSummary(allScope, "2026-05", "2026-05", emptyList(), "   ")

            assertThat(keywordSlot.captured).isNull()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (costCenterCode)")
    inner class Scope {

        @Test
        @DisplayName("전사 권한 + 선택 없음 → 빈 리스트(무제한) 전달")
        fun allBranchesNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistoryForPeriod(any(), any(), capture(codesSlot), any()) } returns emptyList()

            service.getSummary(allScope, "2026-05", "2026-05", emptyList(), null)

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("전사 권한 + 선택 있음 → 선택 지점 그대로 전달")
        fun allBranchesWithSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistoryForPeriod(any(), any(), capture(codesSlot), any()) } returns emptyList()

            service.getSummary(allScope, "2026-05", "2026-05", listOf("A001"), null)

            assertThat(codesSlot.captured).containsExactly("A001")
        }

        @Test
        @DisplayName("지점 권한 + 선택 있음 → 권한 스코프와 교집합만 전달")
        fun branchScopeIntersection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistoryForPeriod(any(), any(), capture(codesSlot), any()) } returns emptyList()

            service.getSummary(branchScope("A001", "A002"), "2026-05", "2026-05", listOf("A001", "B999"), null)

            assertThat(codesSlot.captured).containsExactly("A001")
        }

        @Test
        @DisplayName("지점 권한 + 선택 없음 → 권한 스코프 전체 전달")
        fun branchScopeNoSelection() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistoryForPeriod(any(), any(), capture(codesSlot), any()) } returns emptyList()

            service.getSummary(branchScope("A001", "A002"), "2026-05", "2026-05", emptyList(), null)

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("지점 권한 + 교집합이 비면 repository 미호출 + 빈 결과")
        fun branchScopeEmptyIntersection() {
            val res = service.getSummary(branchScope("A001"), "2026-05", "2026-05", listOf("B999"), null)

            assertThat(res.items).isEmpty()
            assertThat(res.totalCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("11컬럼 xlsx 생성 + 파일명 기간별근무내역_from_to")
        fun exportsXlsx() {
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns listOf(
                schedule(employee()),
            )

            val result = service.exportSummary(allScope, "2026-05", "2026-06", emptyList(), null)

            assertThat(result.filename).isEqualTo("기간별근무내역_2026-05_2026-06.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class Validation {

        @Test
        @DisplayName("년월 형식이 잘못되면 InvalidParameterException")
        fun invalidFormat() {
            assertThatThrownBy { service.getSummary(allScope, "2026/05", "2026-06", emptyList(), null) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("year 범위 외면 InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy { service.getSummary(allScope, "1999-05", "2026-06", emptyList(), null) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("시작년월이 종료년월보다 이후면 InvalidParameterException")
        fun fromAfterTo() {
            assertThatThrownBy { service.getSummary(allScope, "2026-06", "2026-05", emptyList(), null) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("포함 6개월(경계)은 정상 조회된다")
        fun exactlySixMonthsAllowed() {
            every { repository.findWorkHistoryForPeriod(any(), any(), any(), any()) } returns emptyList()

            // 2026-01 ~ 2026-06 = 포함 6개월
            val res = service.getSummary(allScope, "2026-01", "2026-06", emptyList(), null)

            assertThat(res.fromYearMonth).isEqualTo("2026-01")
            assertThat(res.toYearMonth).isEqualTo("2026-06")
        }

        @Test
        @DisplayName("포함 7개월이면 InvalidParameterException (최대 6개월 초과)")
        fun moreThanSixMonthsRejected() {
            // 2026-01 ~ 2026-07 = 포함 7개월
            assertThatThrownBy { service.getSummary(allScope, "2026-01", "2026-07", emptyList(), null) }
                .isInstanceOf(InvalidParameterException::class.java)
        }
    }
}
