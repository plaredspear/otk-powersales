package com.otoki.internal.common.salesforce

import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SFSchemaUtils 테스트")
class SFSchemaUtilsTest {

    @Nested
    @DisplayName("getSFMapping - SF Field 매핑 추출")
    inner class GetSFMappingTests {

        @Test
        @DisplayName("Account 엔티티 - 22개 SF Field 매핑 반환")
        fun getSFMapping_account() {
            val mapping = SFSchemaUtils.getSFMapping(Account::class.java)

            assertThat(mapping).hasSize(22)
            assertThat(mapping["ABCType__c"]).isEqualTo("abc_type")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Industry"]).isEqualTo("industry")
            assertThat(mapping["WERK1_TX__c"]).isEqualTo("werk1_tx")
        }

        @Test
        @DisplayName("User 엔티티 - 13개 SF Field 매핑 반환")
        fun getSFMapping_user() {
            val mapping = SFSchemaUtils.getSFMapping(User::class.java)

            assertThat(mapping).hasSize(13)
            assertThat(mapping["DKRetail__EmpCode__c"]).isEqualTo("employee_id")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__Birthdate__c"]).isEqualTo("birth_date")
            assertThat(mapping["DKRetail__Status__c"]).isEqualTo("status")
            assertThat(mapping["DKRetail__APPLoginActive__c"]).isEqualTo("app_login_active")
            assertThat(mapping["DKRetail__AppAuthority__c"]).isEqualTo("app_authority")
            assertThat(mapping["DKRetail__OrgName__c"]).isEqualTo("org_name")
            assertThat(mapping["CostCenterCode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["DKRetail__WorkPhone__c"]).isEqualTo("work_phone")
            assertThat(mapping["Phone__c"]).isEqualTo("phone")
            assertThat(mapping["DKRetail__HomePhone__c"]).isEqualTo("home_phone")
            assertThat(mapping["DKRetail__StartDate__c"]).isEqualTo("start_date")
            assertThat(mapping["AgreementFlag__c"]).isEqualTo("agreement_flag")
        }

        @Test
        @DisplayName("Product 엔티티 - 34개 SF Field 매핑 반환")
        fun getSFMapping_product() {
            val mapping = SFSchemaUtils.getSFMapping(Product::class.java)

            assertThat(mapping).hasSize(34)
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["DKRetail__ProductCode__c"]).isEqualTo("product_code")
            assertThat(mapping["DKRetail__StoreCondition__c"]).isEqualTo("storage_condition")
            assertThat(mapping["DKRetail__Category1__c"]).isEqualTo("category1")
            assertThat(mapping["StandardPrice__c"]).isEqualTo("standard_price")
            assertThat(mapping["ImgRefPath__c"]).isEqualTo("img_ref_path")
        }
    }

    @Nested
    @DisplayName("getHCMapping - HC Column 매핑 추출")
    inner class GetHCMappingTests {

        @Test
        @DisplayName("Account 엔티티 - 25개 HC Column 매핑 반환")
        fun getHCMapping_account() {
            val mapping = SFSchemaUtils.getHCMapping(Account::class.java)

            assertThat(mapping).hasSize(25)
            assertThat(mapping["sfid"]).isEqualTo("sfid")
            assertThat(mapping["isdeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["name"]).isEqualTo("name")
        }

        @Test
        @DisplayName("User 엔티티 - 20개 HC Column 매핑 반환")
        fun getHCMapping_user() {
            val mapping = SFSchemaUtils.getHCMapping(User::class.java)

            assertThat(mapping).hasSize(20)
            assertThat(mapping["id"]).isEqualTo("id")
            assertThat(mapping["sfid"]).isEqualTo("sfid")
            assertThat(mapping["dkretail__empcode__c"]).isEqualTo("employee_id")
            assertThat(mapping["name"]).isEqualTo("name")
            assertThat(mapping["dkretail__birthdate__c"]).isEqualTo("birth_date")
            assertThat(mapping["dkretail__status__c"]).isEqualTo("status")
            assertThat(mapping["dkretail__apploginactive__c"]).isEqualTo("app_login_active")
            assertThat(mapping["dkretail__appauthority__c"]).isEqualTo("app_authority")
            assertThat(mapping["dkretail__orgname__c"]).isEqualTo("org_name")
            assertThat(mapping["costcentercode__c"]).isEqualTo("cost_center_code")
            assertThat(mapping["dkretail__workphone__c"]).isEqualTo("work_phone")
            assertThat(mapping["phone__c"]).isEqualTo("phone")
            assertThat(mapping["dkretail__homephone__c"]).isEqualTo("home_phone")
            assertThat(mapping["dkretail__startdate__c"]).isEqualTo("start_date")
            assertThat(mapping["agreementflag__c"]).isEqualTo("agreement_flag")
            assertThat(mapping["isdeleted"]).isEqualTo("isdeleted")
            assertThat(mapping["systemmodstamp"]).isEqualTo("systemmodstamp")
            assertThat(mapping["createddate"]).isEqualTo("created_date")
            assertThat(mapping["_hc_lastop"]).isEqualTo("_hc_lastop")
            assertThat(mapping["_hc_err"]).isEqualTo("_hc_err")
        }

        @Test
        @DisplayName("Product 엔티티 - 41개 HC Column 매핑 반환")
        fun getHCMapping_product() {
            val mapping = SFSchemaUtils.getHCMapping(Product::class.java)

            assertThat(mapping).hasSize(41)
            assertThat(mapping["name"]).isEqualTo("name")
            assertThat(mapping["sfid"]).isEqualTo("sfid")
            assertThat(mapping["dkretail__productcode__c"]).isEqualTo("product_code")
            assertThat(mapping["dkretail__storecondition__c"]).isEqualTo("storage_condition")
            assertThat(mapping["isdeleted"]).isEqualTo("is_deleted")
            assertThat(mapping["systemmodstamp"]).isEqualTo("system_mod_stamp")
            assertThat(mapping["_hc_lastop"]).isEqualTo("_hc_lastop")
        }
    }

    @Nested
    @DisplayName("generateImportSql - Import SQL 생성")
    inner class GenerateImportSqlTests {

        @Test
        @DisplayName("Account 엔티티 - SELECT FROM salesforce.account 형태 SQL 반환")
        fun generateImportSql_account() {
            val sql = SFSchemaUtils.generateImportSql(Account::class.java)

            assertThat(sql).startsWith("SELECT ")
            assertThat(sql).contains("FROM salesforce.account")
            // 25개 HC 컬럼이 포함되어야 함
            assertThat(sql).contains("sfid")
            assertThat(sql).contains("name")
        }

        @Test
        @DisplayName("HCTable 미존재 - IllegalArgumentException 발생")
        fun generateImportSql_noHCTable() {
            assertThatThrownBy { SFSchemaUtils.generateImportSql(NoHCTableEntity::class.java) }
                .isInstanceOf(IllegalArgumentException::class.java)
        }
    }

    // @HCTable 없는 테스트용 클래스
    private class NoHCTableEntity
}
