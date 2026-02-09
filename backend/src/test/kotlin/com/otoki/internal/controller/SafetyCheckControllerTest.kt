package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.request.SafetyCheckSubmitRequest
import com.otoki.internal.dto.response.SafetyCheckItemsResponse
import com.otoki.internal.dto.response.SafetyCheckSubmitResponse
import com.otoki.internal.dto.response.SafetyCheckTodayResponse
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.AlreadySubmittedException
import com.otoki.internal.exception.RequiredItemsMissingException
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.SafetyCheckService
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
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(SafetyCheckController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("SafetyCheckController 테스트")
class SafetyCheckControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var safetyCheckService: SafetyCheckService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== GET /api/v1/safety-check/items ==========

    @Nested
    @DisplayName("GET /api/v1/safety-check/items - 항목 조회")
    inner class GetChecklistItemsTests {

        @Test
        @DisplayName("항목 조회 정상 - 200 OK, 카테고리 + 항목 목록 반환")
        fun getChecklistItems_success() {
            // Given
            val mockResponse = SafetyCheckItemsResponse(
                categories = listOf(
                    SafetyCheckItemsResponse.CategoryInfo(
                        id = 1L,
                        name = "안전예방 장비 착용",
                        description = "아래 항목을 모두 체크하세요",
                        items = listOf(
                            SafetyCheckItemsResponse.CheckItemInfo(
                                id = 1L, label = "손목보호대", sortOrder = 1, required = true
                            ),
                            SafetyCheckItemsResponse.CheckItemInfo(
                                id = 2L, label = "안전화", sortOrder = 2, required = true
                            )
                        )
                    ),
                    SafetyCheckItemsResponse.CategoryInfo(
                        id = 2L,
                        name = "사고 예방",
                        description = "아래 항목을 모두 체크하세요",
                        items = listOf(
                            SafetyCheckItemsResponse.CheckItemInfo(
                                id = 3L, label = "지게차 근접 금지", sortOrder = 1, required = true
                            )
                        )
                    )
                )
            )

            whenever(safetyCheckService.getChecklistItems()).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/safety-check/items")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.categories").isArray)
                .andExpect(jsonPath("$.data.categories[0].id").value(1))
                .andExpect(jsonPath("$.data.categories[0].name").value("안전예방 장비 착용"))
                .andExpect(jsonPath("$.data.categories[0].description").value("아래 항목을 모두 체크하세요"))
                .andExpect(jsonPath("$.data.categories[0].items").isArray)
                .andExpect(jsonPath("$.data.categories[0].items[0].id").value(1))
                .andExpect(jsonPath("$.data.categories[0].items[0].label").value("손목보호대"))
                .andExpect(jsonPath("$.data.categories[0].items[0].sort_order").value(1))
                .andExpect(jsonPath("$.data.categories[0].items[0].required").value(true))
                .andExpect(jsonPath("$.data.categories[1].id").value(2))
                .andExpect(jsonPath("$.data.categories[1].name").value("사고 예방"))
                .andExpect(jsonPath("$.data.categories[1].items[0].label").value("지게차 근접 금지"))
        }
    }

    // ========== POST /api/v1/safety-check/submit ==========

    @Nested
    @DisplayName("POST /api/v1/safety-check/submit - 제출")
    inner class SubmitTests {

        @Test
        @DisplayName("제출 정상 - 200 OK, submissionId 반환")
        fun submit_success() {
            // Given
            val submittedAt = LocalDateTime.of(2026, 2, 8, 8, 30, 0)
            val mockResponse = SafetyCheckSubmitResponse(
                submissionId = 123L,
                submittedAt = submittedAt,
                safetyCheckCompleted = true
            )

            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any())).thenReturn(mockResponse)

            val request = SafetyCheckSubmitRequest(
                checkedItemIds = listOf(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, 12L, 13L, 14L, 15L)
            )

            // When & Then
            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("안전점검이 완료되었습니다."))
                .andExpect(jsonPath("$.data.submission_id").value(123))
                .andExpect(jsonPath("$.data.safety_check_completed").value(true))
        }

        @Test
        @DisplayName("제출 빈 항목 - 400 INVALID_PARAMETER")
        fun submit_emptyItems() {
            // Given
            val request = SafetyCheckSubmitRequest(checkedItemIds = emptyList())

            // When & Then
            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("제출 필수 항목 누락 - 400 REQUIRED_ITEMS_MISSING")
        fun submit_requiredItemsMissing() {
            // Given
            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any()))
                .thenThrow(RequiredItemsMissingException())

            val request = SafetyCheckSubmitRequest(checkedItemIds = listOf(1L, 2L))

            // When & Then
            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REQUIRED_ITEMS_MISSING"))
        }

        @Test
        @DisplayName("제출 중복 - 409 ALREADY_SUBMITTED")
        fun submit_alreadySubmitted() {
            // Given
            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any()))
                .thenThrow(AlreadySubmittedException())

            val request = SafetyCheckSubmitRequest(checkedItemIds = listOf(1L, 2L, 3L))

            // When & Then
            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ALREADY_SUBMITTED"))
        }
    }

    // ========== GET /api/v1/safety-check/today ==========

    @Nested
    @DisplayName("GET /api/v1/safety-check/today - 오늘 여부 조회")
    inner class GetTodayStatusTests {

        @Test
        @DisplayName("오늘 완료 - 200 OK, completed=true")
        fun getTodayStatus_completed() {
            // Given
            val submittedAt = LocalDateTime.of(2026, 2, 8, 8, 30, 0)
            val mockResponse = SafetyCheckTodayResponse(
                completed = true,
                submittedAt = submittedAt
            )

            whenever(safetyCheckService.getTodayStatus(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/safety-check/today")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(true))
                .andExpect(jsonPath("$.data.submitted_at").exists())
        }

        @Test
        @DisplayName("오늘 미완료 - 200 OK, completed=false")
        fun getTodayStatus_notCompleted() {
            // Given
            val mockResponse = SafetyCheckTodayResponse(
                completed = false,
                submittedAt = null
            )

            whenever(safetyCheckService.getTodayStatus(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/safety-check/today")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completed").value(false))
                .andExpect(jsonPath("$.data.submitted_at").doesNotExist())
        }
    }
}
