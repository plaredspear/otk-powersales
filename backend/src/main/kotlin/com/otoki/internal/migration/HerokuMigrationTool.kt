package com.otoki.internal.migration

import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFSchemaUtils
import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.sap.entity.ProductBarcode
import jakarta.persistence.Id
import jakarta.persistence.Table
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

    /**
     * 엔티티 등록 정보.
     * @param name 로그 식별명
     * @param entityClass JPA 엔티티 클래스 (@HCTable/@HCColumn 필수)
     * @param columnTransformProvider 마이그레이션 시점에 컬럼 변환 맵을 동적 생성하는 함수.
     *   반환: Map<JPA컬럼명, Map<Heroku원본값, DevDB변환값>>. null이면 변환 없음.
     */
    private data class EntityRegistration(
        val name: String,
        val entityClass: Class<*>,
        val columnTransformProvider: ((Connection) -> Map<String, Map<String, Any?>>)? = null,
    )

    /** @HCTable 엔티티 등록. 추가 엔티티는 여기에 추가. 순서가 마이그레이션 순서를 결정 */
    private val entities = listOf(
        EntityRegistration("account", Account::class.java),
        EntityRegistration("product", Product::class.java),
        EntityRegistration("safetyCheckItem", SafetyCheckItem::class.java),
        EntityRegistration("productBarcode", ProductBarcode::class.java) { targetConn ->
            val sfidToPk = mutableMapOf<String, Any?>()
            targetConn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT sfid, product_id FROM $TARGET_SCHEMA.product WHERE sfid IS NOT NULL").use { rs ->
                    while (rs.next()) {
                        sfidToPk[rs.getString("sfid")] = rs.getLong("product_id")
                    }
                }
            }
            mapOf("product_id" to sfidToPk)
        },
    )

    /** 마이그레이션에서 제외할 HC 전용 컬럼 (JPA @Column name 기준) */
    private val EXCLUDED_HC_COLUMNS = setOf("_hc_lastop", "_hc_err")

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
                entities.forEach { reg ->
                    val columnTransforms = reg.columnTransformProvider?.invoke(targetConn) ?: emptyMap()
                    migrateEntity(reg.name, reg.entityClass, herokuConn, targetConn, columnTransforms)
                }
            }
        }

        println("=== 마이그레이션 완료 ===")
    }

    private fun migrateEntity(
        name: String,
        entityClass: Class<*>,
        herokuConn: Connection,
        targetConn: Connection,
        columnTransforms: Map<String, Map<String, Any?>> = emptyMap(),
    ) {
        val hcTableName = entityClass.getAnnotation(HCTable::class.java)?.value
            ?: throw IllegalArgumentException("$name: @HCTable 어노테이션 없음")
        val targetTableName = entityClass.getAnnotation(Table::class.java)?.name
            ?: hcTableName

        // @Id 필드의 JPA 컬럼명 수집 (PK 제외용)
        val idColumnNames = entityClass.declaredFields
            .filter { it.isAnnotationPresent(Id::class.java) }
            .mapNotNull { it.getAnnotation(jakarta.persistence.Column::class.java)?.name }
            .toSet()

        // @HCColumn 매핑에서 PK + HC 전용 컬럼 제외
        val mappings = SFSchemaUtils.getHCFieldMappings(entityClass)
            .filter { it.jpaColumnName !in idColumnNames }
            .filter { it.jpaColumnName !in EXCLUDED_HC_COLUMNS }

        // 1. Heroku DB에서 읽기 (필터링된 매핑 + 타임스탬프 조건부)
        val hasTimestamp = hasColumn(herokuConn, HEROKU_SCHEMA, hcTableName, "createddate")

        val selectColumns = mappings.map { m ->
            if (m.hcColumnName == m.jpaColumnName) m.hcColumnName
            else "${m.hcColumnName} AS ${m.jpaColumnName}"
        } + if (hasTimestamp) listOf("createddate AS created_at", "systemmodstamp AS updated_at") else emptyList()

        val allJpaColumns = mappings.map { it.jpaColumnName } +
            if (hasTimestamp) listOf("created_at", "updated_at") else emptyList()
        val selectSql = "SELECT ${selectColumns.joinToString(", ")} FROM $HEROKU_SCHEMA.$hcTableName"

        println("[$name] Heroku DB 조회 중...")

        val rows = mutableListOf<Array<Any?>>()
        herokuConn.createStatement().use { stmt ->
            stmt.executeQuery(selectSql).use { rs ->
                while (rs.next()) {
                    rows.add(allJpaColumns.map { col -> rs.getObject(col) }.toTypedArray())
                }
            }
        }
        println("[$name] ${rows.size}건 조회 완료")

        if (rows.isEmpty()) {
            println("[$name] 데이터 없음 — 건너뜀")
            return
        }

        // 2. 대상 테이블 초기화 (PK는 IDENTITY 자동 채번이므로 시퀀스 리셋 불필요)
        targetConn.createStatement().use { stmt ->
            stmt.execute("TRUNCATE TABLE $TARGET_SCHEMA.$targetTableName CASCADE")
        }

        // 3. 배치 INSERT (PK 제외 — IDENTITY 자동 채번)
        // 변환 대상 컬럼의 인덱스 매핑 (성능: 행마다 맵 조회 대신 인덱스로 접근)
        val transformIndices = columnTransforms.mapNotNull { (colName, transformMap) ->
            val idx = allJpaColumns.indexOf(colName)
            if (idx >= 0) idx to transformMap else null
        }.toMap()

        val insertSql = "INSERT INTO $TARGET_SCHEMA.$targetTableName " +
            "(${allJpaColumns.joinToString(", ")}) VALUES " +
            "(${allJpaColumns.indices.joinToString(", ") { "?" }})"

        targetConn.autoCommit = false
        var inserted = 0
        var skipped = 0

        targetConn.prepareStatement(insertSql).use { ps ->
            rows.forEach { row ->
                // 컬럼 값 변환 적용
                var skip = false
                for ((idx, transformMap) in transformIndices) {
                    val originalValue = row[idx]
                    if (originalValue == null) continue // NULL은 그대로
                    val key = originalValue.toString()
                    if (key !in transformMap) {
                        println("[$name] WARN: 변환 실패 — ${allJpaColumns[idx]}='$key' (참조 대상 없음), 행 스킵")
                        skip = true
                        break
                    }
                    row[idx] = transformMap[key]
                }
                if (skip) {
                    skipped++
                    return@forEach
                }

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
        if (skipped > 0) println("[$name] 스킵: ${skipped}건 (참조 변환 실패)")
        println("[$name] 완료: ${inserted}건 이관, ${skipped}건 스킵 (총 ${rows.size}건)")
    }

    private fun hasColumn(conn: Connection, schema: String, table: String, column: String): Boolean {
        val rs = conn.metaData.getColumns(null, schema, table, column)
        val exists = rs.use { it.next() }
        return exists
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
