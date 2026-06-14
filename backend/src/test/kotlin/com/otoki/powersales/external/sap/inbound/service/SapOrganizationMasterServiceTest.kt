package com.otoki.powersales.external.sap.inbound.service

import com.otoki.powersales.domain.org.organization.service.OrganizationReplaceService
import com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceCommand
import com.otoki.powersales.domain.org.organization.service.dto.OrganizationReplaceResult
import com.otoki.powersales.external.sap.inbound.dto.organization.OrganizationMasterRequestItem
import com.otoki.powersales.external.sap.inbound.exception.SapInvalidPayloadException
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("SapOrganizationMasterService 어댑터 테스트")
class SapOrganizationMasterServiceTest {

    private val organizationReplaceService: OrganizationReplaceService = mockk()
    private val service = SapOrganizationMasterService(organizationReplaceService)

    private fun item(suffix: String): OrganizationMasterRequestItem = OrganizationMasterRequestItem(
        ccCd2 = "10$suffix", orgCd2 = "100$suffix", orgNm2 = "본사",
        ccCd3 = "11$suffix", orgCd3 = "110$suffix", orgNm3 = "사업부",
        ccCd4 = "12$suffix", orgCd4 = "120$suffix", orgNm4 = "팀",
        ccCd5 = "13$suffix", orgCd5 = "130$suffix", orgNm5 = "지점$suffix"
    )

    @Nested
    @DisplayName("replaceAll - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (replacedCount=N) → OrganizationMasterDetail (successCount=N, failureCount=0, failures=empty)")
        fun happy_domainResultMapped() {
            every { organizationReplaceService.replaceAll(any()) } returns
                OrganizationReplaceResult(replacedCount = 3)

            val detail = service.replaceAll(listOf(item("1"), item("2"), item("3")))

            assertThat(detail.successCount).isEqualTo(3)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()
        }

        @Test
        @DisplayName("페이로드 검증 실패 (행 전체 null) - SapInvalidPayloadException, 도메인 호출 없음")
        fun validation_allNullRow_throwsAndNoDomainCall() {
            val items = listOf(item("1"), OrganizationMasterRequestItem(), item("3"))

            assertThatThrownBy { service.replaceAll(items) }
                .isInstanceOf(SapInvalidPayloadException::class.java)
                .hasMessageContaining("line 2")

            verify(exactly = 0) { organizationReplaceService.replaceAll(any()) }
        }

        @Test
        @DisplayName("도메인 throw - 예외 재전파 (트랜잭션 롤백은 도메인 측 책임)")
        fun domainThrow_rethrow() {
            every { organizationReplaceService.replaceAll(any()) } throws
                RuntimeException("constraint violation")

            assertThatThrownBy { service.replaceAll(listOf(item("1"))) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("constraint violation")
        }

        @Test
        @DisplayName("DTO 매핑: OrganizationMasterRequestItem → OrganizationReplaceCommand 12 필드")
        fun dtoMapping_itemToCommand() {
            every { organizationReplaceService.replaceAll(any()) } returns
                OrganizationReplaceResult(replacedCount = 1)

            service.replaceAll(listOf(item("0")))

            val captor = slot<List<OrganizationReplaceCommand>>()
            verify { organizationReplaceService.replaceAll(capture(captor)) }
            val command = captor.captured.single()
            assertThat(command.ccCd2).isEqualTo("100")
            assertThat(command.orgCd5).isEqualTo("1300")
            assertThat(command.orgNm5).isEqualTo("지점0")
        }
    }
}
