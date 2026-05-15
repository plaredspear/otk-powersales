package com.otoki.powersales.common.entity

import com.otoki.powersales.common.enums.WorkingCategory1
import com.otoki.powersales.common.enums.WorkingCategory2
import com.otoki.powersales.common.enums.WorkingCategory3
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #711 — StaffReview ↔ Salesforce `StaffReview__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`StaffReview__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject 무변경
 *   - AC2: @SFField 매핑 키셋 (26개 — 기존 21 + Group A 신규 3 + BaseEntity 2)
 *   - AC3: Group A 매핑 (IsDeleted / CreatedById / LastModifiedById)
 *   - AC4: BaseEntity 매핑 (CreatedDate / LastModifiedDate)
 *   - AC5: Picklist enum converter 연결 (WorkingCategory1/2/3)
 *   - AC6: PK / entity-only FK 미부착
 */
@DisplayName("StaffReview SF 어노테이션 검증 (Spec #711)")
class StaffReviewSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'StaffReview__c'")
        fun sfObjectValue() {
            val annotation = StaffReview::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("StaffReview__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(StaffReview::class.java)

        @Test
        @DisplayName("매핑 키 수 = 26 (기존 21 + Group A 3 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(29)
        }

        @Test
        @DisplayName("기존 21개 커스텀 필드 매핑 무변경")
        fun existingCustomFieldMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail_EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["EmployeeName__c"]).isEqualTo("employee_name")
            assertThat(mapping["EmployeeNumber__c"]).isEqualTo("employee_code")
            assertThat(mapping["Branch__c"]).isEqualTo("branch")
            assertThat(mapping["BranchReviews__c"]).isEqualTo("branch_review_sfid")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["EmployeeTotalScore__c"]).isEqualTo("employee_total_score")
            assertThat(mapping["Attendance__c"]).isEqualTo("attendance_score")
            assertThat(mapping["InstructionsDefault__c"]).isEqualTo("instruction_disobedience_score")
            assertThat(mapping["Priority_EventItemManage__c"]).isEqualTo("priority_item_event_score")
            assertThat(mapping["DisplayManageEventGoals__c"]).isEqualTo("display_event_goal_score")
            assertThat(mapping["BusinessPartnerTies__c"]).isEqualTo("account_partnership_score")
            assertThat(mapping["ClothesSatellite__c"]).isEqualTo("clothes_hygiene_score")
            assertThat(mapping["ProductManageCallment__c"]).isEqualTo("product_manage_callment_score")
            assertThat(mapping["EducationalEvaluation__c"]).isEqualTo("education_evaluation_score")
            assertThat(mapping["DKRetail_WorkingCategory1__c"]).isEqualTo("working_category1")
            assertThat(mapping["DKRetail_WorkingCategory2__c"]).isEqualTo("working_category2")
            assertThat(mapping["DKRetail_WorkingCategory3__c"]).isEqualTo("working_category3")
            assertThat(mapping["JobCode__c"]).isEqualTo("job_code")
            assertThat(mapping["FirstDayofMonth__c"]).isEqualTo("first_day_of_month")
        }
    }

    @Nested
    @DisplayName("AC3 — Group A 매핑 (IsDeleted / CreatedById / LastModifiedById)")
    inner class GroupAMappings {

        private val mapping = SFSchemaUtils.getSFMapping(StaffReview::class.java)

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
    }

    @Nested
    @DisplayName("AC4 — BaseEntity 매핑 (CreatedDate / LastModifiedDate)")
    inner class BaseEntityMappings {

        private val mapping = SFSchemaUtils.getSFMapping(StaffReview::class.java)

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
    @DisplayName("AC5 — Picklist enum converter (WorkingCategory1/2/3)")
    inner class PicklistConverters {

        @Test
        @DisplayName("WorkingCategory1 — 진열/행사 fromDisplayNameOrNull 정상 변환")
        fun workingCategory1Converter() {
            assertThat(WorkingCategory1.fromDisplayNameOrNull("진열")).isEqualTo(WorkingCategory1.DISPLAY)
            assertThat(WorkingCategory1.fromDisplayNameOrNull("행사")).isEqualTo(WorkingCategory1.EVENT)
            assertThat(WorkingCategory1.fromDisplayNameOrNull(null)).isNull()
        }

        @Test
        @DisplayName("WorkingCategory2 — 전담/진열겸임 fromDisplayNameOrNull 정상 변환")
        fun workingCategory2Converter() {
            assertThat(WorkingCategory2.fromDisplayNameOrNull("전담")).isEqualTo(WorkingCategory2.DEDICATED)
            assertThat(WorkingCategory2.fromDisplayNameOrNull("진열겸임")).isEqualTo(WorkingCategory2.DISPLAY_CONCURRENT)
            assertThat(WorkingCategory2.fromDisplayNameOrNull(null)).isNull()
        }

        @Test
        @DisplayName("WorkingCategory3 — 고정/격고/순회 fromDisplayNameOrNull 정상 변환")
        fun workingCategory3Converter() {
            assertThat(WorkingCategory3.fromDisplayNameOrNull("고정")).isEqualTo(WorkingCategory3.FIXED)
            assertThat(WorkingCategory3.fromDisplayNameOrNull("격고")).isEqualTo(WorkingCategory3.ALTERNATE)
            assertThat(WorkingCategory3.fromDisplayNameOrNull("순회")).isEqualTo(WorkingCategory3.PATROL)
            assertThat(WorkingCategory3.fromDisplayNameOrNull(null)).isNull()
        }
    }

    @Nested
    @DisplayName("AC6 — PK / entity-only FK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = StaffReview::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("employee_id (FK, entity-only) 에 @SFField 미부착")
        fun employeeIdHasNoSfField() {
            val field = StaffReview::class.java.getDeclaredField("employeeId")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 staff_review_id / employee_id 미등장")
        fun mappingValuesExcludePkAndFk() {
            val mapping = SFSchemaUtils.getSFMapping(StaffReview::class.java)
            assertThat(mapping.values).doesNotContain("staff_review_id", "employee_id")
        }
    }
}
