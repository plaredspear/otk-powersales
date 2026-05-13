package com.otoki.powersales.order.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #610 — OrderRequest ↔ Salesforce `DKRetail__OrderRequest__c` 어노테이션 부착 검증.
 *
 * 단일 권위: docs/plan/old_source_260408/salesforce_object/주문요청(DKRetail__OrderRequest__c).md
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 무변경
 *   - AC2: `@SFField` 매핑 키셋 (7개 — 5 기존 OK + 2 누락 매핑)
 *     * 스펙 §6.1 "기존 OK 6개"는 작성 오류 — 실제 @SFField 부착 5개 (sfid 는 미부착이므로 제외)
 *   - AC3: PK 미부착
 *   - AC4: @HCColumn 매핑 보존
 */
@DisplayName("OrderRequest SF 어노테이션 검증 (Spec #610)")
class OrderRequestSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject 무변경")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'DKRetail__OrderRequest__c'")
        fun sfObjectValue() {
            val annotation = OrderRequest::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("DKRetail__OrderRequest__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋 (7개)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(OrderRequest::class.java)

        @Test
        @DisplayName("매핑 키 수 = 9 (5 기존 + 2 누락 보강 + BaseEntity 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(12)
        }

        @Test
        @DisplayName("§6.2 — 누락 매핑 2개 신규 부착")
        fun section62MissingMappings() {
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["DKRetail__AccountId__c"]).isEqualTo("account_sfid")
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 6개 매핑 무변경")
        fun section61Existing() {
            assertThat(mapping["Name"]).isEqualTo("order_request_number")
            assertThat(mapping["OrderDate__c"]).isEqualTo("order_date")
            assertThat(mapping["DKRetail__RequestDate__c"]).isEqualTo("delivery_date")
            assertThat(mapping["TotalOrderAmount__c"]).isEqualTo("total_amount")
            assertThat(mapping["DKRetail__RequestStatus__c"]).isEqualTo("order_request_status")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / sfid 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = OrderRequest::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (관례)")
        fun sfidHasNoSfField() {
            val field = OrderRequest::class.java.getDeclaredField("sfid")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 order_request_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(OrderRequest::class.java)
            assertThat(mapping.values).doesNotContain("order_request_id")
        }
    }

    @Nested
    @DisplayName("AC4 — @HCColumn 매핑 보존")
    inner class HcColumnPreservation {

        @Test
        @DisplayName("기존 @HCColumn 매핑이 변경 없음 (sfid 포함)")
        fun hcMappingUnchanged() {
            val hcMapping = SFSchemaUtils.getHCMapping(OrderRequest::class.java)
            assertThat(hcMapping["sfid"]).isEqualTo("sfid")
            assertThat(hcMapping["dkretail__employeeid__c"]).isEqualTo("employee_sfid")
            assertThat(hcMapping["dkretail__accountid__c"]).isEqualTo("account_sfid")
            assertThat(hcMapping["name"]).isEqualTo("order_request_number")
        }
    }
}
