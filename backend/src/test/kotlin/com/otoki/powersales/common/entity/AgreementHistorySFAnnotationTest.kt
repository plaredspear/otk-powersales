package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #629 — AgreementHistory ↔ Salesforce `AgreementHistory__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/동의이력(AgreementHistory__c).md
 */
@DisplayName("AgreementHistory SF 어노테이션 검증 (Spec #629)")
class AgreementHistorySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AgreementHistory__c'")
        fun sfObjectValue() {
            val annotation = AgreementHistory::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("AgreementHistory__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 6 (4 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)
            assertThat(mapping).hasSize(6)
        }
    }

    @Nested
    @DisplayName("AC1 — PK·FK·entity-only 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(agreementWordId) 필드에 @SFField 미부착")
        fun agreementWordIdHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("agreementWordId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("entity-only(isDeleted) 필드에 @SFField 미부착")
        fun isDeletedHasNoSfField() {
            val field = AgreementHistory::class.java.getDeclaredField("isDeleted")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 agreement_history_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)
            assertThat(mapping.values).doesNotContain("agreement_history_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (4개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AgreementHistory::class.java)

        @Test
        @DisplayName("4개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["AgreementFlag__c"]).isEqualTo("agreement_flag")
            assertThat(mapping["AgreementDate__c"]).isEqualTo("agreement_date")
            assertThat(mapping["AgreementWordId__c"]).isEqualTo("agreement_word_sfid")
            assertThat(mapping["EmployeeId__c"]).isEqualTo("employee_sfid")
        }
    }
}
