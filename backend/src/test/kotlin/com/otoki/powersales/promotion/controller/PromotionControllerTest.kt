package com.otoki.powersales.promotion.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.promotion.dto.response.MobilePromotionDetailResponse
import com.otoki.powersales.promotion.dto.response.MobilePromotionEmployeeItem
import com.otoki.powersales.promotion.dto.response.MobilePromotionListItem
import com.otoki.powersales.promotion.dto.response.MobilePromotionListResponse
import com.otoki.powersales.promotion.exception.PromotionForbiddenException
import com.otoki.powersales.promotion.exception.PromotionInvalidParameterException
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import com.otoki.powersales.promotion.service.MobilePromotionService
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.isNull
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(PromotionController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("PromotionController 테스트")
class PromotionControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var mobilePromotionService: MobilePromotionService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.LEADER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
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
            whenever(
                mobilePromotionService.getPromotions(
                    eq(1L), isNull(), isNull(), isNull(), eq(0), eq(20)
                )
            ).thenReturn(response)

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
            whenever(
                mobilePromotionService.getPromotions(
                    eq(1L), eq("2026-03-01"), eq("2026-03-31"), isNull(), eq(0), eq(20)
                )
            ).thenReturn(response)

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
            whenever(
                mobilePromotionService.getPromotions(
                    eq(1L), isNull(), isNull(), eq("봄맞이"), eq(0), eq(20)
                )
            ).thenReturn(response)

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
            whenever(
                mobilePromotionService.getPromotions(
                    eq(1L), eq("invalid-date"), isNull(), isNull(), eq(0), eq(20)
                )
            ).thenThrow(PromotionInvalidParameterException())

            mockMvc.perform(
                get("/api/v1/mobile/promotions")
                    .param("startDate", "invalid-date")
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
                    actualAmount = 800000L
                ),
                MobilePromotionEmployeeItem(
                    id = 11L,
                    employeeName = "김영희",
                    scheduleDate = LocalDate.of(2026, 3, 16),
                    workStatus = "근무",
                    workType3 = "순회",
                    targetAmount = 500000L,
                    actualAmount = null
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
            whenever(mobilePromotionService.getPromotion(eq(1L), eq(1L))).thenReturn(detail)

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
            whenever(mobilePromotionService.getPromotion(eq(1L), eq(999L)))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(get("/api/v1/mobile/promotions/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 접근 권한 없음")
        fun getPromotion_forbidden() {
            whenever(mobilePromotionService.getPromotion(eq(1L), eq(5L)))
                .thenThrow(PromotionForbiddenException())

            mockMvc.perform(get("/api/v1/mobile/promotions/5"))
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }
}
