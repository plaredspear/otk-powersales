package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #617 — StaffReview ↔ Salesforce `StaffReview__c` SF 누락 비수식 컬럼 도입 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/사원평가(StaffReview__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (21개 — 기존 16 + 신규 5)
 *   - AC3: PK / FK (entity-only) 미부착
 */
@DisplayName("StaffReview SF 어노테이션 검증 (Spec #617)")
class StaffReviewSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
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
    @DisplayName("AC2 — @SFField 매핑 키셋 (21개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(StaffReview::class.java)

        @Test
        @DisplayName("매핑 키 수 = 21 (기존 16 + 신규 5)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(21)
        }

        @Test
        @DisplayName("§6.2 — 신규 5개 필드 매핑")
        fun section62NewFields() {
            assertThat(mapping["DKRetail_WorkingCategory1__c"]).isEqualTo("working_category1")
            assertThat(mapping["DKRetail_WorkingCategory2__c"]).isEqualTo("working_category2")
            assertThat(mapping["DKRetail_WorkingCategory3__c"]).isEqualTo("working_category3")
            assertThat(mapping["JobCode__c"]).isEqualTo("job_code")
            assertThat(mapping["FirstDayofMonth__c"]).isEqualTo("first_day_of_month")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 매핑 16개 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail_EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["EmployeeName__c"]).isEqualTo("employee_name")
            assertThat(mapping["EmployeeNumber__c"]).isEqualTo("employee_number")
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
        }
    }

    @Nested
    @DisplayName("AC3 — PK / FK (entity-only) 미부착")
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
