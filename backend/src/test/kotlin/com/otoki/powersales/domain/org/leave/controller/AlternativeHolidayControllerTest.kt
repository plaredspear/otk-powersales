package com.otoki.powersales.domain.org.leave.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.platform.common.test.MobileControllerTestSupport
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayCreateResponse
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayListItemResponse
import com.otoki.powersales.domain.org.leave.dto.AlternativeHolidayRequest
import com.otoki.powersales.domain.org.leave.exception.AltHolidayConfirmDateIsWeekendException
import com.otoki.powersales.domain.org.leave.exception.AltHolidayDuplicateException
import com.otoki.powersales.domain.org.leave.service.AlternativeHolidayService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate

@WebMvcTest(AlternativeHolidayController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AlternativeHolidayController 테스트")
class AlternativeHolidayControllerTest : MobileControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper
    @MockkBean private lateinit var alternativeHolidayService: AlternativeHolidayService

    @Nested
    @DisplayName("POST /api/v1/mobile/alternative-holidays - 대체휴무 신청")
    inner class CreateTests {

        @Test
        @DisplayName("성공 - 대체휴무 신청")
        fun create_success() {
            val response = AlternativeHolidayCreateResponse(
                id = 1L,
                actualWorkDate = LocalDate.of(2026, 3, 7),
                targetAltHolidayDate = LocalDate.of(2026, 3, 9),
                status = "신규",
                createdAt = java.time.LocalDateTime.of(2026, 3, 9, 10, 30)
            )
            every { alternativeHolidayService.createAlternativeHoliday(1L, any(), any()) } returns response

            val request = AlternativeHolidayRequest(
                actualWorkDate = LocalDate.of(2026, 3, 7),
                targetAltHolidayDate = LocalDate.of(2026, 3, 9)
            )

            mockMvc.perform(
                post("/api/v1/mobile/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("신규"))
                .andExpect(jsonPath("$.data.actualWorkDate").value("2026-03-07"))
                .andExpect(jsonPath("$.data.targetAltHolidayDate").value("2026-03-09"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.domain.org.leave.controller.AlternativeHolidayControllerTest#createExceptionCases")
        @DisplayName("실패 - service 예외 → 400 ErrorCode 매핑")
        fun create_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: RuntimeException,
            expectedCode: String,
        ) {
            every { alternativeHolidayService.createAlternativeHoliday(1L, any(), any()) } throws exception

            val request = AlternativeHolidayRequest(
                actualWorkDate = LocalDate.of(2026, 3, 7),
                targetAltHolidayDate = LocalDate.of(2026, 3, 9)
            )

            mockMvc.perform(
                post("/api/v1/mobile/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        fun create_missingFields() {
            val invalidJson = """{"actualWorkDate": null}"""

            mockMvc.perform(
                post("/api/v1/mobile/alternative-holidays")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/alternative-holidays - 대체휴무 이력 조회")
    inner class GetTests {

        @Test
        @DisplayName("성공 - 기간 지정 조회")
        fun get_withDateRange() {
            val items = listOf(
                AlternativeHolidayListItemResponse(
                    id = 1L,
                    actualWorkDate = LocalDate.of(2026, 3, 7),
                    targetAltHolidayDate = LocalDate.of(2026, 3, 9),
                    confirmAltHolidayDate = LocalDate.of(2026, 3, 9),
                    status = "승인",
                    changeReason = null,
                    createdAt = java.time.LocalDateTime.of(2026, 3, 9, 10, 30)
                )
            )
            every { alternativeHolidayService.getAlternativeHolidays(1L, any(), any()) } returns items

            mockMvc.perform(
                get("/api/v1/mobile/alternative-holidays")
                    .param("startDate", "2026-01-01")
                    .param("endDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].status").value("승인"))
                .andExpect(jsonPath("$.data[0].confirmAltHolidayDate").value("2026-03-09"))
        }

        @Test
        @DisplayName("성공 - 기간 미지정 조회")
        fun get_defaultDateRange() {
            every { alternativeHolidayService.getAlternativeHolidays(1L, any(), any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/mobile/alternative-holidays"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
        }
    }

    companion object {
        @JvmStatic
        fun createExceptionCases(): List<Arguments> = listOf(
            Arguments.of("신청일 주말 -> ALT_HOLIDAY_CONFIRM_DATE_IS_WEEKEND", AltHolidayConfirmDateIsWeekendException(), "ALT_HOLIDAY_CONFIRM_DATE_IS_WEEKEND"),
            Arguments.of("중복 신청 -> ALT_HOLIDAY_DUPLICATE", AltHolidayDuplicateException(), "ALT_HOLIDAY_DUPLICATE"),
        )
    }
}
