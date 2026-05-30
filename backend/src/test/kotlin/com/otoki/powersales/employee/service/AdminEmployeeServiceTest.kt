package com.otoki.powersales.employee.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.exception.EmployeeNotFoundException
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("AdminEmployeeService 테스트")
class AdminEmployeeServiceTest {

    private val employeeRepository: EmployeeRepository = mockk()

    private val adminEmployeeService = AdminEmployeeService(
        employeeRepository,
    )

    @Nested
    @DisplayName("getEmployees - 사원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 사원 반환")
        fun allBranches_noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", name = "홍길동"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, null, null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].employeeCode).isEqualTo("10000001")
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("전체 권한 + 지점 필터 -> 지정 지점만 조회")
        fun allBranches_withCostCenterCode() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, listOf("A001"), null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("SF 이진 모델 - 지점 권한 사용자도 필터 없이 조회 시 전사 반환 (게이트 통과=전사)")
        fun branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val employees = listOf(
                createEmployee(employeeCode = "10000001", costCenterCode = "A001"),
                createEmployee(employeeCode = "10000002", costCenterCode = "B002"),
            )
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 2L)
            // SF 이진 모델 — 지점 보안축 미적용. costCenterCode 미지정 → 전사(branchFilter=null).
            every { employeeRepository.findEmployees(null, null, null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(2)
        }

        @Test
        @DisplayName("SF 이진 모델 - costCenterCode 는 순수 표시 필터 (권한 외 지점도 차단 안 함)")
        fun branchOnly_displayFilterOnly() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000002", costCenterCode = "B002"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // scope.branchCodes=["A001"] 이지만 SF 이진 모델이라 표시 필터(B002)를 그대로 전달
            every { employeeRepository.findEmployees(null, listOf("B002"), null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "B002", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("B002")
        }

        @Test
        @DisplayName("SF 이진 모델 - branchCodes 비어있어도 게이트 통과자는 전사 반환")
        fun branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            // NoAccess 개념 소멸 — 게이트 통과 = 전사(branchFilter=null)
            every { employeeRepository.findEmployees(null, null, null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("상태 필터 적용 -> 해당 상태 사원만 반환")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", status = "재직"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees("재직", null, null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, "재직", null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("키워드 필터 적용 -> 사번/이름 부분 일치")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val employees = listOf(createEmployee(employeeCode = "10000001", name = "홍길동"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, null, "홍", null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, null, "홍", null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + 허용 지점 필터 -> 해당 지점 사원 반환")
        fun branchOnly_allowedBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)

            val employees = listOf(createEmployee(employeeCode = "10000001", costCenterCode = "A001"))
            val page = PageImpl(employees, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { employeeRepository.findEmployees(null, listOf("A001"), null, null, any()) } returns page

            val result = adminEmployeeService.getEmployees(scope, null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }
    }

    @Nested
    @DisplayName("getEmployee - 단건 상세 조회 (UC-04)")
    inner class GetEmployeeTests {

        @Test
        @DisplayName("존재하는 사원 -> 6개 그룹 필드 응답")
        fun success() {
            val employee = createEmployee(id = 42L, employeeCode = "10000099", name = "김여사")
                .apply {
                    jobCode = "판촉직"
                    jikwee = "사원"
                    homePhone = "010-1234-5678"
                }
            every { employeeRepository.findWithEmployeeInfoById(42L) } returns employee

            val result = adminEmployeeService.getEmployee(42L)

            assertThat(result.id).isEqualTo(42L)
            assertThat(result.employeeCode).isEqualTo("10000099")
            assertThat(result.name).isEqualTo("김여사")
            assertThat(result.jobCode).isEqualTo("판촉직")
            assertThat(result.homePhone).isEqualTo("010-1234-5678")
        }

        @Test
        @DisplayName("존재하지 않는 사원 -> EmployeeNotFoundException")
        fun notFound() {
            every { employeeRepository.findWithEmployeeInfoById(999L) } returns null

            assertThatThrownBy { adminEmployeeService.getEmployee(999L) }
                .isInstanceOf(EmployeeNotFoundException::class.java)
                .hasMessageContaining("999")
        }
    }

    private fun createEmployee(
        id: Long = 1L,
        employeeCode: String = "10000001",
        name: String = "테스트",
        status: String? = "재직",
        costCenterCode: String? = "A001",
        role: String? = null
    ): Employee = Employee(
        id = id,
        employeeCode = employeeCode,
        name = name,
        status = status,
        costCenterCode = costCenterCode,
        role = role
    )
}
