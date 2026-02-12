package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.response.MonthlySalesResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.MonthlySalesService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
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
 * MonthlySalesController 테스트
 */
@WebMvcTest(MonthlySalesController::class)
@AutoConfigureMockMvc(addFilters = false)
class MonthlySalesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var monthlySalesService: MonthlySalesService

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

    // ========== getMonthlySales Tests ==========

    @Test
    @DisplayName("월매출 조회 - 200 OK")
    fun getMonthlySales_success() {
        // Given
        val mockResponse = MonthlySalesResponse(
            customerId = "C001",
            customerName = "GS리테일",
            yearMonth = "202602",
            targetAmount = 28900000,
            achievedAmount = 30089000,
            achievementRate = 104.11,
            categorySales = listOf(
                MonthlySalesResponse.CategorySalesInfo(
                    category = "상온",
                    targetAmount = 43000000,
                    achievedAmount = 60050000,
                    achievementRate = 139.65
                ),
                MonthlySalesResponse.CategorySalesInfo(
                    category = "냉장/냉동",
                    targetAmount = 15000000,
                    achievedAmount = 12000000,
                    achievementRate = 80.00
                )
            ),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(
                currentYear = 30089000,
                previousYear = 25000000
            ),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(
                currentYearAverage = 28500000,
                previousYearAverage = 24000000,
                startMonth = 1,
                endMonth = 2
            )
        )

        whenever(monthlySalesService.getMonthlySales(any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/sales/monthly")
                .param("yearMonth", "202602")
                .param("customerId", "C001")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customer_id").value("C001"))
            .andExpect(jsonPath("$.data.customer_name").value("GS리테일"))
            .andExpect(jsonPath("$.data.year_month").value("202602"))
            .andExpect(jsonPath("$.data.target_amount").value(28900000))
            .andExpect(jsonPath("$.data.achieved_amount").value(30089000))
            .andExpect(jsonPath("$.data.category_sales[0].category").value("상온"))
            .andExpect(jsonPath("$.data.year_comparison.current_year").value(30089000))
            .andExpect(jsonPath("$.data.monthly_average.current_year_average").value(28500000))
    }

    @Test
    @DisplayName("월매출 조회 - yearMonth 필수 누락 시 400 BAD REQUEST")
    fun getMonthlySales_missingYearMonth() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/sales/monthly")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("월매출 조회 - 잘못된 yearMonth 형식 시 400 BAD REQUEST")
    fun getMonthlySales_invalidYearMonthFormat() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/sales/monthly")
                .param("yearMonth", "2026-02")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("월매출 조회 - customerId 없이 전체 조회")
    fun getMonthlySales_withoutCustomerId() {
        // Given
        val mockResponse = MonthlySalesResponse(
            customerId = "ALL",
            customerName = "ALL",
            yearMonth = "202602",
            targetAmount = 100000000,
            achievedAmount = 120000000,
            achievementRate = 120.0,
            categorySales = emptyList(),
            yearComparison = MonthlySalesResponse.YearComparisonInfo(0, 0),
            monthlyAverage = MonthlySalesResponse.MonthlyAverageInfo(0, 0, 1, 2)
        )

        whenever(monthlySalesService.getMonthlySales(any())).thenReturn(mockResponse)

        // When & Then
        mockMvc.perform(
            get("/api/v1/sales/monthly")
                .param("yearMonth", "202602")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customer_id").value("ALL"))
    }
}
