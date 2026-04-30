package com.otoki.powersales.sap.inbound.controller

import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.sap.inbound.dto.attendance.AttendInfoDetail
import com.otoki.powersales.sap.inbound.dto.sales.ChunkResult
import com.otoki.powersales.sap.inbound.service.SapAttendInfoService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SapAttendInfoController::class)
@AutoConfigureMockMvc(addFilters = false)
@Import(SapInboundExceptionHandler::class)
@DisplayName("SapAttendInfoController 테스트")
class SapAttendInfoControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var sapAttendInfoService: SapAttendInfoService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @BeforeEach
    fun setUp() {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                "otoki-sap-client",
                null,
                listOf(SimpleGrantedAuthority("SCOPE_sap.attendance.write"))
            )
    }

    @Nested
    @DisplayName("POST /api/v1/sap/attend-info")
    inner class InsertAttendInfo {

        @Test
        @DisplayName("성공 - 200, RESULT_CODE 200, success_count 1, chunks success")
        fun insert_success() {
            whenever(sapAttendInfoService.insert(any())).thenReturn(
                AttendInfoDetail(
                    successCount = 1,
                    failureCount = 0,
                    failures = emptyList(),
                    chunks = listOf(ChunkResult(0, ChunkResult.STATUS_SUCCESS, 1))
                )
            )

            val payload = """
                {
                  "reqItemList": [
                    {
                      "EmployeeCode": "100123",
                      "StartDate": "20260427",
                      "EndDate": "20260427",
                      "AttendType": "14",
                      "Status": "정상"
                    }
                  ]
                }
            """.trimIndent()

            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(payload)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.RESULT_CODE").value("200"))
                .andExpect(jsonPath("$.RESULT_MSG").value("OK"))
                .andExpect(jsonPath("$.RESULT_DETAIL.success_count").value(1))
                .andExpect(jsonPath("$.RESULT_DETAIL.chunks[0].status").value("success"))
        }

        @Test
        @DisplayName("실패 - reqItemList 누락 -> INVALID_PAYLOAD")
        fun insert_missingReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"other": []}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapAttendInfoService, never()).insert(any())
        }

        @Test
        @DisplayName("실패 - reqItemList 빈 배열 -> INVALID_PAYLOAD")
        fun insert_emptyReqItemList() {
            mockMvc.perform(
                post("/api/v1/sap/attend-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reqItemList": []}""")
            )
                .andExpect(jsonPath("$.RESULT_CODE").value("INVALID_PAYLOAD"))

            verify(sapAttendInfoService, never()).insert(any())
        }
    }
}
