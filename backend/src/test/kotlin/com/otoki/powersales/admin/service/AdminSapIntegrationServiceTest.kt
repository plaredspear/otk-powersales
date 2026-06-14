package com.otoki.powersales.admin.service

import com.otoki.powersales.admin.dto.request.AdminSapInboundAuditQuery
import com.otoki.powersales.admin.dto.request.AdminSapOutboundLogQuery
import com.otoki.powersales.admin.sap.OutboundTriggerType
import com.otoki.powersales.admin.sap.SapInboundCatalog
import com.otoki.powersales.admin.sap.SapOutboundCatalog
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.external.sap.SapConstants
import com.otoki.powersales.external.sap.auth.audit.SapInboundAudit
import com.otoki.powersales.external.sap.auth.audit.SapInboundAuditRepository
import com.otoki.powersales.external.sap.outbound.entity.SapOutboundLog
import com.otoki.powersales.external.sap.outbound.repository.SapOutboundLogRepository
import com.otoki.powersales.external.sap.outbox.SapOutbox
import com.otoki.powersales.external.sap.outbox.SapOutboxRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import java.time.LocalDateTime
import java.util.Optional

@DisplayName("AdminSapIntegrationService 테스트")
class AdminSapIntegrationServiceTest {

    private val inboundAuditRepository: SapInboundAuditRepository = mockk()
    private val outboundLogRepository: SapOutboundLogRepository = mockk()
    private val outboxRepository: SapOutboxRepository = mockk()

    private val service = AdminSapIntegrationService(
        sapInboundAuditRepository = inboundAuditRepository,
        sapOutboundLogRepository = outboundLogRepository,
        sapOutboxRepository = outboxRepository,
    )

    @Nested
    @DisplayName("inboundCatalog")
    inner class InboundCatalog {

        @Test
        @DisplayName("SapInboundCatalog.ITEMS 전량 반환 + 순서 유지")
        fun returnsAllItemsInOrder() {
            val result = service.inboundCatalog()

            assertThat(result).hasSize(SapInboundCatalog.ITEMS.size)
            assertThat(result.map { it.endpointPath })
                .containsExactlyElementsOf(SapInboundCatalog.ITEMS.map { it.endpointPath })
        }

        @Test
        @DisplayName("첫 row 가 조직 마스터 / scope=sap.org.write")
        fun firstRowIsOrganization() {
            val first = service.inboundCatalog().first()

            assertThat(first.endpointPath).isEqualTo("/api/v1/sap/organization")
            assertThat(first.requiredScope).isEqualTo("sap.org.write")
            assertThat(first.controllerClass).isEqualTo("SapOrganizeMasterController")
        }
    }

    @Nested
    @DisplayName("outboundCatalog")
    inner class OutboundCatalog {

        @Test
        @DisplayName("SapOutboundCatalog.ITEMS 전량 반환")
        fun returnsAllItems() {
            val result = service.outboundCatalog()

            assertThat(result).hasSize(SapOutboundCatalog.ITEMS.size)
        }

        @Test
        @DisplayName("모든 row 의 interfaceId 가 SapConstants const 값과 매칭")
        fun interfaceIdMatchesConstants() {
            val constants = setOf(
                SapConstants.SAP_INTERFACE_ATTENDANCE,
                SapConstants.SAP_INTERFACE_DISPLAY_MASTER,
                SapConstants.SAP_INTERFACE_PPT_MASTER,
                SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
                SapConstants.SAP_INTERFACE_ORDER_REQUEST_DETAIL,
                SapConstants.SAP_INTERFACE_ORDER_REQUEST_CANCEL,
                SapConstants.SAP_INTERFACE_LOAN_INQUIRY,
            )

            val ids = service.outboundCatalog().map { it.interfaceId }.toSet()
            assertThat(ids).isEqualTo(constants)
        }

        @Test
        @DisplayName("triggerType 분류가 모두 enum value 중 하나")
        fun triggerTypeIsEnumValue() {
            service.outboundCatalog().forEach {
                assertThat(it.triggerType).isIn(*OutboundTriggerType.entries.toTypedArray())
            }
        }
    }

    @Nested
    @DisplayName("searchInboundAudits")
    inner class SearchInboundAudits {

        @Test
        @DisplayName("필터 4개 모두 null - repository 에 그대로 null 전달")
        fun allFiltersNull() {
            val pageableSlot = slot<Pageable>()
            every {
                inboundAuditRepository.search(any(), any(), any(), any(), any(), capture(pageableSlot))
            } returns PageImpl(emptyList())

            service.searchInboundAudits(AdminSapInboundAuditQuery())

            verify {
                inboundAuditRepository.search(null, null, null, null, null, any())
            }
            assertThat(pageableSlot.captured.pageNumber).isEqualTo(0)
            assertThat(pageableSlot.captured.pageSize).isEqualTo(20)
        }

        @Test
        @DisplayName("단일 eventType 필터 - repository 에 전달")
        fun singleEventTypeFilter() {
            every {
                inboundAuditRepository.search(any(), any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.searchInboundAudits(
                AdminSapInboundAuditQuery(eventType = "REQUEST_ACCEPTED")
            )

            verify {
                inboundAuditRepository.search(null, "REQUEST_ACCEPTED", null, null, null, any())
            }
        }

        @Test
        @DisplayName("page=2 size=50 - 0-base 페이지로 변환되어 repository 전달")
        fun pagingNormalized() {
            val pageableSlot = slot<Pageable>()
            every {
                inboundAuditRepository.search(any(), any(), any(), any(), any(), capture(pageableSlot))
            } returns PageImpl(emptyList())

            service.searchInboundAudits(AdminSapInboundAuditQuery(page = 2, size = 50))

            assertThat(pageableSlot.captured.pageNumber).isEqualTo(1)
            assertThat(pageableSlot.captured.pageSize).isEqualTo(50)
        }

        @Test
        @DisplayName("size > 100 - 100으로 제한")
        fun sizeClamped() {
            val pageableSlot = slot<Pageable>()
            every {
                inboundAuditRepository.search(any(), any(), any(), any(), any(), capture(pageableSlot))
            } returns PageImpl(emptyList())

            service.searchInboundAudits(AdminSapInboundAuditQuery(size = 999))

            assertThat(pageableSlot.captured.pageSize).isEqualTo(100)
        }

        @Test
        @DisplayName("결과 매핑 - audit row 정확 변환")
        fun rowMapping() {
            val audit = SapInboundAudit(
                id = 1L,
                eventType = "REQUEST_ACCEPTED",
                clientId = "sap",
                endpoint = "/x",
                httpMethod = "POST",
                clientIp = "1.2.3.4",
                scope = "scope",
                receivedCount = 10,
                previousCount = 9,
                reason = null,
                createdAt = LocalDateTime.of(2026, 5, 18, 0, 0),
            )
            every {
                inboundAuditRepository.search(any(), any(), any(), any(), any(), any())
            } returns PageImpl(listOf(audit))

            val result = service.searchInboundAudits(AdminSapInboundAuditQuery())

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].id).isEqualTo(1L)
            assertThat(result.items[0].clientIp).isEqualTo("1.2.3.4")
            assertThat(result.items[0].receivedCount).isEqualTo(10)
        }
    }

    @Nested
    @DisplayName("getInboundAuditDetail")
    inner class GetInboundAuditDetail {

        @Test
        @DisplayName("존재하는 id - 모든 필드 1:1 매핑")
        fun existingId() {
            val audit = SapInboundAudit(
                id = 42L,
                eventType = "REQUEST_REJECTED_IP",
                clientId = "sap-test",
                endpoint = "/api/v1/sap/erp-order",
                httpMethod = "POST",
                clientIp = "192.168.0.1",
                scope = "sap.order.write",
                receivedCount = null,
                previousCount = null,
                reason = "IP not allowed",
                createdAt = LocalDateTime.of(2026, 5, 18, 3, 14, 0),
            )
            every { inboundAuditRepository.findById(42L) } returns Optional.of(audit)

            val result = service.getInboundAuditDetail(42L)

            assertThat(result.id).isEqualTo(42L)
            assertThat(result.reason).isEqualTo("IP not allowed")
            assertThat(result.eventType).isEqualTo("REQUEST_REJECTED_IP")
        }

        @Test
        @DisplayName("미존재 id - BusinessException NOT_FOUND")
        fun notFound() {
            every { inboundAuditRepository.findById(99999L) } returns Optional.empty()

            val ex = assertThrows<BusinessException> {
                service.getInboundAuditDetail(99999L)
            }
            assertThat(ex.errorCode).isEqualTo("SAP_INBOUND_AUDIT_NOT_FOUND")
            assertThat(ex.httpStatus.value()).isEqualTo(404)
        }
    }

    @Nested
    @DisplayName("searchOutboundLogs")
    inner class SearchOutboundLogs {

        @Test
        @DisplayName("필터 모두 null + 결과 매핑")
        fun allFiltersNull() {
            val log = SapOutboundLog(
                id = 100L,
                interfaceId = "SD03130",
                endpointPath = "/sap/rest/SD03130",
                requestCount = 50,
                httpStatus = 200,
                resultCode = "SUCCESS",
                resultMsg = "OK",
                attemptCount = 1,
                durationMs = 1234L,
                errorDetail = null,
                requestedAt = LocalDateTime.of(2026, 5, 18, 3, 0, 0),
                completedAt = LocalDateTime.of(2026, 5, 18, 3, 0, 1),
            )
            every {
                outboundLogRepository.search(any(), any(), any(), any(), any())
            } returns PageImpl(listOf(log))

            val result = service.searchOutboundLogs(AdminSapOutboundLogQuery())

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].interfaceId).isEqualTo("SD03130")
            assertThat(result.items[0].durationMs).isEqualTo(1234L)
        }

        @Test
        @DisplayName("resultCode 필터 - repository 전달")
        fun resultCodeFilter() {
            every {
                outboundLogRepository.search(any(), any(), any(), any(), any())
            } returns PageImpl(emptyList())

            service.searchOutboundLogs(AdminSapOutboundLogQuery(resultCode = "FAIL"))

            verify {
                outboundLogRepository.search(null, "FAIL", null, null, any())
            }
        }
    }

    @Nested
    @DisplayName("getOutboundLogDetail")
    inner class GetOutboundLogDetail {

        @Test
        @DisplayName("미존재 id - BusinessException NOT_FOUND")
        fun notFound() {
            every { outboundLogRepository.findById(99999L) } returns Optional.empty()

            val ex = assertThrows<BusinessException> {
                service.getOutboundLogDetail(99999L)
            }
            assertThat(ex.errorCode).isEqualTo("SAP_OUTBOUND_LOG_NOT_FOUND")
            assertThat(ex.httpStatus.value()).isEqualTo(404)
        }
    }

    @Nested
    @DisplayName("searchOutboxPending")
    inner class SearchOutboxPending {

        @Test
        @DisplayName("PENDING + RETRY 결과만 페이지 응답")
        fun pendingOrRetry() {
            val pending = SapOutbox(
                id = 1L,
                domainType = "ORDER_REQUEST_REGISTER",
                aggregateId = 9012L,
                interfaceId = SapConstants.SAP_INTERFACE_ORDER_REQUEST_REGIST,
                payload = "{}",
                status = SapOutbox.STATUS_PENDING,
                retryCount = 0,
                lastError = null,
                sentAt = null,
            ).apply { createdAt = LocalDateTime.of(2026, 5, 18, 2, 45, 11) }
            every { outboxRepository.pagePendingOrRetry(any()) } returns PageImpl(listOf(pending))

            val result = service.searchOutboxPending(1, 20)

            assertThat(result.items).hasSize(1)
            assertThat(result.items[0].status).isEqualTo(SapOutbox.STATUS_PENDING)
            assertThat(result.items[0].aggregateId).isEqualTo(9012L)
        }

        @Test
        @DisplayName("page=1 size=20 - pageable 0-base 변환")
        fun pageableNormalized() {
            val pageableSlot = slot<Pageable>()
            every { outboxRepository.pagePendingOrRetry(capture(pageableSlot)) } returns PageImpl(emptyList())

            service.searchOutboxPending(1, 20)

            assertThat(pageableSlot.captured.pageNumber).isEqualTo(0)
            assertThat(pageableSlot.captured.pageSize).isEqualTo(20)
        }
    }

    @Suppress("unused")
    private fun pageable(page: Int, size: Int) = PageRequest.of(page, size)
}
