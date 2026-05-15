package com.otoki.powersales.admin.controller

import com.otoki.powersales.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.account.dto.response.AccountListItem
import com.otoki.powersales.account.dto.response.AccountListResponse
import com.otoki.powersales.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.account.exception.AccountNameDuplicateException
import com.otoki.powersales.account.exception.AccountNamePrefixRequiredException
import com.otoki.powersales.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.account.exception.AccountNotFoundException
import com.otoki.powersales.account.service.AccountCreateService
import com.otoki.powersales.account.service.AccountDeleteService
import com.otoki.powersales.account.service.AccountUpdateService
import com.otoki.powersales.account.service.AdminAccountService
import com.otoki.powersales.admin.scope.DataScopeHolder
import com.otoki.powersales.admin.security.AdminAuthorityFilter
import com.otoki.powersales.auth.entity.UserRole
import com.otoki.powersales.common.security.GpsConsentFilter
import com.otoki.powersales.common.security.JwtAuthenticationFilter
import com.otoki.powersales.common.security.JwtTokenProvider
import com.otoki.powersales.auth.web.WebUserPrincipal
import com.otoki.powersales.user.entity.ProfileType
import com.otoki.powersales.sap.auth.audit.SapInboundAuditService
import tools.jackson.databind.ObjectMapper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAccountController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAccountController 테스트")
class AdminAccountControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockitoBean
    private lateinit var adminAccountService: AdminAccountService

    @MockitoBean
    private lateinit var accountCreateService: AccountCreateService

    @MockitoBean
    private lateinit var accountUpdateService: AccountUpdateService

    @MockitoBean
    private lateinit var accountDeleteService: AccountDeleteService

    @MockitoBean
    private lateinit var jwtTokenProvider: JwtTokenProvider

    @MockitoBean
    private lateinit var sapInboundAuditService: SapInboundAuditService

    @MockitoBean
    private lateinit var jwtAuthenticationFilter: JwtAuthenticationFilter

    @MockitoBean
    private lateinit var adminAuthorityFilter: AdminAuthorityFilter

    @MockitoBean
    private lateinit var gpsConsentFilter: GpsConsentFilter

    @MockitoBean
    private lateinit var dataScopeHolder: DataScopeHolder

    @BeforeEach
    fun setUp() {
        val principal = WebUserPrincipal(
            userId = 100L,
            usernameValue = "test@otokims.co.kr",
            employeeNumber = "S001",
            employeeId = 1L,
            role = UserRole.BRANCH_MANAGER,
            profileType = ProfileType.STAFF,
            isSalesSupport = false,
            passwordChangeRequired = false,
            encodedPassword = "",
            grantedAuthorities = emptyList(),
            active = true
        )
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(principal, null, principal.authorities)
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts - 거래처 목록 조회")
    inner class GetAccounts {

        @Test
        @DisplayName("성공 - 기본 조회")
        fun getAccounts_success() {
            val response = AccountListResponse(
                content = listOf(
                    AccountListItem(
                        id = 1,
                        externalKey = "AC001234",
                        name = "GS25 역삼점",
                        abcType = "편의점",
                        branchCode = "A001",
                        branchName = "서울1지점",
                        employeeCode = "123456",
                        address1 = "서울시 강남구 역삼동 123-4",
                        phone = "02-1234-5678",
                        accountStatusName = "활성"
                    )
                ),
                page = 0,
                size = 20,
                totalElements = 1,
                totalPages = 1
            )
            whenever(adminAccountService.getAccounts(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray)
                .andExpect(jsonPath("$.data.content[0].externalKey").value("AC001234"))
                .andExpect(jsonPath("$.data.content[0].name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.content[0].abcType").value("편의점"))
                .andExpect(jsonPath("$.data.content[0].branchCode").value("A001"))
                .andExpect(jsonPath("$.data.content[0].branchName").value("서울1지점"))
                .andExpect(jsonPath("$.data.content[0].employeeCode").value("123456"))
                .andExpect(jsonPath("$.data.content[0].address1").value("서울시 강남구 역삼동 123-4"))
                .andExpect(jsonPath("$.data.content[0].phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.data.content[0].accountStatusName").value("활성"))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getAccounts_withFilters() {
            val response = AccountListResponse(
                content = emptyList(),
                page = 0,
                size = 10,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminAccountService.getAccounts(eq("GS25"), eq("편의점"), eq("A001"), eq("활성"), eq(0), eq(10)))
                .thenReturn(response)

            mockMvc.perform(
                get("/api/v1/admin/accounts")
                    .param("keyword", "GS25")
                    .param("abcType", "편의점")
                    .param("branchCode", "A001")
                    .param("accountStatusName", "활성")
                    .param("page", "0")
                    .param("size", "10")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
        }

        @Test
        @DisplayName("성공 - 빈 결과")
        fun getAccounts_empty() {
            val response = AccountListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0
            )
            whenever(adminAccountService.getAccounts(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
                .thenReturn(response)

            mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isEmpty)
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.totalPages").value(0))
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/accounts - 신규 거래처 등록 (Spec #640)")
    inner class CreateAccount {

        @Test
        @DisplayName("C1 성공 - 정상 등록 (201 Created + camelCase 응답)")
        fun createAccount_success() {
            val request = AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = "100123")
            val response = AdminAccountCreateResponse(
                id = 1234,
                name = "(신규) 강남점",
                accountGroup = "9999",
                employeeCode = "100123",
                branchCode = "C001",
                branchName = "강남지점"
            )
            whenever(accountCreateService.create(any())).thenReturn(response)

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1234))
                .andExpect(jsonPath("$.data.name").value("(신규) 강남점"))
                .andExpect(jsonPath("$.data.accountGroup").value("9999"))
                .andExpect(jsonPath("$.data.employeeCode").value("100123"))
                .andExpect(jsonPath("$.data.branchCode").value("C001"))
                .andExpect(jsonPath("$.data.branchName").value("강남지점"))
                .andExpect(jsonPath("$.message").value("거래처 등록 성공"))
        }

        @Test
        @DisplayName("C4 실패 - name blank → 400 (validation)")
        fun createAccount_nameBlank() {
            val rawJson = """{"name":"","employeeCode":"100123"}"""

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(rawJson)
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("C5 실패 - 동일명 등록 시도 → 409 ACCOUNT_NAME_DUPLICATE")
        fun createAccount_duplicate() {
            val request = AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = "100123")
            whenever(accountCreateService.create(any())).thenThrow(AccountNameDuplicateException())

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.error.message").value("동일한 이름의 거래처가 이미 존재합니다."))
        }

        @Test
        @DisplayName("C6 실패 - prefix 미포함 → 400 ACCOUNT_NAME_PREFIX_REQUIRED + 메시지 정합")
        fun createAccount_prefixMissing() {
            val request = AdminAccountCreateRequest(name = "강남점", employeeCode = "100123")
            whenever(accountCreateService.create(any()))
                .thenThrow(AccountNamePrefixRequiredException("(신규)/(기타)"))

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_PREFIX_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("신규 거래처 등록은 ((신규)/(기타)) 중 1개를 필수로 입력하셔야 합니다."))
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/admin/accounts/{id} - 거래처 수정 (Spec #643)")
    inner class UpdateAccount {

        @Test
        @DisplayName("C1 성공 - 정상 수정 (200 OK + camelCase 응답 + ApiResponse wrapper + data.id 정합)")
        fun updateAccount_success() {
            val request = AdminAccountUpdateRequest(
                name = "(신규) 강남점 신호 수정",
                address1 = "서울특별시 강남구 테헤란로 100",
                phone = "02-1234-5678"
            )
            val response = AdminAccountUpdateResponse(
                id = 1234,
                name = "(신규) 강남점 신호 수정",
                accountGroup = "9999",
                employeeCode = "100123",
                branchCode = "C001",
                branchName = "강남지점",
                address1 = "서울특별시 강남구 테헤란로 100",
                address2 = null,
                zipCode = null,
                phone = "02-1234-5678",
                mobilePhone = null,
                representative = null,
                email = null,
                fax = null,
                website = null,
                industry = null,
                description = null,
                businessLicenseNumber = null,
                businessType = null,
                businessCategory = null,
                abcType = "A",
                abcTypeCode = null,
                accountType = null,
                accountStatusName = null,
                accountStatusCode = null,
                accountNumber = null,
                site = null,
                accountSource = null,
                mapCoordinate = null,
                parentSfid = null,
                rating = null,
                ownership = null,
                freezerInstalled = null,
                freezerType = null,
                firstInstalled = null,
                orderEndTime = null,
                closingTime1 = "18:00",
                closingTime2 = null,
                closingTime3 = null,
                remainingCredit = null,
                totalCredit = null,
                annualRevenue = null,
                numberOfEmployees = null,
                consignmentAcc = null,
                distribution = null
            )
            whenever(accountUpdateService.update(eq(1234), any(), any())).thenReturn(response)

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1234))
                .andExpect(jsonPath("$.data.name").value("(신규) 강남점 신호 수정"))
                .andExpect(jsonPath("$.data.accountGroup").value("9999"))
                .andExpect(jsonPath("$.data.employeeCode").value("100123"))
                .andExpect(jsonPath("$.data.branchCode").value("C001"))
                .andExpect(jsonPath("$.data.branchName").value("강남지점"))
                .andExpect(jsonPath("$.data.address1").value("서울특별시 강남구 테헤란로 100"))
                .andExpect(jsonPath("$.data.phone").value("02-1234-5678"))
                .andExpect(jsonPath("$.data.abcType").value("A"))
                .andExpect(jsonPath("$.data.closingTime1").value("18:00"))
                .andExpect(jsonPath("$.message").value("거래처 수정 성공"))
        }

        @Test
        @DisplayName("C2 실패 - 비존재 id → 404 ACCOUNT_NOT_FOUND + 메시지에 id 포함")
        fun updateAccount_notFound() {
            org.mockito.kotlin.doThrow(AccountNotFoundException(9999))
                .whenever(accountUpdateService).update(eq(9999), any(), any())

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 9999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"(신규) 무효"}""")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("거래처를 찾을 수 없습니다: 9999"))
        }

        @Test
        @DisplayName("C3 실패 - prefix 위반 → 400 ACCOUNT_NAME_PREFIX_REQUIRED + 메시지 '거래처 수정은 ...'")
        fun updateAccount_prefixMissing() {
            org.mockito.kotlin.doThrow(AccountNamePrefixRequiredForUpdateException("(신규)/(기타)"))
                .whenever(accountUpdateService).update(eq(1234), any(), any())

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"강남점"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_PREFIX_REQUIRED"))
                .andExpect(jsonPath("$.error.message").value("거래처 수정은 ((신규)/(기타)) 중 1개를 필수로 입력하셔야 합니다."))
        }

        @Test
        @DisplayName("C4 실패 - 동일명 중복 → 409 ACCOUNT_NAME_DUPLICATE")
        fun updateAccount_duplicate() {
            org.mockito.kotlin.doThrow(AccountNameDuplicateException())
                .whenever(accountUpdateService).update(eq(1234), any(), any())

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"(신규) 다른지점"}""")
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_DUPLICATE"))
                .andExpect(jsonPath("$.error.message").value("동일한 이름의 거래처가 이미 존재합니다."))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/accounts/{id} - 거래처 삭제 (Spec #642)")
    inner class DeleteAccount {

        @Test
        @DisplayName("C1 성공 - 정상 삭제 (200 OK + camelCase 응답)")
        fun deleteAccount_success() {
            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 1234))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래처 삭제 성공"))
        }

        @Test
        @DisplayName("C4 실패 - SAP 동기 거래처 삭제 시도 → 409 ACCOUNT_DELETE_BLOCKED_SAP_SYNCED")
        fun deleteAccount_sapSyncedBlocked() {
            org.mockito.kotlin.doThrow(AccountDeleteBlockedSapSyncedException())
                .whenever(accountDeleteService).delete(eq(1234))

            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 1234))
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_DELETE_BLOCKED_SAP_SYNCED"))
                .andExpect(jsonPath("$.error.message").value("거래처 코드가 있는 거래처는 삭제할 수 없습니다."))
        }

        @Test
        @DisplayName("C5 실패 - 존재하지 않는 id → 404 ACCOUNT_NOT_FOUND")
        fun deleteAccount_notFound() {
            org.mockito.kotlin.doThrow(AccountNotFoundException())
                .whenever(accountDeleteService).delete(eq(9999))

            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 9999))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("거래처를 찾을 수 없습니다."))
        }

        @Test
        @DisplayName("C6 실패 - 이미 삭제된 id 재요청 → 404 ACCOUNT_NOT_FOUND (멱등)")
        fun deleteAccount_alreadyDeletedIdempotent() {
            org.mockito.kotlin.doThrow(AccountNotFoundException())
                .whenever(accountDeleteService).delete(eq(1234))

            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 1234))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
        }
    }
}
