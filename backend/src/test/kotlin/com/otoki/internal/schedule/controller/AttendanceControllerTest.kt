package com.otoki.internal.schedule.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.common.dto.response.StoreInfo
import com.otoki.internal.common.dto.response.StoreListResponse
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.schedule.dto.response.CommuteResponse
import com.otoki.internal.schedule.dto.response.CommuteStatusItem
import com.otoki.internal.schedule.dto.response.CommuteStatusResponse
import com.otoki.internal.schedule.exception.AlreadyRegisteredException
import com.otoki.internal.schedule.exception.DistanceExceededException
import com.otoki.internal.schedule.exception.ScheduleNotFoundException
import com.otoki.internal.schedule.service.AttendanceService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
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
    inner class RegisterCommuteTests {

        @Test
        @DisplayName("출근 등록 성공 - 200 OK, schedule_sfid 포함")
        fun registerCommute_success() {
            // Given
            val mockResponse = CommuteResponse(
                scheduleSfid = "SCH-001",
                storeName = "이마트 부산점",
                workType = "ROOM_TEMP",
                distanceKm = 0.12,
                totalCount = 5,
                registeredCount = 2
            )

            whenever(
                attendanceService.registerCommute(
                    eq(1L), eq("SCH-001"), eq(35.1234), eq(129.0567), eq("ROOM_TEMP")
                )
            ).thenReturn(mockResponse)

            val requestJson = """
                {
                    "schedule_sfid": "SCH-001",
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
                .andExpect(jsonPath("$.data.schedule_sfid").value("SCH-001"))
                .andExpect(jsonPath("$.data.store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.work_type").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.distance_km").value(0.12))
                .andExpect(jsonPath("$.data.total_count").value(5))
                .andExpect(jsonPath("$.data.registered_count").value(2))
        }

        @Test
        @DisplayName("거리 초과 - 400 DISTANCE_EXCEEDED")
        fun registerCommute_distanceExceeded() {
            // Given
            whenever(
                attendanceService.registerCommute(
                    eq(1L), eq("SCH-001"), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(DistanceExceededException(1.5))

            val requestJson = """
                {
                    "schedule_sfid": "SCH-001",
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
        @DisplayName("필수 필드 누락 (schedule_sfid 없음) - 400 INVALID_PARAMETER")
        fun registerCommute_missingScheduleSfid() {
            // Given - schedule_sfid 누락
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
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }

        @Test
        @DisplayName("이미 등록 - 409 ALREADY_REGISTERED")
        fun registerCommute_alreadyRegistered() {
            // Given
            whenever(
                attendanceService.registerCommute(
                    eq(1L), eq("SCH-001"), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(AlreadyRegisteredException())

            val requestJson = """
                {
                    "schedule_sfid": "SCH-001",
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
        @DisplayName("스케줄 없음 - 404 SCHEDULE_NOT_FOUND")
        fun registerCommute_scheduleNotFound() {
            // Given
            whenever(
                attendanceService.registerCommute(
                    eq(1L), eq("SCH-999"), eq(35.1234), eq(129.0567), anyOrNull()
                )
            ).thenThrow(ScheduleNotFoundException())

            val requestJson = """
                {
                    "schedule_sfid": "SCH-999",
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

    // ========== GET /api/v1/attendance/stores ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/stores - 거래처 목록 조회")
    inner class GetStoreListTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, latitude/longitude 포함")
        fun getStoreList_success() {
            // Given
            val mockResponse = StoreListResponse(
                stores = listOf(
                    StoreInfo(
                        scheduleSfid = "SCH-001",
                        storeSfid = "ACC-001",
                        storeName = "이마트 부산점",
                        storeTypeCode = "2110",
                        workCategory = "진열",
                        address = "부산시 해운대구 센텀2로 25",
                        latitude = 35.1696,
                        longitude = 129.1314,
                        isRegistered = false
                    ),
                    StoreInfo(
                        scheduleSfid = "SCH-002",
                        storeSfid = "ACC-002",
                        storeName = "홈플러스 서면점",
                        storeTypeCode = "2120",
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

            whenever(attendanceService.getStoreList(eq(1L), eq(null))).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/stores")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("조회 성공"))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores[0].schedule_sfid").value("SCH-001"))
                .andExpect(jsonPath("$.data.stores[0].store_sfid").value("ACC-001"))
                .andExpect(jsonPath("$.data.stores[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.stores[0].store_type_code").value("2110"))
                .andExpect(jsonPath("$.data.stores[0].work_category").value("진열"))
                .andExpect(jsonPath("$.data.stores[0].latitude").value(35.1696))
                .andExpect(jsonPath("$.data.stores[0].longitude").value(129.1314))
                .andExpect(jsonPath("$.data.stores[0].is_registered").value(false))
                .andExpect(jsonPath("$.data.stores[1].is_registered").value(true))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.registered_count").value(1))
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
        }

        @Test
        @DisplayName("키워드 검색 - 200 OK")
        fun getStoreList_withKeyword() {
            // Given
            val mockResponse = StoreListResponse(
                stores = listOf(
                    StoreInfo(
                        scheduleSfid = "SCH-001",
                        storeSfid = "ACC-001",
                        storeName = "이마트 부산점",
                        storeTypeCode = "2110",
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

            whenever(attendanceService.getStoreList(eq(1L), eq("이마트"))).thenReturn(mockResponse)

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/stores")
                    .param("keyword", "이마트")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.registered_count").value(0))
        }
    }

    // ========== GET /api/v1/attendance/status ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/status - 출근 현황 조회")
    inner class GetCommuteStatusTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, status_list 포함")
        fun getCommuteStatus_success() {
            // Given
            val mockResponse = CommuteStatusResponse(
                totalCount = 3,
                registeredCount = 1,
                statusList = listOf(
                    CommuteStatusItem(
                        scheduleSfid = "SCH-001",
                        storeName = "이마트 부산점",
                        workCategory = "진열",
                        status = "REGISTERED"
                    ),
                    CommuteStatusItem(
                        scheduleSfid = "SCH-002",
                        storeName = "홈플러스 서면점",
                        workCategory = "상시",
                        status = "PENDING"
                    ),
                    CommuteStatusItem(
                        scheduleSfid = "SCH-003",
                        storeName = "롯데마트 동래점",
                        workCategory = "상시",
                        status = "PENDING"
                    )
                ),
                currentDate = "2026-02-25"
            )

            whenever(attendanceService.getCommuteStatus(1L)).thenReturn(mockResponse)

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
                .andExpect(jsonPath("$.data.status_list[0].schedule_sfid").value("SCH-001"))
                .andExpect(jsonPath("$.data.status_list[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.status_list[0].work_category").value("진열"))
                .andExpect(jsonPath("$.data.status_list[0].status").value("REGISTERED"))
                .andExpect(jsonPath("$.data.status_list[1].schedule_sfid").value("SCH-002"))
                .andExpect(jsonPath("$.data.status_list[1].store_name").value("홈플러스 서면점"))
                .andExpect(jsonPath("$.data.status_list[1].status").value("PENDING"))
                .andExpect(jsonPath("$.data.current_date").value("2026-02-25"))
        }
    }
}
