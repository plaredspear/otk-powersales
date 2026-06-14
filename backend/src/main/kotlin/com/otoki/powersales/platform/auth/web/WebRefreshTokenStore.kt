package com.otoki.powersales.platform.auth.web

import org.springframework.data.redis.core.RedisTemplate
import tools.jackson.databind.ObjectMapper
import java.time.Duration

/**
 * Web Refresh Token Redis 메타데이터 저장소 (Spec #760).
 *
 * Mobile [com.otoki.powersales.platform.common.security.JwtTokenProvider] 와 분리된 키 prefix(`web_refresh:`)
 * 사용 — 동일 userId 가 Mobile/Web 동시 사용 시 token rotation 이 서로 영향 주지 않도록 격리.
 *
 * - `web_refresh:<tokenId>` → token 메타 JSON (userId, familyId, issuedAt, expiresAt)
 * - `web_user_refresh:<userId>` → 현재 활성 tokenId (사용자별 단일 활성 토큰)
 * - `web_refresh_family:<familyId>` → "revoked" (탈취 감지 시 family 전체 무효화)
 */
class WebRefreshTokenStore(
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper,
) {

    /**
     * Refresh Token 메타데이터 저장 (rotation 시).
     */
    fun store(tokenId: String, userId: Long, familyId: String, ttlMillis: Long) {
        val metadata = mapOf(
            "userId" to userId,
            "familyId" to familyId,
            "issuedAt" to System.currentTimeMillis(),
            "expiresAt" to System.currentTimeMillis() + ttlMillis
        )
        val json = objectMapper.writeValueAsString(metadata)
        val ttl = Duration.ofMillis(ttlMillis)
        redisTemplate.opsForValue().set("$REFRESH_PREFIX$tokenId", json, ttl)
        redisTemplate.opsForValue().set("$USER_REFRESH_PREFIX$userId", tokenId, ttl)
    }

    /** Refresh Token 메타데이터 삭제 (rotation 이전 토큰 회수). */
    fun delete(tokenId: String) {
        redisTemplate.delete("$REFRESH_PREFIX$tokenId")
    }

    /** Refresh Token 메타데이터 존재 여부 — false 면 재사용 시도 (탈취 감지 입력). */
    fun exists(tokenId: String): Boolean =
        redisTemplate.hasKey("$REFRESH_PREFIX$tokenId") == true

    /** Family 전체 무효화 (탈취 감지 시) — 동일 family 의 후속 refresh 시도 모두 차단. */
    fun revokeFamily(familyId: String, ttlMillis: Long) {
        redisTemplate.opsForValue().set(
            "$FAMILY_REVOKED_PREFIX$familyId",
            "revoked",
            Duration.ofMillis(ttlMillis)
        )
    }

    /** Family 무효화 여부 확인. */
    fun isFamilyRevoked(familyId: String): Boolean =
        redisTemplate.hasKey("$FAMILY_REVOKED_PREFIX$familyId") == true

    /** 로그아웃 시 사용자별 활성 refresh token 회수. */
    fun deleteByUserId(userId: Long) {
        val tokenId = redisTemplate.opsForValue().get("$USER_REFRESH_PREFIX$userId")
        if (tokenId != null) {
            redisTemplate.delete("$REFRESH_PREFIX$tokenId")
        }
        redisTemplate.delete("$USER_REFRESH_PREFIX$userId")
    }

    companion object {
        private const val REFRESH_PREFIX = "web_refresh:"
        private const val USER_REFRESH_PREFIX = "web_user_refresh:"
        private const val FAMILY_REVOKED_PREFIX = "web_refresh_family:"
    }
}
