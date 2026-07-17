package com.otoki.powersales._migration.heroku.service

import com.otoki.powersales._migration.sf.service.SfMigrationStage2Service
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.TestPropertySource
import org.springframework.transaction.annotation.Transactional

/**
 * Heroku Stage 2 Service 통합 테스트 — EmployeeInfo(mobile) password substep 검증.
 *
 * SF [com.otoki.powersales._migration.sf.service.SfMigrationStage2ServiceIntegrationTest] 와 동형
 * (`@DataJpaTest` 슬라이스 + H2 PostgreSQL 호환 모드 + native query). employee_info minimal 테이블을
 * BeforeEach 로 직접 생성한다 (Flyway 미사용).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@TestPropertySource(
    properties = [
        "spring.datasource.url=jdbc:h2:mem:herokumigration_stage2_test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.flyway.enabled=false",
    ],
)
@DisplayName("HerokuMigrationStage2Service 통합 테스트")
class HerokuMigrationStage2ServiceIntegrationTest {

    @PersistenceContext
    private lateinit var em: EntityManager

    private lateinit var service: HerokuMigrationStage2Service

    @BeforeEach
    @Transactional
    fun setUp() {
        service = HerokuMigrationStage2Service(em, BCryptPasswordEncoder())

        em.createNativeQuery("CREATE SCHEMA IF NOT EXISTS powersales").executeUpdate()
        em.createNativeQuery("DROP TABLE IF EXISTS powersales.employee_info").executeUpdate()
        em.createNativeQuery(
            """
            CREATE TABLE powersales.employee_info (
                employee_id BIGINT PRIMARY KEY,
                password VARCHAR(200),
                password_change_required BOOLEAN DEFAULT FALSE
            )
            """.trimIndent()
        ).executeUpdate()
    }

    @Test
    @Transactional
    @DisplayName("password — password NULL/'' 인 employee_info 를 고정 상수 BCrypt 로 채움 + 멱등")
    fun runPasswordHash() {
        // E1: password NULL → 대상
        em.createNativeQuery("INSERT INTO powersales.employee_info (employee_id, password) VALUES (1, NULL)").executeUpdate()
        // E2: password '' → 대상
        em.createNativeQuery("INSERT INTO powersales.employee_info (employee_id, password) VALUES (2, '')").executeUpdate()
        // E3: 이미 채워짐 → 미대상 (보존)
        em.createNativeQuery("INSERT INTO powersales.employee_info (employee_id, password) VALUES (3, 'ALREADY-HASHED')").executeUpdate()

        val response = service.runPasswordHash()

        assertThat(response.substep).isEqualTo("password")
        assertThat(response.totalRowsAffected).isEqualTo(2)

        val initial = SfMigrationStage2Service.MIGRATION_INITIAL_PASSWORD
        val pw1 = strOf("SELECT password FROM powersales.employee_info WHERE employee_id = 1")
        val pw2 = strOf("SELECT password FROM powersales.employee_info WHERE employee_id = 2")
        assertThat(pw1).startsWith("\$2a\$")
        assertThat(pw2).startsWith("\$2a\$")
        // 평문은 SF User.password 와 동일한 고정 공통 상수. salt 랜덤이라 hash 는 사용자별로 다름.
        assertThat(BCryptPasswordEncoder().matches(initial, pw1)).isTrue()
        assertThat(BCryptPasswordEncoder().matches(initial, pw2)).isTrue()
        assertThat(pw1).isNotEqualTo(pw2)

        // password_change_required 강제 변경 플래그 설정 확인.
        assertThat(boolOf("SELECT password_change_required FROM powersales.employee_info WHERE employee_id = 1")).isTrue()

        // 이미 채워진 row 는 보존.
        assertThat(strOf("SELECT password FROM powersales.employee_info WHERE employee_id = 3")).isEqualTo("ALREADY-HASHED")

        // 멱등 — 재실행 시 변경 0.
        val again = service.runPasswordHash()
        assertThat(again.totalRowsAffected).isEqualTo(0)
    }

    private fun strOf(sql: String): String? = objOf(sql)?.toString()
    private fun boolOf(sql: String): Boolean = objOf(sql) as Boolean
    private fun objOf(sql: String): Any? {
        val rows = em.createNativeQuery(sql).resultList
        return if (rows.isEmpty()) null else rows[0]
    }
}
