package com.otoki.powersales.sap.inbound.service

import com.otoki.powersales.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.sap.entity.SystemCodeMaster
import com.otoki.powersales.sap.inbound.dto.product.SystemCodeMasterRequestItem
import com.otoki.powersales.sap.repository.SystemCodeMasterRepository
import org.assertj.core.api.Assertions.assertThat
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
@DisplayName("SapSystemCodeMasterService 테스트")
class SapSystemCodeMasterServiceTest {

    @Mock
    private lateinit var systemCodeMasterRepository: SystemCodeMasterRepository

    @Mock
    private lateinit var auditService: SapInboundAuditService

    @InjectMocks
    private lateinit var service: SapSystemCodeMasterService

    @Nested
    @DisplayName("upsert - Happy Path")
    inner class UpsertHappy {

        @Test
        @DisplayName("신규 - INSERT, externalKey = company;group;detail")
        fun upsert_insertNew() {
            whenever(systemCodeMasterRepository.findByExternalKey("1000;H10010;10")).thenReturn(null)

            val detail = service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        groupCodeName = "직원 상태",
                        detailCodeName = "재직",
                        seq = "1"
                    )
                )
            )

            val captor = argumentCaptor<List<SystemCodeMaster>>()
            verify(systemCodeMasterRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved.externalKey).isEqualTo("1000;H10010;10")
            assertThat(saved.companyCode).isEqualTo("1000")
            assertThat(saved.groupCode).isEqualTo("H10010")
            assertThat(saved.detailCode).isEqualTo("10")
            assertThat(saved.detailCodeName).isEqualTo("재직")
            assertThat(detail.successCount).isEqualTo(1)
            verify(auditService).record(any<SapInboundAudit>())
        }

        @Test
        @DisplayName("기존 갱신 - DetailCodeName 변경")
        fun upsert_updateExisting() {
            val existing = SystemCodeMaster(
                companyCode = "1000",
                groupCode = "H10010",
                detailCode = "10",
                externalKey = "1000;H10010;10",
                detailCodeName = "기존이름"
            )
            whenever(systemCodeMasterRepository.findByExternalKey("1000;H10010;10")).thenReturn(existing)

            service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = "1000",
                        groupCode = "H10010",
                        detailCode = "10",
                        detailCodeName = "신규이름"
                    )
                )
            )

            val captor = argumentCaptor<List<SystemCodeMaster>>()
            verify(systemCodeMasterRepository).saveAll(captor.capture())
            val saved = captor.firstValue.single()
            assertThat(saved).isSameAs(existing)
            assertThat(saved.detailCodeName).isEqualTo("신규이름")
        }
    }

    @Nested
    @DisplayName("upsert - Error Path")
    inner class UpsertError {

        @Test
        @DisplayName("GroupCode 누락 - failures")
        fun upsert_missingGroupCode() {
            val detail = service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = "1000",
                        groupCode = null,
                        detailCode = "10"
                    )
                )
            )

            assertThat(detail.successCount).isEqualTo(0)
            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("GroupCode 필수")
            verify(systemCodeMasterRepository, never()).saveAll(any<List<SystemCodeMaster>>())
        }

        @Test
        @DisplayName("CompanyCode 누락 - failures")
        fun upsert_missingCompanyCode() {
            val detail = service.upsert(
                listOf(
                    SystemCodeMasterRequestItem(
                        companyCode = null,
                        groupCode = "H10010",
                        detailCode = "10"
                    )
                )
            )

            assertThat(detail.failureCount).isEqualTo(1)
            assertThat(detail.failures.single().reason).contains("CompanyCode 필수")
        }
    }
}
