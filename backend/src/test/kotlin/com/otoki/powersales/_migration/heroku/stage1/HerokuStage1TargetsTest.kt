package com.otoki.powersales._migration.heroku.stage1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * HerokuStage1Targets — `@HerokuOnly` + `@HCColumn` 리플렉션 메타 자동 생성 검증 (스펙 #853 P1-B §3).
 *
 * DB/S3 의존 없는 순수 단위 테스트. 리플렉션이 어노테이션 단일 출처(SoT)에서 적재 메타를 올바로
 * 도출하는지, ProductSyncBuffer 제외 + 의존성 순서 + csvFileName 규약을 확인.
 */
@DisplayName("HerokuStage1Targets — 리플렉션 메타 자동 생성")
class HerokuStage1TargetsTest {

    @Nested
    @DisplayName("적재 대상 등록")
    inner class TargetRegistration {

        @Test
        @DisplayName("18개 target 등록 (ProductSyncBuffer + DeviceVersion 제외)")
        fun targetCount() {
            assertThat(HerokuStage1Targets.list()).hasSize(18)
        }

        @Test
        @DisplayName("ProductSyncBuffer (if_product__c) 는 미등록 — §1.1 제외")
        fun productSyncBufferExcluded() {
            assertThat(HerokuStage1Targets.get("ProductSyncBuffer")).isNull()
            assertThat(HerokuStage1Targets.list()).doesNotContain("ProductSyncBuffer")
        }

        @Test
        @DisplayName("주요 entity 모두 등록")
        fun coreEntitiesRegistered() {
            listOf(
                "TmpOrder", "TmpOrderProduct", "TmpClaim", "TmpSuggest", "TmpOnsite", "TmpPromotion",
                "TmpClaimCode", "EducationCode", "EducationPost", "EducationPostAttachment",
                "EducationViewHistory", "SafetyCheckItem", "SafetyCheckSubmission", "FavoriteProduct",
                "LoginHistory", "EmployeeAdmin", "ProductExpiration", "EmployeeInfo",
            ).forEach { name ->
                assertThat(HerokuStage1Targets.get(name)).withFailMessage("%s 미등록", name).isNotNull
            }
        }
    }

    @Nested
    @DisplayName("리플렉션 메타 추출")
    inner class ReflectionMeta {

        @Test
        @DisplayName("tableName 은 @Table value")
        fun tableName() {
            assertThat(HerokuStage1Targets.get("TmpOrder")!!.tableName).isEqualTo("tmp_order")
            assertThat(HerokuStage1Targets.get("EducationPost")!!.tableName).isEqualTo("education_post")
            assertThat(HerokuStage1Targets.get("EmployeeInfo")!!.tableName).isEqualTo("employee_info")
        }

        @Test
        @DisplayName("csvFileName 은 @HerokuOnly value + .csv (Heroku 원본 테이블명)")
        fun csvFileName() {
            assertThat(HerokuStage1Targets.get("EmployeeInfo")!!.csvFileName).isEqualTo("employee_mng.csv")
            assertThat(HerokuStage1Targets.get("EducationPost")!!.csvFileName).isEqualTo("education_mng.csv")
            assertThat(HerokuStage1Targets.get("LoginHistory")!!.csvFileName).isEqualTo("employee_his.csv")
            assertThat(HerokuStage1Targets.get("TmpClaimCode")!!.csvFileName).isEqualTo("tmp_claimcode.csv")
            assertThat(HerokuStage1Targets.get("SafetyCheckSubmission")!!.csvFileName)
                .isEqualTo("safetycheck__workschedule__member.csv")
        }

        @Test
        @DisplayName("컬럼 매핑 = @HCColumn value(CSV 헤더) ↔ @Column name(신규 컬럼)")
        fun columnMapping() {
            val tmpOrder = HerokuStage1Targets.get("TmpOrder")!!
            val byHeroku = tmpOrder.columns.associate { it.herokuColumn to it.dbColumn }
            assertThat(byHeroku["tmp_employeecode"]).isEqualTo("employee_code")
            assertThat(byHeroku["tmp_accountcode"]).isEqualTo("account_code")
            assertThat(byHeroku["tmp_orderdate"]).isEqualTo("order_date")
        }

        @Test
        @DisplayName("FK *_id 컬럼 (@HCColumn 없음) 은 매핑 제외 — Stage1 NULL 적재")
        fun fkColumnsExcluded() {
            val tmpOrder = HerokuStage1Targets.get("TmpOrder")!!
            val dbColumns = tmpOrder.columns.map { it.dbColumn }
            // employee_id / account_id 는 @HCColumn 없는 FK → 매핑에서 빠져야 함
            assertThat(dbColumns).doesNotContain("employee_id", "account_id")
            // 자연 키 employee_code / account_code 는 포함
            assertThat(dbColumns).contains("employee_code", "account_code")
        }

        @Test
        @DisplayName("serial PK (@Id @GeneratedValue, @HCColumn 없음) 은 매핑 제외")
        fun serialPkExcluded() {
            val tmpOrder = HerokuStage1Targets.get("TmpOrder")!!
            assertThat(tmpOrder.columns.map { it.dbColumn }).doesNotContain("tmp_order_id")
        }

        @Test
        @DisplayName("AuditedEntity 의 inst_date/upd_date 는 created_at/updated_at 으로 매핑")
        fun auditedEntityTimestamps() {
            val tmpOrder = HerokuStage1Targets.get("TmpOrder")!!
            val byHeroku = tmpOrder.columns.associate { it.herokuColumn to it.dbColumn }
            assertThat(byHeroku["inst_date"]).isEqualTo("created_at")
            assertThat(byHeroku["upd_date"]).isEqualTo("updated_at")
        }

        @Test
        @DisplayName("BaseEntity 상속 entity 는 부모의 createddate/lastmodifieddate 도 수집")
        fun baseEntityInheritedTimestamps() {
            // EducationCode 는 BaseEntity 상속 (createdAt @HCColumn(createddate), updatedAt @HCColumn(lastmodifieddate))
            val educationCode = HerokuStage1Targets.get("EducationCode")!!
            val herokuCols = educationCode.columns.map { it.herokuColumn }
            assertThat(herokuCols).contains("createddate", "lastmodifieddate")
        }

        @Test
        @DisplayName("EmployeeInfo password/pwd_yn 매핑 (Q2 raw 적재)")
        fun employeeInfoPassword() {
            val ei = HerokuStage1Targets.get("EmployeeInfo")!!
            val byHeroku = ei.columns.associate { it.herokuColumn to it.dbColumn }
            assertThat(byHeroku["emp_pwd"]).isEqualTo("password")
            assertThat(byHeroku["pwd_yn"]).isEqualTo("password_change_required")
        }

        @Test
        @DisplayName("EmployeeInfo fcm_token(emp_token) / device_uuid(emp_uuid) 는 마이그레이션 제외 — @HCColumn 이 있어도 매핑 없음 (PII/보안)")
        fun employeeInfoPiiExcluded() {
            val ei = HerokuStage1Targets.get("EmployeeInfo")!!
            assertThat(ei.columns.map { it.dbColumn }).doesNotContain("fcm_token", "device_uuid")
            assertThat(ei.columns.map { it.herokuColumn }).doesNotContain("emp_token", "emp_uuid")
            // 다른 EmployeeInfo 매핑은 정상 유지 (제외가 PII 컬럼에만 적용)
            assertThat(ei.columns.map { it.dbColumn }).contains("password", "employee_code")
        }

        @Test
        @DisplayName("EducationViewHistory costcentercode__c → cost_center_code 매핑 (선행 작업 정합)")
        fun educationViewHistoryCostCenterCode() {
            val evh = HerokuStage1Targets.get("EducationViewHistory")!!
            val byHeroku = evh.columns.associate { it.herokuColumn to it.dbColumn }
            assertThat(byHeroku["costcentercode__c"]).isEqualTo("cost_center_code")
        }
    }

    @Nested
    @DisplayName("EmployeeInfo 공유 PK resolve 선행")
    inner class PkResolve {

        @Test
        @DisplayName("EmployeeInfo 는 requiresPkResolve = true")
        fun employeeInfoRequiresPkResolve() {
            assertThat(HerokuStage1Targets.get("EmployeeInfo")!!.requiresPkResolve).isTrue
        }

        @Test
        @DisplayName("다른 entity 는 requiresPkResolve = false")
        fun othersDoNotRequirePkResolve() {
            listOf("TmpOrder", "EducationPost", "LoginHistory").forEach { name ->
                assertThat(HerokuStage1Targets.get(name)!!.requiresPkResolve)
                    .withFailMessage("%s 는 PK resolve 불요", name).isFalse
            }
        }

        @Test
        @DisplayName("EmployeeInfo 의 PK(employee_id)는 매핑 제외 — JOIN INSERT 로 채움")
        fun employeeInfoPkNotMapped() {
            val ei = HerokuStage1Targets.get("EmployeeInfo")!!
            assertThat(ei.columns.map { it.dbColumn }).doesNotContain("employee_id")
            // 자연 키 employee_code 는 포함 (JOIN 키)
            assertThat(ei.columns.map { it.dbColumn }).contains("employee_code")
        }
    }

    @Nested
    @DisplayName("의존성 순서")
    inner class DependencyOrder {

        @Test
        @DisplayName("EmployeeInfo 는 EducationPost/Tmp* 보다 앞 (employee 적재 후 즉시 PK resolve)")
        fun employeeInfoEarly() {
            val order = HerokuStage1Targets.list()
            assertThat(order.indexOf("EmployeeInfo")).isLessThan(order.indexOf("EducationPost"))
            assertThat(order.indexOf("EmployeeInfo")).isLessThan(order.indexOf("TmpOrder"))
        }

        @Test
        @DisplayName("부모가 자식보다 먼저 — EducationPost < EducationPostAttachment/EducationViewHistory")
        fun parentBeforeChild() {
            val order = HerokuStage1Targets.list()
            assertThat(order.indexOf("EducationPost")).isLessThan(order.indexOf("EducationPostAttachment"))
            assertThat(order.indexOf("EducationPost")).isLessThan(order.indexOf("EducationViewHistory"))
            assertThat(order.indexOf("TmpOrder")).isLessThan(order.indexOf("TmpOrderProduct"))
        }
    }

    @Nested
    @DisplayName("listWithCsv — SINGLE 모드 파일명 자동조립 메타")
    inner class ListWithCsv {

        @Test
        @DisplayName("list() 와 동일한 순서/개수")
        fun sameOrder() {
            assertThat(HerokuStage1Targets.listWithCsv().map { it.targetName })
                .isEqualTo(HerokuStage1Targets.list())
        }

        @Test
        @DisplayName("각 csvFileName 은 get(targetName).csvFileName 과 일치")
        fun csvFileNameMatches() {
            HerokuStage1Targets.listWithCsv().forEach { tc ->
                assertThat(tc.csvFileName).isEqualTo(HerokuStage1Targets.get(tc.targetName)!!.csvFileName)
            }
        }
    }
}
