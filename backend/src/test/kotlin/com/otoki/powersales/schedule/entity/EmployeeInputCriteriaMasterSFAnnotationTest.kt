package com.otoki.powersales.schedule.entity

import com.otoki.powersales.platform.common.salesforce.SFField
import com.otoki.powersales.platform.common.salesforce.SFObject
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * EmployeeInputCriteriaMaster ↔ Salesforce `EmployeeInputCriteriaMaster__c` 어노테이션 검증.
 *
 * 단일 권위: Salesforce Object (`EmployeeInputCriteriaMaster__c`) raw JSON.
 * sf-meta-diff (Q1/Q2/Q3 R-2 + Q4 Formula 6건 제거) 적용 후 상태.
 */
@DisplayName("EmployeeInputCriteriaMaster SF 어노테이션 검증 (sf-meta-diff)")
class EmployeeInputCriteriaMasterSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'EmployeeInputCriteriaMaster__c'")
        fun sfObjectValue() {
            val annotation = EmployeeInputCriteriaMaster::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("EmployeeInputCriteriaMaster__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 15 (SF 표준 Name 1 + 도메인 8 + Group A 4 + BaseEntity 2. Formula 6건 제거)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(EmployeeInputCriteriaMaster::class.java)
            assertThat(mapping).hasSize(15)
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = EmployeeInputCriteriaMaster::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 employee_input_criteria_master_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(EmployeeInputCriteriaMaster::class.java)
            assertThat(mapping.values).doesNotContain("employee_input_criteria_master_id")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (도메인 8개)")
    inner class DomainSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(EmployeeInputCriteriaMaster::class.java)

        @Test
        @DisplayName("도메인 8개 SF API Name → 컬럼명 1:1")
        fun domainMappingValues() {
            assertThat(mapping["BifurcationHalfPersonStandard__c"]).isEqualTo("bifurcation_half_person_standard")
            assertThat(mapping["Boundary__c"]).isEqualTo("boundary")
            assertThat(mapping["Category__c"]).isEqualTo("category_sfid")
            assertThat(mapping["Confirmed__c"]).isEqualTo("confirmed")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Fixed1PersonStandardAmount__c"]).isEqualTo("fixed_1_person_standard_amount")
            assertThat(mapping["TypeOfWork1__c"]).isEqualTo("type_of_work_1")
        }

        @Test
        @DisplayName("Q4 (sf-meta-diff): Formula 6건 매핑 제거")
        fun formulaMappingRemoved() {
            assertThat(mapping).doesNotContainKey("ConfirmAlert__c")
            assertThat(mapping).doesNotContainKey("AccountCategorizedCode__c")
            assertThat(mapping).doesNotContainKey("BifurcationHalfPersonMinAmountInRealmRan__c")
            assertThat(mapping).doesNotContainKey("Fixed1PersonMinAmountInRealmRange__c")
            assertThat(mapping).doesNotContainKey("ValidData__c")
            assertThat(mapping).doesNotContainKey("Valid__c")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (SF 표준 + Group A 4개)")
    inner class StandardSfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(EmployeeInputCriteriaMaster::class.java)

        @Test
        @DisplayName("SF 표준 Name + Group A 4개 SF API Name → 컬럼명 1:1")
        fun standardMappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
        }
    }
}
