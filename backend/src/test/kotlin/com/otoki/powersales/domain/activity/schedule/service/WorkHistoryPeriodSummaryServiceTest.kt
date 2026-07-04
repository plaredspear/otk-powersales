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
import org.junit.jupiter.api.BeforeEach
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
        id: Long = 100L,
    ): Employee {
        val emp = Employee(id = id, employeeCode = code, name = name)
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
    @DisplayName("거래처별 집계 (getAccountSummary)")
    inner class AccountSummary {

        // B그룹(통합일정 지표) 모수 조회 — 기본은 빈 리스트. B그룹 검증 테스트만 개별 override.
        @BeforeEach
        fun setUpBMetricsPopulation() {
            every {
                repository.findAttendedSchedulesByEmployeeAndMonth(any(), any(), any())
            } returns emptyList()
        }

        private fun namedAccount(id: Long, name: String): Account {
            val acc = Account(id = id, externalKey = "B$id")
            acc.name = name
            acc.branchName = "원주1지점"
            acc.accountStatusCode = "02"
            acc.accountType = "대형마트(3대)"
            acc.abcTypeCode = "6111"
            acc.abcType = "이마트"
            return acc
        }

        @Test
        @DisplayName("같은 거래처의 여러 일정을 1행으로 합산한다")
        fun aggregatesByAccount() {
            val emp = employee()
            val martA = namedAccount(1, "이마트 원주점")
            val martB = namedAccount(2, "홈플러스 원주점")
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns listOf(
                schedule(emp, martA, LocalDate.of(2026, 6, 1), WorkingType.WORK, WorkingCategory1.DISPLAY),
                schedule(emp, martA, LocalDate.of(2026, 6, 2), WorkingType.WORK, WorkingCategory1.EVENT),
                schedule(emp, martB, LocalDate.of(2026, 6, 3), WorkingType.WORK, WorkingCategory1.DISPLAY),
            )

            val res = service.getAccountSummary(allScope, "20230016", "2026-06", "2026-06")

            assertThat(res.employeeCode).isEqualTo("20230016")
            assertThat(res.employeeName).isEqualTo("홍길동")
            assertThat(res.items).hasSize(2)
            assertThat(res.totalCount).isEqualTo(2)
            val itemA = res.items.first { it.accountName == "이마트 원주점" }
            assertThat(itemA.totalWorkingDays).isEqualTo(2)
            assertThat(itemA.displayDays).isEqualTo(1)
            assertThat(itemA.eventDays).isEqualTo(1)
            assertThat(itemA.workDays).isEqualTo(2)
            // 통합일정 대비 추가된 거래처 속성 컬럼 (지점명 / 유통형태 / 거래처유형)
            assertThat(itemA.accountBranchName).isEqualTo("원주1지점")
            assertThat(itemA.distributionChannelLabel).isEqualTo("02 대형마트(3대)")
            assertThat(itemA.abcTypeLabel).isEqualTo("6111 이마트")
        }

        @Test
        @DisplayName("거래처 미연결 행(연차 등)은 accountName=null 1행으로 묶어 맨 뒤에 둔다")
        fun groupsNullAccountLast() {
            val emp = employee()
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns listOf(
                schedule(emp, null, LocalDate.of(2026, 6, 1), WorkingType.ANNUAL_LEAVE, null),
                schedule(emp, null, LocalDate.of(2026, 6, 2), WorkingType.ALT_HOLIDAY, null),
                schedule(emp, namedAccount(1, "이마트 원주점"), LocalDate.of(2026, 6, 3)),
            )

            val res = service.getAccountSummary(allScope, "20230016", "2026-06", "2026-06")

            assertThat(res.items).hasSize(2)
            assertThat(res.items.last().accountName).isNull()
            assertThat(res.items.last().totalWorkingDays).isEqualTo(2)
            assertThat(res.items.last().annualLeaveDays).isEqualTo(1)
            assertThat(res.items.last().altHolidayDays).isEqualTo(1)
        }

        @Test
        @DisplayName("총 근무일수 내림차순 → 거래처명 오름차순으로 정렬한다")
        fun sortsByWorkingDaysDescThenName() {
            val emp = employee()
            val one = namedAccount(1, "하나로마트")
            val two = namedAccount(2, "농협마트")
            val three = namedAccount(3, "이마트")
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns listOf(
                schedule(emp, one, LocalDate.of(2026, 6, 1)),
                schedule(emp, two, LocalDate.of(2026, 6, 2)),
                schedule(emp, three, LocalDate.of(2026, 6, 3)),
                schedule(emp, three, LocalDate.of(2026, 6, 4)),
            )

            val res = service.getAccountSummary(allScope, "20230016", "2026-06", "2026-06")

            assertThat(res.items.map { it.accountName })
                .containsExactly("이마트", "농협마트", "하나로마트")
        }

        @Test
        @DisplayName("사번 공백을 trim 해 repository 에 전달한다")
        fun trimsEmployeeCode() {
            val codeSlot = slot<String>()
            every {
                repository.findWorkHistoryForPeriodByEmployee(capture(codeSlot), any(), any(), any())
            } returns emptyList()

            val res = service.getAccountSummary(allScope, "  20230016  ", "2026-05", "2026-06")

            assertThat(codeSlot.captured).isEqualTo("20230016")
            assertThat(res.items).isEmpty()
            assertThat(res.employeeName).isNull()
        }

        @Test
        @DisplayName("사번이 공백뿐이면 InvalidParameterException")
        fun blankEmployeeCodeRejected() {
            assertThatThrownBy { service.getAccountSummary(allScope, "   ", "2026-05", "2026-06") }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("지점 권한이면 권한 스코프 지점을 repository 에 전달한다")
        fun passesBranchScope() {
            val codesSlot = slot<List<String>>()
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), capture(codesSlot))
            } returns emptyList()

            service.getAccountSummary(branchScope("A001", "A002"), "20230016", "2026-05", "2026-05")

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("기간 검증 — 6개월 초과 시 InvalidParameterException")
        fun validatesRange() {
            assertThatThrownBy { service.getAccountSummary(allScope, "20230016", "2026-01", "2026-07") }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        /** costCenterCode 를 세팅한 B그룹 모수용 출근 스케줄. */
        private fun attended(
            emp: Employee,
            acc: Account,
            workingDate: LocalDate,
            costCenter: String = "CC1",
            category1: WorkingCategory1 = WorkingCategory1.DISPLAY,
        ): TeamMemberSchedule = schedule(emp, acc, workingDate, WorkingType.WORK, category1)
            .apply { costCenterCode = costCenter }

        @Test
        @DisplayName("B그룹(통합일정) — 환산근무일수 Σ(1/N)·환산인원·투입횟수를 통합일정 정의대로 산출한다")
        fun computesIntegrationBMetrics() {
            val emp = employee()
            val martA = namedAccount(1, "이마트 원주점")
            val martB = namedAccount(2, "홈플러스 원주점")
            // 표시용 조회 (거래처별 뷰 원천) — 사원 A/B 근무 행.
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns listOf(
                attended(emp, martA, LocalDate.of(2026, 6, 1)),
                attended(emp, martA, LocalDate.of(2026, 6, 2)),
                attended(emp, martB, LocalDate.of(2026, 6, 2)),
            )
            // B그룹 모수 (월 전체·거래처 무관) — 6/1: A만(N=1), 6/2: A+B(N=2). 당월근무일수(CC1) = 2일.
            every {
                repository.findAttendedSchedulesByEmployeeAndMonth(any(), any(), any())
            } returns listOf(
                attended(emp, martA, LocalDate.of(2026, 6, 1)),
                attended(emp, martA, LocalDate.of(2026, 6, 2)),
                attended(emp, martB, LocalDate.of(2026, 6, 2)),
            )

            val res = service.getAccountSummary(allScope, "20230016", "2026-06", "2026-06")

            val a = res.items.first { it.accountName == "이마트 원주점" }
            val b = res.items.first { it.accountName == "홈플러스 원주점" }
            // 환산근무일수: A = 1/1 + 1/2 = 1.5, B = 1/2 = 0.5
            assertThat(a.equivalentWorkingDays).isEqualByComparingTo("1.5000")
            assertThat(b.equivalentWorkingDays).isEqualByComparingTo("0.5000")
            // 투입횟수: 같은 조합의 distinct 근무일 — A = 2(6/1,6/2), B = 1(6/2)
            assertThat(a.totalInputCount).isEqualTo(2)
            assertThat(b.totalInputCount).isEqualTo(1)
            // 단일 월 조회이므로 월별 분해는 비어 있다.
            assertThat(a.monthlyStats).isEmpty()
        }

        @Test
        @DisplayName("B그룹 — 다월 조회 시 월별 분해(환산인원 포함)를 채우고 합산 가능 지표는 기간 합계로 낸다")
        fun fillsMonthlyBreakdownForMultiMonth() {
            val emp = employee()
            val martA = namedAccount(1, "이마트 원주점")
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns listOf(
                attended(emp, martA, LocalDate.of(2026, 5, 1)),
                attended(emp, martA, LocalDate.of(2026, 6, 1)),
            )
            // 5월: A 단독 1일(N=1) → 환산 1.0, 당월근무일수 1 → 환산인원 1.0
            // 6월: A 단독 1일(N=1) → 환산 1.0, 당월근무일수 1 → 환산인원 1.0
            every {
                repository.findAttendedSchedulesByEmployeeAndMonth(any(), any(), any())
            } returns listOf(
                attended(emp, martA, LocalDate.of(2026, 5, 1)),
                attended(emp, martA, LocalDate.of(2026, 6, 1)),
            )

            val res = service.getAccountSummary(allScope, "20230016", "2026-05", "2026-06")

            val a = res.items.first { it.accountName == "이마트 원주점" }
            // 기간 합계: 환산근무일수 1.0 + 1.0 = 2.0, 투입횟수 1 + 1 = 2
            assertThat(a.equivalentWorkingDays).isEqualByComparingTo("2.0000")
            assertThat(a.totalInputCount).isEqualTo(2)
            // 월별 분해 2건 (환산인원은 월별로만)
            assertThat(a.monthlyStats).hasSize(2)
            assertThat(a.monthlyStats.map { it.yearMonth }).containsExactly("2026-05", "2026-06")
            assertThat(a.monthlyStats[0].convertedHeadcount).isEqualByComparingTo("1.0000")
            assertThat(a.monthlyStats[0].workingCategory1).isEqualTo(WorkingCategory1.DISPLAY.displayName)
        }
    }

    @Nested
    @DisplayName("파라미터 검증 (getAccountSummary)")
    inner class Validation {

        @Test
        @DisplayName("년월 형식이 잘못되면 InvalidParameterException")
        fun invalidFormat() {
            assertThatThrownBy { service.getAccountSummary(allScope, "20230016", "2026/05", "2026-06") }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("year 범위 외면 InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy { service.getAccountSummary(allScope, "20230016", "1999-05", "2026-06") }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("시작년월이 종료년월보다 이후면 InvalidParameterException")
        fun fromAfterTo() {
            assertThatThrownBy { service.getAccountSummary(allScope, "20230016", "2026-06", "2026-05") }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("포함 6개월(경계)은 정상 조회된다")
        fun exactlySixMonthsAllowed() {
            every {
                repository.findWorkHistoryForPeriodByEmployee(any(), any(), any(), any())
            } returns emptyList()
            every {
                repository.findAttendedSchedulesByEmployeeAndMonth(any(), any(), any())
            } returns emptyList()

            // 2026-01 ~ 2026-06 = 포함 6개월
            val res = service.getAccountSummary(allScope, "20230016", "2026-01", "2026-06")

            assertThat(res.fromYearMonth).isEqualTo("2026-01")
            assertThat(res.toYearMonth).isEqualTo("2026-06")
        }

        @Test
        @DisplayName("포함 7개월이면 InvalidParameterException (최대 6개월 초과)")
        fun moreThanSixMonthsRejected() {
            // 2026-01 ~ 2026-07 = 포함 7개월
            assertThatThrownBy { service.getAccountSummary(allScope, "20230016", "2026-01", "2026-07") }
                .isInstanceOf(InvalidParameterException::class.java)
        }
    }
}
