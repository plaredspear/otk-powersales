package com.otoki.internal.common.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.common.dto.response.MyAccountInfo
import com.otoki.internal.common.dto.response.MyAccountListResponse
import com.otoki.internal.sap.entity.UserRole
import com.otoki.internal.auth.exception.EmployeeNotFoundException
import com.otoki.internal.common.exception.AccountInvalidParameterException
import com.otoki.internal.common.security.GpsConsentFilter
import com.otoki.internal.common.security.JwtAuthenticationFilter
import com.otoki.internal.admin.security.AdminAuthorityFilter
import com.otoki.internal.common.security.JwtTokenProvider
import com.otoki.internal.common.security.UserPrincipal
import com.otoki.internal.common.service.MyAccountService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AccountController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AccountController 테스트")
class AccountControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var myAccountService: MyAccountService

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

    @Nested
    @DisplayName("GET /api/v1/accounts/my - 내 거래처 목록 조회")
    inner class GetMyAccounts {

        @Test
        @DisplayName("정상 조회 - 거래처 목록 반환 (addressDetail 포함)")
        fun getMyAccounts_Success() {
            // given
            val accounts = listOf(
                MyAccountInfo(
                    accountId = 1L,
                    accountName = "경산농협",
                    accountCode = "STORE001",
                    address = "경북 경산시 중앙로 123",
                    addressDetail = "1층",
                    representativeName = "김영수",
                    phoneNumber = "053-123-4567"
                ),
                MyAccountInfo(
                    accountId = 2L,
                    accountName = "대구중앙마트",
                    accountCode = "STORE002",
                    address = "대구광역시 중구 동성로 456",
                    addressDetail = null,
                    representativeName = "이철수",
                    phoneNumber = "053-234-5678"
                )
            )
            val response = MyAccountListResponse(stores = accounts, totalCount = 2)

            whenever(myAccountService.getMyAccounts(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/accounts/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("내 거래처 목록 조회 성공"))
                .andExpect(jsonPath("$.data.total_count").value(2))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(2))
                .andExpect(jsonPath("$.data.stores[0].account_id").value(1))
                .andExpect(jsonPath("$.data.stores[0].account_name").value("경산농협"))
                .andExpect(jsonPath("$.data.stores[0].account_code").value("STORE001"))
                .andExpect(jsonPath("$.data.stores[0].address").value("경북 경산시 중앙로 123"))
                .andExpect(jsonPath("$.data.stores[0].address_detail").value("1층"))
                .andExpect(jsonPath("$.data.stores[0].representative_name").value("김영수"))
                .andExpect(jsonPath("$.data.stores[0].phone_number").value("053-123-4567"))
        }

        @Test
        @DisplayName("검색어 포함 조회 - 필터링된 결과 반환")
        fun getMyAccounts_WithKeyword_Success() {
            // given
            val keyword = "경산"
            val accounts = listOf(
                MyAccountInfo(
                    accountId = 1L,
                    accountName = "경산농협",
                    accountCode = "STORE001",
                    address = "경북 경산시 중앙로 123",
                    addressDetail = null,
                    representativeName = "김영수",
                    phoneNumber = "053-123-4567"
                )
            )
            val response = MyAccountListResponse(stores = accounts, totalCount = 1)

            whenever(myAccountService.getMyAccounts(eq(1L), eq(keyword)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/accounts/my")
                    .param("keyword", keyword)
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.total_count").value(1))
                .andExpect(jsonPath("$.data.stores[0].account_name").value("경산농협"))
        }

        @Test
        @DisplayName("결과 없음 - 빈 목록 반환")
        fun getMyAccounts_EmptyResult() {
            // given
            val response = MyAccountListResponse(stores = emptyList(), totalCount = 0)

            whenever(myAccountService.getMyAccounts(eq(1L), eq(null)))
                .thenReturn(response)

            // when & then
            mockMvc.perform(
                get("/api/v1/accounts/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.data.total_count").value(0))
                .andExpect(jsonPath("$.data.stores").isArray)
                .andExpect(jsonPath("$.data.stores.length()").value(0))
        }

        @Test
        @DisplayName("사용자 없음 - USER_NOT_FOUND 예외 발생")
        fun getMyAccounts_UserNotFound() {
            // given
            whenever(myAccountService.getMyAccounts(eq(1L), eq(null)))
                .thenThrow(EmployeeNotFoundException())

            // when & then
            mockMvc.perform(
                get("/api/v1/accounts/my")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("USER_NOT_FOUND"))
        }

        @Test
        @DisplayName("키워드 1자 - INVALID_PARAMETER 예외")
        fun getMyAccounts_keywordTooShort() {
            // given
            whenever(myAccountService.getMyAccounts(eq(1L), eq("가")))
                .thenThrow(AccountInvalidParameterException("검색 키워드는 2자 이상이어야 합니다"))

            // when & then
            mockMvc.perform(
                get("/api/v1/accounts/my")
                    .param("keyword", "가")
                    .contentType(MediaType.APPLICATION_JSON)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_PARAMETER"))
        }
    }
}
