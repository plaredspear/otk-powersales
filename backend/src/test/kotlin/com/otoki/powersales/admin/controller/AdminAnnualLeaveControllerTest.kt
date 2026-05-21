package com.otoki.powersales.admin.controller

import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.leave.dto.response.AnnualLeaveDayDto
import com.otoki.powersales.leave.dto.response.EmployeeAnnualLeaveDto
import com.otoki.powersales.leave.service.AdminAnnualLeaveService
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAnnualLeaveController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAnnualLeaveController 테스트")
class AdminAnnualLeaveControllerTest : AdminControllerTestSupport() {

    @MockkBean
    private lateinit var adminAnnualLeaveService: AdminAnnualLeaveService

    @Nested
    @DisplayName("GET /api/v1/admin/annual-leave/summary - 연차 현황 조회")
    inner class GetSummary {

        @Test
        @DisplayName("성공 - 유효한 yearMonth → 200 응답")
        fun validYearMonth_returns200() {
            val data = listOf(
                EmployeeAnnualLeaveDto(
                    employeeCode = "EMP001",
                    employeeName = "홍길동",
                    orgName = "서울1팀",
                    annualLeaveDays = listOf(
                        AnnualLeaveDayDto(date = "2026-03-05", attendTypeName = "연차"),
                        AnnualLeaveDayDto(date = "2026-03-10", attendTypeName = "연차")
                    ),
                    totalCount = 2
                )
            )
            every { adminAnnualLeaveService.getSummary(eq("2026-03"), null) } returns data

            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "2026-03")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data[0].employeeName").value("홍길동"))
                .andExpect(jsonPath("$.data[0].orgName").value("서울1팀"))
                .andExpect(jsonPath("$.data[0].annualLeaveDays[0].date").value("2026-03-05"))
                .andExpect(jsonPath("$.data[0].annualLeaveDays[0].attendTypeName").value("연차"))
                .andExpect(jsonPath("$.data[0].totalCount").value(2))
        }

        @Test
        @DisplayName("실패 - 잘못된 yearMonth 형식 '202603' → 400 응답")
        fun invalidYearMonthFormat_returns400() {
            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "202603")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
        }

        @Test
        @DisplayName("성공 - orgCode 파라미터 포함 → 200 응답")
        fun withOrgCode_returns200() {
            val data = listOf(
                EmployeeAnnualLeaveDto(
                    employeeCode = "EMP001",
                    employeeName = "홍길동",
                    orgName = "서울1팀",
                    annualLeaveDays = listOf(
                        AnnualLeaveDayDto(date = "2026-03-05", attendTypeName = "연차")
                    ),
                    totalCount = 1
                )
            )
            every { adminAnnualLeaveService.getSummary(eq("2026-03"), eq("서울1팀")) } returns data

            mockMvc.perform(
                get("/api/v1/admin/annual-leave/summary")
                    .param("yearMonth", "2026-03")
                    .param("orgCode", "서울1팀")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].employeeCode").value("EMP001"))
                .andExpect(jsonPath("$.data[0].orgName").value("서울1팀"))
                .andExpect(jsonPath("$.data[0].totalCount").value(1))
        }
    }
}
