package com.otoki.powersales.domain.activity.schedule.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.AdminForbiddenException
import com.otoki.powersales.domain.activity.schedule.service.AdminFemaleEmployeePlacementCheckService
import com.otoki.powersales.domain.activity.schedule.service.InvalidParameterException
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.common.enums.WorkingCategory1
import com.otoki.powersales.platform.common.enums.WorkingCategory5
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.domain.activity.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.domain.activity.schedule.repository.TeamMemberScheduleRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("AdminFemaleEmployeePlacementCheckService 테스트")
class AdminFemaleEmployeePlacementCheckServiceTest {

    private val repository: TeamMemberScheduleRepository = mockk()
    private val service = AdminFemaleEmployeePlacementCheckService(repository)

    private val allScope = DataScope(branchCodes = emptyList(), isAllBranches = true)
    private fun branchScope(vararg codes: String) = DataScope(branchCodes = codes.toList(), isAllBranches = false)

    private fun employee(
        code: String,
        name: String,
        role: String = AppAuthority.WOMAN,
        status: String? = "재직",
        orgName: String? = "영업1팀",
        birthDate: String? = "1985-01-01",
        startDate: LocalDate? = LocalDate.of(2019, 3, 2),
    ): Employee = Employee(
        employeeCode = code,
        name = name,
        role = role,
        status = status,
        orgName = orgName,
        birthDate = birthDate,
        startDate = startDate,
        jikwee = "주임",
    )

    private fun account(name: String, branchCode: String, branchName: String): Account {
        val acc = Account(id = 1, externalKey = "SAP$branchCode")
        acc.name = name
        acc.branchCode = branchCode
        acc.branchName = branchName
        acc.accountType = "대형마트(3대)"
        return acc
    }

    private fun schedule(
        emp: Employee,
        acc: Account?,
        workingDate: LocalDate = LocalDate.of(2026, 5, 12),
        cat1: WorkingCategory1? = WorkingCategory1.DISPLAY,
        cat5: WorkingCategory5? = WorkingCategory5.REGULAR,
        secondWorkType: String? = "보조",
    ): TeamMemberSchedule {
        val s = TeamMemberSchedule(
            workingDate = workingDate,
            workingCategory1 = cat1,
            workingCategory5 = cat5,
            secondWorkType = secondWorkType,
        )
        s.employee = emp
        s.account = acc
        return s
    }

    @Nested
    @DisplayName("조회")
    inner class GetPlacementCheck {

        @Test
        @DisplayName("여사원·조장 일정을 행 단위로 반환하고 21컬럼을 매핑한다")
        fun mapsRows() {
            val woman = employee("100234", "홍길동", role = AppAuthority.WOMAN)
            val acc = account("○○마트 강남점", "B0123", "강남지점")
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(woman, acc))

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(res.year).isEqualTo(2026)
            assertThat(res.month).isEqualTo(5)
            assertThat(res.items).hasSize(1)
            val item = res.items[0]
            assertThat(item.employeeCode).isEqualTo("100234")
            assertThat(item.name).isEqualTo("홍길동")
            assertThat(item.orgName).isEqualTo("영업1팀")
            assertThat(item.employmentStatus).isEqualTo("재직")
            assertThat(item.accountType).isEqualTo("대형마트(3대)")
            assertThat(item.accountName).isEqualTo("○○마트 강남점")
            assertThat(item.accountSapCode).isEqualTo("SAPB0123")
            assertThat(item.accountBranchName).isEqualTo("강남지점")
            assertThat(item.workingCategory1).isEqualTo("진열")
            assertThat(item.workingCategory5).isEqualTo("상시")
            assertThat(item.secondWorkType).isEqualTo("보조")
            assertThat(item.workingDate).isEqualTo("2026-05-12")
        }

        @Test
        @DisplayName("repository 에 여사원/조장 role 과 월 1일~말일 범위를 전달한다")
        fun passesRolesAndMonthRange() {
            val fromSlot = slot<LocalDate>()
            val toSlot = slot<LocalDate>()
            val rolesSlot = slot<List<String>>()
            every {
                repository.findPlacementCheck(capture(fromSlot), capture(toSlot), capture(rolesSlot), any())
            } returns emptyList()

            service.getPlacementCheck(allScope, 2026, 2, emptyList())

            assertThat(fromSlot.captured).isEqualTo(LocalDate.of(2026, 2, 1))
            assertThat(toSlot.captured).isEqualTo(LocalDate.of(2026, 2, 28))
            assertThat(rolesSlot.captured).containsExactlyInAnyOrder(AppAuthority.WOMAN, AppAuthority.LEADER)
        }

        @Test
        @DisplayName("퇴직 상태 사원도 employmentStatus=퇴직 으로 포함한다")
        fun includesResigned() {
            val resigned = employee("100999", "이퇴직", status = "퇴직")
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(resigned, account("마트", "B1", "지점")))

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(res.items).hasSize(1)
            assertThat(res.items[0].employmentStatus).isEqualTo("퇴직")
        }

        @Test
        @DisplayName("나이와 근속연수를 조회 월 말일 기준으로 계산한다")
        fun calculatesAgeAndService() {
            val emp = employee("100234", "홍길동", birthDate = "1985-01-01", startDate = LocalDate.of(2019, 3, 2))
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(emp, account("마트", "B1", "지점")))

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            // 2026-05-31 기준: 만 41세, 근속 7년
            assertThat(res.items[0].age).isEqualTo(41)
            assertThat(res.items[0].yearsOfService).isEqualTo(7)
        }

        @Test
        @DisplayName("birthDate 가 null 이면 age 는 null (예외 없음)")
        fun nullBirthDate() {
            val emp = employee("100234", "홍길동", birthDate = null)
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(emp, account("마트", "B1", "지점")))

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(res.items[0].age).isNull()
        }

        @Test
        @DisplayName("birthDate 가 yyyyMMdd 형식이어도 파싱한다")
        fun parsesCompactBirthDate() {
            val emp = employee("100234", "홍길동", birthDate = "19850101")
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(emp, account("마트", "B1", "지점")))

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(res.items[0].age).isEqualTo(41)
        }

        @Test
        @DisplayName("orgName·사번·근무일자 순으로 정렬한다")
        fun sorts() {
            val e1 = employee("200", "B사원", orgName = "영업2팀")
            val e2 = employee("100", "A사원", orgName = "영업1팀")
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns listOf(
                schedule(e1, account("마트", "B1", "지점"), workingDate = LocalDate.of(2026, 5, 20)),
                schedule(e2, account("마트", "B1", "지점"), workingDate = LocalDate.of(2026, 5, 3)),
            )

            val res = service.getPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(res.items.map { it.employeeCode }).containsExactly("100", "200")
        }
    }

    @Nested
    @DisplayName("권한 스코프")
    inner class Scope {

        @Test
        @DisplayName("isAllBranches=false 인데 스코프 외 costCenterCode 만 입력하면 403")
        fun forbiddenWhenNoIntersection() {
            assertThatThrownBy {
                service.getPlacementCheck(branchScope("A001"), 2026, 5, listOf("Z999"))
            }.isInstanceOf(AdminForbiddenException::class.java)
        }

        @Test
        @DisplayName("isAllBranches=false 이고 입력 미지정이면 권한 범위 전체로 조회한다")
        fun usesScopeBranchesWhenEmpty() {
            val codesSlot = slot<List<String>>()
            every { repository.findPlacementCheck(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getPlacementCheck(branchScope("A001", "A002"), 2026, 5, emptyList())

            assertThat(codesSlot.captured).containsExactlyInAnyOrder("A001", "A002")
        }

        @Test
        @DisplayName("교집합만 repository 에 전달한다")
        fun passesIntersection() {
            val codesSlot = slot<List<String>>()
            every { repository.findPlacementCheck(any(), any(), any(), capture(codesSlot)) } returns emptyList()

            service.getPlacementCheck(branchScope("A001", "A002"), 2026, 5, listOf("A001", "Z999"))

            assertThat(codesSlot.captured).containsExactly("A001")
        }
    }

    @Nested
    @DisplayName("파라미터 검증")
    inner class Validation {

        @Test
        @DisplayName("year 범위 외면 InvalidParameterException")
        fun invalidYear() {
            assertThatThrownBy { service.getPlacementCheck(allScope, 1999, 5, emptyList()) }
                .isInstanceOf(InvalidParameterException::class.java)
        }

        @Test
        @DisplayName("month 범위 외면 InvalidParameterException")
        fun invalidMonth() {
            assertThatThrownBy { service.getPlacementCheck(allScope, 2026, 13, emptyList()) }
                .isInstanceOf(InvalidParameterException::class.java)
        }
    }

    @Nested
    @DisplayName("엑셀 export")
    inner class Export {

        @Test
        @DisplayName("21컬럼 헤더의 xlsx 를 생성하고 파일명을 yyyyMM 으로 짓는다")
        fun exportsXlsx() {
            val emp = employee("100234", "홍길동")
            every { repository.findPlacementCheck(any(), any(), any(), any()) } returns
                listOf(schedule(emp, account("마트", "B1", "지점")))

            val result = service.exportPlacementCheck(allScope, 2026, 5, emptyList())

            assertThat(result.filename).isEqualTo("여사원배치점검_202605.xlsx")
            assertThat(result.bytes).isNotEmpty()
        }
    }
}
