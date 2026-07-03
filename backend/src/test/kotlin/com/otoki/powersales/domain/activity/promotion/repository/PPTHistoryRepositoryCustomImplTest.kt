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
        em.clear()
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
