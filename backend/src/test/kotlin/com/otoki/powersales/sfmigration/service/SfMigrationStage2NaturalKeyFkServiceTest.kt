package com.otoki.powersales.sfmigration.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * SfMigrationStage2NaturalKeyFkService 단위 테스트 (spec #800).
 *
 * Q1~Q5 옵션 1 정합 — SQL builder 명제 검증 + EntityManager 호출 횟수 검증.
 * 실제 DB 실행은 PG 통합 테스트 (별도 — H2 의 UPDATE FROM 미지원으로 단위 테스트는 SQL 명제만).
 */
@DisplayName("SfMigrationStage2NaturalKeyFkService — spec #800")
class SfMigrationStage2NaturalKeyFkServiceTest {

    private val em = mockk<EntityManager>()
    private val service = SfMigrationStage2NaturalKeyFkService(em)

    @Nested
    @DisplayName("buildUpdateSql — Q3 옵션 1 (UPDATE ... FROM ... WHERE ... IS NULL)")
    inner class BuildUpdateSql {

        @Test
        @DisplayName("sharing_rule_condition.sharing_rule_developer_name → sharing_rule.sharing_rule_id")
        fun sharingRuleCondition() {
            val spec = NaturalKeyFkSpec(
                sourceTable = "sharing_rule_condition",
                sourceColumn = "sharing_rule_developer_name",
                refTable = "sharing_rule",
                refColumn = "developer_name",
                targetIdColumn = "sharing_rule_id",
            )
            val sql = service.buildUpdateSql(spec)
            assertThat(sql).contains("UPDATE powersales.sharing_rule_condition s")
            assertThat(sql).contains("SET sharing_rule_id = r.sharing_rule_id")
            assertThat(sql).contains("FROM powersales.sharing_rule r")
            assertThat(sql).contains("WHERE r.developer_name = s.sharing_rule_developer_name")
            // Q5 — 멱등성 단언
            assertThat(sql).contains("AND s.sharing_rule_id IS NULL")
        }

        @Test
        @DisplayName("permission_set_assignment.permission_set_sfid → permission_set_flags.permission_set_flags_id (#798)")
        fun permissionSetAssignmentSfid() {
            val spec = NaturalKeyFkSpec(
                sourceTable = "permission_set_assignment",
                sourceColumn = "permission_set_sfid",
                refTable = "permission_set_flags",
                refColumn = "permission_set_sfid",
                targetIdColumn = "permission_set_flags_id",
            )
            val sql = service.buildUpdateSql(spec)
            assertThat(sql).contains("UPDATE powersales.permission_set_assignment s")
            assertThat(sql).contains("SET permission_set_flags_id = r.permission_set_flags_id")
            assertThat(sql).contains("FROM powersales.permission_set_flags r")
            assertThat(sql).contains("WHERE r.permission_set_sfid = s.permission_set_sfid")
            assertThat(sql).contains("AND s.permission_set_flags_id IS NULL")
        }

        @Test
        @DisplayName("PG reserved keyword `user` 쿼팅 — 본 service 의 ref 중 user 미사용이나 향후 안전성")
        fun userIdentifierQuoting() {
            val spec = NaturalKeyFkSpec(
                sourceTable = "user",
                sourceColumn = "username",
                refTable = "user",
                refColumn = "username",
                targetIdColumn = "user_id",
            )
            val sql = service.buildUpdateSql(spec)
            assertThat(sql).contains("UPDATE powersales.\"user\" s")
            assertThat(sql).contains("FROM powersales.\"user\" r")
        }
    }

    @Nested
    @DisplayName("runNaturalKeyFkResolve — 9 매핑 일괄 적용")
    inner class RunNaturalKeyFkResolve {

        @Test
        @DisplayName("NATURAL_KEY_FK_MAPPINGS 9 entry + sharing_rule_target 2 분기 + permission_set_flags.sfid 1건 모두에 대해 UPDATE 12회 + COUNT 20회")
        fun allMappingsExecuted() {
            val updateQuery = mockk<Query>()
            every { updateQuery.executeUpdate() } returns 10
            val countQuery = mockk<Query>()
            every { countQuery.singleResult } returns 0L  // 모든 매칭 성공

            // UPDATE / SELECT COUNT 구분: SQL 본문에 "UPDATE" 시작 시 update query 반환
            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("UPDATE") }) } returns updateQuery
            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("SELECT") }) } returns countQuery

            val response = service.runNaturalKeyFkResolve()

            assertThat(response.substep).isEqualTo("fk-natural-key")
            // 9 NaturalKey + 2 sharing_rule_target (ROLE*/GROUP) + 1 permission_set_flags.sfid = 12 SubstepResult
            assertThat(response.results).hasSize(NATURAL_KEY_FK_MAPPINGS.size + 3)
            assertThat(response.totalRowsAffected).isEqualTo((NATURAL_KEY_FK_MAPPINGS.size + 3) * 10)

            // UPDATE: 9 (NaturalKey) + 2 (sharing_rule_target ROLE*/GROUP) + 1 (permission_set_flags.sfid) = 12회
            verify(exactly = NATURAL_KEY_FK_MAPPINGS.size + 3) {
                em.createNativeQuery(match<String> { it.trimStart().startsWith("UPDATE") })
            }
            // SELECT COUNT: 9 NaturalKey × 2 (전/후) + 1 (sharing_rule_target unmatched) + 1 (permission_set_flags.sfid unmatched) = 20회
            verify(exactly = NATURAL_KEY_FK_MAPPINGS.size * 2 + 2) {
                em.createNativeQuery(match<String> { it.trimStart().startsWith("SELECT") })
            }
        }

        @Test
        @DisplayName("결과 SubstepResult.label 은 'sourceTable.sourceColumn → refTable.targetIdColumn' 패턴 + sharing_rule_target 전용 분기 label")
        fun substepResultLabelFormat() {
            val updateQuery = mockk<Query>()
            every { updateQuery.executeUpdate() } returns 0
            val countQuery = mockk<Query>()
            every { countQuery.singleResult } returns 0L

            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("UPDATE") }) } returns updateQuery
            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("SELECT") }) } returns countQuery

            val response = service.runNaturalKeyFkResolve()

            val labels = response.results.map { it.label }
            assertThat(labels).contains(
                "sharing_rule_condition.sharing_rule_developer_name → sharing_rule.sharing_rule_id",
                "profile_flags.profile_name → profile.profile_id",
                "permission_set_assignment.permission_set_sfid → permission_set_flags.permission_set_flags_id",
                "sharing_rule_target.target_developer_name (target_type=ROLE*) → user_role.user_role_id",
                "sharing_rule_target.target_developer_name (target_type=GROUP) → group.group_id",
            )
        }

        @Test
        @DisplayName("Q5 멱등성 — 두 번째 호출 시 모든 row 가 이미 채워져 있다면 totalRowsAffected = 0")
        fun idempotencyAfterAllRowsResolved() {
            val updateQuery = mockk<Query>()
            every { updateQuery.executeUpdate() } returns 0  // 멱등: 이미 채워진 row 는 WHERE NULL 조건으로 skip
            val countQuery = mockk<Query>()
            every { countQuery.singleResult } returns 0L

            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("UPDATE") }) } returns updateQuery
            every { em.createNativeQuery(match<String> { it.trimStart().startsWith("SELECT") }) } returns countQuery

            val response = service.runNaturalKeyFkResolve()

            assertThat(response.totalRowsAffected).isEqualTo(0)
            assertThat(response.results).allMatch { it.rowsAffected == 0 }
        }
    }
}
