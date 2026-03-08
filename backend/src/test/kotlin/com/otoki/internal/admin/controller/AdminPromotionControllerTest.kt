package com.otoki.internal.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.request.PromotionCreateRequest
import com.otoki.internal.admin.dto.response.PromotionDetailResponse
import com.otoki.internal.admin.dto.response.PromotionListItem
import com.otoki.internal.admin.dto.response.PromotionListResponse
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminPromotionService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.promotion.exception.AccountNotFoundException
import com.otoki.internal.promotion.exception.InvalidDateRangeException
import com.otoki.internal.promotion.exception.PromotionForbiddenException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminPromotionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionController 테스트")
class AdminPromotionControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockitoBean private lateinit var adminPromotionService: AdminPromotionService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions - 목록 조회")
    inner class GetPromotions {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getPromotions_success() {
            val response = PromotionListResponse(
                content = listOf(
                    PromotionListItem(
                        id = 1L,
                        promotionNumber = "PM00000001",
                        promotionName = "GS25 역삼점 3월 라면 행사",
                        promotionType = "일반행사",
                        accountName = "GS25 역삼점",
                        startDate = LocalDate.of(2026, 3, 10),
                        endDate = LocalDate.of(2026, 3, 20),
                        targetAmount = 5000000,
                        category = "라면",
                        costCenterCode = "1101",
                        isDeleted = false,
                        createdAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0)
                    )
                ),
                page = 0, size = 20, totalElements = 1, totalPages = 1
            )
            whenever(adminPromotionService.getPromotions(
                eq(1L), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].promotion_number").value("PM00000001"))
                .andExpect(jsonPath("$.data.content[0].promotion_name").value("GS25 역삼점 3월 라면 행사"))
                .andExpect(jsonPath("$.data.content[0].account_name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.content[0].category").value("라면"))
                .andExpect(jsonPath("$.data.content[0].cost_center_code").value("1101"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getPromotions_empty() {
            val response = PromotionListResponse(
                content = emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            whenever(adminPromotionService.getPromotions(
                eq(1L), anyOrNull(), anyOrNull(), anyOrNull(),
                anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()
            )).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.total_elements").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions/{id} - 상세 조회")
    inner class GetPromotion {

        @Test
        @DisplayName("성공 - 상세 정보 반환")
        fun getPromotion_success() {
            val response = createDetailResponse()
            whenever(adminPromotionService.getPromotion(eq(1L), eq(1L))).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotion_number").value("PM00000001"))
                .andExpect(jsonPath("$.data.account_name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.primary_product_name").value("진라면 매운맛 120g"))
                .andExpect(jsonPath("$.data.category").value("라면"))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터")
        fun getPromotion_notFound() {
            whenever(adminPromotionService.getPromotion(eq(1L), eq(999L)))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(get("/api/v1/admin/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 권한 외 조회")
        fun getPromotion_forbidden() {
            whenever(adminPromotionService.getPromotion(eq(1L), eq(1L)))
                .thenThrow(PromotionForbiddenException())

            mockMvc.perform(get("/api/v1/admin/promotions/1"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/promotions - 생성")
    inner class CreatePromotion {

        @Test
        @DisplayName("성공 - 행사마스터 생성")
        fun createPromotion_success() {
            val response = createDetailResponse()
            whenever(adminPromotionService.createPromotion(eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotion_number").value("PM00000001"))
                .andExpect(jsonPath("$.data.cost_center_code").value("1101"))
        }

        @Test
        @DisplayName("실패 - 행사명 누락")
        fun createPromotion_missingName() {
            val invalidJson = """
                {
                    "promotion_name": "",
                    "account_id": 100,
                    "start_date": "2026-03-10",
                    "end_date": "2026-03-20"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 날짜 범위 오류")
        fun createPromotion_invalidDateRange() {
            whenever(adminPromotionService.createPromotion(eq(1L), any()))
                .thenThrow(InvalidDateRangeException())

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }

        @Test
        @DisplayName("실패 - 미존재 거래처")
        fun createPromotion_accountNotFound() {
            whenever(adminPromotionService.createPromotion(eq(1L), any()))
                .thenThrow(AccountNotFoundException())

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/promotions/{id} - 수정")
    inner class UpdatePromotion {

        @Test
        @DisplayName("성공 - 행사마스터 수정")
        fun updatePromotion_success() {
            val response = createDetailResponse(promotionName = "수정된 행사명")
            whenever(adminPromotionService.updatePromotion(eq(1L), eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                put("/api/v1/admin/promotions/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotion_name").value("수정된 행사명"))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터 수정")
        fun updatePromotion_notFound() {
            whenever(adminPromotionService.updatePromotion(eq(1L), eq(999L), any()))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(
                put("/api/v1/admin/promotions/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/promotions/{id} - 삭제")
    inner class DeletePromotion {

        @Test
        @DisplayName("성공 - soft delete")
        fun deletePromotion_success() {
            doNothing().whenever(adminPromotionService).deletePromotion(eq(1L), eq(1L))

            mockMvc.perform(delete("/api/v1/admin/promotions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터 삭제")
        fun deletePromotion_notFound() {
            whenever(adminPromotionService.deletePromotion(eq(1L), eq(999L)))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(delete("/api/v1/admin/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 권한 외 삭제")
        fun deletePromotion_forbidden() {
            whenever(adminPromotionService.deletePromotion(eq(1L), eq(1L)))
                .thenThrow(PromotionForbiddenException())

            mockMvc.perform(delete("/api/v1/admin/promotions/1"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    // Helpers
    private fun createDetailResponse(
        promotionName: String = "GS25 역삼점 3월 라면 행사"
    ) = PromotionDetailResponse(
        id = 1L,
        promotionNumber = "PM00000001",
        promotionName = promotionName,
        promotionType = "일반행사",
        accountId = 100,
        accountName = "GS25 역삼점",
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = 200L,
        primaryProductName = "진라면 매운맛 120g",
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        targetAmount = 5000000,
        costCenterCode = "1101",
        category = "라면",
        isDeleted = false,
        createdAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0),
        updatedAt = LocalDateTime.of(2026, 3, 8, 10, 0, 0)
    )

    private fun createRequest() = PromotionCreateRequest(
        promotionName = "GS25 역삼점 3월 라면 행사",
        promotionType = "일반행사",
        accountId = 100,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = 200L,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        targetAmount = 5000000
    )
}
