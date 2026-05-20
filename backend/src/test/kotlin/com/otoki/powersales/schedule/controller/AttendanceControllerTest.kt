package com.otoki.powersales.schedule.controller

import tools.jackson.databind.ObjectMapper
import com.otoki.powersales.common.dto.response.AccountInfo
import com.otoki.powersales.common.dto.response.AccountListResponse
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.schedule.dto.response.AttendanceRegisterResponse
import com.otoki.powersales.schedule.dto.response.AttendanceStatusItem
import com.otoki.powersales.schedule.dto.response.AttendanceStatusResponse
import com.otoki.powersales.schedule.exception.*
import com.otoki.powersales.schedule.service.AttendanceService
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
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

    @MockkBean
    private lateinit var attendanceService: AttendanceService

    @MockkBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockkBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockkBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockkBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.WOMAN)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== POST /api/v1/mobile/attendance ==========

    @Nested
    @DisplayName("POST /api/v1/mobile/attendance - 출근등록")
    inner class RegisterTests {

        @Test
        @DisplayName("출근 등록 성공 - 200 OK, schedule_id 포함")
        fun register_success() {
            // Given
            val mockResponse = AttendanceRegisterResponse(
                scheduleId = 10L,
                accountName = "이마트 부산점",
                workType = "ROOM_TEMP",
                distanceKm = 0.0, // Spec #585 Q4: 응답에 실제 거리 미노출
                totalCount = 5,
                registeredCount = 2
            )

            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), eq("ROOM_TEMP")
                )
            } returns mockResponse

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567,
                    "workType": "ROOM_TEMP"
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("출근등록 완료"))
                .andExpect(jsonPath("$.data.scheduleId").value(10))
                .andExpect(jsonPath("$.data.accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.workType").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.distanceKm").value(0.0))
                .andExpect(jsonPath("$.data.totalCount").value(5))
                .andExpect(jsonPath("$.data.registeredCount").value(2))
        }

        @Test
        @DisplayName("안전점검 미완료 - 400 SAFETY_CHECK_REQUIRED")
        fun register_safetyCheckRequired() {
            // Given
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws SafetyCheckRequiredException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SAFETY_CHECK_REQUIRED"))
        }

        @Test
        @DisplayName("거리 초과 - 400 ATT_GPS_DISTANCE_EXCEEDED")
        fun register_distanceExceeded() {
            // Given
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws DistanceExceededException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ATT_GPS_DISTANCE_EXCEEDED"))
        }

        @Test
        @DisplayName("거래처 좌표 누락 - 400 ATT_ACCOUNT_COORDS_MISSING")
        fun register_accountCoordsMissing() {
            // Given
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws AccountCoordsMissingException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ATT_ACCOUNT_COORDS_MISSING"))
        }

        @Test
        @DisplayName("사원 위치 좌표 무효 - 400 ATT_INVALID_COORDS")
        fun register_invalidCoords() {
            // Given
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(91.0), eq(129.0567), any()
                )
} throws InvalidCoordsException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 91.0,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ATT_INVALID_COORDS"))
        }

        @Test
        @DisplayName("schedule_id, display_work_schedule_id 둘 다 없음 - 400 ATTENDANCE_TARGET_REQUIRED")
        fun register_bothNull_targetRequired() {
            // Given - 둘 다 누락 → 서비스에서 AttendanceTargetRequiredException
            every {
                attendanceService.register(
                    eq(1L), null, null, null, eq(35.1234), eq(129.0567), any()
                )
} throws AttendanceTargetRequiredException()

            val requestJson = """
                {
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
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
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws AlreadyRegisteredException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
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
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws AttendanceTimeExceededException()

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
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
            every {
                attendanceService.register(
                    eq(1L), eq(99999L), null, null, eq(35.1234), eq(129.0567), any()
                )
} throws TeamMemberScheduleNotFoundException()

            val requestJson = """
                {
                    "scheduleId": 99999,
                    "latitude": 35.1234,
                    "longitude": 129.0567
                }
            """.trimIndent()

            // When & Then
            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("SCHEDULE_NOT_FOUND"))
        }
    }

    // ========== GET /api/v1/mobile/attendance/accounts ==========

    @Nested
    @DisplayName("GET /api/v1/mobile/attendance/accounts - 거래처 목록 조회")
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

            every { attendanceService.getAccountList(eq(1L), null) } returns mockResponse

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/attendance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.safetyCheckCompleted").value(true))
                .andExpect(jsonPath("$.data.accounts").isArray)
                .andExpect(jsonPath("$.data.accounts[0].scheduleId").value(1))
                .andExpect(jsonPath("$.data.accounts[0].accountId").value(1))
                .andExpect(jsonPath("$.data.accounts[0].accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.accounts[0].accountTypeCode").value("2110"))
                .andExpect(jsonPath("$.data.accounts[0].workCategory").value("진열"))
                .andExpect(jsonPath("$.data.accounts[0].latitude").value(35.1696))
                .andExpect(jsonPath("$.data.accounts[0].longitude").value(129.1314))
                .andExpect(jsonPath("$.data.accounts[0].isRegistered").value(false))
                .andExpect(jsonPath("$.data.accounts[1].isRegistered").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.registeredCount").value(1))
                .andExpect(jsonPath("$.data.currentDate").value("2026-02-25"))
                .andExpect(jsonPath("$.data.registrationDeadline").value("17:00"))
                .andExpect(jsonPath("$.data.isRegistrationClosed").value(false))
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

            every { attendanceService.getAccountList(eq(1L), eq("이마트")) } returns mockResponse

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/attendance/accounts")
                    .param("keyword", "이마트")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.safetyCheckCompleted").value(false))
                .andExpect(jsonPath("$.data.accounts").isArray)
                .andExpect(jsonPath("$.data.accounts[0].accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.totalCount").value(1))
                .andExpect(jsonPath("$.data.registeredCount").value(0))
        }
    }

    // ========== GET /api/v1/mobile/attendance/status ==========

    @Nested
    @DisplayName("GET /api/v1/mobile/attendance/status - 출근 현황 조회")
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

            every { attendanceService.getStatus(1L) } returns mockResponse

            // When & Then
            mockMvc.perform(
                get("/api/v1/mobile/attendance/status")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.registeredCount").value(1))
                .andExpect(jsonPath("$.data.statusList").isArray)
                .andExpect(jsonPath("$.data.statusList[0].scheduleId").value(1))
                .andExpect(jsonPath("$.data.statusList[0].accountName").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.statusList[0].workCategory").value("진열"))
                .andExpect(jsonPath("$.data.statusList[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.statusList[1].scheduleId").value(2))
                .andExpect(jsonPath("$.data.statusList[1].accountName").value("홈플러스 서면점"))
                .andExpect(jsonPath("$.data.statusList[1].status").value("PENDING"))
                .andExpect(jsonPath("$.data.currentDate").value("2026-02-25"))
        }
    }
}
