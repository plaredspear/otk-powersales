package com.otoki.powersales.promotion.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.auth.entity.AppAuthority
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.promotion.dto.response.MobilePromotionDetailResponse
import com.otoki.powersales.promotion.dto.response.MobilePromotionEmployeeItem
import com.otoki.powersales.promotion.dto.response.MobilePromotionListItem
import com.otoki.powersales.promotion.dto.response.MobilePromotionListResponse
import com.otoki.powersales.promotion.dto.response.MyPromotionAssignmentItem
import com.otoki.powersales.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.promotion.exception.PromotionInvalidParameterException
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.service.MobilePromotionService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(PromotionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PromotionController 테스트")
class PromotionControllerTest : MobileControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var mobilePromotionService: MobilePromotionService

    @BeforeEach
    fun setUpLeaderPrincipal() {
        authenticateAs(userId = 1L, role = AppAuthority.LEADER)
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/promotions - 행사 목록 조회")
    inner class GetPromotions {

        @Test
        @DisplayName("성공 - 행사 목록 반환")
        fun getPromotions_success() {
            val items = listOf(
                MobilePromotionListItem(
                    id = 1L,
                    promotionNumber = "PM-2026-001",
                    promotionType = "시식",
                    accountName = "이마트 강남점",
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 3, 31),
                    standLocation = "1층 입구",
                    isClosed = false,
                    myScheduleDate = LocalDate.of(2026, 3, 15)
                ),
                MobilePromotionListItem(
                    id = 2L,
                    promotionNumber = "PM-2026-002",
                    promotionType = "진열",
                    accountName = "홈플러스 잠실점",
                    startDate = LocalDate.of(2026, 3, 10),
                    endDate = LocalDate.of(2026, 3, 20),
                    standLocation = "2층 매대",
                    isClosed = false,
                    myScheduleDate = null
                )
            )
            val response = MobilePromotionListResponse(
                content = items,
                page = 0,
                size = 20,
                totalElements = 2L,
                totalPages = 1
            )
            every { mobilePromotionService.getPromotions(1L, null, null, null, null, 0, 20) } returns response

            mockMvc.perform(get("/api/v1/mobile/promotions"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].id").value(1))
                .andExpect(jsonPath("$.data.content[0].promotionNumber").value("PM-2026-001"))
                .andExpect(jsonPath("$.data.content[0].promotionType").value("시식"))
                .andExpect(jsonPath("$.data.content[0].accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.content[0].startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.content[0].endDate").value("2026-03-31"))
                .andExpect(jsonPath("$.data.content[0].standLocation").value("1층 입구"))
                .andExpect(jsonPath("$.data.content[0].isClosed").value(false))
                .andExpect(jsonPath("$.data.content[0].myScheduleDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
        }

        @Test
        @DisplayName("성공 - 날짜 필터 조회")
        fun getPromotions_withDateFilter() {
            val response = MobilePromotionListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0L,
                totalPages = 0
            )
            every { mobilePromotionService.getPromotions(1L, "2026-03-01", "2026-03-31", null, null, 0, 20) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/promotions")
                    .param("startDate", "2026-03-01")
                    .param("endDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - 키워드 검색")
        fun getPromotions_withKeyword() {
            val items = listOf(
                MobilePromotionListItem(
                    id = 1L,
                    promotionNumber = "PM-2026-001",
                    promotionType = "시식",
                    accountName = "이마트 강남점",
                    startDate = LocalDate.of(2026, 3, 1),
                    endDate = LocalDate.of(2026, 3, 31),
                    standLocation = "1층 입구",
                    isClosed = false,
                    myScheduleDate = null
                )
            )
            val response = MobilePromotionListResponse(
                content = items,
                page = 0,
                size = 20,
                totalElements = 1L,
                totalPages = 1
            )
            every { mobilePromotionService.getPromotions(1L, null, null, "봄맞이", null, 0, 20) } returns response

            mockMvc.perform(
                get("/api/v1/mobile/promotions")
                    .param("keyword", "봄맞이")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식")
        fun getPromotions_invalidDateFormat() {
            every { mobilePromotionService.getPromotions(1L, "invalid-date", null, null, null, 0, 20) } throws
                PromotionInvalidParameterException()

            mockMvc.perform(
                get("/api/v1/mobile/promotions")
                    .param("startDate", "invalid-date")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/promotions/my-assignments - 담당 행사 일람")
    inner class GetMyAssignments {

        @Test
        @DisplayName("성공 - 담당 행사 목록 반환 (date 미지정)")
        fun getMyAssignments_success() {
            val items = listOf(
                MyPromotionAssignmentItem(
                    id = 10L,
                    promotionId = 1L,
                    promotionNumber = "PM-2026-001",
                    promotionType = "시식",
                    accountName = "이마트 강남점",
                    scheduleDate = LocalDate.of(2026, 6, 9),
                    standLocation = "엔드",
                    isClosed = false
                )
            )
            every { mobilePromotionService.getMyAssignments(1L, null) } returns items

            mockMvc.perform(get("/api/v1/mobile/promotions/my-assignments"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].promotionId").value(1))
                .andExpect(jsonPath("$.data[0].promotionNumber").value("PM-2026-001"))
                .andExpect(jsonPath("$.data[0].accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data[0].scheduleDate").value("2026-06-09"))
                .andExpect(jsonPath("$.data[0].standLocation").value("엔드"))
                .andExpect(jsonPath("$.data[0].isClosed").value(false))
        }

        @Test
        @DisplayName("성공 - date 파라미터 전달")
        fun getMyAssignments_withDate() {
            every { mobilePromotionService.getMyAssignments(1L, "2026-06-09") } returns emptyList()

            mockMvc.perform(
                get("/api/v1/mobile/promotions/my-assignments")
                    .param("date", "2026-06-09")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(0))
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식")
        fun getMyAssignments_invalidDate() {
            every { mobilePromotionService.getMyAssignments(1L, "bad-date") } throws
                PromotionInvalidParameterException()

            mockMvc.perform(
                get("/api/v1/mobile/promotions/my-assignments")
                    .param("date", "bad-date")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/promotions/{id} - 행사 상세 조회")
    inner class GetPromotion {

        @Test
        @DisplayName("성공 - 행사 상세 반환")
        fun getPromotion_success() {
            val employees = listOf(
                MobilePromotionEmployeeItem(
                    id = 10L,
                    employeeName = "홍길동",
                    scheduleDate = LocalDate.of(2026, 3, 15),
                    workStatus = "근무",
                    workType3 = "고정",
                    targetAmount = 1000000L,
                    actualAmount = 800000L,
                    isMine = true,
                    isClosed = false
                ),
                MobilePromotionEmployeeItem(
                    id = 11L,
                    employeeName = "김영희",
                    scheduleDate = LocalDate.of(2026, 3, 16),
                    workStatus = "근무",
                    workType3 = "순회",
                    targetAmount = 500000L,
                    actualAmount = null,
                    isMine = false,
                    isClosed = false
                )
            )
            val detail = MobilePromotionDetailResponse(
                id = 1L,
                promotionNumber = "PM-2026-001",
                promotionType = "시식",
                accountId = 100,
                accountName = "이마트 강남점",
                startDate = LocalDate.of(2026, 3, 1),
                endDate = LocalDate.of(2026, 3, 31),
                primaryProductName = "진라면",
                otherProduct = "너구리",
                message = "행사 안내 메시지",
                standLocation = "1층 입구",
                productType = "라면",
                isClosed = false,
                remark = "비고 내용",
                employees = employees
            )
            every { mobilePromotionService.getPromotion(1L, 1L) } returns detail

            mockMvc.perform(get("/api/v1/mobile/promotions/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.promotionNumber").value("PM-2026-001"))
                .andExpect(jsonPath("$.data.promotionType").value("시식"))
                .andExpect(jsonPath("$.data.accountId").value(100))
                .andExpect(jsonPath("$.data.accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.startDate").value("2026-03-01"))
                .andExpect(jsonPath("$.data.endDate").value("2026-03-31"))
                .andExpect(jsonPath("$.data.primaryProductName").value("진라면"))
                .andExpect(jsonPath("$.data.otherProduct").value("너구리"))
                .andExpect(jsonPath("$.data.message").value("행사 안내 메시지"))
                .andExpect(jsonPath("$.data.standLocation").value("1층 입구"))
                .andExpect(jsonPath("$.data.productType").value("라면"))
                .andExpect(jsonPath("$.data.isClosed").value(false))
                .andExpect(jsonPath("$.data.remark").value("비고 내용"))
                .andExpect(jsonPath("$.data.employees").isArray)
                .andExpect(jsonPath("$.data.employees.length()").value(2))
                .andExpect(jsonPath("$.data.employees[0].id").value(10))
                .andExpect(jsonPath("$.data.employees[0].employeeName").value("홍길동"))
                .andExpect(jsonPath("$.data.employees[0].scheduleDate").value("2026-03-15"))
                .andExpect(jsonPath("$.data.employees[0].workStatus").value("근무"))
                .andExpect(jsonPath("$.data.employees[0].workType3").value("고정"))
                .andExpect(jsonPath("$.data.employees[1].id").value(11))
                .andExpect(jsonPath("$.data.employees[1].employeeName").value("김영희"))
        }

        @Test
        @DisplayName("실패 - 미존재 행사")
        fun getPromotion_notFound() {
            every { mobilePromotionService.getPromotion(1L, 999L) } throws PromotionNotFoundException()

            mockMvc.perform(get("/api/v1/mobile/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 접근 권한 없음")
        fun getPromotion_forbidden() {
            every { mobilePromotionService.getPromotion(1L, 5L) } throws PromotionForbiddenException()

            mockMvc.perform(get("/api/v1/mobile/promotions/5"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }
}
