package com.otoki.powersales.herokumigration.stage1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * HerokuStage1S3CopyService.buildSelectClause — audit 컬럼 now() fallback 검증.
 *
 * Heroku 원본에 audit 컬럼이 없는 BaseEntity 상속 4종(EducationCode / SafetyCheckItem /
 * EmployeeAdmin / SafetyCheckSubmission)은 CSV 에 createddate/lastmodifieddate 가 없어 staging 에
 * created_at/updated_at 이 NULL 로 남는다. created_at/updated_at 은 NOT NULL 이므로 INSERT-SELECT 시
 * `COALESCE(<col>, now())` 로 마이그레이션 실행 시각 fallback 이 적용되어야 한다 (그러지 않으면 부팅/적재
 * 시 "null value in column created_at violates not-null constraint").
 *
 * DB/S3 의존 없는 순수 문자열 단위 테스트.
 */
@DisplayName("HerokuStage1S3CopyService.buildSelectClause — audit now() fallback")
class HerokuStage1SelectClauseTest {

    @Test
    @DisplayName("created_at/updated_at 은 COALESCE(<col>, now()) 로 감싼다 (alias 있음)")
    fun auditColumnsWrappedWithAlias() {
        val sql = HerokuStage1S3CopyService.buildSelectClause(
            listOf("employee_code", "created_at", "updated_at"),
            "s",
        )
        assertThat(sql).isEqualTo("s.employee_code, COALESCE(s.created_at, now()), COALESCE(s.updated_at, now())")
    }

    @Test
    @DisplayName("created_at/updated_at 은 COALESCE(<col>, now()) 로 감싼다 (alias 없음)")
    fun auditColumnsWrappedWithoutAlias() {
        val sql = HerokuStage1S3CopyService.buildSelectClause(
            listOf("edu_code", "created_at", "updated_at"),
            "",
        )
        assertThat(sql).isEqualTo("edu_code, COALESCE(created_at, now()), COALESCE(updated_at, now())")
    }

    @Test
    @DisplayName("audit 가 아닌 컬럼은 그대로 select (COALESCE 미적용)")
    fun nonAuditColumnsPlain() {
        val sql = HerokuStage1S3CopyService.buildSelectClause(
            listOf("employee_code", "account_code", "order_date"),
            "s",
        )
        assertThat(sql).isEqualTo("s.employee_code, s.account_code, s.order_date")
    }

    @Test
    @DisplayName("AuditedEntity 매핑(created_at/updated_at)도 COALESCE — CSV 값 있으면 staging 값 통과, 없으면 now()")
    fun auditedEntityAlsoCoalesced() {
        // AuditedEntity 는 inst_date/upd_date 가 created_at/updated_at 로 매핑되어 CSV 에 값이 있다.
        // COALESCE(s.created_at, now()) 는 NULL 아닌 값을 그대로 통과시키므로 원본 시각 보존 + NULL 시 now().
        val sql = HerokuStage1S3CopyService.buildSelectClause(
            listOf("employee_code", "created_at", "updated_at"),
            "s",
        )
        assertThat(sql).contains("COALESCE(s.created_at, now())")
        assertThat(sql).contains("COALESCE(s.updated_at, now())")
    }

    @Test
    @DisplayName("BaseEntity 상속 4종은 created_at/updated_at 이 매핑에 포함 — fallback 적용 대상")
    fun baseEntityTargetsIncludeAuditColumns() {
        listOf("EducationCode", "SafetyCheckItem", "EmployeeAdmin", "SafetyCheckSubmission").forEach { name ->
            val meta = HerokuStage1Targets.get(name)!!
            val dbColumns = meta.columns.map { it.dbColumn }
            assertThat(dbColumns)
                .withFailMessage("%s 의 컬럼 매핑에 created_at/updated_at 이 있어야 fallback 대상", name)
                .contains("created_at", "updated_at")

            // 이 매핑으로 생성한 SELECT 절에 audit COALESCE 가 반드시 들어가야 한다.
            val sql = HerokuStage1S3CopyService.buildSelectClause(dbColumns, "s")
            assertThat(sql)
                .withFailMessage("%s SELECT 절에 created_at now() fallback 누락", name)
                .contains("COALESCE(s.created_at, now())")
        }
    }
}
