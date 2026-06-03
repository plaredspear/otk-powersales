package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.common.test.AdminControllerTestSupport
import com.otoki.powersales.promotion.dto.request.PromotionEmployeeRequest
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeDetailResponse
import com.otoki.powersales.promotion.dto.response.PromotionEmployeeListResponse
import com.otoki.powersales.promotion.service.AdminPromotionConfirmService
import com.otoki.powersales.promotion.service.AdminPromotionEmployeeService
import com.otoki.powersales.promotion.exception.PromotionEmployeeNotFoundException
import com.otoki.powersales.promotion.exception.PromotionNotFoundException
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import io.mockk.every
import io.mockk.just
import io.mockk.Runs
import org.junit.jupiter.api.BeforeEach
import org.springframework.core.MethodParameter
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDate
import java.math.BigDecimal

@WebMvcTest(AdminPromotionEmployeeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminPromotionEmployeeController 테스트")
class AdminPromotionEmployeeControllerTest : AdminControllerTestSupport() {

    @Autowired private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminPromotionEmployeeService: AdminPromotionEmployeeService
    @MockkBean private lateinit var adminPromotionConfirmService: AdminPromotionConfirmService

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns
            DataScope(branchCodes = emptyList(), isAllBranches = true)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/promotions/{promotionId}/employees - 목록 조회")
    inner class GetEmployees {

        @Test
        @DisplayName("성공 - 조원 목록 반환")
        fun getEmployees_success() {
            val response = listOf(createListResponse())
            every { adminPromotionEmployeeService.getEmployees(any(), eq(10L)) } returns response

            mockMvc.perform(get("/api/v1/admin/promotions/10/employees"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].employeeCode").value("20030117"))
                .andExpect(jsonPath("$.data[0].employeeName").value("김여사"))
                .andExpect(jsonPath("$.data[0].workStatus").value("근무"))
        }

        @Test
        @DisplayName("실패 - 행사 미존재")
        fun getEmployees_promotionNotFound() {
            every { adminPromotionEmployeeService.getEmployees(any(), eq(999L)) } throws PromotionNotFoundException()

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
            every { adminPromotionEmployeeService.getEmployee(1L) } returns response

            mockMvc.perform(get("/api/v1/admin/promotion-employees/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.createdAt").exists())
                .andExpect(jsonPath("$.data.updatedAt").exists())
        }

        @Test
        @DisplayName("실패 - ID 미존재")
        fun getEmployee_notFound() {
            every { adminPromotionEmployeeService.getEmployee(999L) } throws PromotionEmployeeNotFoundException()

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
            every { adminPromotionEmployeeService.createEmployee(eq(10L), any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/promotions/10/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.promoCloseByTm").value(false))
        }

        @Test
        @DisplayName("실패 - 행사 미존재")
        fun createEmployee_promotionNotFound() {
            every { adminPromotionEmployeeService.createEmployee(eq(999L), any()) } throws PromotionNotFoundException()

            mockMvc.perform(
                post("/api/v1/admin/promotions/999/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(createRequest()))
            )
                .andExpect(status().isNotFound)
        }

        @Test
        @DisplayName("성공 - 빈 Body로 추가 시 scheduleDate null로 생성")
        fun createEmployee_emptyBody_success() {
            val response = createDetailResponse()
            every { adminPromotionEmployeeService.createEmployee(eq(10L), any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/promotions/10/employees")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{}")
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("성공 - 투입일 포함, 나머지 필드 없이 추가")
        fun createEmployee_scheduleDateOnly_success() {
            val response = createDetailResponse()
            every { adminPromotionEmployeeService.createEmployee(eq(10L), any()) } returns response

            val partialJson = """{"scheduleDate":"2026-03-15"}"""

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
            every { adminPromotionEmployeeService.updateEmployee(any(), eq(1L), eq(1L), any()) } returns response

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
            every { adminPromotionEmployeeService.updateEmployee(any(), eq(999L), eq(1L), any()) } throws PromotionEmployeeNotFoundException()

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
            every { adminPromotionEmployeeService.deleteEmployee(any(), 1L) } just Runs

            mockMvc.perform(delete("/api/v1/admin/promotion-employees/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }

        @Test
        @DisplayName("실패 - ID 미존재")
        fun deleteEmployee_notFound() {
            every { adminPromotionEmployeeService.deleteEmployee(any(), 999L) } throws PromotionEmployeeNotFoundException()

            mockMvc.perform(delete("/api/v1/admin/promotion-employees/999"))
                .andExpect(status().isNotFound)
        }
    }

    // --- Helper methods ---

    private fun createListResponse() = PromotionEmployeeListResponse(
        id = 1L,
        name = "PE00000001",
        promotionId = 10L,
        employeeId = 100L,
        employeeCode = "20030117",
        employeeName = "김여사",
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "행사",
        workType3 = "고정",
        scheduleId = null,
        promoCloseByTm = false,
        basePrice = BigDecimal.valueOf(1500L),
        dailyTargetCount = BigDecimal.valueOf(100L),
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
        employeeId = 100L,
        employeeCode = "20030117",
        employeeName = "김여사",
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "행사",
        workType3 = "고정",
        scheduleId = null,
        promoCloseByTm = false,
        basePrice = BigDecimal.valueOf(1500L),
        dailyTargetCount = BigDecimal.valueOf(100L),
        targetAmount = 100000,
        actualAmount = 80000,
        primaryProductAmount = null,
        primarySalesQuantity = null,
        primarySalesPrice = null,
        otherSalesAmount = null,
        otherSalesQuantity = null,
        s3ImageUniqueKey = null,
        createdAt = java.time.LocalDateTime.of(2026, 3, 10, 10, 0, 0),
        updatedAt = java.time.LocalDateTime.of(2026, 3, 10, 10, 0, 0)
    )

    private fun createRequest() = PromotionEmployeeRequest(
        employeeId = 100L,
        scheduleDate = LocalDate.of(2026, 3, 15),
        workStatus = "근무",
        workType1 = "행사",
        workType3 = "고정",
        basePrice = BigDecimal.valueOf(1500L),
        dailyTargetCount = BigDecimal.valueOf(100L)
    )
}
