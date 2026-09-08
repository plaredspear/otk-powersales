package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.service.ClaimDeliveryStatusRule
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDateTime

/**
 * 전송상태 파생 승격 대상 조회 쿼리 검증 — [ClaimDeliveryStatusRule] 의 SQL 표현이 규칙과 일치하는지 확인한다.
 *
 * JPQL 파싱/파라미터 바인딩(ClaimStatus converter, `trim(coalesce(...))`) 은 컨텍스트 기동 시점에만
 * 드러나므로 mock 테스트로는 덮이지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class ClaimDeliveryStatusPromotionQueryTest {

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    private val afterCutoff: LocalDateTime = ClaimDeliveryStatusRule.PROMOTION_START_AT.plusDays(1)
    private val beforeCutoff: LocalDateTime = ClaimDeliveryStatusRule.PROMOTION_START_AT.minusSeconds(1)

    private fun persist(
        sfid: String? = null,
        status: ClaimStatus? = ClaimStatus.DRAFT,
        createdAt: LocalDateTime = afterCutoff,
        counselNumber: String? = null,
        interfaceDate: LocalDateTime? = null,
    ): Claim {
        val claim = Claim(
            sfid = sfid,
            status = status,
            counselNumber = counselNumber,
            interfaceDate = interfaceDate,
        ).apply { this.createdAt = createdAt }
        return testEntityManager.persistAndFlush(claim)
    }

    private fun countTargets() = claimRepository.countDeliveryStatusPromotionTargets(
        ClaimDeliveryStatusRule.PROMOTION_START_AT,
        ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
    )

    private fun findTargets() = claimRepository.findDeliveryStatusPromotionTargets(
        ClaimDeliveryStatusRule.PROMOTION_START_AT,
        ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
        PageRequest.of(0, 100),
    )

    @Test
    @DisplayName("증거 보유 + 커트라인 이후 + 이관분 아님 인 건만 대상으로 조회된다")
    fun findsOnlyPromotableClaims() {
        val byCounsel = persist(counselNumber = "CS-100")
        val byInterfaceDate = persist(interfaceDate = LocalDateTime.of(2026, 8, 20, 10, 0))
        val sendFailed = persist(status = ClaimStatus.SEND_FAILED, counselNumber = "CS-101")

        persist()                                                        // 증거 없음
        persist(counselNumber = "   ")                                   // blank 는 증거 아님
        persist(counselNumber = "CS-200", createdAt = beforeCutoff)      // 커트라인 이전 등록
        persist(counselNumber = "CS-300", sfid = "a0X0000000000001")     // SF 이관분
        persist(counselNumber = "CS-400", status = ClaimStatus.SENT)     // 이미 전송완료
        testEntityManager.clear()

        assertThat(countTargets()).isEqualTo(3)
        assertThat(findTargets().map { it.id })
            .containsExactly(byCounsel.id, byInterfaceDate.id, sendFailed.id)
    }

    @Test
    @DisplayName("status 가 NULL 인 행도 대상에 포함된다")
    fun includesNullStatus() {
        val nullStatus = persist(status = null, counselNumber = "CS-500")
        testEntityManager.clear()

        assertThat(countTargets()).isEqualTo(1)
        assertThat(findTargets().map { it.id }).containsExactly(nullStatus.id)
    }

    @Test
    @DisplayName("pageable 로 처리 상한을 건다 — 건수는 전체, 조회는 상한까지")
    fun appliesPageableLimit() {
        repeat(3) { persist(counselNumber = "CS-60$it") }
        testEntityManager.clear()

        val limited = claimRepository.findDeliveryStatusPromotionTargets(
            ClaimDeliveryStatusRule.PROMOTION_START_AT,
            ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
            PageRequest.of(0, 2),
        )

        assertThat(countTargets()).isEqualTo(3)
        assertThat(limited).hasSize(2)
    }
}
