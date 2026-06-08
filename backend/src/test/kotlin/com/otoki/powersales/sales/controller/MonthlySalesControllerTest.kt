package com.otoki.powersales.sales.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.sales.dto.response.ElectronicSalesResponse
import com.otoki.powersales.sales.dto.response.MonthlySalesResponse
import com.otoki.powersales.sales.dto.response.PosSalesResponse
import com.otoki.powersales.sales.service.ElectronicSalesService
import com.otoki.powersales.sales.service.LogisticsSalesService
import com.otoki.powersales.sales.service.MonthlySalesService
import com.otoki.powersales.sales.service.PosSalesService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(MonthlySalesController::class)
@AutoConfigureMockMvc(addFilters = false)
class MonthlySalesControllerTest : MobileControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var monthlySalesService: MonthlySalesService

    // MonthlySalesController 가 생성자 주입받는 의존성 — @WebMvcTest 컨텍스트 로딩에 필요.
    @MockkBean
    private lateinit var logisticsSalesService: LogisticsSalesService

    @MockkBean
    private lateinit var electronicSalesService: ElectronicSalesService

    @MockkBean
    private lateinit var posSalesService: PosSalesService

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

        every { monthlySalesService.getMonthlySales(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/sales/monthly")
                .param("yearMonth", "202602")
                .param("customerId", "C001")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customerId").value("C001"))
            .andExpect(jsonPath("$.data.customerName").value("GS리테일"))
            .andExpect(jsonPath("$.data.yearMonth").value("202602"))
            .andExpect(jsonPath("$.data.targetAmount").value(28900000))
            .andExpect(jsonPath("$.data.achievedAmount").value(30089000))
            .andExpect(jsonPath("$.data.categorySales[0].category").value("상온"))
            .andExpect(jsonPath("$.data.yearComparison.currentYear").value(30089000))
            .andExpect(jsonPath("$.data.monthlyAverage.currentYearAverage").value(28500000))
    }

    @Test
    @DisplayName("월매출 조회 - yearMonth 필수 누락 시 400 BAD REQUEST")
    fun getMonthlySales_missingYearMonth() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/sales/monthly")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    @DisplayName("월매출 조회 - 잘못된 yearMonth 형식 시 400 BAD REQUEST")
    fun getMonthlySales_invalidYearMonthFormat() {
        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/sales/monthly")
                .param("yearMonth", "2026-02")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    // ========== getElectronicSales Tests ==========

    @Test
    @DisplayName("전산매출 조회 - 200 OK (제품별 실적)")
    fun getElectronicSales_success() {
        val mockResponse = ElectronicSalesResponse(
            customerId = 1,
            customerName = "사과마을",
            sapAccountCode = "12345",
            yearMonth = "202602",
            items = listOf(
                ElectronicSalesResponse.ProductSales(
                    productCode = "01101123",
                    productName = "갈릭 아이올리소스 240g",
                    amount = 3500,
                    quantity = 10,
                ),
            ),
        )

        every { electronicSalesService.getElectronicSales(1, "202602") } returns mockResponse

        mockMvc.perform(
            get("/api/v1/mobile/sales/electronic")
                .param("customerId", "1")
                .param("yearMonth", "202602")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.sapAccountCode").value("12345"))
            .andExpect(jsonPath("$.data.items[0].productCode").value("01101123"))
            .andExpect(jsonPath("$.data.items[0].amount").value(3500))
            .andExpect(jsonPath("$.data.items[0].quantity").value(10))
    }

    @Test
    @DisplayName("전산매출 조회 - 잘못된 yearMonth 형식 시 400 BAD REQUEST")
    fun getElectronicSales_invalidYearMonth() {
        mockMvc.perform(
            get("/api/v1/mobile/sales/electronic")
                .param("customerId", "1")
                .param("yearMonth", "2026-02")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isBadRequest)
    }

    // ========== getPosSales Tests ==========

    @Test
    @DisplayName("POS매출 조회 - 200 OK (제품별 실적)")
    fun getPosSales_success() {
        val mockResponse = PosSalesResponse(
            customerId = 1,
            customerName = "이마트 강남점",
            sapAccountCode = "12345",
            yearMonth = "202602",
            items = listOf(
                PosSalesResponse.ProductSales(
                    productCode = "01101123",
                    productName = "진라면 매운맛 120g",
                    barcode = "8801234567890",
                    amount = 45000,
                    quantity = 30,
                ),
            ),
        )

        every { posSalesService.getPosSales(1, "202602") } returns mockResponse

        mockMvc.perform(
            get("/api/v1/mobile/sales/pos")
                .param("customerId", "1")
                .param("yearMonth", "202602")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.customerName").value("이마트 강남점"))
            .andExpect(jsonPath("$.data.items[0].productCode").value("01101123"))
            .andExpect(jsonPath("$.data.items[0].barcode").value("8801234567890"))
            .andExpect(jsonPath("$.data.items[0].amount").value(45000))
            .andExpect(jsonPath("$.data.items[0].quantity").value(30))
    }

    @Test
    @DisplayName("POS매출 조회 - 잘못된 yearMonth 형식 시 400 BAD REQUEST")
    fun getPosSales_invalidYearMonth() {
        mockMvc.perform(
            get("/api/v1/mobile/sales/pos")
                .param("customerId", "1")
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

        every { monthlySalesService.getMonthlySales(any()) } returns mockResponse

        // When & Then
        mockMvc.perform(
            get("/api/v1/mobile/sales/monthly")
                .param("yearMonth", "202602")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customerId").value("ALL"))
    }
}
