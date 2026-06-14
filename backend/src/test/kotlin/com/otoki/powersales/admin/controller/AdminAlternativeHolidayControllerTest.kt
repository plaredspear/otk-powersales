package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.leave.dto.request.AlternativeHolidayCreateRequest
import com.otoki.powersales.leave.dto.request.AlternativeHolidayRejectRequest
import com.otoki.powersales.leave.dto.response.AlternativeHolidayApproveResponse
import com.otoki.powersales.leave.dto.response.AlternativeHolidayCreateResponse
import com.otoki.powersales.leave.dto.response.AlternativeHolidayListItem
import com.otoki.powersales.leave.dto.response.AlternativeHolidayRejectResponse
import com.otoki.powersales.leave.service.AdminAlternativeHolidayService
import com.otoki.powersales.leave.exception.*
import com.otoki.powersales.leave.enums.AltHolidayStatus
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(AdminAlternativeHolidayController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAlternativeHolidayController 테스트")
class AdminAlternativeHolidayControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockkBean private lateinit var adminAlternativeHolidayService: AdminAlternativeHolidayService

    @Nested
    @DisplayName("GET /api/v1/admin/alternative-holidays - 목록 조회")
    inner class GetList {

        @Test
        @DisplayName("성공 - 기간별 대체휴무 목록 반환")
        fun getList_success() {
            val items = listOf(
                AlternativeHolidayListItem(
                    id = 1, employeeCode = "12345678", employeeName = "홍길동", orgName = "서울1팀",
                    actualWorkDate = LocalDate.of(2026, 3, 7),
                    targetAltHolidayDate = LocalDate.of(2026, 3, 9),
                    confirmAltHolidayDate = null, status = AltHolidayStatus.NEW, changeReason = null,
                    createdByEmpNo = "admin01", createdAt = java.time.LocalDateTime.of(2026, 3, 8, 10, 0)
                )
            )
            every { adminAlternativeHolidayService.getAlternativeHolidays(
                any(), any(), any(), any(), any()
            ) } returns items

            mockMvc.perform(
                get("/api/v1/admin/alternative-holidays")
                    .param("startDate", "2026-03-01")
                    .param("endDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data[0].employeeCode").value("12345678"))
                .andExpect(jsonPath("$.data[0].orgName").value("서울1팀"))
                .andExpect(jsonPath("$.data[0].status").value("신규"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/alternative-holidays - 신청")
    inner class Create {

        @Test
        @DisplayName("성공 - 대체휴무 신청")
        fun create_success() {
            val response = AlternativeHolidayCreateResponse(id = 1, status = "신규")
            every { adminAlternativeHolidayService.createAlternativeHoliday(any(), eq(1L)) } returns response

            val request = AlternativeHolidayCreateRequest(
                employeeCode = "12345678",
                actualWorkDate = LocalDate.of(2026, 3, 7),
                targetAltHolidayDate = LocalDate.of(2026, 3, 9)
            )
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("신규"))
        }

        @Test
        @DisplayName("실패 - 신청일이 공휴일")
        fun create_holidayError() {
            every { adminAlternativeHolidayService.createAlternativeHoliday(any(), eq(1L)) } throws AltHolidayConfirmDateIsHolidayException()

            val json = """{"employeeCode": "12345678", "actualWorkDate": "2026-03-07", "targetAltHolidayDate": "2026-01-01"}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ALT_HOLIDAY_CONFIRM_DATE_IS_HOLIDAY"))
        }

        @Test
        @DisplayName("실패 - 사번 누락")
        fun create_missingEmployeeCode() {
            val json = """{"employeeCode": "", "actualWorkDate": "2026-03-07", "targetAltHolidayDate": "2026-03-09"}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/alternative-holidays/{id}/approve - 승인")
    inner class Approve {

        @Test
        @DisplayName("성공 - 대체휴무 승인")
        fun approve_success() {
            val response = AlternativeHolidayApproveResponse(
                id = 1, status = "승인", confirmAltHolidayDate = LocalDate.of(2026, 3, 9)
            )
            every { adminAlternativeHolidayService.approveAlternativeHoliday(eq(1L), any()) } returns response

            val json = """{"confirmAltHolidayDate": "2026-03-09"}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays/1/approve")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("승인"))
                .andExpect(jsonPath("$.data.confirmAltHolidayDate").value("2026-03-09"))
        }

        @Test
        @DisplayName("실패 - 잘못된 상태")
        fun approve_invalidStatus() {
            every { adminAlternativeHolidayService.approveAlternativeHoliday(eq(1L), any()) } throws AltHolidayInvalidStatusException()

            val json = """{}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays/1/approve")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ALT_HOLIDAY_INVALID_STATUS"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/alternative-holidays/{id}/reject - 반려")
    inner class Reject {

        @Test
        @DisplayName("성공 - 대체휴무 반려")
        fun reject_success() {
            val response = AlternativeHolidayRejectResponse(id = 1, status = "반려")
            every { adminAlternativeHolidayService.rejectAlternativeHoliday(eq(1L), any()) } returns response

            val request = AlternativeHolidayRejectRequest(changeReason = "인력 부족")
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays/1/reject")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.status").value("반려"))
        }

        @Test
        @DisplayName("실패 - 사유 누락")
        fun reject_noReason() {
            val json = """{"changeReason": ""}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays/1/reject")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun reject_notFound() {
            every { adminAlternativeHolidayService.rejectAlternativeHoliday(eq(99999L), any()) } throws AltHolidayNotFoundException()

            val json = """{"changeReason": "테스트"}"""
            mockMvc.perform(
                post("/api/v1/admin/alternative-holidays/99999/reject")
                    .contentType(MediaType.APPLICATION_JSON).content(json)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ALT_HOLIDAY_NOT_FOUND"))
        }
    }
}
