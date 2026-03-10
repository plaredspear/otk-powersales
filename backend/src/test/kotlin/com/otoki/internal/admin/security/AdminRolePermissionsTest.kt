package com.otoki.internal.admin.security

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminRolePermissions 테스트")
class AdminRolePermissionsTest {

    @Nested
    @DisplayName("getPermissions")
    inner class GetPermissions {

        @Test
        @DisplayName("조장 역할 - 모든 권한 반환")
        fun jojangReturnsAllPermissions() {
            val permissions = AdminRolePermissions.getPermissions("조장")

            assertThat(permissions).containsExactlyInAnyOrder(
                AdminPermission.DASHBOARD_READ,
                AdminPermission.EMPLOYEE_READ,
                AdminPermission.ACCOUNT_READ,
                AdminPermission.PROMOTION_READ,
                AdminPermission.PROMOTION_WRITE
            )
        }

        @Test
        @DisplayName("미등록 역할 - 빈 권한 반환")
        fun unknownRoleReturnsEmptySet() {
            val permissions = AdminRolePermissions.getPermissions("미등록역할")

            assertThat(permissions).isEmpty()
        }

        @Test
        @DisplayName("null 역할 - 빈 권한 반환")
        fun nullRoleReturnsEmptySet() {
            val permissions = AdminRolePermissions.getPermissions(null)

            assertThat(permissions).isEmpty()
        }
    }
}
