package com.otoki.powersales._migration.sf.stage1

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * [Stage1S3CopyService.buildConflictClause] 의 ON CONFLICT 절 생성 단위 검증.
 * partial unique index 용 predicate 분기 / DO NOTHING 폴백 / COALESCE 보강 절을 커버.
 */
class Stage1ConflictClauseTest {

    private fun meta(conflict: ConflictUpdate?): EntityMetadata =
        EntityMetadata(
            targetName = "T",
            sObjectName = "T__c",
            tableName = "t",
            csvFileName = "t.csv",
            fields = listOf(FieldMapping("Id", "sfid", nullable = false)),
            conflictUpdate = conflict,
        )

    @Test
    @DisplayName("conflictUpdate 미지정 → ON CONFLICT DO NOTHING")
    fun doNothingWhenNull() {
        val sql = Stage1S3CopyService.buildConflictClause(meta(null), "t")
        assertThat(sql).isEqualTo("ON CONFLICT DO NOTHING")
    }

    @Test
    @DisplayName("full unique 충돌키 → ON CONFLICT (col) DO UPDATE + COALESCE 보강")
    fun fullUniqueDoUpdate() {
        val cu = ConflictUpdate(conflictColumn = "sfid", updateColumns = listOf("name", "account_sfid"))
        val sql = Stage1S3CopyService.buildConflictClause(meta(cu), "t")
        assertThat(sql).isEqualTo(
            "ON CONFLICT (sfid) DO UPDATE SET " +
                "name = COALESCE(EXCLUDED.name, t.name), " +
                "account_sfid = COALESCE(EXCLUDED.account_sfid, t.account_sfid)",
        )
    }

    @Test
    @DisplayName("partial unique 충돌키 → ON CONFLICT (col) WHERE <predicate> DO UPDATE")
    fun partialUniqueIncludesPredicate() {
        val cu = ConflictUpdate(
            conflictColumn = "sfid",
            conflictPredicate = "sfid IS NOT NULL",
            updateColumns = listOf("name"),
        )
        val sql = Stage1S3CopyService.buildConflictClause(meta(cu), "t")
        assertThat(sql).isEqualTo(
            "ON CONFLICT (sfid) WHERE sfid IS NOT NULL DO UPDATE SET name = COALESCE(EXCLUDED.name, t.name)",
        )
    }

    @Test
    @DisplayName("quotedTable 은 COALESCE 기존값 참조에 그대로 사용 (예: \"user\")")
    fun quotedTableUsedInCoalesce() {
        val cu = ConflictUpdate(conflictColumn = "name", updateColumns = listOf("sfid"))
        val sql = Stage1S3CopyService.buildConflictClause(meta(cu), "\"user\"")
        assertThat(sql).isEqualTo(
            "ON CONFLICT (name) DO UPDATE SET sfid = COALESCE(EXCLUDED.sfid, \"user\".sfid)",
        )
    }

    @Test
    @DisplayName("updateOnly → UPDATE ... FROM staging (신규 INSERT 없음, arbiter 조인 + s.key NOT NULL)")
    fun updateOnlyFromStaging() {
        val cu = ConflictUpdate(
            conflictColumn = "sfid",
            updateOnly = true,
            updateColumns = listOf("name", "account_sfid"),
        )
        val sql = Stage1S3CopyService.buildUpdateFromStagingSql(
            meta(cu), cu, "powersales.t", "t", "powersales._copy_staging_t",
        )
        assertThat(sql).isEqualTo(
            "UPDATE powersales.t SET " +
                "name = COALESCE(s.name, t.name), account_sfid = COALESCE(s.account_sfid, t.account_sfid) " +
                "FROM powersales._copy_staging_t s " +
                "WHERE t.sfid = s.sfid AND s.sfid IS NOT NULL",
        )
    }

    @Test
    @DisplayName("updateOnly + conflictPredicate → target 측 partial 조건이 WHERE 에 AND 로 추가")
    fun updateOnlyWithPredicate() {
        val cu = ConflictUpdate(
            conflictColumn = "sfid",
            conflictPredicate = "sfid IS NOT NULL",
            updateOnly = true,
            updateColumns = listOf("name"),
        )
        val sql = Stage1S3CopyService.buildUpdateFromStagingSql(
            meta(cu), cu, "powersales.t", "t", "powersales._copy_staging_t",
        )
        assertThat(sql).isEqualTo(
            "UPDATE powersales.t SET name = COALESCE(s.name, t.name) " +
                "FROM powersales._copy_staging_t s " +
                "WHERE t.sfid = s.sfid AND s.sfid IS NOT NULL AND t.sfid IS NOT NULL",
        )
    }
}
