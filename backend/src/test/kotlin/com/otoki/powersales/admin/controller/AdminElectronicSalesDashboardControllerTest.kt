package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardDetailResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardFilterOptionsResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.ElectronicSalesProductLookupItem
import com.otoki.powersales.domain.sales.service.ElectronicSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.ElectronicSalesDashboardExcelExporter
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
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
import java.time.LocalDate

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
    @DisplayName("GET /list - 거래처별 전산매출 페이징 응답 반환 (일 단위 기간 + 합계)")
    fun listHappyPath() {
        val response = ElectronicSalesDashboardListResponse(
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 20),
            totalSalesAmount = 5_000_000L,
            totalSalesQuantity = 1_200L,
            items = listOf(
                ElectronicSalesDashboardListItem(
                    accountId = 1, accountName = "거래처A",
                    sapAccountCode = "SAP1", branchCode = "1000", branchName = "서울지점",
                    salesAmount = 5_000_000L, salesQuantity = 1_200L,
                )
            ),
            pageInfo = ElectronicSalesDashboardListResponse.PageInfo(0, 20, 1L, 1),
        )
        every { queryService.getList(any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/list")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-20")
                .param("costCenterCodes", "1000")
                .param("distributionChannels", "02 슈퍼")
                .param("accountTypes", "6111 이마트")
                .param("productIds", "10,11")
                .param("category2", "면류")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.items[0].accountId").value(1))
            .andExpect(jsonPath("$.data.items[0].salesAmount").value(5000000))
            .andExpect(jsonPath("$.data.totalSalesAmount").value(5000000))
            .andExpect(jsonPath("$.data.totalSalesQuantity").value(1200))
            .andExpect(jsonPath("$.data.pageInfo.totalElements").value(1))

        verify {
            queryService.getList(
                any(),
                match { req ->
                    req.startDate == LocalDate.of(2026, 5, 1) &&
                        req.endDate == LocalDate.of(2026, 5, 20) &&
                        req.distributionChannels == listOf("02 슈퍼") &&
                        req.accountTypes == listOf("6111 이마트") &&
                        req.productIds == listOf(10L, 11L) &&
                        req.category2 == "면류"
                },
            )
        }
    }

    @Test
    @DisplayName("GET /list/export - 엑셀 헤더 Content-Disposition")
    fun listExport() {
        every { queryService.getListForExport(any(), any()) } returns emptyList()
        every { excelExporter.export(any(), any(), any()) } returns ExcelResult(
            bytes = byteArrayOf(1, 2, 3),
            filename = "electronic-sales-2026-05-01-2026-05-20.xlsx",
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/list/export")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-20")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("electronic-sales-2026-05-01-2026-05-20.xlsx")))
    }

    @Test
    @DisplayName("GET /detail/{customerId} - 제품별 상세 응답 (기간/제품 필터 전달)")
    fun detailHappyPath() {
        val response = ElectronicSalesDashboardDetailResponse(
            customerId = 1, customerName = "거래처A", sapAccountCode = "SAP1",
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 20),
            totalAmount = 5_000_000L, totalQuantity = 1_200L,
            items = listOf(
                ElectronicSalesDashboardDetailResponse.ProductSales(
                    productCode = "P1", productName = "라면", amount = 3_000_000L, quantity = 800L,
                )
            ),
        )
        every { queryService.getDetail(any(), any(), any(), any(), any(), any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/electronic/detail/1")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-20")
                .param("productIds", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.totalAmount").value(5000000))
            .andExpect(jsonPath("$.data.items[0].productName").value("라면"))

        verify {
            queryService.getDetail(
                any(), 1L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20),
                listOf(10L), null, null,
            )
        }
    }

    @Test
    @DisplayName("GET /filter-options - 유통형태/거래처유형/분류 옵션 응답")
    fun filterOptions() {
        every { queryService.getFilterOptions() } returns ElectronicSalesDashboardFilterOptionsResponse(
            distributionChannels = listOf("02 슈퍼"),
            accountTypes = listOf("6111 이마트"),
            categories = listOf(
                ElectronicSalesDashboardFilterOptionsResponse.CategoryGroup("면류", listOf("봉지면")),
            ),
            dependentAccountTypes = mapOf("02 슈퍼" to listOf("6111 이마트")),
        )

        mockMvc.perform(get("/api/v1/admin/sales/electronic/filter-options"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.distributionChannels[0]").value("02 슈퍼"))
            .andExpect(jsonPath("$.data.accountTypes[0]").value("6111 이마트"))
            .andExpect(jsonPath("$.data.categories[0].category2").value("면류"))
            .andExpect(jsonPath("$.data.categories[0].category3s[0]").value("봉지면"))
            .andExpect(jsonPath("$.data.dependentAccountTypes['02 슈퍼'][0]").value("6111 이마트"))
    }

    @Test
    @DisplayName("GET /product-lookup - 제품 검색 결과 응답")
    fun productLookup() {
        every { queryService.searchProducts("진라면") } returns listOf(
            ElectronicSalesProductLookupItem(productId = 10, name = "진라면", productCode = "P100", barcode = "880001"),
        )

        mockMvc.perform(get("/api/v1/admin/sales/electronic/product-lookup").param("keyword", "진라면"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data[0].productId").value(10))
            .andExpect(jsonPath("$.data[0].barcode").value("880001"))
    }
}
