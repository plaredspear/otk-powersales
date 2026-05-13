package com.otoki.powersales.organization.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #618 — Organization ↔ Salesforce `Org__c` 매핑 검증 + ExternalKey 도입.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/조직(Org__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` + 매핑 키셋 (13개 — 기존 12 + 신규 1)
 *   - AC2: 신규 `ExternalKey__c` 매핑
 *   - AC3: PK 미부착 + 빌드 회귀
 */
@DisplayName("Organization SF 어노테이션 검증 (Spec #618)")
class OrganizationSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject + 매핑 키셋")
    inner class ClassAndMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Organization::class.java)

        @Test
        @DisplayName("@SFObject 값은 'Org__c'")
        fun sfObjectValue() {
            val annotation = Organization::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Org__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 15 (기존 12 + 신규 1 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(18)
        }

        @Test
        @DisplayName("§6.1 — 기존 12개 매핑 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["CostCenterLevel2__c"]).isEqualTo("cc_cd2")
            assertThat(mapping["OrgCodeLevel2__c"]).isEqualTo("org_cd2")
            assertThat(mapping["OrgNameLevel2__c"]).isEqualTo("org_nm2")
            assertThat(mapping["CostCenterLevel3__c"]).isEqualTo("cc_cd3")
            assertThat(mapping["OrgCodeLevel3__c"]).isEqualTo("org_cd3")
            assertThat(mapping["OrgNameLevel3__c"]).isEqualTo("org_nm3")
            assertThat(mapping["CostCenterLevel4__c"]).isEqualTo("cc_cd4")
            assertThat(mapping["OrgCodeLevel4__c"]).isEqualTo("org_cd4")
            assertThat(mapping["OrgNameLevel4__c"]).isEqualTo("org_nm4")
            assertThat(mapping["CostCenterLevel5__c"]).isEqualTo("cc_cd5")
            assertThat(mapping["OrgCodeLevel5__c"]).isEqualTo("org_cd5")
            assertThat(mapping["OrgNameLevel5__c"]).isEqualTo("org_nm5")
        }
    }

    @Nested
    @DisplayName("AC2 — 신규 ExternalKey__c 매핑")
    inner class NewMapping {

        @Test
        @DisplayName("§6.2 — ExternalKey__c → external_key")
        fun externalKeyMapping() {
            val mapping = SFSchemaUtils.getSFMapping(Organization::class.java)
            assertThat(mapping["ExternalKey__c"]).isEqualTo("external_key")
        }
    }

    @Nested
    @DisplayName("AC3 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Organization::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 organization_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Organization::class.java)
            assertThat(mapping.values).doesNotContain("organization_id")
        }
    }
}
