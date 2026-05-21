package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.leave.dto.request.HolidayMasterCreateRequest
import com.otoki.powersales.leave.dto.request.HolidayMasterUpdateRequest
import com.otoki.powersales.leave.dto.response.HolidayMasterResponse
import com.otoki.powersales.leave.service.AdminHolidayMasterService
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.auth.entity.UserRoleEnum
import com.otoki.powersales.leave.exception.HolidayDateDuplicateException
import com.otoki.powersales.leave.exception.HolidayNotFoundException
import com.otoki.powersales.leave.exception.InvalidHolidayTypeException
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import java.time.LocalDate

@WebMvcTest(AdminHolidayMasterController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminHolidayMasterController 테스트")
class AdminHolidayMasterControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminHolidayMasterService: AdminHolidayMasterService
    @MockkBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockkBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockkBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = 1L,
            role = UserRoleEnum.BRANCH_MANAGER,
            costCenterCode = null,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            permissions = emptySet(),
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
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
                HolidayMasterResponse(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정", type = "공휴일"),
                HolidayMasterResponse(id = 2, holidayDate = LocalDate.of(2026, 3, 1), name = "삼일절", type = "공휴일")
            )
            every { adminHolidayMasterService.getHolidayMasters(2026) } returns holidays

            mockMvc.perform(get("/api/v1/admin/holiday-masters").param("year", "2026"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].holidayDate").value("2026-01-01"))
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
                type = "기타"
            )
            val response = HolidayMasterResponse(id = 16, holidayDate = LocalDate.of(2026, 8, 17), name = "임시공휴일", type = "기타")
            every { adminHolidayMasterService.createHolidayMaster(any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.data.id").value(16))
                .andExpect(jsonPath("$.data.name").value("임시공휴일"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminHolidayMasterControllerTest#createExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun create_exceptions(@Suppress("UNUSED_PARAMETER") name: String, exception: Throwable, expectedCode: String) {
            every { adminHolidayMasterService.createHolidayMaster(any()) } throws exception

            val json = """{"holidayDate": "2026-01-01", "name": "신정", "type": "공휴일"}"""
            mockMvc.perform(
                post("/api/v1/admin/holiday-masters")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }

        @Test
        @DisplayName("실패 - name 누락")
        fun create_missingName() {
            val json = """{"holidayDate": "2026-08-17", "name": "", "type": "기타"}"""
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
            val response = HolidayMasterResponse(id = 1, holidayDate = LocalDate.of(2026, 1, 1), name = "신정(수정)", type = "공휴일")
            every { adminHolidayMasterService.updateHolidayMaster(eq(1L), any()) } returns response

            val request = HolidayMasterUpdateRequest(
                holidayDate = LocalDate.of(2026, 1, 1),
                name = "신정(수정)",
                type = "공휴일"
            )
            mockMvc.perform(
                put("/api/v1/admin/holiday-masters/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.name").value("신정(수정)"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun update_notFound() {
            every { adminHolidayMasterService.updateHolidayMaster(eq(99999L), any()) } throws HolidayNotFoundException()

            val json = """{"holidayDate": "2026-01-01", "name": "신정", "type": "공휴일"}"""
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
            every { adminHolidayMasterService.deleteHolidayMaster(any()) } just Runs

            mockMvc.perform(delete("/api/v1/admin/holiday-masters/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun delete_notFound() {
            every { adminHolidayMasterService.deleteHolidayMaster(99999L) } throws HolidayNotFoundException()

            mockMvc.perform(delete("/api/v1/admin/holiday-masters/99999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("HOLIDAY_NOT_FOUND"))
        }
    }

    companion object {
        @JvmStatic
        fun createExceptions(): List<Arguments> = listOf(
            Arguments.of("duplicateDate -> HOLIDAY_DATE_DUPLICATE", HolidayDateDuplicateException(), "HOLIDAY_DATE_DUPLICATE"),
            Arguments.of("invalidType -> INVALID_HOLIDAY_TYPE", InvalidHolidayTypeException(), "INVALID_HOLIDAY_TYPE"),
        )
    }
}
