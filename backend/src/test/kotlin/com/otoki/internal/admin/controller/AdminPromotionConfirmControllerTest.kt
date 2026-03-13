package com.otoki.internal.admin.controller

import com.otoki.internal.admin.dto.response.PromotionConfirmResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminPromotionConfirmService
import com.otoki.internal.admin.service.AdminPromotionEmployeeService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.promotion.exception.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminPromotionEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionEmployeeController - 행사 확정 테스트")
class AdminPromotionConfirmControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var adminPromotionEmployeeService: AdminPromotionEmployeeService
    @MockitoBean private lateinit var adminPromotionConfirmService: AdminPromotionConfirmService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter
    @MockitoBean private lateinit var dataScopeHolder: DataScopeHolder

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.ADMIN)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

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
            whenever(adminPromotionConfirmService.confirmPromotion(10L)).thenReturn(response)

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.promotion_id").value(10))
                .andExpect(jsonPath("$.data.total_employees").value(3))
                .andExpect(jsonPath("$.data.upserted_schedules").value(3))
        }

        @Test
        @DisplayName("실패 - 행사 미존재 -> 404")
        fun confirm_notFound() {
            whenever(adminPromotionConfirmService.confirmPromotion(999L))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(post("/api/v1/admin/promotions/999/confirm"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PROMOTION_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 조원 0명 -> 400")
        fun confirm_noEmployees() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(NoEmployeesException())

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("NO_EMPLOYEES"))
        }

        @Test
        @DisplayName("실패 - 필수값 누락 -> 400")
        fun confirm_valuesRequired() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(ValuesRequiredException("김철수의 필수 항목을 입력하세요 (work_type1)"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("VALUES_REQUIRED"))
        }

        @Test
        @DisplayName("실패 - 투입일 범위 초과 -> 400")
        fun confirm_dateOutOfRange() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(DateOutOfRangeException("김철수의 투입일이 행사 기간을 벗어납니다"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("DATE_OUT_OF_RANGE"))
        }

        @Test
        @DisplayName("실패 - 근무유형3 수량 초과 -> 400")
        fun confirm_workType3Limit() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(WorkType3LimitExceededException("초과"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("WORK_TYPE3_LIMIT_EXCEEDED"))
        }

        @Test
        @DisplayName("실패 - 연차/대휴 충돌 -> 400")
        fun confirm_leaveConflict() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(LeaveConflictException("충돌"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("LEAVE_CONFLICT"))
        }

        @Test
        @DisplayName("실패 - 거래처 중복 -> 400")
        fun confirm_duplicateSchedule() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(DuplicateScheduleException("중복"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_SCHEDULE"))
        }

        @Test
        @DisplayName("실패 - 여사원 휴직 -> 400")
        fun confirm_employeeOnLeave() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(EmployeeOnLeaveException("휴직"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_ON_LEAVE"))
        }

        @Test
        @DisplayName("실패 - 여사원 퇴직 -> 400")
        fun confirm_employeeResigned() {
            whenever(adminPromotionConfirmService.confirmPromotion(10L))
                .thenThrow(EmployeeResignedException("퇴직"))

            mockMvc.perform(post("/api/v1/admin/promotions/10/confirm"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("EMPLOYEE_RESIGNED"))
        }
    }
}
