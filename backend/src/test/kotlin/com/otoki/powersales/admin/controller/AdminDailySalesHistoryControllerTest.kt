package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.sales.dto.response.DailySalesHistoryListItem
import com.otoki.powersales.domain.sales.dto.response.DailySalesHistoryListResponse
import com.otoki.powersales.domain.sales.service.AdminDailySalesHistoryService
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpStatus
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminDailySalesHistoryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminDailySalesHistoryController 테스트")
class AdminDailySalesHistoryControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminDailySalesHistoryService: AdminDailySalesHistoryService

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    @MockkBean private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            firstArg<MethodParameter>().hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/daily-sales-histories - ORORA 일매출 조회")
    inner class GetDailySalesHistories {

        @Test
        @DisplayName("성공 - 거래처 + 매출월 일별 행과 합계 반환")
        fun success() {
            every { adminDailySalesHistoryService.getDailySalesHistories(any(), "1000000", "202607") } returns
                DailySalesHistoryListResponse(
                    salesMonth = "202607",
                    sapAccountCode = "1000000",
                    accountName = "GS25 역삼점",
                    branchName = "강남지점",
                    content = listOf(
                        DailySalesHistoryListItem(
                            id = 1,
                            salesDate = "20260731",
                            sapAccountCode = "1000000",
                            externalKey = "100000020260731",
                            erpSalesAmount = 1000.0,
                            erpDistributionAmount = 200.0,
                            ledgerAmount = null,
                            createdAt = LocalDateTime.of(2026, 7, 31, 11, 0),
                            updatedAt = LocalDateTime.of(2026, 7, 31, 11, 0),
                        ),
                    ),
                    totalErpSalesAmount = 1000.0,
                    totalErpDistributionAmount = 200.0,
                    totalLedgerAmount = 0.0,
                    lastMaterializedAt = LocalDateTime.of(2026, 7, 31, 11, 0),
                )

            mockMvc.perform(
                get("/api/v1/admin/daily-sales-histories")
                    .param("accountCode", "1000000")
                    .param("salesMonth", "202607"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountName").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].salesDate").value("20260731"))
                .andExpect(jsonPath("$.data.totalErpSalesAmount").value(1000.0))
                .andExpect(jsonPath("$.data.lastMaterializedAt").value("2026-07-31T11:00:00"))
        }

        @Test
        @DisplayName("실패 - 거래처 부재 시 404 + ACCOUNT_NOT_FOUND")
        fun accountNotFound() {
            every { adminDailySalesHistoryService.getDailySalesHistories(any(), any(), any()) } throws
                BusinessException(
                    errorCode = "ACCOUNT_NOT_FOUND",
                    message = "거래처를 찾을 수 없습니다: 9999999",
                    httpStatus = HttpStatus.NOT_FOUND,
                )

            mockMvc.perform(
                get("/api/v1/admin/daily-sales-histories")
                    .param("accountCode", "9999999")
                    .param("salesMonth", "202607"),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 거래처코드 파라미터 누락 시 400")
        fun missingAccountCode() {
            mockMvc.perform(get("/api/v1/admin/daily-sales-histories").param("salesMonth", "202607"))
                .andExpect(status().isBadRequest)
        }
    }
}
