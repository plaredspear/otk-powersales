package com.otoki.powersales.productexpiration.controller

import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.common.test.MobileControllerTestSupport
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationBatchDeleteResponse
import com.otoki.powersales.productexpiration.dto.response.ProductExpirationItemResponse
import com.otoki.powersales.productexpiration.exception.InvalidAlertDateException
import com.otoki.powersales.productexpiration.exception.InvalidProductExpirationDateRangeException
import com.otoki.powersales.productexpiration.exception.ProductExpirationForbiddenException
import com.otoki.powersales.productexpiration.exception.ProductExpirationNotFoundException
import com.otoki.powersales.productexpiration.service.ProductExpirationService
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(ProductExpirationController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("ProductExpirationController 테스트")
class ProductExpirationControllerTest : MobileControllerTestSupport() {

    @MockkBean
    private lateinit var productExpirationService: ProductExpirationService

    private fun createItemResponse(
        seq: Int = 1,
        productCode: String = "30310009",
        productName: String = "고등어김치&무조림(캔)280G",
        accountCode: String = "1025172",
        accountName: String = "(유)경산식품",
        expirationDate: String = "2026-03-10",
        alarmDate: String = "2026-03-09",
        dDay: Int = -3,
        description: String? = null,
        isExpired: Boolean = true
    ): ProductExpirationItemResponse {
        return ProductExpirationItemResponse(
            seq = seq,
            productCode = productCode,
            productName = productName,
            accountCode = accountCode,
            accountName = accountName,
            expirationDate = expirationDate,
            alarmDate = alarmDate,
            dDay = dDay,
            description = description,
            isExpired = isExpired
        )
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/product-expiration - 목록 조회")
    inner class GetProductExpirationList {

        @Test
        @DisplayName("성공 - 전체 거래처 조회")
        fun getList_success() {
            val items = listOf(createItemResponse(seq = 1), createItemResponse(seq = 2))
            every { productExpirationService.getProductExpirationList(1L, null, any(), any()) } returns items

            mockMvc.perform(
                get("/api/v1/mobile/product-expiration")
                    .param("fromDate", "2026-03-01")
                    .param("toDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].seq").value(1))
                .andExpect(jsonPath("$.data[0].productCode").value("30310009"))
        }

        @Test
        @DisplayName("성공 - 특정 거래처 조회")
        fun getList_withAccountCode() {
            val items = listOf(createItemResponse())
            every { productExpirationService.getProductExpirationList(1L, "1025172", any(), any()) } returns items

            mockMvc.perform(
                get("/api/v1/mobile/product-expiration")
                    .param("accountCode", "1025172")
                    .param("fromDate", "2026-03-01")
                    .param("toDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].accountCode").value("1025172"))
        }

        @Test
        @DisplayName("실패 - 날짜 범위 초과")
        fun getList_dateRangeExceeded() {
            every {
                productExpirationService.getProductExpirationList(1L, null, any(), any())
            } throws InvalidProductExpirationDateRangeException("날짜 범위가 올바르지 않습니다")

            mockMvc.perform(
                get("/api/v1/mobile/product-expiration")
                    .param("fromDate", "2026-01-01")
                    .param("toDate", "2026-12-31")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/product-expiration - 등록")
    inner class CreateProductExpiration {

        @Test
        @DisplayName("성공 - 유통기한 등록")
        fun create_success() {
            val response = createItemResponse(dDay = 7, isExpired = false)
            every { productExpirationService.createProductExpiration(1L, any()) } returns response

            val requestJson = """
                {
                    "accountCode": "1025172",
                    "accountName": "(유)경산식품",
                    "productCode": "30310009",
                    "productName": "고등어김치&무조림(캔)280G",
                    "expirationDate": "2026-03-10",
                    "alarmDate": "2026-03-09"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productCode").value("30310009"))
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        fun create_missingField() {
            val requestJson = """
                {
                    "accountCode": "1025172",
                    "productCode": "30310009",
                    "expirationDate": "2026-03-10",
                    "alarmDate": "2026-03-09"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 알림일 오류")
        fun create_invalidAlertDate() {
            every { productExpirationService.createProductExpiration(1L, any()) } throws InvalidAlertDateException()

            val requestJson = """
                {
                    "accountCode": "1025172",
                    "accountName": "(유)경산식품",
                    "productCode": "30310009",
                    "productName": "제품명",
                    "expirationDate": "2026-03-10",
                    "alarmDate": "2026-03-10"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_ALERT_DATE"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/mobile/product-expiration/{seq} - 수정")
    inner class UpdateProductExpiration {

        @Test
        @DisplayName("성공 - 유통기한 수정")
        fun update_success() {
            val response = createItemResponse(expirationDate = "2026-04-10", alarmDate = "2026-04-09")
            every { productExpirationService.updateProductExpiration(1L, 1, any()) } returns response

            val requestJson = """
                {
                    "expirationDate": "2026-04-10",
                    "alarmDate": "2026-04-09",
                    "description": "수정됨"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/mobile/product-expiration/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expirationDate").value("2026-04-10"))
        }

        @Test
        @DisplayName("실패 - 타인 데이터")
        fun update_forbidden() {
            every {
                productExpirationService.updateProductExpiration(1L, 1, any())
            } throws ProductExpirationForbiddenException()

            val requestJson = """
                {
                    "expirationDate": "2026-04-10",
                    "alarmDate": "2026-04-09"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/mobile/product-expiration/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_FORBIDDEN"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/mobile/product-expiration/{seq} - 단건 삭제")
    inner class DeleteProductExpiration {

        @Test
        @DisplayName("성공 - 유통기한 삭제")
        fun delete_success() {
            every { productExpirationService.deleteProductExpiration(1L, 1) } just Runs

            mockMvc.perform(delete("/api/v1/mobile/product-expiration/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("유통기한이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 seq")
        fun delete_notFound() {
            every {
                productExpirationService.deleteProductExpiration(1L, 999)
            } throws ProductExpirationNotFoundException()

            mockMvc.perform(delete("/api/v1/mobile/product-expiration/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/product-expiration/batch-delete - 일괄 삭제")
    inner class BatchDeleteProductExpiration {

        @Test
        @DisplayName("성공 - 일괄 삭제 3건")
        fun batchDelete_success() {
            val response = ProductExpirationBatchDeleteResponse(deletedCount = 3)
            every { productExpirationService.deleteProductExpirationBatch(1L, any()) } returns response

            val requestJson = """{"ids": [1, 2, 3]}"""

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deletedCount").value(3))
                .andExpect(jsonPath("$.message").value("3건의 유통기한이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 타인 데이터 포함")
        fun batchDelete_forbidden() {
            every {
                productExpirationService.deleteProductExpirationBatch(1L, any())
            } throws ProductExpirationForbiddenException()

            val requestJson = """{"ids": [1, 2]}"""

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("PRODUCT_EXPIRATION_FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 빈 ids 리스트")
        fun batchDelete_emptyIds() {
            val requestJson = """{"ids": []}"""

            mockMvc.perform(
                post("/api/v1/mobile/product-expiration/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
