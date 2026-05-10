package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.organization.service.OrganizationReplaceService
import com.otoki.powersales.organization.service.dto.OrganizationReplaceCommand
import com.otoki.powersales.organization.service.dto.OrganizationReplaceResult
import com.otoki.powersales.sap.inbound.dto.organize.OrganizeMasterRequestItem
import com.otoki.powersales.sap.inbound.exception.SapInvalidPayloadException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExtendWith(MockitoExtension::class)
@DisplayName("SapOrganizeMasterService 어댑터 테스트")
class SapOrganizeMasterServiceTest {

    @Mock
    private lateinit var organizationReplaceService: OrganizationReplaceService

    @InjectMocks
    private lateinit var service: SapOrganizeMasterService

    private fun item(suffix: String): OrganizeMasterRequestItem = OrganizeMasterRequestItem(
        ccCd2 = "10$suffix", orgCd2 = "100$suffix", orgNm2 = "본사",
        ccCd3 = "11$suffix", orgCd3 = "110$suffix", orgNm3 = "사업부",
        ccCd4 = "12$suffix", orgCd4 = "120$suffix", orgNm4 = "팀",
        ccCd5 = "13$suffix", orgCd5 = "130$suffix", orgNm5 = "지점$suffix"
    )

    @Nested
    @DisplayName("replaceAll - 어댑터 책임")
    inner class AdapterResponsibilities {

        @Test
        @DisplayName("happy: 도메인 결과 (replacedCount=N) → OrganizeMasterDetail (successCount=N, failureCount=0, failures=empty)")
        fun happy_domainResultMapped() {
            whenever(organizationReplaceService.replaceAll(any())).thenReturn(
                OrganizationReplaceResult(replacedCount = 3)
            )

            val detail = service.replaceAll(listOf(item("1"), item("2"), item("3")))

            assertThat(detail.successCount).isEqualTo(3)
            assertThat(detail.failureCount).isEqualTo(0)
            assertThat(detail.failures).isEmpty()
        }

        @Test
        @DisplayName("페이로드 검증 실패 (행 전체 null) - SapInvalidPayloadException, 도메인 호출 없음")
        fun validation_allNullRow_throwsAndNoDomainCall() {
            val items = listOf(item("1"), OrganizeMasterRequestItem(), item("3"))

            assertThatThrownBy { service.replaceAll(items) }
                .isInstanceOf(SapInvalidPayloadException::class.java)
                .hasMessageContaining("line 2")

            verify(organizationReplaceService, never()).replaceAll(any())
        }

        @Test
        @DisplayName("도메인 throw - 예외 재전파 (트랜잭션 롤백은 도메인 측 책임)")
        fun domainThrow_rethrow() {
            whenever(organizationReplaceService.replaceAll(any()))
                .thenThrow(RuntimeException("constraint violation"))

            assertThatThrownBy { service.replaceAll(listOf(item("1"))) }
                .isInstanceOf(RuntimeException::class.java)
                .hasMessage("constraint violation")
        }

        @Test
        @DisplayName("DTO 매핑: OrganizeMasterRequestItem → OrganizationReplaceCommand 12 필드")
        fun dtoMapping_itemToCommand() {
            whenever(organizationReplaceService.replaceAll(any())).thenReturn(
                OrganizationReplaceResult(replacedCount = 1)
            )

            service.replaceAll(listOf(item("0")))

            val captor = argumentCaptor<List<OrganizationReplaceCommand>>()
            verify(organizationReplaceService).replaceAll(captor.capture())
            val command = captor.firstValue.single()
            assertThat(command.ccCd2).isEqualTo("100")
            assertThat(command.orgCd5).isEqualTo("1300")
            assertThat(command.orgNm5).isEqualTo("지점0")
        }
    }
}
