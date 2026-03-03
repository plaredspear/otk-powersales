package com.otoki.internal.shelflife.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.common.entity.UserRole
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.shelflife.dto.response.ShelfLifeBatchDeleteResponse
import com.otoki.internal.shelflife.dto.response.ShelfLifeItemResponse
import com.otoki.internal.shelflife.exception.InvalidAlertDateException
import com.otoki.internal.shelflife.exception.InvalidShelfLifeDateRangeException
import com.otoki.internal.shelflife.exception.ShelfLifeForbiddenException
import com.otoki.internal.shelflife.exception.ShelfLifeNotFoundException
import com.otoki.internal.shelflife.service.ShelfLifeService
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

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

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.USER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

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
    ): ShelfLifeItemResponse {
        return ShelfLifeItemResponse(
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
    @DisplayName("GET /api/v1/shelf-life - 목록 조회")
    inner class GetShelfLifeList {

        @Test
        @DisplayName("성공 - 전체 거래처 조회")
        fun getList_success() {
            val items = listOf(createItemResponse(seq = 1), createItemResponse(seq = 2))
            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq(null), any(), any()))
                .thenReturn(items)

            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("fromDate", "2026-03-01")
                    .param("toDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray)
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].seq").value(1))
                .andExpect(jsonPath("$.data[0].product_code").value("30310009"))
        }

        @Test
        @DisplayName("성공 - 특정 거래처 조회")
        fun getList_withAccountCode() {
            val items = listOf(createItemResponse())
            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq("1025172"), any(), any()))
                .thenReturn(items)

            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("accountCode", "1025172")
                    .param("fromDate", "2026-03-01")
                    .param("toDate", "2026-03-31")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data[0].account_code").value("1025172"))
        }

        @Test
        @DisplayName("실패 - 날짜 범위 초과")
        fun getList_dateRangeExceeded() {
            whenever(shelfLifeService.getShelfLifeList(eq(1L), eq(null), any(), any()))
                .thenThrow(InvalidShelfLifeDateRangeException("날짜 범위가 올바르지 않습니다"))

            mockMvc.perform(
                get("/api/v1/shelf-life")
                    .param("fromDate", "2026-01-01")
                    .param("toDate", "2026-12-31")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_RANGE"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/shelf-life - 등록")
    inner class CreateShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 등록")
        fun create_success() {
            val response = createItemResponse(dDay = 7, isExpired = false)
            whenever(shelfLifeService.createShelfLife(eq(1L), any())).thenReturn(response)

            val requestJson = """
                {
                    "account_code": "1025172",
                    "account_name": "(유)경산식품",
                    "product_code": "30310009",
                    "product_name": "고등어김치&무조림(캔)280G",
                    "expiration_date": "2026-03-10",
                    "alarm_date": "2026-03-09"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.product_code").value("30310009"))
        }

        @Test
        @DisplayName("실패 - 필수 필드 누락")
        fun create_missingField() {
            val requestJson = """
                {
                    "account_code": "1025172",
                    "product_code": "30310009",
                    "expiration_date": "2026-03-10",
                    "alarm_date": "2026-03-09"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("실패 - 알림일 오류")
        fun create_invalidAlertDate() {
            whenever(shelfLifeService.createShelfLife(eq(1L), any()))
                .thenThrow(InvalidAlertDateException())

            val requestJson = """
                {
                    "account_code": "1025172",
                    "account_name": "(유)경산식품",
                    "product_code": "30310009",
                    "product_name": "제품명",
                    "expiration_date": "2026-03-10",
                    "alarm_date": "2026-03-10"
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/shelf-life")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_ALERT_DATE"))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/shelf-life/{seq} - 수정")
    inner class UpdateShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 수정")
        fun update_success() {
            val response = createItemResponse(expirationDate = "2026-04-10", alarmDate = "2026-04-09")
            whenever(shelfLifeService.updateShelfLife(eq(1L), eq(1), any())).thenReturn(response)

            val requestJson = """
                {
                    "expiration_date": "2026-04-10",
                    "alarm_date": "2026-04-09",
                    "description": "수정됨"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.expiration_date").value("2026-04-10"))
        }

        @Test
        @DisplayName("실패 - 타인 데이터")
        fun update_forbidden() {
            whenever(shelfLifeService.updateShelfLife(eq(1L), eq(1), any()))
                .thenThrow(ShelfLifeForbiddenException())

            val requestJson = """
                {
                    "expiration_date": "2026-04-10",
                    "alarm_date": "2026-04-09"
                }
            """.trimIndent()

            mockMvc.perform(
                put("/api/v1/shelf-life/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/shelf-life/{seq} - 단건 삭제")
    inner class DeleteShelfLife {

        @Test
        @DisplayName("성공 - 유통기한 삭제")
        fun delete_success() {
            mockMvc.perform(delete("/api/v1/shelf-life/1"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("유통기한이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 seq")
        fun delete_notFound() {
            whenever(shelfLifeService.deleteShelfLife(eq(1L), eq(999)))
                .thenThrow(ShelfLifeNotFoundException())

            mockMvc.perform(delete("/api/v1/shelf-life/999"))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/shelf-life/batch-delete - 일괄 삭제")
    inner class BatchDeleteShelfLife {

        @Test
        @DisplayName("성공 - 일괄 삭제 3건")
        fun batchDelete_success() {
            val response = ShelfLifeBatchDeleteResponse(deletedCount = 3)
            whenever(shelfLifeService.deleteShelfLifeBatch(eq(1L), any())).thenReturn(response)

            val requestJson = """{"ids": [1, 2, 3]}"""

            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.deleted_count").value(3))
                .andExpect(jsonPath("$.message").value("3건의 유통기한이 삭제되었습니다"))
        }

        @Test
        @DisplayName("실패 - 타인 데이터 포함")
        fun batchDelete_forbidden() {
            whenever(shelfLifeService.deleteShelfLifeBatch(eq(1L), any()))
                .thenThrow(ShelfLifeForbiddenException())

            val requestJson = """{"ids": [1, 2]}"""

            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("FORBIDDEN"))
        }

        @Test
        @DisplayName("실패 - 빈 ids 리스트")
        fun batchDelete_emptyIds() {
            val requestJson = """{"ids": []}"""

            mockMvc.perform(
                post("/api/v1/shelf-life/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }
    }
}
