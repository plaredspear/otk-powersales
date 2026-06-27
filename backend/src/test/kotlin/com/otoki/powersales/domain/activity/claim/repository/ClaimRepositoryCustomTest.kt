package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * ClaimRepositoryCustom QueryDSL 구현 검증.
 *
 * 기존 @Query(JPQL) → QueryDSL 전환의 동작 동등성 회귀 가드. mock 기반 ClaimQueryServiceTest 가
 * 잡지 못하는 실 DB 동작(발생일자 BETWEEN 경계, accountId null/지정 분기, 정렬, 사원/원가센터 스코프)을 검증한다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class)
class ClaimRepositoryCustomTest {

    @Autowired
    private lateinit var claimRepository: ClaimRepository

    @Autowired
    private lateinit var em: TestEntityManager

    private fun persistEmployee(code: String, name: String): Employee =
        em.persistAndFlush(Employee(employeeCode = code, name = name))

    private fun persistAccount(name: String): Account =
        em.persistAndFlush(Account(name = name))

    private fun persistClaim(
        employee: Employee? = null,
        account: Account? = null,
        costCenterCode: String? = null,
        date: LocalDate,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 6, 1, 0, 0),
    ): Claim {
        val claim = Claim(
            employee = employee,
            account = account,
            costCenterCode = costCenterCode,
            date = date,
        )
        claim.createdAt = createdAt
        return em.persistAndFlush(claim)
    }

    @Nested
    @DisplayName("findOwnClaims — 본인 등록분")
    inner class FindOwnClaims {

        @Test
        @DisplayName("본인 사원 + 발생일자 BETWEEN 범위 내 건만 조회한다")
        fun ownAndDateRange() {
            val me = persistEmployee("EMP-ME", "나")
            val other = persistEmployee("EMP-OTHER", "타인")
            // 범위 내(본인)
            persistClaim(employee = me, date = LocalDate.of(2026, 6, 10))
            // 범위 경계(시작일 == 발생일자) 포함
            persistClaim(employee = me, date = LocalDate.of(2026, 6, 1))
            // 범위 경계(종료일 == 발생일자) 포함
            persistClaim(employee = me, date = LocalDate.of(2026, 6, 30))
            // 범위 밖
            persistClaim(employee = me, date = LocalDate.of(2026, 5, 31))
            persistClaim(employee = me, date = LocalDate.of(2026, 7, 1))
            // 타인 등록분(제외 대상)
            persistClaim(employee = other, date = LocalDate.of(2026, 6, 10))
            em.clear()

            val result = claimRepository.findOwnClaims(
                me.id, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null
            )

            assertThat(result).hasSize(3)
            assertThat(result.map { it.date }).containsExactlyInAnyOrder(
                LocalDate.of(2026, 6, 10),
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 6, 30),
            )
            assertThat(result).allMatch { it.employee?.id == me.id }
        }

        @Test
        @DisplayName("accountId 지정 시 해당 거래처 건만, null 이면 전체를 조회한다")
        fun accountIdFilter() {
            val me = persistEmployee("EMP-ME", "나")
            val a1 = persistAccount("거래처1")
            val a2 = persistAccount("거래처2")
            persistClaim(employee = me, account = a1, date = LocalDate.of(2026, 6, 10))
            persistClaim(employee = me, account = a2, date = LocalDate.of(2026, 6, 11))
            em.clear()

            val filtered = claimRepository.findOwnClaims(
                me.id, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), a1.id
            )
            assertThat(filtered).hasSize(1)
            assertThat(filtered[0].account?.id).isEqualTo(a1.id)

            val all = claimRepository.findOwnClaims(
                me.id, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null
            )
            assertThat(all).hasSize(2)
        }

        @Test
        @DisplayName("발생일자 DESC, createdAt DESC 순으로 정렬한다")
        fun ordering() {
            val me = persistEmployee("EMP-ME", "나")
            // 같은 발생일자 — createdAt 으로 tie-break
            val older = persistClaim(
                employee = me, date = LocalDate.of(2026, 6, 10),
                createdAt = LocalDateTime.of(2026, 6, 10, 9, 0),
            )
            val newer = persistClaim(
                employee = me, date = LocalDate.of(2026, 6, 10),
                createdAt = LocalDateTime.of(2026, 6, 10, 15, 0),
            )
            // 더 이른 발생일자 — 마지막으로 정렬
            val earlierDate = persistClaim(employee = me, date = LocalDate.of(2026, 6, 5))
            em.clear()

            val result = claimRepository.findOwnClaims(
                me.id, LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null
            )

            assertThat(result.map { it.id }).containsExactly(newer.id, older.id, earlierDate.id)
        }
    }

    @Nested
    @DisplayName("findCostCenterClaims — 같은 원가센터")
    inner class FindCostCenterClaims {

        @Test
        @DisplayName("원가센터 일치 건만, 발생일자 BETWEEN 범위 내에서 조회한다")
        fun costCenterAndDateRange() {
            val emp = persistEmployee("EMP-1", "사원")
            // 일치 원가센터 + 범위 내
            persistClaim(employee = emp, costCenterCode = "CC01", date = LocalDate.of(2026, 6, 10))
            // 일치 원가센터 + 범위 밖
            persistClaim(employee = emp, costCenterCode = "CC01", date = LocalDate.of(2026, 7, 1))
            // 다른 원가센터(제외 대상)
            persistClaim(employee = emp, costCenterCode = "CC02", date = LocalDate.of(2026, 6, 10))
            em.clear()

            val result = claimRepository.findCostCenterClaims(
                "CC01", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), null
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].costCenterCode).isEqualTo("CC01")
            assertThat(result[0].date).isEqualTo(LocalDate.of(2026, 6, 10))
        }

        @Test
        @DisplayName("accountId 지정 시 해당 거래처 건만 조회한다")
        fun accountIdFilter() {
            val emp = persistEmployee("EMP-1", "사원")
            val a1 = persistAccount("거래처1")
            val a2 = persistAccount("거래처2")
            persistClaim(employee = emp, account = a1, costCenterCode = "CC01", date = LocalDate.of(2026, 6, 10))
            persistClaim(employee = emp, account = a2, costCenterCode = "CC01", date = LocalDate.of(2026, 6, 11))
            em.clear()

            val result = claimRepository.findCostCenterClaims(
                "CC01", LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30), a1.id
            )

            assertThat(result).hasSize(1)
            assertThat(result[0].account?.id).isEqualTo(a1.id)
        }
    }
}
