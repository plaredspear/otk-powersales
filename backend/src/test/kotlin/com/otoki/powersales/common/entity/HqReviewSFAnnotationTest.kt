package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #708 — HqReview ↔ Salesforce `HQReview__c` 어노테이션 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`HQReview__c`)
 */
@DisplayName("HqReview SF 어노테이션 검증 (Spec #708)")
class HqReviewSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'HQReview__c'")
        fun sfObjectValue() {
            val annotation = HqReview::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("HQReview__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 13 (entity 11 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(HqReview::class.java)
            assertThat(mapping).hasSize(13)
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(HqReview::class.java)

        @Test
        @DisplayName("entity 커스텀 필드 7개 SF API Name → 컬럼명 1:1")
        fun customFieldMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["BranchCode__c"]).isEqualTo("branch_code")
            assertThat(mapping["BranchName__c"]).isEqualTo("branch_name")
            assertThat(mapping["FirstDayofMonth__c"]).isEqualTo("first_day_of_month")
            assertThat(mapping["EvaluationyType__c"]).isEqualTo("evaluation_type")
            assertThat(mapping["ABCTypeCode__c"]).isEqualTo("abc_type_code")
            assertThat(mapping["HR_Code_c__c"]).isEqualTo("hr_code")
        }

        @Test
        @DisplayName("Group A — IsDeleted / OwnerId / CreatedById / LastModifiedById 매핑")
        fun groupAMappings() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("BaseEntity — CreatedDate / LastModifiedDate 매핑")
        fun baseEntityMappings() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class NonMappedFieldExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = HqReview::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 hq_review_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(HqReview::class.java)
            assertThat(mapping.values).doesNotContain("hq_review_id")
        }
    }
}
