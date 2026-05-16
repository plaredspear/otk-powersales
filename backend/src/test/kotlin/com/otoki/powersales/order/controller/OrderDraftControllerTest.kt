package com.otoki.powersales.order.controller

import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.order.dto.response.OrderDraftDetailResponse
import com.otoki.powersales.order.dto.response.OrderDraftLineResponse
import com.otoki.powersales.order.dto.response.OrderDraftSaveResponse
import com.otoki.powersales.order.exception.OrderDraftAccountForbiddenException
import com.otoki.powersales.order.exception.OrderDraftInvalidRequestException
import com.otoki.powersales.order.service.OrderDraftService
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.LocalDateTime

@WebMvcTest(OrderDraftController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderDraftController 테스트 (#596)")
class OrderDraftControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var orderDraftService: OrderDraftService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter


    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(testPrincipal, null, testPrincipal.authorities)
    }

    @Nested
    @DisplayName("POST /api/v1/mobile/orders/draft - 등록")
    inner class SaveTests {

        private val validBody = """
            {
              "accountId": 5678,
              "totalAmount": 1234567,
              "lines": [
                {
                  "lineNumber": 10, "productCode": "P001", "unit": "BOX",
                  "quantity": 10, "quantityPieces": 100, "quantityBoxes": 10,
                  "unitPrice": 12345, "amount": 1234500
                }
              ]
            }
        """.trimIndent()

        @Test
        @DisplayName("H1 - 정상 등록 → 200 + draftId")
        fun success() {
            whenever(orderDraftService.save(any(), any())).thenReturn(
                OrderDraftSaveResponse(draftId = 99L, savedAt = LocalDateTime.of(2026, 5, 4, 10, 0)),
            )

            mockMvc.perform(
                post("/api/v1/mobile/orders/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody),
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.draftId").value(99))
                .andExpect(jsonPath("$.message").value("임시저장이 완료되었습니다"))
        }

        @Test
        @DisplayName("E2 - lines 누락 → 400")
        fun emptyLines() {
            val body = """
                { "accountId": 5678, "totalAmount": 0, "lines": [] }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/mobile/orders/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(body),
            )
                .andExpect(status().isBadRequest)
        }

        @Test
        @DisplayName("E1 - 본인 외 거래처 → 403 ORD_DRAFT_ACCOUNT_FORBIDDEN")
        fun forbidden() {
            whenever(orderDraftService.save(any(), any()))
                .thenThrow(OrderDraftAccountForbiddenException())

            mockMvc.perform(
                post("/api/v1/mobile/orders/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody),
            )
                .andExpect(status().isForbidden)
                .andExpect(jsonPath("$.error.code").value("ORD_DRAFT_ACCOUNT_FORBIDDEN"))
        }

        @Test
        @DisplayName("E3 - 단위 잘못 (Service 예외) → 400 ORD_DRAFT_INVALID_REQUEST")
        fun invalidUnit() {
            whenever(orderDraftService.save(any(), any()))
                .thenThrow(OrderDraftInvalidRequestException("단위 오류"))

            mockMvc.perform(
                post("/api/v1/mobile/orders/draft")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validBody),
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ORD_DRAFT_INVALID_REQUEST"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/mobile/orders/draft - 조회")
    inner class GetTests {

        @Test
        @DisplayName("H4 - 임시저장 없음 → 200 + data:null")
        fun empty() {
            whenever(orderDraftService.findByUserId(any())).thenReturn(null)

            mockMvc.perform(get("/api/v1/mobile/orders/draft"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }

        @Test
        @DisplayName("H3 - 임시저장 있음 → 200 + 헤더+라인")
        fun found() {
            val response = OrderDraftDetailResponse(
                draftId = 99L,
                accountId = 5678L,
                accountName = "홍길동상회",
                accountExternalKey = "EK001",
                totalAmount = 1234567,
                savedAt = LocalDateTime.of(2026, 5, 4, 10, 0),
                lines = listOf(
                    OrderDraftLineResponse(
                        lineNumber = 10,
                        productCode = "P001",
                        productName = "진라면",
                        unit = "BOX",
                        quantity = BigDecimal("10"),
                        quantityPieces = 100,
                        quantityBoxes = BigDecimal("10"),
                        unitPrice = BigDecimal("12345"),
                        amount = BigDecimal("1234500"),
                    ),
                ),
            )
            whenever(orderDraftService.findByUserId(any())).thenReturn(response)

            mockMvc.perform(get("/api/v1/mobile/orders/draft"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.draftId").value(99))
                .andExpect(jsonPath("$.data.accountName").value("홍길동상회"))
                .andExpect(jsonPath("$.data.totalAmount").value(1234567))
                .andExpect(jsonPath("$.data.lines[0].productCode").value("P001"))
                .andExpect(jsonPath("$.data.lines[0].unit").value("BOX"))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/mobile/orders/draft - 삭제 (멱등)")
    inner class DeleteTests {

        @Test
        @DisplayName("H5/H6 - 있어도 없어도 204")
        fun noContent() {
            mockMvc.perform(delete("/api/v1/mobile/orders/draft"))
                .andExpect(status().isNoContent)
        }
    }
}
