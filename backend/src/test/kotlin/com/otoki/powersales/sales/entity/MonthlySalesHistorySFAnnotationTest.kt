package com.otoki.powersales.sales.entity

import com.otoki.powersales.common.salesforce.SFField
import com.otoki.powersales.common.salesforce.SFObject
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * Spec #601 — MonthlySalesHistory ↔ Salesforce `MonthlySalesHistory__c` 어노테이션 부착 검증.
 *
 * 단일 권위: Salesforce Object (`MonthlySalesHistory__c`)
 *
 * 검증 분류:
 *   - AC1: 클래스 `@SFObject` 부착
 *   - AC2: `@SFField` 매핑 키셋 (37개 — 18 기존 + 2 SAP 보존 + 17 신규)
 *   - AC3: PK / FK 미부착 단언
 */
@DisplayName("MonthlySalesHistory SF 어노테이션 검증 (Spec #601)")
class MonthlySalesHistorySFAnnotationTest {

    @Nested
    @DisplayName("AC1 — 클래스 @SFObject")
    inner class ClassAnnotation {

        @Test
        @DisplayName("@SFObject 값은 'MonthlySalesHistory__c'")
        fun sfObjectValue() {
            val annotation = MonthlySalesHistory::class.java.getAnnotation(SFObject::class.java)
            assertThat(annotation).isNotNull
            assertThat(annotation.value).isEqualTo("MonthlySalesHistory__c")
        }
    }

    @Nested
    @DisplayName("AC2 — @SFField 매핑 키셋")
    inner class SfFieldMapping {

        private val mapping = SFSchemaUtils.getSFMapping(MonthlySalesHistory::class.java)

        @Test
        @DisplayName("매핑 키 수 = 34 (HQReviews__c 1건 제거. 28 + R-2 3 + CreatedDate/LastModifiedDate 2)")
        fun mappingKeySize() {
            assertThat(mapping).hasSize(34)
        }

        @Test
        @DisplayName("§6.2 — 기존 10개 매칭 필드 SF API Name → 컬럼명 1:1 (Spec #740: Formula 8건 제거)")
        fun section62Existing() {
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["SalesYear__c"]).isEqualTo("sales_year")
            assertThat(mapping["SalesMonth__c"]).isEqualTo("sales_month")
            assertThat(mapping["LastMonthResults__c"]).isEqualTo("last_month_results")
            assertThat(mapping["ShipClosingAmount__c"]).isEqualTo("ship_closing_amount")
            assertThat(mapping["ABCClosingAmount1__c"]).isEqualTo("abc_closing_amount1")
            assertThat(mapping["ABCClosingAmount2__c"]).isEqualTo("abc_closing_amount2")
            assertThat(mapping["ABCClosingAmount3__c"]).isEqualTo("abc_closing_amount3")
            assertThat(mapping["AmbientPurpose__c"]).isEqualTo("ambient_purpose")
            assertThat(mapping["FridgePurpose__c"]).isEqualTo("fridge_purpose")
        }

        @Test
        @DisplayName("§6.3 — SAP 보존 2개 필드 매핑")
        fun section63SapPreserved() {
            assertThat(mapping["Externalkey__c"]).isEqualTo("external_key")
            assertThat(mapping["TotalLedgerAmount__c"]).isEqualTo("total_ledger_amount")
        }

        @Test
        @DisplayName("§6.4 — 신규 17개 필드 매핑")
        fun section64NewFields() {
            assertThat(mapping["AccountId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["SAPAccountCode__c"]).isEqualTo("sap_account_code")
            assertThat(mapping["SalesDate__c"]).isEqualTo("sales_date")
            assertThat(mapping["LastMonthlySalesHistory__c"]).isEqualTo("last_monthly_sales_history_sfid")
            assertThat(mapping["Confirm__c"]).isEqualTo("is_confirmed")
            assertThat(mapping["Remark__c"]).isEqualTo("remark")
            assertThat(mapping["ShipClosingAmountNH__c"]).isEqualTo("ship_closing_amount_nh")
            assertThat(mapping["ShipClosingAmount1__c"]).isEqualTo("ship_closing_amount1")
            assertThat(mapping["ShipClosingAmount2__c"]).isEqualTo("ship_closing_amount2")
            assertThat(mapping["ShipClosingAmount3__c"]).isEqualTo("ship_closing_amount3")
            assertThat(mapping["ShipClosingAmount4__c"]).isEqualTo("ship_closing_amount4")
            assertThat(mapping["ShipClosingSumAmount__c"]).isEqualTo("ship_closing_sum_amount")
            assertThat(mapping["ABCClosingAmount4__c"]).isEqualTo("abc_closing_amount4")
            assertThat(mapping["ABCClosingSumAmount__c"]).isEqualTo("abc_closing_sum_amount")
            assertThat(mapping["LastMonthTargetByHand__c"]).isEqualTo("last_month_target_by_hand")
            assertThat(mapping["ThisMonthTarget__c"]).isEqualTo("this_month_target")
        }
    }

    @Nested
    @DisplayName("AC3 — PK / FK 미부착")
    inner class PkFkExclusion {

        @Test
        @DisplayName("PK(id) / FK(account) 필드에 @SFField 미부착")
        fun pkAndFkHaveNoSfField() {
            val idField = MonthlySalesHistory::class.java.getDeclaredField("id")
            assertThat(idField.isAnnotationPresent(SFField::class.java)).isFalse()

            val accountField = MonthlySalesHistory::class.java.getDeclaredField("account")
            assertThat(accountField.isAnnotationPresent(SFField::class.java)).isFalse()
        }

        @Test
        @DisplayName("매핑 values 에 monthly_sales_history_id / account_id 미등장")
        fun mappingValuesExcludePkFk() {
            val mapping = SFSchemaUtils.getSFMapping(MonthlySalesHistory::class.java)
            assertThat(mapping.values).doesNotContain("monthly_sales_history_id", "account_id")
        }

        @Test
        @DisplayName("sfid 필드에 @SFField 미부착 (SOQL 자동 포함 정책)")
        fun sfidHasNoSfField() {
            val sfidField = MonthlySalesHistory::class.java.getDeclaredField("sfid")
            assertThat(sfidField.isAnnotationPresent(SFField::class.java)).isFalse()
        }
    }

}
