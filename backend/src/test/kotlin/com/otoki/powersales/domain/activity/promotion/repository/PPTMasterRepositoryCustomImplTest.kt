package com.otoki.powersales.domain.activity.promotion.repository

import com.otoki.powersales.domain.activity.promotion.entity.ProfessionalPromotionTeamMaster
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
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("PPTMasterRepositoryCustomImpl - findValidMasters 유효 조건 (legacy ValidData__c='유효' 정합)")
class PPTMasterRepositoryCustomImplTest {

    @Autowired private lateinit var repository: PPTMasterRepository
    @Autowired private lateinit var em: TestEntityManager

    private val today: LocalDate = LocalDate.of(2026, 6, 15)

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        em.clear()
    }

    // employeeId 는 EMPLOYEE FK 제약이 있어 본 테스트(유효 조건만 검증)에선 null 로 둔다.
    private fun persist(
        startDate: LocalDate,
        endDate: LocalDate?,
        isConfirmed: Boolean,
    ): ProfessionalPromotionTeamMaster {
        val master = ProfessionalPromotionTeamMaster(
            employeeId = null,
            teamType = ProfessionalPromotionTeamType.RAMEN_SALE,
            startDate = startDate,
            endDate = endDate,
            isConfirmed = isConfirmed,
        )
        em.persistAndFlush(master)
        return master
    }

    @Test
    @DisplayName("미확정(isConfirmed=false) 마스터는 날짜가 유효해도 제외 — legacy ValidData__c formula Confirmed==false→'미확정' 동등")
    fun findValidMasters_excludesUnconfirmed() {
        // 날짜는 유효 범위(시작 ≤ today ≤ 종료) 인데 미확정인 마스터
        val unconfirmed = persist(
            startDate = today.minusDays(1),
            endDate = today.plusDays(10),
            isConfirmed = false,
        )
        // 동일 날짜 + 확정 마스터
        val confirmed = persist(
            startDate = today.minusDays(1),
            endDate = today.plusDays(10),
            isConfirmed = true,
        )
        em.clear()

        val result = repository.findValidMasters(today)

        val ids = result.map { it.id }
        assertThat(ids).contains(confirmed.id)
        assertThat(ids).doesNotContain(unconfirmed.id)
    }

    @Test
    @DisplayName("확정 + 날짜 유효 — 종료일 NULL / 종료일 미래 / 종료일=today 모두 포함")
    fun findValidMasters_includesConfirmedWithinDateRange() {
        val endNull = persist(today.minusDays(5), null, isConfirmed = true)
        val endFuture = persist(today.minusDays(5), today.plusDays(5), isConfirmed = true)
        val endToday = persist(today.minusDays(5), today, isConfirmed = true)
        em.clear()

        val result = repository.findValidMasters(today)

        assertThat(result.map { it.id })
            .containsExactlyInAnyOrder(endNull.id, endFuture.id, endToday.id)
    }

    @Test
    @DisplayName("날짜 범위 밖(시작 미래 / 종료 과거)은 확정이어도 제외")
    fun findValidMasters_excludesOutOfDateRange() {
        val notStarted = persist(today.plusDays(1), null, isConfirmed = true)
        val alreadyEnded = persist(today.minusDays(10), today.minusDays(1), isConfirmed = true)
        em.clear()

        val result = repository.findValidMasters(today)

        assertThat(result.map { it.id })
            .doesNotContain(notStarted.id, alreadyEnded.id)
    }

    @Test
    @DisplayName("findExpiringMasters - 종료일=today 라도 미확정(isConfirmed=false)은 제외, 확정만 포함 (legacy Batch_PPTMaster2 정합)")
    fun findExpiringMasters_excludesUnconfirmed() {
        // 종료일이 today 인 미확정 마스터 — 레거시 ValidData__c='유효' 전제로 만료 대상에서 제외
        val unconfirmed = persist(today.minusDays(3), today, isConfirmed = false)
        // 종료일이 today 인 확정 마스터 — 만료 대상
        val confirmed = persist(today.minusDays(3), today, isConfirmed = true)
        // 종료일이 today 가 아닌 확정 마스터 — 만료 대상 아님
        val notExpiringToday = persist(today.minusDays(3), today.plusDays(1), isConfirmed = true)
        em.clear()

        val result = repository.findExpiringMasters(today)

        val ids = result.map { it.id }
        assertThat(ids).contains(confirmed.id)
        assertThat(ids).doesNotContain(unconfirmed.id, notExpiringToday.id)
    }
}
