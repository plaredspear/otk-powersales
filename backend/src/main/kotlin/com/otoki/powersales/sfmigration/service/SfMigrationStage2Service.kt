package com.otoki.powersales.sfmigration.service

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
 * - 2-B picklist : 한글 picklist → enum 변환 (Employee.role / PPT / User.profile_type)
 * - 2-C password : BCrypt password hash (sfid IS NOT NULL AND password NULL 인 user)
 * - 2-D permission : sf_permission_set_assignment_raw → user_permission 매핑
 *
 * 2-A FK resolve 는 별도 클래스 (SfMigrationStage2FkService) 로 분리.
 */
@Service
class SfMigrationStage2Service(
    @PersistenceContext private val em: EntityManager,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * Stage 2-B — 한글 picklist 값을 enum 값으로 일괄 UPDATE.
     *
     * 3개 컬럼 (Employee.role / User.profile_type / User.cost_center_code derived 캐시) 을
     * 순차 호출. admin UI 의 "일괄 실행" 진입점. 매칭 실패 row 는 fallback 또는 NULL 처리.
     *
     * Employee.professional_promotion_team 은 SF picklist 한글 값과 backend Converter 인식
     * 형식 (displayName 한글) 이 identity 라 변환 substep 불필요 — Stage 1 raw 적재로 충분.
     */
    @Transactional
    fun runPicklistMapping(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        results += runPicklistEmployeeRole().results
        results += runPicklistUserProfileType().results
        results += runUserCostCenterCodeSync().results

        return SfMigrationStage2Response(
            substep = "picklist",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

    /** Stage 2-B (employee.role) — 한글 AppAuthority → UserRole enum.name */
    @Transactional
    fun runPicklistEmployeeRole(): SfMigrationStage2Response {
        val rows = applyMapping(
            tableName = "employee",
            columnName = "role",
            mapping = APP_AUTHORITY_TO_USER_ROLE,
            fallbackValue = USER_ROLE_FALLBACK,
        )
        return singleResultResponse(
            substep = "picklist.employee_role",
            label = "Employee.role",
            rows = rows,
        )
    }

    /** Stage 2-B (user.profile_type) — 한글 Profile.Name → ProfileType enum.name */
    @Transactional
    fun runPicklistUserProfileType(): SfMigrationStage2Response {
        val rows = applyMapping(
            tableName = "user",
            columnName = "profile_type",
            mapping = PROFILE_NAME_TO_PROFILE_TYPE,
            fallbackValue = PROFILE_TYPE_FALLBACK,
        )
        return singleResultResponse(
            substep = "picklist.user_profile_type",
            label = "User.profile_type",
            rows = rows,
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

    /**
     * Stage 2-D — Permission mapping.
     *
     * `sf_permission_set_assignment_raw` staging 테이블 → `user_permission` 적용.
     * PERMISSION_SET_TO_PERMISSIONS 매핑 표에 정의된 PermissionSet 만 처리하고,
     * 그 외 (INTENTIONALLY_SKIPPED_PERMISSION_SETS 포함 unmapped) 는 묵시적 skip.
     */
    @Transactional
    fun runPermissionMapping(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()
        var totalInserted = 0

        for ((permSetName, adminPermissions) in PERMISSION_SET_TO_PERMISSIONS) {
            for (adminPermission in adminPermissions) {
                val sql = """
                    INSERT INTO powersales.user_permission (user_id, permission, created_at)
                    SELECT DISTINCT u.user_id, :perm, NOW()
                    FROM powersales.sf_permission_set_assignment_raw psa
                    JOIN powersales."user" u ON u.employee_code = psa.assignee_employee_code
                    WHERE psa.permission_set_name = :setName
                    ON CONFLICT DO NOTHING
                """.trimIndent()
                val n = em.createNativeQuery(sql)
                    .setParameter("perm", adminPermission)
                    .setParameter("setName", permSetName)
                    .executeUpdate()
                totalInserted += n
                results += SubstepResult(
                    label = "$permSetName -> $adminPermission",
                    rowsAffected = n,
                )
            }
        }

        return SfMigrationStage2Response(
            substep = "permission",
            results = results,
            totalRowsAffected = totalInserted,
        )
    }

    /**
     * 한 테이블의 한 컬럼에 매핑 표 (raw → enum) 적용. fallback 이 있으면 매칭 실패 row 는 fallback 값으로,
     * 없으면 NULL 로 비운다.
     *
     * 테이블/컬럼명은 SQL identifier 바인딩 불가라 직접 보간 — 호출자는 화이트리스트로 제한된 값만 전달해야 한다.
     * 본 서비스는 매핑 표마다 호출 위치를 고정하므로 SQL injection 위험 없음.
     */
    private fun applyMapping(
        tableName: String,
        columnName: String,
        mapping: Map<String, String>,
        fallbackValue: String?,
    ): Int {
        val quotedTable = if (tableName == "user") "\"user\"" else tableName
        var totalUpdated = 0

        for ((rawValue, enumValue) in mapping) {
            val sql = "UPDATE powersales.$quotedTable SET $columnName = :enumValue WHERE $columnName = :rawValue"
            totalUpdated += em.createNativeQuery(sql)
                .setParameter("enumValue", enumValue)
                .setParameter("rawValue", rawValue)
                .executeUpdate()
        }

        val enumList = mapping.values.toSet()
        if (enumList.isEmpty()) return totalUpdated

        val placeholders = enumList.mapIndexed { i, _ -> ":v$i" }.joinToString(", ")
        val fallbackSql = if (fallbackValue != null) {
            "UPDATE powersales.$quotedTable SET $columnName = :fallback " +
                "WHERE $columnName IS NOT NULL AND $columnName NOT IN ($placeholders)"
        } else {
            "UPDATE powersales.$quotedTable SET $columnName = NULL " +
                "WHERE $columnName IS NOT NULL AND $columnName NOT IN ($placeholders)"
        }
        val query = em.createNativeQuery(fallbackSql)
        if (fallbackValue != null) query.setParameter("fallback", fallbackValue)
        enumList.forEachIndexed { i, v -> query.setParameter("v$i", v) }
        totalUpdated += query.executeUpdate()

        return totalUpdated
    }
}
