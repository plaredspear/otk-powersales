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
     * Employee.role / Employee.professional_promotion_team / User.profile_type 3개 컬럼을
     * 매핑 표 (한글 → enum) 로 변환한다. 매칭 실패 row 는 fallback 값 또는 NULL 로 처리.
     */
    @Transactional
    fun runPicklistMapping(): SfMigrationStage2Response {
        val results = mutableListOf<SubstepResult>()

        val roleRows = applyMapping(
            tableName = "employee",
            columnName = "role",
            mapping = APP_AUTHORITY_TO_USER_ROLE,
            fallbackValue = USER_ROLE_FALLBACK,
        )
        results += SubstepResult(label = "Employee.role", rowsAffected = roleRows)

        val pptRows = applyMapping(
            tableName = "employee",
            columnName = "professional_promotion_team",
            mapping = PPT_KOREAN_TO_ENUM,
            fallbackValue = null,
        )
        results += SubstepResult(label = "Employee.professional_promotion_team", rowsAffected = pptRows)

        val profileRows = applyMapping(
            tableName = "user",
            columnName = "profile_type",
            mapping = PROFILE_NAME_TO_PROFILE_TYPE,
            fallbackValue = PROFILE_TYPE_FALLBACK,
        )
        results += SubstepResult(label = "User.profile_type", rowsAffected = profileRows)

        return SfMigrationStage2Response(
            substep = "picklist",
            results = results,
            totalRowsAffected = results.sumOf { it.rowsAffected },
        )
    }

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
