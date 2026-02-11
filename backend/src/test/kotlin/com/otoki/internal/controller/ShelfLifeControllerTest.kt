package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.request.ShelfLifeBatchDeleteRequest
import com.otoki.internal.dto.request.ShelfLifeCreateRequest
import com.otoki.internal.dto.request.ShelfLifeUpdateRequest
import com.otoki.internal.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.dto.response.ShelfLifeItemResponse
import com.otoki.internal.dto.response.ShelfLifeListResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.*
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.ShelfLifeService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*

@WebMvcTest(ShelfLifeController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ShelfLifeController 테스트")
class ShelfLifeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var shelfLifeService: ShelfLifeService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private lateinit var testPrincipal: UserPrincipal

    @BeforeEach
    fun setUp() {
        testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== 목록 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/shelf-life - 목록 조회")
    inner class GetShelfLifeList {

        @Test
        @DisplayName("성공 - 전체 거래처 조회")
        fun getShelfLifeList_WithoutStoreId_ReturnsSuccess() {
            // given
            val response = ShelfLifeListResponse(
                totalCount = 2,
                expiredItems = listOf(
                    ShelfLifeItemResponse(
                        id = 1L,
                        productCode = "P001",
                        productName = "오뚜기 카레",
                        storeName = "이마트 강남점",
                        storeId = 10L,
                        expiryDate = "2026-02-01",
                        alertDate = "2026-01-25",
                        dDay = -10,
                        description = null,
                        isExpired = true
                    )
                ),
                upcomingItems = listOf(
                    ShelfLifeItemResponse(
                        id = 2L,
                        productCode = "P002",
                        productName = "오뚜기 라면",
                        storeName = "이마트 강남점",
                        storeId = 10L,
                        expiryDate = "2026-03-01",
                        alertDate = "2026-02-22",
                        dDay = 18,
                        description = "주의 필요",
                        isExpired = false
                    )
                )
            )

            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq(null), eq("2026-01-01"), eq("2026-06-30")))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("fromDate", "2026-01-01")
                    .param("toDate", "2026-06-30")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.expired_items").isArray)
                .andExpect(jsonPath("$.data.expired_items[0].id").value(1))
                .andExpect(jsonPath("$.data.expired_items[0].product_code").value("P001"))
                .andExpect(jsonPath("$.data.expired_items[0].product_name").value("오뚜기 카레"))
                .andExpect(jsonPath("$.data.expired_items[0].dday").value(-10))
                .andExpect(jsonPath("$.data.expired_items[0].is_expired").value(true))
                .andExpect(jsonPath("$.data.upcoming_items").isArray)
                .andExpect(jsonPath("$.data.upcoming_items[0].id").value(2))
                .andExpect(jsonPath("$.data.upcoming_items[0].dday").value(18))
                .andExpect(jsonPath("$.data.upcoming_items[0].is_expired").value(false))
                .andExpect(jsonPath("$.data.upcoming_items[0].description").value("주의 필요"))
        }

        @Test
        @DisplayName("성공 - 특정 거래처 필터 조회")
        fun getShelfLifeList_WithStoreId_ReturnsSuccess() {
            // given
            val response = ShelfLifeListResponse(
                totalCount = 0,
                expiredItems = emptyList(),
                upcomingItems = emptyList()
            )

            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq(10L), eq("2026-01-01"), eq("2026-03-01")))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("storeId", "10")
                    .param("fromDate", "2026-01-01")
                    .param("toDate", "2026-03-01")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_count").value(0))
                .andExpect(jsonPath("$.data.expired_items").isEmpty)
                .andExpect(jsonPath("$.data.upcoming_items").isEmpty)
        }

        @Test
        @DisplayName("실패 - fromDate 누락")
        fun getShelfLifeList_MissingFromDate_ReturnsBadRequest() {
            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("toDate", "2026-06-30")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 범위")
        fun getShelfLifeList_InvalidDateRange_ReturnsBadRequest() {
            // given
            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq(null), eq("2026-06-30"), eq("2026-01-01")))
                .thenThrow(InvalidShelfLifeDateRangeException("종료일은 시작일 이후여야 합니다"))

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("fromDate", "2026-06-30")
                    .param("toDate", "2026-01-01")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }
    }

    // ========== 단건 조회 ==========

    @Nested
    @DisplayName("GET /api/v1/shelf-life/{shelfLifeId} - 단건 조회")
    inner class GetShelfLife {

        @Test
        @DisplayName("성공 - 단건 조회")
        fun getShelfLife_WithValidId_ReturnsSuccess() {
            // given
            val response = ShelfLifeItemResponse(
                id = 1L,
                productCode = "P001",
                productName = "오뚜기 카레",
                storeName = "이마트 강남점",
                storeId = 10L,
                expiryDate = "2026-03-01",
                alertDate = "2026-02-22",
                dDay = 18,
                description = "주의 필요",
                isExpired = false
            )

            whenever(shelfLifeService.getShelfLife(eq(1L), eq(1L))).thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product_code").value("P001"))
                .andExpect(jsonPath("$.data.product_name").value("오뚜기 카레"))
                .andExpect(jsonPath("$.data.store_name").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.store_id").value(10))
                .andExpect(jsonPath("$.data.expiry_date").value("2026-03-01"))
                .andExpect(jsonPath("$.data.alert_date").value("2026-02-22"))
                .andExpect(jsonPath("$.data.dday").value(18))
                .andExpect(jsonPath("$.data.description").value("주의 필요"))
                .andExpect(jsonPath("$.data.is_expired").value(false))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun getShelfLife_WithNonExistentId_ReturnsNotFound() {
            // given
            whenever(shelfLifeService.getShelfLife(eq(1L), eq(999L)))
                .thenThrow(ShelfLifeNotFoundException())

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life/999")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 타인의 데이터 접근")
        fun getShelfLife_WithOtherUserData_ReturnsForbidden() {
            // given
            whenever(shelfLifeService.getShelfLife(eq(1L), eq(5L)))
                .thenThrow(ShelfLifeForbiddenException())

            // when & then
            mockMvc.perform(
                get("/api/v1/shelf-life/5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    // ========== 등록 ==========

    @Nested
    @DisplayName("POST /api/v1/shelf-life - 등록")
    inner class CreateShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 등록")
        fun createShelfLife_WithValidRequest_ReturnsCreated() {
            // given
            val request = ShelfLifeCreateRequest(
                storeId = 10L,
                productCode = "P001",
                expiryDate = "2026-06-01",
                alertDate = "2026-05-25",
                description = "냉장보관"
            )
            val response = ShelfLifeItemResponse(
                id = 1L,
                productCode = "P001",
                productName = "오뚜기 카레",
                storeName = "이마트 강남점",
                storeId = 10L,
                expiryDate = "2026-06-01",
                alertDate = "2026-05-25",
                dDay = 110,
                description = "냉장보관",
                isExpired = false
            )

            whenever(shelfLifeService.createShelfLife(eq(1L), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.product_code").value("P001"))
                .andExpect(jsonPath("$.data.product_name").value("오뚜기 카레"))
                .andExpect(jsonPath("$.data.store_name").value("이마트 강남점"))
                .andExpect(jsonPath("$.data.expiry_date").value("2026-06-01"))
                .andExpect(jsonPath("$.data.alert_date").value("2026-05-25"))
                .andExpect(jsonPath("$.data.description").value("냉장보관"))
        }

        @Test
        @DisplayName("실패 - storeId 누락")
        fun createShelfLife_MissingStoreId_ReturnsBadRequest() {
            // given
            val invalidJson = """
                {
                    "store_id": null,
                    "product_code": "P001",
                    "expiry_date": "2026-06-01",
                    "alert_date": "2026-05-25"
                }
            """.trimIndent()

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - productCode 누락")
        fun createShelfLife_MissingProductCode_ReturnsBadRequest() {
            // given
            val invalidJson = """
                {
                    "store_id": 10,
                    "product_code": "",
                    "expiry_date": "2026-06-01",
                    "alert_date": "2026-05-25"
                }
            """.trimIndent()

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식")
        fun createShelfLife_InvalidDateFormat_ReturnsBadRequest() {
            // given
            val invalidJson = """
                {
                    "store_id": 10,
                    "product_code": "P001",
                    "expiry_date": "2026/06/01",
                    "alert_date": "2026-05-25"
                }
            """.trimIndent()

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 알림 날짜가 유통기한 이후")
        fun createShelfLife_AlertDateAfterExpiry_ReturnsBadRequest() {
            // given
            val request = ShelfLifeCreateRequest(
                storeId = 10L,
                productCode = "P001",
                expiryDate = "2026-06-01",
                alertDate = "2026-06-05"
            )

            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(InvalidAlertDateException())

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_ALERT_DATE"))
        }

        @Test
        @DisplayName("실패 - 중복 등록")
        fun createShelfLife_Duplicate_ReturnsConflict() {
            // given
            val request = ShelfLifeCreateRequest(
                storeId = 10L,
                productCode = "P001",
                expiryDate = "2026-06-01",
                alertDate = "2026-05-25"
            )

            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(DuplicateShelfLifeException())

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DUPLICATE_ENTRY"))
        }

        @Test
        @DisplayName("실패 - 거래처 미존재")
        fun createShelfLife_StoreNotFound_ReturnsNotFound() {
            // given
            val request = ShelfLifeCreateRequest(
                storeId = 999L,
                productCode = "P001",
                expiryDate = "2026-06-01",
                alertDate = "2026-05-25"
            )

            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(ShelfLifeStoreNotFoundException())

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 제품 미존재")
        fun createShelfLife_ProductNotFound_ReturnsNotFound() {
            // given
            val request = ShelfLifeCreateRequest(
                storeId = 10L,
                productCode = "INVALID",
                expiryDate = "2026-06-01",
                alertDate = "2026-05-25"
            )

            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(ShelfLifeProductNotFoundException())

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
        }
    }

    // ========== 수정 ==========

    @Nested
    @DisplayName("PUT /api/v1/shelf-life/{shelfLifeId} - 수정")
    inner class UpdateShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 수정")
        fun updateShelfLife_WithValidRequest_ReturnsSuccess() {
            // given
            val request = ShelfLifeUpdateRequest(
                expiryDate = "2026-07-01",
                alertDate = "2026-06-25",
                description = "수정된 메모"
            )
            val response = ShelfLifeItemResponse(
                id = 1L,
                productCode = "P001",
                productName = "오뚜기 카레",
                storeName = "이마트 강남점",
                storeId = 10L,
                expiryDate = "2026-07-01",
                alertDate = "2026-06-25",
                dDay = 140,
                description = "수정된 메모",
                isExpired = false
            )

            whenever(shelfLifeService.updateShelfLife(eq(1L), eq(1L), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                put("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.expiry_date").value("2026-07-01"))
                .andExpect(jsonPath("$.data.alert_date").value("2026-06-25"))
                .andExpect(jsonPath("$.data.description").value("수정된 메모"))
        }

        @Test
        @DisplayName("실패 - expiryDate 누락")
        fun updateShelfLife_MissingExpiryDate_ReturnsBadRequest() {
            // given
            val invalidJson = """
                {
                    "expiry_date": "",
                    "alert_date": "2026-06-25"
                }
            """.trimIndent()

            // when & then
            mockMvc.perform(
                put("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun updateShelfLife_NonExistentId_ReturnsNotFound() {
            // given
            val request = ShelfLifeUpdateRequest(
                expiryDate = "2026-07-01",
                alertDate = "2026-06-25"
            )

            whenever(shelfLifeService.updateShelfLife(eq(1L), eq(999L), any()))
                .thenThrow(ShelfLifeNotFoundException())

            // when & then
            mockMvc.perform(
                put("/api/v1/shelf-life/999")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 타인의 데이터 수정 시도")
        fun updateShelfLife_OtherUserData_ReturnsForbidden() {
            // given
            val request = ShelfLifeUpdateRequest(
                expiryDate = "2026-07-01",
                alertDate = "2026-06-25"
            )

            whenever(shelfLifeService.updateShelfLife(eq(1L), eq(5L), any()))
                .thenThrow(ShelfLifeForbiddenException())

            // when & then
            mockMvc.perform(
                put("/api/v1/shelf-life/5")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    // ========== 단건 삭제 ==========

    @Nested
    @DisplayName("DELETE /api/v1/shelf-life/{shelfLifeId} - 단건 삭제")
    inner class DeleteShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 삭제")
        fun deleteShelfLife_WithValidId_ReturnsSuccess() {
            // when & then
            mockMvc.perform(
                delete("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 ID")
        fun deleteShelfLife_NonExistentId_ReturnsNotFound() {
            // given
            whenever(shelfLifeService.deleteShelfLife(eq(1L), eq(999L)))
                .thenThrow(ShelfLifeNotFoundException())

            // when & then
            mockMvc.perform(
                delete("/api/v1/shelf-life/999")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }

        @Test
        @DisplayName("실패 - 타인의 데이터 삭제 시도")
        fun deleteShelfLife_OtherUserData_ReturnsForbidden() {
            // given
            whenever(shelfLifeService.deleteShelfLife(eq(1L), eq(5L)))
                .thenThrow(ShelfLifeForbiddenException())

            // when & then
            mockMvc.perform(
                delete("/api/v1/shelf-life/5")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    // ========== 일괄 삭제 ==========

    @Nested
    @DisplayName("POST /api/v1/shelf-life/batch-delete - 일괄 삭제")
    inner class DeleteShelfLifeBatch {

        @Test
        @DisplayName("성공 - 일괄 삭제")
        fun deleteShelfLifeBatch_WithValidIds_ReturnsSuccess() {
            // given
            val request = ShelfLifeBatchDeleteRequest(ids = listOf(1L, 2L, 3L))
            val response = ShelfLifeBatchDeleteResponse(deletedCount = 3)

            whenever(shelfLifeService.deleteShelfLifeBatch(eq(1L), any())).thenReturn(response)

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deleted_count").value(3))
        }

        @Test
        @DisplayName("실패 - ids 비어있음")
        fun deleteShelfLifeBatch_EmptyIds_ReturnsBadRequest() {
            // given
            val invalidJson = """
                {
                    "ids": []
                }
            """.trimIndent()

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 타인의 데이터 포함")
        fun deleteShelfLifeBatch_ContainsOtherUserData_ReturnsForbidden() {
            // given
            val request = ShelfLifeBatchDeleteRequest(ids = listOf(1L, 5L))

            whenever(shelfLifeService.deleteShelfLifeBatch(eq(1L), any()))
                .thenThrow(ShelfLifeForbiddenException())

            // when & then
            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }
}
