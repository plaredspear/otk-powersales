package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.sap.entity.User
import com.otoki.internal.sap.repository.UserRepository
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
import java.util.*

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminDataScopeService 테스트")
class AdminDataScopeServiceTest {

    @Mock
    private lateinit var userRepository: UserRepository

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
            val user = createTestUser(id = userId, appAuthority = "영업부장", costCenterCode = "B001")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "사업부장", costCenterCode = "B002")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "영업본부장", costCenterCode = "B003")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "영업지원실", costCenterCode = "B004")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "조장", costCenterCode = "B100")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "지점장", costCenterCode = "B200")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = "조장", costCenterCode = null)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = null, costCenterCode = "B300")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            val user = createTestUser(id = userId, appAuthority = null, costCenterCode = null)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).isEmpty()
        }

        // ========== 기타 권한 (else 분기) ==========

        @Test
        @DisplayName("알 수 없는 권한 + costCenterCode 있음 - userId 조회 -> isAllBranches=false, branchCodes에 costCenterCode 포함")
        fun resolve_withUnknownAuthorityAndCostCenter_returnsBranchScope() {
            // Given
            val userId = 10L
            val user = createTestUser(id = userId, appAuthority = "기타권한", costCenterCode = "B400")
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

            // When
            val result = adminDataScopeService.resolve(userId)

            // Then
            assertThat(result.isAllBranches).isFalse()
            assertThat(result.branchCodes).containsExactly("B400")
        }

        @Test
        @DisplayName("알 수 없는 권한 + costCenterCode null - userId 조회 -> isAllBranches=false, branchCodes 비어있음")
        fun resolve_withUnknownAuthorityAndNullCostCenter_returnsEmptyBranchCodes() {
            // Given
            val userId = 11L
            val user = createTestUser(id = userId, appAuthority = "기타권한", costCenterCode = null)
            whenever(userRepository.findById(userId)).thenReturn(Optional.of(user))

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
            whenever(userRepository.findById(userId)).thenReturn(Optional.empty())

            // When & Then
            assertThatThrownBy { adminDataScopeService.resolve(userId) }
                .isInstanceOf(IllegalStateException::class.java)
                .hasMessageContaining("사용자를 찾을 수 없습니다")
                .hasMessageContaining(userId.toString())
        }
    }

    // ========== Helper Methods ==========

    private fun createTestUser(
        id: Long = 1L,
        employeeId: String = "12345678",
        name: String = "홍길동",
        appAuthority: String? = null,
        costCenterCode: String? = null
    ): User {
        return User(
            id = id,
            employeeId = employeeId,
            name = name,
            appAuthority = appAuthority,
            costCenterCode = costCenterCode
        )
    }
}
