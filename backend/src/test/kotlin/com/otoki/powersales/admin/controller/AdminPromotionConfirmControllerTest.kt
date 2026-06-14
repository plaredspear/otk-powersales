package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import com.otoki.powersales.promotion.dto.response.PromotionConfirmResponse
import com.otoki.powersales.promotion.service.AdminPromotionConfirmService
import com.otoki.powersales.promotion.service.AdminPromotionEmployeeService
import com.otoki.powersales.promotion.exception.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import io.mockk.every
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPromotionEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionEmployeeController - 행사 확정 테스트")
class AdminPromotionConfirmControllerTest : AdminControllerTestSupport() {

    @MockkBean private lateinit var adminPromotionEmployeeService: AdminPromotionEmployeeService
    @MockkBean private lateinit var adminPromotionConfirmService: AdminPromotionConfirmService

    @Nested
    @DisplayName("POST /api/v1/admin/promotions/{promotionId}/confirm - 행사 확정")
    inner class ConfirmPromotion {

        @Test
        @DisplayName("성공 - 행사 확정")
        fun confirm_success() {
            val response = PromotionConfirmResponse(
                promotionId = 10L,
                totalEmployees = 3,
                upsertedTeamMemberSchedules = 3
            )
            every { adminPromotionConfirmService.confirmPromotion(10L) } returns response

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.promotionId").value(10))
                .andExpect(jsonPath("$.data.totalEmployees").value(3))
                .andExpect(jsonPath("$.data.upsertedSchedules").value(3))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminPromotionConfirmControllerTest#confirmExceptions")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun confirm_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            promotionId: Long,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { adminPromotionConfirmService.confirmPromotion(promotionId) } throws exception

            mockMvc.perform(post("/api/v1/admin/promotions/$promotionId/confirm"))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun confirmExceptions(): List<Arguments> = listOf(
            Arguments.of("notFound -> 404 PROMOTION_NOT_FOUND", 999L, PromotionNotFoundException(), 404, "PROMOTION_NOT_FOUND"),
            Arguments.of("noEmployees -> 400 NO_EMPLOYEES", 10L, NoEmployeesException(), 400, "NO_EMPLOYEES"),
            Arguments.of(
                "valuesRequired -> 400 VALUES_REQUIRED",
                10L,
                ValuesRequiredException("김철수의 필수 항목을 입력하세요 (근무유형1)"),
                400,
                "VALUES_REQUIRED",
            ),
            Arguments.of(
                "dateOutOfRange -> 400 DATE_OUT_OF_RANGE",
                10L,
                DateOutOfRangeException("김철수의 투입일이 행사 기간을 벗어납니다"),
                400,
                "DATE_OUT_OF_RANGE",
            ),
            Arguments.of(
                "workType3Limit -> 400 WORK_TYPE3_LIMIT_EXCEEDED",
                10L,
                WorkType3LimitExceededException("초과"),
                400,
                "WORK_TYPE3_LIMIT_EXCEEDED",
            ),
            Arguments.of("leaveConflict -> 400 LEAVE_CONFLICT", 10L, LeaveConflictException("충돌"), 400, "LEAVE_CONFLICT"),
            Arguments.of(
                "duplicateSchedule -> 400 DUPLICATE_SCHEDULE",
                10L,
                DuplicateScheduleException("중복"),
                400,
                "DUPLICATE_SCHEDULE",
            ),
            Arguments.of("employeeOnLeave -> 400 EMPLOYEE_ON_LEAVE", 10L, EmployeeOnLeaveException("휴직"), 400, "EMPLOYEE_ON_LEAVE"),
            Arguments.of("employeeResigned -> 400 EMPLOYEE_RESIGNED", 10L, EmployeeResignedException("퇴직"), 400, "EMPLOYEE_RESIGNED"),
        )
    }
}
