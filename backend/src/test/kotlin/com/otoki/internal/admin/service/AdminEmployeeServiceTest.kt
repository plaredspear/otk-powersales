package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminEmployeeService 테스트")
class AdminEmployeeServiceTest {

    @Mock
    private lateinit var dataScopeHolder: DataScopeHolder

    @Mock
    private lateinit var userRepository: UserRepository

    @InjectMocks
    private lateinit var adminEmployeeService: AdminEmployeeService

    @Nested
    @DisplayName("getEmployees - 사원 목록 조회")
    inner class GetEmployeesTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 사원 반환")
        fun allBranches_noFilter() {
            // Given
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", name = "홍길동"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq(null), eq(null), eq(null), eq(null), any())).thenReturn(page)

            // When
            val result = adminEmployeeService.getEmployees(null, null, null, null, 0, 20)

            // Then
            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].employeeId).isEqualTo("10000001")
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("전체 권한 + 지점 필터 -> 지정 지점만 조회")
        fun allBranches_withCostCenterCode() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", costCenterCode = "A001"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq(null), eq(listOf("A001")), eq(null), eq(null), any())).thenReturn(page)

            val result = adminEmployeeService.getEmployees(null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].costCenterCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("지점 권한 - 필터 없이 조회 -> 본인 지점 사원만 반환")
        fun branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", costCenterCode = "A001"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq(null), eq(listOf("A001")), eq(null), eq(null), any())).thenReturn(page)

            val result = adminEmployeeService.getEmployees(null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + 권한 외 지점 필터 -> 빈 결과")
        fun branchOnly_forbiddenBranch() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminEmployeeService.getEmployees(null, "B002", null, null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("지점 권한 + branchCodes 비어있음 -> 빈 결과")
        fun branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminEmployeeService.getEmployees(null, null, null, null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("상태 필터 적용 -> 해당 상태 사원만 반환")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", status = "재직"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq("재직"), eq(null), eq(null), eq(null), any())).thenReturn(page)

            val result = adminEmployeeService.getEmployees("재직", null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("키워드 필터 적용 -> 사번/이름 부분 일치")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", name = "홍길동"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq(null), eq(null), eq("홍"), eq(null), any())).thenReturn(page)

            val result = adminEmployeeService.getEmployees(null, null, "홍", null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + 허용 지점 필터 -> 해당 지점 사원 반환")
        fun branchOnly_allowedBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val users = listOf(createUser(employeeId = "10000001", costCenterCode = "A001"))
            val page = PageImpl(users, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(userRepository.findEmployees(eq(null), eq(listOf("A001")), eq(null), eq(null), any())).thenReturn(page)

            val result = adminEmployeeService.getEmployees(null, "A001", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }
    }

    private fun createUser(
        id: Long = 1L,
        employeeId: String = "10000001",
        name: String = "테스트",
        status: String? = "재직",
        costCenterCode: String? = "A001",
        appAuthority: String? = null
    ): User = User(
        id = id,
        employeeId = employeeId,
        name = name,
        status = status,
        costCenterCode = costCenterCode,
        appAuthority = appAuthority
    )
}
