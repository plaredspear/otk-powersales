package com.otoki.powersales.admin.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationBatchDeleteResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationListResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationResponse
import com.otoki.powersales.productexpiration.dto.response.AdminProductExpirationSummaryResponse
import com.otoki.powersales.productexpiration.service.AdminProductExpirationService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.auth.entity.UserRole
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

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

    @MockkBean
    private lateinit var adminProductExpirationService: AdminProductExpirationService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private fun setUpPrincipal(userId: Long = 1L, role: UserRole = UserRole.WOMAN) {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeCode = "S001",
            employeeId = userId,
            role = role,
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
        createdAt = java.time.LocalDateTime.of(2026, 4, 1, 10, 0, 0),
        updatedAt = java.time.LocalDateTime.of(2026, 4, 1, 10, 0, 0)
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
            every { adminProductExpirationService.getList(eq(1L), any(), any(), any(), any(), any(), any()) } returns response

            mockMvc.perform(get("/api/v1/admin/product-expiration"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].productName").value("진라면"))
                .andExpect(jsonPath("$.data.content[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.content[0].accountName").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.content[0].employeeName").value("홍길동"))
                .andExpect(jsonPath("$.data.content[0].expirationDate").value("2026-05-01"))
                .andExpect(jsonPath("$.data.content[0].alarmDate").value("2026-04-24"))
                .andExpect(jsonPath("$.data.content[0].dDay").value(26))
                .andExpect(jsonPath("$.data.content[0].status").value("NORMAL"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/product-expiration/{id} - 상세 조회")
    inner class GetDetail {

        @Test
        @DisplayName("성공 - 상세 조회")
        fun getDetail_success() {
            every { adminProductExpirationService.getDetail(1L, 1) } returns sampleResponse()

            mockMvc.perform(get("/api/v1/admin/product-expiration/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.productName").value("진라면"))
                .andExpect(jsonPath("$.data.dDay").value(26))
                .andExpect(jsonPath("$.data.description").value("테스트 설명"))
        }

        @Test
        @DisplayName("실패 - 미존재 ID → 404")
        fun getDetail_notFound() {
            every { adminProductExpirationService.getDetail(1L, 999) } throws ProductExpirationNotFoundException()

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
            every { adminProductExpirationService.create(eq(1L), any()) } returns sampleResponse()

            val json = """
                {
                    "employeeCode": "E001",
                    "accountCode": "A001",
                    "productCode": "P001",
                    "expirationDate": "2026-05-01",
                    "alarmDate": "2026-04-24",
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
                .andExpect(jsonPath("$.data.productName").value("진라면"))
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락 → 400")
        fun create_missingRequiredField() {
            val json = """
                {
                    "employeeCode": "E001",
                    "accountCode": "A001"
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
            every { adminProductExpirationService.create(eq(1L), any()) } throws InvalidAlertDateException()

            val json = """
                {
                    "employeeCode": "E001",
                    "accountCode": "A001",
                    "productCode": "P001",
                    "expirationDate": "2026-05-01",
                    "alarmDate": "2026-06-01",
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
            every { adminProductExpirationService.create(eq(1L), any()) } throws ProductExpirationForbiddenException()

            val json = """
                {
                    "employeeCode": "E099",
                    "accountCode": "A001",
                    "productCode": "P001",
                    "expirationDate": "2026-05-01",
                    "alarmDate": "2026-04-24",
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
            every { adminProductExpirationService.update(eq(1L), eq(1), any()) } returns updated

            val json = """
                {
                    "expirationDate": "2026-06-01",
                    "alarmDate": "2026-05-25",
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
                .andExpect(jsonPath("$.data.expirationDate").value("2026-06-01"))
                .andExpect(jsonPath("$.data.alarmDate").value("2026-05-25"))
                .andExpect(jsonPath("$.data.dDay").value(57))
        }

        @Test
        @DisplayName("실패 - 미존재 → 404")
        fun update_notFound() {
            every { adminProductExpirationService.update(eq(1L), eq(999), any()) } throws ProductExpirationNotFoundException()

            val json = """
                {
                    "expirationDate": "2026-06-01",
                    "alarmDate": "2026-05-25"
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
            every { adminProductExpirationService.delete(any(), any()) } just Runs

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
            every { adminProductExpirationService.batchDelete(eq(1L), any()) } returns response

            val json = """{"ids": [1, 2, 3]}"""

            mockMvc.perform(
                post("/api/v1/admin/product-expiration/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(3))
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
            every { adminProductExpirationService.getSummary(1L) } returns response

            mockMvc.perform(get("/api/v1/admin/product-expiration/summary"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(100))
                .andExpect(jsonPath("$.data.expiredCount").value(10))
                .andExpect(jsonPath("$.data.imminentCount").value(20))
                .andExpect(jsonPath("$.data.normalCount").value(70))
        }
    }
}
