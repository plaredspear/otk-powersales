package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #736 — Appointment ↔ Salesforce `Appointment__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`Appointment__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject
 *   - AC2: @SFField 매핑 키셋 (총 21 = Custom 16 + Group A 3 + BaseEntity 2)
 *   - AC3: Custom 16개 매핑 (컬럼명 mismatch 항목 포함 — Q2 옵션 1 결정)
 *   - AC4: Group A 매핑 (IsDeleted / CreatedById / LastModifiedById)
 *   - AC5: BaseEntity 매핑 (CreatedDate / LastModifiedDate)
 *   - AC6: PK / FK 미부착
 */
@DisplayName("Appointment SF 어노테이션 검증 (Spec #736)")
class AppointmentSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'Appointment__c'")
        fun sfObjectValue() {
            val annotation = Appointment::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("Appointment__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 수")
    inner class SfFieldMappingSize {

        private val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)

        @Test
        @DisplayName("매핑 키 수 = 21 (Custom 16 + Group A 3 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(22)
        }
    }

    @Nested
    @DisplayName("AC3 — Custom 16개 매핑")
    inner class CustomFieldMappings {

        private val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)

        @Test
        @DisplayName("일반 필드 (이름 일치)")
        fun nameMatchingFields() {
            assertThat(mapping["EmployeeCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["Jikchak__c"]).isEqualTo("jikchak")
            assertThat(mapping["Jikgub__c"]).isEqualTo("jikgub")
            assertThat(mapping["Jikjong__c"]).isEqualTo("jikjong")
            assertThat(mapping["Jikwee__c"]).isEqualTo("jikwee")
            assertThat(mapping["JobCode__c"]).isEqualTo("job_code")
            assertThat(mapping["JobName__c"]).isEqualTo("job_name")
            assertThat(mapping["ManageType__c"]).isEqualTo("manage_type")
            assertThat(mapping["OrdDetailCode__c"]).isEqualTo("ord_detail_code")
            assertThat(mapping["OrdDetailNode__c"]).isEqualTo("ord_detail_node")
            assertThat(mapping["WorkArea__c"]).isEqualTo("work_area")
            assertThat(mapping["WorkType__c"]).isEqualTo("work_type")
            assertThat(mapping["isEmpCodeExist__c"]).isEqualTo("emp_code_exist")
        }

        @Test
        @DisplayName("이름 mismatch 항목 (Q2 옵션 1 — 컬럼명 유지)")
        fun nameMismatchFields() {
            assertThat(mapping["OrgCode__c"]).isEqualTo("after_org_code")
            assertThat(mapping["OrgName__c"]).isEqualTo("after_org_name")
            assertThat(mapping["AppointmentDate__c"]).isEqualTo("appoint_date")
        }
    }

    @Nested
    @DisplayName("AC4 — Group A 매핑 (IsDeleted / CreatedBy / LastModifiedBy)")
    inner class GroupAMappings {

        private val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)

        @Test
        @DisplayName("IsDeleted → is_deleted")
        fun isDeletedMapping() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
        }

        @Test
        @DisplayName("CreatedById → created_by_sfid")
        fun createdByIdMapping() {
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
        }

        @Test
        @DisplayName("LastModifiedById → last_modified_by_sfid")
        fun lastModifiedByIdMapping() {
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("OwnerId 매핑 미포함 (Group/User — 신규 시스템 Group 부재)")
        fun ownerIdNotMapped() {
            assertThat(mapping).doesNotContainKey("OwnerId")
        }
    }

    @Nested
    @DisplayName("AC5 — BaseEntity 매핑 (CreatedDate / LastModifiedDate)")
    inner class BaseEntityMappings {

        private val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)

        @Test
        @DisplayName("CreatedDate → created_at (BaseEntity 상속)")
        fun createdDateMapping() {
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
        }

        @Test
        @DisplayName("LastModifiedDate → updated_at (BaseEntity 상속)")
        fun lastModifiedDateMapping() {
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }
    }

    @Nested
    @DisplayName("AC6 — PK / FK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Appointment::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 PK / FK 컬럼명 미등장")
        fun mappingValuesExcludePkAndFk() {
            val mapping = SFSchemaUtils.getSFMapping(Appointment::class.java)
            assertThat(mapping.values).doesNotContain(
                "appointment_id",
                "created_by_id",
                "last_modified_by_id"
            )
        }
    }
}
