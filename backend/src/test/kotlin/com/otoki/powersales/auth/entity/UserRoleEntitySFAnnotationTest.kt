package com.otoki.powersales.auth.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #780 — UserRoleEntity ↔ Salesforce `UserRole` 매핑 검증.
 *
 * SF describe 실측 16 필드 중 7 필드만 보존. SF UserRole 은 CreatedDate / CreatedById 자체가 부재 —
 * BaseEntity 미상속 (LastModifiedDate / LastModifiedById 만 audit 컬럼).
 *
 * 클래스명 `UserRoleEntity` — 동일 패키지의 [UserRole] enum 과 충돌 회피.
 */
@DisplayName("UserRoleEntity SF 어노테이션 검증 (Spec #780)")
class UserRoleEntitySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject + 매핑 키셋")
    inner class ClassAndMapping {

        private val mapping = SFSchemaUtils.getSFMapping(UserRoleEntity::class.java)

        @Test
        @DisplayName("@SFObject 값은 'UserRole'")
        fun sfObjectValue() {
            val annotation = UserRoleEntity::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("UserRole")
        }

        @Test
        @DisplayName("매핑 키 = SF 핵심 메타 6 (BaseEntity 미상속 — CreatedDate/CreatedById 부재)")
        fun mappingKeySize() {
            // Name / DeveloperName / RollupDescription / ParentRoleId / LastModifiedDate / LastModifiedById (6)
            assertThat(mapping).hasSize(6)
        }

        @Test
        @DisplayName("SF 필드 → JPA 컬럼 매핑")
        fun fieldMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DeveloperName"]).isEqualTo("developer_name")
            assertThat(mapping["RollupDescription"]).isEqualTo("rollup_description")
            assertThat(mapping["ParentRoleId"]).isEqualTo("parent_user_role_sfid")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("BaseEntity 의 CreatedDate / CreatedById 는 미보유")
        fun noCreatedAuditFields() {
            assertThat(mapping.keys).doesNotContain("CreatedDate", "CreatedById")
        }
    }

    @Nested
    @DisplayName("AC2 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = UserRoleEntity::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 user_role_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(UserRoleEntity::class.java)
            assertThat(mapping.values).doesNotContain("user_role_id")
        }
    }

    @Nested
    @DisplayName("AC3 — Portal / Forecast / Access picklist 9 필드 미보존")
    inner class IrrelevantFieldsExcluded {

        private val mapping = SFSchemaUtils.getSFMapping(UserRoleEntity::class.java)

        @Test
        @DisplayName("PortalType / PortalAccountId / PortalAccountOwnerId 미보유")
        fun portalFieldsExcluded() {
            assertThat(mapping.keys).doesNotContain(
                "PortalType",
                "PortalAccountId",
                "PortalAccountOwnerId",
            )
        }

        @Test
        @DisplayName("ForecastUserId / MayForecastManagerShare 미보유")
        fun forecastFieldsExcluded() {
            assertThat(mapping.keys).doesNotContain(
                "ForecastUserId",
                "MayForecastManagerShare",
            )
        }

        @Test
        @DisplayName("OpportunityAccessForAccountOwner / CaseAccessForAccountOwner / ContactAccessForAccountOwner 미보유")
        fun accessPicklistFieldsExcluded() {
            assertThat(mapping.keys).doesNotContain(
                "OpportunityAccessForAccountOwner",
                "CaseAccessForAccountOwner",
                "ContactAccessForAccountOwner",
            )
        }
    }
}
