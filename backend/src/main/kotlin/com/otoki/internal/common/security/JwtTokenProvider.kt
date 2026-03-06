package com.otoki.internal.common.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.otoki.internal.sap.entity.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.SecretKey

/**
 * JWT 토큰 생성 및 검증
 */
@Component
class JwtTokenProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
    private val redisTemplate: RedisTemplate<String, String>,
    private val objectMapper: ObjectMapper
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    // Phase 1: 인메모리 블랙리스트 (Phase 2에서 Redis로 교체)
    private val blacklist = ConcurrentHashMap<String, Date>()

    /**
     * Access Token 생성
     */
    fun createAccessToken(userId: Long, role: UserRole, agreementFlag: Boolean = false): String {
        val now = Date()
        val expiry = Date(now.time + accessExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .claim("type", "access")
            .claim("agreement_flag", agreementFlag)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * Refresh Token 생성 (Rotation 지원)
     * family_id: Token Family ID (최초 로그인 시 생성, UUID)
     * token_id: 개별 Token ID (매 갱신 시 새로 생성, UUID)
     */
    fun createRefreshToken(userId: Long, familyId: String, tokenId: String): String {
        val now = Date()
        val expiry = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .claim("family_id", familyId)
            .claim("token_id", tokenId)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * 토큰 검증 (만료, 서명, 블랙리스트 확인)
     */
    fun validateToken(token: String): Boolean {
        return try {
            if (isBlacklisted(token)) return false
            val claims = parseClaims(token)
            !claims.expiration.before(Date())
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 토큰에서 userId 추출
     */
    fun getUserIdFromToken(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    /**
     * 토큰에서 role 추출
     */
    fun getRoleFromToken(token: String): UserRole {
        val roleName = parseClaims(token).get("role", String::class.java)
        return UserRole.valueOf(roleName)
    }

    /**
     * 토큰에서 agreement_flag 추출
     */
    fun getAgreementFlagFromToken(token: String): Boolean {
        return parseClaims(token).get("agreement_flag", java.lang.Boolean::class.java)?.booleanValue() ?: false
    }

    /**
     * 토큰 타입 확인 (access / refresh)
     */
    fun getTokenType(token: String): String {
        return parseClaims(token).get("type", String::class.java)
    }

    /**
     * 토큰에서 family_id 추출
     */
    fun getFamilyIdFromToken(token: String): String {
        return parseClaims(token).get("family_id", String::class.java)
    }

    /**
     * 토큰에서 token_id 추출
     */
    fun getTokenIdFromToken(token: String): String {
        return parseClaims(token).get("token_id", String::class.java)
    }

    /**
     * 토큰을 블랙리스트에 추가 (로그아웃 시 사용)
     */
    fun blacklistToken(token: String) {
        try {
            val claims = parseClaims(token)
            blacklist[token] = claims.expiration
            cleanExpiredBlacklist()
        } catch (e: ExpiredJwtException) {
            // 이미 만료된 토큰은 블랙리스트에 추가할 필요 없음
        }
    }

    /**
     * Access Token 만료 시간(초) 반환
     */
    fun getAccessTokenExpirationSeconds(): Int {
        return (accessExpiration / 1000).toInt()
    }

    // ========== Redis operations for Refresh Token Rotation ==========

    /**
     * Refresh Token 메타데이터를 Redis에 저장
     */
    fun storeRefreshToken(tokenId: String, userId: Long, familyId: String) {
        val metadata = mapOf(
            "userId" to userId,
            "familyId" to familyId,
            "issuedAt" to System.currentTimeMillis(),
            "expiresAt" to System.currentTimeMillis() + refreshExpiration
        )
        val json = objectMapper.writeValueAsString(metadata)
        val ttl = Duration.ofMillis(refreshExpiration)
        redisTemplate.opsForValue().set("refresh:$tokenId", json, ttl)
        redisTemplate.opsForValue().set("user_refresh:$userId", tokenId, ttl)
    }

    /**
     * Refresh Token을 Redis에서 삭제
     */
    fun deleteRefreshToken(tokenId: String) {
        redisTemplate.delete("refresh:$tokenId")
    }

    /**
     * userId 기반으로 Refresh Token을 Redis에서 삭제 (로그아웃 시 사용)
     */
    fun deleteRefreshTokenByUserId(userId: Long) {
        val tokenId = redisTemplate.opsForValue().get("user_refresh:$userId")
        if (tokenId != null) {
            redisTemplate.delete("refresh:$tokenId")
        }
        redisTemplate.delete("user_refresh:$userId")
    }

    /**
     * Refresh Token이 Redis에 존재하는지 확인
     */
    fun isRefreshTokenStored(tokenId: String): Boolean =
        redisTemplate.hasKey("refresh:$tokenId") == true

    /**
     * Token Family 전체 무효화 (탈취 감지 시)
     */
    fun revokeTokenFamily(familyId: String) {
        redisTemplate.opsForValue().set(
            "refresh_family:$familyId", "revoked", Duration.ofMillis(refreshExpiration)
        )
    }

    /**
     * Token Family가 무효화되었는지 확인
     */
    fun isTokenFamilyRevoked(familyId: String): Boolean =
        redisTemplate.hasKey("refresh_family:$familyId") == true

    /**
     * 토큰이 만료되었는지 확인 (서명은 유효하나 만료 시간 초과)
     */
    fun isTokenExpired(token: String): Boolean {
        return try {
            parseClaims(token)
            false // 정상 파싱 → 아직 만료되지 않음
        } catch (e: ExpiredJwtException) {
            true // 서명 유효, 만료됨
        } catch (e: Exception) {
            false // 서명 오류 등 만료가 아닌 다른 이유
        }
    }

    private fun isBlacklisted(token: String): Boolean {
        return blacklist.containsKey(token)
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /**
     * 만료된 블랙리스트 항목 정리
     */
    private fun cleanExpiredBlacklist() {
        val now = Date()
        blacklist.entries.removeIf { it.value.before(now) }
    }
}
