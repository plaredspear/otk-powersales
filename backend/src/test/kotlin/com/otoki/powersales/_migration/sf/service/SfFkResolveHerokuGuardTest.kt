package com.otoki.powersales._migration.sf.service

import com.otoki.powersales._migration.heroku.stage1.HerokuStage1Targets
import jakarta.persistence.Column
import jakarta.persistence.Table
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SF Stage 2 FK Resolve — `@HerokuOnly` 테이블 가드 회귀 테스트.
 *
 * SF FK Resolve 는 powersales schema 의 모든 `*_sfid` 컬럼을 무차별 스캔하므로
 * ([SfMigrationStage2FkService.listSfidColumns]) `@HerokuOnly` 전용 테이블도 끌려온다. 기본 정책은
 * "Heroku 전용 테이블은 SF substep 에서 제외" 이며
 * ([SfMigrationStage2FkService.herokuOnlyTablesExcludedFromSfFkResolve]), 그 값이 진짜 SF Id 라
 * 처리가 필요한 테이블만 [HEROKU_TABLES_WITH_SF_SFID] 에 등록해 예외 허용한다.
 *
 * 본 테스트는:
 *  1. HEROKU_TABLES_WITH_SF_SFID 내용을 고정 (의도치 않은 추가/삭제 방지)
 *  2. 등록 테이블이 실제로 `@HerokuOnly` 엔티티이며 `_sfid` 컬럼을 보유하는지 검증
 *  3. `_sfid` 컬럼을 가진 `@HerokuOnly` 테이블 (= SF FK Resolve 가 끌어올 후보) 이 새로 생기면,
 *     SF Id 여부를 검토해 allowlist 등록(처리) 또는 미등록(제외) 을 결정하도록 known set 으로 강제.
 *
 * `@HerokuOnly` 엔티티 전수는 [HerokuStage1Targets.targetClasses] 에서 얻는다 (Heroku Stage1 적재
 * 대상이 곧 `@HerokuOnly` 엔티티 전체).
 */
@DisplayName("FK Resolve — @HerokuOnly 테이블 가드 회귀")
class SfFkResolveHerokuGuardTest {

    private data class HerokuEntity(val tableName: String, val sfidColumns: List<String>)

    private fun herokuOnlyEntities(): List<HerokuEntity> =
        HerokuStage1Targets.targetClasses().mapNotNull { kClass ->
            val java = kClass.java
            val tableName = java.getAnnotation(Table::class.java)?.name?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null
            val sfidCols = java.declaredFields
                .mapNotNull { f -> f.getAnnotation(Column::class.java)?.name }
                .filter { it.endsWith("_sfid") }
            HerokuEntity(tableName, sfidCols)
        }

    @Test
    @DisplayName("HEROKU_TABLES_WITH_SF_SFID 내용 고정")
    fun pinnedAllowlist() {
        assertThat(HEROKU_TABLES_WITH_SF_SFID)
            .containsExactlyInAnyOrder("safety_check_submission", "product_expiration")
    }

    @Test
    @DisplayName("예외 허용 테이블은 실제 @HerokuOnly 엔티티 + _sfid 컬럼 보유")
    fun allowlistedTablesAreRealHerokuEntitiesWithSfid() {
        val byTable = herokuOnlyEntities().associateBy { it.tableName }
        for (table in HEROKU_TABLES_WITH_SF_SFID) {
            val entity = byTable[table]
            assertThat(entity)
                .`as`("HEROKU_TABLES_WITH_SF_SFID 의 '$table' 은 실제 @HerokuOnly 엔티티여야 함")
                .isNotNull
            assertThat(entity!!.sfidColumns)
                .`as`("'$table' 은 SF FK Resolve 대상 _sfid 컬럼을 보유해야 함")
                .isNotEmpty
        }
    }

    @Test
    @DisplayName("_sfid 컬럼을 가진 @HerokuOnly 테이블은 모두 명시 판정됨 (allowlist 등록 또는 제외)")
    fun everyHerokuTableWithSfidIsClassified() {
        val candidates = herokuOnlyEntities()
            .filter { it.sfidColumns.isNotEmpty() }
            .map { it.tableName }
            .toSet()

        // 현재까지 검토 완료된 후보 — SF Id 라 allowlist 등록(처리) 결정된 것.
        // 신규 @HerokuOnly 테이블에 _sfid 가 추가되면 SF Id 여부를 검토해 HEROKU_TABLES_WITH_SF_SFID
        // 등록(처리) 또는 미등록(제외) 을 결정하고 이 known set 을 갱신할 것.
        val knownCandidates = setOf("safety_check_submission", "product_expiration")

        assertThat(candidates)
            .`as`(
                "新 @HerokuOnly 테이블에 _sfid 컬럼이 추가되면 SF Id 여부를 검토해 " +
                    "HEROKU_TABLES_WITH_SF_SFID 등록(처리) 또는 미등록(제외) 을 결정하고 known set 을 갱신하라",
            )
            .isEqualTo(knownCandidates)
    }
}
