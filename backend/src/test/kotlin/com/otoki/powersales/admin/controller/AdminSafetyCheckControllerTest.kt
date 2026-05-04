package com.otoki.powersales.admin.controller

import com.otoki.powersales.safetycheck.dto.response.EquipmentStatus
import com.otoki.powersales.safetycheck.dto.response.MemberStatus
import com.otoki.powersales.safetycheck.dto.response.SafetyCheckStatusResponse
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.safetycheck.service.AdminSafetyCheckService
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import com.otoki.powersales.common.security.UserPrincipal
import com.otoki.powersales.auth.entity.UserRole
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.LocalDateTime

@WebMvcTest(AdminSafetyCheckController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminSafetyCheckController 테스트")
class AdminSafetyCheckControllerTest {

    @Autowired private lateinit var mockMvc: MockMvc
    @MockitoBean private lateinit var adminSafetyCheckService: AdminSafetyCheckService
    @MockitoBean private lateinit var jwtTokenProvider: JwtTokenProvider
    @MockitoBean private lateinit var sapInboundAuditService: SapInboundAuditService
    @MockitoBean private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter
    @MockitoBean private lateinit var adminAuthorityFilter: AdminAuthorityFilter
    @MockitoBean private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        val principal = UserPrincipal(userId = 1L, role = UserRole.LEADER)
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/safety-check/status")
    inner class GetStatus {

        @Test
        @DisplayName("성공 - 특정 날짜 조회")
        fun getStatus_withDate() {
            val response = SafetyCheckStatusResponse(
                date = "2026-03-17",
                totalCount = 2,
                submittedCount = 1,
                notSubmittedCount = 1,
                members = listOf(
                    MemberStatus(
                        id = 42L,
                        employeeCode = "123456",
                        employeeName = "홍길동",
                        accountCode = "1234567890",
                        accountName = "이마트 강남점",
                        submitted = true,
                        submittedAt = LocalDateTime.of(2026, 3, 17, 9, 15, 30),
                        startTime = LocalDateTime.of(2026, 3, 17, 9, 10, 0),
                        equipments = listOf(
                            EquipmentStatus(1, "손목보호대 착용", "예"),
                            EquipmentStatus(2, "숨수건 소지", "예")
                        ),
                        yesCount = 7,
                        noCount = 2,
                        precautions = "매장 내 안전사고 유의",
                        precautionCount = 1,
                        workReportStatus = "출근"
                    ),
                    MemberStatus(
                        id = 55L,
                        employeeCode = "654321",
                        employeeName = "김영희",
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

            mockMvc.perform(get("/api/v1/admin/safety-check/status").param("date", "2026-03-17"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.date").value("2026-03-17"))
                .andExpect(jsonPath("$.data.totalCount").value(2))
                .andExpect(jsonPath("$.data.submittedCount").value(1))
                .andExpect(jsonPath("$.data.notSubmittedCount").value(1))
                .andExpect(jsonPath("$.data.members[0].employeeCode").value("123456"))
                .andExpect(jsonPath("$.data.members[0].accountCode").value("1234567890"))
                .andExpect(jsonPath("$.data.members[0].submitted").value(true))
                .andExpect(jsonPath("$.data.members[0].startTime").value("2026-03-17T09:10:00"))
                .andExpect(jsonPath("$.data.members[0].equipments[0].seqNum").value(1))
                .andExpect(jsonPath("$.data.members[0].equipments[0].label").value("손목보호대 착용"))
                .andExpect(jsonPath("$.data.members[0].equipments[0].answer").value("예"))
                .andExpect(jsonPath("$.data.members[0].yesCount").value(7))
                .andExpect(jsonPath("$.data.members[0].precautions").value("매장 내 안전사고 유의"))
                .andExpect(jsonPath("$.data.members[0].workReportStatus").value("출근"))
                .andExpect(jsonPath("$.data.members[1].submitted").value(false))
                .andExpect(jsonPath("$.data.members[1].startTime").isEmpty)
                .andExpect(jsonPath("$.data.members[1].equipments").isEmpty)
        }

        @Test
        @DisplayName("성공 - date 미지정 시 오늘 기준")
        fun getStatus_noDate() {
            val response = SafetyCheckStatusResponse(
                date = "2026-03-17",
                totalCount = 0,
                submittedCount = 0,
                notSubmittedCount = 0,
                members = emptyList()
            )
            whenever(adminSafetyCheckService.getStatus(eq(1L), any())).thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/safety-check/status"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.members").isEmpty)
        }

        @Test
        @DisplayName("실패 - 잘못된 날짜 형식")
        fun getStatus_invalidDateFormat() {
            mockMvc.perform(get("/api/v1/admin/safety-check/status").param("date", "20260317"))
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("INVALID_DATE_FORMAT"))
        }
    }
}
