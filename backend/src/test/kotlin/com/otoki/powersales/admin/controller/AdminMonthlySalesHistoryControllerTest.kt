package com.otoki.powersales.admin.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesHistoryListItem
import com.otoki.powersales.domain.sales.dto.response.MonthlySalesHistoryListResponse
import com.otoki.powersales.domain.sales.service.AdminMonthlySalesHistoryService
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

@WebMvcTest(AdminMonthlySalesHistoryController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminMonthlySalesHistoryController 테스트")
class AdminMonthlySalesHistoryControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminMonthlySalesHistoryService: AdminMonthlySalesHistoryService

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
    @DisplayName("GET /api/v1/admin/monthly-sales-histories - ORORA 월매출 조회")
    inner class GetMonthlySalesHistories {

        @Test
        @DisplayName("성공 - 거래처 + 매출년월 적재 행과 합계 반환")
        fun success() {
            every { adminMonthlySalesHistoryService.getMonthlySalesHistories(any(), "1000000", "202607") } returns
                MonthlySalesHistoryListResponse(
                    salesMonth = "202607",
                    sapAccountCode = "1000000",
                    accountName = "GS25 역삼점",
                    branchName = "강남지점",
                    content = listOf(
                        MonthlySalesHistoryListItem(
                            id = 1,
                            salesYear = "2026",
                            salesMonth = "07",
                            sapAccountCode = "1000000",
                            externalKey = "1000000202607",
                            abcClosingAmount1 = 5000.0,
                            abcClosingAmount2 = 3000.0,
                            abcClosingAmount3 = 2000.0,
                            abcClosingAmount4 = 2000.0,
                            abcClosingSumAmount = 12000.0,
                            shipClosingAmount1 = 1000.0,
                            shipClosingAmount2 = 900.0,
                            shipClosingAmount3 = 800.0,
                            shipClosingAmount4 = 700.0,
                            shipClosingSumAmount = 3400.0,
                            isDeleted = false,
                            createdAt = LocalDateTime.of(2026, 8, 9, 11, 30),
                            updatedAt = LocalDateTime.of(2026, 8, 9, 11, 30),
                        ),
                    ),
                    totalAbcClosingAmount = 12000.0,
                    totalShipClosingAmount = 3400.0,
                    lastMaterializedAt = LocalDateTime.of(2026, 8, 9, 11, 30),
                )

            mockMvc.perform(
                get("/api/v1/admin/monthly-sales-histories")
                    .param("accountCode", "1000000")
                    .param("salesMonth", "202607"),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accountName").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].salesYear").value("2026"))
                .andExpect(jsonPath("$.data.content[0].salesMonth").value("07"))
                .andExpect(jsonPath("$.data.content[0].abcClosingAmount1").value(5000.0))
                .andExpect(jsonPath("$.data.content[0].shipClosingSumAmount").value(3400.0))
                // Jackson 이 Boolean 의 `is` prefix 를 떼지 않도록 @get:JsonProperty 로 고정한 필드명.
                // `deleted` 로 나가면 web 의 isDeleted 가 항상 undefined 라 삭제 표시가 죽는다.
                .andExpect(jsonPath("$.data.content[0].isDeleted").value(false))
                .andExpect(jsonPath("$.data.totalAbcClosingAmount").value(12000.0))
                .andExpect(jsonPath("$.data.lastMaterializedAt").value("2026-08-09T11:30:00"))
        }

        @Test
        @DisplayName("실패 - 거래처 부재 시 404 + ACCOUNT_NOT_FOUND")
        fun accountNotFound() {
            every { adminMonthlySalesHistoryService.getMonthlySalesHistories(any(), any(), any()) } throws
                BusinessException(
                    errorCode = "ACCOUNT_NOT_FOUND",
                    message = "거래처를 찾을 수 없습니다: 9999999",
                    httpStatus = HttpStatus.NOT_FOUND,
                )

            mockMvc.perform(
                get("/api/v1/admin/monthly-sales-histories")
                    .param("accountCode", "9999999")
                    .param("salesMonth", "202607"),
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 거래처코드 파라미터 누락 시 400")
        fun missingAccountCode() {
            mockMvc.perform(get("/api/v1/admin/monthly-sales-histories").param("salesMonth", "202607"))
                .andExpect(status().isBadRequest)
        }
    }
}
