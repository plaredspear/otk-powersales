package com.otoki.powersales.employee.entity

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #607 — Employee ↔ Salesforce `DKRetail__Employee__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/사원(DKRetail__Employee__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (36개 — 25 기존 + 3 누락 매핑 + 8 신규)
 *   - AC3: PK 미부착
 *   - AC5: Picklist 정합 (Gender ↔ 남/여, UserRole ↔ 조장/여사원/지점장)
 */
@DisplayName("Employee SF 어노테이션 검증 (Spec #607)")
class EmployeeSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__Employee__c'")
        fun sfObjectValue() {
            val annotation = Employee::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__Employee__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(Employee::class.java)

        @Test
        @DisplayName("매핑 키 수 = 38 (25 기존 + 3 누락 + 8 신규 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(38)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 3개 신규 부착")
        fun section62MissingMappings() {
            assertThat(mapping["DKRetail__WorkEmail__c"]).isEqualTo("work_email")
            assertThat(mapping["DKRetail__Email__c"]).isEqualTo("email")
            assertThat(mapping["ProfessionalPromotionTeam__c"]).isEqualTo("professional_promotion_team")
        }

        @Test
        @DisplayName("§6.3 — 신규 8개 필드 매핑 (Q2 옵션 1: dkCostCenterCode 포함)")
        fun section63NewFields() {
            assertThat(mapping["DKRetail__CostCenterCode__c"]).isEqualTo("dk_cost_center_code")
            assertThat(mapping["DKRetail__LocationCode__c"]).isEqualTo("location_code")
            assertThat(mapping["DKRetail__TotalAnnualLeave__c"]).isEqualTo("total_annual_leave")
            assertThat(mapping["DKRetail__UsedAnnualLeave__c"]).isEqualTo("used_annual_leave")
            assertThat(mapping["DKRetail__ManagerId__c"]).isEqualTo("manager_sfid")
            assertThat(mapping["PostponedAppointment__c"]).isEqualTo("postponed_appointment_sfid")
            assertThat(mapping["LockingFlag__c"]).isEqualTo("locking_flag")
            assertThat(mapping["prnflag__c"]).isEqualTo("prn_flag")
        }

        @Test
        @DisplayName("기존 OK 25개 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["DKRetail__EmpCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__AppAuthority__c"]).isEqualTo("role")
            assertThat(mapping["DKRetail__Sex__c"]).isEqualTo("gender")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = Employee::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례 — HCColumn 만)")
        fun sfidHasNoSfField() {
            val field = Employee::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 employee_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(Employee::class.java)
            assertThat(mapping.values).doesNotContain("employee_id")
        }
    }

    @Nested
    @DisplayName("AC5 — Picklist 정합 검증 (Q3 옵션 1: 정합 단언만)")
    inner class PicklistConsistency {

        @Test
        @DisplayName("Gender displayName ↔ SF Sex picklist 한국어 (남/여) 1:1")
        fun genderSfMapping() {
            assertThat(Gender.MALE.displayName).isEqualTo("남")
            assertThat(Gender.FEMALE.displayName).isEqualTo("여")
        }

        @Test
        @DisplayName("UserRole.fromKorean 이 SF AppAuthority 4값 중 3개를 운영 enum 매핑 (조장/여사원/지점장)")
        fun userRoleSfMappingOperational() {
            assertThat(UserRole.fromKorean("조장")).isEqualTo(UserRole.LEADER)
            assertThat(UserRole.fromKorean("여사원")).isEqualTo(UserRole.WOMAN)
            assertThat(UserRole.fromKorean("지점장")).isEqualTo(UserRole.BRANCH_MANAGER)
        }

        @Test
        @DisplayName("UserRole.fromKorean(\"AccountViewAll\") → UNKNOWN (운영 enum 미매핑 — 후속 별도 스펙)")
        fun userRoleAccountViewAllNotMapped() {
            assertThat(UserRole.fromKorean("AccountViewAll")).isEqualTo(UserRole.UNKNOWN)
        }
    }
}
