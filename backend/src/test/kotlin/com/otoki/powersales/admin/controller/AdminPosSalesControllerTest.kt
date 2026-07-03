package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListItem
import com.otoki.powersales.domain.sales.dto.response.PosSalesDashboardListResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesRangeResponse
import com.otoki.powersales.domain.sales.dto.response.PosSalesResponse
import com.otoki.powersales.domain.sales.service.PosSalesAdminQueryService
import com.otoki.powersales.domain.sales.service.PosSalesDashboardExcelExporter
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

@WebMvcTest(AdminPosSalesController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPosSalesController 테스트")
class AdminPosSalesControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var queryService: PosSalesAdminQueryService
    @MockkBean private lateinit var excelExporter: PosSalesDashboardExcelExporter

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
    @DisplayName("GET /list - 거래처별 POS매출 페이징 응답 반환 (일 단위 기간 + 합계 + 필터 전달)")
    fun listHappyPath() {
        val response = PosSalesDashboardListResponse(
            startDate = LocalDate.of(2026, 5, 1),
            endDate = LocalDate.of(2026, 5, 20),
            totalSalesAmount = 5_000_000L,
            totalSalesQuantity = 1_200L,
            items = listOf(
                PosSalesDashboardListItem(
                    accountId = 1, accountName = "거래처A",
                    sapAccountCode = "SAP1", branchCode = "1000", branchName = "서울지점",
                    salesAmount = 5_000_000L, salesQuantity = 1_200L,
                )
            ),
            pageInfo = PosSalesDashboardListResponse.PageInfo(0, 20, 1L, 1),
        )
        every { queryService.getList(any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/list")
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
            filename = "pos-sales-2026-05-01-2026-05-20.xlsx",
        )

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/list/export")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-20")
                .param("costCenterCodes", "1000")
        )
            .andExpect(status().isOk)
            .andExpect(content().contentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("pos-sales-2026-05-01-2026-05-20.xlsx")))
    }

    @Test
    @DisplayName("GET /detail/{customerId} - 제품별 상세 응답 (기간/제품 필터 전달, 바코드 포함)")
    fun detailHappyPath() {
        val response = PosSalesRangeResponse(
            customerId = 1, customerName = "거래처A", sapAccountCode = "SAP1",
            startDate = "2026-05-01",
            endDate = "2026-05-20",
            totalAmount = 5_000_000L, totalQuantity = 1_200L,
            items = listOf(
                PosSalesResponse.ProductSales(
                    productCode = "P1", productName = "라면", barcode = "880001",
                    amount = 3_000_000L, quantity = 800L,
                )
            ),
        )
        every { queryService.getDetail(any(), any(), any(), any(), any(), any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/detail/1")
                .param("startDate", "2026-05-01")
                .param("endDate", "2026-05-20")
                .param("productIds", "10")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.data.customerId").value(1))
            .andExpect(jsonPath("$.data.totalAmount").value(5000000))
            .andExpect(jsonPath("$.data.items[0].productName").value("라면"))
            .andExpect(jsonPath("$.data.items[0].barcode").value("880001"))

        verify {
            queryService.getDetail(
                any(), 1L,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 20),
                listOf(10L), null, null,
            )
        }
    }

    @Test
    @DisplayName("GET /list - 날짜 형식 위반 시 400")
    fun listInvalidDate() {
        mockMvc.perform(
            get("/api/v1/admin/sales/pos/list")
                .param("startDate", "2026-05")
                .param("endDate", "2026-05-20")
                .param("costCenterCodes", "1000"),
        )
            .andExpect(status().isBadRequest)
    }
}
