package com.otoki.powersales._migration.sf.service

import com.otoki.powersales.platform.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS
import com.otoki.powersales.domain.support.notice.service.NoticeImagePlaceholder
import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales.platform.auth.permission.LeaderProfileFlagsSeed
import com.otoki.powersales.platform.auth.permission.SystemAdminGrantList
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * SF 데이터 마이그레이션 Stage 2 — Logical 변환 (1회성 cut-over, 런칭 후 폐기).
 *
 * 본 클래스는 scripts/sf-data-migration/migrate-stage2.main.kts 의 backend 흡수본이다.
 * 운영 서버에서 실행되어 RDS 와의 latency 를 단축한다.
 *
 * 구현 substep:
 * - 2-B picklist : User.cost_center_code derived 캐시 동기화만 (Employee.role enum 변환은 spec #807 폐기)
 * - 2-C password : BCrypt password hash (sfid IS NOT NULL AND password NULL 인 user).
 *                  초기 평문은 고정 상수 [MIGRATION_INITIAL_PASSWORD] (최초 로그인 시 변경 강제).
 *
 * 2-A FK resolve 는 별도 클래스 (SfMigrationStage2FkService) 로 분리.
 * 2-D permission 은 spec #801 SF 권한 모델 전면 적용으로 폐기 — user_permission 테이블 자체 폐기.
 * 2-B user.profile_type substep 은 spec #806 의 ProfileType destructive 폐기로 제거.
 * 2-B employee.role substep 은 spec #807 의 UserRoleEnum destructive 폐기로 제거 —
 * SF AppAuthority picklist value (한글) 가 곧 저장값이라 변환 substep 자체가 불필요.
 */
@Service
class SfMigrationStage2Service(
    @PersistenceContext private val em: EntityManager,
    private val passwordEncoder: PasswordEncoder,
) {

    companion object {
        /**
         * 마이그레이션 대상 사용자의 초기 비밀번호 평문 (BCrypt 로 hash 되어 적재).
         *
         * User(web) / EmployeeInfo(mobile) 양쪽 password substep 이 동일 값을 공유한다
         * ([com.otoki.powersales._migration.heroku.service.HerokuMigrationStage2Service] 재사용).
         * 적재 시 `password_change_required = TRUE` 로 최초 로그인 시 강제 변경을 유도한다.
         */
        const val MIGRATION_INITIAL_PASSWORD = "pwrs1234!"

        /**
         * `leader-profile-flags` substep 의 적용 대상 profile.name.
         *
         * [LeaderProfileFlagsSeed.SEEDS] 는 조장 계열 2종(`6.조장` / `7.영업사원 + 조장`) 을 정의하나,
         * 본 substep 은 그중 `6.조장` 만 적용한다 (사용자 결정). `7.영업사원 + 조장` 은 web admin
         * 권한 편집으로 수동 처리. 대상 확대 시 본 집합에 이름을 추가하면 된다.
         */
        val LEADER_FLAGS_TARGET_PROFILE_NAMES = setOf("6.조장")

        // 본문 HTML 의 <img ...> 태그 전체 (rtaImage 포함 여부는 src 추출 후 판정).
        private val RTA_IMG_REGEX = Regex("""<img\b[^>]*>""", RegexOption.IGNORE_CASE)
        // <img> 태그 안의 src 값 — rtaImage 서블릿 URL 만 대상 (이미 placeholder(notice-image://)면 미매칭 → skip).
        private val RTA_SRC_REGEX =
            Regex("""\bsrc\s*=\s*"([^"]*rtaImage[^"]*)"""", RegexOption.IGNORE_CASE)
        // <img> 태그 안의 alt 값 (placeholder 로 보존).
        private val RTA_ALT_REGEX =
            Regex("""\balt\s*=\s*"([^"]*)"""", RegexOption.IGNORE_CASE)
        // src URL 의 파라미터 추출용 (refid). url 이 &amp; 인코딩 형태일 수 있으므로 디코딩 후 파싱.
        private fun rtaParam(url: String, key: String): String {
            val decoded = unescapeHtml(url)
            val m = Regex("""[?&]${Regex.escape(key)}=([^&]+)""").find(decoded) ?: return ""
            return m.groupValues[1]
        }

        // 최소 HTML 엔티티 디코딩 (&amp; / &quot; / &lt; / &gt; / &#39;). src/alt 파싱용.
        private fun unescapeHtml(s: String): String = s
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }

    /**
     * Stage 2-B picklist — User.cost_center_code derived 캐시 동기화만.
     */
    @Transactional
    fun runPicklistMapping(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        results += runUserCostCenterCodeSync().results
        results += runPptMasterBranchCodeSync().results

        return SfMigrationStage2Response(
            substep = "picklist",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * Stage 2-B (user.cost_center_code) — Employee.cost_center_code → User.cost_center_code derived 캐시 동기화.
     *
     * 상관 서브쿼리 형태 — H2 / PostgreSQL 양쪽 모두 표준 SQL 로 동작.
     */
    @Transactional
    fun runUserCostCenterCodeSync(): SfMigrationStage2Response {
        val rows = em.createNativeQuery(
            """
            UPDATE powersales."user"
            SET cost_center_code = (
                SELECT e.cost_center_code FROM powersales.employee e
                WHERE e.employee_code = powersales."user".employee_code
            )
            WHERE employee_code IS NOT NULL
              AND EXISTS (
                SELECT 1 FROM powersales.employee e
                WHERE e.employee_code = powersales."user".employee_code
              )
            """.trimIndent()
        ).executeUpdate()
        return singleResultResponse(
            substep = "picklist.user_cost_center_code",
            label = "User.cost_center_code (sync from Employee)",
            rows = rows,
        )
    }

    /**
     * Stage 2-B (professional_promotion_team_master.branch_code) —
     * Employee.cost_center_code → ProfessionalPromotionTeamMaster.branch_code derived 동기화.
     *
     * branch_code 는 SF `CostCenterCode__c`(라벨 "조직유형") 에 매핑되어 있으나, 해당 SF 필드는
     * 운영에서 한 번도 채워지지 않은 dead field (extract 전 행 빈값) 라 SF 적재분의 branch_code 가
     * 전부 NULL 이다. 신규 등록 로직(AdminPPTMasterService.createMaster)은 사원의 cost_center_code 로
     * branch_code 를 채우므로, 마이그레이션 적재분도 동일 출처(employee_id → Employee.cost_center_code)
     * 로 동기화해 정합을 맞춘다.
     *
     * 상관 서브쿼리 형태 — H2 / PostgreSQL 양쪽 모두 표준 SQL 로 동작.
     * 멱등: branch_code IS NULL 한정이라 이미 채워진 row (신규 등록분 포함) 는 건드리지 않는다.
     */
    @Transactional
    fun runPptMasterBranchCodeSync(): SfMigrationStage2Response {
        val rows = em.createNativeQuery(
            """
            UPDATE powersales.professional_promotion_team_master ppt
            SET branch_code = (
                SELECT e.cost_center_code FROM powersales.employee e
                WHERE e.employee_id = ppt.employee_id
            )
            WHERE ppt.branch_code IS NULL
              AND EXISTS (
                SELECT 1 FROM powersales.employee e
                WHERE e.employee_id = ppt.employee_id
                  AND e.cost_center_code IS NOT NULL
              )
            """.trimIndent()
        ).executeUpdate()
        return singleResultResponse(
            substep = "picklist.ppt_master_branch_code",
            label = "ProfessionalPromotionTeamMaster.branch_code (sync from Employee)",
            rows = rows,
        )
    }

    private fun singleResultResponse(substep: String, label: String, rows: Int): SfMigrationStage2Response =
        SfMigrationStage2Response(
            substep = substep,
            results = listOf(SubstepResult(label = label, rowsAffected = rows)),
            totalRowsAffected = rows,
        )

    /**
     * Stage 2-A (polymorphic parent) — UploadFile 의 record_sfid 로 부모 entity 를 찾아
     * parent_type (엔티티명) + parent_id (Long FK) 를 동시에 채운다.
     *
     * **record_sfid 직접 조인 방식**: SF Object__c (object_type) 는 모바일 등록 경로(claim /
     * site_activity)에서 미설정(NULL)이라 신뢰할 수 없다. 대신 record_sfid (부모 SObject Id)
     * 를 각 부모 테이블의 sfid 와 조인해 **실제 매칭되는 테이블**을 부모로 확정한다. SF Id 는
     * 전역 유니크라 한 record_sfid 는 최대 한 부모 테이블에만 매칭된다 (충돌 없음).
     * object_type 은 SF 원본 보존용으로만 유지하며 본 resolve 의 분기 키로 쓰지 않는다.
     *
     * 일반 FK resolve (`SfMigrationStage2FkService`) 는 `*_sfid` → `*_id` 1:1 패턴만 처리하므로,
     * UploadFile 처럼 한 `record_sfid` 컬럼이 여러 entity 를 가리키는 polymorphic 케이스는 본
     * substep 이 별도 처리한다.
     *
     * 매핑 표는 [UPLOAD_FILE_POLYMORPHIC_PARENTS] (SoT) — entityName → (refTable, refIdColumn).
     * 한 entry 당 한 UPDATE 실행하며, 재호출 시 `parent_id IS NULL` 조건으로 멱등성 확보.
     */
    @Transactional
    fun runUploadFilePolymorphicParent(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        // record_sfid ↔ 부모 sfid 조인으로 parent_type (엔티티명) + parent_id 동시 설정.
        // object_type 무관. parent_id IS NULL 한정 (멱등) — 이미 연결된 row 는 건드리지 않는다.
        for ((entityName, spec) in UPLOAD_FILE_POLYMORPHIC_PARENTS) {
            val rows = em.createNativeQuery(
                """
                UPDATE powersales.upload_file uf
                SET parent_type = :entityName,
                    parent_id = c.${spec.refIdColumn}
                FROM powersales.${spec.refTable} c
                WHERE uf.record_sfid = c.sfid
                  AND uf.parent_id IS NULL
                """.trimIndent()
            )
                .setParameter("entityName", entityName)
                .executeUpdate()
            results += SubstepResult(
                label = "upload_file ($entityName ← record_sfid = ${spec.refTable}.sfid)",
                rowsAffected = rows,
            )
        }
        return SfMigrationStage2Response(
            substep = "uploadFilePolymorphicParent",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /**
     * Stage 2 (notice rtaImage placeholder) — 공지 본문(notice.contents) HTML 에 박힌 SF rtaImage 서블릿
     * `<img>` 태그를 만료 없는 placeholder `<img src="notice-image://{refid}" data-refid="{refid}">` 로 치환한다.
     *
     * 배경:
     *   공지 본문 인라인 이미지는 레거시에서 SF rtaImage 서블릿 URL (`...rtaImage?eid=...&refid=0EM...`) 로 본문
     *   HTML 에 박혀 있어 SF org 세션 인증에 묶인다. 신규 시스템은 이미지를 private S3 + presigned URL 로만
     *   조회하는데 presigned 는 만료되므로 본문에 완성 URL 을 박을 수 없다. 따라서 본문에는 만료 없는
     *   placeholder 만 영구 저장하고, 조회 시점에 [com.otoki.powersales.domain.support.notice.service.NoticeService.getNoticeDetail]
     *   가 data-refid 로 presigned URL 을 rewrite 한다.
     *
     * 처리:
     *   notice 전 행의 contents 를 조회해 본문 HTML 을 직접 파싱한다. `<img ...rtaImage...>` 태그마다 src 의
     *   `refid` 파라미터를 추출해 placeholder `<img>` 로 통째 교체한다 (src 만 바꾸면 data-refid 속성을 못 붙이므로
     *   태그 전체 교체). refid 가 없는 rtaImage 태그는 건너뛴다 (치환 불가 — 잔존 카운트로 보고).
     *
     * 멱등 (2단계 보증):
     *   1) SELECT 단계의 `contents LIKE '%rtaImage%'` 1차 필터 — 이미 치환 완료된 본문(placeholder 의 src 는
     *      `notice-image://` 라 rtaImage 미포함)은 조회조차 되지 않는다.
     *   2) 본문 순회 시 RTA_IMG_REGEX 가 모든 `<img>` 를 잡되, src 에 rtaImage 가 없는 태그(placeholder 포함)는
     *      RTA_SRC_REGEX 미매칭으로 원본 그대로 반환 → skip. 따라서 2회 실행해도 동일 결과.
     *   (참고) 조회 시점의 placeholder → presigned rewrite 는 NoticeService 가 NoticeImagePlaceholder 의
     *   정규식으로 별도 수행한다 — 본 치환과는 분리된 단계.
     *
     * @param dryRun true 면 실제 UPDATE 없이 변경 대상 집계만 (기본). false 면 commit.
     */
    @Transactional
    fun runNoticeRtaPlaceholderRewrite(dryRun: Boolean): SfMigrationStage2Response {
        @Suppress("UNCHECKED_CAST")
        val rows = em.createNativeQuery(
            "SELECT notice_id, contents FROM powersales.notice " +
                "WHERE contents IS NOT NULL AND contents LIKE '%rtaImage%' AND is_deleted IS NOT TRUE"
        ).resultList as List<Array<Any?>>

        var changedNotices = 0
        var replacedImages = 0
        var skippedNoRefid = 0
        var remainingRtaNotices = 0

        val updateStmt = if (!dryRun) {
            em.createNativeQuery("UPDATE powersales.notice SET contents = :contents WHERE notice_id = :id")
        } else {
            null
        }

        for (row in rows) {
            val id = (row[0] as Number).toLong()
            // contents 는 TEXT 컬럼 — 드라이버/모드에 따라 String 또는 java.sql.Clob 으로 올 수 있어 양쪽 처리.
            val original = when (val c = row[1]) {
                is String -> c
                is java.sql.Clob -> c.getSubString(1, c.length().toInt())
                else -> continue
            }

            var localReplaced = 0
            val rewritten = RTA_IMG_REGEX.replace(original) replace@{ match ->
                val tag = match.value
                val srcMatch = RTA_SRC_REGEX.find(tag) ?: return@replace tag
                val src = srcMatch.groupValues[1]
                val refid = rtaParam(src, "refid")
                if (refid.isEmpty()) {
                    skippedNoRefid++
                    return@replace tag
                }
                val alt = RTA_ALT_REGEX.find(tag)?.groupValues?.get(1)?.let { unescapeHtml(it) } ?: ""
                localReplaced++
                NoticeImagePlaceholder.build(refid, alt)
            }

            if (rewritten == original) continue

            replacedImages += localReplaced
            changedNotices++
            if (rewritten.contains("rtaImage")) remainingRtaNotices++

            if (!dryRun) {
                updateStmt!!.setParameter("contents", rewritten)
                    .setParameter("id", id)
                    .executeUpdate()
            }
        }

        val prefix = if (dryRun) "[DRY-RUN] " else ""
        val results = listOf(
            SubstepResult(label = "${prefix}변경 대상 공지 수", rowsAffected = changedNotices),
            SubstepResult(label = "${prefix}치환 이미지 수 (rtaImage <img> → placeholder)", rowsAffected = replacedImages),
            SubstepResult(label = "refid 없는 rtaImage 태그 (치환 불가 skip)", rowsAffected = skippedNoRefid),
            SubstepResult(label = "치환 후 rtaImage 잔존 공지 (확인 필요)", rowsAffected = remainingRtaNotices),
        )
        return SfMigrationStage2Response(
            substep = if (dryRun) "noticeRtaPlaceholder (dry-run)" else "noticeRtaPlaceholder",
            results = results,
            // 멱등/안전 운영을 위해 totalRowsAffected 는 "치환 이미지 수" 로 정의 (변경 규모 직관).
            totalRowsAffected = replacedImages,
        )
    }

    /**
     * 2-C password — SF 마이그레이션 대상 user 의 초기 비밀번호를 [MIGRATION_INITIAL_PASSWORD]
     * 고정 상수의 BCrypt hash 로 채운다.
     *
     * 대상: `sfid IS NOT NULL AND (password IS NULL OR password = '')`. 멱등 (이미 채워진 row skip).
     * `password_change_required = TRUE` 로 최초 로그인 시 강제 변경 유도.
     *
     * BCrypt salt 는 매 encode 마다 랜덤이라 사용자별 hash 는 서로 다르지만 평문은 모두 동일하다.
     * 사용자마다 hash 가 달라야 하므로 상수 hash 를 재사용하지 않고 row 별로 encode 한다.
     */
    @Transactional
    fun runPasswordHash(): SfMigrationStage2Response {
        val codesQuery = em.createNativeQuery(
            "SELECT employee_code FROM powersales.\"user\" " +
                "WHERE sfid IS NOT NULL AND (password IS NULL OR password = '') " +
                "AND employee_code IS NOT NULL"
        )
        @Suppress("UNCHECKED_CAST")
        val codes = codesQuery.resultList as List<String>

        var totalUpdated = 0
        for (code in codes) {
            val hash = passwordEncoder.encode(MIGRATION_INITIAL_PASSWORD)
            val updated = em.createNativeQuery(
                "UPDATE powersales.\"user\" SET password = :hash, password_change_required = TRUE " +
                    "WHERE employee_code = :code AND (password IS NULL OR password = '')"
            )
                .setParameter("hash", hash)
                .setParameter("code", code)
                .executeUpdate()
            totalUpdated += updated
        }

        return SfMigrationStage2Response(
            substep = "password",
            results = listOf(SubstepResult(label = "User.password (BCrypt)", rowsAffected = totalUpdated)),
            totalRowsAffected = totalUpdated,
        )
    }

    /**
     * Stage 2 (user.profile_id reconcile) — SF User.ProfileId(=user.profile_sfid) 를 profile_id 의 **최종 권위**로 강제 정합.
     *
     * ## 배경 (정합 사고)
     * 일반 FK Resolve([SfMigrationStage2FkService])는 `profile_id = COALESCE(t.profile_id, ...)`
     * + `WHERE (t.profile_id IS NULL ...)` 가드라 **profile_id 가 이미 채워져 있으면 덮어쓰지 않는다**.
     * 그런데 SAP 인바운드 provisioning([com.otoki.powersales.user.service.UserProvisioningService.profileIdFor])이
     * `employee.role`(SF AppAuthority picklist) 기반 fallback(`else -> "5.영업사원"`)으로 user 를 먼저 INSERT 하면,
     * FK Resolve 실행 시점에 profile_id 가 이미 `5.영업사원`/`9. Staff` 로 선점되어 SF 실제 Profile(예: `6.조장`)
     * 로 갱신되지 못한다. 즉 provisioning 과 마이그레이션이 같은 profile_id 컬럼을 두고 경쟁하고, `IS NULL`
     * 가드 탓에 "먼저 채운 쪽이 이기는" 구조가 되어 SF 권위가 뒤집힌다.
     *
     * ## 정합 원칙
     * SF 마이그레이션이 있는 사원(`profile_sfid` 보유)은 **SF User.Profile 이 최종 권위**다. 본 substep 은
     * `profile_sfid → profile.sfid` 조인으로 SF 정답 profile_id 를 산출해 `COALESCE` 없이 **무조건 override** 한다.
     * FK Resolve 를 재실행해도 provisioning 선점이 재발할 수 있으므로, cut-over 시점 fk substep **직후 1회**
     * 실행하여 profile_id 를 SF 정답으로 수렴시킨다. 멱등 (이미 일치하는 row 는 IS DISTINCT FROM 조건으로 skip).
     *
     * ## 시스템 관리자 격상 보존 + 지정 사번 강제 격상
     * 운영에서 SF Profile(`9. Staff` 등)보다 높게 격상된 `시스템 관리자` 계정 은 SF 정답(9.Staff)으로
     * 되돌리면 관리자 권한을 박탈하게 된다. 따라서 두 가지로 방어한다:
     * 1. **현재 profile 이 '시스템 관리자'면 override 대상에서 제외** (이미 격상된 계정 보존 — 예:
     *    [com.otoki.powersales.platform.common.config.ProdAdminBootstrapInitializer] 부트스트랩 계정).
     * 2. **[SystemAdminGrantList] 사번은 override 대상에서 제외 + 별도로 '시스템 관리자' 로 강제 upsert**.
     *    이 사번들은 SF 상 `9. Staff` (비관리자) 라 override 하면 관리자에서 탈락하고, DB reset 직후에는
     *    현재 profile 이 아직 관리자가 아니라 1번 가드도 안 걸린다. 따라서 "현재 상태 보존" 이 아니라
     *    "지정 사번 강제 격상" 이 필요하며, 그 지정 출처가 [SystemAdminGrantList] SoT 다. reset 후에도
     *    본 substep 이 멱등 재현한다.
     */
    @Transactional
    fun runUserProfileSfidReconcile(): SfMigrationStage2Response {
        val grantCodes = SystemAdminGrantList.EMPLOYEE_CODES

        // ① SF override — profile_sfid → SF 정답 profile_id 로 무조건 정합.
        //    단 (a) 이미 시스템 관리자로 격상된 계정, (b) SystemAdminGrantList 지정 사번 은 제외.
        val overrideRows = em.createNativeQuery(
            """
            UPDATE powersales."user" u
            SET profile_id = p_sf.profile_id
            FROM powersales.profile p_sf
            WHERE p_sf.sfid = u.profile_sfid
              AND u.profile_sfid IS NOT NULL
              AND u.profile_id IS DISTINCT FROM p_sf.profile_id
              AND u.employee_code NOT IN (:grantCodes)
              AND NOT EXISTS (
                SELECT 1 FROM powersales.profile p_now
                WHERE p_now.profile_id = u.profile_id
                  AND p_now.name = :sysAdminName
              )
            """.trimIndent()
        )
            .setParameter("grantCodes", grantCodes)
            .setParameter("sysAdminName", SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
            .executeUpdate()

        // ② 지정 사번 강제 격상 — SystemAdminGrantList 사번의 profile_id 를 '시스템 관리자' 로 upsert.
        //    이미 시스템 관리자면 IS DISTINCT FROM 으로 skip (멱등).
        val grantRows = em.createNativeQuery(
            """
            UPDATE powersales."user" u
            SET profile_id = p_admin.profile_id
            FROM powersales.profile p_admin
            WHERE p_admin.name = :sysAdminName
              AND u.employee_code IN (:grantCodes)
              AND u.profile_id IS DISTINCT FROM p_admin.profile_id
            """.trimIndent()
        )
            .setParameter("sysAdminName", SystemAdminProfilePolicy.SYSTEM_ADMIN_PROFILE_NAME)
            .setParameter("grantCodes", grantCodes)
            .executeUpdate()

        return SfMigrationStage2Response(
            substep = "userProfileSfidReconcile",
            results = listOf(
                SubstepResult(
                    label = "User.profile_id (SF profile_sfid override — 관리자/지정 사번 제외)",
                    rowsAffected = overrideRows,
                ),
                SubstepResult(
                    label = "User.profile_id (시스템 관리자 지정 사번 강제 격상 — SystemAdminGrantList)",
                    rowsAffected = grantRows,
                ),
            ),
            totalRowsAffected = overrideRows + grantRows,
        )
    }

    /**
     * 조장 Profile 의 ProfileFlags 초기 권한 적용 — [LeaderProfileFlagsSeed] SoT 기준.
     *
     * ## 왜 부팅 Runner 가 아니라 Stage 2 substep 인가
     * 과거 `LeaderProfileFlagsSyncRunner` 가 부팅 시 동일 sync 를 수행했으나, Stage 1 적재 **이전**에
     * 실행되어 `findByProfileId` 가 SF row 를 못 찾고 (profile_name=NULL, profile_id=존재) row 를
     * 별도 create → Stage 1 의 (profile_name=존재, profile_id=NULL) row 와 공존 →
     * Stage 2 FK Resolve 가 profile_id 를 채우는 순간 `profile_flags_profile_id_key` UNIQUE 위반이
     * 발생했다 (운영 관측). 그래서 Runner 는 비활성화(@Component 미부착)되어 있다.
     *
     * 본 substep 은 그 sync 를 **사용자가 순서를 통제하는 Stage 2 시점**으로 옮긴 것이다. 호출 시점에는
     * 이미 Stage 1 적재 + `fk-natural-key` 가 profile_flags.profile_id 를 채워둔 상태라 create 분기가
     * 불필요하며, 실제로 **create 를 하지 않는다** (아래 skip 정책).
     *
     * ## 적용 규칙
     * - **row 부재 시 create 하지 않고 skip** — UNIQUE 충돌 재발 방지. row 는 Stage 1 SF 적재분이
     *   유일 출처이며, 부재는 "Stage 1/fk-natural-key 미완료" 를 뜻하므로 조용히 만들지 않고 보고한다.
     * - **`is_locally_modified = TRUE` (web admin 편집분) 은 skip** — 운영 편집 자율성 보존
     *   (SF 재적재 dirty-skip 정책과 동일).
     * - 그 외에는 SoT 값으로 update + `is_locally_modified = FALSE` 유지.
     *
     * ## 적용 대상
     * [LeaderProfileFlagsSeed.SEEDS] 중 **`6.조장` 단건**. `7.영업사원 + 조장` 은 web admin 수동 편집
     * 대상으로 남긴다 (사용자 결정). 대상 확대가 필요하면 [LEADER_FLAGS_TARGET_PROFILE_NAMES] 에 추가.
     *
     * ## 실행 순서
     * `fk` → `fk-natural-key` **이후**에 호출해야 한다. 그 전에는 profile_flags.profile_id 가 NULL 이라
     * profile 조인이 전건 미해소되어 skip 만 보고된다.
     *
     * 멱등 — 동일 값 재적용은 rowsAffected 0.
     */
    @Transactional
    fun runLeaderProfileFlags(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        for (seed in LeaderProfileFlagsSeed.SEEDS) {
            if (seed.profileName !in LEADER_FLAGS_TARGET_PROFILE_NAMES) continue

            // profile.name → profile_flags row 조회. Stage 1 적재 + fk-natural-key 로 profile_id 가
            // 채워져 있어야 매칭된다 (create 분기 없음 — 부재 시 skip).
            val existing = em.createNativeQuery(
                """
                SELECT pf.is_locally_modified
                FROM powersales.profile_flags pf
                JOIN powersales.profile p ON p.profile_id = pf.profile_id
                WHERE p.name = :profileName
                """.trimIndent()
            )
                .setParameter("profileName", seed.profileName)
                .resultList
                .firstOrNull() as? Boolean

            if (existing == null) {
                results += SubstepResult(
                    label = "profile_flags['${seed.profileName}'] skip — row 부재 " +
                        "(Stage 1 적재 / fk-natural-key 선행 필요)",
                    rowsAffected = 0,
                )
                continue
            }
            if (existing) {
                results += SubstepResult(
                    label = "profile_flags['${seed.profileName}'] skip — web admin 편집분 보존 " +
                        "(is_locally_modified=TRUE)",
                    rowsAffected = 0,
                )
                continue
            }

            val updated = em.createNativeQuery(
                """
                UPDATE powersales.profile_flags pf
                SET permissions_view_all_data = :viewAllData,
                    permissions_modify_all_data = :modifyAllData,
                    permissions_view_all_users = :viewAllUsers,
                    permissions_manage_users = :manageUsers,
                    permissions_api_enabled = :apiEnabled,
                    object_permissions = CAST(:objectPermissions AS jsonb),
                    custom_permissions = CAST(:customPermissions AS jsonb)
                FROM powersales.profile p
                WHERE p.profile_id = pf.profile_id
                  AND p.name = :profileName
                  AND pf.is_locally_modified = FALSE
                """.trimIndent()
            )
                .setParameter("viewAllData", seed.viewAllData)
                .setParameter("modifyAllData", seed.modifyAllData)
                .setParameter("viewAllUsers", seed.viewAllUsers)
                .setParameter("manageUsers", seed.manageUsers)
                .setParameter("apiEnabled", seed.apiEnabled)
                .setParameter("objectPermissions", seed.objectPermissionsJson)
                .setParameter("customPermissions", seed.customPermissionsJson)
                .setParameter("profileName", seed.profileName)
                .executeUpdate()

            results += SubstepResult(
                label = "profile_flags['${seed.profileName}'] SoT 적용 (LeaderProfileFlagsSeed)",
                rowsAffected = updated,
            )
        }

        return SfMigrationStage2Response(
            substep = "leaderProfileFlags",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }
}
