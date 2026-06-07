package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS
import com.otoki.powersales.notice.service.NoticeImagePlaceholder
import com.otoki.powersales.sfmigration.dto.SfMigrationStage2Response
import com.otoki.powersales.sfmigration.dto.SubstepResult
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
 * - 2-C password : BCrypt password hash (sfid IS NOT NULL AND password NULL 인 user)
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

    private fun singleResultResponse(substep: String, label: String, rows: Int): SfMigrationStage2Response =
        SfMigrationStage2Response(
            substep = substep,
            results = listOf(SubstepResult(label = label, rowsAffected = rows)),
            totalRowsAffected = rows,
        )

    /**
     * Stage 2-C — BCrypt password hash.
     *
     * 비밀번호가 비어 있는 (NULL 또는 빈 문자열) SF migrated user 의 password 를
     * `employee_code` 자체를 평문 비밀번호로 하여 BCrypt hash 로 채운다. cut-over 직후
     * 사용자에게 password_change_required=TRUE 로 강제 변경 유도.
     *
     * 운영 backend 의 PasswordEncoder (BCrypt strength 10) 빈을 재사용한다.
     */
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
     *   placeholder 만 영구 저장하고, 조회 시점에 [com.otoki.powersales.notice.service.NoticeService.getNoticeDetail]
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
            val hash = passwordEncoder.encode(code)
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
}
