package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.domain.sales.service.PosSalesService
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPosSalesController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPosSalesController 테스트")
class AdminPosSalesControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var posSalesService: PosSalesService

    @BeforeEach
    fun setUpAdmin() {
        authenticateAsAdmin(role = null)
    }

    @Test
    @DisplayName("GET /api/v1/admin/sales/pos - 제품별 명세 조회")
    fun getPosSales_success() {
        every { posSalesService.getPosSales(1L, "202602") } returns PosSalesResponse(
            customerId = 1L,
            customerName = "사과마을",
            sapAccountCode = "12345",
            yearMonth = "202602",
            items = listOf(
                PosSalesResponse.ProductSales(
                    productCode = "01101123",
                    productName = "갈릭 아이올리소스 240g",
                    barcode = "8801045123456",
                    amount = 3500L,
                    quantity = 10L,
                ),
            ),
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/pos")
                .param("customerId", "1")
                .param("yearMonth", "202602"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.customerName").value("사과마을"))
            .andExpect(jsonPath("$.data.items[0].productCode").value("01101123"))
    }

    @Test
    @DisplayName("GET /api/v1/admin/sales/pos/export - Excel byte 응답 + Content-Disposition")
    fun exportPosSales_success() {
        every { posSalesService.exportPosSales(1L, "202602") } returns ExcelResult(
            bytes = ByteArray(800),
            filename = "POS매출_사과마을_202602.xlsx",
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/export")
                .param("customerId", "1")
                .param("yearMonth", "202602"),
        )
            .andExpect(status().isOk)
            .andExpect(
                header().string(
                    "Content-Type",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                )
            )
            .andExpect(header().exists("Content-Disposition"))

        verify { posSalesService.exportPosSales(1L, "202602") }
    }

    @Test
    @DisplayName("GET /api/v1/admin/sales/pos/export - yearMonth 형식 위반 시 400")
    fun exportPosSales_invalidYearMonth() {
        mockMvc.perform(
            get("/api/v1/admin/sales/pos/export")
                .param("customerId", "1")
                .param("yearMonth", "2026"),
        )
            .andExpect(status().isBadRequest)
    }
}
