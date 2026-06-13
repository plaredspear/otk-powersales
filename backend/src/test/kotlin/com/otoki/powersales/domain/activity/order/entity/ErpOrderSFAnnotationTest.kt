package com.otoki.powersales.domain.activity.order.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #624 — ErpOrder ↔ Salesforce `ERP_Order__c` 어노테이션 검증.
 *
 * 단일 권위: (외부 문서) 주문(ERP_Order__c).md
 */
@DisplayName("ErpOrder SF 어노테이션 검증 (Spec #624)")
class ErpOrderSFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'ERP_Order__c'")
        fun sfObjectValue() {
            val annotation = ErpOrder::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("ERP_Order__c")
        }
    }

    @Nested
    @DisplayName("AC1 — @SFField 매핑 키셋 (13개 = 기존 12 + 신규 1)")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(ErpOrder::class.java)

        @Test
        @DisplayName("매핑 키 수 = 18 (OwnerId prod 부재 제거 후)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(18)
        }

        @Test
        @DisplayName("§6.1 — 기존 OK 12개 매핑 무변경")
        fun section61ExistingMappings() {
            assertThat(mapping["Name"]).isEqualTo("sap_order_number")
            assertThat(mapping["SAPAccountCode__c"]).isEqualTo("sap_account_code")
            assertThat(mapping["SAPAccountName__c"]).isEqualTo("sap_account_name")
            assertThat(mapping["DeliveryRequestDate__c"]).isEqualTo("delivery_request_date")
            assertThat(mapping["OrderDate__c"]).isEqualTo("order_date")
            assertThat(mapping["EmployeeCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["EmployeeName__c"]).isEqualTo("employee_name")
            assertThat(mapping["TotalOrderAmount__c"]).isEqualTo("order_sales_amount")
            assertThat(mapping["OrderChannel__c"]).isEqualTo("order_channel")
            assertThat(mapping["OrderChannel_NM__c"]).isEqualTo("order_channel_nm")
            assertThat(mapping["OrderType__c"]).isEqualTo("order_type")
            assertThat(mapping["OrderType_NM__c"]).isEqualTo("order_type_nm")
        }

        @Test
        @DisplayName("§6.2 — 신규 도입 1개: AccountId__c → account_sfid")
        fun section62NewMapping() {
            assertThat(mapping["AccountId__c"]).isEqualTo("account_sfid")
        }
    }

    @Nested
    @DisplayName("AC1 — PK 미부착")
    inner class PkExclusion {

        @Test
        @DisplayName("PK(id) 필드에 @SFField 미부착")
        fun idHasNoSfField() {
            val field = ErpOrder::class.java.getDeclaredField("id")
            assertThat(field.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 erp_order_id 미등장")
        fun mappingValuesExcludePk() {
            val mapping = SFSchemaUtils.getSFMapping(ErpOrder::class.java)
            assertThat(mapping.values).doesNotContain("erp_order_id")
        }
    }
}
