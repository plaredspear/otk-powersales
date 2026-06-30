package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminSapInboundAuditQuery
import com.otoki.powersales.admin.dto.request.AdminSapOutboundLogQuery
import com.otoki.powersales.admin.dto.response.SapInboundAuditDetail
import com.otoki.powersales.admin.dto.response.SapInboundAuditListResponse
import com.otoki.powersales.admin.dto.response.SapInboundAuditRow
import com.otoki.powersales.admin.dto.response.SapInboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundCatalogItemDto
import com.otoki.powersales.admin.dto.response.SapOutboundLogDetail
import com.otoki.powersales.admin.dto.response.SapOutboundLogListResponse
import com.otoki.powersales.admin.dto.response.SapOutboxPendingListResponse
import com.otoki.powersales.admin.dto.response.SapOutboxPendingRow
import com.otoki.powersales.admin.sap.OutboundTriggerType
import com.otoki.powersales.admin.service.AdminSapIntegrationService
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.mockk.slot
import io.mockk.verify
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminSapIntegrationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSapIntegrationController 테스트")
class AdminSapIntegrationControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminSapIntegrationService: AdminSapIntegrationService

    @Test
    @DisplayName("GET /inbound/catalog - 카탈로그 row 응답 매핑")
    fun inboundCatalog_ok() {
        every { adminSapIntegrationService.inboundCatalog() } returns listOf(
                SapInboundCatalogItemDto(
                    endpointPath = "/api/v1/sap/organization",
                    koreanName = "조직 마스터 수신",
                    requiredScope = "sap.org.write",
                    targetEntity = "Organization",
                    controllerClass = "SapOrganizationMasterController",
                    description = "조직 마스터 UPSERT.",
                    enabled = true,
                )
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].endpointPath").value("/api/v1/sap/organization"))
            .andExpect(jsonPath("$.data[0].requiredScope").value("sap.org.write"))
            .andExpect(jsonPath("$.data[0].targetEntity").value("Organization"))
            .andExpect(jsonPath("$.data[0].enabled").value(true))
    }

    @Test
    @DisplayName("PUT /inbound/toggle - service.setInboundEnabled 위임 + 최신 카탈로그 반환")
    fun inboundToggle_ok() {
        every { adminSapIntegrationService.setInboundEnabled("/api/v1/sap/account", false) } returns Unit
        every { adminSapIntegrationService.inboundCatalog() } returns listOf(
            SapInboundCatalogItemDto(
                endpointPath = "/api/v1/sap/account",
                koreanName = "거래처 마스터 수신",
                requiredScope = "sap.account.write",
                targetEntity = "Account",
                controllerClass = "SapAccountMasterController",
                description = "거래처 마스터 UPSERT.",
                enabled = false,
            )
        )

        mockMvc.perform(
            put("/api/v1/admin/sap-integration/inbound/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"endpointPath":"/api/v1/sap/account","enabled":false}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].endpointPath").value("/api/v1/sap/account"))
            .andExpect(jsonPath("$.data[0].enabled").value(false))

        verify { adminSapIntegrationService.setInboundEnabled("/api/v1/sap/account", false) }
    }

    @Test
    @DisplayName("GET /inbound/audits - 필터 없이 호출 시 default page=1, size=20")
    fun inboundAudits_defaultPaging() {
        every { adminSapIntegrationService.searchInboundAudits(any()) } returns
            SapInboundAuditListResponse(
                items = listOf(
                    SapInboundAuditRow(
                        id = 12345L,
                        eventType = "REQUEST_ACCEPTED",
                        clientId = "sap-prod",
                        endpoint = "/api/v1/sap/organization",
                        httpMethod = "POST",
                        clientIp = "10.0.1.42",
                        scope = "sap.org.write",
                        receivedCount = 142,
                        previousCount = 140,
                        reason = null,
                        createdAt = java.time.LocalDateTime.of(2026, 5, 18, 3, 15, 22),
                    )
                ),
                totalCount = 1L,
                currentPage = 1,
                pageSize = 20,
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/audits"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].id").value(12345))
            .andExpect(jsonPath("$.data.items[0].eventType").value("REQUEST_ACCEPTED"))
            .andExpect(jsonPath("$.data.totalCount").value(1))
    }

    @Test
    @DisplayName("GET /inbound/audits - 필터 조합이 service query 로 전달")
    fun inboundAudits_filtersForwarded() {
        every { adminSapIntegrationService.searchInboundAudits(any()) } returns SapInboundAuditListResponse(items = emptyList(), totalCount = 0L, currentPage = 2, pageSize = 50)

        mockMvc.perform(
            get("/api/v1/admin/sap-integration/inbound/audits")
                .param("clientId", "sap-prod")
                .param("eventType", "REQUEST_REJECTED_IP")
                .param("endpoint", "/api/v1/sap/organization")
                .param("from", "2026-05-17T00:00:00Z")
                .param("to", "2026-05-18T00:00:00Z")
                .param("page", "2")
                .param("size", "50")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentPage").value(2))
            .andExpect(jsonPath("$.data.pageSize").value(50))

        val captor = slot<AdminSapInboundAuditQuery>()
        verify { adminSapIntegrationService.searchInboundAudits(capture(captor)) }
        val q = captor.captured
        assert(q.clientId == "sap-prod")
        assert(q.eventType == "REQUEST_REJECTED_IP")
        assert(q.endpoint == "/api/v1/sap/organization")
        assert(q.from == java.time.LocalDateTime.of(2026, 5, 17, 0, 0, 0))
        assert(q.to == java.time.LocalDateTime.of(2026, 5, 18, 0, 0, 0))
        assert(q.page == 2)
        assert(q.size == 50)
    }

    @Test
    @DisplayName("GET /inbound/audits/{id} - 단건 상세 응답")
    fun inboundAuditDetail_ok() {
        every { adminSapIntegrationService.getInboundAuditDetail(eq(123L)) } returns SapInboundAuditDetail(
                id = 123L,
                eventType = "REQUEST_REJECTED_IP",
                clientId = "sap-test",
                endpoint = "/api/v1/sap/erp-order",
                httpMethod = "POST",
                clientIp = "192.168.0.1",
                scope = "sap.order.write",
                receivedCount = null,
                previousCount = null,
                reason = "IP not allowed",
                createdAt = java.time.LocalDateTime.of(2026, 5, 18, 3, 14, 0),
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/audits/123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(123))
            .andExpect(jsonPath("$.data.reason").value("IP not allowed"))
    }

    @Test
    @DisplayName("GET /inbound/audits/{id} - 미존재 id 시 BusinessException 404")
    fun inboundAuditDetail_notFound() {
        every { adminSapIntegrationService.getInboundAuditDetail(eq(99999L)) } throws BusinessException(
                errorCode = "SAP_INBOUND_AUDIT_NOT_FOUND",
                message = "SAP 인바운드 audit 을 찾을 수 없습니다: 99999",
                httpStatus = HttpStatus.NOT_FOUND,
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/audits/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /outbound/catalog - 카탈로그 row 응답 매핑")
    fun outboundCatalog_ok() {
        every { adminSapIntegrationService.outboundCatalog() } returns
            listOf(
                SapOutboundCatalogItemDto(
                    interfaceId = "SD03130",
                    koreanName = "여사원일정 스케줄 배치",
                    triggerType = OutboundTriggerType.BATCH,
                    senderClass = "com.otoki.powersales.external.sap.outbound.sender.TeamMemberScheduleSapSender",
                    description = "매일 새벽 attendance 송신.",
                ),
                SapOutboundCatalogItemDto(
                    interfaceId = "SD03050",
                    koreanName = "주문 등록 (Outbox)",
                    triggerType = OutboundTriggerType.OUTBOX,
                    senderClass = "com.otoki.powersales.order.sap.sender.OrderRequestRegisterSender",
                    description = "outbox 큐 폴링 송신.",
                ),
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/outbound/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].interfaceId").value("SD03130"))
            .andExpect(jsonPath("$.data[0].triggerType").value("BATCH"))
            .andExpect(jsonPath("$.data[1].triggerType").value("OUTBOX"))
    }

    @Test
    @DisplayName("GET /outbound/logs - 필터 조합 전달")
    fun outboundLogs_filtersForwarded() {
        every { adminSapIntegrationService.searchOutboundLogs(any()) } returns SapOutboundLogListResponse(items = emptyList(), totalCount = 0L, currentPage = 1, pageSize = 20)

        mockMvc.perform(
            get("/api/v1/admin/sap-integration/outbound/logs")
                .param("interfaceId", "SD03130")
                .param("resultCode", "FAIL")
                .param("from", "2026-05-17T00:00:00Z")
                .param("to", "2026-05-18T00:00:00Z")
        )
            .andExpect(status().isOk)

        val captor = slot<AdminSapOutboundLogQuery>()
        verify { adminSapIntegrationService.searchOutboundLogs(capture(captor)) }
        val q = captor.captured
        assert(q.interfaceId == "SD03130")
        assert(q.resultCode == "FAIL")
        assert(q.from == java.time.LocalDateTime.of(2026, 5, 17, 0, 0, 0))
        assert(q.to == java.time.LocalDateTime.of(2026, 5, 18, 0, 0, 0))
    }

    @Test
    @DisplayName("GET /outbound/logs/{id} - errorDetail 포함")
    fun outboundLogDetail_ok() {
        every { adminSapIntegrationService.getOutboundLogDetail(eq(67890L)) } returns SapOutboundLogDetail(
                id = 67890L,
                interfaceId = "SD03040",
                endpointPath = "/sap/rest/SD03040",
                requestCount = 1,
                httpStatus = 504,
                resultCode = "FAIL",
                resultMsg = "Gateway Timeout",
                attemptCount = 3,
                durationMs = 30024L,
                errorDetail = "java.net.SocketTimeoutException: Read timed out at ...",
                requestedAt = java.time.LocalDateTime.of(2026, 5, 18, 2, 0, 0),
                completedAt = java.time.LocalDateTime.of(2026, 5, 18, 2, 0, 30),
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/outbound/logs/67890"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.resultCode").value("FAIL"))
            .andExpect(jsonPath("$.data.errorDetail").value("java.net.SocketTimeoutException: Read timed out at ..."))
            .andExpect(jsonPath("$.data.attemptCount").value(3))
    }

    @Test
    @DisplayName("GET /outbound/outbox-pending - PENDING + RETRY row 응답")
    fun outboxPending_ok() {
        every { adminSapIntegrationService.searchOutboxPending(eq(1), eq(20)) } returns
            SapOutboxPendingListResponse(
                items = listOf(
                    SapOutboxPendingRow(
                        id = 5001L,
                        domainType = "ORDER_REQUEST_REGISTER",
                        aggregateId = 9012L,
                        interfaceId = "SD03050",
                        status = "RETRY",
                        retryCount = 2,
                        lastError = "Connection timeout after 30s",
                        createdAt = java.time.LocalDateTime.of(2026, 5, 18, 2, 45, 11),
                        sentAt = null,
                    )
                ),
                totalCount = 1L,
                currentPage = 1,
                pageSize = 20,
            )

        mockMvc.perform(get("/api/v1/admin/sap-integration/outbound/outbox-pending"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].status").value("RETRY"))
            .andExpect(jsonPath("$.data.items[0].retryCount").value(2))
            .andExpect(jsonPath("$.data.totalCount").value(1))
    }
}
