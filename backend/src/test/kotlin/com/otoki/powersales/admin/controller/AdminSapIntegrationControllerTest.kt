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
import com.otoki.powersales.admin.dto.response.SapOutboundLogRow
import com.otoki.powersales.admin.dto.response.SapOutboxPendingListResponse
import com.otoki.powersales.admin.dto.response.SapOutboxPendingRow
import com.otoki.powersales.admin.sap.OutboundTriggerType
import com.otoki.powersales.admin.service.AdminSapIntegrationService
import com.otoki.powersales.common.exception.BusinessException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.HttpStatus
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminSapIntegrationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSapIntegrationController 테스트")
class AdminSapIntegrationControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockitoBean private lateinit var adminSapIntegrationService: AdminSapIntegrationService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @Test
    @DisplayName("GET /inbound/catalog - 카탈로그 row 응답 매핑")
    fun inboundCatalog_ok() {
        whenever(adminSapIntegrationService.inboundCatalog()).thenReturn(
            listOf(
                SapInboundCatalogItemDto(
                    endpointPath = "/api/v1/sap/organization",
                    koreanName = "조직 마스터 수신",
                    requiredScope = "sap.org.write",
                    targetEntity = "OrganizeMaster",
                    controllerClass = "SapOrganizeMasterController",
                    description = "조직 마스터 UPSERT.",
                )
            )
        )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data[0].endpointPath").value("/api/v1/sap/organization"))
            .andExpect(jsonPath("$.data[0].requiredScope").value("sap.org.write"))
            .andExpect(jsonPath("$.data[0].targetEntity").value("OrganizeMaster"))
    }

    @Test
    @DisplayName("GET /inbound/audits - 필터 없이 호출 시 default page=1, size=20")
    fun inboundAudits_defaultPaging() {
        whenever(adminSapIntegrationService.searchInboundAudits(any())).thenReturn(
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
        whenever(adminSapIntegrationService.searchInboundAudits(any())).thenReturn(
            SapInboundAuditListResponse(items = emptyList(), totalCount = 0L, currentPage = 2, pageSize = 50)
        )

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

        val captor = argumentCaptor<AdminSapInboundAuditQuery>()
        verify(adminSapIntegrationService).searchInboundAudits(captor.capture())
        val q = captor.firstValue
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
        whenever(adminSapIntegrationService.getInboundAuditDetail(eq(123L))).thenReturn(
            SapInboundAuditDetail(
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
        )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/audits/123"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.id").value(123))
            .andExpect(jsonPath("$.data.reason").value("IP not allowed"))
    }

    @Test
    @DisplayName("GET /inbound/audits/{id} - 미존재 id 시 BusinessException 404")
    fun inboundAuditDetail_notFound() {
        whenever(adminSapIntegrationService.getInboundAuditDetail(eq(99999L))).thenThrow(
            BusinessException(
                errorCode = "SAP_INBOUND_AUDIT_NOT_FOUND",
                message = "SAP 인바운드 audit 을 찾을 수 없습니다: 99999",
                httpStatus = HttpStatus.NOT_FOUND,
            )
        )

        mockMvc.perform(get("/api/v1/admin/sap-integration/inbound/audits/99999"))
            .andExpect(status().isNotFound)
    }

    @Test
    @DisplayName("GET /outbound/catalog - 카탈로그 row 응답 매핑")
    fun outboundCatalog_ok() {
        whenever(adminSapIntegrationService.outboundCatalog()).thenReturn(
            listOf(
                SapOutboundCatalogItemDto(
                    interfaceId = "TeamMemberSchedule",
                    koreanName = "일반 출근 일일 batch",
                    triggerType = OutboundTriggerType.BATCH,
                    senderClass = "com.otoki.powersales.sap.outbound.sender.AttendanceSapSender",
                    description = "매일 새벽 attendance 송신.",
                ),
                SapOutboundCatalogItemDto(
                    interfaceId = "IF_REST_SAP_OrderRequestRegist",
                    koreanName = "주문 등록 (Outbox)",
                    triggerType = OutboundTriggerType.OUTBOX,
                    senderClass = "com.otoki.powersales.order.sap.sender.OrderRequestRegisterSender",
                    description = "outbox 큐 폴링 송신.",
                ),
            )
        )

        mockMvc.perform(get("/api/v1/admin/sap-integration/outbound/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].interfaceId").value("TeamMemberSchedule"))
            .andExpect(jsonPath("$.data[0].triggerType").value("BATCH"))
            .andExpect(jsonPath("$.data[1].triggerType").value("OUTBOX"))
    }

    @Test
    @DisplayName("GET /outbound/logs - 필터 조합 전달")
    fun outboundLogs_filtersForwarded() {
        whenever(adminSapIntegrationService.searchOutboundLogs(any())).thenReturn(
            SapOutboundLogListResponse(items = emptyList(), totalCount = 0L, currentPage = 1, pageSize = 20)
        )

        mockMvc.perform(
            get("/api/v1/admin/sap-integration/outbound/logs")
                .param("interfaceId", "TeamMemberSchedule")
                .param("resultCode", "FAIL")
                .param("from", "2026-05-17T00:00:00Z")
                .param("to", "2026-05-18T00:00:00Z")
        )
            .andExpect(status().isOk)

        val captor = argumentCaptor<AdminSapOutboundLogQuery>()
        verify(adminSapIntegrationService).searchOutboundLogs(captor.capture())
        val q = captor.firstValue
        assert(q.interfaceId == "TeamMemberSchedule")
        assert(q.resultCode == "FAIL")
        assert(q.from == java.time.LocalDateTime.of(2026, 5, 17, 0, 0, 0))
        assert(q.to == java.time.LocalDateTime.of(2026, 5, 18, 0, 0, 0))
    }

    @Test
    @DisplayName("GET /outbound/logs/{id} - errorDetail 포함")
    fun outboundLogDetail_ok() {
        whenever(adminSapIntegrationService.getOutboundLogDetail(eq(67890L))).thenReturn(
            SapOutboundLogDetail(
                id = 67890L,
                interfaceId = "LoanInquiry",
                endpointPath = "/sap/rest/loan",
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
        whenever(adminSapIntegrationService.searchOutboxPending(eq(1), eq(20))).thenReturn(
            SapOutboxPendingListResponse(
                items = listOf(
                    SapOutboxPendingRow(
                        id = 5001L,
                        domainType = "ORDER_REQUEST_REGISTER",
                        aggregateId = 9012L,
                        interfaceId = "IF_REST_SAP_OrderRequestRegist",
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
        )

        mockMvc.perform(get("/api/v1/admin/sap-integration/outbound/outbox-pending"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.items[0].status").value("RETRY"))
            .andExpect(jsonPath("$.data.items[0].retryCount").value(2))
            .andExpect(jsonPath("$.data.totalCount").value(1))
    }
}
