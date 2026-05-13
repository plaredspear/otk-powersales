package com.otoki.powersales.schedule.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #608 — DisplayWorkSchedule ↔ Salesforce `DKRetail__DisplayWorkScheduleMaster__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/진열사원 스케줄 마스터(DisplayWorkScheduleMaster__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (13개 — 12 기존 + 1 누락 매핑)
 *   - AC3: PK 미부착
 */
@DisplayName("DisplayWorkSchedule SF 어노테이션 검증 (Spec #608)")
class DisplayWorkScheduleSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DisplayWorkScheduleMaster__c'")
        fun sfObjectValue() {
            val annotation = DisplayWorkSchedule::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DisplayWorkScheduleMaster__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (Spec #732 — Group A R-2 추가)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(DisplayWorkSchedule::class.java)

        @Test
        @DisplayName("매핑 키 수 = 17 (12 기존 + 1 누락 매핑 + BaseEntity 2 + Spec #732 R-2 CreatedBy/LastModifiedBy 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(18)
        }

        @Test
        @DisplayName("Spec #732 — Group A R-2 (CreatedById/LastModifiedById) 매핑 포함")
        fun spec732R2Mapping() {
            assertThat(mapping["CreatedById"]).isEqualTo("created_by_sfid")
            assertThat(mapping["LastModifiedById"]).isEqualTo("last_modified_by_sfid")
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 1개 신규 부착: TypeOfWork4__c → type_of_work4")
        fun section62MissingMapping() {
            assertThat(mapping["TypeOfWork4__c"]).isEqualTo("type_of_work4")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 12개 매핑 무변경 샘플")
        fun section61ExistingSample() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Account__c"]).isEqualTo("account_sfid")
            assertThat(mapping["FullName__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Confirmed__c"]).isEqualTo("confirmed")
            assertThat(mapping["TypeOfWork1__c"]).isEqualTo("type_of_work1")
            assertThat(mapping["TypeOfWork3__c"]).isEqualTo("type_of_work3")
            assertThat(mapping["TypeOfWork5__c"]).isEqualTo("type_of_work5")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["LastMonthRevenue__c"]).isEqualTo("last_month_revenue")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = DisplayWorkSchedule::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례 — HCColumn 만)")
        fun sfidHasNoSfField() {
            val field = DisplayWorkSchedule::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 display_work_schedule_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(DisplayWorkSchedule::class.java)
            assertThat(mapping.values).doesNotContain("display_work_schedule_id")
        }
    }
}
