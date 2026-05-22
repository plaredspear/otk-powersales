package com.otoki.powersales.account.service

import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.repository.AccountRepository
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@DisplayName("AdminAccountService 테스트")
class AdminAccountServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val policyEvaluator: com.otoki.powersales.auth.sharing.service.SharingRulePolicyEvaluator =
        mockk(relaxed = true)

    private val adminAccountService = AdminAccountService(
        accountRepository,
        policyEvaluator,
    )

    @Nested
    @DisplayName("getAccounts - 거래처 목록 조회")
    inner class GetAccountsTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 거래처 반환")
        fun allBranches_noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val accounts = listOf(createAccount(name = "GS25 역삼점", externalKey = "AC001234"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, null, null, null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].externalKey).isEqualTo("AC001234")
            assertThat(result.content[0].name).isEqualTo("GS25 역삼점")
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("전체 권한 + 지점 필터 -> 지정 지점만 조회")
        fun allBranches_withBranchCode() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, null, listOf("A001"), null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, null, "A001", null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].branchCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("지점 권한 - 필터 없이 조회 -> 본인 지점 거래처만 반환")
        fun branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, null, listOf("A001"), null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + 권한 외 지점 필터 -> 빈 결과")
        fun branchOnly_forbiddenBranch() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)

            val result = adminAccountService.getAccounts(scope, null, null, "B002", null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("지점 권한 + branchCodes 비어있음 -> 빈 결과")
        fun branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)

            val result = adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("키워드 필터 적용 -> 거래처코드/거래처명 부분 일치")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val accounts = listOf(createAccount(name = "GS25 역삼점"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin("GS25", null, null, null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, "GS25", null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("ABC유형 필터 적용 -> 해당 유형만 반환")
        fun abcTypeFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val accounts = listOf(createAccount(abcType = "편의점"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, "편의점", null, null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, "편의점", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].abcType).isEqualTo("편의점")
        }

        @Test
        @DisplayName("상태 필터 적용 -> 해당 상태만 반환")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)

            val accounts = listOf(createAccount(accountStatusName = "활성"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, null, null, "활성", any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, null, null, "활성", 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].accountStatusName).isEqualTo("활성")
        }

        @Test
        @DisplayName("지점 권한 + 허용 지점 필터 -> 해당 지점 거래처 반환")
        fun branchOnly_allowedBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            every { accountRepository.searchForAdmin(null, null, listOf("A001"), null, any()) } returns page

            val result = adminAccountService.getAccounts(scope, null, null, "A001", null, 0, 20)

            assertThat(result.content).hasSize(1)
        }
    }

    private fun createAccount(
        id: Int = 1,
        name: String? = "테스트 거래처",
        externalKey: String? = "AC000001",
        abcType: String? = "편의점",
        branchCode: String? = "A001",
        branchName: String? = "서울1지점",
        employeeCode: String? = "123456",
        address1: String? = "서울시 강남구",
        phone: String? = "02-1234-5678",
        accountStatusName: String? = "활성"
    ): Account = Account(
        id = id,
        name = name,
        externalKey = externalKey,
        abcType = abcType,
        branchCode = branchCode,
        branchName = branchName,
        employeeCode = employeeCode,
        address1 = address1,
        phone = phone,
        accountStatusName = accountStatusName
    )
}
