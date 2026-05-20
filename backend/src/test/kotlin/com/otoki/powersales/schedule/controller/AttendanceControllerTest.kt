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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
            val mockResponse = AttendanceRegisterResponse(
                scheduleId = 10L,
                accountName = "이마트 부산점",
                workType = "ROOM_TEMP",
                distanceKm = 0.0,
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

            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.scheduleId").value(10))
        }

        /**
         * 분기별 비즈니스 검증은 [com.otoki.powersales.schedule.service.AttendanceServiceTest] 가 담당.
         * controller 책임은 예외 → HTTP status + errorCode 매핑이므로 본 파라미터 테이블로 1테스트로 집약.
         */
        @ParameterizedTest(name = "{0} → {1} {2}")
        @MethodSource("com.otoki.powersales.schedule.controller.AttendanceControllerTest#registerExceptionCases")
        fun register_exceptionMapping(
            @Suppress("UNUSED_PARAMETER") caseName: String,
            expectedStatus: Int,
            expectedCode: String,
            exception: RuntimeException,
            latitude: Double
        ) {
            every {
                attendanceService.register(
                    eq(1L), eq(10L), null, null, eq(latitude), eq(129.0567), any()
                )
            } throws exception

            val requestJson = """
                {
                    "scheduleId": 10,
                    "latitude": $latitude,
                    "longitude": 129.0567
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }

        @Test
        @DisplayName("schedule_id, display_work_schedule_id 둘 다 없음 - 400 ATTENDANCE_TARGET_REQUIRED")
        fun register_bothNull_targetRequired() {
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

            mockMvc.perform(
                post("/api/v1/mobile/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(requestJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ATTENDANCE_TARGET_REQUIRED"))
        }
    }

    // ========== GET /api/v1/mobile/attendance/accounts ==========

    @Nested
    @DisplayName("GET /api/v1/mobile/attendance/accounts - 거래처 목록 조회")
    inner class GetAccountListTests {

        @Test
        @DisplayName("정상 조회 + 안전점검 완료 - 200 OK, safety_check_completed=true")
        fun getAccountList_success() {
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

            mockMvc.perform(
                get("/api/v1/mobile/attendance/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.safetyCheckCompleted").value(true))
                .andExpect(jsonPath("$.data.accounts[0].isRegistered").value(false))
                .andExpect(jsonPath("$.data.accounts[1].isRegistered").value(true))
                // Controller 가 추가하는 deadline / closed 필드는 service mock 응답에 없으므로
                // controller 가 후처리하는 것을 검증해야 함 — 유지
                .andExpect(jsonPath("$.data.registrationDeadline").value("17:00"))
                .andExpect(jsonPath("$.data.isRegistrationClosed").value(false))
        }

        @Test
        @DisplayName("키워드 검색 - 200 OK, service 에 keyword 전달")
        fun getAccountList_withKeyword() {
            val mockResponse = AccountListResponse(
                safetyCheckCompleted = false,
                accounts = emptyList(),
                totalCount = 0,
                registeredCount = 0,
                currentDate = "2026-02-25"
            )

            every { attendanceService.getAccountList(eq(1L), eq("이마트")) } returns mockResponse

            mockMvc.perform(
                get("/api/v1/mobile/attendance/accounts")
                    .param("keyword", "이마트")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
    }

    // ========== GET /api/v1/mobile/attendance/status ==========

    @Nested
    @DisplayName("GET /api/v1/mobile/attendance/status - 출근 현황 조회")
    inner class GetStatusTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, status_list 포함")
        fun getStatus_success() {
            val mockResponse = AttendanceStatusResponse(
                totalCount = 3,
                registeredCount = 1,
                statusList = listOf(
                    AttendanceStatusItem(1L, "이마트 부산점", "진열", "REGISTERED"),
                    AttendanceStatusItem(2L, "홈플러스 서면점", "상시", "PENDING"),
                    AttendanceStatusItem(3L, "롯데마트 동래점", "상시", "PENDING")
                ),
                currentDate = "2026-02-25"
            )

            every { attendanceService.getStatus(1L) } returns mockResponse

            mockMvc.perform(
                get("/api/v1/mobile/attendance/status")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalCount").value(3))
                .andExpect(jsonPath("$.data.statusList").isArray)
        }
    }

    companion object {
        @JvmStatic
        fun registerExceptionCases(): List<Arguments> = listOf(
            Arguments.of("안전점검 미완료", 400, "SAFETY_CHECK_REQUIRED", SafetyCheckRequiredException(), 35.1234),
            Arguments.of("거리 초과", 400, "ATT_GPS_DISTANCE_EXCEEDED", DistanceExceededException(), 35.1234),
            Arguments.of("거래처 좌표 누락", 400, "ATT_ACCOUNT_COORDS_MISSING", AccountCoordsMissingException(), 35.1234),
            Arguments.of("사원 좌표 무효", 400, "ATT_INVALID_COORDS", InvalidCoordsException(), 91.0),
            Arguments.of("이미 등록", 409, "ALREADY_REGISTERED", AlreadyRegisteredException(), 35.1234),
            Arguments.of("17시 이후", 400, "ATTENDANCE_TIME_EXCEEDED", AttendanceTimeExceededException(), 35.1234),
            Arguments.of("스케줄 없음", 404, "SCHEDULE_NOT_FOUND", TeamMemberScheduleNotFoundException(), 35.1234)
        )
    }
}
