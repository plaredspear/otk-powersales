package com.otoki.powersales._migration.heroku.service

import com.otoki.powersales._migration.heroku.stage1.HerokuStage1Targets
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * HerokuFkResolveTables — Stage 2 FK resolve 매핑 SoT 정합 검증 (스펙 #853 P2-B).
 *
 * DB 의존 없는 순수 단위 테스트. 패턴 A/B 매핑이 실제 적재 테이블의 컬럼과 정합하는지,
 * 참조/부모 매칭 키가 2026-06-06 레거시 검증 결과(account.external_key / product.product_code)와
 * 일치하는지 확인.
 */
@DisplayName("HerokuFkResolveTables — 패턴 A/B 매핑 SoT")
class HerokuFkResolveTablesTest {

    @Nested
    @DisplayName("패턴 A — 자연 키 → serial id")
    inner class NaturalKey {

        @Test
        @DisplayName("account_code / sap_account_code 는 account.external_key 매칭 (레거시 검증 정합)")
        fun accountExternalKey() {
            val accountFks = HerokuFkResolveTables.NATURAL_KEY_FK.filter { it.fkColumn == "account_id" }
            assertThat(accountFks).isNotEmpty
            accountFks.forEach {
                assertThat(it.refTable).isEqualTo("account")
                assertThat(it.refKeyColumn)
                    .withFailMessage("account 매칭 키는 external_key (SAP Account Code) 여야 함")
                    .isEqualTo("external_key")
                assertThat(it.refIdColumn).isEqualTo("account_id")
            }
            // tmp_claim/tmp_onsite 는 sap_account_code 컬럼 사용
            val sourceColumns = accountFks.map { "${it.sourceTable}:${it.sourceColumn}" }
            assertThat(sourceColumns).contains(
                "tmp_claim:sap_account_code",
                "tmp_onsite:sap_account_code",
                "tmp_order:account_code",
                "tmp_suggest:account_code",
            )
        }

        @Test
        @DisplayName("product_code 는 product.product_code 매칭")
        fun productCode() {
            val productFks = HerokuFkResolveTables.NATURAL_KEY_FK.filter { it.fkColumn == "product_id" }
            assertThat(productFks).isNotEmpty
            productFks.forEach {
                assertThat(it.refTable).isEqualTo("product")
                assertThat(it.refKeyColumn).isEqualTo("product_code")
            }
            // tmp_promotion 은 promotion_product_code 컬럼 사용
            assertThat(productFks.map { "${it.sourceTable}:${it.sourceColumn}" })
                .contains("tmp_promotion:promotion_product_code", "tmp_order_product:product_code")
        }

        @Test
        @DisplayName("employee_code / emp_code 는 employee.employee_code 매칭")
        fun employeeCode() {
            val employeeFks = HerokuFkResolveTables.NATURAL_KEY_FK.filter { it.fkColumn == "employee_id" }
            assertThat(employeeFks).isNotEmpty
            employeeFks.forEach {
                assertThat(it.refTable).isEqualTo("employee")
                assertThat(it.refKeyColumn).isEqualTo("employee_code")
            }
            // education_post / education_view_history 는 emp_code 컬럼
            assertThat(employeeFks.map { "${it.sourceTable}:${it.sourceColumn}" })
                .contains("education_post:emp_code", "education_view_history:emp_code")
        }

        @Test
        @DisplayName("EmployeeInfo 는 패턴 A 목록에 없음 (Stage1 적재 시점 PK resolve)")
        fun employeeInfoExcluded() {
            assertThat(HerokuFkResolveTables.NATURAL_KEY_FK.map { it.sourceTable })
                .doesNotContain("employee_info")
        }
    }

    @Nested
    @DisplayName("패턴 B — 부모 FK")
    inner class ParentFk {

        @Test
        @DisplayName("tmp_order_product → tmp_order (employee_code 1:1 매칭)")
        fun tmpOrderProduct() {
            val m = HerokuFkResolveTables.PARENT_FK.first { it.sourceTable == "tmp_order_product" }
            assertThat(m.fkColumn).isEqualTo("tmp_order_id")
            assertThat(m.parentTable).isEqualTo("tmp_order")
            assertThat(m.sourceColumn).isEqualTo("employee_code")
            assertThat(m.parentKeyColumn).isEqualTo("employee_code")
            assertThat(m.parentIdColumn).isEqualTo("tmp_order_id")
        }

        @Test
        @DisplayName("education_post_attachment / education_view_history → education_post (edu_id)")
        fun educationChildren() {
            val children = HerokuFkResolveTables.PARENT_FK.filter { it.parentTable == "education_post" }
            assertThat(children.map { it.sourceTable })
                .containsExactlyInAnyOrder("education_post_attachment", "education_view_history")
            children.forEach {
                assertThat(it.fkColumn).isEqualTo("education_post_id")
                assertThat(it.sourceColumn).isEqualTo("edu_id")
                assertThat(it.parentKeyColumn).isEqualTo("edu_id")
                assertThat(it.parentIdColumn).isEqualTo("education_post_id")
            }
        }
    }

    @Nested
    @DisplayName("적재 테이블 정합 — 패턴 A/B 의 sourceTable 은 Stage1 적재 대상")
    inner class SourceTableConsistency {

        @Test
        @DisplayName("모든 sourceTable 은 HerokuStage1Targets 의 tableName 에 존재")
        fun allSourceTablesAreStage1Targets() {
            val stage1Tables = HerokuStage1Targets.list()
                .mapNotNull { HerokuStage1Targets.get(it)?.tableName }
                .toSet()
            val sourceTables =
                (HerokuFkResolveTables.NATURAL_KEY_FK.map { it.sourceTable } +
                    HerokuFkResolveTables.PARENT_FK.map { it.sourceTable }).toSet()
            val orphans = sourceTables - stage1Tables
            assertThat(orphans)
                .withFailMessage("FK resolve 대상이지만 Stage1 적재 대상 아님: %s", orphans)
                .isEmpty()
        }
    }
}
