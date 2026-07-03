package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamHistory
import com.otoki.powersales.domain.activity.promotion.enums.ProfessionalPromotionTeamType
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("PPTHistoryRepositoryCustomImpl - searchHistories teamType 필터 (변경 후 기준)")
class PPTHistoryRepositoryCustomImplTest {

    @Autowired private lateinit var repository: PPTHistoryRepository
    @Autowired private lateinit var em: TestEntityManager

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        // Hibernate ddl-auto 가 new_value/old_value 에 enum 5값 CHECK 제약을 자동 생성한다.
        // 운영 PostgreSQL 스키마엔 이 CHECK 가 없어 '일반' 등 raw 문자열이 실재하므로,
        // 마이그레이션 시나리오(raw '일반') 재현을 위해 테스트 스키마에서 CHECK 제약을 제거한다.
        dropCheckConstraints()
        em.clear()
    }

    // new_value/old_value 컬럼의 자동 생성 CHECK 제약을 제거 (제약명은 H2 가 자동 부여하므로 조회).
    // DDL 은 H2 에서 auto-commit 이라 트랜잭션 롤백으로 원복되지 않으나, @DataJpaTest 는 클래스마다
    // 별도 컨텍스트/스키마라 다른 테스트 클래스로 새어나가지 않는다 (본 클래스 전용 조정).
    private fun dropCheckConstraints() {
        @Suppress("UNCHECKED_CAST")
        val names = em.entityManager.createNativeQuery(
            "SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS " +
                "WHERE TABLE_NAME = 'PROFESSIONAL_PROMOTION_TEAM_HISTORY' AND CONSTRAINT_TYPE = 'CHECK'"
        ).resultList as List<String>
        names.forEach { name ->
            em.entityManager.createNativeQuery(
                "ALTER TABLE professional_promotion_team_history DROP CONSTRAINT IF EXISTS \"$name\""
            ).executeUpdate()
        }
    }

    // employeeId 는 EMPLOYEE FK 제약이 있어 본 테스트(teamType 필터만 검증)에선 null 로 둔다.
    private fun persist(
        oldValue: ProfessionalPromotionTeamType?,
        newValue: ProfessionalPromotionTeamType?,
    ): ProfessionalPromotionTeamHistory {
        val history = ProfessionalPromotionTeamHistory(
            oldValue = oldValue,
            newValue = newValue,
        )
        em.persistAndFlush(history)
        return history
    }

    // SF 레거시 마이그레이션분 재현 — new_value 컬럼에 raw 문자열('일반' 등, enum 아님)을
    // COPY 로 직접 적재한 상황. 엔티티 컨버터로는 표현 불가하므로 native INSERT 로 넣는다.
    private fun persistRawNewValue(rawNewValue: String?): Long {
        em.entityManager
            .createNativeQuery(
                "INSERT INTO professional_promotion_team_history (new_value, created_at, updated_at) " +
                    "VALUES (:v, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)"
            )
            .setParameter("v", rawNewValue)
            .executeUpdate()
        return (em.entityManager
            .createNativeQuery(
                "SELECT MAX(professional_promotion_team_history_id) FROM professional_promotion_team_history"
            )
            .singleResult as Number).toLong()
    }

    private fun search(
        teamType: ProfessionalPromotionTeamType? = null,
        teamTypeGeneral: Boolean = false,
    ): List<Long> = repository.searchHistories(
        null, null, teamType, teamTypeGeneral, null, null, null, PageRequest.of(0, 20)
    ).content.map { it.history.id }

    @Test
    @DisplayName("teamTypeGeneral=true → 변경 후(newValue) 가 null(일반, 미지정 해제) 인 이력만 조회")
    fun searchHistories_generalFilter_returnsOnlyNullNewValue() {
        val toGeneral = persist(oldValue = ProfessionalPromotionTeamType.RAMEN_SALE, newValue = null)
        val toRamen = persist(oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE)
        val toDumpling = persist(
            oldValue = ProfessionalPromotionTeamType.RAMEN_SALE,
            newValue = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
        )
        em.clear()

        val ids = search(teamTypeGeneral = true)

        assertThat(ids).containsExactly(toGeneral.id)
        assertThat(ids).doesNotContain(toRamen.id, toDumpling.id)
    }

    @Test
    @DisplayName("teamTypeGeneral=true → SF 마이그레이션분(new_value='일반' 문자열) 과 신규분(null) 을 모두 조회")
    fun searchHistories_generalFilter_matchesMigratedGeneralStringAndNull() {
        val toNull = persist(oldValue = ProfessionalPromotionTeamType.RAMEN_SALE, newValue = null)
        val migratedGeneral = persistRawNewValue("일반") // 레거시 해제 이력 (문자열 '일반')
        persist(oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE) // 잡히면 안 됨
        val migratedOther = persistRawNewValue("해당없음") // enum 도 '일반' 도 아닌 raw 값 — 잡히면 안 됨
        em.clear()

        val ids = search(teamTypeGeneral = true)

        assertThat(ids).containsExactlyInAnyOrder(toNull.id, migratedGeneral)
        assertThat(ids).doesNotContain(migratedOther)
    }

    @Test
    @DisplayName("teamTypeGeneral=true 면 teamType 인자는 무시 — 여전히 newValue null 만 조회")
    fun searchHistories_generalFilter_ignoresTeamType() {
        val toGeneral = persist(oldValue = ProfessionalPromotionTeamType.RAMEN_SALE, newValue = null)
        persist(oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE)
        em.clear()

        val ids = search(teamType = ProfessionalPromotionTeamType.RAMEN_SALE, teamTypeGeneral = true)

        assertThat(ids).containsExactly(toGeneral.id)
    }

    @Test
    @DisplayName("teamType 지정(일반 아님) → 변경 후 가 해당 조인 이력만 조회 (기존 동작 유지)")
    fun searchHistories_teamTypeFilter_returnsMatchingNewValue() {
        persist(oldValue = ProfessionalPromotionTeamType.RAMEN_SALE, newValue = null)
        val toRamen = persist(oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE)
        persist(
            oldValue = ProfessionalPromotionTeamType.RAMEN_SALE,
            newValue = ProfessionalPromotionTeamType.FRESH_SALE_DUMPLING,
        )
        em.clear()

        val ids = search(teamType = ProfessionalPromotionTeamType.RAMEN_SALE)

        assertThat(ids).containsExactly(toRamen.id)
    }

    @Test
    @DisplayName("필터 없음(teamType null + general false) → 전체 이력 조회")
    fun searchHistories_noFilter_returnsAll() {
        persist(oldValue = ProfessionalPromotionTeamType.RAMEN_SALE, newValue = null)
        persist(oldValue = null, newValue = ProfessionalPromotionTeamType.RAMEN_SALE)
        em.clear()

        val ids = search()

        assertThat(ids).hasSize(2)
    }
}
