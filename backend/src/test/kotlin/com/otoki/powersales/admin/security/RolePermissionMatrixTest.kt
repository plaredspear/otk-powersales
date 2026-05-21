package com.otoki.powersales.admin.security

import com.otoki.powersales.auth.entity.UserRoleEnum
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("RolePermissionMatrix SoT 검증")
class RolePermissionMatrixTest {

    @Test
    @DisplayName("모든 AdminPermission 은 최소 1개 운영 역할에 부여되어야 한다")
    fun everyPermissionAssignedToAtLeastOneRole() {
        val granted = RolePermissionMatrix.MATRIX.values.flatten().toSet()
        val missing = AdminPermission.entries.toSet() - granted
        assertThat(missing).`as`("어디에도 부여되지 않은 권한").isEmpty()
    }

    @Test
    @DisplayName("매트릭스는 운영 역할 7종을 모두 포함한다 (WOMAN/ACCOUNT_VIEW_ALL/UNKNOWN 제외)")
    fun matrixCoversAllOperationalRoles() {
        val expected = setOf(
            UserRoleEnum.SYSTEM_ADMIN,
            UserRoleEnum.SALES_SUPPORT,
            UserRoleEnum.LEADER,
            UserRoleEnum.BRANCH_MANAGER,
            UserRoleEnum.SALES_MANAGER,
            UserRoleEnum.BUSINESS_MANAGER,
            UserRoleEnum.HEADQUARTERS_MANAGER,
        )
        assertThat(RolePermissionMatrix.MATRIX.keys).isEqualTo(expected)
    }

    @Test
    @DisplayName("SYSTEM_ADMIN 은 모든 AdminPermission 을 보유한다")
    fun systemAdminHoldsAllPermissions() {
        val systemAdminPerms = RolePermissionMatrix.MATRIX[UserRoleEnum.SYSTEM_ADMIN].orEmpty()
        assertThat(systemAdminPerms).isEqualTo(AdminPermission.entries.toSet())
    }

    @Test
    @DisplayName("asPairs() 는 매트릭스의 (role, permission) 합계와 일치한다")
    fun asPairsMatchesMatrixTotal() {
        val totalCount = RolePermissionMatrix.MATRIX.values.sumOf { it.size }
        assertThat(RolePermissionMatrix.asPairs()).hasSize(totalCount)
    }
}
