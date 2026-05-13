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

@DisplayName("AttendInfo SF 어노테이션 검증 (Spec #619)")
class AttendInfoSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'AttendInfo__c'")
        fun sfObjectValue() {
            val annotation = AttendInfo::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("AttendInfo__c")
        }

        @Test
        @DisplayName("매핑 키 수 = 7 (5 + BaseEntity 2)")
        fun mappingKeySize() {
            val mapping = SFSchemaUtils.getSFMapping(AttendInfo::class.java)
            assertThat(mapping).hasSize(12)
        }
    }

    @Nested
    @DisplayName("AC2 — PK 컨벤션 정합")
    inner class PkConvention {

        @Test
        @DisplayName("PK(id) 필드 @Column(name = \"attend_info_id\") 명시")
        fun pkColumnNameConvention() {
            val field = AttendInfo::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(Id::class.java)).isTrue()
            val column = field.getAnnotation(Column::class.java)
            assertThat(column).isNotNull
            assertThat(column.name).isEqualTo("attend_info_id")
        }

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = AttendInfo::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 attend_info_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(AttendInfo::class.java)
            assertThat(mapping.values).doesNotContain("attend_info_id")
        }
    }

    @Nested
    @DisplayName("AC3 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(AttendInfo::class.java)

        @Test
        @DisplayName("5개 SF API Name → 컬럼명 1:1")
        fun mappingValues() {
            assertThat(mapping["EmployeeCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["AttendType__c"]).isEqualTo("attend_type")
            assertThat(mapping["Status__c"]).isEqualTo("status")
        }

        @Test
        @DisplayName("매핑 키셋 정확히 일치 (Spec #703 — BaseEntity CreatedDate/LastModifiedDate 포함)")
        fun mappingKeysExact() {
            assertThat(mapping.keys)
                .containsExactlyInAnyOrder(
                    "Name",
                    "EmployeeCode__c", "StartDate__c", "EndDate__c",
                    "AttendType__c", "Status__c",
                    "CreatedDate", "LastModifiedDate",
                    "OwnerId", "CreatedById", "LastModifiedById",
                    "IsDeleted"
                )
        }
    }
}
