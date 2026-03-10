package com.otoki.internal.admin.service

import com.otoki.internal.admin.dto.DataScope
import com.otoki.internal.admin.scope.DataScopeHolder
import com.otoki.internal.sap.entity.Account
import com.otoki.internal.sap.repository.AccountRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort

@ExtendWith(MockitoExtension::class)
@DisplayName("AdminAccountService 테스트")
class AdminAccountServiceTest {

    @Mock
    private lateinit var dataScopeHolder: DataScopeHolder

    @Mock
    private lateinit var accountRepository: AccountRepository

    @InjectMocks
    private lateinit var adminAccountService: AdminAccountService

    @Nested
    @DisplayName("getAccounts - 거래처 목록 조회")
    inner class GetAccountsTests {

        @Test
        @DisplayName("전체 권한 - 필터 없이 조회 -> 전체 거래처 반환")
        fun allBranches_noFilter() {
            // Given
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(name = "GS25 역삼점", externalKey = "AC001234"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq(null), eq(null), eq(null), any())).thenReturn(page)

            // When
            val result = adminAccountService.getAccounts(null, null, null, null, 0, 20)

            // Then
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
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq(null), eq(listOf("A001")), eq(null), any())).thenReturn(page)

            val result = adminAccountService.getAccounts(null, null, "A001", null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].branchCode).isEqualTo("A001")
        }

        @Test
        @DisplayName("지점 권한 - 필터 없이 조회 -> 본인 지점 거래처만 반환")
        fun branchOnly_noFilter() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq(null), eq(listOf("A001")), eq(null), any())).thenReturn(page)

            val result = adminAccountService.getAccounts(null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("지점 권한 + 권한 외 지점 필터 -> 빈 결과")
        fun branchOnly_forbiddenBranch() {
            val scope = DataScope(branchCodes = listOf("A001"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminAccountService.getAccounts(null, null, "B002", null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("지점 권한 + branchCodes 비어있음 -> 빈 결과")
        fun branchOnly_emptyBranchCodes() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val result = adminAccountService.getAccounts(null, null, null, null, 0, 20)

            assertThat(result.content).isEmpty()
            assertThat(result.totalElements).isEqualTo(0)
        }

        @Test
        @DisplayName("키워드 필터 적용 -> 거래처코드/거래처명 부분 일치")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(name = "GS25 역삼점"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq("GS25"), eq(null), eq(null), eq(null), any())).thenReturn(page)

            val result = adminAccountService.getAccounts("GS25", null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
        }

        @Test
        @DisplayName("ABC유형 필터 적용 -> 해당 유형만 반환")
        fun abcTypeFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(abcType = "편의점"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq("편의점"), eq(null), eq(null), any())).thenReturn(page)

            val result = adminAccountService.getAccounts(null, "편의점", null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].abcType).isEqualTo("편의점")
        }

        @Test
        @DisplayName("상태 필터 적용 -> 해당 상태만 반환")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(accountStatusName = "활성"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq(null), eq(null), eq("활성"), any())).thenReturn(page)

            val result = adminAccountService.getAccounts(null, null, null, "활성", 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].accountStatusName).isEqualTo("활성")
        }

        @Test
        @DisplayName("지점 권한 + 허용 지점 필터 -> 해당 지점 거래처 반환")
        fun branchOnly_allowedBranch() {
            val scope = DataScope(branchCodes = listOf("A001", "A002"), isAllBranches = false)
            whenever(dataScopeHolder.require()).thenReturn(scope)

            val accounts = listOf(createAccount(branchCode = "A001"))
            val page = PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 1L)
            whenever(accountRepository.searchForAdmin(eq(null), eq(null), eq(listOf("A001")), eq(null), any())).thenReturn(page)

            val result = adminAccountService.getAccounts(null, null, "A001", null, 0, 20)

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
