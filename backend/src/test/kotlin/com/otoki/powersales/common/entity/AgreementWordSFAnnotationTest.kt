package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #630 — AgreementWord ↔ Salesforce `AgreementWord__c` 어노테이션 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/동의문구(AgreementWord__c).md
 */
@DisplayName("AgreementWord SF 어노테이션 검증 (Spec #630)")
class AgreementWordSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AgreementWord__c'")
        fun sfObjectValue() {
            val annotation = AgreementWord::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("AgreementWord__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 5")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)
            assertThat(mapping).hasSize(5)
        }
    }

    @Nested
    @DisplayName("AC1 — PK·entity-only 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AgreementWord::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("entity-only(isDeleted) 필드에 @SFField 미부착")
        fun isDeletedHasNoSfField() {
            val field = AgreementWord::class.java.getDeclaredField("isDeleted")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 agreement_word_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)
            assertThat(mapping.values).doesNotContain("agreement_word_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (5개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)

        @Test
        @DisplayName("5개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Contents__c"]).isEqualTo("contents")
            assertThat(mapping["Active__c"]).isEqualTo("active")
            assertThat(mapping["ActiveDate__c"]).isEqualTo("active_date")
            assertThat(mapping["AfterActiveDate__c"]).isEqualTo("after_active_date")
        }
    }
}
