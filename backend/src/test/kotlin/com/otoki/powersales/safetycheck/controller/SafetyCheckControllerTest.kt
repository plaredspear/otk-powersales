package com.otoki.powersales.safetycheck.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.safetycheck.dto.response.EquipmentStatus
import com.otoki.powersales.safetycheck.dto.response.MemberStatus
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckStatusResponse
import com.otoki.powersales.safetycheck.service.AdminSafetyCheckService
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckItemsResponse
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckSubmitResponse
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckTodayResponse
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.safetycheck.exception.AlreadySubmittedException
import com.otoki.powersales.safetycheck.exception.RequiredItemsMissingException
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.safetycheck.service.SafetyCheckService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
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
    private lateinit var adminSafetyCheckService: AdminSafetyCheckService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    @Nested
    @DisplayName("GET /api/v1/safety-check/items - 항목 조회")
    inner class GetChecklistItemsTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, 카테고리별 항목 반환")
        fun getChecklistItems_success() {
            // Given
            val mockResponse = SafetyCheckItemsResponse(
                categories = listOf(
                    SafetyCheckItemsResponse.CategoryInfo(
                        questionNum = 1,
                        title = "안전예방 장비 착용",
                        inputType = "RADIO",
                        required = true,
                        options = listOf("예", "해당없음"),
                        items = listOf(
                            SafetyCheckItemsResponse.CheckItemInfo(seqNum = 1, contents = "손목보호대를 착용했습니다"),
                            SafetyCheckItemsResponse.CheckItemInfo(seqNum = 2, contents = "숨수건을 소지하고 있습니다")
                        )
                    ),
                    SafetyCheckItemsResponse.CategoryInfo(
                        questionNum = 2,
                        title = "안전사고 예방사항",
                        inputType = "CHECKBOX",
                        required = false,
                        options = null,
                        items = listOf(
                            SafetyCheckItemsResponse.CheckItemInfo(seqNum = 1, contents = "예방사항 항목 1")
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
                .andExpect(jsonPath("$.data.categories[0].question_num").value(1))
                .andExpect(jsonPath("$.data.categories[0].title").value("안전예방 장비 착용"))
                .andExpect(jsonPath("$.data.categories[0].input_type").value("RADIO"))
                .andExpect(jsonPath("$.data.categories[0].required").value(true))
                .andExpect(jsonPath("$.data.categories[0].options[0]").value("예"))
                .andExpect(jsonPath("$.data.categories[0].options[1]").value("해당없음"))
                .andExpect(jsonPath("$.data.categories[0].items[0].seq_num").value(1))
                .andExpect(jsonPath("$.data.categories[0].items[0].contents").value("손목보호대를 착용했습니다"))
                .andExpect(jsonPath("$.data.categories[1].question_num").value(2))
                .andExpect(jsonPath("$.data.categories[1].input_type").value("CHECKBOX"))
                .andExpect(jsonPath("$.data.categories[1].required").value(false))
                .andExpect(jsonPath("$.data.categories[1].options").doesNotExist())
        }
    }

    @Nested
    @DisplayName("POST /api/v1/safety-check/submit - 제출")
    inner class SubmitTests {

        @Test
        @DisplayName("정상 제출 - 200 OK")
        fun submit_success() {
            // Given
            val submittedAt = LocalDateTime.of(2026, 3, 15, 9, 2, 30)
            val mockResponse = SafetyCheckSubmitResponse(
                submittedAt = submittedAt,
                safetyCheckCompleted = true
            )

            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any())).thenReturn(mockResponse)

            val requestJson = """
            {
                "start_time": "2026-03-15T09:00:00",
                "complete_time": "2026-03-15T09:02:30",
                "equipments": [
                    {"seq_num": 1, "answer": "예"},
                    {"seq_num": 2, "answer": "해당없음"},
                    {"seq_num": 3, "answer": "예"},
                    {"seq_num": 4, "answer": "예"},
                    {"seq_num": 5, "answer": "해당없음"},
                    {"seq_num": 6, "answer": "해당없음"},
                    {"seq_num": 7, "answer": "예"},
                    {"seq_num": 8, "answer": "예"},
                    {"seq_num": 9, "answer": "예"}
                ],
                "precautions": ["예방사항 1", "예방사항 3"]
            }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("안전점검이 완료되었습니다."))
                .andExpect(jsonPath("$.data.safety_check_completed").value(true))
                .andExpect(jsonPath("$.data.submitted_at").exists())
        }

        @Test
        @DisplayName("장비 항목 누락 - 400 INVALID_PARAMETER")
        fun submit_emptyEquipments() {
            val requestJson = """
            {
                "start_time": "2026-03-15T09:00:00",
                "complete_time": "2026-03-15T09:02:30",
                "equipments": [],
                "precautions": []
            }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("필수 항목 누락 - 400 REQUIRED_ITEMS_MISSING")
        fun submit_requiredItemsMissing() {
            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any()))
                .thenThrow(RequiredItemsMissingException())

            val requestJson = """
            {
                "start_time": "2026-03-15T09:00:00",
                "complete_time": "2026-03-15T09:02:30",
                "equipments": [{"seq_num": 1, "answer": "예"}, {"seq_num": 2, "answer": "예"}],
                "precautions": []
            }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REQUIRED_ITEMS_MISSING"))
        }

        @Test
        @DisplayName("중복 제출 - 409 ALREADY_SUBMITTED")
        fun submit_alreadySubmitted() {
            whenever(safetyCheckService.submitSafetyCheck(eq(1L), any()))
                .thenThrow(AlreadySubmittedException())

            val requestJson = """
            {
                "start_time": "2026-03-15T09:00:00",
                "complete_time": "2026-03-15T09:02:30",
                "equipments": [{"seq_num": 1, "answer": "예"}],
                "precautions": []
            }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/safety-check/submit")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ALREADY_SUBMITTED"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/safety-check/status - 안전점검 현황 조회")
    inner class GetStatusTests {

        @Test
        @DisplayName("성공 - 특정 날짜 조회")
        fun getStatus_withDate() {
            val response = SafetyCheckStatusResponse(
                date = "2026-03-21",
                totalCount = 2,
                submittedCount = 1,
                notSubmittedCount = 1,
                members = listOf(
                    MemberStatus(
                        id = 101L,
                        employeeCode = "123456",
                        employeeName = "김영희",
                        accountCode = "1234567890",
                        accountName = "이마트 강남점",
                        submitted = true,
                        submittedAt = LocalDateTime.of(2026, 3, 21, 8, 30, 0),
                        startTime = LocalDateTime.of(2026, 3, 21, 8, 25, 0),
                        equipments = listOf(
                            EquipmentStatus(1, "손목보호대 착용", "예"),
                            EquipmentStatus(2, "숨수건 소지", "예")
                        ),
                        yesCount = 7,
                        noCount = 2,
                        precautions = "소화기 위치 확인;비상구 확인",
                        precautionCount = 2,
                        workReportStatus = "Y"
                    ),
                    MemberStatus(
                        id = 102L,
                        employeeCode = "654321",
                        employeeName = "박철수",
                        accountCode = "9876543210",
                        accountName = "홈플러스 역삼점",
                        submitted = false,
                        submittedAt = null,
                        startTime = null,
                        equipments = emptyList(),
                        yesCount = 0,
                        noCount = 0,
                        precautions = null,
                        precautionCount = 0,
                        workReportStatus = null
                    )
                )
            )
            whenever(adminSafetyCheckService.getStatus(eq(1L), any())).thenReturn(response)

            mockMvc.perform(get("/api/v1/safety-check/status").param("date", "2026-03-21"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.date").value("2026-03-21"))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.submitted_count").value(1))
                .andExpect(jsonPath("$.data.not_submitted_count").value(1))
                .andExpect(jsonPath("$.data.members[0].employee_code").value("123456"))
                .andExpect(jsonPath("$.data.members[0].submitted").value(true))
                .andExpect(jsonPath("$.data.members[0].equipments[0].seq_num").value(1))
                .andExpect(jsonPath("$.data.members[1].submitted").value(false))
                .andExpect(jsonPath("$.data.members[1].equipments").isEmpty)
        }

        @Test
        @DisplayName("성공 - date 미지정 시 오늘 기준")
        fun getStatus_noDate() {
            val response = SafetyCheckStatusResponse(
                date = "2026-03-22",
                totalCount = 0,
                submittedCount = 0,
                notSubmittedCount = 0,
                members = emptyList()
            )
            whenever(adminSafetyCheckService.getStatus(eq(1L), any())).thenReturn(response)

            mockMvc.perform(get("/api/v1/safety-check/status"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.members").isEmpty)
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식")
        fun getStatus_invalidDateFormat() {
            mockMvc.perform(get("/api/v1/safety-check/status").param("date", "20260321"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_FORMAT"))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/safety-check/today - 오늘 여부 조회")
    inner class GetTodayStatusTests {

        @Test
        @DisplayName("오늘 완료 - 200 OK, completed=true")
        fun getTodayStatus_completed() {
            val submittedAt = LocalDateTime.of(2026, 3, 15, 9, 2, 30)
            val mockResponse = SafetyCheckTodayResponse(
                completed = true,
                submittedAt = submittedAt
            )

            whenever(safetyCheckService.getTodayStatus(1L)).thenReturn(mockResponse)

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
            val mockResponse = SafetyCheckTodayResponse(
                completed = false,
                submittedAt = null
            )

            whenever(safetyCheckService.getTodayStatus(1L)).thenReturn(mockResponse)

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
