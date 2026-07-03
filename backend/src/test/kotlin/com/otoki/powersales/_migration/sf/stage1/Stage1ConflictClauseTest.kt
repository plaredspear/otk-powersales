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
}
