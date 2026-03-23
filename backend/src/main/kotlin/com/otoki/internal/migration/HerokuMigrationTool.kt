package com.otoki.internal.migration

import com.otoki.internal.common.entity.AgreementWord
import com.otoki.internal.entity.PushMessage
import com.otoki.internal.common.salesforce.HCTable
import com.otoki.internal.common.salesforce.SFSchemaUtils
import com.otoki.internal.safetycheck.entity.SafetyCheckItem
import com.otoki.internal.safetycheck.entity.SafetyCheckSubmission
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.entity.Employee
import com.otoki.internal.sap.entity.EmployeeInfo
import com.otoki.internal.sap.entity.Product
import com.otoki.internal.notice.entity.Notice
import com.otoki.internal.schedule.entity.DisplayWorkSchedule
import com.otoki.internal.schedule.entity.TeamMemberSchedule
import com.otoki.internal.education.entity.EducationPost
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
 * │ Migrate │ Heroku DB (salesforce2)            │ Dev DB (salesforce2)        │ Entity                  │ 참조키 (sfid FK)                                              │ 비고               │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │  YES    │ account                            │ account                     │ Account                 │ —                                                             │                    │
 * │  YES    │ dkretail__product__c               │ product                     │ Product                 │ —                                                             │                    │
 * │  YES    │ safetycheck_list                   │ safety_check_item           │ SafetyCheckItem         │ —                                                             │                    │
 * │  YES    │ dkretail__employee__c              │ employee                    │ Employee                │ —                                                             │ 종속: employee_mng │
 * │  YES    │ productbarcode__c                  │ product_barcode             │ ProductBarcode          │ product__c → product.sfid                                     │ UPDATE: product_id │
 * │  YES    │ dkretail__notice__c                │ notice                      │ Notice                  │ employeeid__c → employee.sfid                                 │ FK: employee_id    │
 * │  YES    │ displayworkschedulemaster__c       │ display_work_schedule       │ DisplayWorkSchedule     │ account__c → account.sfid, fullname__c → employee.sfid,       │ UPDATE: account_id, employee_id │
 * │         │                                    │                             │                         │   ownerid → (owner_sfid 저장만)                               │                    │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │  YES    │ dkretail__teammemberschedule__c    │ team_member_schedule        │ TeamMemberSchedule      │ accountid__c → account.sfid, dkretail__employeeid__c →         │ UPDATE: account_id, employee_id, team_leader_id, promotion_employee_id │
 * │         │                                    │                             │                         │   employee.sfid, teamleadersfid__c → employee.sfid,           │                    │
 * │         │                                    │                             │                         │   dkretail__promotionempid__c → employee.sfid                 │                    │
 * │  YES    │ safetycheck__workschedule__member  │ safety_check_submission     │ SafetyCheckSubmission   │ employeeid__c → employee.sfid, masterId →                     │ FK: employee_id(inline), display_work_schedule_id/team_member_schedule_id(post-UPDATE) │
 * │         │                                    │                             │                         │   displayworkschedulemaster__c.sfid, eventmasterid → sfid     │                    │
 * │  YES    │ education_mng                      │ education_post              │ EducationPost           │ empcode__c → employee.employee_code                           │ UPDATE: employee_id │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │  YES    │ agreementword__c                   │ agreement_word              │ AgreementWord           │ —                                                             │                    │
 * │   no    │ commute_distance                   │ —                           │ —                       │ —                                                             │                    │
 * │   no    │ education_code_mng                 │ education_code              │ EducationCode           │ —                                                             │                    │
 * │   no    │ education_file_mng                 │ education_post_attachment   │ EducationPostAttachment │ edu_id → education_mng (코드값, sfid 아님)                    │                    │
 * │   no    │ education_member_history           │ education_view_history      │ EducationViewHistory    │ community_id → education_mng (코드값, sfid 아님)              │                    │
 * │   no    │ employee_admin_mng                 │ employee_admin              │ EmployeeAdmin           │ —                                                             │                    │
 * │   no    │ employee_his                       │ login_history               │ LoginHistory                    │ —                                                             │                    │
 * │  YES    │ employee_mng                       │ employee_info               │ EmployeeInfo            │ —                                                             │ Employee 종속 테이블, 자연키 PK │
 * │   no    │ expirationdate__mng                │ expirationdate__mng         │ ShelfLife               │ employee_id → employee.sfid                                   │                    │
 * │   no    │ hqreview__c                        │ hq_review                   │ HqReview                │ —                                                             │                    │
 * │   no    │ if_product__c                      │ if_product                  │ InterfaceProduct        │ —                                                             │                    │
 * │   no    │ monthlysaleshistory__c             │ monthly_sales_history       │ MonthlySalesHistory     │ —                                                             │                    │
 * │   no    │ product_favorites                  │ product_favorites           │ FavoriteProduct         │ —                                                             │                    │
 * │  YES    │ pushmessage__c                     │ push_message                │ PushMessage             │ —                                                             │                    │
 * │   no    │ pushmessagereceiver__c             │ push_message_receiver       │ PushMessageReceiver     │ employeeid__c → employee.sfid, messageid__c → pushmessage.sfid │                    │
 * │   no    │ staffreview__c                     │ staff_review                │ StaffReview             │ dkretail_employeeid__c → employee.sfid                        │                    │
 * │   no    │ theme__c                           │ inspection_theme            │ InspectionTheme         │ —                                                             │                    │
 * │   no    │ tmp_claim                          │ tmp_claim                   │ TmpClaim                │ —                                                             │                    │
 * │   no    │ tmp_claimcode                      │ tmp_claimcode               │ —                       │ —                                                             │                    │
 * │   no    │ tmp_onsite                         │ —                           │ —                       │ —                                                             │ Heroku 전용        │
 * │   no    │ tmp_order                          │ —                           │ —                       │ —                                                             │ Heroku 전용        │
 * │   no    │ tmp_order_product                  │ —                           │ —                       │ —                                                             │ Heroku 전용        │
 * │   no    │ tmp_promotion                      │ —                           │ —                       │ —                                                             │ Heroku 전용        │
 * │   no    │ tmp_suggest                        │ —                           │ —                       │ —                                                             │ Heroku 전용        │
 * │   no    │ uploadfile__c                      │ upload_file                 │ UploadFile              │ recordid__c → 다형성 sfid (여러 오브젝트 참조)                 │                    │
 * │   no    │ _hcmeta                            │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _sf_event_log                      │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _trigger_log                       │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _trigger_log_archive               │ —                           │ —                       │ —                                                             │ 시스템             │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │   예외  │ device_version_mng                 │ device_version              │ DeviceVersion           │ —                                                             │ migration 대상 아님 │
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
        /** true이면 @Id 필드를 INSERT에 포함 (자연키 PK). false(기본)이면 제외 (IDENTITY 자동채번) */
        val includeId: Boolean = false,
        /** Heroku 타임스탬프 소스 컬럼명. null이면 createddate/systemmodstamp 자동감지 */
        val timestampColumns: Pair<String, String>? = null,
        val columnTransformProvider: ((Connection) -> Map<String, Map<String, Any?>>)? = null,
    )

    /** @HCTable 엔티티 등록. 추가 엔티티는 여기에 추가. 순서가 마이그레이션 순서를 결정 */
    private val entities = listOf(
        EntityRegistration("account", Account::class.java),
        EntityRegistration("product", Product::class.java),
        EntityRegistration("safetyCheckItem", SafetyCheckItem::class.java),
        EntityRegistration("employee", Employee::class.java, dependentTables = listOf("employee_info")),
        EntityRegistration(
            "employeeInfo", EmployeeInfo::class.java,
            includeId = true,
            timestampColumns = Pair("inst_date", "upd_date"),
        ),
        EntityRegistration("productBarcode", ProductBarcode::class.java),
        EntityRegistration("notice", Notice::class.java),
        EntityRegistration("displayWorkSchedule", DisplayWorkSchedule::class.java),
        EntityRegistration("teamMemberSchedule", TeamMemberSchedule::class.java),
        EntityRegistration("educationPost", EducationPost::class.java),
        EntityRegistration("agreementWord", AgreementWord::class.java),
        EntityRegistration("pushMessage", PushMessage::class.java),
        EntityRegistration("safetyCheckSubmission", SafetyCheckSubmission::class.java),
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
                    migrateEntity(reg.name, reg.entityClass, herokuConn, targetConn, columnTransforms, reg.dependentTables, reg.includeId, reg.timestampColumns)
                }

                // Notice: employee_sfid → employee_id 역참조 UPDATE
                println("[notice] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.notice n " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE n.employee_sfid = e.sfid"
                    )
                    println("[notice] employee_id UPDATE 완료: ${updated}건")
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

                // DisplayWorkSchedule: employee_sfid → employee_id 역참조 UPDATE
                println("[displayWorkSchedule] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.display_work_schedule d " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE d.employee_sfid = e.sfid"
                    )
                    println("[displayWorkSchedule] employee_id UPDATE 완료: ${updated}건")
                }

                // TeamMemberSchedule: account_sfid → account_id 역참조 UPDATE
                println("[teamMemberSchedule] account_sfid → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.team_member_schedule t " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE t.account_sfid = a.sfid"
                    )
                    println("[teamMemberSchedule] account_id UPDATE 완료: ${updated}건")
                }

                // TeamMemberSchedule: employee_sfid → employee_id 역참조 UPDATE
                println("[teamMemberSchedule] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.team_member_schedule t " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.employee_sfid = e.sfid"
                    )
                    println("[teamMemberSchedule] employee_id UPDATE 완료: ${updated}건")
                }

                // TeamMemberSchedule: team_leader_sfid → team_leader_id 역참조 UPDATE
                println("[teamMemberSchedule] team_leader_sfid → team_leader_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.team_member_schedule t " +
                            "SET team_leader_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.team_leader_sfid = e.sfid"
                    )
                    println("[teamMemberSchedule] team_leader_id UPDATE 완료: ${updated}건")
                }

                // TeamMemberSchedule: promotion_employee_sfid → promotion_employee_id 역참조 UPDATE
                println("[teamMemberSchedule] promotion_employee_sfid → promotion_employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.team_member_schedule t " +
                            "SET promotion_employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.promotion_employee_sfid = e.sfid"
                    )
                    println("[teamMemberSchedule] promotion_employee_id UPDATE 완료: ${updated}건")
                }

                // SafetyCheckSubmission: employee_sfid → employee_id 역참조 UPDATE
                println("[safetyCheckSubmission] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.safety_check_submission s " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE s.employee_sfid = e.sfid"
                    )
                    println("[safetyCheckSubmission] employee_id UPDATE 완료: ${updated}건")
                }

                // SafetyCheckSubmission: (employee_sfid, working_date, display_work_schedule_sfid) 중복 행 제거
                // — 레거시에 유니크 제약이 없어 중복 삽입된 불완전 행(start_time IS NULL) 삭제
                println("[safetyCheckSubmission] 중복 행 제거 중...")
                targetConn.createStatement().use { stmt ->
                    val deleted = stmt.executeUpdate(
                        """DELETE FROM $TARGET_SCHEMA.safety_check_submission
                           WHERE safety_check_submission_id IN (
                               SELECT safety_check_submission_id FROM (
                                   SELECT safety_check_submission_id,
                                          ROW_NUMBER() OVER (
                                              PARTITION BY employee_sfid, working_date, display_work_schedule_sfid
                                              ORDER BY start_time IS NULL, safety_check_submission_id
                                          ) AS rn
                                   FROM $TARGET_SCHEMA.safety_check_submission
                                   WHERE display_work_schedule_sfid IS NOT NULL
                               ) sub WHERE sub.rn > 1
                           )"""
                    )
                    if (deleted > 0) println("[safetyCheckSubmission] 중복 ${deleted}건 삭제")
                }

                // SafetyCheckSubmission: display_work_schedule_sfid → display_work_schedule_id 역참조 UPDATE
                println("[safetyCheckSubmission] display_work_schedule_sfid → display_work_schedule_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.safety_check_submission s " +
                            "SET display_work_schedule_id = d.display_work_schedule_id " +
                            "FROM $TARGET_SCHEMA.display_work_schedule d " +
                            "WHERE s.display_work_schedule_sfid = d.sfid"
                    )
                    println("[safetyCheckSubmission] display_work_schedule_id UPDATE 완료: ${updated}건")
                }

                // SafetyCheckSubmission: team_member_schedule_sfid → team_member_schedule_id 역참조 UPDATE
                println("[safetyCheckSubmission] team_member_schedule_sfid → team_member_schedule_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.safety_check_submission s " +
                            "SET team_member_schedule_id = t.team_member_schedule_id " +
                            "FROM $TARGET_SCHEMA.team_member_schedule t " +
                            "WHERE s.team_member_schedule_sfid = t.sfid"
                    )
                    println("[safetyCheckSubmission] team_member_schedule_id UPDATE 완료: ${updated}건")
                }

                // EducationPost: emp_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[educationPost] emp_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.education_post ep " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE ep.emp_code = e.employee_code"
                    )
                    println("[educationPost] employee_id UPDATE 완료: ${updated}건")
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
        includeId: Boolean = false,
        timestampColumns: Pair<String, String>? = null,
    ) {
        val hcTableName = entityClass.getAnnotation(HCTable::class.java)?.value
            ?: throw IllegalArgumentException("$name: @HCTable 어노테이션 없음")
        val targetTableName = entityClass.getAnnotation(Table::class.java)?.name
            ?: hcTableName

        // @Id 필드의 JPA 컬럼명 수집 (PK 제외용 — includeId=true이면 제외하지 않음)
        val idColumnNames = if (includeId) emptySet() else entityClass.declaredFields
            .filter { it.isAnnotationPresent(Id::class.java) }
            .mapNotNull { it.getAnnotation(jakarta.persistence.Column::class.java)?.name }
            .toSet()

        // @HCColumn 매핑에서 PK 제외 (includeId=true이면 PK도 포함)
        val mappings = SFSchemaUtils.getHCFieldMappings(entityClass)
            .filter { it.jpaColumnName !in idColumnNames }

        // 1. Heroku DB에서 읽기 (필터링된 매핑 + 타임스탬프 조건부)
        val mappedJpaColumns = mappings.map { it.jpaColumnName }.toSet()
        val hasTimestamp = if (timestampColumns != null) {
            true // 명시적 타임스탬프 컬럼 지정
        } else {
            hasColumn(herokuConn, HEROKU_SCHEMA, hcTableName, "createddate")
                && "created_at" !in mappedJpaColumns // @HCColumn로 이미 매핑된 경우 중복 방지
        }
        val tsCreated = timestampColumns?.first ?: "createddate"
        val tsUpdated = timestampColumns?.second ?: "systemmodstamp"

        val selectColumns = mappings.map { m ->
            if (m.hcColumnName == m.jpaColumnName) "\"${m.hcColumnName}\""
            else "\"${m.hcColumnName}\" AS ${m.jpaColumnName}"
        } + if (hasTimestamp) listOf("$tsCreated AS created_at", "$tsUpdated AS updated_at") else emptyList()

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

        // 2. 대상 테이블 초기화 (RESTART IDENTITY로 PK 시퀀스도 1부터 리셋)
        targetConn.createStatement().use { stmt ->
            // FK 없이 연관된 종속 테이블 먼저 TRUNCATE (DB CASCADE 미적용 대상)
            dependentTables.forEach { depTable ->
                stmt.execute("TRUNCATE TABLE $TARGET_SCHEMA.$depTable RESTART IDENTITY CASCADE")
                println("[$name] 종속 테이블 $depTable TRUNCATE 완료")
            }
            stmt.execute("TRUNCATE TABLE $TARGET_SCHEMA.$targetTableName RESTART IDENTITY CASCADE")
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
