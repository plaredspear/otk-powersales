package com.otoki.powersales.sfmigration.service

import com.otoki.powersales.common.storage.UPLOAD_FILE_POLYMORPHIC_PARENTS
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
