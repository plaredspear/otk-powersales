#!/usr/bin/env kotlin

/**
 * dev DB 초기화 — Heroku 마이그레이션 대상 19개 Heroku-only 테이블 일괄 TRUNCATE.
 *
 * ⚠️ 운영 DB 금지 — 운영 RDS endpoint 감지 시 거부. localhost(SSM 터널) 만 허용.
 *
 * dev 리허설 반복용. 적재 본체(Stage 1)는 backend web 화면의 Reset 모드(적재 전 TRUNCATE)가
 * 담당하므로 본 스크립트는 전체 초기화(전 테이블 동시 비우기) 편의용이다.
 *
 * 사전 조건: scripts/db-tunnel.sh -s dev 로 SSM 터널이 열려 있어야 한다.
 * 실행: kotlin scripts/heroku-data-migration/reset-dev.main.kts
 */

@file:Repository("https://repo.maven.apache.org/maven2/")
@file:DependsOn("org.postgresql:postgresql:42.7.4")

import java.io.File
import java.sql.DriverManager
import java.util.Properties

// 적재 순서 역순으로 TRUNCATE 할 필요 없이 CASCADE 로 일괄 처리. backend HerokuStage1Targets 의
// 19개 신규 테이블명과 1:1. ProductSyncBuffer(product_sync_buffer) 는 마이그레이션 제외라 미포함.
val HEROKU_ONLY_TABLES = listOf(
    "education_code",
    "tmp_claim_code",
    "device_version",
    "safety_check_item",
    "employee_admin",
    "employee_info",
    "education_post",
    "education_post_attachment",
    "education_view_history",
    "tmp_order",
    "tmp_order_product",
    "tmp_claim",
    "tmp_suggest",
    "tmp_onsite",
    "tmp_promotion",
    "product_favorite",
    "login_history",
    "product_expiration",
    "safety_check_submission",
)

val scriptDir = File(System.getProperty("user.dir"))
val propsFile = scriptDir.resolve("scripts/heroku-data-migration/db.properties")
    .takeIf { it.isFile }
    ?: scriptDir.resolve("db.properties")

if (!propsFile.isFile) {
    System.err.println("❌ db.properties 미존재 — db.properties.template 을 복사해 작성하세요: ${propsFile.absolutePath}")
    kotlin.system.exitProcess(1)
}

val props = Properties().apply { propsFile.inputStream().use { load(it) } }
val jdbcUrl = props.getProperty("jdbc.url") ?: error("db.properties 에 jdbc.url 없음")
val jdbcUser = props.getProperty("jdbc.user") ?: error("db.properties 에 jdbc.user 없음")
val jdbcPassword = props.getProperty("jdbc.password") ?: ""

println("=".repeat(60))
println("Heroku 데이터 마이그레이션 — dev DB 초기화")
println("=".repeat(60))
println("jdbc url : $jdbcUrl")
println("jdbc user: $jdbcUser")
println("tables   : ${HEROKU_ONLY_TABLES.size} 개")
println()

// 운영 환경 사전 가드 — 운영 RDS host 면 거부.
if (jdbcUrl.contains("rds.amazonaws.com") && !jdbcUrl.contains("localhost")) {
    System.err.println("⚠️  운영 RDS endpoint 감지 — reset 거부. dev tunnel(localhost:15432) 만 허용.")
    kotlin.system.exitProcess(1)
}

val conn = DriverManager.getConnection(jdbcUrl, jdbcUser, jdbcPassword)
conn.autoCommit = false
try {
    val quoted = HEROKU_ONLY_TABLES.joinToString(", ") { "powersales.$it" }
    conn.createStatement().use { st ->
        st.executeUpdate("TRUNCATE TABLE $quoted RESTART IDENTITY CASCADE")
    }
    conn.commit()
    println("✅ dev DB reset 완료 — ${HEROKU_ONLY_TABLES.size} 개 테이블 TRUNCATE")
} catch (e: Exception) {
    conn.rollback()
    System.err.println("❌ RESET FAILED: ${e.message}")
    throw e
} finally {
    conn.close()
}
