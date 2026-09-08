package com.otoki.powersales.domain.activity.claim.service

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("ClaimDeliveryStatusRule (조치/처리 정보 보유 → 전송완료 파생 승격) 테스트")
class ClaimDeliveryStatusRuleTest {

    private val afterCutoff: LocalDateTime = ClaimDeliveryStatusRule.PROMOTION_START_AT.plusDays(1)
    private val beforeCutoff: LocalDateTime = ClaimDeliveryStatusRule.PROMOTION_START_AT.minusSeconds(1)

    private fun claim(
        sfid: String? = null,
        status: ClaimStatus? = ClaimStatus.DRAFT,
        createdAt: LocalDateTime = afterCutoff,
        counselNumber: String? = null,
        actionStatus: String? = null,
        interfaceDate: LocalDateTime? = null,
    ) = Claim(
        id = 1L,
        sfid = sfid,
        status = status,
        counselNumber = counselNumber,
        actionStatus = actionStatus,
        interfaceDate = interfaceDate,
    ).apply { this.createdAt = createdAt }

    @Test
    @DisplayName("상담번호가 있으면 임시저장 → 전송완료 승격 (레거시 afterUpdateStatus 정합)")
    fun promotes_whenCounselNumberExists() {
        val claim = claim(counselNumber = "C-1001")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isTrue()
        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
    }

    @Test
    @DisplayName("전송일시(코스모스 전송 성공 시각)만 있어도 승격")
    fun promotes_whenInterfaceDateExists() {
        val claim = claim(interfaceDate = LocalDateTime.of(2026, 8, 20, 10, 0))

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isTrue()
        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
    }

    @Test
    @DisplayName("전송실패도 승격 대상 — 레거시 규칙이 '전송실패 || 임시저장' 을 함께 본다")
    fun promotes_whenSendFailed() {
        val claim = claim(status = ClaimStatus.SEND_FAILED, actionStatus = "접수")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isTrue()
        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
    }

    @Test
    @DisplayName("status 가 NULL 이어도 조치 정보가 있으면 승격")
    fun promotes_whenStatusNull() {
        val claim = claim(status = null, actionStatus = "완료")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isTrue()
        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
    }

    @Test
    @DisplayName("커트라인(2026-08-10) 이전 등록분은 승격하지 않는다")
    fun skips_whenCreatedBeforeCutoff() {
        val claim = claim(createdAt = beforeCutoff, counselNumber = "C-1001")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isFalse()
        assertThat(claim.status).isEqualTo(ClaimStatus.DRAFT)
    }

    @Test
    @DisplayName("SF 이관분(sfid 보유)은 원본 Status 가 권위라 승격하지 않는다")
    fun skips_whenMigratedFromSf() {
        val claim = claim(sfid = "a0X0000000000001", counselNumber = "C-1001")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isFalse()
        assertThat(claim.status).isEqualTo(ClaimStatus.DRAFT)
    }

    @Test
    @DisplayName("조치 정보가 blank 면 증거로 보지 않는다 — sync 가 빈 값으로도 덮어쓰기 때문")
    fun skips_whenEvidenceIsBlank() {
        val claim = claim(counselNumber = "   ", actionStatus = "")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isFalse()
        assertThat(claim.status).isEqualTo(ClaimStatus.DRAFT)
    }

    @Test
    @DisplayName("이미 전송완료면 무변경 (재승격 없음)")
    fun skips_whenAlreadySent() {
        val claim = claim(status = ClaimStatus.SENT, counselNumber = "C-1001")

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isFalse()
        assertThat(claim.status).isEqualTo(ClaimStatus.SENT)
    }

    @Test
    @DisplayName("조치 정보가 전혀 없으면 임시저장 유지 — 접수~조치 회신 시차 구간은 구제 대상 아님")
    fun skips_whenNoEvidence() {
        val claim = claim()

        assertThat(ClaimDeliveryStatusRule.promoteIfDelivered(claim)).isFalse()
        assertThat(claim.status).isEqualTo(ClaimStatus.DRAFT)
    }
}
