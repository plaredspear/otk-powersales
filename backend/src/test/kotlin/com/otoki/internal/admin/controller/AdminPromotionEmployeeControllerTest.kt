package com.otoki.internal.admin.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.admin.dto.request.PromotionEmployeeRequest
import com.otoki.internal.admin.dto.response.PromotionEmployeeDetailResponse
import com.otoki.internal.admin.dto.response.PromotionEmployeeListResponse
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.admin.service.AdminPromotionConfirmService
import com.otoki.internal.admin.service.AdminPromotionEmployeeService
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.internal.promotion.exception.PromotionNotFoundException
import com.otoki.internal.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.time.LocalDateTime

@WebMvcTest(AdminPromotionEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionEmployeeController 테스트")
class AdminPromotionEmployeeControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @Autowired private lateinit var objectMapper: ObjectMapper

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
    @DisplayName("GET /api/v1/admin/promotions/{promotionId}/employees - 목록 조회")
    inner class GetEmployees {

        @Test
        @DisplayName("성공 - 조원 목록 반환")
        fun getEmployees_success() {
            val response = listOf(createListResponse())
            whenever(adminPromotionEmployeeService.getEmployees(10L)).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotions/10/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].employee_sfid").value("a0B5g00000XYZabc"))
                .andExpect(jsonPath("$.data[0].employee_name").value("김여사"))
                .andExpect(jsonPath("$.data[0].work_status").value("근무"))
        }

        @Test
        @DisplayName("실패 - 행사 미존재")
        fun getEmployees_promotionNotFound() {
            whenever(adminPromotionEmployeeService.getEmployees(999L))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(get("/api/v1/admin/promotions/999/employees"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotion-employees/{id} - 상세 조회")
    inner class GetEmployee {

        @Test
        @DisplayName("성공 - 조원 상세 반환")
        fun getEmployee_success() {
            val response = createDetailResponse()
            whenever(adminPromotionEmployeeService.getEmployee(1L)).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/promotion-employees/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.created_at").exists())
                .andExpect(jsonPath("$.data.updated_at").exists())
        }

        @Test
        @DisplayName("실패 - ID 미존재")
        fun getEmployee_notFound() {
            whenever(adminPromotionEmployeeService.getEmployee(999L))
                .thenThrow(PromotionEmployeeNotFoundException())

            mockMvc.perform(get("/api/v1/admin/promotion-employees/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/promotions/{promotionId}/employees - 등록")
    inner class CreateEmployee {

        @Test
        @DisplayName("성공 - 201 반환")
        fun createEmployee_success() {
            val response = createDetailResponse()
            whenever(adminPromotionEmployeeService.createEmployee(eq(10L), any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/promotions/10/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.promo_close_by_tm").value(false))
        }

        @Test
        @DisplayName("실패 - 행사 미존재")
        fun createEmployee_promotionNotFound() {
            whenever(adminPromotionEmployeeService.createEmployee(eq(999L), any()))
                .thenThrow(PromotionNotFoundException())

            mockMvc.perform(
                post("/api/v1/admin/promotions/999/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("실패 - 빈 Body로 추가 시 scheduleDate 필수 검증")
        fun createEmployee_emptyBody_scheduleDateRequired() {
            mockMvc.perform(
                post("/api/v1/admin/promotions/10/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("성공 - 투입일 포함, 나머지 필드 없이 추가")
        fun createEmployee_scheduleDateOnly_success() {
            val response = createDetailResponse()
            whenever(adminPromotionEmployeeService.createEmployee(eq(10L), any())).thenReturn(response)

            val partialJson = """{"schedule_date":"2026-03-15"}"""

            mockMvc.perform(
                post("/api/v1/admin/promotions/10/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(partialJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/promotion-employees/{id} - 수정")
    inner class UpdateEmployee {

        @Test
        @DisplayName("성공 - 200 반환")
        fun updateEmployee_success() {
            val response = createDetailResponse()
            whenever(adminPromotionEmployeeService.updateEmployee(eq(1L), eq(1L), any())).thenReturn(response)

            mockMvc.perform(
                put("/api/v1/admin/promotion-employees/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
        }

        @Test
        @DisplayName("실패 - ID 미존재")
        fun updateEmployee_notFound() {
            whenever(adminPromotionEmployeeService.updateEmployee(eq(999L), eq(1L), any()))
                .thenThrow(PromotionEmployeeNotFoundException())

            mockMvc.perform(
                put("/api/v1/admin/promotion-employees/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isNotFound)
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/promotion-employees/{id} - 삭제")
    inner class DeleteEmployee {

        @Test
        @DisplayName("성공 - 200 반환")
        fun deleteEmployee_success() {
            doNothing().whenever(adminPromotionEmployeeService).deleteEmployee(1L)

            mockMvc.perform(delete("/api/v1/admin/promotion-employees/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - ID 미존재")
        fun deleteEmployee_notFound() {
            whenever(adminPromotionEmployeeService.deleteEmployee(999L))
                .thenThrow(PromotionEmployeeNotFoundException())

            mockMvc.perform(delete("/api/v1/admin/promotion-employees/999"))
                .andExpect(status().isNotFound)
        }
    }

    // --- Helper methods ---

    private fun createListResponse() = PromotionEmployeeListResponse(
        id = 1L,
        promotionId = 10L,
        employeeSfid = "a0B5g00000XYZabc",
        employeeName = "김여사",
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "시식",
        workType3 = "고정",
        workType4 = "냉장",
        professionalPromotionTeam = "라면세일조",
        scheduleId = null,
        promoCloseByTm = false,
        basePrice = 1500,
        dailyTargetCount = 100,
        targetAmount = 100000,
        actualAmount = 80000,
        primaryProductAmount = null,
        primarySalesQuantity = null,
        primarySalesPrice = null,
        otherSalesAmount = null,
        otherSalesQuantity = null,
        s3ImageUniqueKey = null
    )

    private fun createDetailResponse() = PromotionEmployeeDetailResponse(
        id = 1L,
        promotionId = 10L,
        employeeSfid = "a0B5g00000XYZabc",
        employeeName = "김여사",
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "시식",
        workType3 = "고정",
        workType4 = "냉장",
        professionalPromotionTeam = "라면세일조",
        scheduleId = null,
        promoCloseByTm = false,
        basePrice = 1500,
        dailyTargetCount = 100,
        targetAmount = 100000,
        actualAmount = 80000,
        primaryProductAmount = null,
        primarySalesQuantity = null,
        primarySalesPrice = null,
        otherSalesAmount = null,
        otherSalesQuantity = null,
        s3ImageUniqueKey = null,
        createdAt = LocalDateTime.of(2026, 3, 10, 10, 0, 0),
        updatedAt = LocalDateTime.of(2026, 3, 10, 10, 0, 0)
    )

    private fun createRequest() = PromotionEmployeeRequest(
        employeeSfid = "a0B5g00000XYZabc",
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "시식",
        workType3 = "고정",
        workType4 = "냉장",
        professionalPromotionTeam = "라면세일조",
        basePrice = 1500,
        dailyTargetCount = 100
    )
}
