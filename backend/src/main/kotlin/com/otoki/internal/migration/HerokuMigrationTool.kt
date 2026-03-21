package com.otoki.internal.migration

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.entity.PushMessage
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFSchemaUtils
import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
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
 * ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ Heroku DB (salesforce2)            │ Dev DB (salesforce2)        │ Entity                  │ 참조키 (sfid FK)                                              │ Migrate │ 비고               │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │ account                            │ account                     │ Account                 │ —                                                             │  YES    │                    │
 * │ dkretail__product__c               │ product                     │ Product                 │ —                                                             │  YES    │                    │
 * │ safetycheck_list                   │ safety_check_item           │ SafetyCheckItem         │ —                                                             │  YES    │                    │
 * │ dkretail__employee__c              │ employee                    │ Employee                │ —                                                             │  YES    │ 종속: employee_mng │
 * │ productbarcode__c                  │ product_barcode             │ ProductBarcode          │ product__c → product.sfid                                     │  YES    │ UPDATE: product_id │
 * │ dkretail__notice__c                │ notice                      │ Notice                  │ employeeid__c → employee.sfid                                 │  YES    │ FK: employee_id    │
 * │ displayworkschedulemaster__c       │ display_work_schedule       │ DisplayWorkSchedule     │ account__c → account.sfid, ownerid → employee.sfid            │  YES    │ FK: account_id, employee_id │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │ dkretail__teammemberschedule__c    │ team_member_schedule        │ TeamMemberSchedule      │ accountid__c → account.sfid, dkretail__employeeid__c →         │   no    │                    │
 * │                                    │                             │                         │   employee.sfid, teamleadersfid__c → employee.sfid,           │         │                    │
 * │                                    │                             │                         │   dkretail__promotionempid__c → employee.sfid                 │         │                    │
 * │ safetycheck__workschedule__member  │ safety_check_submission     │ SafetyCheckSubmission   │ employeeid__c → employee.sfid, eventmasterid → sfid           │   no    │                    │
 * │ education_mng                      │ education_post              │ EducationPost           │ —                                                             │   no    │                    │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │ agreementword__c                   │ agreement_word              │ AgreementWord           │ —                                                             │  YES    │                    │
 * │ commute_distance                   │ —                           │ —                       │ —                                                             │   no    │                    │
 * │ device_version_mng                 │ device_version              │ DeviceVersion           │ —                                                             │   no    │                    │
 * │ education_code_mng                 │ education_code              │ EducationCode           │ —                                                             │   no    │                    │
 * │ education_file_mng                 │ education_post_attachment   │ EducationPostAttachment │ edu_id → education_mng (코드값, sfid 아님)                    │   no    │                    │
 * │ education_member_history           │ education_view_history      │ EducationViewHistory    │ community_id → education_mng (코드값, sfid 아님)              │   no    │                    │
 * │ employee_admin_mng                 │ employee_admin              │ EmployeeAdmin           │ —                                                             │   no    │                    │
 * │ employee_his                       │ employee_his                │ LoginHistory            │ —                                                             │   no    │                    │
 * │ employee_mng                       │ employee_mng                │ EmployeeMng             │ —                                                             │   no    │ Employee 종속 테이블 │
 * │ expirationdate__mng                │ expirationdate__mng         │ ShelfLife               │ employee_id → employee.sfid                                   │   no    │                    │
 * │ hqreview__c                        │ hq_review                   │ HqReview                │ —                                                             │   no    │                    │
 * │ if_product__c                      │ if_product                  │ InterfaceProduct        │ —                                                             │   no    │                    │
 * │ monthlysaleshistory__c             │ monthly_sales_history       │ MonthlySalesHistory     │ —                                                             │   no    │                    │
 * │ product_favorites                  │ product_favorites           │ FavoriteProduct         │ —                                                             │   no    │                    │
 * │ pushmessage__c                     │ push_message                │ PushMessage             │ —                                                             │  YES    │                    │
 * │ pushmessagereceiver__c             │ push_message_receiver       │ PushMessageReceiver     │ employeeid__c → employee.sfid, messageid__c → pushmessage.sfid │   no    │                    │
 * │ staffreview__c                     │ staff_review                │ StaffReview             │ dkretail_employeeid__c → employee.sfid                        │   no    │                    │
 * │ theme__c                           │ inspection_theme            │ InspectionTheme         │ —                                                             │   no    │                    │
 * │ tmp_claim                          │ tmp_claim                   │ TmpClaim                │ —                                                             │   no    │                    │
 * │ tmp_claimcode                      │ tmp_claimcode               │ —                       │ —                                                             │   no    │                    │
 * │ tmp_onsite                         │ —                           │ —                       │ —                                                             │   no    │ Heroku 전용        │
 * │ tmp_order                          │ —                           │ —                       │ —                                                             │   no    │ Heroku 전용        │
 * │ tmp_order_product                  │ —                           │ —                       │ —                                                             │   no    │ Heroku 전용        │
 * │ tmp_promotion                      │ —                           │ —                       │ —                                                             │   no    │ Heroku 전용        │
 * │ tmp_suggest                        │ —                           │ —                       │ —                                                             │   no    │ Heroku 전용        │
 * │ uploadfile__c                      │ upload_file                 │ UploadFile              │ recordid__c → 다형성 sfid (여러 오브젝트 참조)                 │   no    │                    │
 * │ _hcmeta                            │ —                           │ —                       │ —                                                             │   no    │ 시스템             │
 * │ _sf_event_log                      │ —                           │ —                       │ —                                                             │   no    │ 시스템             │
 * │ _trigger_log                       │ —                           │ —                       │ —                                                             │   no    │ 시스템             │
 * │ _trigger_log_archive               │ —                           │ —                       │ —                                                             │   no    │ 시스템             │
 * └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
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
        /** FK 없이 연관된 테이블 목록 — TRUNCATE 시 함께 정리 (DB FK CASCADE 미적용 대상) */
        val dependentTables: List<String> = emptyList(),
        val columnTransformProvider: ((Connection) -> Map<String, Map<String, Any?>>)? = null,
    )

    /** @HCTable 엔티티 등록. 추가 엔티티는 여기에 추가. 순서가 마이그레이션 순서를 결정 */
    private val entities = listOf(
        EntityRegistration("account", Account::class.java),
        EntityRegistration("product", Product::class.java),
        EntityRegistration("safetyCheckItem", SafetyCheckItem::class.java),
        EntityRegistration("employee", Employee::class.java, dependentTables = listOf("employee_mng")),
        EntityRegistration("productBarcode", ProductBarcode::class.java),
        EntityRegistration("notice", Notice::class.java) { targetConn ->
            val sfidToPk = mutableMapOf<String, Any?>()
            targetConn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT sfid, id FROM $TARGET_SCHEMA.employee WHERE sfid IS NOT NULL").use { rs ->
                    while (rs.next()) {
                        sfidToPk[rs.getString("sfid")] = rs.getLong("id")
                    }
                }
            }
            mapOf("employee_id" to sfidToPk)
        },
        EntityRegistration("displayWorkSchedule", DisplayWorkSchedule::class.java) { targetConn ->
            val employeeSfidToPk = mutableMapOf<String, Any?>()
            targetConn.createStatement().use { stmt ->
                stmt.executeQuery("SELECT sfid, id FROM $TARGET_SCHEMA.employee WHERE sfid IS NOT NULL").use { rs ->
                    while (rs.next()) {
                        employeeSfidToPk[rs.getString("sfid")] = rs.getLong("id")
                    }
                }
            }
            mapOf("employee_id" to employeeSfidToPk)
        },
        EntityRegistration("agreementWord", AgreementWord::class.java),
        EntityRegistration("pushMessage", PushMessage::class.java),
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
                entities.forEach { reg ->
                    val columnTransforms = reg.columnTransformProvider?.invoke(targetConn) ?: emptyMap()
                    migrateEntity(reg.name, reg.entityClass, herokuConn, targetConn, columnTransforms, reg.dependentTables)
                }

                // ProductBarcode: product_sfid → product_id 역참조 UPDATE
                println("[productBarcode] product_sfid → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.product_barcode pb " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE pb.product_sfid = p.sfid"
                    )
                    println("[productBarcode] product_id UPDATE 완료: ${updated}건")
                }

                // DisplayWorkSchedule: account_sfid → account_id 역참조 UPDATE
                println("[displayWorkSchedule] account_sfid → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.display_work_schedule d " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE d.account_sfid = a.sfid"
                    )
                    println("[displayWorkSchedule] account_id UPDATE 완료: ${updated}건")
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
        dependentTables: List<String> = emptyList(),
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

        // @HCColumn 매핑에서 PK 제외
        val mappings = SFSchemaUtils.getHCFieldMappings(entityClass)
            .filter { it.jpaColumnName !in idColumnNames }

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
            // FK 없이 연관된 종속 테이블 먼저 TRUNCATE (DB CASCADE 미적용 대상)
            dependentTables.forEach { depTable ->
                stmt.execute("TRUNCATE TABLE $TARGET_SCHEMA.$depTable CASCADE")
                println("[$name] 종속 테이블 $depTable TRUNCATE 완료")
            }
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
