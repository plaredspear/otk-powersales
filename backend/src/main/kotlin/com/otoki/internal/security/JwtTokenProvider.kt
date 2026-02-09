package com.otoki.internal.security

import com.otoki.internal.entity.UserRole
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
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
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    // Phase 1: 인메모리 블랙리스트 (Phase 2에서 Redis로 교체)
    private val blacklist = ConcurrentHashMap<String, Date>()

    /**
     * Access Token 생성
     */
    fun createAccessToken(userId: Long, role: UserRole): String {
        val now = Date()
        val expiry = Date(now.time + accessExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("role", role.name)
            .claim("type", "access")
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /**
     * Refresh Token 생성
     */
    fun createRefreshToken(userId: Long): String {
        val now = Date()
        val expiry = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
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
     * 토큰 타입 확인 (access / refresh)
     */
    fun getTokenType(token: String): String {
        return parseClaims(token).get("type", String::class.java)
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
