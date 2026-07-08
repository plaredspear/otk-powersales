package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.request.AdminScheduledJobQuery
import com.otoki.powersales.admin.dto.response.OroraMaterializeAcceptedResponse
import com.otoki.powersales.admin.dto.response.RegisteredScheduledJobDto
import com.otoki.powersales.admin.dto.response.ScheduledJobManualTriggerResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobRunDto
import com.otoki.powersales.admin.dto.response.ScheduledJobRunListResponse
import com.otoki.powersales.admin.dto.response.ScheduledJobSummaryResponse
import com.otoki.powersales.admin.service.AdminScheduledJobService
import com.otoki.powersales.admin.service.PPTMasterManualTriggerService
import com.otoki.powersales.platform.batch.PPTMasterExpireBatch
import com.otoki.powersales.platform.batch.PPTMasterSyncBatch
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.slot
import io.mockk.verify
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminScheduledJobController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminScheduledJobController 테스트")
class AdminScheduledJobControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminScheduledJobService: AdminScheduledJobService
    @MockkBean private lateinit var pptMasterManualTriggerService: PPTMasterManualTriggerService

    @Test
    @DisplayName("GET /runs - 필터 없이 호출 시 service 에 page=1, size=20 기본값 전달")
    fun runs_defaultPaging() {
        val response = ScheduledJobRunListResponse(
            items = listOf(
                ScheduledJobRunDto(
                    id = 1L,
                    jobName = "sap-outbox-worker",
                    startedAt = java.time.LocalDateTime.of(2026, 5, 18, 1, 0, 0),
                    endedAt = java.time.LocalDateTime.of(2026, 5, 18, 1, 0, 5),
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
        every { adminScheduledJobService.search(any()) } returns response

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
        val captor = slot<AdminScheduledJobQuery>()
        every { adminScheduledJobService.search(capture(captor)) } returns ScheduledJobRunListResponse(items = emptyList(), totalCount = 0L, currentPage = 2, pageSize = 50)

        mockMvc.perform(
            get("/api/v1/admin/scheduled-jobs/runs")
                .param("jobName", "sap-outbox-worker")
                .param("status", "FAILURE")
                .param("from", "2026-05-17T00:00:00Z")
                .param("to", "2026-05-18T00:00:00Z")
                .param("page", "2")
                .param("size", "50")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.currentPage").value(2))
            .andExpect(jsonPath("$.data.pageSize").value(50))

        val q = captor.captured
        assert(q.jobName == "sap-outbox-worker")
        assert(q.status == "FAILURE")
        assert(q.from == java.time.LocalDateTime.of(2026, 5, 17, 0, 0, 0))
        assert(q.to == java.time.LocalDateTime.of(2026, 5, 18, 0, 0, 0))
        assert(q.page == 2)
        assert(q.size == 50)
    }

    @Test
    @DisplayName("GET /catalog - 응답 매핑 + enabled 활성/비활성 플래그 직렬화")
    fun catalog_ok() {
        every { adminScheduledJobService.catalog() } returns listOf(
                RegisteredScheduledJobDto("sap-outbox-worker", "*/30 * * * * *", "SAP outbox worker", enabled = true, runtimeEnabled = true),
                RegisteredScheduledJobDto("pptMaster.expire", "0 0 23 * * *", "전문행사조 만료", enabled = false, runtimeEnabled = true),
            )

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/catalog"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].jobName").value("sap-outbox-worker"))
            .andExpect(jsonPath("$.data[0].enabled").value(true))
            .andExpect(jsonPath("$.data[1].cron").value("0 0 23 * * *"))
            .andExpect(jsonPath("$.data[1].enabled").value(false))
    }

    @Test
    @DisplayName("GET /summary - default windowHours=24 응답")
    fun summary_default() {
        val to = java.time.LocalDateTime.of(2026, 5, 18, 12, 0, 0)
        val from = to.minus(24, java.time.temporal.ChronoUnit.HOURS)
        every { adminScheduledJobService.summary(eq(24L)) } returns ScheduledJobSummaryResponse(
                windowFrom = from,
                windowTo = to,
                totalCount = 100L,
                runningCount = 2L,
                successCount = 95L,
                failureCount = 3L,
                distinctJobNames = listOf("sap-outbox-worker", "pptMaster.expire"),
            )

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.totalCount").value(100))
            .andExpect(jsonPath("$.data.successCount").value(95))
            .andExpect(jsonPath("$.data.failureCount").value(3))
            .andExpect(jsonPath("$.data.runningCount").value(2))
            .andExpect(jsonPath("$.data.distinctJobNames.length()").value(2))
    }

    @Test
    @DisplayName("POST /orora-monthly/trigger - salesMonth 가 service 로 전달되고 202 접수 응답 반환")
    fun triggerOroraMonthly_withMonth() {
        every { adminScheduledJobService.triggerOroraMonthly("202604") } returns
            OroraMaterializeAcceptedResponse(
                jobName = "ororaMonthlySalesMaterialize",
                salesMonth = "202604",
                message = "202604 적재를 시작했습니다.",
            )

        mockMvc.perform(
            post("/api/v1/admin/scheduled-jobs/orora-monthly/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"salesMonth":"202604"}""")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.salesMonth").value("202604"))
            .andExpect(jsonPath("$.data.accepted").value(true))

        verify(exactly = 1) { adminScheduledJobService.triggerOroraMonthly("202604") }
    }

    @Test
    @DisplayName("POST /orora-monthly/trigger - body 없으면 service 에 null 전달 (전월 자동)")
    fun triggerOroraMonthly_noBody() {
        every { adminScheduledJobService.triggerOroraMonthly(null) } returns
            OroraMaterializeAcceptedResponse("ororaMonthlySalesMaterialize", "202605", true, "202605 적재를 시작했습니다.")

        mockMvc.perform(post("/api/v1/admin/scheduled-jobs/orora-monthly/trigger"))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.data.salesMonth").value("202605"))

        verify(exactly = 1) { adminScheduledJobService.triggerOroraMonthly(null) }
    }

    @Test
    @DisplayName("POST /orora-daily/trigger - salesMonth 가 service 로 전달되고 202 접수 응답 반환")
    fun triggerOroraDaily_withMonth() {
        every { adminScheduledJobService.triggerOroraDaily("202606") } returns
            OroraMaterializeAcceptedResponse(
                jobName = "ororaDailySalesMaterialize",
                salesMonth = "202606",
                message = "202606 적재를 시작했습니다.",
            )

        mockMvc.perform(
            post("/api/v1/admin/scheduled-jobs/orora-daily/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"salesMonth":"202606"}""")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.salesMonth").value("202606"))
            .andExpect(jsonPath("$.data.accepted").value(true))

        verify(exactly = 1) { adminScheduledJobService.triggerOroraDaily("202606") }
    }

    @Test
    @DisplayName("POST /orora-daily/trigger - body 없으면 service 에 null 전달 (당월 자동)")
    fun triggerOroraDaily_noBody() {
        every { adminScheduledJobService.triggerOroraDaily(null) } returns
            OroraMaterializeAcceptedResponse("ororaDailySalesMaterialize", "202606", true, "202606 적재를 시작했습니다.")

        mockMvc.perform(post("/api/v1/admin/scheduled-jobs/orora-daily/trigger"))
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.data.salesMonth").value("202606"))

        verify(exactly = 1) { adminScheduledJobService.triggerOroraDaily(null) }
    }

    @Test
    @DisplayName("POST /orora-daily/chunk/trigger - chunkIndex + salesMonth 가 service 로 전달")
    fun triggerOroraDailyChunk_withMonth() {
        every { adminScheduledJobService.triggerOroraDailyChunk(2, "202606") } returns
            OroraMaterializeAcceptedResponse(
                jobName = "ororaDailySalesMaterialize",
                salesMonth = "202606",
                message = "202606 적재를 시작했습니다.",
            )

        mockMvc.perform(
            post("/api/v1/admin/scheduled-jobs/orora-daily/chunk/trigger")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"chunkIndex":2,"salesMonth":"202606"}""")
        )
            .andExpect(status().isAccepted)
            .andExpect(jsonPath("$.data.salesMonth").value("202606"))
            .andExpect(jsonPath("$.data.accepted").value(true))

        verify(exactly = 1) { adminScheduledJobService.triggerOroraDailyChunk(2, "202606") }
    }

    @Test
    @DisplayName("GET /orora-daily/chunks - 청크 카탈로그 응답 매핑")
    fun getOroraDailyChunks_ok() {
        every { adminScheduledJobService.ororaDailyChunkCatalog() } returns
            com.otoki.powersales.admin.dto.response.OroraDailyChunkCatalogResponse(
                chunkCount = 2,
                chunkSize = 2000L,
                chunks = listOf(
                    com.otoki.powersales.admin.dto.response.OroraMonthlyChunkInfo(0, "0001000000", "0001001999"),
                    com.otoki.powersales.admin.dto.response.OroraMonthlyChunkInfo(1, "0001002000", "0001003999"),
                ),
            )

        mockMvc.perform(get("/api/v1/admin/scheduled-jobs/orora-daily/chunks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.chunkCount").value(2))
            .andExpect(jsonPath("$.data.chunks[0].fromAccountCode").value("0001000000"))

        verify(exactly = 1) { adminScheduledJobService.ororaDailyChunkCatalog() }
    }

    @Test
    @DisplayName("POST /ppt-master/expire/trigger - expire jobName 으로 service 위임")
    fun triggerPptMaster_expire() {
        every { pptMasterManualTriggerService.trigger(PPTMasterExpireBatch.JOB_NAME) } returns
            ScheduledJobManualTriggerResponse(PPTMasterExpireBatch.JOB_NAME, "SUCCESS")

        mockMvc.perform(post("/api/v1/admin/scheduled-jobs/ppt-master/expire/trigger"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.jobName").value(PPTMasterExpireBatch.JOB_NAME))
            .andExpect(jsonPath("$.data.status").value("SUCCESS"))

        verify(exactly = 1) { pptMasterManualTriggerService.trigger(PPTMasterExpireBatch.JOB_NAME) }
    }

    @Test
    @DisplayName("POST /ppt-master/sync/trigger - sync jobName 으로 service 위임")
    fun triggerPptMaster_sync() {
        every { pptMasterManualTriggerService.trigger(PPTMasterSyncBatch.JOB_NAME) } returns
            ScheduledJobManualTriggerResponse(PPTMasterSyncBatch.JOB_NAME, "SUCCESS")

        mockMvc.perform(post("/api/v1/admin/scheduled-jobs/ppt-master/sync/trigger"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.jobName").value(PPTMasterSyncBatch.JOB_NAME))

        verify(exactly = 1) { pptMasterManualTriggerService.trigger(PPTMasterSyncBatch.JOB_NAME) }
    }

    @Test
    @DisplayName("POST /ppt-master/{action}/trigger - 미지원 action 은 400 + service 미호출")
    fun triggerPptMaster_invalidAction() {
        mockMvc.perform(post("/api/v1/admin/scheduled-jobs/ppt-master/unknown/trigger"))
            .andExpect(status().isBadRequest)

        verify(exactly = 0) { pptMasterManualTriggerService.trigger(any()) }
    }
}
