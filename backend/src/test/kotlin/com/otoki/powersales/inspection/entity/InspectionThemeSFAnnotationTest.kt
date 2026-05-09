package com.otoki.powersales.inspection.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #633 — InspectionTheme ↔ Salesforce `Theme__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/현장점검_등록(Theme__c).md
 */
@DisplayName("InspectionTheme SF 어노테이션 검증 (Spec #633)")
class InspectionThemeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'Theme__c'")
        fun sfObjectValue() {
            val annotation = InspectionTheme::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Theme__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 7")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(InspectionTheme::class.java)
            assertThat(mapping).hasSize(7)
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (7개)")
    inner class DomainSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(InspectionTheme::class.java)

        @Test
        @DisplayName("도메인 7개 SF API Name → 컬럼명 1:1")
        fun domainMappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Title__c"]).isEqualTo("title")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Department__c"]).isEqualTo("department")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["PublicFlag__c"]).isEqualTo("public_flag")
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = InspectionTheme::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }
}
