package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.activity.claim.repository.ClaimRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

@DisplayName("ClaimDeliveryStatusBackfillService (전송상태 파생 승격 존량 백필) 테스트")
class ClaimDeliveryStatusBackfillServiceTest {

    private lateinit var claimRepository: ClaimRepository
    private lateinit var service: ClaimDeliveryStatusBackfillService

    private val afterCutoff: LocalDateTime = ClaimDeliveryStatusRule.PROMOTION_START_AT.plusDays(1)

    @BeforeEach
    fun setUp() {
        claimRepository = mockk()
        service = ClaimDeliveryStatusBackfillService(claimRepository)
    }

    private fun target(id: Long, counselNumber: String? = "C-$id", sfid: String? = null) =
        Claim(id = id, sfid = sfid, status = ClaimStatus.DRAFT, counselNumber = counselNumber)
            .apply { createdAt = afterCutoff }

    private fun stub(total: Long, targets: List<Claim>) {
        every { claimRepository.countDeliveryStatusPromotionTargets(any(), any()) } returns total
        every { claimRepository.findDeliveryStatusPromotionTargets(any(), any(), any()) } returns targets
    }

    @Test
    @DisplayName("대상 건을 전송완료로 올리고 scanned/promoted/remaining 을 집계한다")
    fun backfill_promotesTargets() {
        val targets = listOf(target(1L), target(2L))
        stub(total = 5, targets = targets)

        val result = service.backfill()

        assertThat(targets).allMatch { it.status == ClaimStatus.SENT }
        assertThat(result.scanned).isEqualTo(2)
        assertThat(result.promoted).isEqualTo(2)
        // 잔여는 "승격 전 총량 - 승격분" — 승격 후 재집계하면 auto-flush 로 이중 차감된다.
        assertThat(result.remaining).isEqualTo(3)
    }

    @Test
    @DisplayName("쿼리에 걸려도 규칙 게이트를 통과하지 못하면 승격하지 않는다 (규칙이 최종 판정)")
    fun backfill_ruleIsFinalGate() {
        // sfid 보유 = SF 이관분 → 규칙이 거른다.
        val migrated = target(1L, sfid = "a0X0000000000001")
        stub(total = 1, targets = listOf(migrated))

        val result = service.backfill()

        assertThat(migrated.status).isEqualTo(ClaimStatus.DRAFT)
        assertThat(result.scanned).isEqualTo(1)
        assertThat(result.promoted).isZero()
        assertThat(result.remaining).isEqualTo(1)
    }

    @Test
    @DisplayName("limit 은 1..MAX_LIMIT 로 clamp 되어 pageSize 에 반영된다")
    fun backfill_clampsLimit() {
        stub(total = 0, targets = emptyList())
        val pageable = slot<Pageable>()

        service.backfill(limit = 99_999)

        verify { claimRepository.findDeliveryStatusPromotionTargets(any(), any(), capture(pageable)) }
        assertThat(pageable.captured.pageSize).isEqualTo(ClaimDeliveryStatusBackfillService.MAX_LIMIT)
    }

    @Test
    @DisplayName("커트라인/상태 파라미터를 규칙 상수 그대로 전달한다")
    fun backfill_passesRuleParameters() {
        stub(total = 0, targets = emptyList())

        service.countTargets()

        verify {
            claimRepository.countDeliveryStatusPromotionTargets(
                ClaimDeliveryStatusRule.PROMOTION_START_AT,
                ClaimDeliveryStatusRule.PROMOTABLE_STATUSES,
            )
        }
    }
}
