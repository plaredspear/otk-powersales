package com.otoki.powersales.common.migration

import com.otoki.powersales.common.entity.AgreementWord
import com.otoki.powersales.common.entity.EmployeeAdmin
import com.otoki.powersales.common.entity.LoginHistory
import com.otoki.powersales.common.entity.HqReview
import com.otoki.powersales.common.entity.PushMessage
import com.otoki.powersales.common.entity.PushMessageReceiver
import com.otoki.powersales.common.entity.ProductSyncBuffer
import com.otoki.powersales.common.entity.StaffReview
import com.otoki.powersales.draft.entity.TmpClaim
import com.otoki.powersales.draft.entity.TmpClaimCode
import com.otoki.powersales.draft.entity.TmpOnsite
import com.otoki.powersales.draft.entity.TmpOrder
import com.otoki.powersales.draft.entity.TmpOrderProduct
import com.otoki.powersales.draft.entity.TmpPromotion
import com.otoki.powersales.draft.entity.TmpSuggest
import com.otoki.powersales.inspection.entity.InspectionTheme
import com.otoki.powersales.common.salesforce.HCTable
import com.otoki.powersales.common.salesforce.SFSchemaUtils
import com.otoki.powersales.safetycheck.entity.SafetyCheckItem
import com.otoki.powersales.safetycheck.entity.SafetyCheckSubmission
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.employee.entity.EmployeeInfo
import com.otoki.powersales.product.entity.Product
import com.otoki.powersales.notice.entity.Notice
import com.otoki.powersales.schedule.entity.DisplayWorkSchedule
import com.otoki.powersales.schedule.entity.TeamMemberSchedule
import com.otoki.powersales.education.entity.EducationCode
import com.otoki.powersales.education.entity.EducationPost
import com.otoki.powersales.education.entity.EducationPostAttachment
import com.otoki.powersales.education.entity.EducationViewHistory
import com.otoki.powersales.sales.entity.MonthlySalesHistory
import com.otoki.powersales.product.entity.FavoriteProduct
import com.otoki.powersales.productexpiration.entity.ProductExpiration
import com.otoki.powersales.product.entity.ProductBarcode
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.io.File
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Timestamp
import java.time.LocalDateTime
import java.util.Properties
import kotlin.collections.iterator

/**
 * Heroku DB → Dev DB 데이터 마이그레이션 도구
 * Spring 컨텍스트 없이 독립 실행. @HCTable/@HCColumn 어노테이션 기반 매핑.
 *
 * ┌──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┐
 * │ Migrate │ Heroku DB (salesforce2)            │ Dev DB (powersales)         │ Entity                  │ 참조키 (sfid FK)                                              │ 비고               │
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
 * │  YES    │ dkretail__teammemberschedule__c    │ team_member_schedule        │ TeamMemberSchedule      │ accountid__c → account.sfid, dkretail__employeeid__c →         │ UPDATE: account_id, employee_id, team_leader_id │
 * │         │                                    │                             │                         │   employee.sfid, teamleadersfid__c → employee.sfid,           │ (promotion_employee_id 는 SF 전용 → SalesforceMigrationTool 단계) │
 * │         │                                    │                             │                         │   dkretail__promotionempid__c → promotion_employee.sfid (SF)  │                    │
 * │  YES    │ safetycheck__workschedule__member  │ safety_check_submission     │ SafetyCheckSubmission   │ employeeid__c → employee.sfid, masterId →                     │ FK: employee_id(inline), display_work_schedule_id/team_member_schedule_id(post-UPDATE) │
 * │         │                                    │                             │                         │   displayworkschedulemaster__c.sfid, eventmasterid → sfid     │                    │
 * │  YES    │ education_mng                      │ education_post              │ EducationPost           │ empcode__c → employee.employee_code                           │ UPDATE: employee_id │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │  YES    │ agreementword__c                   │ agreement_word              │ AgreementWord           │ —                                                             │                    │
 * │   no    │ commute_distance                   │ —                           │ —                       │ —                                                             │                    │
 * │  YES    │ education_code_mng                 │ education_code              │ EducationCode           │ —                                                             │                    │
 * │  YES    │ education_file_mng                 │ education_post_attachment   │ EducationPostAttachment │ edu_id → education_post.edu_id                                │ UPDATE: education_post_id │
 * │  YES    │ education_member_history           │ education_view_history      │ EducationViewHistory    │ community_id → education_post.edu_id, empcode__c → employee.employee_code │ UPDATE: education_post_id, employee_id │
 * │  YES    │ employee_admin_mng                 │ employee_admin              │ EmployeeAdmin           │ —                                                             │                    │
 * │  YES    │ employee_his                       │ login_history               │ LoginHistory                    │ —                                                             │                    │
 * │  YES    │ employee_mng                       │ employee_info               │ EmployeeInfo            │ —                                                             │ Employee 종속 테이블, 자연키 PK │
 * │  YES    │ expirationdate__mng                │ product_expiration          │ ProductExpiration       │ account_code → account.external_key, product_code → product.product_code, employee_id → employee.sfid │ UPDATE: account_id, product_id, employee_id │
 * │  YES    │ hqreview__c                        │ hq_review                   │ HqReview                │ —                                                             │                    │
 * │  YES    │ if_product__c                      │ product_sync_buffer         │ ProductSyncBuffer       │ —                                                             │                    │
 * │  YES    │ monthlysaleshistory__c             │ monthly_sales_history       │ MonthlySalesHistory     │ —                                                             │                    │
 * │  YES    │ product_favorites                  │ product_favorite            │ FavoriteProduct         │ —                                                             │                    │
 * │  YES    │ pushmessage__c                     │ push_message                │ PushMessage             │ —                                                             │                    │
 * │  YES    │ pushmessagereceiver__c             │ push_message_receiver       │ PushMessageReceiver     │ employeeid__c → employee.sfid, messageid__c → pushmessage.sfid │ UPDATE: employee_id, push_message_id │
 * │  YES    │ staffreview__c                     │ staff_review                │ StaffReview             │ dkretail_employeeid__c → employee.sfid                        │                    │
 * │  YES    │ theme__c                           │ inspection_theme            │ InspectionTheme         │ —                                                             │                    │
 * │  YES    │ tmp_claim                          │ tmp_claim                   │ TmpClaim                │ sap_account_code → account.external_key, employee_code → employee.employee_code, product_code → product.product_code │ UPDATE: account_id, employee_id, product_id │
 * │  YES    │ tmp_claimcode                      │ tmp_claim_code              │ TmpClaimCode            │ —                                                             │                    │
 * │  YES    │ tmp_onsite                         │ tmp_onsite                  │ TmpOnsite               │ sap_account_code → account.external_key, employee_code → employee.employee_code, product_code → product.product_code, theme_code → inspection_theme.name │ UPDATE: account_id, employee_id, product_id, inspection_theme_id │
 * │  YES    │ tmp_order                          │ tmp_order                   │ TmpOrder                │ employee_code → employee.employee_code, account_code → account.external_key │ UPDATE: employee_id, account_id │
 * │  YES    │ tmp_order_product                  │ tmp_order_product           │ TmpOrderProduct         │ employee_code → employee.employee_code, product_code → product.product_code │ UPDATE: employee_id, product_id │
 * │  YES    │ tmp_promotion                      │ tmp_promotion               │ TmpPromotion            │ employee_code → employee.employee_code, promotion_product_code → product.product_code │ UPDATE: employee_id, product_id │
 * │  YES    │ tmp_suggest                        │ tmp_suggest                 │ TmpSuggest              │ employee_code → employee.employee_code, product_code → product.product_code, account_code → account.external_key │ UPDATE: employee_id, product_id, account_id │
 * │   no    │ professionalpromotionteammaster__c │ professional_promotion_team_master │ ProfessionalPromotionTeamMaster │ account__c → account.sfid, employeenumber__c → employee.employee_code │ Heroku 미동기화, SF API 전용 │
 * │   no    │ uploadfile__c                      │ upload_file                 │ UploadFile              │ recordid__c → 다형성 sfid (여러 오브젝트 참조)                 │                    │
 * │   no    │ _hcmeta                            │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _sf_event_log                      │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _trigger_log                       │ —                           │ —                       │ —                                                             │ 시스템             │
 * │   no    │ _trigger_log_archive               │ —                           │ —                       │ —                                                             │ 시스템             │
 * ├──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┤
 * │   예외  │ device_version_mng                 │ device_version              │ DeviceVersion           │ —                                                             │ migration 대상 아님 │
 * └──────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────────┘
 *
 * 실행 (db-tunnel.sh / hc-import.sh 와 동일한 stage 컨벤션):
 *   사전 조건:
 *     - 별도 터미널에서 `scripts/db-tunnel.sh -s <stage>` 가 떠 있어야 함
 *         dev  → localhost:15432, prod → localhost:25432
 *     - 타겟 DB 비밀번호 환경변수: dev → DEV_OTK_PWRS_DB_PASSWORD, prod → PROD_OTK_PWRS_DB_PASSWORD
 *     - HEROKU_DB_PASSWORD 환경변수 또는 docs/plan/old-accounts.json 자동 로드
 *
 *   dev (기본):
 *     ./gradlew migrateHeroku
 *
 *   prod (추가 confirm 프롬프트):
 *     STAGE=prod ./gradlew migrateHeroku
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
        EntityRegistration("educationPostAttachment", EducationPostAttachment::class.java),
        EntityRegistration("educationViewHistory", EducationViewHistory::class.java),
        EntityRegistration("educationCode", EducationCode::class.java),
        EntityRegistration("employeeAdmin", EmployeeAdmin::class.java, includeId = true),
        EntityRegistration("agreementWord", AgreementWord::class.java),
        EntityRegistration("pushMessage", PushMessage::class.java),
        EntityRegistration("pushMessageReceiver", PushMessageReceiver::class.java),
        EntityRegistration("hqReview", HqReview::class.java),
        EntityRegistration("loginHistory", LoginHistory::class.java),
        EntityRegistration("safetyCheckSubmission", SafetyCheckSubmission::class.java),
        EntityRegistration("monthlySalesHistory", MonthlySalesHistory::class.java),
        EntityRegistration("productExpiration", ProductExpiration::class.java),
        EntityRegistration("favoriteProduct", FavoriteProduct::class.java, includeId = true),
        EntityRegistration("inspectionTheme", InspectionTheme::class.java),
        EntityRegistration("productSyncBuffer", ProductSyncBuffer::class.java),
        EntityRegistration("staffReview", StaffReview::class.java),
        EntityRegistration("tmpClaim", TmpClaim::class.java),
        EntityRegistration("tmpClaimCode", TmpClaimCode::class.java),
        EntityRegistration("tmpOnsite", TmpOnsite::class.java),
        EntityRegistration("tmpOrder", TmpOrder::class.java),
        EntityRegistration("tmpOrderProduct", TmpOrderProduct::class.java),
        EntityRegistration("tmpPromotion", TmpPromotion::class.java),
        EntityRegistration("tmpSuggest", TmpSuggest::class.java),
    )

    private const val HEROKU_SCHEMA = "salesforce2"
    private const val TARGET_SCHEMA = "powersales"
    private const val BATCH_SIZE = 1000

    // Heroku DB (읽기 전용)
    private const val HEROKU_HOST = "ec2-13-159-136-112.ap-northeast-1.compute.amazonaws.com"
    private const val HEROKU_PORT = "5432"
    private const val HEROKU_DB = "dc7oam5vbbb7d7"
    private const val HEROKU_USER = "u4bee3ek26k44g"
    private const val HEROKU_URL = "jdbc:postgresql://$HEROKU_HOST:$HEROKU_PORT/$HEROKU_DB?sslmode=require"

    // 타겟 DB (stage 별 분기 — db-tunnel.sh / hc-import.sh 컨벤션 일치)
    // 모든 stage 는 SSM 터널 경유로 localhost 접속.
    private val STAGE = (System.getenv("STAGE") ?: "dev").lowercase()

    private data class TargetConfig(
        val host: String,
        val port: String,
        val user: String,
        val passwordEnvVar: String,
    )

    private val TARGET = when (STAGE) {
        "dev" -> TargetConfig("localhost", "15432", "otkadmin", "DEV_OTK_PWRS_DB_PASSWORD")
        "prod" -> TargetConfig("localhost", "25432", "postgres", "PROD_OTK_PWRS_DB_PASSWORD")
        else -> error("지원하지 않는 STAGE: $STAGE (지원: dev | prod)")
    }

    private const val TARGET_DB = "otoki"
    private val TARGET_URL = "jdbc:postgresql://${TARGET.host}:${TARGET.port}/$TARGET_DB"
    private val TARGET_USER = TARGET.user
    private val TARGET_PASSWORD = System.getenv(TARGET.passwordEnvVar) ?: ""

    @JvmStatic
    fun main(args: Array<String>) {
        println("=== Heroku → 타겟 DB 마이그레이션 (stage=$STAGE) ===")

        // 1. 타겟 DB 비밀번호 검증
        if (TARGET_PASSWORD.isBlank()) {
            println("ERROR: 환경변수 \$${TARGET.passwordEnvVar} 가 설정되지 않았습니다.")
            println("  scripts/db-tunnel.sh -s $STAGE --password 로 조회 후 export 하세요.")
            return
        }

        // 2. HEROKU_DB_PASSWORD 로드 (env var → docs/plan/old-accounts.json 자동 탐색)
        val herokuPassword = System.getenv("HEROKU_DB_PASSWORD")
            ?: loadHerokuPasswordFromAccountsJson()
            ?: run {
                println("ERROR: HEROKU_DB_PASSWORD 환경변수도 없고, docs/plan/old-accounts.json 도 찾지 못했습니다.")
                println("  방법 1: export HEROKU_DB_PASSWORD=\$(jq -r '.[\"dev-heroku-db\"].PASSWORD' docs/plan/old-accounts.json)")
                println("  방법 2: docs/plan/old-accounts.json 을 worktree 상위 (otoki/) 또는 main/ 에 두기")
                return
            }

        // 3. SSM 터널 alive 체크
        if (!isPortOpen(TARGET.host, TARGET.port.toInt())) {
            println("ERROR: 타겟 포트가 열려있지 않습니다: ${TARGET.host}:${TARGET.port}")
            println("  별도 터미널에서 다음을 먼저 실행하세요: scripts/db-tunnel.sh -s $STAGE")
            return
        }

        // 4. prod 안전 확인
        if (STAGE == "prod") {
            print("⚠️  운영(prod) DB 의 powersales 스키마를 모두 TRUNCATE → INSERT 합니다. 계속? [y/N] ")
            val answer = readLine()?.trim()?.lowercase()
            if (answer != "y") {
                println("사용자에 의해 취소되었습니다.")
                return
            }
        }

        println("타겟: ${TARGET.host}:${TARGET.port}/$TARGET_DB (user=$TARGET_USER)")
        println("=== 시작 (${entities.size}개 엔티티) ===")

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

                // TeamMemberSchedule.promotion_employee_id (FK to promotion_employee.promotion_employee_id) 는
                // 본 단계에서 채우지 않는다. promotion_employee_sfid 는 PromotionEmployee(SF 전용 @SFObject) 의
                // sfid 형식이므로 employee.sfid 와 매칭되지 않으며, promotion_employee 마스터 자체가 HC 동기 대상
                // 아니므로 마이그레이션 시점에 비어있다. 향후 SalesforceMigrationTool 구현 시 처리.
                //
                // 동일 사유로 다음 SF 전용 엔티티의 *_sfid → *_id UPDATE 도 본 도구 범위 외:
                //   - Promotion (account_sfid, primary_product_sfid)
                //   - PromotionEmployee (promotion_sfid, team_member_schedule_sfid)
                //   - MonthlyFemaleEmployeeIntegrationSchedule (account_sfid, employee_sfid)
                //   - ProfessionalPromotionTeamMaster (account_sfid)
                //   - ProfessionalPromotionTeamHistory (employee_sfid)

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

                // StaffReview: employee_sfid → employee_id 역참조 UPDATE
                println("[staffReview] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.staff_review s " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE s.employee_sfid = e.sfid"
                    )
                    println("[staffReview] employee_id UPDATE 완료: ${updated}건")
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

                // EducationPostAttachment: edu_id → education_post.edu_id 매칭으로 education_post_id UPDATE
                println("[educationPostAttachment] edu_id → education_post_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.education_post_attachment epa " +
                            "SET education_post_id = ep.education_post_id " +
                            "FROM $TARGET_SCHEMA.education_post ep " +
                            "WHERE epa.edu_id = ep.edu_id"
                    )
                    println("[educationPostAttachment] education_post_id UPDATE 완료: ${updated}건")
                }

                // EducationViewHistory: edu_id → education_post.edu_id 매칭으로 education_post_id UPDATE
                println("[educationViewHistory] edu_id → education_post_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.education_view_history evh " +
                            "SET education_post_id = ep.education_post_id " +
                            "FROM $TARGET_SCHEMA.education_post ep " +
                            "WHERE evh.edu_id = ep.edu_id"
                    )
                    println("[educationViewHistory] education_post_id UPDATE 완료: ${updated}건")
                }

                // EducationViewHistory: emp_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[educationViewHistory] emp_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.education_view_history evh " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE evh.emp_code = e.employee_code"
                    )
                    println("[educationViewHistory] employee_id UPDATE 완료: ${updated}건")
                }

                // ProductExpiration: account_code → account_id 역참조 UPDATE
                println("[productExpiration] account_code → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.product_expiration pe " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE pe.account_code = a.external_key"
                    )
                    println("[productExpiration] account_id UPDATE 완료: ${updated}건")
                }

                // ProductExpiration: product_code → product_id 역참조 UPDATE
                println("[productExpiration] product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.product_expiration pe " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE pe.product_code = p.product_code"
                    )
                    println("[productExpiration] product_id UPDATE 완료: ${updated}건")
                }

                // ProductExpiration: employee_sfid → employee_id 역참조 UPDATE
                println("[productExpiration] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.product_expiration pe " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE pe.employee_sfid = e.sfid"
                    )
                    println("[productExpiration] employee_id UPDATE 완료: ${updated}건")
                }

                // PushMessageReceiver: employee_sfid → employee_id 역참조 UPDATE
                println("[pushMessageReceiver] employee_sfid → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.push_message_receiver r " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE r.employee_sfid = e.sfid"
                    )
                    println("[pushMessageReceiver] employee_id UPDATE 완료: ${updated}건")
                }

                // PushMessageReceiver: push_message_sfid → push_message_id 역참조 UPDATE
                println("[pushMessageReceiver] push_message_sfid → push_message_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.push_message_receiver r " +
                            "SET push_message_id = pm.push_message_id " +
                            "FROM $TARGET_SCHEMA.push_message pm " +
                            "WHERE r.push_message_sfid = pm.sfid"
                    )
                    println("[pushMessageReceiver] push_message_id UPDATE 완료: ${updated}건")
                }

                // MonthlySalesHistory: account_external_key → account.external_key 매칭으로 account_id UPDATE
                println("[monthlySalesHistory] account_external_key → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.monthly_sales_history msh " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE msh.account_external_key = a.external_key"
                    )
                    println("[monthlySalesHistory] account_id UPDATE 완료: ${updated}건")
                    val unmatched = stmt.executeQuery(
                        "SELECT COUNT(*) FROM $TARGET_SCHEMA.monthly_sales_history WHERE account_id IS NULL AND account_external_key IS NOT NULL"
                    )
                    if (unmatched.next()) {
                        val count = unmatched.getInt(1)
                        if (count > 0) println("[monthlySalesHistory] account_id 매칭 실패: ${count}건")
                    }
                }

                // TmpClaim: sap_account_code → account.external_key 매칭으로 account_id UPDATE
                println("[tmpClaim] sap_account_code → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_claim tc " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE tc.sap_account_code = a.external_key"
                    )
                    println("[tmpClaim] account_id UPDATE 완료: ${updated}건")
                }

                // TmpClaim: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpClaim] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_claim tc " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE tc.employee_code = e.employee_code"
                    )
                    println("[tmpClaim] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpClaim: product_code → product.product_code 매칭으로 product_id UPDATE
                println("[tmpClaim] product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_claim tc " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE tc.product_code = p.product_code"
                    )
                    println("[tmpClaim] product_id UPDATE 완료: ${updated}건")
                }

                // TmpOnsite: sap_account_code → account.external_key 매칭으로 account_id UPDATE
                println("[tmpOnsite] sap_account_code → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_onsite t " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE t.sap_account_code = a.external_key"
                    )
                    println("[tmpOnsite] account_id UPDATE 완료: ${updated}건")
                }

                // TmpOnsite: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpOnsite] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_onsite t " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.employee_code = e.employee_code"
                    )
                    println("[tmpOnsite] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpOnsite: product_code → product.product_code 매칭으로 product_id UPDATE
                println("[tmpOnsite] product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_onsite t " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE t.product_code = p.product_code"
                    )
                    println("[tmpOnsite] product_id UPDATE 완료: ${updated}건")
                }

                // TmpOnsite: theme_code → inspection_theme.name 매칭으로 inspection_theme_id UPDATE
                println("[tmpOnsite] theme_code → inspection_theme_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_onsite t " +
                            "SET inspection_theme_id = it.inspection_theme_id " +
                            "FROM $TARGET_SCHEMA.inspection_theme it " +
                            "WHERE t.theme_code = it.name"
                    )
                    println("[tmpOnsite] inspection_theme_id UPDATE 완료: ${updated}건")
                }

                // TmpOrder: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpOrder] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_order t " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.employee_code = e.employee_code"
                    )
                    println("[tmpOrder] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpOrder: account_code → account.external_key 매칭으로 account_id UPDATE
                println("[tmpOrder] account_code → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_order t " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE t.account_code = a.external_key"
                    )
                    println("[tmpOrder] account_id UPDATE 완료: ${updated}건")
                }

                // TmpOrderProduct: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpOrderProduct] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_order_product t " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE t.employee_code = e.employee_code"
                    )
                    println("[tmpOrderProduct] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpOrderProduct: product_code → product.product_code 매칭으로 product_id UPDATE
                println("[tmpOrderProduct] product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_order_product t " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE t.product_code = p.product_code"
                    )
                    println("[tmpOrderProduct] product_id UPDATE 완료: ${updated}건")
                }

                // TmpPromotion: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpPromotion] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_promotion tp " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE tp.employee_code = e.employee_code"
                    )
                    println("[tmpPromotion] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpPromotion: promotion_product_code → product.product_code 매칭으로 product_id UPDATE
                println("[tmpPromotion] promotion_product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_promotion tp " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE tp.promotion_product_code = p.product_code"
                    )
                    println("[tmpPromotion] product_id UPDATE 완료: ${updated}건")
                }

                // TmpSuggest: employee_code → employee.employee_code 매칭으로 employee_id UPDATE
                println("[tmpSuggest] employee_code → employee_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_suggest ts " +
                            "SET employee_id = e.employee_id " +
                            "FROM $TARGET_SCHEMA.employee e " +
                            "WHERE ts.employee_code = e.employee_code"
                    )
                    println("[tmpSuggest] employee_id UPDATE 완료: ${updated}건")
                }

                // TmpSuggest: product_code → product.product_code 매칭으로 product_id UPDATE
                println("[tmpSuggest] product_code → product_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_suggest ts " +
                            "SET product_id = p.product_id " +
                            "FROM $TARGET_SCHEMA.product p " +
                            "WHERE ts.product_code = p.product_code"
                    )
                    println("[tmpSuggest] product_id UPDATE 완료: ${updated}건")
                }

                // TmpSuggest: account_code → account.external_key 매칭으로 account_id UPDATE
                println("[tmpSuggest] account_code → account_id UPDATE 중...")
                targetConn.createStatement().use { stmt ->
                    val updated = stmt.executeUpdate(
                        "UPDATE $TARGET_SCHEMA.tmp_suggest ts " +
                            "SET account_id = a.account_id " +
                            "FROM $TARGET_SCHEMA.account a " +
                            "WHERE ts.account_code = a.external_key"
                    )
                    println("[tmpSuggest] account_id UPDATE 완료: ${updated}건")
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
            .mapNotNull { it.getAnnotation(Column::class.java)?.name }
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

        // 타임스탬프 NULL 보정 (Heroku 데이터 일부 NULL 존재)
        val createdAtIdx = allJpaColumns.indexOf("created_at")
        val updatedAtIdx = allJpaColumns.indexOf("updated_at")
        if (createdAtIdx >= 0 && updatedAtIdx >= 0) {
            val now = Timestamp.valueOf(LocalDateTime.now())
            rows.forEach { row ->
                if (row[createdAtIdx] == null) row[createdAtIdx] = now
                if (row[updatedAtIdx] == null) row[updatedAtIdx] = row[createdAtIdx]
            }
        }

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
     * docs/plan/old-accounts.json 에서 dev-heroku-db.PASSWORD 를 읽는다.
     * docs/ 는 worktree 별로 두지 않고 otoki/ 프로젝트 루트에 위치하므로 후보 경로를 순서대로 탐색.
     * working dir 은 backend/ 이므로 ../../docs (otoki 루트) 가 표준이고, ../docs (worktree 내부) 는 fallback.
     */
    private fun loadHerokuPasswordFromAccountsJson(): String? {
        val candidates = listOf(
            File("../../docs/plan/old-accounts.json"),  // 표준: otoki/docs (worktree 상위)
            File("../docs/plan/old-accounts.json"),     // fallback: worktree 내부
        )
        val file = candidates.firstOrNull { it.exists() } ?: return null
        return try {
            val mapper = com.fasterxml.jackson.databind.ObjectMapper()
            val node = mapper.readTree(file)
            node.path("dev-heroku-db").path("PASSWORD").asText().takeIf { it.isNotBlank() }
        } catch (e: Exception) {
            println("WARN: ${file.path} 파싱 실패: ${e.message}")
            null
        }
    }

    /** TCP 포트가 열려있는지 1초 timeout 으로 체크 (SSM 터널 alive 확인용) */
    private fun isPortOpen(host: String, port: Int): Boolean {
        return try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(host, port), 1000)
                true
            }
        } catch (e: Exception) {
            false
        }
    }
}
