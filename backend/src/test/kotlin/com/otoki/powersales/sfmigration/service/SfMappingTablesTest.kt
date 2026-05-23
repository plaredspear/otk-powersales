package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.admin.security.AdminPermission
import com.otoki.powersales.auth.converter.UserRoleConverter
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.promotion.entity.converter.ProfessionalPromotionTeamTypeConverter
import com.otoki.powersales.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.user.entity.converter.ProfileTypeConverter
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * sfmigration 매핑 표 SoT 정합 테스트.
 *
 * 매핑 표의 모든 결과값 (Map.value) 이 DB column 의 JPA AttributeConverter 가 인식하는
 * 형식과 일치해야 한다. 발산 시 Stage 2-B picklist substep 실행 후 JPA read 단계에서
 * Converter 가 null/fallback 반환 → 모든 row 데이터 손실.
 *
 * 검증 차원: 매핑 결과값을 Converter.convertToEntityAttribute() 에 통과시켜 non-null + 정확값 반환 확인.
 */
@DisplayName("SfMappingTables SoT 정합 테스트")
class SfMappingTablesTest {

    @Nested
    @DisplayName("APP_AUTHORITY_TO_USER_ROLE — employee.role / UserRoleConverter")
    inner class UserRoleMapping {

        private val converter = UserRoleConverter()

        @Test
        @DisplayName("모든 매핑 결과값이 UserRoleConverter 가 인식하는 enum.name (영문) 형식")
        fun allValuesConvertibleByConverter() {
            val invalid = APP_AUTHORITY_TO_USER_ROLE.values.filter { value ->
                converter.convertToEntityAttribute(value) == null ||
                    converter.convertToEntityAttribute(value) == UserRoleEnum.UNKNOWN
            }
            assertThat(invalid)
                .withFailMessage("APP_AUTHORITY_TO_USER_ROLE 의 다음 결과값이 UserRoleConverter 인식 실패: %s", invalid)
                .isEmpty()
        }

        @Test
        @DisplayName("fallback 값 UNKNOWN 이 UserRole enum 에 존재한다")
        fun fallbackExists() {
            assertThat(UserRoleEnum.entries.map { it.name }).contains(USER_ROLE_FALLBACK)
        }
    }

    @Nested
    @DisplayName("PPT_KOREAN_TO_ENUM — employee.professional_promotion_team / ProfessionalPromotionTeamTypeConverter")
    inner class PptMapping {

        private val converter = ProfessionalPromotionTeamTypeConverter()

        @Test
        @DisplayName("모든 매핑 결과값이 Converter 가 인식하는 displayName (한글) 형식")
        fun allValuesConvertibleByConverter() {
            val invalid = PPT_KOREAN_TO_ENUM.values.filter { value ->
                converter.convertToEntityAttribute(value) == null
            }
            assertThat(invalid)
                .withFailMessage(
                    "PPT_KOREAN_TO_ENUM 의 다음 결과값이 ProfessionalPromotionTeamTypeConverter 인식 실패: %s",
                    invalid,
                )
                .isEmpty()
        }
    }

    @Nested
    @DisplayName("PERMISSION_SET_TO_PERMISSIONS")
    inner class PermissionSetMapping {

        @Test
        @DisplayName("모든 매핑 결과값이 AdminPermission enum 에 존재한다")
        fun allValuesExistInAdminPermissionEnum() {
            val permissionNames = AdminPermission.entries.map { it.name }.toSet()
            val invalid = PERMISSION_SET_TO_PERMISSIONS.values.flatten().filterNot { it in permissionNames }
            assertThat(invalid).isEmpty()
        }
    }

    @Nested
    @DisplayName("PROFILE_NAME_TO_PROFILE_TYPE — user.profile_type / ProfileTypeConverter")
    inner class ProfileTypeMapping {

        private val converter = ProfileTypeConverter()

        @Test
        @DisplayName("모든 매핑 결과값이 ProfileTypeConverter 가 인식하는 ProfileType.value 형식 (V169 SF raw 값)")
        fun allValuesConvertibleByConverter() {
            val invalid = PROFILE_NAME_TO_PROFILE_TYPE.values.toSet().filter { value ->
                val converted = converter.convertToEntityAttribute(value)
                // ProfileType.fromValue 는 STAFF fallback 을 반환할 수 있어 정확값 매칭 확인.
                converted == null || converted.value != value
            }
            assertThat(invalid)
                .withFailMessage(
                    "PROFILE_NAME_TO_PROFILE_TYPE 의 다음 결과값이 ProfileTypeConverter 정확값 매칭 실패: %s",
                    invalid,
                )
                .isEmpty()
        }

        @Test
        @DisplayName("fallback 값이 ProfileTypeConverter 가 인식하는 ProfileType.value 형식")
        fun fallbackConvertibleByConverter() {
            val converted = converter.convertToEntityAttribute(PROFILE_TYPE_FALLBACK)
            assertThat(converted)
                .withFailMessage("PROFILE_TYPE_FALLBACK '%s' 가 ProfileTypeConverter 인식 실패", PROFILE_TYPE_FALLBACK)
                .isNotNull
            assertThat(converted!!.value).isEqualTo(PROFILE_TYPE_FALLBACK)
        }
    }
}
