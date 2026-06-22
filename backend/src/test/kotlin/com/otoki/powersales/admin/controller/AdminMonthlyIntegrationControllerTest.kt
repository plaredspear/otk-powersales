package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.domain.activity.schedule.service.AdminMonthlyIntegrationService
import com.otoki.powersales.domain.activity.schedule.service.InvalidParameterException
import com.otoki.powersales.platform.common.util.excel.ExcelResult
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.activity.schedule.dto.response.CategoryScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.CategoryScheduleResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleItem
import com.otoki.powersales.domain.activity.schedule.dto.response.MonthlyIntegrationScheduleResponse
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.math.BigDecimal

@WebMvcTest(AdminMonthlyIntegrationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminMonthlyIntegrationController 테스트")
class AdminMonthlyIntegrationControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminMonthlyIntegrationService: AdminMonthlyIntegrationService

    @Nested
    @DisplayName("GET /api/v1/admin/schedules/monthly-integration - 통합일정 조회")
    inner class GetMonthlyIntegration {

        @Test
        @DisplayName("성공 - 통합일정 조회")
        fun success() {
            val response = MonthlyIntegrationScheduleResponse(
                year = 2026, month = 3,
                items = listOf(
                    MonthlyIntegrationScheduleItem(
                        branchName = "서울1지점",
                        accountBranchName = "강남지점",
                        accountCode = "1234567",
                        accountName = "이마트 강남점",
                        employeeCode = "200001",
                        title = null,
                        employeeName = "김영희",
                        workingCategory1 = "진열",
                        workingCategory3 = "고정",
                        workingCategory4 = null,
                        workingCategory5 = "상시",
                        totalInputCount = 22,
                        equivalentWorkingDays = BigDecimal("11.000"),
                        convertedHeadcount = BigDecimal("0.500"),
                        avgClosingAmount = 15000000
                    )
                ),
                totalCount = 1
            )
            every { adminMonthlyIntegrationService.getMonthlyIntegration(eq(2026), eq(3), any(), any()) } returns response

            mockMvc.perform(
                get("/api/v1/admin/schedules/monthly-integration")
                    .param("year", "2026")
                    .param("month", "3")
                    .param("costCenterCodes", "CC001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.year").value(2026))
                .andExpect(jsonPath("$.data.month").value(3))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.items[0].branchName").value("서울1지점"))
                .andExpect(jsonPath("$.data.items[0].employeeCode").value("200001"))
                .andExpect(jsonPath("$.data.items[0].totalInputCount").value(22))
        }

        @Test
        @DisplayName("성공 - 사번/이름 검색어(keyword) 가 서비스로 전달됨")
        fun passesKeyword() {
            val response = MonthlyIntegrationScheduleResponse(
                year = 2026, month = 3, items = emptyList(), totalCount = 0
            )
            every {
                adminMonthlyIntegrationService.getMonthlyIntegration(eq(2026), eq(3), any(), eq("김영희"))
            } returns response

            mockMvc.perform(
                get("/api/v1/admin/schedules/monthly-integration")
                    .param("year", "2026")
                    .param("month", "3")
                    .param("costCenterCodes", "CC001")
                    .param("keyword", "김영희")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            io.mockk.verify {
                adminMonthlyIntegrationService.getMonthlyIntegration(2026, 3, any(), "김영희")
            }
        }

        @Test
        @DisplayName("실패 - year 범위 초과")
        fun invalidYear() {
            every { adminMonthlyIntegrationService.getMonthlyIntegration(eq(1999), eq(3), any(), any()) } throws InvalidParameterException("year는 2020~2099 범위여야 합니다")

            mockMvc.perform(
                get("/api/v1/admin/schedules/monthly-integration")
                    .param("year", "1999")
                    .param("month", "3")
                    .param("costCenterCodes", "CC001")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedules/monthly-integration/export - 엑셀 다운로드")
    inner class ExportMonthlyIntegration {

        @Test
        @DisplayName("성공 - 엑셀 파일 반환")
        fun success() {
            val excelResult = ExcelResult(
                bytes = byteArrayOf(0x50, 0x4B), // dummy xlsx header
                filename = "2026년3월_여사원 통합일정 조회_20260322_120000.xlsx"
            )
            every { adminMonthlyIntegrationService.exportMonthlyIntegration(eq(2026), eq(3), any(), any()) } returns excelResult

            mockMvc.perform(
                get("/api/v1/admin/schedules/monthly-integration/export")
                    .param("year", "2026")
                    .param("month", "3")
                    .param("costCenterCodes", "CC001")
            )
                .andExpect(status().isOk)
                .andExpect(header().string("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/schedules/monthly-integration/category - 근무형태별 여사원인원현황")
    inner class GetCategorySchedule {

        @Test
        @DisplayName("성공 - 카테고리 조회")
        fun success() {
            val response = CategoryScheduleResponse(
                year = 2026, month = 3,
                items = listOf(
                    CategoryScheduleItem(
                        branchName = "서울1지점",
                        currentMonthTotal = BigDecimal("25.5"),
                        previousMonthTotal = BigDecimal("24.0"),
                        totalChange = BigDecimal("1.5"),
                        displayFixed = BigDecimal("8.000"),
                        displayAlternate = BigDecimal("4.000"),
                        displayPatrol = BigDecimal("3.000"),
                        currentMonthDisplayTotal = BigDecimal("15.000"),
                        previousMonthDisplayTotal = BigDecimal("14.500"),
                        displayChange = BigDecimal("0.500"),
                        eventAmbient = BigDecimal("6.000"),
                        eventFrozenChilled = BigDecimal("4.500"),
                        currentMonthEventTotal = BigDecimal("10.500"),
                        previousMonthEventTotal = BigDecimal("9.500"),
                        eventChange = BigDecimal("1.000")
                    )
                )
            )
            every { adminMonthlyIntegrationService.getCategorySchedule(eq(2026), eq(3), any()) } returns response

            mockMvc.perform(
                get("/api/v1/admin/schedules/monthly-integration/category")
                    .param("year", "2026")
                    .param("month", "3")
                    .param("costCenterCodes", "CC001")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].branchName").value("서울1지점"))
                .andExpect(jsonPath("$.data.items[0].currentMonthTotal").value(25.5))
        }
    }
}
