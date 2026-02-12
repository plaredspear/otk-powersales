package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.response.DailySalesListResponse
import com.otoki.internal.dto.response.EventDetailResponse
import com.otoki.internal.dto.response.EventListResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.EventNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.EventService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

/**
 * EventController 테스트
 */
@WebMvcTest(EventController::class)
@AutoConfigureMockMvc(addFilters = false)
class EventControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var eventService: EventService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== getEvents Tests ==========

    @Test
    @DisplayName("행사 목록 조회 - 200 OK")
    fun getEvents_success() {
        // Given
        val mockResponse = EventListResponse(
            content = listOf(
                EventListResponse.EventInfo(
                    eventId = "EVT001",
                    eventType = "[시식]",
                    eventName = "상온(오뚜기카레_매운맛100G)",
                    startDate = "2026-02-10",
                    endDate = "2026-02-28",
                    customerId = "C001",
                    customerName = "GS리테일"
                )
            ),
            page = 0,
            size = 10,
            totalElements = 1,
            totalPages = 1
        )

        whenever(eventService.getEvents(eq(1L), any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/events")
                .param("page", "0")
                .param("size", "10")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.content[0].event_id").value("EVT001"))
            .andExpect(jsonPath("$.data.content[0].event_type").value("[시식]"))
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(10))
            .andExpect(jsonPath("$.data.total_elements").value(1))
    }

    @Test
    @DisplayName("행사 목록 조회 - 거래처 필터")
    fun getEvents_withCustomerFilter() {
        // Given
        val mockResponse = EventListResponse(
            content = emptyList(),
            page = 0,
            size = 10,
            totalElements = 0,
            totalPages = 0
        )

        whenever(eventService.getEvents(eq(1L), any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/events")
                .param("customerId", "C001")
                .param("date", "2026-02-12")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    // ========== getEventDetail Tests ==========

    @Test
    @DisplayName("행사 상세 조회 - 200 OK")
    fun getEventDetail_success() {
        // Given
        val mockResponse = EventDetailResponse(
            event = EventDetailResponse.EventInfo(
                eventId = "EVT001",
                eventType = "[시식]",
                eventName = "상온(오뚜기카레_매운맛100G)",
                startDate = "2026-02-10",
                endDate = "2026-02-28",
                customerId = "C001",
                customerName = "GS리테일",
                assigneeId = "EMP001"
            ),
            salesInfo = EventDetailResponse.SalesInfo(
                targetAmount = 5160000,
                achievedAmount = 500000,
                achievementRate = 10.0,
                progressRate = 8.0
            ),
            products = EventDetailResponse.ProductsInfo(
                mainProduct = EventDetailResponse.ProductInfo(
                    productCode = "10010003",
                    productName = "오뚜기카레_매운맛100G",
                    isMainProduct = true
                ),
                subProducts = listOf(
                    EventDetailResponse.ProductInfo(
                        productCode = "10010004",
                        productName = "오뚜기카레_순한맛100G",
                        isMainProduct = false
                    )
                )
            ),
            isTodayRegistered = false,
            canRegisterToday = true
        )

        whenever(eventService.getEventDetail(eq(1L), eq("EVT001"))).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/events/EVT001")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.event.event_id").value("EVT001"))
            .andExpect(jsonPath("$.data.sales_info.target_amount").value(5160000))
            .andExpect(jsonPath("$.data.products.main_product.product_code").value("10010003"))
            .andExpect(jsonPath("$.data.can_register_today").value(true))
    }

    @Test
    @DisplayName("행사 상세 조회 - 404 NOT FOUND")
    fun getEventDetail_notFound() {
        // Given
        whenever(eventService.getEventDetail(eq(1L), eq("EVT999")))
            .thenThrow(EventNotFoundException())

        // When & Then
        mockMvc.perform(
            get("/api/v1/events/EVT999")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("EVENT_NOT_FOUND"))
    }

    // ========== getDailySales Tests ==========

    @Test
    @DisplayName("일별 매출 목록 조회 - 200 OK")
    fun getDailySales_success() {
        // Given
        val mockResponse = DailySalesListResponse(
            dailySales = emptyList()
        )

        whenever(eventService.getDailySales(eq(1L), eq("EVT001"))).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/events/EVT001/daily-sales")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.daily_sales").isEmpty)
    }

    @Test
    @DisplayName("일별 매출 목록 조회 - 404 NOT FOUND")
    fun getDailySales_notFound() {
        // Given
        whenever(eventService.getDailySales(eq(1L), eq("EVT999")))
            .thenThrow(EventNotFoundException())

        // When & Then
        mockMvc.perform(
            get("/api/v1/events/EVT999/daily-sales")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("EVENT_NOT_FOUND"))
    }
}
