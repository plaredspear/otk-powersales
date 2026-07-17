package com.otoki.powersales._migration.heroku.service

import com.otoki.powersales._migration.sf.dto.SfMigrationStage2Response
import com.otoki.powersales._migration.sf.dto.SubstepResult
import com.otoki.powersales._migration.sf.service.SfMigrationStage2Service
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Heroku 데이터 마이그레이션 Stage 2 — Logical 변환 (1회성 cut-over, 런칭 후 폐기).
 *
 * SF [SfMigrationStage2Service] 의 logical 변환 substep 과 동형. FK resolve 는 별도 클래스
 * ([HerokuFkResolveService]) 로 분리돼 있으므로 본 클래스는 값 변환 substep 만 담는다.
 *
 * 구현 substep:
 * - password : EmployeeInfo(mobile) 초기 비밀번호를 BCrypt hash 로 채움. Heroku Stage 1 은
 *   레거시 평문(emp_pwd)을 적재하지 않고 password NULL 로 두므로 (HerokuStage1Targets.EXCLUDED_COLUMNS),
 *   본 substep 이 SF User.password 재해시와 동일한 초기 비밀번호 정책을 적용한다.
 */
@Service
class HerokuMigrationStage2Service(
    @PersistenceContext private val em: EntityManager,
    private val passwordEncoder: PasswordEncoder,
) {

    /**
     * password — 마이그레이션 대상 EmployeeInfo 의 초기 비밀번호를
     * [SfMigrationStage2Service.MIGRATION_INITIAL_PASSWORD] 고정 상수의 BCrypt hash 로 채운다.
     *
     * 대상: `password IS NULL OR password = ''` 인 employee_info row (Heroku Stage 1 적재분).
     * 멱등 (이미 채워진 row skip). `password_change_required = TRUE` 로 최초 로그인 시 강제 변경 유도.
     *
     * SF User.password 와 동일 상수를 공유하므로 web / mobile 초기 비밀번호가 일치한다. BCrypt salt 는
     * 매 encode 마다 랜덤이라 사용자별 hash 는 다르지만 평문은 모두 동일하다 → row 별로 encode 한다.
     */
    @Transactional
    fun runPasswordHash(): SfMigrationStage2Response {
        val idsQuery = em.createNativeQuery(
            "SELECT employee_id FROM powersales.employee_info " +
                "WHERE password IS NULL OR password = ''"
        )
        @Suppress("UNCHECKED_CAST")
        val ids = (idsQuery.resultList as List<Number>).map { it.toLong() }

        var totalUpdated = 0
        for (id in ids) {
            val hash = passwordEncoder.encode(SfMigrationStage2Service.MIGRATION_INITIAL_PASSWORD)
            val updated = em.createNativeQuery(
                "UPDATE powersales.employee_info SET password = :hash, password_change_required = TRUE " +
                    "WHERE employee_id = :id AND (password IS NULL OR password = '')"
            )
                .setParameter("hash", hash)
                .setParameter("id", id)
                .executeUpdate()
            totalUpdated += updated
        }

        return SfMigrationStage2Response(
            substep = "password",
            results = listOf(SubstepResult(label = "EmployeeInfo.password (BCrypt)", rowsAffected = totalUpdated)),
            totalRowsAffected = totalUpdated,
        )
    }
}
