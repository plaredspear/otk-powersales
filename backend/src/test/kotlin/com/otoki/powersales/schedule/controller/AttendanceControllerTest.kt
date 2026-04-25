package com.otoki.powersales.schedule.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.dto.response.AccountInfo
import com.otoki.powersales.common.dto.response.AccountListResponse
import com.otoki.powersales.sap.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.schedule.dto.response.AttendanceStatusItem
import com.otoki.powersales.schedule.dto.response.AttendanceStatusResponse
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.service.AttendanceService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AttendanceController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AttendanceController 테스트")
class AttendanceControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var attendanceService: AttendanceService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter

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

    // ========== POST /api/v1/attendance ==========

    @Nested
    @DisplayName("POST /api/v1/attendance - 출근등록")
    inner class RegisterTests {

        @Test
        @DisplayName("출근 등록 성공 - 200 OK, schedule_id 포함")
        fun register_success() {
            // Given
            val mockResponse = AttendanceRegisterResponse(
                scheduleId = 10L,
                accountName = "이마트 부산점",
                workType = "ROOM_TEMP",
                distanceKm = 0.12,
                totalCount = 5,
                registeredCount = 2
            )

            whenever(
                attendanceService.register(
                    eq(1L), eq(10L), eq(null), eq(35.1234), eq(129.0567), eq("ROOM_TEMP")
                )
            ).thenReturn(mockResponse)

            val requestJson = """
                {
                    "schedule_id": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567,
                    "work_type": "ROOM_TEMP"
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("출근등록 완료"))
                .andExpect(jsonPath("$.data.schedule_id").value(10))
                .andExpect(jsonPath("$.data.account_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.work_type").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.distance_km").value(0.12))
                .andExpect(jsonPath("$.data.total_count").value(5))
                .andExpect(jsonPath("$.data.registered_count").value(2))
        }

        @Test
        @DisplayName("안전점검 미완료 - 400 SAFETY_CHECK_REQUIRED")
        fun register_safetyCheckRequired() {
            // Given
            whenever(
                attendanceService.register(
                    eq(1L), eq(10L), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(SafetyCheckRequiredException())

            val requestJson = """
                {
                    "schedule_id": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SAFETY_CHECK_REQUIRED"))
        }

        @Test
        @DisplayName("거리 초과 - 400 DISTANCE_EXCEEDED")
        fun register_distanceExceeded() {
            // Given
            whenever(
                attendanceService.register(
                    eq(1L), eq(10L), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(DistanceExceededException(1.5))

            val requestJson = """
                {
                    "schedule_id": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("DISTANCE_EXCEEDED"))
        }

        @Test
        @DisplayName("schedule_id, display_work_schedule_id 둘 다 없음 - 400 ATTENDANCE_TARGET_REQUIRED")
        fun register_bothNull_targetRequired() {
            // Given - 둘 다 누락 → 서비스에서 AttendanceTargetRequiredException
            whenever(
                attendanceService.register(
                    eq(1L), eq(null), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(AttendanceTargetRequiredException())

            val requestJson = """
                {
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ATTENDANCE_TARGET_REQUIRED"))
        }

        @Test
        @DisplayName("이미 등록 - 409 ALREADY_REGISTERED")
        fun register_alreadyRegistered() {
            // Given
            whenever(
                attendanceService.register(
                    eq(1L), eq(10L), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(AlreadyRegisteredException())

            val requestJson = """
                {
                    "schedule_id": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ALREADY_REGISTERED"))
        }

        @Test
        @DisplayName("17시 이후 등록 - 400 ATTENDANCE_TIME_EXCEEDED")
        fun register_timeExceeded() {
            // Given
            whenever(
                attendanceService.register(
                    eq(1L), eq(10L), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(AttendanceTimeExceededException())

            val requestJson = """
                {
                    "schedule_id": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ATTENDANCE_TIME_EXCEEDED"))
        }

        @Test
        @DisplayName("스케줄 없음 - 404 SCHEDULE_NOT_FOUND")
        fun register_scheduleNotFound() {
            // Given
            whenever(
                attendanceService.register(
                    eq(1L), eq(99999L), eq(null), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(TeamMemberScheduleNotFoundException())

            val requestJson = """
                {
                    "schedule_id": 99999,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"))
        }
    }

    // ========== GET /api/v1/attendance/accounts ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/accounts - 거래처 목록 조회")
    inner class GetAccountListTests {

        @Test
        @DisplayName("정상 조회 + 안전점검 완료 - 200 OK, safety_check_completed=true")
        fun getAccountList_success() {
            // Given
            val mockResponse = AccountListResponse(
                safetyCheckCompleted = true,
                accounts = listOf(
                    AccountInfo(
                        scheduleId = 1L,
                        accountId = 1,
                        accountName = "이마트 부산점",
                        accountTypeCode = "2110",
                        workCategory = "진열",
                        address = "부산시 해운대구 센텀2로 25",
                        latitude = 35.1696,
                        longitude = 129.1314,
                        isRegistered = false
                    ),
                    AccountInfo(
                        scheduleId = 2L,
                        accountId = 2,
                        accountName = "홈플러스 서면점",
                        accountTypeCode = "2120",
                        workCategory = "상시",
                        address = "부산시 부산진구 부전동 168-7",
                        latitude = 35.1577,
                        longitude = 129.0602,
                        isRegistered = true
                    )
                ),
                totalCount = 2,
                registeredCount = 1,
                currentDate = "2026-02-25"
            )

            whenever(attendanceService.getAccountList(eq(1L), eq(null))).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.safety_check_completed").value(true))
                .andExpect(jsonPath("$.data.accounts").isArray)
                .andExpect(jsonPath("$.data.accounts[0].schedule_id").value(1))
                .andExpect(jsonPath("$.data.accounts[0].account_id").value(1))
                .andExpect(jsonPath("$.data.accounts[0].account_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.accounts[0].account_type_code").value("2110"))
                .andExpect(jsonPath("$.data.accounts[0].work_category").value("진열"))
                .andExpect(jsonPath("$.data.accounts[0].latitude").value(35.1696))
                .andExpect(jsonPath("$.data.accounts[0].longitude").value(129.1314))
                .andExpect(jsonPath("$.data.accounts[0].is_registered").value(false))
                .andExpect(jsonPath("$.data.accounts[1].is_registered").value(true))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.registered_count").value(1))
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
                .andExpect(jsonPath("$.data.registration_deadline").value("17:00"))
                .andExpect(jsonPath("$.data.is_registration_closed").value(false))
        }

        @Test
        @DisplayName("키워드 검색 - 200 OK")
        fun getAccountList_withKeyword() {
            // Given
            val mockResponse = AccountListResponse(
                safetyCheckCompleted = false,
                accounts = listOf(
                    AccountInfo(
                        scheduleId = 1L,
                        accountId = 1,
                        accountName = "이마트 부산점",
                        accountTypeCode = "2110",
                        workCategory = "진열",
                        address = "부산시 해운대구 센텀2로 25",
                        latitude = 35.1696,
                        longitude = 129.1314,
                        isRegistered = false
                    )
                ),
                totalCount = 1,
                registeredCount = 0,
                currentDate = "2026-02-25"
            )

            whenever(attendanceService.getAccountList(eq(1L), eq("이마트"))).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/accounts")
                    .param("keyword", "이마트")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.safety_check_completed").value(false))
                .andExpect(jsonPath("$.data.accounts").isArray)
                .andExpect(jsonPath("$.data.accounts[0].account_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.registered_count").value(0))
        }
    }

    // ========== GET /api/v1/attendance/status ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/status - 출근 현황 조회")
    inner class GetStatusTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, status_list 포함")
        fun getStatus_success() {
            // Given
            val mockResponse = AttendanceStatusResponse(
                totalCount = 3,
                registeredCount = 1,
                statusList = listOf(
                    AttendanceStatusItem(
                        scheduleId = 1L,
                        accountName = "이마트 부산점",
                        workCategory = "진열",
                        status = "REGISTERED"
                    ),
                    AttendanceStatusItem(
                        scheduleId = 2L,
                        accountName = "홈플러스 서면점",
                        workCategory = "상시",
                        status = "PENDING"
                    ),
                    AttendanceStatusItem(
                        scheduleId = 3L,
                        accountName = "롯데마트 동래점",
                        workCategory = "상시",
                        status = "PENDING"
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(attendanceService.getStatus(1L)).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/status")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(3))
                .andExpect(jsonPath("$.data.registered_count").value(1))
                .andExpect(jsonPath("$.data.status_list").isArray)
                .andExpect(jsonPath("$.data.status_list[0].schedule_id").value(1))
                .andExpect(jsonPath("$.data.status_list[0].account_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.status_list[0].work_category").value("진열"))
                .andExpect(jsonPath("$.data.status_list[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.status_list[1].schedule_id").value(2))
                .andExpect(jsonPath("$.data.status_list[1].account_name").value("홈플러스 서면점"))
                .andExpect(jsonPath("$.data.status_list[1].status").value("PENDING"))
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
        }
    }
}
