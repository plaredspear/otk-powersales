package com.otoki.powersales.auth.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #780 — Profile ↔ Salesforce `Profile` 매핑 검증.
 *
 * SF describe 실측 573 필드 중 8 필드만 보존. read-only audit lookup.
 * 단일 권위: Salesforce SObject `Profile`.
 */
@DisplayName("Profile SF 어노테이션 검증 (Spec #780)")
class ProfileSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject + 매핑 키셋")
    inner class ClassAndMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Profile::class.java)

        @Test
        @DisplayName("@SFObject 값은 'Profile'")
        fun sfObjectValue() {
            val annotation = Profile::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Profile")
        }

        @Test
        @DisplayName("매핑 키 = SF 핵심 메타 5 + BaseEntity 2 = 7")
        fun mappingKeySize() {
            // Name / UserType / Description / CreatedById / LastModifiedById (5) + BaseEntity (CreatedDate / LastModifiedDate)
            assertThat(mapping).hasSize(7)
        }

        @Test
        @DisplayName("SF 필드 → JPA 컬럼 매핑")
        fun fieldMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["UserType"]).isEqualTo("user_type")
            assertThat(mapping["Description"]).isEqualTo("description")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
            // BaseEntity
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC2 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Profile::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 profile_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Profile::class.java)
            assertThat(mapping.values).doesNotContain("profile_id")
        }
    }

    @Nested
    @DisplayName("AC3 — Permissions* boolean 521+개 미보존 (운영 무관)")
    inner class PermissionsBooleansExcluded {

        @Test
        @DisplayName("Permissions* boolean 컬럼 미정의")
        fun permissionsExcluded() {
            val mapping = SFSchemaUtils.getSFMapping(Profile::class.java)
            // SF Profile 의 PermissionsEmailSingle / PermissionsManageUsers 등은 본 entity 에 미보존
            assertThat(mapping.keys.filter { it.startsWith("Permissions") }).isEmpty()
        }
    }
}
