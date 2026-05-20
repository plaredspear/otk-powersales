package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.claim.entity.Claim
import com.otoki.powersales.claim.enums.ClaimDateType
import com.otoki.powersales.claim.enums.ClaimType1
import com.otoki.powersales.claim.enums.ClaimType2
import com.otoki.powersales.claim.repository.ClaimRepository
import com.otoki.powersales.employee.entity.Employee
import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.inbound.dto.claim.ClaimStatusRequestItem
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

@DisplayName("SapClaimStatusService 테스트")
class SapClaimStatusServiceTest {

    private val claimRepository: ClaimRepository = mockk()
    private val auditService: SapInboundAuditService = mockk()
    private val service = SapClaimStatusService(claimRepository, auditService)

    @BeforeEach
    fun setUp() {
        every { auditService.record(any<SapInboundAudit>()) } answers { firstArg() }
        every { claimRepository.findAllByNameIn(any()) } returns emptyList()
        every { claimRepository.saveAll(any<List<Claim>>()) } answers { firstArg() }
    }

    private fun claim(
        name: String?,
        actionStatus: String? = null
    ): Claim = Claim(
        employee = Employee(employeeCode = "100123", name = "홍길동"),
        account = Account(),
        accountName = "홍길동상회",
        productCode = "P001",
        productName = "진라면",
        dateType = ClaimDateType.EXPIRY_DATE,
        date = LocalDate.of(2026, 4, 28),
        claimType1 = ClaimType1.B,
        claimType2 = ClaimType2.BD,
        defectDescription = "변질",
        defectQuantity = BigDecimal.valueOf(1L),
        name = name,
        actionStatus = actionStatus
    )

    private fun item(
        name: String? = "CLM-2026-04-001",
        claimSequence: String? = "001",
        actionCode: String? = "AC02",
        claimStatus: String? = "처리완료",
        content: String? = "교환 처리 완료",
        reasonType: String? = "RT01",
        cosmosKey: String? = "CSMS-20260428-001"
    ): ClaimStatusRequestItem = ClaimStatusRequestItem(
        name = name,
        claimSequence = claimSequence,
        actionCode = actionCode,
        claimStatus = claimStatus,
        content = content,
        reasonType = reasonType,
        cosmosKey = cosmosKey
    )

    @Nested
    @DisplayName("update - Happy Path")
    inner class UpdateHappy {

        @Test
        @DisplayName("기존 Claim 갱신 - SAP 처리 필드 6종 모두 적용")
        fun update_existingClaim() {
            val existing = claim(name = "CLM-2026-04-001")
            every { claimRepository.findAllByNameIn(listOf("CLM-2026-04-001")) } returns listOf(existing)
            every { claimRepository.saveAll(any<List<Claim>>()) } answers { firstArg() }
            every { auditService.record(any<SapInboundAudit>()) } answers { firstArg() }

            val detail = service.update(listOf(item()))

            val captor = slot<List<Claim>>()
            verify { claimRepository.saveAll(capture(captor)) }
            val saved = captor.captured.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.counselNumber).isEqualTo("001")
            assertThat(saved.actionCode).isEqualTo("AC02")
            assertThat(saved.actionStatus).isEqualTo("처리완료")
            assertThat(saved.actContent).isEqualTo("교환 처리 완료")
            assertThat(saved.reasonType).isEqualTo("RT01")
            assertThat(saved.cosmosKey).isEqualTo("CSMS-20260428-001")
            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(0)
            verify { auditService.record(any<SapInboundAudit>()) }
        }

        @Test
        @DisplayName("다중 행 일괄 - 모두 매칭 성공 시 success=N")
        fun update_multipleRows() {
            val a = claim(name = "CLM-001")
            val b = claim(name = "CLM-002")
            every { claimRepository.findAllByNameIn(listOf("CLM-001", "CLM-002")) } returns listOf(a, b)
            every { claimRepository.saveAll(any<List<Claim>>()) } answers { firstArg() }
            every { auditService.record(any<SapInboundAudit>()) } answers { firstArg() }

            val detail = service.update(
                listOf(item(name = "CLM-001"), item(name = "CLM-002"))
            )

            assertThat(detail.successCount).isEqualTo(2)
            assertThat(detail.failureCount).isEqualTo(0)
        }
    }

    @Nested
    @DisplayName("update - Error Path")
    inner class UpdateError {

        @Test
        @DisplayName("Name 누락 - failure (Name 필수)")
        fun update_missingName() {
            val detail = service.update(listOf(item(name = null)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("Name 필수")
            verify(exactly = 0) { claimRepository.saveAll(any<List<Claim>>()) }
        }

        @Test
        @DisplayName("Claim 미존재 - failure (claim not found), INSERT 안 함")
        fun update_claimNotFound() {
            every { claimRepository.findAllByNameIn(listOf("CLM-NOTEXIST")) } returns emptyList()

            val detail = service.update(listOf(item(name = "CLM-NOTEXIST")))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().name).isEqualTo("CLM-NOTEXIST")
            assertThat(detail.failures.single().reason).isEqualTo("claim not found")
            verify(exactly = 0) { claimRepository.saveAll(any<List<Claim>>()) }
        }

        @Test
        @DisplayName("ActionStatus 길이 초과 - failure (actionStatus length exceeded)")
        fun update_actionStatusLengthExceeded() {
            val tooLong = "x".repeat(SapClaimStatusService.ACTION_STATUS_MAX_LENGTH + 1)
            // 길이 초과 검증이 조회 전에 발생하므로 mock 불필요

            val detail = service.update(listOf(item(claimStatus = tooLong)))

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).isEqualTo("actionStatus length exceeded")
        }

        @Test
        @DisplayName("부분 실패 - 1건 매칭, 1건 미존재")
        fun update_partialFailure() {
            val existing = claim(name = "CLM-OK")
            every { claimRepository.findAllByNameIn(listOf("CLM-OK", "CLM-NG")) } returns listOf(existing)
            every { claimRepository.saveAll(any<List<Claim>>()) } answers { firstArg() }
            every { auditService.record(any<SapInboundAudit>()) } answers { firstArg() }

            val detail = service.update(
                listOf(item(name = "CLM-OK"), item(name = "CLM-NG"))
            )

            assertThat(detail.successCount).isEqualTo(1)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().name).isEqualTo("CLM-NG")
        }
    }
}
