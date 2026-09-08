package com.otoki.powersales.domain.foundation.account.repository

import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles

/**
 * 미삭제 거래처 조회의 NULL 취약성 회귀 방지.
 *
 * 종전 `IsDeletedNot(true)` 파생 쿼리(= `is_deleted <> true`)는 SQL 3값 논리로 `is_deleted IS NULL`
 * 행을 통째로 탈락시켜, SAP 로 적재된(is_deleted 미세팅) 거래처가 주문서 작성·매출 화면에서
 * 사라졌다 (2026-09-08 운영 장애). mockk 단위 테스트로는 잡히지 않는 SQL 의미 차이라 JPA 슬라이스로 고정한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class AccountRepositoryTest {

    @Autowired
    private lateinit var accountRepository: AccountRepository

    @Autowired
    private lateinit var em: TestEntityManager

    @Test
    @DisplayName("findByIdInAndNotDeleted - is_deleted 가 NULL 인 거래처도 미삭제로 조회된다")
    fun findByIdInAndNotDeleted_includesNull() {
        val nullFlag = persist(name = "NULL거래처", isDeleted = null)
        val notDeleted = persist(name = "미삭제거래처", isDeleted = false)
        val deleted = persist(name = "삭제거래처", isDeleted = true)
        em.clear()

        val result = accountRepository.findByIdInAndNotDeleted(listOf(nullFlag.id, notDeleted.id, deleted.id))

        assertThat(result.map { it.name })
            .containsExactlyInAnyOrder("NULL거래처", "미삭제거래처")
    }

    @Test
    @DisplayName("findByBranchCodeAndAccountGroupInAndNotDeleted - is_deleted 가 NULL 인 거래처도 미삭제로 조회된다")
    fun findByBranchCodeAndAccountGroupInAndNotDeleted_includesNull() {
        persist(name = "NULL거래처", isDeleted = null, branchCode = "5830", accountGroup = "1000")
        persist(name = "미삭제거래처", isDeleted = false, branchCode = "5830", accountGroup = "1010")
        persist(name = "삭제거래처", isDeleted = true, branchCode = "5830", accountGroup = "1000")
        persist(name = "타지점거래처", isDeleted = null, branchCode = "9999", accountGroup = "1000")
        persist(name = "타그룹거래처", isDeleted = null, branchCode = "5830", accountGroup = "2000")
        em.clear()

        val result = accountRepository
            .findByBranchCodeAndAccountGroupInAndNotDeleted("5830", listOf("1000", "1010"))

        assertThat(result.map { it.name })
            .containsExactlyInAnyOrder("NULL거래처", "미삭제거래처")
    }

    private fun persist(
        name: String,
        isDeleted: Boolean?,
        branchCode: String? = null,
        accountGroup: String? = null,
    ): Account = em.persist(
        Account(
            name = name,
            isDeleted = isDeleted,
            branchCode = branchCode,
            accountGroup = accountGroup,
        )
    )
}
