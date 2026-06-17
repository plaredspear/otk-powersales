package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListResponse
import com.otoki.powersales.domain.sales.service.ElectronicSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.ElectronicSalesDashboardExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminElectronicSalesDashboardController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminElectronicSalesDashboardController 테스트")
class AdminElectronicSalesDashboardControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var queryService: ElectronicSalesAdminQueryService
    @MockkBean private lateinit var excelExporter: ElectronicSalesDashboardExcelExporter

    @MockkBean private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUpSystemAdminAndArgResolver() {
        authenticateAsAdmin(role = null)
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Test
    @DisplayName("GET /list - 거래처별 전산매출 페이징 응답 반환")
    fun listHappyPath() {
        val response = ElectronicSalesDashboardListResponse(
            items = listOf(
                ElectronicSalesDashboardListItem(
                    accountId = 1, accountName = "거래처A",
                    sapAccountCode = "SAP1", branchCode = "1000", branchName = "서울지점",
                    salesYear = 2026, salesMonth = 5,
                    salesAmount = 5_000_000L, salesQuantity = 1_200L,
                )
            ),
            pageInfo = ElectronicSalesDashboardListResponse.PageInfo(0, 20, 1L, 1),
        )
        every { queryService.getList(any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/list")
                .param("year", "2026")
                .param("month", "5")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].accountId").value(1))
            .andExpect(jsonPath("$.data.items[0].salesAmount").value(5000000))
            .andExpect(jsonPath("$.data.items[0].salesQuantity").value(1200))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))
    }

    @Test
    @DisplayName("GET /list/export - 엑셀 헤더 Content-Disposition")
    fun listExport() {
        every { queryService.getListForExport(any(), any()) } returns emptyList()
        every { excelExporter.export(any(), any(), any()) } returns ExcelResult(
            bytes = byteArrayOf(1, 2, 3),
            filename = "electronic-sales-2026-05.xlsx",
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/list/export")
                .param("year", "2026")
                .param("month", "5")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("electronic-sales-2026-05.xlsx")))
    }

    @Test
    @DisplayName("GET /detail/{customerId} - 제품별 상세 응답")
    fun detailHappyPath() {
        val response = ElectronicSalesDashboardDetailResponse(
            customerId = 1, customerName = "거래처A", sapAccountCode = "SAP1",
            salesYear = 2026, salesMonth = 5,
            totalAmount = 5_000_000L, totalQuantity = 1_200L,
            items = listOf(
                ElectronicSalesDashboardDetailResponse.ProductSales(
                    productCode = "P1", productName = "라면", amount = 3_000_000L, quantity = 800L,
                )
            ),
        )
        every { queryService.getDetail(any(), any(), any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/detail/1")
                .param("year", "2026")
                .param("month", "5")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.totalAmount").value(5000000))
            .andExpect(jsonPath("$.data.items[0].productName").value("라면"))
    }
}
