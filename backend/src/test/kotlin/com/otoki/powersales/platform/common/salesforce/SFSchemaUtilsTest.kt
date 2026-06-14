package com.otoki.powersales.platform.common.salesforce

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.platform.common.salesforce.SFSchemaUtils
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SFSchemaUtils 테스트")
class SFSchemaUtilsTest {

    @Nested
    @DisplayName("getSFMapping - SF Field 매핑 추출")
    inner class GetSFMappingTests {

        @Test
        @DisplayName("Account 엔티티 - 69개 SF Field 매핑 반환 (Spec #602/#703: 22 기존 + 17 SAP 보존 + 23 신규 + #644 OwnerId + #703 Group A 3 + BaseEntity 2 + SF prod 정합 IsPriorityRecord 1)")
        fun getSFMapping_account() {
            val mapping = SFSchemaUtils.getSFMapping(Account::class.java)

            assertThat(mapping).hasSize(69)
            assertThat(mapping["ABCType__c"]).isEqualTo("abc_type")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Industry"]).isEqualTo("industry")
            assertThat(mapping["WERK1_TX__c"]).isEqualTo("werk1_tx")
            assertThat(mapping["CreatedDate"]).isEqualTo("created_at")
            assertThat(mapping["LastModifiedDate"]).isEqualTo("updated_at")
        }

        @Test
        @DisplayName("Employee 엔티티 - 43개 SF Field 매핑 반환 (Spec #607 38 + Spec #713 신규 6 − SF prod 부재 prn_flag 1)")
        fun getSFMapping_user() {
            val mapping = SFSchemaUtils.getSFMapping(Employee::class.java)

            assertThat(mapping).hasSize(43)
            assertThat(mapping["DKRetail__EmpCode__c"]).isEqualTo("employee_code")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__Birthdate__c"]).isEqualTo("birth_date")
            assertThat(mapping["DKRetail__Status__c"]).isEqualTo("status")
            assertThat(mapping["DKRetail__APPLoginActive__c"]).isEqualTo("app_login_active")
            assertThat(mapping["DKRetail__AppAuthority__c"]).isEqualTo("role")
            assertThat(mapping["DKRetail__OrgName__c"]).isEqualTo("org_name")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["DKRetail__WorkPhone__c"]).isEqualTo("work_phone")
            assertThat(mapping["Phone__c"]).isEqualTo("phone")
            assertThat(mapping["DKRetail__HomePhone__c"]).isEqualTo("home_phone")
            assertThat(mapping["DKRetail__StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["AgreementFlag__c"]).isEqualTo("agreement_flag")
        }

        @Test
        @DisplayName("TeamMemberSchedule 엔티티 - 48개 SF Field 매핑 반환 (Spec #762: Formula 7건 제거 + Spec #849: DKRetail__AccountId__c 부활 1건)")
        fun getSFMapping_teamMemberSchedule() {
            val mapping = SFSchemaUtils.getSFMapping(TeamMemberSchedule::class.java)

            assertThat(mapping).hasSize(48)
            assertThat(mapping["DisplayWorkScheduleMaster__c"]).isEqualTo("display_work_schedule_sfid")
            assertThat(mapping["DKRetail__EmployeeId__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__WorkingDate__c"]).isEqualTo("working_date")
            assertThat(mapping["DKRetail__WorkingType__c"]).isEqualTo("working_type")
            assertThat(mapping["DKRetail__WorkingCategory1__c"]).isEqualTo("working_category1")
            assertThat(mapping["DKRetail__WorkingCategory2__c"]).isEqualTo("working_category2")
            assertThat(mapping["DKRetail__WorkingCategory3__c"]).isEqualTo("working_category3")
            assertThat(mapping["WorkingCategory4__c"]).isEqualTo("working_category4")
            assertThat(mapping["AccountId__c"]).isEqualTo("account_sfid")
            assertThat(mapping["teamleadersfid__c"]).isEqualTo("team_leader_sfid")
            assertThat(mapping["DKRetail__AltHolidayId__c"]).isEqualTo("alt_holiday_sfid")
            assertThat(mapping["DKRetail__CommuteLogId__c"]).isEqualTo("commute_log_sfid")
            assertThat(mapping["DKRetail__PromotionEmpId__c"]).isEqualTo("promotion_employee_sfid")
            assertThat(mapping["CommuteReportDateTime__c"]).isEqualTo("commute_report_datetime")
            assertThat(mapping["ID__c"]).isEqualTo("id_field")
            assertThat(mapping["TraversalFlag__c"]).isEqualTo("traversal_flag")
            assertThat(mapping["Equipment1__c"]).isEqualTo("equipment1")
            assertThat(mapping["Equipment2__c"]).isEqualTo("equipment2")
            assertThat(mapping["Equipment3__c"]).isEqualTo("equipment3")
            assertThat(mapping["Equipment4__c"]).isEqualTo("equipment4")
            assertThat(mapping["Equipment5__c"]).isEqualTo("equipment5")
            assertThat(mapping["Equipment6__c"]).isEqualTo("equipment6")
            assertThat(mapping["Equipment7__c"]).isEqualTo("equipment7")
            assertThat(mapping["Equipment8__c"]).isEqualTo("equipment8")
            assertThat(mapping["Equipment9__c"]).isEqualTo("equipment9")
            assertThat(mapping["Equipment10__c"]).isEqualTo("equipment10")
            assertThat(mapping["Yes_ChkCnt__c"]).isEqualTo("yes_chk_cnt")
            assertThat(mapping["No_ChkCnt__c"]).isEqualTo("no_chk_cnt")
            assertThat(mapping["precaution_chk__c"]).isEqualTo("precaution_chk")
            assertThat(mapping["precaution__c"]).isEqualTo("precaution")
            assertThat(mapping["StartTime__c"]).isEqualTo("start_time")
            assertThat(mapping["CompleteTime__c"]).isEqualTo("complete_time")
        }

        @Test
        @DisplayName("DisplayWorkSchedule 엔티티 - 18개 SF Field 매핑 반환 (sf-meta-diff — ConfirmationAlert__c Formula 컬럼 제거)")
        fun getSFMapping_displayWorkSchedule() {
            val mapping = SFSchemaUtils.getSFMapping(DisplayWorkSchedule::class.java)

            assertThat(mapping).hasSize(18)
            assertThat(mapping["Account__c"]).isEqualTo("account_sfid")
            assertThat(mapping["FullName__c"]).isEqualTo("employee_sfid")
            assertThat(mapping["StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["EndDate__c"]).isEqualTo("end_date")
            assertThat(mapping["Confirmed__c"]).isEqualTo("confirmed")
            assertThat(mapping["TypeOfWork1__c"]).isEqualTo("type_of_work1")
            assertThat(mapping["TypeOfWork3__c"]).isEqualTo("type_of_work3")
            assertThat(mapping["TypeOfWork5__c"]).isEqualTo("type_of_work5")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["OwnerId"]).isEqualTo("owner_sfid")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["LastMonthRevenue__c"]).isEqualTo("last_month_revenue")
        }

        @Test
        @DisplayName("Product 엔티티 - 45개 SF Field 매핑 반환 (sf-meta-diff Q15: StandardPrice__c + BoxReceivingQuantity__c Formula 제거)")
        fun getSFMapping_product() {
            val mapping = SFSchemaUtils.getSFMapping(Product::class.java)

            assertThat(mapping).hasSize(45)
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["DKRetail__StoreCondition__c"]).isEqualTo("storage_condition")
            assertThat(mapping["DKRetail__Category1__c"]).isEqualTo("category1")
            assertThat(mapping["DKRetail__StandardUnitPrice__c"]).isEqualTo("standard_unit_price")
            assertThat(mapping["ImgRefPath__c"]).isEqualTo("img_ref_path")
        }
    }

}
