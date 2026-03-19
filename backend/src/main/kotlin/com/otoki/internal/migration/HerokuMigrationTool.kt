package com.otoki.internal.migration

import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFSchemaUtils
import com.otoki.internal.sap.entity.Account
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.util.Properties

/**
 * Heroku DB → Dev DB 데이터 마이그레이션 도구
 * Spring 컨텍스트 없이 독립 실행. @HCTable/@HCColumn 어노테이션 기반 매핑.
 *
 * 실행:
 *   HEROKU_DB_PASSWORD=$(jq -r '.["dev-heroku-db"].PASSWORD' docs/plan/old-accounts.json) ./gradlew migrateHeroku
 */
object HerokuMigrationTool {

    /** @HCTable 엔티티 등록. 추가 엔티티는 여기에 추가 */
    private val entities = listOf(
        "account" to Account::class.java,
    )

    private const val HEROKU_SCHEMA = "salesforce2"
    private const val TARGET_SCHEMA = "salesforce2"
    private const val BATCH_SIZE = 1000

    // Heroku DB (읽기 전용)
    private const val HEROKU_HOST = "ec2-13-159-136-112.ap-northeast-1.compute.amazonaws.com"
    private const val HEROKU_PORT = "5432"
    private const val HEROKU_DB = "dc7oam5vbbb7d7"
    private const val HEROKU_USER = "u4bee3ek26k44g"
    private const val HEROKU_URL = "jdbc:postgresql://$HEROKU_HOST:$HEROKU_PORT/$HEROKU_DB?sslmode=require"

    // Dev DB (기본값, 환경변수 → ~/.pgpass 순으로 비밀번호 탐색)
    private const val TARGET_HOST = "dev-db.codapt.kr"
    private const val TARGET_PORT = "5432"
    private const val TARGET_DB = "otoki"
    private const val TARGET_USER_DEFAULT = "otoki_admin"

    private val TARGET_URL = System.getenv("DATABASE_URL")
        ?: "jdbc:postgresql://$TARGET_HOST:$TARGET_PORT/$TARGET_DB"
    private val TARGET_USER = System.getenv("DATABASE_USERNAME") ?: TARGET_USER_DEFAULT
    private val TARGET_PASSWORD = System.getenv("DATABASE_PASSWORD")
        ?: readPgpass(TARGET_HOST, TARGET_PORT, TARGET_DB, TARGET_USER_DEFAULT)
        ?: ""

    @JvmStatic
    fun main(args: Array<String>) {
        val herokuPassword = System.getenv("HEROKU_DB_PASSWORD")
        if (herokuPassword.isNullOrBlank()) {
            println("ERROR: HEROKU_DB_PASSWORD 환경변수가 필요합니다")
            println("  HEROKU_DB_PASSWORD=\$(jq -r '.[\"dev-heroku-db\"].PASSWORD' docs/plan/old-accounts.json) ./gradlew migrateHeroku")
            return
        }

        println("=== Heroku → Dev DB 마이그레이션 시작 (${entities.size}개 엔티티) ===")

        createConnection(HEROKU_URL, HEROKU_USER, herokuPassword).use { herokuConn ->
            createConnection(TARGET_URL, TARGET_USER, TARGET_PASSWORD).use { targetConn ->
                entities.forEach { (name, entityClass) ->
                    migrateEntity(name, entityClass, herokuConn, targetConn)
                }
            }
        }

        println("=== 마이그레이션 완료 ===")
    }

    private fun migrateEntity(
        name: String,
        entityClass: Class<*>,
        herokuConn: Connection,
        targetConn: Connection
    ) {
        val tableName = entityClass.getAnnotation(HCTable::class.java)?.value
            ?: throw IllegalArgumentException("$name: @HCTable 어노테이션 없음")
        val mappings = SFSchemaUtils.getHCFieldMappings(entityClass)
        val jpaColumns = mappings.map { it.jpaColumnName }

        // 1. Heroku DB에서 읽기 (@HCColumn 매핑 + 타임스탬프)
        val baseSql = SFSchemaUtils.generateImportSql(entityClass, HEROKU_SCHEMA)
        val selectSql = baseSql.replace(
            " FROM ",
            ", createddate AS created_at, systemmodstamp AS updated_at FROM "
        )
        val allColumns = jpaColumns + listOf("created_at", "updated_at")

        println("[$name] Heroku DB 조회 중...")

        val rows = mutableListOf<Array<Any?>>()
        herokuConn.createStatement().use { stmt ->
            stmt.executeQuery(selectSql).use { rs ->
                while (rs.next()) {
                    rows.add(allColumns.map { col -> rs.getObject(col) }.toTypedArray())
                }
            }
        }
        println("[$name] ${rows.size}건 조회 완료")

        if (rows.isEmpty()) {
            println("[$name] 데이터 없음 — 건너뜀")
            return
        }

        // 2. 대상 테이블 초기화 + 시퀀스 리셋
        targetConn.createStatement().use { stmt ->
            stmt.execute("TRUNCATE TABLE $TARGET_SCHEMA.$tableName CASCADE")
            stmt.execute("ALTER SEQUENCE $TARGET_SCHEMA.${tableName}_id_seq RESTART WITH 1")
        }

        // 3. 배치 INSERT (ID 포함, 원본 그대로 보존)
        val insertSql = "INSERT INTO $TARGET_SCHEMA.$tableName " +
            "(${allColumns.joinToString(", ")}) VALUES " +
            "(${allColumns.indices.joinToString(", ") { "?" }})"

        targetConn.autoCommit = false
        var inserted = 0

        targetConn.prepareStatement(insertSql).use { ps ->
            rows.forEach { row ->
                row.forEachIndexed { i, value -> ps.setObject(i + 1, value) }
                ps.addBatch()
                inserted++

                if (inserted % BATCH_SIZE == 0) {
                    ps.executeBatch()
                    targetConn.commit()
                    println("[$name] $inserted / ${rows.size}")
                }
            }
            ps.executeBatch()
            targetConn.commit()
        }

        targetConn.autoCommit = true
        println("[$name] $inserted / ${rows.size}")
        println("[$name] 완료: ${rows.size}건")
    }

    private fun createConnection(url: String, user: String, password: String): Connection {
        val props = Properties().apply {
            setProperty("user", user)
            setProperty("password", password)
        }
        return DriverManager.getConnection(url, props)
    }

    /**
     * ~/.pgpass 에서 비밀번호를 읽는다.
     * 형식: hostname:port:database:username:password
     */
    private fun readPgpass(host: String, port: String, db: String, user: String): String? {
        val pgpassFile = File(System.getProperty("user.home"), ".pgpass")
        if (!pgpassFile.exists()) return null

        return pgpassFile.readLines()
            .filter { !it.startsWith("#") && it.isNotBlank() }
            .firstNotNullOfOrNull { line ->
                val parts = line.split(":")
                if (parts.size >= 5) {
                    val (h, p, d, u) = parts
                    if ((h == "*" || h == host) && (p == "*" || p == port) &&
                        (d == "*" || d == db) && (u == "*" || u == user)
                    ) {
                        parts.drop(4).joinToString(":")
                    } else null
                } else null
            }
    }
}
