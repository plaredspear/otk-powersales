package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.user.entity.ProfileType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * sfmigration 매핑 표 SoT 정합 테스트.
 *
 * 매핑 표의 모든 enum 값 (Map.value) 이 backend enum 의 실 value 와 일치해야 한다.
 * 발산 시 stage2-B Service 가 native UPDATE 시 enum 컬럼 cast 실패 → 마이그레이션 abort.
 */
@DisplayName("SfMappingTables SoT 정합 테스트")
class SfMappingTablesTest {

    @Nested
    @DisplayName("APP_AUTHORITY_TO_USER_ROLE")
    inner class UserRoleMapping {

        @Test
        @DisplayName("모든 매핑 value 가 UserRole enum 에 존재한다")
        fun allValuesExistInUserRoleEnum() {
            val roleNames = UserRole.entries.map { it.name }.toSet()
            val invalid = APP_AUTHORITY_TO_USER_ROLE.values.filterNot { it in roleNames }
            assertThat(invalid).isEmpty()
        }

        @Test
        @DisplayName("fallback 값 UNKNOWN 이 UserRole enum 에 존재한다")
        fun fallbackExists() {
            assertThat(UserRole.entries.map { it.name }).contains(USER_ROLE_FALLBACK)
        }
    }

    @Nested
    @DisplayName("PPT_KOREAN_TO_ENUM")
    inner class PptMapping {

        @Test
        @DisplayName("모든 매핑 value 가 ProfessionalPromotionTeamType enum 에 존재한다")
        fun allValuesExistInPptEnum() {
            val pptNames = ProfessionalPromotionTeamType.entries.map { it.name }.toSet()
            val invalid = PPT_KOREAN_TO_ENUM.values.filterNot { it in pptNames }
            assertThat(invalid).isEmpty()
        }
    }

    @Nested
    @DisplayName("PERMISSION_SET_TO_PERMISSIONS")
    inner class PermissionSetMapping {

        @Test
        @DisplayName("모든 매핑 value 가 AdminPermission enum 에 존재한다")
        fun allValuesExistInAdminPermissionEnum() {
            val permissionNames = AdminPermission.entries.map { it.name }.toSet()
            val invalid = PERMISSION_SET_TO_PERMISSIONS.values.flatten().filterNot { it in permissionNames }
            assertThat(invalid).isEmpty()
        }
    }

    @Nested
    @DisplayName("PROFILE_NAME_TO_PROFILE_TYPE")
    inner class ProfileTypeMapping {

        @Test
        @DisplayName("모든 매핑 value 가 ProfileType enum 에 존재한다")
        fun allValuesExistInProfileTypeEnum() {
            val profileTypeNames = ProfileType.entries.map { it.name }.toSet()
            val invalid = PROFILE_NAME_TO_PROFILE_TYPE.values.filterNot { it in profileTypeNames }
            assertThat(invalid).isEmpty()
        }

        @Test
        @DisplayName("fallback 값 STAFF 가 ProfileType enum 에 존재한다")
        fun fallbackExists() {
            assertThat(ProfileType.entries.map { it.name }).contains(PROFILE_TYPE_FALLBACK)
        }
    }
}
