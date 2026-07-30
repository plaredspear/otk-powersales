package com.otoki.powersales.platform.auth.web

import com.otoki.powersales.platform.auth.token.RefreshTokenAudience
import com.otoki.powersales.platform.auth.token.RefreshTokenStore

/**
 * Web Refresh Token 메타데이터 저장소 (Spec #760).
 *
 * Mobile [com.otoki.powersales.platform.common.security.JwtTokenProvider] 와 저장 공간을 분리해
 * 동일 userId 가 Mobile/Web 을 동시 사용해도 token rotation 이 서로 영향 주지 않도록 격리한다.
 * 격리 축은 [RefreshTokenAudience.WEB] 이며, 실제 저장은 공용 [RefreshTokenStore] (DB SoT) 가 맡는다.
 *
 * ## Redis → DB 전환 (본 클래스가 그 발단)
 *
 * 이전 구현은 Redis 키 3종(`web_refresh:` / `web_user_refresh:` / `web_refresh_family:`) 을 직접
 * 다뤘고, Redis 미가동 시 [store] 가 곧바로 RedisConnectionFailureException 을 던져 **웹 로그인이
 * 500 으로 실패**했다. 이제 refresh 계열은 DB 를 SoT 로 쓰므로 Redis 장애와 무관하게 동작한다.
 *
 * **주의 — WEB 의 `userId` 는 `users.users_id`** 이고 MOBILE 의 `userId`(= `employee.employee_id`)
 * 와 다른 id 공간이다. audience 없이 userId 만으로 조회하면 두 채널이 충돌한다.
 */
class WebRefreshTokenStore(
    private val refreshTokenStore: RefreshTokenStore,
) {

    /** Refresh Token 메타데이터 저장 (발급/rotation 시). */
    fun store(tokenId: String, userId: Long, familyId: String, ttlMillis: Long) {
        refreshTokenStore.store(RefreshTokenAudience.WEB, tokenId, userId, familyId, ttlMillis)
    }

    /**
     * Refresh Token 을 원자적으로 소비 (검증 + 회수 동시).
     *
     * @return `false` = 이미 사용됐거나 만료된 토큰 → 재사용(탈취) 판정. 호출부가 [revokeFamily]
     *  후 `TOKEN_REUSE_DETECTED` 로 차단한다.
     *  구 `exists` + `delete` 2단계는 동시 요청에서 둘 다 통과해 탈취 감지가 무력화됐다
     *  ([RefreshTokenStore.consume] KDoc 참조).
     */
    fun consume(tokenId: String): Boolean =
        refreshTokenStore.consume(RefreshTokenAudience.WEB, tokenId)

    /** Family 전체 무효화 (탈취 감지 시) — 동일 family 의 후속 refresh 시도 모두 차단. */
    fun revokeFamily(familyId: String, ttlMillis: Long) {
        refreshTokenStore.revokeFamily(RefreshTokenAudience.WEB, familyId, ttlMillis)
    }

    /** Family 무효화 여부 확인. */
    fun isFamilyRevoked(familyId: String): Boolean =
        refreshTokenStore.isFamilyRevoked(RefreshTokenAudience.WEB, familyId)

    /**
     * 로그아웃 시 사용자별 refresh token 회수.
     *
     * 구 Redis 구현은 `web_user_refresh:<userId>` 포인터가 가리키는 최신 1건만 지웠으나, 현재는
     * 해당 사용자의 WEB refresh token 을 전량 회수한다 ([RefreshTokenStore.deleteByUserId] 참조).
     */
    fun deleteByUserId(userId: Long) {
        refreshTokenStore.deleteByUserId(RefreshTokenAudience.WEB, userId)
    }
}
