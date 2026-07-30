package com.otoki.powersales.platform.auth.token

import com.otoki.powersales.platform.auth.token.repository.RefreshTokenFamilyRevocationRepository
import com.otoki.powersales.platform.common.config.QueryDslConfig
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionTemplate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * [RefreshTokenStore] 테스트 — Redis SoT → DB SoT 전환의 핵심 의미론 검증.
 *
 * 특히 다음 3가지가 깨지면 인증이 무너지므로 회귀 가드를 둔다.
 *  1. "행의 존재 = 미사용 유효 토큰" (탈취 감지의 전제)
 *  2. 만료행이 물리적으로 남아 있어도 유효하지 않다 (Redis TTL 이 해 주던 일)
 *  3. MOBILE/WEB 의 userId 는 다른 id 공간 — audience 격리
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
@Import(QueryDslConfig::class, RefreshTokenStore::class)
class RefreshTokenStoreTest {

    @Autowired
    private lateinit var store: RefreshTokenStore

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @Autowired
    private lateinit var familyRevocationRepository: RefreshTokenFamilyRevocationRepository

    private val oneHour = 3_600_000L
    private val expired = -1_000L

    @Nested
    @DisplayName("refresh token 저장/조회")
    inner class StoreAndExists {

        @Test
        @DisplayName("저장한 토큰은 유효로 조회된다")
        fun storedTokenExists() {
            store.store(RefreshTokenAudience.MOBILE, "t1", 1L, "f1", oneHour)

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "t1")).isTrue()
        }

        @Test
        @DisplayName("저장한 적 없는 토큰은 false — 재사용(탈취) 판정 입력")
        fun unknownTokenDoesNotExist() {
            assertThat(store.exists(RefreshTokenAudience.MOBILE, "nope")).isFalse()
        }

        @Test
        @DisplayName("만료된 토큰은 행이 남아 있어도 false — Redis TTL 대체 판정")
        fun expiredTokenDoesNotExist() {
            store.store(RefreshTokenAudience.MOBILE, "t-expired", 1L, "f1", expired)

            // 정리 배치 전이라 행 자체는 남아 있지만 유효하지 않아야 한다
            assertThat(store.exists(RefreshTokenAudience.MOBILE, "t-expired")).isFalse()
            assertThat(store.deleteExpired().first).isEqualTo(1)
        }

        @Test
        @DisplayName("삭제한 토큰은 false — rotation 후 이전 토큰 재사용 차단")
        fun deletedTokenDoesNotExist() {
            store.store(RefreshTokenAudience.MOBILE, "t1", 1L, "f1", oneHour)
            store.consume(RefreshTokenAudience.MOBILE, "t1")

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "t1")).isFalse()
        }
    }

    @Nested
    @DisplayName("원자적 소비 (consume) — 회전 검증 + 회수")
    inner class Consume {

        @Test
        @DisplayName("유효 토큰의 첫 소비만 성공하고 두 번째는 실패한다 — 재사용(탈취) 판정")
        fun firstConsumeWinsSecondFails() {
            store.store(RefreshTokenAudience.MOBILE, "t1", 1L, "f1", oneHour)

            assertThat(store.consume(RefreshTokenAudience.MOBILE, "t1")).isTrue()
            assertThat(store.consume(RefreshTokenAudience.MOBILE, "t1")).isFalse()
        }

        @Test
        @DisplayName("저장한 적 없는 토큰은 소비 실패")
        fun unknownTokenCannotBeConsumed() {
            assertThat(store.consume(RefreshTokenAudience.MOBILE, "nope")).isFalse()
        }

        @Test
        @DisplayName("만료된 토큰은 소비 실패하고 행은 남는다 — 정리 배치가 회수")
        fun expiredTokenCannotBeConsumed() {
            store.store(RefreshTokenAudience.MOBILE, "t-expired", 1L, "f1", expired)

            assertThat(store.consume(RefreshTokenAudience.MOBILE, "t-expired")).isFalse()
            assertThat(store.deleteExpired().first).isEqualTo(1)
        }

        /**
         * 핵심 회귀 가드 — 구 `exists` + `delete` 2단계 구현은 동시 요청 둘 다 통과시켜
         * 같은 family 에 유효 토큰 2개를 만들고 탈취 감지를 영구 무력화했다.
         * 단일 조건부 DELETE 는 어떤 인터리빙에서도 정확히 하나만 성공해야 한다.
         */
        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("동일 토큰 동시 소비 — 정확히 1건만 성공한다")
        fun concurrentConsumeAllowsExactlyOneWinner() {
            val tokenId = "t-concurrent"
            store.store(RefreshTokenAudience.MOBILE, tokenId, 1L, "f1", oneHour)

            val threads = 8
            val start = CountDownLatch(1)
            val pool = Executors.newFixedThreadPool(threads)
            try {
                val results = (1..threads).map {
                    pool.submit<Boolean> {
                        start.await()
                        store.consume(RefreshTokenAudience.MOBILE, tokenId)
                    }
                }
                start.countDown()
                val winners = results.count { it.get(10, TimeUnit.SECONDS) }

                assertThat(winners)
                    .`as`("동시 소비 중 정확히 1건만 성공해야 한다 (나머지는 재사용 판정)")
                    .isEqualTo(1)
            } finally {
                pool.shutdownNow()
            }
            assertThat(store.exists(RefreshTokenAudience.MOBILE, tokenId)).isFalse()
        }
    }

    @Nested
    @DisplayName("audience 격리 — MOBILE/WEB 의 userId 는 서로 다른 id 공간")
    inner class AudienceIsolation {

        @Test
        @DisplayName("동일 tokenId 를 두 채널에 저장해도 서로 간섭하지 않는다")
        fun sameTokenIdIsolatedAcrossAudiences() {
            store.store(RefreshTokenAudience.MOBILE, "same", 1L, "f1", oneHour)
            store.store(RefreshTokenAudience.WEB, "same", 1L, "f2", oneHour)

            store.consume(RefreshTokenAudience.MOBILE, "same")

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "same")).isFalse()
            assertThat(store.exists(RefreshTokenAudience.WEB, "same")).isTrue()
        }

        @Test
        @DisplayName("사용자 전량 회수는 같은 audience 에만 적용된다")
        fun deleteByUserIdScopedToAudience() {
            store.store(RefreshTokenAudience.MOBILE, "m1", 7L, "f1", oneHour)
            store.store(RefreshTokenAudience.WEB, "w1", 7L, "f2", oneHour)

            store.deleteByUserId(RefreshTokenAudience.MOBILE, 7L)

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "m1")).isFalse()
            assertThat(store.exists(RefreshTokenAudience.WEB, "w1")).isTrue()
        }

        @Test
        @DisplayName("family 무효화도 audience 별로 격리된다")
        fun familyRevocationScopedToAudience() {
            store.revokeFamily(RefreshTokenAudience.MOBILE, "fam", oneHour)

            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "fam")).isTrue()
            assertThat(store.isFamilyRevoked(RefreshTokenAudience.WEB, "fam")).isFalse()
        }
    }

    @Nested
    @DisplayName("사용자별 전량 회수 (로그아웃 / 단말 초기화)")
    inner class DeleteByUser {

        @Test
        @DisplayName("구 Redis 구현과 달리 최신 1건이 아니라 해당 사용자 전량을 회수한다")
        fun deletesAllTokensOfUser() {
            // 로그아웃 없이 재로그인을 반복해 세션이 여러 개 쌓인 상태
            store.store(RefreshTokenAudience.MOBILE, "old", 9L, "f1", oneHour)
            store.store(RefreshTokenAudience.MOBILE, "new", 9L, "f2", oneHour)

            store.deleteByUserId(RefreshTokenAudience.MOBILE, 9L)

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "old")).isFalse()
            assertThat(store.exists(RefreshTokenAudience.MOBILE, "new")).isFalse()
        }

        @Test
        @DisplayName("다른 사용자의 토큰은 보존된다")
        fun keepsOtherUsersTokens() {
            store.store(RefreshTokenAudience.MOBILE, "mine", 1L, "f1", oneHour)
            store.store(RefreshTokenAudience.MOBILE, "theirs", 2L, "f2", oneHour)

            store.deleteByUserId(RefreshTokenAudience.MOBILE, 1L)

            assertThat(store.exists(RefreshTokenAudience.MOBILE, "theirs")).isTrue()
        }
    }

    @Nested
    @DisplayName("Token Family 무효화 (탈취 감지)")
    inner class FamilyRevocation {

        @Test
        @DisplayName("무효화한 family 는 revoked 로 판정된다")
        fun revokedFamilyIsDetected() {
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", oneHour)

            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "f1")).isTrue()
        }

        @Test
        @DisplayName("무효화 기록이 만료되면 해제된다 — Redis TTL 동작 정합")
        fun expiredRevocationIsReleased() {
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", expired)

            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "f1")).isFalse()
        }

        @Test
        @DisplayName("동일 family 재무효화는 UNIQUE 위반 없이 멱등하며 차단 기간을 연장한다")
        fun reRevokeIsIdempotentAndExtends() {
            // 먼저 곧 만료될 무효화를 넣고, 이어서 장기 무효화를 요청
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", expired)
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", oneHour)

            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "f1")).isTrue()
        }

        @Test
        @DisplayName("재무효화가 기존의 더 긴 차단 기간을 단축하지 않는다")
        fun reRevokeDoesNotShortenExistingWindow() {
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", oneHour)
            store.revokeFamily(RefreshTokenAudience.MOBILE, "f1", expired)

            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "f1")).isTrue()
        }
    }

    @Nested
    @DisplayName("탈취 감지 무효화의 트랜잭션 독립성")
    inner class RevocationSurvivesRollback {

        /**
         * 호출부(AuthService.refreshAccessToken / WebAuthenticationService.refresh)는 revokeFamily 직후
         * TokenReuseDetectedException 을 던지고, 두 메서드 모두 @Transactional 이라 그 예외가 트랜잭션을
         * 롤백시킨다. 무효화가 호출부 트랜잭션에 참여하면 롤백과 함께 사라져 탈취 감지가 무력화된다.
         * (Redis 구현에는 없던, DB 전환으로 새로 생긴 함정)
         */
        @Test
        @Transactional(propagation = Propagation.NOT_SUPPORTED)
        @DisplayName("호출부 트랜잭션이 롤백돼도 family 무효화는 살아남는다")
        fun revocationCommitsIndependentlyOfCallerRollback() {
            val familyId = "fam-rollback-guard"
            val txTemplate = TransactionTemplate(transactionManager)

            // 호출부를 재현: 트랜잭션 안에서 revokeFamily 후 예외 → 롤백
            assertThatThrownBy {
                txTemplate.execute {
                    store.revokeFamily(RefreshTokenAudience.MOBILE, familyId, oneHour)
                    throw IllegalStateException("TokenReuseDetected 재현")
                }
            }.isInstanceOf(IllegalStateException::class.java)

            try {
                assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, familyId)).isTrue()
            } finally {
                // 본 테스트만 ambient 트랜잭션이 없어 실제 커밋되므로 직접 정리한다
                familyRevocationRepository.findByAudienceAndFamilyId(RefreshTokenAudience.MOBILE, familyId)
                    ?.let { familyRevocationRepository.delete(it) }
            }
        }
    }

    @Nested
    @DisplayName("만료행 정리 (Redis TTL 대체 배치)")
    inner class CleanupExpired {

        @Test
        @DisplayName("만료행만 삭제하고 유효행은 보존한다")
        fun deletesOnlyExpiredRows() {
            store.store(RefreshTokenAudience.MOBILE, "alive", 1L, "f1", oneHour)
            store.store(RefreshTokenAudience.MOBILE, "dead", 2L, "f2", expired)
            store.revokeFamily(RefreshTokenAudience.MOBILE, "fam-alive", oneHour)
            store.revokeFamily(RefreshTokenAudience.WEB, "fam-dead", expired)

            val (tokens, revocations) = store.deleteExpired()

            assertThat(tokens).isEqualTo(1)
            assertThat(revocations).isEqualTo(1)
            assertThat(store.exists(RefreshTokenAudience.MOBILE, "alive")).isTrue()
            assertThat(store.isFamilyRevoked(RefreshTokenAudience.MOBILE, "fam-alive")).isTrue()
        }
    }
}
