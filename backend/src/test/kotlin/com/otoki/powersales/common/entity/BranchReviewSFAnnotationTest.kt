package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #735 — BranchReview ↔ Salesforce `BranchReview__c` SF Object 정합 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/sf-object-meta/_raw/BranchReview__c.json
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject
 *   - AC2: @SFField 매핑 키셋 (총 48개 — Custom 42 + Group A 4 + Standard 1 + BaseEntity 2 = 49, 그 중 Group A `IsDeleted`/OwnerId/CreatedById/LastModifiedById 4 + Custom 42 + Name 1 + BaseEntity 2)
 *   - AC3: Custom 42개 매핑
 *   - AC4: Group A 매핑 (IsDeleted / OwnerId / CreatedById / LastModifiedById)
 *   - AC5: BaseEntity 매핑 (CreatedDate / LastModifiedDate)
 *   - AC6: PK / FK 미부착
 */
@DisplayName("BranchReview SF 어노테이션 검증 (Spec #735)")
class BranchReviewSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'BranchReview__c'")
        fun sfObjectValue() {
            val annotation = BranchReview::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("BranchReview__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 수")
    inner class SfFieldMappingSize {

        private val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)

        @Test
        @DisplayName("매핑 키 수 = 49 (Name 1 + Custom 42 + Group A 4 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(49)
        }
    }

    @Nested
    @DisplayName("AC3 — Custom 42개 매핑 (식별/시점 4 + 판촉 19 + 레이디 19)")
    inner class CustomFieldMappings {

        private val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)

        @Test
        @DisplayName("Name → name")
        fun nameMapping() {
            assertThat(mapping["Name"]).isEqualTo("name")
        }

        @Test
        @DisplayName("식별 / 시점 4개")
        fun identityAndTimeFields() {
            assertThat(mapping["BranchName__c"]).isEqualTo("branch_name")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["FirstDayofMonth__c"]).isEqualTo("first_day_of_month")
            assertThat(mapping["Confirmed__c"]).isEqualTo("confirmed")
        }

        @Test
        @DisplayName("판촉 부문 — 평가 인원 + 합계 9 + 평균 9")
        fun salesPromotionFields() {
            assertThat(mapping["EmployeeEvaluationNumber__c"]).isEqualTo("employee_evaluation_number")
            // 합계 9
            assertThat(mapping["SumAttendance__c"]).isEqualTo("sum_attendance")
            assertThat(mapping["SumBusinessPartnerTies__c"]).isEqualTo("sum_business_partner_ties")
            assertThat(mapping["SumClothesSatellite__c"]).isEqualTo("sum_clothes_satellite")
            assertThat(mapping["SumDisplayManageEventGoals__c"]).isEqualTo("sum_display_manage_event_goals")
            assertThat(mapping["SumEducationalEvaluation__c"]).isEqualTo("sum_educational_evaluation")
            assertThat(mapping["SumInstructionsDefault__c"]).isEqualTo("sum_instructions_default")
            assertThat(mapping["SumPriority_EventItemManage__c"]).isEqualTo("sum_priority_event_item_manage")
            assertThat(mapping["SumProductManageCallment__c"]).isEqualTo("sum_product_manage_callment")
            assertThat(mapping["SumTotalScore__c"]).isEqualTo("sum_total_score")
            // 평균 9
            assertThat(mapping["AttendanceAverage__c"]).isEqualTo("attendance_average")
            assertThat(mapping["BusinessPartnerTiesAverage__c"]).isEqualTo("business_partner_ties_average")
            assertThat(mapping["ClothesSatelliteAverage__c"]).isEqualTo("clothes_satellite_average")
            assertThat(mapping["DisplayManageEventGoalsAverage__c"]).isEqualTo("display_manage_event_goals_average")
            assertThat(mapping["EducationalEvaluationAverage__c"]).isEqualTo("educational_evaluation_average")
            assertThat(mapping["InstructionsDefaultAverage__c"]).isEqualTo("instructions_default_average")
            assertThat(mapping["Priority_EventItemManageAverage__c"]).isEqualTo("priority_event_item_manage_average")
            assertThat(mapping["ProductManageCallmentAverage__c"]).isEqualTo("product_manage_callment_average")
            assertThat(mapping["SumTotalScoreAverage__c"]).isEqualTo("sum_total_score_average")
        }

        @Test
        @DisplayName("레이디 부문 — 평가 인원 + 합계 9 + 평균 9")
        fun ladyFields() {
            assertThat(mapping["EmployeeEvaluationNumber_lady__c"]).isEqualTo("employee_evaluation_number_lady")
            // 합계 9
            assertThat(mapping["SumAttendance_lady__c"]).isEqualTo("sum_attendance_lady")
            assertThat(mapping["SumBusinessPartnerTies_lady__c"]).isEqualTo("sum_business_partner_ties_lady")
            assertThat(mapping["SumClothesSatellite_lady__c"]).isEqualTo("sum_clothes_satellite_lady")
            assertThat(mapping["SumDisplayManageEventGoals_lady__c"]).isEqualTo("sum_display_manage_event_goals_lady")
            assertThat(mapping["SumEducationalEvaluation_lady__c"]).isEqualTo("sum_educational_evaluation_lady")
            assertThat(mapping["SumInstructionsDefault_lady__c"]).isEqualTo("sum_instructions_default_lady")
            assertThat(mapping["SumPriority_EventItemManage_lady__c"]).isEqualTo("sum_priority_event_item_manage_lady")
            assertThat(mapping["SumProductManageCallment_lady__c"]).isEqualTo("sum_product_manage_callment_lady")
            assertThat(mapping["SumTotalScore_lady__c"]).isEqualTo("sum_total_score_lady")
            // 평균 9
            assertThat(mapping["AttendanceAverage_lady__c"]).isEqualTo("attendance_average_lady")
            assertThat(mapping["BusinessPartnerTiesAverage_lady__c"]).isEqualTo("business_partner_ties_average_lady")
            assertThat(mapping["ClothesSatelliteAverage_lady__c"]).isEqualTo("clothes_satellite_average_lady")
            assertThat(mapping["DisplayManageEventGoalsAverage_lady__c"]).isEqualTo("display_manage_event_goals_average_lady")
            assertThat(mapping["EducationalEvaluationAverage_lady__c"]).isEqualTo("educational_evaluation_average_lady")
            assertThat(mapping["InstructionsDefaultAverage_lady__c"]).isEqualTo("instructions_default_average_lady")
            assertThat(mapping["Priority_EventItemManageAverage_lady__c"]).isEqualTo("priority_event_item_manage_average_lady")
            assertThat(mapping["ProductManageCallmentAverage_lady__c"]).isEqualTo("product_manage_callment_average_lady")
            assertThat(mapping["SumTotalScoreAverage_lady__c"]).isEqualTo("sum_total_score_average_lady")
        }
    }

    @Nested
    @DisplayName("AC4 — Group A 매핑 (IsDeleted / Owner / CreatedBy / LastModifiedBy)")
    inner class GroupAMappings {

        private val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)

        @Test
        @DisplayName("IsDeleted → is_deleted")
        fun isDeletedMapping() {
            assertThat(mapping["IsDeleted"]).isEqualTo("is_deleted")
        }

        @Test
        @DisplayName("OwnerId → owner_sfid")
        fun ownerIdMapping() {
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
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
    @DisplayName("AC5 — BaseEntity 매핑 (CreatedDate / LastModifiedDate)")
    inner class BaseEntityMappings {

        private val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)

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
            val field = BranchReview::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 PK / FK 컬럼명 미등장")
        fun mappingValuesExcludePkAndFk() {
            val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)
            assertThat(mapping.values).doesNotContain(
                "branch_review_id",
                "owner_id",
                "created_by_id",
                "last_modified_by_id"
            )
        }
    }
}
