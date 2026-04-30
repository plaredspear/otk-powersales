package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.request.HolidayMasterCreateRequest
import com.otoki.powersales.admin.dto.request.HolidayMasterUpdateRequest
import com.otoki.powersales.admin.dto.response.HolidayMasterResponse
import com.otoki.powersales.admin.service.AdminHolidayMasterService
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.leave.exception.HolidayDateDuplicateException
import com.otoki.powersales.leave.exception.HolidayNotFoundException
import com.otoki.powersales.leave.exception.InvalidHolidayTypeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(AdminHolidayMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminHolidayMasterController 테스트")
class AdminHolidayMasterControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminHolidayMasterService: AdminHolidayMasterService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/holiday-masters - 목록 조회")
    inner class GetHolidayMasters {

        @Test
        @DisplayName("성공 - 연도별 공휴일 목록 반환")
        fun getHolidayMasters_success() {
            val holidays = listOf(
                HolidayMasterResponse(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정", type = "법정공휴일"),
                HolidayMasterResponse(id = 2, holidayDate = LocalDate.of(2026, 3, 1), name = "삼일절", type = "법정공휴일")
            )
            whenever(adminHolidayMasterService.getHolidayMasters(2026)).thenReturn(holidays)

            mockMvc.perform(get("/api/v1/admin/holiday-masters").param("year", "2026"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].holiday_date").value("2026-01-01"))
                .andExpect(jsonPath("$.data[0].name").value("신정"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/holiday-masters - 등록")
    inner class CreateHolidayMaster {

        @Test
        @DisplayName("성공 - 공휴일 등록")
        fun create_success() {
            val request = HolidayMasterCreateRequest(
                holidayDate = LocalDate.of(2026, 8, 17),
                name = "임시공휴일",
                type = "임시공휴일"
            )
            val response = HolidayMasterResponse(id = 16, holidayDate = LocalDate.of(2026, 8, 17), name = "임시공휴일", type = "임시공휴일")
            whenever(adminHolidayMasterService.createHolidayMaster(any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(16))
                .andExpect(jsonPath("$.data.name").value("임시공휴일"))
        }

        @Test
        @DisplayName("실패 - 중복 날짜")
        fun create_duplicateDate() {
            whenever(adminHolidayMasterService.createHolidayMaster(any()))
                .thenThrow(HolidayDateDuplicateException())

            val json = """{"holiday_date": "2026-01-01", "name": "신정", "type": "법정공휴일"}"""
            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("HOLIDAY_DATE_DUPLICATE"))
        }

        @Test
        @DisplayName("실패 - 잘못된 유형")
        fun create_invalidType() {
            whenever(adminHolidayMasterService.createHolidayMaster(any()))
                .thenThrow(InvalidHolidayTypeException())

            val json = """{"holiday_date": "2026-08-17", "name": "기타", "type": "기타"}"""
            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_HOLIDAY_TYPE"))
        }

        @Test
        @DisplayName("실패 - name 누락")
        fun create_missingName() {
            val json = """{"holiday_date": "2026-08-17", "name": "", "type": "임시공휴일"}"""
            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/holiday-masters/{id} - 수정")
    inner class UpdateHolidayMaster {

        @Test
        @DisplayName("성공 - 공휴일 수정")
        fun update_success() {
            val response = HolidayMasterResponse(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정(수정)", type = "법정공휴일")
            whenever(adminHolidayMasterService.updateHolidayMaster(eq(1L), any())).thenReturn(response)

            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정(수정)",
                type = "법정공휴일"
            )
            mockMvc.perform(
                put("/api/v1/admin/holiday-masters/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("신정(수정)"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun update_notFound() {
            whenever(adminHolidayMasterService.updateHolidayMaster(eq(99999L), any()))
                .thenThrow(HolidayNotFoundException())

            val json = """{"holiday_date": "2026-01-01", "name": "신정", "type": "법정공휴일"}"""
            mockMvc.perform(
                put("/api/v1/admin/holiday-masters/99999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("HOLIDAY_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/holiday-masters/{id} - 삭제")
    inner class DeleteHolidayMaster {

        @Test
        @DisplayName("성공 - 공휴일 삭제")
        fun delete_success() {
            mockMvc.perform(delete("/api/v1/admin/holiday-masters/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun delete_notFound() {
            whenever(adminHolidayMasterService.deleteHolidayMaster(99999L))
                .thenThrow(HolidayNotFoundException())

            mockMvc.perform(delete("/api/v1/admin/holiday-masters/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("HOLIDAY_NOT_FOUND"))
        }
    }
}
