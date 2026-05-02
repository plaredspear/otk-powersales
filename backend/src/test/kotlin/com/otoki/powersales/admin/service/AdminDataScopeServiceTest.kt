package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.repository.EmployeeRepository
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminDataScopeService 테스트")
class AdminDataScopeServiceTest {

    @Mock
    private lateinit var employeeRepository: EmployeeRepository

    @InjectMocks
    private lateinit var adminDataScopeService: AdminDataScopeService

    @Nested
    @DisplayName("resolve")
    inner class Resolve {

        // ========== 전체 지점 권한 (ALL_BRANCHES_AUTHORITIES) ==========

        @Test
        @DisplayName("영업부장 - userId 조회 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesDirector_returnsAllBranches() {
            // Given
            val userId = 1L
            val employee = createTestEmployee(id = userId, role = UserRole.SALES_MANAGER, costCenterCode = "B001")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("사업부장 - userId 조회 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withDivisionDirector_returnsAllBranches() {
            // Given
            val userId = 2L
            val employee = createTestEmployee(id = userId, role = UserRole.BUSINESS_MANAGER, costCenterCode = "B002")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("영업본부장 - userId 조회 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesHeadquartersDirector_returnsAllBranches() {
            // Given
            val userId = 3L
            val employee = createTestEmployee(id = userId, role = UserRole.HEADQUARTERS_MANAGER, costCenterCode = "B003")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("영업지원실 - userId 조회 -> isAllBranches=true, branchCodes 비어있음")
        fun resolve_withSalesSupportOffice_returnsAllBranches() {
            // Given
            val userId = 4L
            val employee = createTestEmployee(id = userId, role = UserRole.SALES_SUPPORT, costCenterCode = "B004")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isTrue()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 지점 한정 권한 (BRANCH_ONLY_AUTHORITIES) ==========

        @Test
        @DisplayName("조장 + costCenterCode 있음 - userId 조회 -> isAllBranches=false, branchCodes에 costCenterCode 포함")
        fun resolve_withLeaderAndCostCenter_returnsBranchScope() {
            // Given
            val userId = 5L
            val employee = createTestEmployee(id = userId, role = UserRole.LEADER, costCenterCode = "B100")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B100")
        }

        @Test
        @DisplayName("지점장 + costCenterCode 있음 - userId 조회 -> isAllBranches=false, branchCodes에 costCenterCode 포함")
        fun resolve_withBranchManagerAndCostCenter_returnsBranchScope() {
            // Given
            val userId = 6L
            val employee = createTestEmployee(id = userId, role = UserRole.BRANCH_MANAGER, costCenterCode = "B200")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B200")
        }

        @Test
        @DisplayName("조장 + costCenterCode null - userId 조회 -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withLeaderAndNullCostCenter_returnsEmptyBranchCodes() {
            // Given
            val userId = 7L
            val employee = createTestEmployee(id = userId, role = UserRole.LEADER, costCenterCode = null)
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== appAuthority가 null인 경우 ==========

        @Test
        @DisplayName("appAuthority null + costCenterCode 있음 - userId 조회 -> isAllBranches=false, branchCodes에 costCenterCode 포함")
        fun resolve_withNullAuthorityAndCostCenter_returnsBranchScope() {
            // Given
            val userId = 8L
            val employee = createTestEmployee(id = userId, role = null, costCenterCode = "B300")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B300")
        }

        @Test
        @DisplayName("appAuthority null + costCenterCode null - userId 조회 -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withNullAuthorityAndNullCostCenter_returnsEmptyBranchCodes() {
            // Given
            val userId = 9L
            val employee = createTestEmployee(id = userId, role = null, costCenterCode = null)
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 기타 권한 (else 분기) ==========

        @Test
        @DisplayName("UNKNOWN 권한 - 빈 데이터 스코프 반환 (Spec #573)")
        fun resolve_withUnknownAuthorityAndCostCenter_returnsEmpty() {
            // Given
            val userId = 10L
            val employee = createTestEmployee(id = userId, role = UserRole.UNKNOWN, costCenterCode = "B400")
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("알 수 없는 권한 + costCenterCode null - userId 조회 -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withUnknownAuthorityAndNullCostCenter_returnsEmptyBranchCodes() {
            // Given
            val userId = 11L
            val employee = createTestEmployee(id = userId, role = UserRole.UNKNOWN, costCenterCode = null)
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(employee)

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 사용자 미존재 ==========

        @Test
        @DisplayName("존재하지 않는 userId - 조회 실패 -> IllegalStateException 발생")
        fun resolve_withNonExistentUser_throwsException() {
            // Given
            val userId = 999L
            whenever(employeeRepository.findWithEmployeeInfoById(userId)).thenReturn(null)

            // When & Then
            assertThatThrownBy { adminDataScopeService.resolve(userId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("사용자를 찾을 수 없습니다")
                .hasMessageContaining(userId.toString())
        }
    }

    // ========== Helper Methods ==========

    private fun createTestEmployee(
        id: Long = 1L,
        employeeCode: String = "12345678",
        name: String = "홍길동",
        role: UserRole? = null,
        costCenterCode: String? = null
    ): Employee {
        return Employee(
            id = id,
            employeeCode = employeeCode,
            name = name,
            role = role,
            costCenterCode = costCenterCode
        )
    }
}
