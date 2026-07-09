package com.otoki.powersales.domain.foundation.account.service

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.account.exception.AccountNotFoundException
import com.otoki.powersales.domain.foundation.account.repository.AccountRepository
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.auth.sharing.service.SharingRulePolicyEvaluator
import com.querydsl.core.types.Predicate
import com.querydsl.core.types.dsl.EntityPathBase
import com.querydsl.core.types.dsl.Expressions
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort

/**
 * AdminAccountService 단위 테스트 (spec #787 — 단일화 후).
 *
 * 본 spec 단일화로 evaluator + Repository 합성 호출 패턴으로 검증 전환.
 * - 기존 `searchForAdmin` mock 패턴 → `findAllAccessibleByPolicy` mock 패턴
 * - 기존 별도 진입점 `getAccessibleAccountsByPolicy` 흡수 (별도 테스트 클래스 제거)
 */
@DisplayName("AdminAccountService 테스트 (spec #787 — 단일화)")
class AdminAccountServiceTest {

    private val accountRepository: AccountRepository = mockk()
    private val policyEvaluator: SharingRulePolicyEvaluator = mockk()
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver = mockk()
    private val branchCodeExpander: BranchCodeExpander = mockk()
    // 실제 BooleanExpression 인스턴스 — mockk(relaxed=true) 는 BooleanBuilder.and() 의 cast 가 실패하므로
    // QueryDSL Expressions 의 실 인스턴스를 사용해 BooleanBuilder 합성이 동작하도록 한다.
    private val stubPolicyPredicate: Predicate = Expressions.asBoolean(true).isTrue

    private val adminAccountService = AdminAccountService(
        accountRepository,
        policyEvaluator,
        womenScheduleBranchResolver,
        branchCodeExpander,
    )

    @Nested
    @DisplayName("getAccounts — sharing policy 단일화")
    inner class GetAccountsTests {

        @Test
        @DisplayName("evaluator.buildPredicate 호출 (sObjectName=Account, entityPath=QAccount.account)")
        fun servicePassesScopeToEvaluator() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true, userId = 100L)
            stubFindAllAccessibleByPolicy(emptyList())

            every {
                policyEvaluator.buildPredicate(
                    scope = scope,
                    sObjectName = "Account",
                    entityPath = any<EntityPathBase<*>>(),
                )
            } returns stubPolicyPredicate

            adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            verify(exactly = 1) {
                policyEvaluator.buildPredicate(
                    scope = scope,
                    sObjectName = "Account",
                    entityPath = any<EntityPathBase<*>>(),
                )
            }
        }

        @Test
        @DisplayName("필터 없이 조회 → Repository.findAllAccessibleByPolicy 호출")
        fun noFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            val accounts = listOf(createAccount(name = "GS25 역삼점", externalKey = "AC001234"))
            stubFindAllAccessibleByPolicy(accounts)

            val result = adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(1)
            assertThat(result.content[0].externalKey).isEqualTo("AC001234")
            assertThat(result.totalElements).isEqualTo(1)
            assertThat(result.totalPages).isEqualTo(1)
        }

        @Test
        @DisplayName("keyword 필터 → Repository 에 keyword 전달")
        fun keywordFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            stubFindAllAccessibleByPolicy(listOf(createAccount(name = "GS25 역삼점")))

            adminAccountService.getAccounts(scope, "GS25", null, null, null, 0, 20)

            verify(exactly = 1) {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = any(),
                    keyword = "GS25",
                    abcType = null,
                    accountType = null,
                    accountStatusName = null,
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            }
        }

        @Test
        @DisplayName("abcType 필터 → Repository 에 abcType 전달")
        fun abcTypeFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            stubFindAllAccessibleByPolicy(listOf(createAccount(abcType = "편의점")))

            adminAccountService.getAccounts(scope, null, "편의점", null, null, 0, 20)

            verify(exactly = 1) {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = any(),
                    keyword = null,
                    abcType = "편의점",
                    accountType = null,
                    accountStatusName = null,
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            }
        }

        @Test
        @DisplayName("accountStatusName 필터 → Repository 에 accountStatusName 전달")
        fun statusFilter() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            stubFindAllAccessibleByPolicy(listOf(createAccount(accountStatusName = "활성")))

            adminAccountService.getAccounts(scope, null, null, null, "활성", 0, 20)

            verify(exactly = 1) {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = any(),
                    keyword = null,
                    abcType = null,
                    accountType = null,
                    accountStatusName = "활성",
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            }
        }

        @Test
        @DisplayName("branchCode request param → policyPredicate 에 AND 합성 (Repository policyPredicate 인자 변형)")
        fun branchCodeAddedToPolicyPredicate() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            val composedSlot = slot<Predicate>()
            every {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = capture(composedSlot),
                    keyword = null,
                    abcType = null,
                    accountType = null,
                    accountStatusName = null,
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            } returns PageImpl(emptyList(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)

            adminAccountService.getAccounts(scope, null, null, "A001", null, 0, 20)

            // composed predicate 가 evaluator 결과 + branchCode AND 합성
            // BooleanBuilder 표현 — toString 에 stub + branchCode 모두 포함
            assertThat(composedSlot.captured.toString()).contains("account.branchCode = A001")
        }

        @Test
        @DisplayName("페이지네이션 — Repository 가 반환한 Page 메타 보존")
        fun paginationMeta() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            val accounts = (1..20).map { createAccount(id = it.toLong(), name = "Acc-$it") }
            every {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = any(),
                    keyword = null,
                    abcType = null,
                    accountType = null,
                    accountStatusName = null,
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            } returns PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), 100L)

            val result = adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(result.content).hasSize(20)
            assertThat(result.totalElements).isEqualTo(100)
            assertThat(result.totalPages).isEqualTo(5)
            assertThat(result.page).isEqualTo(0)
            assertThat(result.size).isEqualTo(20)
        }

        @Test
        @DisplayName("pageable 정렬 — Sort.by(name).ascending() 적용")
        fun sortByNameAsc() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            val pageableSlot = slot<Pageable>()
            every {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = any(),
                    keyword = null,
                    abcType = null,
                    accountType = null,
                    accountStatusName = null,
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = capture(pageableSlot),
                )
            } returns PageImpl(emptyList(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)

            adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            assertThat(pageableSlot.captured.sort.toString()).isEqualTo("name: ASC")
        }
    }

    @Nested
    @DisplayName("getAccounts — myBranchScopePrincipal (SF 행사마스터 lookup 동등: 지점 화이트리스트 → branch_code IN)")
    inner class MyBranchScopeTests {

        private val principal: WebUserPrincipal = mockk()

        @Test
        @DisplayName("지점 화이트리스트 → BranchCodeExpander 확장 → branch_code IN 매칭, buildPredicate 미호출")
        fun myBranchScopeUsesBranchCodeIn() {
            // resolver 가 SF CurrentUserBranchNameList 정합으로 허용 지점 산출
            every { womenScheduleBranchResolver.resolveBranches(principal) } returns
                listOf(BranchResponse(branchCode = "5832", branchName = "원주1지점"))
            // BranchCodeExpander 가 이력 코드까지 확장 (여기선 단순 pass-through + 1개 이력)
            every { branchCodeExpander.expand(setOf("5832")) } returns setOf("5832", "E5832")

            val composedSlot = slot<Predicate>()
            every {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = capture(composedSlot),
                    keyword = any(),
                    abcType = any(),
                    accountType = any(),
                    accountStatusName = any(),
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            } returns PageImpl(emptyList(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)

            adminAccountService.getAccounts(
                DataScope(branchCodes = emptyList(), isAllBranches = false),
                null, null, null, null, 0, 20, myBranchScopePrincipal = principal
            )

            // branch_code IN (확장집합) 단일 필드 매칭 — owner/조직코드 OR 아님
            val predicateStr = composedSlot.captured.toString()
            assertThat(predicateStr).contains("account.branchCode in [")
            assertThat(predicateStr).contains("5832")
            assertThat(predicateStr).contains("E5832")
            assertThat(predicateStr).doesNotContain("account.divisionCode")
            assertThat(predicateStr).doesNotContain("account.salesDeptCode")
            // sharing policy(owner/hierarchy) 경로는 타지 않음
            verify(exactly = 0) {
                policyEvaluator.buildPredicate(any(), any(), any())
            }
        }

        @Test
        @DisplayName("허용 지점 없음 → 매칭 0건 (false predicate)")
        fun myBranchScopeNoBranchBlocksAll() {
            every { womenScheduleBranchResolver.resolveBranches(principal) } returns emptyList()

            val composedSlot = slot<Predicate>()
            every {
                accountRepository.findAllAccessibleByPolicy(
                    policyPredicate = capture(composedSlot),
                    keyword = any(),
                    abcType = any(),
                    accountType = any(),
                    accountStatusName = any(),
                    applyPromotionFilter = any(),
                    excludeClosedAccount = any(),
                    coordinatesMissing = any(),
                    pageable = any(),
                )
            } returns PageImpl(emptyList(), PageRequest.of(0, 20, Sort.by("name").ascending()), 0L)

            adminAccountService.getAccounts(
                DataScope(branchCodes = emptyList(), isAllBranches = false),
                null, null, null, null, 0, 20, myBranchScopePrincipal = principal
            )

            assertThat(composedSlot.captured.toString()).doesNotContain("account.branchCode in")
            verify(exactly = 0) { branchCodeExpander.expand(any()) }
        }

        @Test
        @DisplayName("myBranchScopePrincipal=null (기본) → 기존 sharing policy 경로 유지 (buildPredicate 호출)")
        fun defaultStillUsesSharingPolicy() {
            val scope = DataScope(branchCodes = listOf("5832"), isAllBranches = false)
            stubEvaluator(scope)
            stubFindAllAccessibleByPolicy(emptyList())

            adminAccountService.getAccounts(scope, null, null, null, null, 0, 20)

            verify(exactly = 1) {
                policyEvaluator.buildPredicate(
                    scope = scope,
                    sObjectName = "Account",
                    entityPath = any<EntityPathBase<*>>(),
                )
            }
            verify(exactly = 0) { womenScheduleBranchResolver.resolveBranches(any()) }
        }
    }

    @Nested
    @DisplayName("getAccountDetail — sharing policy 단건 조회")
    inner class GetAccountDetailTests {

        @Test
        @DisplayName("가시 범위 안 거래처 → AccountDetailResponse 매핑 반환")
        fun returnsDetailWhenAccessible() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            val account = createAccount(id = 7, name = "GS25 역삼점", externalKey = "AC001234")
            every { accountRepository.findAccessibleByPolicyAndId(any(), 7) } returns account

            val result = adminAccountService.getAccountDetail(scope, 7)

            assertThat(result.id).isEqualTo(7)
            assertThat(result.name).isEqualTo("GS25 역삼점")
            assertThat(result.externalKey).isEqualTo("AC001234")
        }

        @Test
        @DisplayName("evaluator.buildPredicate(sObjectName=Account) 결과를 Repository 에 전달")
        fun passesPolicyPredicateToRepository() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true, userId = 100L)
            stubEvaluator(scope)
            every { accountRepository.findAccessibleByPolicyAndId(any(), 7) } returns createAccount(id = 7)

            adminAccountService.getAccountDetail(scope, 7)

            verify(exactly = 1) {
                policyEvaluator.buildPredicate(
                    scope = scope,
                    sObjectName = "Account",
                    entityPath = any<EntityPathBase<*>>(),
                )
            }
            verify(exactly = 1) { accountRepository.findAccessibleByPolicyAndId(any(), 7) }
        }

        @Test
        @DisplayName("가시 범위 밖 / 부재 거래처 → AccountNotFoundException")
        fun throwsWhenNotAccessible() {
            val scope = DataScope(branchCodes = emptyList(), isAllBranches = true)
            stubEvaluator(scope)
            every { accountRepository.findAccessibleByPolicyAndId(any(), 999) } returns null

            assertThatThrownBy { adminAccountService.getAccountDetail(scope, 999) }
                .isInstanceOf(AccountNotFoundException::class.java)
        }
    }

    private fun stubEvaluator(scope: DataScope) {
        every {
            policyEvaluator.buildPredicate(
                scope = scope,
                sObjectName = "Account",
                entityPath = any<EntityPathBase<*>>(),
            )
        } returns stubPolicyPredicate
    }

    private fun stubFindAllAccessibleByPolicy(accounts: List<Account>) {
        every {
            accountRepository.findAllAccessibleByPolicy(
                policyPredicate = any(),
                keyword = any(),
                abcType = any(),
                accountType = any(),
                accountStatusName = any(),
                applyPromotionFilter = any(),
                excludeClosedAccount = any(),
                coordinatesMissing = any(),
                pageable = any(),
            )
        } returns PageImpl(accounts, PageRequest.of(0, 20, Sort.by("name").ascending()), accounts.size.toLong())
    }

    private fun createAccount(
        id: Long = 1L,
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
