package com.otoki.powersales.leave.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Column
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AlternativeHoliday SF 어노테이션 검증 (Spec #622)")
class AlternativeHolidaySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__AlternativeHoliday__c'")
        fun sfObjectValue() {
            val annotation = AlternativeHoliday::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__AlternativeHoliday__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 11 (Spec #740 EmpName 제거: 6 + BaseEntity 2 + R-2 3)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AlternativeHoliday::class.java)
            assertThat(mapping).hasSize(11)
        }
    }

    @Nested
    @DisplayName("AC2 — PK/FK 미부착")
    inner class PkFkConvention {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"alternative_holiday_id\") 명시")
        fun pkColumnNameConvention() {
            val field = AlternativeHoliday::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("alternative_holiday_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AlternativeHoliday::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = AlternativeHoliday::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("createdBy 감사 필드에 @SFField 미부착")
        fun createdByHasNoSfField() {
            val field = AlternativeHoliday::class.java.getDeclaredField("createdBy")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 alternative_holiday_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AlternativeHoliday::class.java)
            assertThat(mapping.values).doesNotContain("alternative_holiday_id")
        }
    }

    @Nested
    @DisplayName("AC3 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AlternativeHoliday::class.java)

        @Test
        @DisplayName("6개 SF API Name → 컬럼명 1:1 (Spec #740: EmpName Formula 제거)")
        fun mappingValues() {
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__ActualWorkDate__c"]).isEqualTo("actual_work_date")
            assertThat(mapping["DKRetail__TargetAltHolidayDate__c"]).isEqualTo("target_alt_holiday_date")
            assertThat(mapping["DKRetail__ConfirmAltHolidayDate__c"]).isEqualTo("confirm_alt_holiday_date")
            assertThat(mapping["DKRetail__Status__c"]).isEqualTo("status")
            assertThat(mapping["DKRetail__ChangeReason__c"]).isEqualTo("change_reason")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Spec #703 — BaseEntity CreatedDate/LastModifiedDate 포함)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "DKRetail__EmployeeId__c",
                    "DKRetail__ActualWorkDate__c", "DKRetail__TargetAltHolidayDate__c",
                    "DKRetail__ConfirmAltHolidayDate__c", "DKRetail__Status__c",
                    "DKRetail__ChangeReason__c",
                    "CreatedDate", "LastModifiedDate",
                    "OwnerId", "CreatedById", "LastModifiedById"
                )
        }
    }
}
