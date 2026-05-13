package com.otoki.powersales.common.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * BranchReview ↔ Salesforce `BranchReview__c` SF Object 정합 검증.
 *
 * 단일 권위: Salesforce describe 메타 (`BranchReview__c`)
 *
 * Roll-Up Summary 20 + Formula 18 (판촉/레이디 평가 인원 / 합계 / 평균) 은
 * §6.7 정책에 따라 entity 매핑 부재 — 본 테스트의 검증 대상 아님.
 *
 * 검증 분류:
 *   - AC1: 클래스 @SFObject
 *   - AC2: @SFField 매핑 키셋 수 (9 = Name + 식별/시점 4 + Group A 4 매핑 + BaseEntity 2 - BaseEntity 2 중복 계산 보정)
 *   - AC3: 식별 / 시점 매핑 (Name / BranchName__c / CostCenterCode__c / FirstDayofMonth__c / Confirmed__c)
 *   - AC4: Group A 매핑 (IsDeleted / OwnerId / CreatedById / LastModifiedById)
 *   - AC5: BaseEntity 매핑 (CreatedDate / LastModifiedDate)
 *   - AC6: PK / FK 미부착
 */
@DisplayName("BranchReview SF 어노테이션 검증")
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
        @DisplayName("매핑 키 수 = 11 (Name 1 + 식별/시점 4 + Group A 4 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(11)
        }
    }

    @Nested
    @DisplayName("AC3 — 식별 / 시점 + Name 매핑 (5건)")
    inner class IdentityAndTimeMappings {

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
    @DisplayName("AC6 — PK / FK 미부착 + Formula 컬럼 부재 (§6.7)")
    inner class PkAndFormulaExclusion {

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

        @Test
        @DisplayName("매핑 keys 에 Formula / Roll-Up Summary 38개 SF API Name 미등장 (§6.7)")
        fun mappingKeysExcludeFormulaFields() {
            val mapping = SFSchemaUtils.getSFMapping(BranchReview::class.java)
            assertThat(mapping.keys).doesNotContain(
                // 판촉 부문 Roll-Up Summary
                "EmployeeEvaluationNumber__c",
                "SumAttendance__c",
                "SumBusinessPartnerTies__c",
                "SumClothesSatellite__c",
                "SumDisplayManageEventGoals__c",
                "SumEducationalEvaluation__c",
                "SumInstructionsDefault__c",
                "SumPriority_EventItemManage__c",
                "SumProductManageCallment__c",
                "SumTotalScore__c",
                // 판촉 부문 Formula (Average)
                "AttendanceAverage__c",
                "BusinessPartnerTiesAverage__c",
                "ClothesSatelliteAverage__c",
                "DisplayManageEventGoalsAverage__c",
                "EducationalEvaluationAverage__c",
                "InstructionsDefaultAverage__c",
                "Priority_EventItemManageAverage__c",
                "ProductManageCallmentAverage__c",
                "SumTotalScoreAverage__c",
                // 레이디 부문
                "EmployeeEvaluationNumber_lady__c",
                "SumAttendance_lady__c",
                "SumBusinessPartnerTies_lady__c",
                "SumClothesSatellite_lady__c",
                "SumDisplayManageEventGoals_lady__c",
                "SumEducationalEvaluation_lady__c",
                "SumInstructionsDefault_lady__c",
                "SumPriority_EventItemManage_lady__c",
                "SumProductManageCallment_lady__c",
                "SumTotalScore_lady__c",
                "AttendanceAverage_lady__c",
                "BusinessPartnerTiesAverage_lady__c",
                "ClothesSatelliteAverage_lady__c",
                "DisplayManageEventGoalsAverage_lady__c",
                "EducationalEvaluationAverage_lady__c",
                "InstructionsDefaultAverage_lady__c",
                "Priority_EventItemManageAverage_lady__c",
                "ProductManageCallmentAverage_lady__c",
                "SumTotalScoreAverage_lady__c"
            )
        }
    }
}
