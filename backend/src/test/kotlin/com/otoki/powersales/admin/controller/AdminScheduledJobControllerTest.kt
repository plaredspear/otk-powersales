package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.admin.service.AdminScheduledJobService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminScheduledJobController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminScheduledJobController 테스트")
class AdminScheduledJobControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockitoBean private lateinit var adminScheduledJobService: AdminScheduledJobService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @Test
    @DisplayName("GET /runs - 필터 없이 호출 시 service 에 page=1, size=20 기본값 전달")
    fun runs_defaultPaging() {
        val response = ScheduledJobRunListResponse(
            items = listOf(
                ScheduledJobRunDto(
                    id = 1L,
                    jobName = "sap-outbox-worker",
                    startedAt = LocalDateTime.of(2026, 5, 18, 1, 0, 0),
                    endedAt = LocalDateTime.of(2026, 5, 18, 1, 0, 5),
                    durationMs = 5000L,
                    status = "SUCCESS",
                    errorMessage = null,
                    metadata = """{"processed":3}""",
                )
            ),
            totalCount = 1L,
            currentPage = 1,
            pageSize = 20,
        )
        whenever(adminScheduledJobService.search(any())).thenReturn(response)

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/runs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalCount").value(1))
            .andExpect(jsonPath("$.data.items[0].jobName").value("sap-outbox-worker"))
            .andExpect(jsonPath("$.data.items[0].status").value("SUCCESS"))
            .andExpect(jsonPath("$.data.items[0].durationMs").value(5000))
    }

    @Test
    @DisplayName("GET /runs - 필터 파라미터가 service query 로 전달")
    fun runs_withFilters() {
        whenever(adminScheduledJobService.search(any())).thenReturn(
            ScheduledJobRunListResponse(items = emptyList(), totalCount = 0L, currentPage = 2, pageSize = 50)
        )

        mockMvc.perform(
            get("/api/v1/admin/scheduled-jobs/runs")
                .param("jobName", "sap-outbox-worker")
                .param("status", "FAILURE")
                .param("from", "2026-05-17T00:00:00")
                .param("to", "2026-05-18T00:00:00")
                .param("page", "2")
                .param("size", "50")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentPage").value(2))
            .andExpect(jsonPath("$.data.pageSize").value(50))

        val captor = org.mockito.kotlin.argumentCaptor<AdminScheduledJobQuery>()
        org.mockito.kotlin.verify(adminScheduledJobService).search(captor.capture())
        val q = captor.firstValue
        assert(q.jobName == "sap-outbox-worker")
        assert(q.status == "FAILURE")
        assert(q.from == LocalDateTime.of(2026, 5, 17, 0, 0, 0))
        assert(q.to == LocalDateTime.of(2026, 5, 18, 0, 0, 0))
        assert(q.page == 2)
        assert(q.size == 50)
    }

    @Test
    @DisplayName("GET /catalog - 정적 9건 응답 매핑")
    fun catalog_ok() {
        whenever(adminScheduledJobService.catalog()).thenReturn(
            listOf(
                RegisteredScheduledJobDto("sap-outbox-worker", "*/30 * * * * *", "SAP outbox worker"),
                RegisteredScheduledJobDto("pptMaster.expire", "0 0 23 * * *", "전문행사조 만료"),
            )
        )

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].jobName").value("sap-outbox-worker"))
            .andExpect(jsonPath("$.data[1].cron").value("0 0 23 * * *"))
    }

    @Test
    @DisplayName("GET /summary - default windowHours=24 응답")
    fun summary_default() {
        val to = LocalDateTime.of(2026, 5, 18, 12, 0, 0)
        val from = to.minusHours(24)
        whenever(adminScheduledJobService.summary(eq(24L))).thenReturn(
            ScheduledJobSummaryResponse(
                windowFrom = from,
                windowTo = to,
                totalCount = 100L,
                runningCount = 2L,
                successCount = 95L,
                failureCount = 3L,
                distinctJobNames = listOf("sap-outbox-worker", "pptMaster.expire"),
            )
        )

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalCount").value(100))
            .andExpect(jsonPath("$.data.successCount").value(95))
            .andExpect(jsonPath("$.data.failureCount").value(3))
            .andExpect(jsonPath("$.data.runningCount").value(2))
            .andExpect(jsonPath("$.data.distinctJobNames.length()").value(2))
    }
}
