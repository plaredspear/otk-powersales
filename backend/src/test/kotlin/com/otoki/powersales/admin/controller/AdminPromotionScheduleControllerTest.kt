package com.otoki.powersales.admin.controller

import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkDeleteResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleBulkUpdateResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleItem
import com.otoki.powersales.schedule.dto.response.PromotionScheduleListResponse
import com.otoki.powersales.schedule.dto.response.PromotionScheduleMember
import com.otoki.powersales.schedule.dto.response.SchedulePeriod
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.schedule.service.AdminPromotionScheduleService
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.schedule.exception.PromotionScheduleNotFoundPartialException
import com.otoki.powersales.schedule.exception.PromotionScheduleNotInPromotionException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.ObjectMapper
import java.time.LocalDate

@WebMvcTest(AdminPromotionScheduleController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionScheduleController 테스트")
class AdminPromotionScheduleControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockitoBean private lateinit var adminPromotionScheduleService: AdminPromotionScheduleService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    private val promotionId = 100L

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeNumber = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions/{id}/schedules - 일정 목록")
    inner class GetSchedules {

        @Test
        @DisplayName("성공 - 행사 일정 트리 반환")
        fun getSchedules_success() {
            val response = PromotionScheduleListResponse(
                promotionId = promotionId,
                schedulePeriod = SchedulePeriod(LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 7)),
                members = listOf(
                    PromotionScheduleMember(
                        promotionEmployeeId = 200L,
                        employeeId = 50L,
                        employeeNumber = "20030001",
                        employeeName = "홍길동",
                        schedules = listOf(
                            PromotionScheduleItem(
                                scheduleId = 1001L,
                                workingDate = LocalDate.of(2026, 5, 1),
                                accountId = 300,
                                accountCode = "SAP001",
                                accountName = "이마트 강남점",
                                workingCategory1 = "행사",
                                workingCategory3 = "고정",
                                workingCategory4 = null
                            )
                        )
                    )
                ),
                totalMemberCount = 1,
                totalScheduleCount = 1
            )
            whenever(adminPromotionScheduleService.getSchedules(eq(promotionId), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotions/$promotionId/schedules"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionId").value(100))
                .andExpect(jsonPath("$.data.totalMemberCount").value(1))
                .andExpect(jsonPath("$.data.totalScheduleCount").value(1))
                .andExpect(jsonPath("$.data.members[0].employeeNumber").value("20030001"))
                .andExpect(jsonPath("$.data.members[0].schedules[0].accountCode").value("SAP001"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/promotions/{id}/schedules/bulk - 일괄 변경")
    inner class BulkUpdate {

        @Test
        @DisplayName("성공 - 일괄 변경 응답")
        fun bulkUpdate_success() {
            whenever(adminPromotionScheduleService.bulkUpdate(eq(promotionId), any()))
                .thenReturn(PromotionScheduleBulkUpdateResponse(updatedCount = 2, scheduleIds = listOf(1001L, 1002L)))

            val body = """
                {
                  "items": [
                    {
                      "scheduleId": 1001,
                      "accountId": 301,
                      "workingDate": "2026-05-02",
                      "workingCategory1": "행사",
                      "workingCategory3": "고정"
                    },
                    {
                      "scheduleId": 1002,
                      "accountId": 302,
                      "workingDate": "2026-05-03",
                      "workingCategory1": "행사",
                      "workingCategory3": "순회"
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/admin/promotions/$promotionId/schedules/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.updatedCount").value(2))
                .andExpect(jsonPath("$.data.scheduleIds[0]").value(1001))
        }

        @Test
        @DisplayName("실패 - 빈 items 배열 -> 400")
        fun bulkUpdate_empty() {
            val body = """{"items": []}"""

            mockMvc.perform(
                put("/api/v1/admin/promotions/$promotionId/schedules/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 다른 행사 소속 일정 -> 403 FORBIDDEN")
        fun bulkUpdate_forbidden() {
            whenever(adminPromotionScheduleService.bulkUpdate(eq(promotionId), any()))
                .thenThrow(PromotionScheduleNotInPromotionException())

            val body = """
                {
                  "items": [
                    {"scheduleId": 1001, "accountId": 301, "workingDate": "2026-05-02", "workingCategory1": "행사", "workingCategory3": "고정"}
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/admin/promotions/$promotionId/schedules/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_IN_PROMOTION"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/promotions/{id}/schedules/bulk - 일괄 삭제")
    inner class BulkDelete {

        @Test
        @DisplayName("성공 - 3건 삭제")
        fun bulkDelete_success() {
            whenever(adminPromotionScheduleService.bulkDelete(eq(promotionId), any()))
                .thenReturn(PromotionScheduleBulkDeleteResponse(deletedCount = 3))

            val body = """{"scheduleIds": [1001, 1002, 1003]}"""

            mockMvc.perform(
                delete("/api/v1/admin/promotions/$promotionId/schedules/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(3))
        }

        @Test
        @DisplayName("실패 - 부분 미존재 -> 404 missing_ids 포함")
        fun bulkDelete_partialNotFound() {
            whenever(adminPromotionScheduleService.bulkDelete(eq(promotionId), any()))
                .thenThrow(PromotionScheduleNotFoundPartialException(listOf(1003L, 1005L)))

            val body = """{"scheduleIds": [1001, 1003, 1005]}"""

            mockMvc.perform(
                delete("/api/v1/admin/promotions/$promotionId/schedules/bulk")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND_PARTIAL"))
                .andExpect(jsonPath("$.error.details.missingIds[0]").value(1003))
                .andExpect(jsonPath("$.error.details.missingIds[1]").value(1005))
        }
    }
}
