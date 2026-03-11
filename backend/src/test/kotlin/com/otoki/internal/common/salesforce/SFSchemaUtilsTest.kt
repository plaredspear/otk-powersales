package com.otoki.internal.common.salesforce

import com.otoki.internal.sap.entity.Account
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
            assertThat(mapping["ABCType__c"]).isEqualTo("abctype__c")
            assertThat(mapping["Name"]).isEqualTo("name")
            assertThat(mapping["Industry"]).isEqualTo("industry")
            assertThat(mapping["WERK1_TX__c"]).isEqualTo("werk1_tx__c")
        }
    }

    @Nested
    @DisplayName("getHCMapping - HC Column 매핑 추출")
    inner class GetHCMappingTests {

        @Test
        @DisplayName("Account 엔티티 - 29개 HC Column 매핑 반환")
        fun getHCMapping_account() {
            val mapping = SFSchemaUtils.getHCMapping(Account::class.java)

            assertThat(mapping).hasSize(29)
            assertThat(mapping["sfid"]).isEqualTo("sfid")
            assertThat(mapping["_hc_lastop"]).isEqualTo("_hc_lastop")
            assertThat(mapping["isdeleted"]).isEqualTo("isdeleted")
            assertThat(mapping["name"]).isEqualTo("name")
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
            // 29개 HC 컬럼이 포함되어야 함
            assertThat(sql).contains("sfid")
            assertThat(sql).contains("name")
            assertThat(sql).contains("_hc_lastop")
            assertThat(sql).contains("_hc_err")
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
