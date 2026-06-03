package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.promotion.dto.request.PromotionCreateRequest
import com.otoki.powersales.promotion.dto.response.*
import com.otoki.powersales.promotion.service.AdminPromotionService
import com.otoki.powersales.promotion.service.AdminPromotionTargetActualReportService
import com.otoki.powersales.promotion.exception.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(AdminPromotionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionController 테스트")
class AdminPromotionControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminPromotionService: AdminPromotionService

    @MockkBean private lateinit var targetActualReportService: AdminPromotionTargetActualReportService

    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions/form-meta - 폼 메타 조회")
    inner class GetPromotionFormMeta {

        @Test
        @DisplayName("성공 - 행사유형 + 매대위치 반환")
        fun getFormMeta_success() {
            val response = PromotionFormMetaResponse(
                promotionTypes = listOf(
                    PromotionTypeOption(value = "SAMPLING", name = "시식"),
                    PromotionTypeOption(value = "RECOMMENDATION", name = "권장")
                ),
                standLocations = listOf(
                    StandLocationOption(value = "FROZEN_EVENT", name = "냉동행사장"),
                    StandLocationOption(value = "ISLAND", name = "아일랜드")
                )
            )
            every { adminPromotionService.getPromotionFormMeta() } returns response

            mockMvc.perform(get("/api/v1/admin/promotions/form-meta"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionTypes[0].value").value("SAMPLING"))
                .andExpect(jsonPath("$.data.standLocations[0].value").value("FROZEN_EVENT"))
        }
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
                        promotionName = "냉장/냉동(XO_교자 (324GX2))",
                        promotionType = "시식",
                        accountName = "GS25 역삼점",
                        accountCode = "1052715",
                        startDate = LocalDate.of(2026, 3, 10),
                        endDate = LocalDate.of(2026, 3, 20),
                        primaryProductName = "XO_교자 (324GX2)",
                        primaryProductCode = "8801045123456",
                        standLocation = "평대",
                        productType = "냉장/냉동",
                        category1 = "냉장",
                        isClosed = false,
                        costCenterCode = "1101",
                        targetAmount = 1500000.0,
                        actualAmount = 1230000.0,
                        createdById = 42L,
                        createdByName = "홍길동",
                        isDeleted = false,
                        createdAt = java.time.LocalDateTime.of(2026, 3, 8, 10, 0, 0)
                    )
                ),
                page = 0, size = 20, totalElements = 1, totalPages = 1
            )
            every { adminPromotionService.getPromotions(
                any(), any(), any(), any(), any(), any(), any()
            ) } returns response

            mockMvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].promotionNumber").value("PM00000001"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getPromotions_empty() {
            val response = PromotionListResponse(
                content = emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0
            )
            every { adminPromotionService.getPromotions(
                any(), any(), any(), any(), any(), any(), any()
            ) } returns response

            mockMvc.perform(get("/api/v1/admin/promotions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions/{id} - 상세 조회")
    inner class GetPromotion {

        @Test
        @DisplayName("성공 - 상세 정보 반환")
        fun getPromotion_success() {
            val response = createDetailResponse()
            every { adminPromotionService.getPromotion(any(), eq(1L)) } returns response

            mockMvc.perform(get("/api/v1/admin/promotions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionNumber").value("PM00000001"))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터")
        fun getPromotion_notFound() {
            every { adminPromotionService.getPromotion(any(), eq(999L)) } throws PromotionNotFoundException()

            mockMvc.perform(get("/api/v1/admin/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 권한 외 조회")
        fun getPromotion_forbidden() {
            every { adminPromotionService.getPromotion(any(), eq(1L)) } throws PromotionForbiddenException()

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
            every { adminPromotionService.createPromotion(eq(1L), any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotionNumber").value("PM00000001"))
        }

        /**
         * Validation 실패는 @Valid 메커니즘에 의해 동일 INVALID_PARAMETER 응답으로 매핑.
         * 분기별 메시지 차이가 없으므로 1 케이스로 집약.
         */
        @ParameterizedTest(name = "validation 실패 - {0}")
        @ValueSource(strings = [
            // missingPrimaryProductId
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":"매장 입구"}""",
            // nullPrimaryProductId
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","primaryProductId":null,"standLocation":"매장 입구"}""",
            // missingPromotionType
            """{"accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":"매장 입구"}""",
            // missingStandLocation
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20"}""",
            // emptyStandLocation
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":""}""",
            // blankStandLocation
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":"   "}"""
        ])
        fun createPromotion_validationFailures(json: String) {
            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 날짜 범위 오류")
        fun createPromotion_invalidDateRange() {
            every { adminPromotionService.createPromotion(eq(1L), any()) } throws InvalidDateRangeException()

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }

        @Test
        @DisplayName("실패 - 유효하지 않은 매대위치")
        fun createPromotion_invalidStandLocation() {
            every { adminPromotionService.createPromotion(eq(1L), any()) } throws InvalidStandLocationException()

            mockMvc.perform(
                post("/api/v1/admin/promotions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_STAND_LOCATION"))
        }

        @Test
        @DisplayName("실패 - 미존재 거래처")
        fun createPromotion_accountNotFound() {
            every { adminPromotionService.createPromotion(eq(1L), any()) } throws AccountNotFoundException()

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
            val response = createDetailResponse()
            every { adminPromotionService.updatePromotion(any(), any(), eq(1L), eq(1L), any()) } returns response

            mockMvc.perform(
                put("/api/v1/admin/promotions/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @ParameterizedTest(name = "validation 실패 - {0}")
        @ValueSource(strings = [
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":"매장 입구"}""",
            """{"accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20","standLocation":"매장 입구"}""",
            """{"promotionType":"시식","accountId":100,"startDate":"2026-03-10","endDate":"2026-03-20"}"""
        ])
        fun updatePromotion_validationFailures(json: String) {
            mockMvc.perform(
                put("/api/v1/admin/promotions/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터 수정")
        fun updatePromotion_notFound() {
            every { adminPromotionService.updatePromotion(any(), any(), eq(999L), eq(1L), any()) } throws PromotionNotFoundException()

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
            every { adminPromotionService.deletePromotion(any(), any(), eq(1L)) } just Runs

            mockMvc.perform(delete("/api/v1/admin/promotions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 미존재 행사마스터 삭제")
        fun deletePromotion_notFound() {
            every { adminPromotionService.deletePromotion(any(), any(), eq(999L)) } throws PromotionNotFoundException()

            mockMvc.perform(delete("/api/v1/admin/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 권한 외 삭제")
        fun deletePromotion_forbidden() {
            every { adminPromotionService.deletePromotion(any(), any(), eq(1L)) } throws PromotionForbiddenException()

            mockMvc.perform(delete("/api/v1/admin/promotions/1"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    // Helpers
    private fun createDetailResponse(
        remark: String? = null
    ) = PromotionDetailResponse(
        id = 1L,
        promotionNumber = "PM00000001",
        promotionName = "시식(진라면 매운맛 120g)",
        promotionType = "시식",
        accountId = 100,
        accountName = "GS25 역삼점",
        accountCode = "1088139",
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = 200L,
        primaryProductName = "진라면 매운맛 120g",
        primaryProductCode = "10010011",
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        costCenterCode = "1101",
        productType = "냉장/냉동",
        category1 = null,
        targetAmount = 400000L,
        actualAmount = 0L,
        isClosed = false,
        isDeleted = false,
        createdById = 10L,
        createdByName = "홍길동",
        createdAt = java.time.LocalDateTime.of(2026, 3, 8, 10, 0, 0),
        updatedAt = java.time.LocalDateTime.of(2026, 3, 8, 10, 0, 0),
        remark = remark
    )

    private fun createRequest() = PromotionCreateRequest(
        promotionType = "시식",
        accountId = 100,
        startDate = LocalDate.of(2026, 3, 10),
        endDate = LocalDate.of(2026, 3, 20),
        primaryProductId = 200L,
        otherProduct = "너구리, 진짬뽕",
        message = "3월 라면 프로모션 진행",
        standLocation = "매장 입구 좌측",
        remark = null
    )
}
