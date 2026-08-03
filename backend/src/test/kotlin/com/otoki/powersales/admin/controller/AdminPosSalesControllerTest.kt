package com.otoki.powersales.admin.controller

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.SelectorBranchResult
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.sales.dto.response.PosSalesAccountItem
import com.otoki.powersales.domain.sales.dto.response.PosSalesAccountListResponse
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
    @MockkBean private lateinit var branchScopeGateway: com.otoki.powersales.admin.service.BranchScopeGateway

    @MockkBean private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun setUpSystemAdminAndArgResolver() {
        authenticateAsAdmin(role = null)
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)

        // 지점 스코프 게이트웨이 — 조회 코드는 선택값 그대로, DataScope 는 손대지 않는 pass-through stub.
        every { branchScopeGateway.applyDataScope(any(), any()) } answers { secondArg() }
        every { branchScopeGateway.resolveQueryCodes(any(), any(), any()) } answers { secondArg() }
        // 거래처 → 지점 역산 — 입력 코드가 곧 셀렉터 코드인 단순 stub.
        every { branchScopeGateway.resolveSelectorBranches(any(), any(), any()) } answers {
            thirdArg<Collection<String?>>()
                .filterNotNull()
                .associateWith { SelectorBranchResult.Resolved(it, "지점$it") }
        }
    }

    @Test
    @DisplayName("GET /accounts - 1단 거래처 목록 반환 (POS 미접촉, 거래처 필터 전달)")
    fun accountsHappyPath() {
        val response = PosSalesAccountListResponse(
            totalElements = 1,
            items = listOf(
                PosSalesAccountItem(
                    accountId = 1, accountName = "거래처A",
                    sapAccountCode = "SAP1",
                    distributionChannel = "01 대형마트(3대)", accountType = "6111 이마트",
                    branchCode = "1000", branchName = "서울지점",
                ),
            ),
        )
        every { queryService.getAccounts(any(), any()) } returns response

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/accounts")
                .param("costCenterCodes", "1000")
                .param("distributionChannels", "02 슈퍼")
                .param("accountTypes", "6111 이마트")
                .param("customerKeyword", "거래처"),
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].accountId").value(1))
            .andExpect(jsonPath("$.data.items[0].sapAccountCode").value("SAP1"))
            .andExpect(jsonPath("$.data.items[0].distributionChannel").value("01 대형마트(3대)"))
            .andExpect(jsonPath("$.data.items[0].accountType").value("6111 이마트"))
            // 지점 역산 결과 — 화면이 지점 체크박스를 자동으로 채우는 근거
            .andExpect(jsonPath("$.data.items[0].selectorBranchCode").value("1000"))
            .andExpect(jsonPath("$.data.items[0].selectorBranchStatus").value("RESOLVED"))

        verify {
            queryService.getAccounts(
                any(),
                match { req ->
                    req.costCenterCodes == listOf("1000") &&
                        req.distributionChannels == listOf("02 슈퍼") &&
                        req.accountTypes == listOf("6111 이마트") &&
                        req.customerKeyword == "거래처"
                },
            )
        }
    }

    @Test
    @DisplayName("GET /accounts - 지점 미선택 + 거래처명만으로 검색 가능 (지점 선행 강제 완화)")
    fun accountsWithoutBranchSelection() {
        every { queryService.getAccounts(any(), any()) } returns PosSalesAccountListResponse(0, emptyList())

        mockMvc.perform(
            get("/api/v1/admin/sales/pos/accounts")
                .param("customerKeyword", "이마트"),
        )
            .andExpect(status().isOk)

        // 지점 미선택은 빈 목록 그대로 전달 — resolveQueryCodes 로 화이트리스트를 채워 넣지 않는다.
        verify { queryService.getAccounts(any(), match { it.costCenterCodes.isEmpty() }) }
        verify(exactly = 0) { branchScopeGateway.resolveQueryCodes(any(), any(), any()) }
    }

    @Test
    @DisplayName("GET /list - 선택 거래처 POS매출 페이징 응답 반환 (일 단위 기간 + 합계 + accountIds/제품 필터 전달)")
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
                .param("accountIds", "1,2")
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
                        req.accountIds == listOf(1L, 2L) &&
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
                .param("accountIds", "1,2")
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
                .param("accountIds", "1"),
        )
            .andExpect(status().isBadRequest)
    }
}
