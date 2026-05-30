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
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.common.test.AdminControllerTestSupport
import tools.jackson.databind.ObjectMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(AdminAccountController::class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("AdminAccountController 테스트")
class AdminAccountControllerTest : AdminControllerTestSupport() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean private lateinit var adminAccountService: AdminAccountService
    @MockkBean private lateinit var accountCreateService: AccountCreateService
    @MockkBean private lateinit var accountUpdateService: AccountUpdateService
    @MockkBean private lateinit var accountDeleteService: AccountDeleteService

    // controller 의 @CurrentDataScope 파라미터를 채우는 ArgumentResolver 를 mock 으로 교체.
    @MockkBean
    private lateinit var currentAdminContextArgumentResolver: CurrentAdminContextArgumentResolver

    @BeforeEach
    fun stubArgumentResolver() {
        every { currentAdminContextArgumentResolver.supportsParameter(any()) } answers {
            val parameter = firstArg<MethodParameter>()
            parameter.hasParameterAnnotation(CurrentDataScope::class.java)
        }
        every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns DataScope(branchCodes = emptyList(), isAllBranches = true)
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
            every { adminAccountService.getAccounts(any(), any(), any(), any(), any(), any(), any(), any()) } returns response

            mockMvc.perform(get("/api/v1/admin/accounts"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].externalKey").value("AC001234"))
                .andExpect(jsonPath("$.data.content[0].name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.totalElements").value(1))
        }

        @Test
        @DisplayName("성공 - 필터 파라미터 전달")
        fun getAccounts_withFilters() {
            val response = AccountListResponse(content = emptyList(), page = 0, size = 10, totalElements = 0, totalPages = 0)
            every { adminAccountService.getAccounts(any(), eq("GS25"), eq("편의점"), eq("A001"), eq("활성"), eq(0), eq(10), eq(false)) } returns response

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
                .andExpect(jsonPath("$.data.totalElements").value(0))
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
            every { accountCreateService.create(any()) } returns response

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isCreated)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1234))
                .andExpect(jsonPath("$.data.name").value("(신규) 강남점"))
                .andExpect(jsonPath("$.message").value("거래처 등록 성공"))
        }

        @Test
        @DisplayName("C4 실패 - name blank → 400 (validation)")
        fun createAccount_nameBlank() {
            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","employeeCode":"100123"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.success").value(false))
        }

        @Test
        @DisplayName("C5 실패 - 동일명 등록 시도 → 409 ACCOUNT_NAME_DUPLICATE")
        fun createAccount_duplicate() {
            val request = AdminAccountCreateRequest(name = "(신규) 강남점", employeeCode = "100123")
            every { accountCreateService.create(any()) } throws AccountNameDuplicateException()

            mockMvc.perform(
                post("/api/v1/admin/accounts")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isConflict)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NAME_DUPLICATE"))
        }

        @Test
        @DisplayName("C6 실패 - prefix 미포함 → 400 ACCOUNT_NAME_PREFIX_REQUIRED + 메시지 정합")
        fun createAccount_prefixMissing() {
            val request = AdminAccountCreateRequest(name = "강남점", employeeCode = "100123")
            every { accountCreateService.create(any()) } throws AccountNamePrefixRequiredException("(신규)/(기타)")

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
            every { accountUpdateService.update(eq(1234), any(), any()) } returns response

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1234))
                .andExpect(jsonPath("$.data.name").value("(신규) 강남점 신호 수정"))
                .andExpect(jsonPath("$.message").value("거래처 수정 성공"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminAccountControllerTest#updateExceptionCases")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun updateAccount_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { accountUpdateService.update(eq(1234), any(), any()) } throws exception

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"(신규) 다른지점"}""")
            )
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }

        @Test
        @DisplayName("C2 실패 - 비존재 id → 404 ACCOUNT_NOT_FOUND + 메시지에 id 포함")
        fun updateAccount_notFoundMessage() {
            every { accountUpdateService.update(eq(9999), any(), any()) } throws AccountNotFoundException(9999)

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 9999)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"(신규) 무효"}""")
            )
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.message").value("거래처를 찾을 수 없습니다: 9999"))
        }

        @Test
        @DisplayName("C3 실패 - prefix 위반 메시지 정합 - '거래처 수정은 ...'")
        fun updateAccount_prefixMessage() {
            every { accountUpdateService.update(eq(1234), any(), any()) } throws AccountNamePrefixRequiredForUpdateException("(신규)/(기타)")

            mockMvc.perform(
                put("/api/v1/admin/accounts/{id}", 1234)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"강남점"}""")
            )
                .andExpect(status().isBadRequest)
                .andExpect(jsonPath("$.error.message").value("거래처 수정은 ((신규)/(기타)) 중 1개를 필수로 입력하셔야 합니다."))
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/admin/accounts/{id} - 거래처 삭제 (Spec #642)")
    inner class DeleteAccount {

        @Test
        @DisplayName("C1 성공 - 정상 삭제 (200 OK + camelCase 응답)")
        fun deleteAccount_success() {
            every { accountDeleteService.delete(eq(1234)) } just Runs

            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 1234))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("거래처 삭제 성공"))
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.otoki.powersales.admin.controller.AdminAccountControllerTest#deleteExceptionCases")
        @DisplayName("실패 - 예외 → ErrorCode 매핑")
        fun deleteAccount_exceptions(
            @Suppress("UNUSED_PARAMETER") name: String,
            exception: Throwable,
            expectedStatus: Int,
            expectedCode: String
        ) {
            every { accountDeleteService.delete(eq(1234)) } throws exception

            mockMvc.perform(delete("/api/v1/admin/accounts/{id}", 1234))
                .andExpect(status().`is`(expectedStatus))
                .andExpect(jsonPath("$.error.code").value(expectedCode))
        }
    }

    companion object {
        @JvmStatic
        fun updateExceptionCases(): List<Arguments> = listOf(
            Arguments.of("notFound -> 404 ACCOUNT_NOT_FOUND", AccountNotFoundException(1234), 404, "ACCOUNT_NOT_FOUND"),
            Arguments.of(
                "prefixMissing -> 400 ACCOUNT_NAME_PREFIX_REQUIRED",
                AccountNamePrefixRequiredForUpdateException("(신규)/(기타)"),
                400,
                "ACCOUNT_NAME_PREFIX_REQUIRED",
            ),
            Arguments.of("duplicate -> 409 ACCOUNT_NAME_DUPLICATE", AccountNameDuplicateException(), 409, "ACCOUNT_NAME_DUPLICATE"),
        )

        @JvmStatic
        fun deleteExceptionCases(): List<Arguments> = listOf(
            Arguments.of(
                "sapSynced -> 409 ACCOUNT_DELETE_BLOCKED_SAP_SYNCED",
                AccountDeleteBlockedSapSyncedException(),
                409,
                "ACCOUNT_DELETE_BLOCKED_SAP_SYNCED",
            ),
            Arguments.of("notFound -> 404 ACCOUNT_NOT_FOUND", AccountNotFoundException(), 404, "ACCOUNT_NOT_FOUND"),
        )
    }
}
