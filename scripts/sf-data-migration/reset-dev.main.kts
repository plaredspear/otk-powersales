#!/usr/bin/env kotlin

/**
 * dev DB 초기화 — SF 마이그레이션 산출물 (sfid IS NOT NULL row) 일괄 삭제.
 *
 * ⚠️ 운영 DB 금지 — db.properties 가 dev 환경인지 사전 확인.
 *
 * 본 스크립트는 migrate-stage2.main.kts 의 `--reset` 분기를 추출한 dev 전용 도구.
 * 운영 backend 에는 절대 노출되지 않는다 (Stage 2 자체는 admin REST 엔드포인트로 흡수됨).
 *
 * 실행: kotlin scripts/sf-data-migration/reset-dev.main.kts
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("com.opencsv:opencsv:5.9")
@file:DependsOn("org.postgresql:postgresql:42.7.4")
@file:Import("common.kts")

import java.io.File
import java.sql.Connection

fun applyDbReset(conn: Connection) {
    conn.prepareStatement("TRUNCATE TABLE powersales.sf_permission_set_assignment_raw").use { it.executeUpdate() }
    conn.prepareStatement(
        "DELETE FROM powersales.user_permission WHERE user_id IN " +
            "(SELECT user_id FROM powersales.\"user\" WHERE sfid IS NOT NULL)"
    ).use { it.executeUpdate() }

    val tablesToReset = mutableListOf<String>()
    for ((_, spec) in TARGET_SPECS) {
        val meta = spec.meta
        if (meta is EntityMetadata) tablesToReset.add(meta.tableName)
    }
    for (table in tablesToReset.reversed()) {
        val quoted = quoteTable(table)
        try {
            conn.prepareStatement("DELETE FROM powersales.$quoted WHERE sfid IS NOT NULL").use { it.executeUpdate() }
        } catch (e: Exception) {
            println("[reset warn] $table: ${e.message}")
        }
    }
}

val scriptDir = File(System.getProperty("user.dir"))
val dbConfig = loadDbConfig(scriptDir)

println("=".repeat(60))
println("SF 데이터 마이그레이션 — dev DB 초기화")
println("=".repeat(60))
println("jdbc url : ${dbConfig.jdbcUrl}")
println("jdbc user: ${dbConfig.user}")
println()

// 운영 환경 사전 가드 — 운영 host 면 거부
val prodHostPatterns = listOf("rds.amazonaws.com")
if (prodHostPatterns.any { dbConfig.jdbcUrl.contains(it) && !dbConfig.jdbcUrl.contains("localhost") }) {
    println("⚠️  운영 RDS endpoint 감지 — reset 거부. dev tunnel (localhost:15432) 만 허용.")
    System.exit(1)
}

val conn = openConnection(dbConfig)
try {
    applyDbReset(conn)
    conn.commit()
    println("✅ dev DB reset 완료")
} catch (e: Exception) {
    conn.rollback()
    println("❌ RESET FAILED: ${e.message}")
    throw e
} finally {
    conn.close()
}
