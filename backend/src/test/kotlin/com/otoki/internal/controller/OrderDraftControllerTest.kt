package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.request.DraftItemRequest
import com.otoki.internal.dto.request.OrderDraftRequest
import com.otoki.internal.dto.response.DraftItemResponse
import com.otoki.internal.dto.response.DraftSavedResponse
import com.otoki.internal.dto.response.OrderDraftResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.ClientNotFoundException
import com.otoki.internal.exception.DraftNotFoundException
import com.otoki.internal.exception.InvalidDeliveryDateException
import com.otoki.internal.exception.ProductNotFoundException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.OrderDraftService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
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
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@WebMvcTest(OrderDraftController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("OrderDraftController 테스트")
class OrderDraftControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var orderDraftService: OrderDraftService

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

    @Test
    @DisplayName("GET /api/v1/me/orders/draft - 임시저장 조회 성공 (데이터 존재)")
    fun getDraft_WhenDraftExists_ReturnsSuccess() {
        // given
        val savedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        val draftResponse = OrderDraftResponse(
            clientId = 100L,
            clientName = "테스트 거래처",
            deliveryDate = "2026-02-15",
            items = listOf(
                DraftItemResponse(
                    productCode = "P001",
                    productName = "테스트 상품",
                    boxQuantity = 5,
                    pieceQuantity = 3,
                    unitPrice = 10000L,
                    amount = 53000L,
                    piecesPerBox = 10,
                    minOrderUnit = 1,
                    supplyQuantity = 53,
                    dcQuantity = 0
                )
            ),
            totalAmount = 53000L,
            savedAt = savedAt
        )

        whenever(orderDraftService.getMyDraft(eq(1L))).thenReturn(draftResponse)

        // when & then (Jackson SNAKE_CASE 설정 적용)
        mockMvc.perform(
            get("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").exists())
            .andExpect(jsonPath("$.data.client_id").value(100))
            .andExpect(jsonPath("$.data.client_name").value("테스트 거래처"))
            .andExpect(jsonPath("$.data.delivery_date").value("2026-02-15"))
            .andExpect(jsonPath("$.data.items").isArray)
            .andExpect(jsonPath("$.data.items[0].product_code").value("P001"))
            .andExpect(jsonPath("$.data.items[0].product_name").value("테스트 상품"))
            .andExpect(jsonPath("$.data.items[0].box_quantity").value(5))
            .andExpect(jsonPath("$.data.items[0].piece_quantity").value(3))
            .andExpect(jsonPath("$.data.items[0].unit_price").value(10000))
            .andExpect(jsonPath("$.data.items[0].amount").value(53000))
            .andExpect(jsonPath("$.data.total_amount").value(53000))
            .andExpect(jsonPath("$.data.saved_at").value(savedAt))
    }

    @Test
    @DisplayName("GET /api/v1/me/orders/draft - 임시저장 조회 성공 (데이터 없음)")
    fun getDraft_WhenNoDraft_ReturnsNullData() {
        // given
        whenever(orderDraftService.getMyDraft(eq(1L))).thenReturn(null)

        // when & then
        mockMvc.perform(
            get("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data").isEmpty)
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - 임시저장 생성 성공")
    fun saveDraft_WithValidRequest_ReturnsSuccess() {
        // given
        val savedAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        // Jackson SNAKE_CASE이므로 objectMapper로 직렬화 시 snake_case로 변환됨
        val request = OrderDraftRequest(
            clientId = 100L,
            deliveryDate = "2026-02-15",
            items = listOf(
                DraftItemRequest(
                    productCode = "P001",
                    boxQuantity = 5,
                    pieceQuantity = 3
                )
            )
        )
        val response = DraftSavedResponse(savedAt = savedAt)

        whenever(orderDraftService.saveDraft(eq(1L), any())).thenReturn(response)

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.saved_at").value(savedAt))
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - clientId 누락 시 검증 실패")
    fun saveDraft_WithMissingClientId_ReturnsValidationError() {
        // given - snake_case JSON으로 직접 구성
        val invalidJson = """
            {
                "client_id": null,
                "delivery_date": "2026-02-15",
                "items": [{"product_code": "P001", "box_quantity": 5, "piece_quantity": 3}]
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - items 비어있을 때 검증 실패")
    fun saveDraft_WithEmptyItems_ReturnsValidationError() {
        // given
        val invalidJson = """
            {
                "client_id": 100,
                "delivery_date": "2026-02-15",
                "items": []
            }
        """.trimIndent()

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").exists())
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - 거래처 미존재 시 404 반환")
    fun saveDraft_WithNonExistentClient_ReturnsNotFound() {
        // given
        val request = OrderDraftRequest(
            clientId = 999L,
            deliveryDate = "2026-02-15",
            items = listOf(
                DraftItemRequest(productCode = "P001", boxQuantity = 5, pieceQuantity = 3)
            )
        )

        whenever(orderDraftService.saveDraft(eq(1L), any()))
            .thenThrow(ClientNotFoundException())

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("CLIENT_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - 상품 미존재 시 404 반환")
    fun saveDraft_WithNonExistentProduct_ReturnsNotFound() {
        // given
        val request = OrderDraftRequest(
            clientId = 100L,
            deliveryDate = "2026-02-15",
            items = listOf(
                DraftItemRequest(productCode = "INVALID", boxQuantity = 5, pieceQuantity = 3)
            )
        )

        whenever(orderDraftService.saveDraft(eq(1L), any()))
            .thenThrow(ProductNotFoundException("INVALID"))

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("PRODUCT_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    @Test
    @DisplayName("POST /api/v1/me/orders/draft - 잘못된 배송일자 시 400 반환")
    fun saveDraft_WithInvalidDeliveryDate_ReturnsBadRequest() {
        // given
        val request = OrderDraftRequest(
            clientId = 100L,
            deliveryDate = "2026-01-01",
            items = listOf(
                DraftItemRequest(productCode = "P001", boxQuantity = 5, pieceQuantity = 3)
            )
        )

        whenever(orderDraftService.saveDraft(eq(1L), any()))
            .thenThrow(InvalidDeliveryDateException())

        // when & then
        mockMvc.perform(
            post("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("INVALID_DATE"))
            .andExpect(jsonPath("$.error.message").exists())
    }

    @Test
    @DisplayName("DELETE /api/v1/me/orders/draft - 임시저장 삭제 성공")
    fun deleteDraft_WhenDraftExists_ReturnsSuccess() {
        // when & then
        mockMvc.perform(
            delete("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.success").value(true))
    }

    @Test
    @DisplayName("DELETE /api/v1/me/orders/draft - 임시저장 미존재 시 404 반환")
    fun deleteDraft_WhenNoDraft_ReturnsNotFound() {
        // given
        whenever(orderDraftService.deleteDraft(eq(1L)))
            .thenThrow(DraftNotFoundException())

        // when & then
        mockMvc.perform(
            delete("/api/v1/me/orders/draft")
                .contentType(MediaType.APPLICATION_JSON)
        )
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error.code").value("DRAFT_NOT_FOUND"))
            .andExpect(jsonPath("$.error.message").exists())
    }
}
