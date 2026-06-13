package com.otoki.powersales.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.entity.AccountType
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingType
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AdminFemaleEmployeeWorkHistoryService 테스트")
class AdminFemaleEmployeeWorkHistoryServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = AdminFemaleEmployeeWorkHistoryService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(
        code: String = "20230016",
        name: String = "홍길동",
        birthDate: String? = "1985-01-01",
    ): Employee = Employee(employeeCode = code, name = name, birthDate = birthDate)

    private fun account(): Account {
        val acc = Account(id = 1, externalKey = "B0123")
        acc.name = "○○마트 강남점"
        acc.branchCode = "B0123"
        acc.branchName = "강남지점"
        acc.accountType = AccountType.DISCOUNT_STORE
        return acc
    }

    private fun schedule(
        emp: Employee,
        acc: Account? = account(),
        workingDate: LocalDate = LocalDate.of(2026, 5, 12),
    ): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            name = "2026-05-12 진열",
            workingDate = workingDate,
            workingType = WorkingType.WORK,
            workingCategory1 = WorkingCategory1.DISPLAY,
            secondWorkType = "보조",
        )
        s.employee = emp
        s.account = acc
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetWorkHistory {

        @Test
        @DisplayName("사번의 월간 근무내역을 15컬럼으로 매핑한다")
        fun mapsRows() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee()))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5)

            assertThat(res.employeeCode).isEqualTo("20230016")
            assertThat(res.year).isEqualTo(2026)
            assertThat(res.month).isEqualTo(5)
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.scheduleName).isEqualTo("2026-05-12 진열")
            assertThat(item.name).isEqualTo("홍길동")
            assertThat(item.employeeCode).isEqualTo("20230016")
            assertThat(item.workingDate).isEqualTo("2026-05-12")
            assertThat(item.accountName).isEqualTo("○○마트 강남점")
            assertThat(item.accountBranchCode).isEqualTo("B0123")
            assertThat(item.accountBranchName).isEqualTo("강남지점")
            assertThat(item.workingType).isEqualTo("근무")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.secondWorkType).isEqualTo("보조")
        }

        @Test
        @DisplayName("repository 에 사번·월 1일~말일·trim 된 사번을 전달한다")
        fun passesParams() {
            val codeSlot = slot<String>()
            val fromSlot = slot<LocalDate>()
            val toSlot = slot<LocalDate>()
            every {
                repository.findWorkHistory(capture(codeSlot), capture(fromSlot), capture(toSlot), any())
            } returns emptyList()

            service.getWorkHistory(allScope, " 20230016 ", 2026, 2)

            assertThat(codeSlot.captured).isEqualTo("20230016")
            assertThat(fromSlot.captured).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(toSlot.captured).isEqualTo(LocalDate.of(2026, 2, 28))
        }

        @Test
        @DisplayName("일정이 없으면 빈 items (예외 없음)")
        fun emptyResult() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns emptyList()

            val res = service.getWorkHistory(allScope, "99999999", 2026, 5)

            assertThat(res.items).isEmpty()
        }

        @Test
        @DisplayName("나이를 조회 월 말일 기준으로 계산한다")
        fun calculatesAge() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee(birthDate = "1985-01-01")))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5)

            assertThat(res.items[0].age).isEqualTo(41)
        }

        @Test
        @DisplayName("birthDate 가 null 이면 age 는 null")
        fun nullBirthDate() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee(birthDate = null)))

            val res = service.getWorkHistory(allScope, "20230016", 2026, 5)

            assertThat(res.items[0].age).isNull()
        }
    }

    @Nested
    @DisplayName("지점 스코프 (costCenterCode)")
    inner class Scope {

        @Test
        @DisplayName("isAllBranches=true 면 branchCodes 빈 리스트(무제한) 전달")
        fun allBranchesUnrestricted() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(allScope, "20230016", 2026, 5)

            assertThat(codesSlot.captured).isEmpty()
        }

        @Test
        @DisplayName("isAllBranches=false 면 scope.branchCodes(costCenterCode) 전달")
        fun branchScopeRestricted() {
            val codesSlot = slot<List<String>>()
            every { repository.findWorkHistory(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getWorkHistory(branchScope("A001", "A002"), "20230016", 2026, 5)

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class Validation {

        @Test
        @DisplayName("employeeCode 공백이면 InvalidParameterException")
        fun blankEmployeeCode() {
            assertThatThrownBy { service.getWorkHistory(allScope, "  ", 2026, 5) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("year 범위 외면 InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy { service.getWorkHistory(allScope, "20230016", 1999, 5) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("month 범위 외면 InvalidParameterException")
        fun invalidMonth() {
            assertThatThrownBy { service.getWorkHistory(allScope, "20230016", 2026, 13) }
                .isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("15컬럼 xlsx 생성 + 파일명 사번_yyyyMM")
        fun exportsXlsx() {
            every { repository.findWorkHistory(any(), any(), any(), any()) } returns
                listOf(schedule(employee()))

            val result = service.exportWorkHistory(allScope, "20230016", 2026, 5)

            assertThat(result.filename).isEqualTo("여사원근무내역_20230016_202605.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
