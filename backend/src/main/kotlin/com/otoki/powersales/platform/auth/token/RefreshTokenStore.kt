package com.otoki.powersales.platform.auth.token

import com.otoki.powersales.platform.auth.token.entity.RefreshToken
import com.otoki.powersales.platform.auth.token.entity.RefreshTokenFamilyRevocation
import com.otoki.powersales.platform.auth.token.repository.RefreshTokenFamilyRevocationRepository
import com.otoki.powersales.platform.auth.token.repository.RefreshTokenRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * Refresh Token Rotation 저장소 (DB SoT).
 *
 * 모바일([com.otoki.powersales.platform.common.security.JwtTokenProvider]) 과
 * 웹([com.otoki.powersales.platform.auth.web.WebRefreshTokenStore]) 공용이며, 채널 격리는
 * [RefreshTokenAudience] 로 한다.
 *
 * ## Redis → DB 전환 배경
 *
 * 이전에는 refresh 메타데이터가 Redis 에만 존재해, Redis 장애가 곧 **로그인/토큰갱신 전면 불가**
 * 였다 (store 호출이 곧바로 RedisConnectionFailureException → 500). access token 블랙리스트는
 * 매 요청 조회라 DB 직행이 부담이므로 Redis 에 남기고(장애 시 미등재 fallback), 사용자당 시간당
 * 1회 수준인 refresh 계열만 DB 를 SoT 로 삼는다.
 *
 * ## 핵심 의미론 — "행의 존재 = 아직 사용되지 않은 유효 토큰"
 *
 * 갱신 시 이전 행을 삭제하므로, 이미 삭제된 tokenId 로 갱신이 들어오면 재사용(탈취) 이다.
 * 검증과 회수는 [consume] 하나로 원자적으로 수행하며(2단계로 나누면 동시 요청이 둘 다 통과),
 * 호출부는 [consume] 이 false 면 [revokeFamily] 로 family 전체를 차단한다.
 * 만료행이 정리 배치 전까지 물리적으로 잔존하므로, 모든 유효성 판정은 `expiresAt > now` 를
 * 함께 본다 (Redis TTL 이 해 주던 일).
 */
@Service
class RefreshTokenStore(
    private val refreshTokenRepository: RefreshTokenRepository,
    private val familyRevocationRepository: RefreshTokenFamilyRevocationRepository,
) {

    /**
     * 발급/갱신된 refresh token 메타데이터 저장.
     *
     * @param ttlMillis refresh JWT 자체의 TTL 과 반드시 일치시킨다. 더 짧으면 아직 유효한 JWT 가
     *  [exists]=false 로 탈취 오탐되어 family 가 무효화된다 (자동로그인 ON 의 60일 세션 주의).
     */
    @Transactional
    fun store(
        audience: RefreshTokenAudience,
        tokenId: String,
        userId: Long,
        familyId: String,
        ttlMillis: Long,
    ) {
        val now = LocalDateTime.now()
        refreshTokenRepository.save(
            RefreshToken(
                audience = audience,
                tokenId = tokenId,
                userId = userId,
                familyId = familyId,
                issuedAt = now,
                expiresAt = now.plus(Duration.ofMillis(ttlMillis)),
            )
        )
    }

    /**
     * 갱신(rotation) 시 refresh token 을 **원자적으로 소비**한다 — 검증과 회수를 한 번에.
     *
     * @return `true` = 유효 토큰이었고 방금 회수됨(정상 갱신 진행).
     *  `false` = 이미 사용됐거나 만료된 토큰 → **재사용(탈취) 판정** → 호출부가 [revokeFamily] 후
     *  `TOKEN_REUSE_DETECTED` 로 차단한다.
     *
     * ## 왜 exists + delete 2단계가 아니라 원자적 소비인가
     *
     * 동일 토큰으로 동시 요청 2건이 오면 2단계 구현은 **둘 다 exists=true 를 통과**해 각자 새
     * 토큰을 발급받는다. 그 결과 같은 family 에 유효 토큰이 2개 생기고, 이후 각자 정상 회전하므로
     * **재사용 감지가 영영 발동하지 않는다** — 탈취범이 조용히 병행 세션을 갖게 되어 rotation
     * 기반 탈취 감지가 통째로 무력화된다.
     *
     * 단일 DELETE 는 DB 행 잠금으로 정확히 하나만 성공시키므로, 진 쪽이 곧바로 탈취로 판정된다.
     * (Redis 구현에서는 Lua/WATCH 가 필요했으나 DB SoT 전환으로 조건부 DELETE 하나면 충분해졌다.)
     */
    @Transactional
    fun consume(audience: RefreshTokenAudience, tokenId: String): Boolean =
        refreshTokenRepository.consume(audience, tokenId, LocalDateTime.now()) == 1

    /**
     * 해당 채널의 사용자 refresh token 전량 회수 (로그아웃 / 비밀번호 변경 / 단말 초기화).
     *
     * Redis 구현은 `user_refresh:<userId>` 포인터가 가리키는 **최신 1건만** 지웠기에, 로그아웃
     * 없이 재로그인을 반복한 사용자의 옛 refresh token 이 TTL(최대 60일) 동안 살아남았다.
     * 세 호출부 모두 의도가 "이 사용자의 세션을 회수한다" 이므로 전량 삭제로 정합을 맞춘다.
     */
    @Transactional
    fun deleteByUserId(audience: RefreshTokenAudience, userId: Long) {
        refreshTokenRepository.deleteByAudienceAndUserId(audience, userId)
    }

    /**
     * 유효(미만료 + 미사용) refresh token 존재 여부 — **조회 전용**.
     *
     * 회전 검증에는 쓰지 말 것. 조회 후 삭제하는 2단계는 동시 요청에서 둘 다 통과해 탈취 감지가
     * 무력화된다 ([consume] 의 KDoc 참조).
     */
    @Transactional(readOnly = true)
    fun exists(audience: RefreshTokenAudience, tokenId: String): Boolean =
        refreshTokenRepository.existsByAudienceAndTokenIdAndExpiresAtAfter(
            audience, tokenId, LocalDateTime.now()
        )

    /**
     * Token Family 전체 무효화 (탈취 감지 시).
     *
     * 이미 무효화된 family 면 차단 만료만 연장한다 — UNIQUE(audience, family_id) 위반 없이 멱등.
     *
     * ## REQUIRES_NEW 인 이유 (필수 — 바꾸면 탈취 감지가 무력화된다)
     *
     * 호출부는 본 메서드를 호출한 **직후 `TokenReuseDetectedException` 을 던진다**
     * ([com.otoki.powersales.platform.auth.service.AuthService.refreshAccessToken],
     * [com.otoki.powersales.platform.auth.web.service.WebAuthenticationService.refresh]).
     * 두 메서드 모두 `@Transactional` 이므로 그 RuntimeException 은 트랜잭션을 롤백시킨다.
     * 무효화 기록이 호출부 트랜잭션에 참여하면 **롤백과 함께 사라져 family 가 차단되지 않고**,
     * 공격자는 탈취한 refresh token 으로 계속 재시도할 수 있다.
     *
     * Redis 구현에서는 저장이 트랜잭션 밖이라 이 문제가 없었다 — DB 전환으로 새로 생긴 함정이라
     * 별도 트랜잭션으로 분리해 호출부 롤백과 무관하게 커밋시킨다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun revokeFamily(audience: RefreshTokenAudience, familyId: String, ttlMillis: Long) {
        val now = LocalDateTime.now()
        val expiresAt = now.plus(Duration.ofMillis(ttlMillis))
        val existing = familyRevocationRepository.findByAudienceAndFamilyId(audience, familyId)
        if (existing != null) {
            existing.extendUntil(expiresAt)
            return
        }
        familyRevocationRepository.save(
            RefreshTokenFamilyRevocation(
                audience = audience,
                familyId = familyId,
                revokedAt = now,
                expiresAt = expiresAt,
            )
        )
    }

    /** Family 무효화 여부. 만료된 무효화 기록은 Redis TTL 만료와 동일하게 해제로 본다. */
    @Transactional(readOnly = true)
    fun isFamilyRevoked(audience: RefreshTokenAudience, familyId: String): Boolean =
        familyRevocationRepository.existsByAudienceAndFamilyIdAndExpiresAtAfter(
            audience, familyId, LocalDateTime.now()
        )

    /**
     * 만료행 물리 정리 (Redis TTL 대체). 배치 [com.otoki.powersales.platform.batch.RefreshTokenCleanupBatch] 진입점.
     *
     * @return 삭제된 (refresh token, family 무효화) 행 수
     */
    @Transactional
    fun deleteExpired(): Pair<Int, Int> {
        val now = LocalDateTime.now()
        return refreshTokenRepository.deleteExpired(now) to familyRevocationRepository.deleteExpired(now)
    }
}
