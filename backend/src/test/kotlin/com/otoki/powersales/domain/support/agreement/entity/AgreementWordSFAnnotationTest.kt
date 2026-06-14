package com.otoki.powersales.domain.support.agreement.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #707 — AgreementWord ↔ Salesforce `AgreementWord__c` 어노테이션 검증.
 *
 * 단일 권위: Salesforce Object 메타 (`AgreementWord__c`)
 */
@DisplayName("AgreementWord SF 어노테이션 검증 (Spec #707)")
class AgreementWordSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AgreementWord__c'")
        fun sfObjectValue() {
            val annotation = AgreementWord::class.java.getAnnotation(SFObject::class.java)
            Assertions.assertThat(annotation).isNotNull
            Assertions.assertThat(annotation.value).isEqualTo("AgreementWord__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 11")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)
            Assertions.assertThat(mapping).hasSize(11)
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AgreementWord::class.java.getDeclaredField("id")
            Assertions.assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 agreement_word_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)
            Assertions.assertThat(mapping.values).doesNotContain("agreement_word_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (11개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AgreementWord::class.java)

        @Test
        @DisplayName("기존 5개 SF API Name → 컬럼명 1:1")
        fun existingMappingValues() {
            Assertions.assertThat(mapping["Name"]).isEqualTo("name")
            Assertions.assertThat(mapping["Contents__c"]).isEqualTo("contents")
            Assertions.assertThat(mapping["Active__c"]).isEqualTo("active")
            Assertions.assertThat(mapping["ActiveDate__c"]).isEqualTo("active_date")
            Assertions.assertThat(mapping["AfterActiveDate__c"]).isEqualTo("after_active_date")
        }

        @Test
        @DisplayName("Spec #707 신규 5개 — IsDeleted, CreatedDate, OwnerId, CreatedById, LastModifiedById")
        fun newMappingValues() {
            Assertions.assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            Assertions.assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            Assertions.assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            Assertions.assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            Assertions.assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("sf-meta-diff Q1 — LastModifiedDate → updated_at")
        fun lastModifiedDateMapping() {
            Assertions.assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }
}