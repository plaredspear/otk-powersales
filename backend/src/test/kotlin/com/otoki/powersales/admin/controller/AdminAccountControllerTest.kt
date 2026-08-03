package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountCreateRequest
import com.otoki.powersales.domain.foundation.account.dto.request.AdminAccountUpdateRequest
import com.otoki.powersales.domain.foundation.account.dto.response.AccountDetailResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListItem
import com.otoki.powersales.domain.foundation.account.dto.response.AccountListResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountCreateResponse
import com.otoki.powersales.domain.foundation.account.dto.response.AdminAccountUpdateResponse
import com.otoki.powersales.domain.foundation.account.exception.AccountDeleteBlockedSapSyncedException
import com.otoki.powersales.domain.foundation.account.exception.AccountNameDuplicateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNamePrefixRequiredException
import com.otoki.powersales.domain.foundation.account.exception.AccountNamePrefixRequiredForUpdateException
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.service.AccountCreateService
import com.otoki.powersales.domain.foundation.account.service.AccountDeleteService
import com.otoki.powersales.domain.foundation.account.service.AccountUpdateService
import com.otoki.powersales.domain.foundation.account.service.AdminAccountService
import com.otoki.powersales.admin.service.BranchScopeGateway
import com.otoki.powersales.admin.dto.BranchScopeResult
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.security.CurrentAdminContextArgumentResolver
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.permission.SalesSupportTeam2Policy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.test.AdminControllerTestSupport
import org.assertj.core.api.Assertions.assertThat
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
import io.mockk.slot
import io.mockk.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.MethodParameter
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import com.ninjasquad.springmockk.MockkBean
import com.otoki.powersales.domain.foundation.account.entity.Account
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
    @MockkBean private lateinit var branchScopeGateway: BranchScopeGateway

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

        // 지점 스코프 게이트웨이 기본 stub — 선택값이 있으면 확장 코드 목록, 없으면 필터 미적용(전건).
        every { branchScopeGateway.applyDataScope(any(), any()) } answers { secondArg() }
        every { branchScopeGateway.resolveScope(any(), any<String>(), any()) } answers {
            val requested = secondArg<String?>()
            if (requested.isNullOrBlank()) BranchScopeResult.Unrestricted
            else BranchScopeResult.Allowed(listOf(requested), listOf(requested))
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/branches - 지점 셀렉터 옵션")
    inner class GetBranches {

        @Test
        @DisplayName("성공 - 권한별 지점 목록 반환")
        fun getBranches_success() {
            every { branchScopeGateway.resolveBranches(any(), any()) } returns listOf(
                BranchResponse(branchCode = "1100", branchName = "강남지점"),
                BranchResponse(branchCode = "1200", branchName = "서초지점"),
            )

            mockMvc.perform(get("/api/v1/admin/accounts/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].branchCode").value("1100"))
                .andExpect(jsonPath("$.data[0].branchName").value("강남지점"))
                .andExpect(jsonPath("$.data[1].branchCode").value("1200"))
        }

        @Test
        @DisplayName("라우팅 - /branches 리터럴 경로가 /{id}(Long) 보다 우선 매칭")
        fun getBranches_literalPathTakesPriorityOverIdPathVariable() {
            // /branches 가 @GetMapping("/{id}") 로 잘못 라우팅되면 "branches" → Long 변환 실패로 400 이 된다.
            // 리터럴 경로 우선 매칭이 보장되어 200 + 지점 목록이 반환되는지 검증 (회귀 가드).
            every { branchScopeGateway.resolveBranches(any(), any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/admin/accounts/branches"))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
        }
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
                        accountStatusName = "활성",
                        accountType = "편의점",
                        zipCode = "06234",
                        representative = "홍길동",
                        ownerName = "김성준",
                        geocodeUnresolved = false
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
            every { adminAccountService.getAccounts(any(), eq("GS25"), eq("편의점"), eq(listOf("A001")), eq("활성"), eq(0), eq(10), eq(false)) } returns response

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
    @DisplayName("영업지원2팀(4889) 거래처 조회 전 지점 예외")
    inner class SalesSupportTeam2AccountScope {

        /** 영업지원2팀 소속으로 로그인 — 지점 코드만 다르고 나머지는 기본 stub 과 동일. */
        private fun authenticateAsTeam2() = authenticateAsAdmin(
            role = AppAuthority.BRANCH_MANAGER,
            costCenterCode = SalesSupportTeam2Policy.ORG_CODE,
        )

        /** 비전사 사용자의 DataScope — 본인 지점 1건 (전 지점 전환 여부를 관찰하기 위한 출발점). */
        private fun stubOwnBranchDataScope(branchCode: String = SalesSupportTeam2Policy.ORG_CODE) {
            every { currentAdminContextArgumentResolver.resolveArgument(any(), any(), any(), any()) } returns
                DataScope(branchCodes = listOf(branchCode), isAllBranches = false)
        }

        private fun emptyListResponse() =
            AccountListResponse(content = emptyList(), page = 0, size = 20, totalElements = 0, totalPages = 0)

        @Test
        @DisplayName("지점 셀렉터 - 전사 principal 사본으로 게이트웨이를 호출한다 (조직 전건 목록)")
        fun branches_useAllBranchPrincipal() {
            authenticateAsTeam2()
            val principalSlot = slot<WebUserPrincipal>()
            every { branchScopeGateway.resolveBranches(capture(principalSlot), any()) } returns emptyList()

            mockMvc.perform(get("/api/v1/admin/accounts/branches")).andExpect(status().isOk)

            assertThat(principalSlot.captured.isSalesSupport).isTrue()
            // 원본 principal 은 그대로 — 사본만 전사로 바뀐다 (다른 화면 스코프 무영향).
            assertThat(principalSlot.captured.costCenterCode).isEqualTo(SalesSupportTeam2Policy.ORG_CODE)
        }

        @Test
        @DisplayName("목록 - DataScope 를 전 지점(isAllBranches=true)으로 바꿔 서비스에 넘긴다")
        fun list_widensDataScopeToAllBranches() {
            authenticateAsTeam2()
            stubOwnBranchDataScope()
            val scopeSlot = slot<DataScope>()
            every {
                adminAccountService.getAccounts(capture(scopeSlot), any(), any(), any(), any(), any(), any(), any())
            } returns emptyListResponse()

            mockMvc.perform(get("/api/v1/admin/accounts")).andExpect(status().isOk)

            // Account 는 OWD Private 이라 이 축을 열지 않으면 owner 불일치로 전부 누락된다.
            assertThat(scopeSlot.captured.isAllBranches).isTrue()
            assertThat(scopeSlot.captured.branchCodes).isEmpty()
        }

        @Test
        @DisplayName("상세 - 목록과 동일한 전 지점 스코프로 조회한다 (목록엔 보이는데 상세 404 방지)")
        fun detail_usesSameAllBranchScope() {
            authenticateAsTeam2()
            stubOwnBranchDataScope()
            val scopeSlot = slot<DataScope>()
            every { adminAccountService.getAccountDetail(capture(scopeSlot), eq(7L)) } returns
                AccountDetailResponse.from(Account(id = 7, name = "GS25 역삼점"))

            mockMvc.perform(get("/api/v1/admin/accounts/7")).andExpect(status().isOk)

            assertThat(scopeSlot.captured.isAllBranches).isTrue()
        }

        @Test
        @DisplayName("일반 지점 사용자 - principal / DataScope 를 손대지 않는다 (회귀 가드)")
        fun otherBranchUserUnaffected() {
            authenticateAsAdmin(role = AppAuthority.BRANCH_MANAGER, costCenterCode = "5832")
            stubOwnBranchDataScope(branchCode = "5832")
            val principalSlot = slot<WebUserPrincipal>()
            val scopeSlot = slot<DataScope>()
            every { branchScopeGateway.resolveScope(capture(principalSlot), any<String>(), any()) } returns
                BranchScopeResult.Unrestricted
            every {
                adminAccountService.getAccounts(capture(scopeSlot), any(), any(), any(), any(), any(), any(), any())
            } returns emptyListResponse()

            mockMvc.perform(get("/api/v1/admin/accounts")).andExpect(status().isOk)

            assertThat(principalSlot.captured.isSalesSupport).isFalse()
            assertThat(scopeSlot.captured.isAllBranches).isFalse()
            assertThat(scopeSlot.captured.branchCodes).containsExactly("5832")
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/lookup-for-monthly-sales - ORORA 월매출 거래처 lookup")
    inner class LookupAccountsForMonthlySales {

        @Test
        @DisplayName("성공 - 행사 lookupFilter 미적용(applyPromotionFilter=false) 으로 조회한다")
        fun lookupForMonthlySales_doesNotApplyPromotionFilter() {
            val response = AccountListResponse(
                content = emptyList(),
                page = 0,
                size = 20,
                totalElements = 0,
                totalPages = 0
            )
            // 매출 적재 확인 화면이라 폐업 거래처의 과거 매출도 조회 대상 — applyPromotionFilter 를
            // 넘기지 않으면 기본값 true 로 되돌아 폐업 거래처가 조용히 사라진다. 인자를 테스트로 고정.
            every {
                adminAccountService.getAccounts(any(), eq("역삼"), isNull(), isNull(), isNull(), eq(0), eq(20), eq(false))
            } returns response

            mockMvc.perform(
                get("/api/v1/admin/accounts/lookup-for-monthly-sales").param("keyword", "역삼")
            )
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))

            verify {
                adminAccountService.getAccounts(any(), eq("역삼"), isNull(), isNull(), isNull(), eq(0), eq(20), eq(false))
            }
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/accounts/{id} - 거래처 상세 조회")
    inner class GetAccountDetail {

        @Test
        @DisplayName("성공 - 상세 조회 (200 OK + 기본 정보 + 거래처코드)")
        fun getAccountDetail_success() {
            val response = AccountDetailResponse.from(
                Account(
                    id = 7,
                    name = "GS25 역삼점",
                    externalKey = "AC001234",
                    abcType = "편의점",
                    branchCode = "A001",
                    branchName = "서울1지점",
                    accountStatusName = "활성"
                )
            )
            every { adminAccountService.getAccountDetail(any(), eq(7)) } returns response

            mockMvc.perform(get("/api/v1/admin/accounts/{id}", 7))
                .andExpect(status().isOk)
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(7))
                .andExpect(jsonPath("$.data.name").value("GS25 역삼점"))
                .andExpect(jsonPath("$.data.externalKey").value("AC001234"))
        }

        @Test
        @DisplayName("실패 - 비존재/가시 범위 밖 id → 404 ACCOUNT_NOT_FOUND")
        fun getAccountDetail_notFound() {
            every { adminAccountService.getAccountDetail(any(), eq(9999)) } throws AccountNotFoundException(9999)

            mockMvc.perform(get("/api/v1/admin/accounts/{id}", 9999))
                .andExpect(status().isNotFound)
                .andExpect(jsonPath("$.error.code").value("ACCOUNT_NOT_FOUND"))
                .andExpect(jsonPath("$.error.message").value("거래처를 찾을 수 없습니다: 9999"))
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
            every { accountDeleteService.delete(eq(1234), any()) } just Runs

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
            every { accountDeleteService.delete(eq(1234), any()) } throws exception

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
