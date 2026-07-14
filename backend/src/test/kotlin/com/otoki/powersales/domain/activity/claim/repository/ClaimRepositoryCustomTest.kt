package com.otoki.powersales.domain.activity.claim.repository

import com.otoki.powersales.domain.activity.claim.entity.Claim
import com.otoki.powersales.domain.activity.claim.enums.ClaimSfSendStatus
import com.otoki.powersales.domain.activity.claim.enums.ClaimStatus
import com.otoki.powersales.domain.foundation.account.entity.Account
import com.otoki.powersales.domain.foundation.product.entity.Product
import com.otoki.powersales.domain.org.employee.entity.Employee
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.hibernate.Hibernate
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

    private fun persistProduct(code: String, name: String): Product =
        em.persistAndFlush(Product(productCode = code, name = name))

    private fun persistClaim(
        employee: Employee? = null,
        account: Account? = null,
        product: Product? = null,
        costCenterCode: String? = null,
        date: LocalDate,
        createdAt: LocalDateTime = LocalDateTime.of(2026, 6, 1, 0, 0),
    ): Claim {
        val claim = Claim(
            employee = employee,
            account = account,
            product = product,
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

    @Nested
    @DisplayName("findByIdWithSfRefs — SF 송신 snapshot 복원")
    inner class FindByIdWithSfRefs {

        /**
         * SF 송신([ClaimSfDispatchService.dispatch]) 은 `@Async` + AFTER_COMMIT 로 claim 을 재로드해
         * employee/account/product 를 읽는다. 이 연관은 `@ManyToOne(LAZY)` 라 enhancement 환경에서
         * 미초기화 프록시로 노출돼 `claim.employee?.employeeCode` 가 null 로 평가되는 회귀가 있었다
         * (claimId=159077 등록 SF 송신 실패). fetch join 이 세 연관을 초기화된 채 반환함을 가드한다.
         */
        @Test
        @DisplayName("employee/account/product 를 초기화된 상태로 반환한다")
        fun eagerlyLoadsSfRefs() {
            val emp = persistEmployee("EMP-SF", "송신사원")
            val acc = persistAccount("송신거래처")
            val prod = persistProduct("P-SF", "송신제품")
            val claim = persistClaim(
                employee = emp, account = acc, product = prod,
                date = LocalDate.of(2026, 6, 10),
            )
            // 영속성 컨텍스트를 비워 재로드 시 프록시 미초기화 조건을 재현.
            em.clear()

            val loaded = claimRepository.findByIdWithSfRefs(claim.id)

            assertThat(loaded).isNotNull
            assertThat(Hibernate.isInitialized(loaded!!.employee)).isTrue()
            assertThat(Hibernate.isInitialized(loaded.account)).isTrue()
            assertThat(Hibernate.isInitialized(loaded.product)).isTrue()
            assertThat(loaded.employee?.employeeCode).isEqualTo("EMP-SF")
            assertThat(loaded.account?.id).isEqualTo(acc.id)
            assertThat(loaded.product?.productCode).isEqualTo("P-SF")
        }

        @Test
        @DisplayName("존재하지 않는 id 는 null 을 반환한다")
        fun returnsNullWhenMissing() {
            assertThat(claimRepository.findByIdWithSfRefs(999999L)).isNull()
        }
    }

    @Nested
    @DisplayName("findResendTargetIds — SF 재전송 대상 (신규 등록 건만, 마이그레이션 제외)")
    inner class FindResendTargetIds {

        private fun persistClaimWith(
            sfSendStatus: ClaimSfSendStatus?,
            sfSendAttemptCount: Int = 0,
            status: ClaimStatus? = null,
        ): Claim {
            val claim = Claim(
                date = LocalDate.of(2026, 6, 1),
                status = status,
                sfSendStatus = sfSendStatus,
                sfSendAttemptCount = sfSendAttemptCount,
            )
            return em.persistAndFlush(claim)
        }

        @Test
        @DisplayName("SF origin 마이그레이션 건(sfSendStatus=NULL)은 status='전송실패'여도 재전송 대상이 아니다")
        fun excludesMigratedRows() {
            // 마이그레이션 건: 코스모스 전송상태(status)=전송실패 지만 신규→SF 전송상태(sfSendStatus)=NULL.
            val migrated = persistClaimWith(sfSendStatus = null, status = ClaimStatus.SEND_FAILED)
            // 신규 등록 후 SF 전송실패 건: sfSendStatus=전송실패.
            val newFailed = persistClaimWith(sfSendStatus = ClaimSfSendStatus.SEND_FAILED)

            val targets = claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt = 3)

            assertThat(targets).containsExactly(newFailed.id)
            assertThat(targets).doesNotContain(migrated.id)
        }

        @Test
        @DisplayName("sfSendStatus=SENT/PENDING 건은 대상이 아니고, SEND_FAILED 만 대상이다")
        fun onlySendFailed() {
            val sent = persistClaimWith(sfSendStatus = ClaimSfSendStatus.SENT)
            val pending = persistClaimWith(sfSendStatus = ClaimSfSendStatus.PENDING)
            val failed = persistClaimWith(sfSendStatus = ClaimSfSendStatus.SEND_FAILED)

            val targets = claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt = 3)

            assertThat(targets).containsExactly(failed.id)
            assertThat(targets).doesNotContain(sent.id, pending.id)
        }

        @Test
        @DisplayName("시도횟수 상한(sfSendAttemptCount >= maxAttempt) 건은 제외된다")
        fun excludesAttemptCapExceeded() {
            val underCap = persistClaimWith(sfSendStatus = ClaimSfSendStatus.SEND_FAILED, sfSendAttemptCount = 2)
            val atCap = persistClaimWith(sfSendStatus = ClaimSfSendStatus.SEND_FAILED, sfSendAttemptCount = 3)

            val targets = claimRepository.findResendTargetIds(ClaimSfSendStatus.SEND_FAILED, maxAttempt = 3)

            assertThat(targets).containsExactly(underCap.id)
            assertThat(targets).doesNotContain(atCap.id)
        }
    }
}
