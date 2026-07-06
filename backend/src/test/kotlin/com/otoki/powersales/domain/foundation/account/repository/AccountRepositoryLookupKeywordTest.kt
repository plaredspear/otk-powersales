package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.user.entity.User
import com.querydsl.core.types.dsl.Expressions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.springframework.context.annotation.Import
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles

/**
 * 행사마스터 거래처 고급 검색(Enhanced Lookup) 동등 — keyword 다중 컬럼 OR 매칭 검증.
 *
 * SF 고급 검색은 검색창 1개로 결과 그리드 여러 컬럼을 relevance 매칭한다. 신규는 keyword 단일 입력을
 * 거래처명(name) / SAP코드(externalKey) / 전화(phone) / 대표자명(representative) / 주소(address1) /
 * 거래처지점명(branchName) OR 매칭으로 근사한다 ([AccountRepositoryCustomImpl.findAllAccessibleByPolicy]).
 *
 * promotionLookupFilter(계정그룹 1000/1010 + 폐업 배제)가 항상 AND 로 합성되므로,
 * 모든 후보 거래처는 accountGroup ∈ {1000,1010} + 비폐업으로 세팅한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository.findAllAccessibleByPolicy — 고급 검색 keyword 다중 컬럼 매칭")
class AccountRepositoryLookupKeywordTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    // 항상 true — sharing policy 통과 (지점 스코프 없는 순수 keyword 매칭 검증용).
    private val allowAll = Expressions.asBoolean(true).isTrue

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        testEntityManager.clear()
    }

    private fun persistLookupAccount(
        name: String,
        externalKey: String,
        phone: String? = null,
        representative: String? = null,
        address1: String? = null,
        branchName: String? = null,
        accountType: String? = null,
        accountStatusName: String = "거래",
    ): Account {
        val account = Account(
            name = name,
            externalKey = externalKey,
            phone = phone,
            representative = representative,
            address1 = address1,
            branchName = branchName,
            accountType = accountType,
            accountGroup = "1000",
            accountStatusName = accountStatusName,
            isDeleted = false,
        )
        val saved = testEntityManager.persistAndFlush(account)
        testEntityManager.clear()
        return saved
    }

    private fun lookup(
        keyword: String?,
        accountType: String? = null,
        accountStatusName: String? = null,
    ) = accountRepository.findAllAccessibleByPolicy(
        policyPredicate = allowAll,
        keyword = keyword,
        abcType = null,
        accountType = accountType,
        accountStatusName = accountStatusName,
        applyPromotionFilter = true,
        excludeClosedAccount = true,
        pageable = PageRequest.of(0, 20),
    )

    @Test
    @DisplayName("거래처명으로 매칭")
    fun matchesByName() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "EXT-2")

        val result = lookup("이마트")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("SAP거래처코드로 매칭")
    fun matchesByExternalKey() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "C400000081")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "C400000099")

        val result = lookup("400000081")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("전화번호로 매칭 (확장 컬럼)")
    fun matchesByPhone() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1", phone = "02-380-5678")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "EXT-2", phone = "051-999-0000")

        val result = lookup("380-5678")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("대표자명으로 매칭 (확장 컬럼)")
    fun matchesByRepresentative() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1", representative = "강희석")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "EXT-2", representative = "김성준")

        val result = lookup("강희석")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("주소로 매칭 (확장 컬럼)")
    fun matchesByAddress() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1", address1 = "서울 영등포구 여의도동")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "EXT-2", address1 = "부산 해운대구")

        val result = lookup("영등포구")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("거래처지점명으로 매칭 (확장 컬럼)")
    fun matchesByBranchName() {
        val hit = persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1", branchName = "강남1지점")
        persistLookupAccount(name = "홈플러스 서면점", externalKey = "EXT-2", branchName = "부산1지점")

        val result = lookup("강남1")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("대소문자 무시 매칭")
    fun matchesCaseInsensitive() {
        val hit = persistLookupAccount(name = "GS25 역삼점", externalKey = "AC001234")
        persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-2")

        val result = lookup("gs25")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("어느 컬럼에도 매칭 안 되면 0건")
    fun noMatchReturnsEmpty() {
        persistLookupAccount(name = "이마트 부산점", externalKey = "EXT-1", phone = "02-380-5678")

        val result = lookup("존재하지않는키워드")

        assertThat(result.content).isEmpty()
    }

    @Test
    @DisplayName("소유자(ownerUser)를 fetch join 으로 로드 — 세션 종료 후에도 이름 접근 가능 (N+1 회피)")
    fun loadsOwnerViaFetchJoin() {
        val owner = testEntityManager.persistAndFlush(
            User(username = "owner01", employeeCode = "E0001", password = "x").apply { name = "김성준" }
        )
        val account = Account(
            name = "이마트 여의도점",
            externalKey = "OWNER-1",
            accountGroup = "1000",
            accountStatusName = "거래",
            isDeleted = false,
            ownerUser = owner,
        )
        testEntityManager.persistAndFlush(account)
        testEntityManager.clear()

        val result = lookup("여의도")

        // fetch join 이 없으면 clear() 후 LAZY 프록시 접근 시 LazyInitializationException 발생.
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].ownerUser?.name).isEqualTo("김성준")
    }

    @Test
    @DisplayName("거래처유형(accountType) 필터 정확 일치")
    fun filtersByAccountType() {
        val hit = persistLookupAccount(name = "이마트 A", externalKey = "T-1", accountType = "대형마트(3대)")
        persistLookupAccount(name = "GS25 B", externalKey = "T-2", accountType = "편의점")

        val result = lookup(keyword = null, accountType = "대형마트(3대)")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("거래상태(accountStatusName) 필터 정확 일치")
    fun filtersByAccountStatusName() {
        val hit = persistLookupAccount(name = "이마트 A", externalKey = "S-1", accountStatusName = "출고중지")
        persistLookupAccount(name = "GS25 B", externalKey = "S-2", accountStatusName = "거래")

        val result = lookup(keyword = null, accountStatusName = "출고중지")

        assertThat(result.content.map { it.id }).containsExactly(hit.id)
    }

    @Test
    @DisplayName("필터 옵션 distinct — 거래처유형/거래상태 값 반환 (폐업 제외)")
    fun distinctFilterOptions() {
        persistLookupAccount(name = "이마트 A", externalKey = "D-1", accountType = "대형마트(3대)", accountStatusName = "거래")
        persistLookupAccount(name = "GS25 B", externalKey = "D-2", accountType = "편의점", accountStatusName = "출고중지")
        persistLookupAccount(name = "GS25 C", externalKey = "D-3", accountType = "편의점", accountStatusName = "거래")
        // 폐업 거래처는 게이팅으로 제외 → distinct 결과에 '폐업' 안 뜸.
        persistLookupAccount(name = "폐점 D", externalKey = "D-4", accountType = "슈퍼", accountStatusName = "폐업")

        val types = accountRepository.findDistinctAccountTypes(allowAll)
        val statuses = accountRepository.findDistinctAccountStatusNames(allowAll)

        assertThat(types).containsExactlyInAnyOrder("대형마트(3대)", "편의점")
        assertThat(statuses).containsExactlyInAnyOrder("거래", "출고중지")
        assertThat(statuses).doesNotContain("폐업")
        assertThat(types).doesNotContain("슈퍼")
    }
}
