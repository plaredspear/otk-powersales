package com.otoki.internal.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.dto.request.AttendanceRequest
import com.otoki.internal.dto.response.*
import com.otoki.internal.entity.UserRole
import com.otoki.internal.exception.*
import com.otoki.internal.security.JwtAuthenticationFilter
import com.otoki.internal.security.JwtTokenProvider
import com.otoki.internal.security.UserPrincipal
import com.otoki.internal.service.AttendanceService
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

    private val testPrincipal = UserPrincipal(userId = 1L, role = UserRole.USER)

    @BeforeEach
    fun setUp() {
        val authentication = UsernamePasswordAuthenticationToken(
            testPrincipal, null, testPrincipal.authorities
        )
        SecurityContextHolder.getContext().authentication = authentication
    }

    // ========== GET /api/v1/attendance/stores ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/stores - 거래처 목록 조회")
    inner class GetStoreListTests {

        @Test
        @DisplayName("정상 조회 - 200 OK, 거래처 목록 반환")
        fun getStoreList_success() {
            // Given
            val mockResponse = StoreListResponse(
                workerType = "PATROL",
                stores = listOf(
                    StoreInfo(
                        storeId = 101,
                        storeName = "이마트 부산점",
                        storeCode = "ST-00101",
                        workCategory = "진열",
                        address = "부산시 해운대구 센텀2로 25",
                        isRegistered = false,
                        registeredWorkType = null
                    ),
                    StoreInfo(
                        storeId = 102,
                        storeName = "홈플러스 서면점",
                        storeCode = "ST-00102",
                        workCategory = "상시",
                        address = "부산시 부산진구 부전동 168-7",
                        isRegistered = true,
                        registeredWorkType = "ROOM_TEMP"
                    )
                ),
                totalCount = 2,
                registeredCount = 1,
                currentDate = "2026-02-09"
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
                .andExpect(jsonPath("$.data.worker_type").value("PATROL"))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores[0].store_id").value(101))
                .andExpect(jsonPath("$.data.stores[0].store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.stores[0].store_code").value("ST-00101"))
                .andExpect(jsonPath("$.data.stores[0].is_registered").value(false))
                .andExpect(jsonPath("$.data.stores[0].registered_work_type").doesNotExist())
                .andExpect(jsonPath("$.data.stores[1].is_registered").value(true))
                .andExpect(jsonPath("$.data.stores[1].registered_work_type").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.registered_count").value(1))
                .andExpect(jsonPath("$.data.current_date").value("2026-02-09"))
        }

        @Test
        @DisplayName("키워드 검색 조회 - 200 OK")
        fun getStoreList_withKeyword() {
            // Given
            val mockResponse = StoreListResponse(
                workerType = "PATROL",
                stores = listOf(
                    StoreInfo(
                        storeId = 101,
                        storeName = "이마트 부산점",
                        storeCode = "ST-00101",
                        workCategory = "진열",
                        address = "부산시 해운대구",
                        isRegistered = false,
                        registeredWorkType = null
                    )
                ),
                totalCount = 1,
                registeredCount = 0,
                currentDate = "2026-02-09"
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
        }
    }

    // ========== POST /api/v1/attendance ==========

    @Nested
    @DisplayName("POST /api/v1/attendance - 출근등록")
    inner class RegisterAttendanceTests {

        @Test
        @DisplayName("정상 등록 - 201 Created")
        fun registerAttendance_success() {
            // Given
            val registeredAt = LocalDateTime.of(2026, 2, 9, 9, 5, 23)
            val mockResponse = AttendanceResponse(
                attendanceId = 1001,
                storeId = 101,
                storeName = "이마트 부산점",
                workType = "ROOM_TEMP",
                registeredAt = registeredAt,
                totalCount = 5,
                registeredCount = 2
            )

            whenever(attendanceService.registerAttendance(eq(1L), eq(101L), eq("ROOM_TEMP")))
                .thenReturn(mockResponse)

            val request = AttendanceRequest(storeId = 101L, workType = "ROOM_TEMP")

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("출근등록 완료"))
                .andExpect(jsonPath("$.data.attendance_id").value(1001))
                .andExpect(jsonPath("$.data.store_id").value(101))
                .andExpect(jsonPath("$.data.store_name").value("이마트 부산점"))
                .andExpect(jsonPath("$.data.work_type").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.total_count").value(5))
                .andExpect(jsonPath("$.data.registered_count").value(2))
        }

        @Test
        @DisplayName("storeId 누락 - 400 INVALID_PARAMETER")
        fun registerAttendance_missingStoreId() {
            // Given - storeId null
            val requestJson = """{"store_id": null, "work_type": "ROOM_TEMP"}"""

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
        @DisplayName("workType 누락 - 400 INVALID_PARAMETER")
        fun registerAttendance_missingWorkType() {
            // Given - workType 빈 문자열
            val requestJson = """{"store_id": 101, "work_type": ""}"""

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
        @DisplayName("유효하지 않은 workType - 400 INVALID_WORK_TYPE")
        fun registerAttendance_invalidWorkType() {
            // Given
            whenever(attendanceService.registerAttendance(eq(1L), eq(101L), eq("INVALID")))
                .thenThrow(InvalidWorkTypeException())

            val request = AttendanceRequest(storeId = 101L, workType = "INVALID")

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_WORK_TYPE"))
        }

        @Test
        @DisplayName("거래처 없음 - 404 STORE_NOT_FOUND")
        fun registerAttendance_storeNotFound() {
            // Given
            whenever(attendanceService.registerAttendance(eq(1L), eq(999L), eq("ROOM_TEMP")))
                .thenThrow(StoreNotFoundException())

            val request = AttendanceRequest(storeId = 999L, workType = "ROOM_TEMP")

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("STORE_NOT_FOUND"))
        }

        @Test
        @DisplayName("중복 등록 - 409 ALREADY_REGISTERED")
        fun registerAttendance_alreadyRegistered() {
            // Given
            whenever(attendanceService.registerAttendance(eq(1L), eq(101L), eq("ROOM_TEMP")))
                .thenThrow(AlreadyRegisteredException())

            val request = AttendanceRequest(storeId = 101L, workType = "ROOM_TEMP")

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ALREADY_REGISTERED"))
        }

        @Test
        @DisplayName("격고 한도 초과 - 409 REGISTRATION_LIMIT_EXCEEDED")
        fun registerAttendance_limitExceeded() {
            // Given
            whenever(attendanceService.registerAttendance(eq(1L), eq(103L), eq("ROOM_TEMP")))
                .thenThrow(RegistrationLimitExceededException())

            val request = AttendanceRequest(storeId = 103L, workType = "ROOM_TEMP")

            // When & Then
            mockMvc.perform(
                post("/api/v1/attendance")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("REGISTRATION_LIMIT_EXCEEDED"))
        }
    }

    // ========== GET /api/v1/attendance/status ==========

    @Nested
    @DisplayName("GET /api/v1/attendance/status - 현황 조회")
    inner class GetAttendanceStatusTests {

        @Test
        @DisplayName("정상 현황 조회 - 200 OK")
        fun getAttendanceStatus_success() {
            // Given
            val registeredAt = LocalDateTime.of(2026, 2, 9, 9, 5, 23)
            val mockResponse = AttendanceStatusResponse(
                totalCount = 3,
                registeredCount = 1,
                statusList = listOf(
                    AttendanceStatusInfo(
                        storeId = 101,
                        storeName = "이마트 부산점",
                        status = "COMPLETED",
                        workType = "ROOM_TEMP",
                        registeredAt = registeredAt
                    ),
                    AttendanceStatusInfo(
                        storeId = 102,
                        storeName = "홈플러스 서면점",
                        status = "PENDING",
                        workType = null,
                        registeredAt = null
                    ),
                    AttendanceStatusInfo(
                        storeId = 103,
                        storeName = "롯데마트 동래점",
                        status = "PENDING",
                        workType = null,
                        registeredAt = null
                    )
                ),
                currentDate = "2026-02-09"
            )

            whenever(attendanceService.getAttendanceStatus(1L)).thenReturn(mockResponse)

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
                .andExpect(jsonPath("$.data.status_list[0].store_id").value(101))
                .andExpect(jsonPath("$.data.status_list[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.status_list[0].work_type").value("ROOM_TEMP"))
                .andExpect(jsonPath("$.data.status_list[0].registered_at").exists())
                .andExpect(jsonPath("$.data.status_list[1].status").value("PENDING"))
                .andExpect(jsonPath("$.data.status_list[1].work_type").doesNotExist())
                .andExpect(jsonPath("$.data.status_list[1].registered_at").doesNotExist())
                .andExpect(jsonPath("$.data.current_date").value("2026-02-09"))
        }

        @Test
        @DisplayName("사용자 없음 - 404 USER_NOT_FOUND")
        fun getAttendanceStatus_userNotFound() {
            // Given
            whenever(attendanceService.getAttendanceStatus(1L))
                .thenThrow(UserNotFoundException())

            // When & Then
            mockMvc.perform(
                get("/api/v1/attendance/status")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }
    }
}
