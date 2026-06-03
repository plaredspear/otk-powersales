package com.otoki.powersales.account.repository

import com.otoki.powersales.account.entity.Account
import com.otoki.powersales.account.entity.QAccount.Companion.account
import com.otoki.powersales.common.config.QueryDslConfig
import com.querydsl.core.types.dsl.Expressions
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 거래처 상세 조회 — sharing policy Predicate + soft-delete 제외 + id 매칭.
 *
 * 가시 범위 밖(policy predicate 불일치) / soft-delete / 부재 거래처는 모두 null 반환
 * (호출 측에서 404 변환 — SF sharing rule "권한 없는 레코드는 존재하지 않음" 동등).
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
@DisplayName("AccountRepository.findAccessibleByPolicyAndId 테스트")
class AccountRepositoryFindAccessibleByPolicyAndIdTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var testEntityManager: TestEntityManager

    // 항상 true — sharing policy 통과 케이스 (no-filter, viewAllData 동등).
    private val allowAll = Expressions.asBoolean(true).isTrue

    @BeforeEach
    fun setUp() {
        accountRepository.deleteAll()
        testEntityManager.clear()
    }

    @Test
    @DisplayName("policy 통과 + 활성 거래처 → hit")
    fun accessibleActive_returnsAccount() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-A1", branchCode = "A001", isDeleted = false)
        )
        testEntityManager.clear()

        val result = accountRepository.findAccessibleByPolicyAndId(allowAll, saved.id)
        assertThat(result).isNotNull
        assertThat(result?.id).isEqualTo(saved.id)
    }

    @Test
    @DisplayName("is_deleted = true → empty (soft-delete 제외)")
    fun deleted_returnsNull() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-A2", isDeleted = true)
        )
        testEntityManager.clear()

        val result = accountRepository.findAccessibleByPolicyAndId(allowAll, saved.id)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("policy predicate 불일치 (가시 범위 밖) → empty")
    fun notAccessibleByPolicy_returnsNull() {
        val saved = testEntityManager.persistAndFlush(
            Account(name = "(신규) 강남점", externalKey = "EXT-A3", branchCode = "A001", isDeleted = false)
        )
        testEntityManager.clear()

        // branchCode = 'Z999' 만 가시 — 저장된 'A001' 거래처는 매칭 0건.
        val policy = account.branchCode.eq("Z999")
        val result = accountRepository.findAccessibleByPolicyAndId(policy, saved.id)
        assertThat(result).isNull()
    }

    @Test
    @DisplayName("row 부재 → empty")
    fun noRow_returnsNull() {
        val result = accountRepository.findAccessibleByPolicyAndId(allowAll, 9999)
        assertThat(result).isNull()
    }
}
