package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.admin.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.powersales.admin.dto.response.AdminProductExpirationListResponse
import com.otoki.powersales.admin.dto.response.AdminProductExpirationResponse
import com.otoki.powersales.admin.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.admin.service.AdminProductExpirationService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.sap.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminProductExpirationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminProductExpirationController 테스트")
class AdminProductExpirationControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminProductExpirationService: AdminProductExpirationService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private fun setUpPrincipal(userId: Long = 1L, role: UserRole = UserRole.USER) {
        val principal = UserPrincipal(userId = userId, role = role)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @BeforeEach
    fun setUp() {
        setUpPrincipal()
    }

    private fun sampleResponse(id: Int = 1) = AdminProductExpirationResponse(
        id = id,
        seq = 1,
        productName = "진라면",
        productCode = "P001",
        accountName = "이마트 강남점",
        accountCode = "A001",
        employeeName = "홍길동",
        employeeCode = "E001",
        expirationDate = "2026-05-01",
        alarmDate = "2026-04-24",
        dDay = 26,
        status = "NORMAL",
        description = "테스트 설명",
        createdAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0),
        updatedAt = LocalDateTime.of(2026, 4, 1, 10, 0, 0)
    )

    @Nested
    @DisplayName("GET /api/v1/admin/product-expiration - 목록 조회")
    inner class GetList {

        @Test
        @DisplayName("성공 - 기본 목록 조회")
        fun getList_success() {
            val response = AdminProductExpirationListResponse(
                content = listOf(sampleResponse()),
                page = 0,
                size = 20,
                totalElements = 1L,
                totalPages = 1
            )
            whenever(adminProductExpirationService.getList(eq(1L), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), any()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/product-expiration"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].product_name").value("진라면"))
                .andExpect(jsonPath("$.data.content[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.content[0].account_name").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.content[0].employee_name").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].expiration_date").value("2026-05-01"))
                .andExpect(jsonPath("$.data.content[0].alarm_date").value("2026-04-24"))
                .andExpect(jsonPath("$.data.content[0].d_day").value(26))
                .andExpect(jsonPath("$.data.content[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.data.total_elements").value(1))
                .andExpect(jsonPath("$.data.total_pages").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/product-expiration/{id} - 상세 조회")
    inner class GetDetail {

        @Test
        @DisplayName("성공 - 상세 조회")
        fun getDetail_success() {
            whenever(adminProductExpirationService.getDetail(1L, 1)).thenReturn(sampleResponse())

            mockMvc.perform(get("/api/v1/admin/product-expiration/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product_name").value("진라면"))
                .andExpect(jsonPath("$.data.d_day").value(26))
                .andExpect(jsonPath("$.data.description").value("테스트 설명"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID → 404")
        fun getDetail_notFound() {
            whenever(adminProductExpirationService.getDetail(1L, 999))
                .thenThrow(ProductExpirationNotFoundException())

            mockMvc.perform(get("/api/v1/admin/product-expiration/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/product-expiration - 등록")
    inner class Create {

        @Test
        @DisplayName("성공 - 201 Created")
        fun create_success() {
            whenever(adminProductExpirationService.create(eq(1L), any())).thenReturn(sampleResponse())

            val json = """
                {
                    "employee_code": "E001",
                    "account_code": "A001",
                    "product_code": "P001",
                    "expiration_date": "2026-05-01",
                    "alarm_date": "2026-04-24",
                    "description": "테스트 설명"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product_name").value("진라면"))
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락 → 400")
        fun create_missingRequiredField() {
            val json = """
                {
                    "employee_code": "E001",
                    "account_code": "A001"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 알림일 오류 → 400")
        fun create_invalidAlertDate() {
            whenever(adminProductExpirationService.create(eq(1L), any()))
                .thenThrow(InvalidAlertDateException())

            val json = """
                {
                    "employee_code": "E001",
                    "account_code": "A001",
                    "product_code": "P001",
                    "expiration_date": "2026-05-01",
                    "alarm_date": "2026-06-01",
                    "description": null
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_ALERT_DATE"))
        }

        @Test
        @DisplayName("실패 - 권한 없음 → 403")
        fun create_forbidden() {
            whenever(adminProductExpirationService.create(eq(1L), any()))
                .thenThrow(ProductExpirationForbiddenException())

            val json = """
                {
                    "employee_code": "E099",
                    "account_code": "A001",
                    "product_code": "P001",
                    "expiration_date": "2026-05-01",
                    "alarm_date": "2026-04-24",
                    "description": null
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/admin/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_FORBIDDEN"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/product-expiration/{id} - 수정")
    inner class Update {

        @Test
        @DisplayName("성공 - 수정")
        fun update_success() {
            val updated = sampleResponse().copy(
                expirationDate = "2026-06-01",
                alarmDate = "2026-05-25",
                dDay = 57
            )
            whenever(adminProductExpirationService.update(eq(1L), eq(1), any())).thenReturn(updated)

            val json = """
                {
                    "expiration_date": "2026-06-01",
                    "alarm_date": "2026-05-25",
                    "description": "테스트 설명"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/admin/product-expiration/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiration_date").value("2026-06-01"))
                .andExpect(jsonPath("$.data.alarm_date").value("2026-05-25"))
                .andExpect(jsonPath("$.data.d_day").value(57))
        }

        @Test
        @DisplayName("실패 - 미존재 → 404")
        fun update_notFound() {
            whenever(adminProductExpirationService.update(eq(1L), eq(999), any()))
                .thenThrow(ProductExpirationNotFoundException())

            val json = """
                {
                    "expiration_date": "2026-06-01",
                    "alarm_date": "2026-05-25"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/admin/product-expiration/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/product-expiration/{id} - 삭제")
    inner class Delete {

        @Test
        @DisplayName("성공 - 삭제")
        fun delete_success() {
            mockMvc.perform(delete("/api/v1/admin/product-expiration/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty)
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/product-expiration/batch-delete - 일괄 삭제")
    inner class BatchDelete {

        @Test
        @DisplayName("성공 - 일괄 삭제")
        fun batchDelete_success() {
            val response = AdminProductExpirationBatchDeleteResponse(deletedCount = 3)
            whenever(adminProductExpirationService.batchDelete(eq(1L), any())).thenReturn(response)

            val json = """{"ids": [1, 2, 3]}"""

            mockMvc.perform(
                post("/api/v1/admin/product-expiration/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deleted_count").value(3))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/product-expiration/summary - 요약 조회")
    inner class GetSummary {

        @Test
        @DisplayName("성공 - 요약 조회")
        fun getSummary_success() {
            val response = AdminProductExpirationSummaryResponse(
                totalCount = 100L,
                expiredCount = 10L,
                imminentCount = 20L,
                normalCount = 70L
            )
            whenever(adminProductExpirationService.getSummary(1L)).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/product-expiration/summary"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_count").value(100))
                .andExpect(jsonPath("$.data.expired_count").value(10))
                .andExpect(jsonPath("$.data.imminent_count").value(20))
                .andExpect(jsonPath("$.data.normal_count").value(70))
        }
    }
}
