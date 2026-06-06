package com.otoki.powersales.herokumigration.service

import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Heroku Stage 2 FK Resolve — 패턴 A (자연 키 → serial id) + 패턴 B (부모 FK) 일괄 적용.
 *
 * SF [com.otoki.powersales.sfmigration.service.SfMigrationStage2NaturalKeyFkService] 의
 * LEFT JOIN UPDATE 패턴과 동형 (테이블 단위 단일 UPDATE). 매핑 SoT 는 [HerokuFkResolveTables].
 *
 * ## 동작
 * 1. [HerokuFkResolveTables.NATURAL_KEY_FK] 순회 — `UPDATE source s SET fk = r.id FROM ref r
 *    WHERE r.key = s.col AND s.fk IS NULL` (패턴 A)
 * 2. [HerokuFkResolveTables.PARENT_FK] 순회 — 부모 자연 키로 부모 serial PK 조인 (패턴 B)
 * 3. 각 UPDATE 후 매칭 실패 (자연 키는 있으나 ref 에 없음) row 를 unmatched 로 집계 (FK NULL 유지, 리포트만)
 *
 * ## 멱등성
 * `WHERE s.<fk> IS NULL AND s.<natural_key> IS NOT NULL` 조건이라 재호출 시 이미 채운 row 는 0건 매칭.
 *
 * 식별자는 모두 [HerokuFkResolveTables] 의 컴파일 타임 상수 — SQL injection 없음.
 *
 * 전체 단일 트랜잭션 — 하나라도 실패하면 rollback. 진행 상태는 [HerokuFkResolveProgress] 로 노출.
 */
@Service
class HerokuFkResolveService(
    @PersistenceContext private val em: EntityManager,
    private val progress: HerokuFkResolveProgress,
) {

    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * 패턴 A + B FK resolve 일괄 실행.
     *
     * 입력: Stage 1 으로 적재된 테이블의 NULL FK 컬럼. 출력: 자연 키 lookup 으로 채운 FK + unmatched 리포트.
     * 외부 호출: 없음 (DB UPDATE 만). 부수 효과: powersales 스키마 FK 컬럼 갱신.
     */
    @Transactional
    fun runFkResolve() {
        val total = HerokuFkResolveTables.NATURAL_KEY_FK.size + HerokuFkResolveTables.PARENT_FK.size
        progress.begin(total)
        log.info(
            "[heroku-fk] start — naturalKey={} parent={}",
            HerokuFkResolveTables.NATURAL_KEY_FK.size, HerokuFkResolveTables.PARENT_FK.size,
        )

        try {
            for (m in HerokuFkResolveTables.NATURAL_KEY_FK) {
                resolveNaturalKey(m)
            }
            for (m in HerokuFkResolveTables.PARENT_FK) {
                resolveParent(m)
            }
            progress.finishOk()
            log.info("[heroku-fk] done — total {} rows affected", progress.totalRowsAffected)
        } catch (e: Throwable) {
            val msg = "${e.javaClass.simpleName}: ${e.message ?: "unknown"}"
            progress.finishWithFailure(msg)
            log.error("[heroku-fk] FAILED reason={}", msg, e)
            throw e
        }
    }

    /** 패턴 A — 자연 키 → 참조 테이블 serial id 채움 + unmatched 집계. */
    private fun resolveNaturalKey(m: HerokuFkResolveTables.NaturalKeyFk) {
        progress.beginTable(m.sourceTable)
        val s = HerokuFkResolveTables.SCHEMA
        val src = quote(m.sourceTable)
        val ref = quote(m.refTable)
        val sql = """
            UPDATE $s.$src s
            SET ${m.fkColumn} = r.${m.refIdColumn}
            FROM $s.$ref r
            WHERE r.${m.refKeyColumn} = s.${m.sourceColumn}
              AND s.${m.fkColumn} IS NULL
        """.trimIndent()
        val updated = em.createNativeQuery(sql).executeUpdate()

        val unmatched = countUnmatched(m.sourceTable, m.fkColumn, m.sourceColumn)
        progress.finishTable(
            HerokuFkResolveProgress.TableResult(
                table = m.sourceTable,
                column = m.fkColumn,
                rowsAffected = updated.toLong(),
            ),
        )
        progress.addUnmatched(
            HerokuFkResolveProgress.Unmatched(
                table = m.sourceTable,
                column = m.fkColumn,
                naturalKey = m.naturalKey,
                unmatchedCount = unmatched,
            ),
        )
        log.info(
            "[heroku-fk] A {}.{} ({} → {}.{}) : updated={} unmatched={}",
            m.sourceTable, m.fkColumn, m.sourceColumn, m.refTable, m.refKeyColumn, updated, unmatched,
        )
        if (unmatched > 0) {
            log.warn(
                "[heroku-fk] {}.{} : {} row 매칭 실패 (자연 키 {} 가 {}.{} 에 없음) — FK NULL 유지",
                m.sourceTable, m.fkColumn, unmatched, m.sourceColumn, m.refTable, m.refKeyColumn,
            )
        }
    }

    /** 패턴 B — 부모 자연 키로 부모 serial PK 조인 + unmatched 집계. */
    private fun resolveParent(m: HerokuFkResolveTables.ParentFk) {
        progress.beginTable(m.sourceTable)
        val s = HerokuFkResolveTables.SCHEMA
        val src = quote(m.sourceTable)
        val parent = quote(m.parentTable)
        val sql = """
            UPDATE $s.$src s
            SET ${m.fkColumn} = p.${m.parentIdColumn}
            FROM $s.$parent p
            WHERE p.${m.parentKeyColumn} = s.${m.sourceColumn}
              AND s.${m.fkColumn} IS NULL
        """.trimIndent()
        val updated = em.createNativeQuery(sql).executeUpdate()

        val unmatched = countUnmatched(m.sourceTable, m.fkColumn, m.sourceColumn)
        progress.finishTable(
            HerokuFkResolveProgress.TableResult(
                table = m.sourceTable,
                column = m.fkColumn,
                rowsAffected = updated.toLong(),
            ),
        )
        progress.addUnmatched(
            HerokuFkResolveProgress.Unmatched(
                table = m.sourceTable,
                column = m.fkColumn,
                naturalKey = m.sourceColumn,
                unmatchedCount = unmatched,
            ),
        )
        log.info(
            "[heroku-fk] B {}.{} ({} → {}.{}) : updated={} unmatched={}",
            m.sourceTable, m.fkColumn, m.sourceColumn, m.parentTable, m.parentKeyColumn, updated, unmatched,
        )
        if (unmatched > 0) {
            log.warn(
                "[heroku-fk] {}.{} : {} row 부모 매칭 실패 ({} 가 {}.{} 에 없음) — FK NULL 유지",
                m.sourceTable, m.fkColumn, unmatched, m.sourceColumn, m.parentTable, m.parentKeyColumn,
            )
        }
    }

    /** FK 컬럼이 NULL 인데 자연 키는 있는 row 수 (매칭 실패 측정). */
    private fun countUnmatched(table: String, fkColumn: String, naturalKeyColumn: String): Long {
        val s = HerokuFkResolveTables.SCHEMA
        val sql = """
            SELECT COUNT(*) FROM $s.${quote(table)} s
            WHERE s.$fkColumn IS NULL AND s.$naturalKeyColumn IS NOT NULL
        """.trimIndent()
        return (em.createNativeQuery(sql).singleResult as Number).toLong()
    }

    /** PG reserved keyword 대응 (Heroku 대상엔 없으나 안전 차원). */
    private fun quote(name: String): String =
        if (name in PG_RESERVED) "\"$name\"" else name

    companion object {
        private val PG_RESERVED = setOf("user", "group", "order", "desc")
    }
}
