package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import jakarta.persistence.Column
import jakarta.persistence.Id
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AttendanceLog SF 어노테이션 검증 (Spec #620)")
class AttendanceLogSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — @SFObject + 매핑 키셋")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__CommuteLog__c'")
        fun sfObjectValue() {
            val annotation = AttendanceLog::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__CommuteLog__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 8 (6 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AttendanceLog::class.java)
            assertThat(mapping).hasSize(8)
        }
    }

    @Nested
    @DisplayName("AC2 — PK/FK 미부착")
    inner class PkFkConvention {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"attendance_log_id\") 명시")
        fun pkColumnNameConvention() {
            val field = AttendanceLog::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("attendance_log_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AttendanceLog::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(employeeId) 필드에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = AttendanceLog::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("FK(accountId) 필드에 @SFField 미부착")
        fun accountIdHasNoSfField() {
            val field = AttendanceLog::class.java.getDeclaredField("accountId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 attendance_log_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AttendanceLog::class.java)
            assertThat(mapping.values).doesNotContain("attendance_log_id")
        }
    }

    @Nested
    @DisplayName("AC3 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AttendanceLog::class.java)

        @Test
        @DisplayName("6개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__CommuteDate__c"]).isEqualTo("attendance_date")
            assertThat(mapping["DKRetail__AccId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["DKRetail__SecondWorkType__c"]).isEqualTo("second_work_type")
            assertThat(mapping["DKRetail__Reason__c"]).isEqualTo("reason")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Spec #703 — BaseEntity CreatedDate/LastModifiedDate 포함)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "Name", "DKRetail__EmployeeId__c", "DKRetail__CommuteDate__c",
                    "DKRetail__AccId__c", "DKRetail__SecondWorkType__c", "DKRetail__Reason__c",
                    "CreatedDate", "LastModifiedDate"
                )
        }
    }
}
