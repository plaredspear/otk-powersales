package com.otoki.powersales.platform.common.security

import tools.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Duration
import java.util.*
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

    /**
     * Mobile (Employee 기반) Access Token 생성.
     *
     * @param passwordChangeRequired 강제 변경 미완료 사원 여부 (Spec #584). `true` 면 가드 필터가
     *  화이트리스트(change-password/logout/refresh) 외 호출을 차단한다.
     * @return audience="mobile" claim 이 박힌 access JWT — Web FilterChain 은 거부 (Spec #760).
     */
    fun createAccessToken(
        userId: Long,
        role: String?,
        agreementFlag: Boolean = false,
        passwordChangeRequired: Boolean = false,
        deviceId: String? = null
    ): String {
        val now = Date()
        val expiry = Date(now.time + accessExpiration)

        val builder = Jwts.builder()
            .subject(userId.toString())
            .claim("role", role)
            .claim("type", "access")
            .claim("audience", AUDIENCE_MOBILE)
            .claim("agreement_flag", agreementFlag)
            .claim("password_change_required", passwordChangeRequired)
            .issuedAt(now)
            .expiration(expiry)

        // 단말 바인딩 검증 대상일 때만 device_id 를 박는다. 클레임 부재 토큰(검증 면제/구 토큰)은
        // 매 요청 필터가 단말 검증을 건너뛴다 (excluded 사번이 잠기는 사고 방지 + 무중단 롤아웃).
        if (deviceId != null) {
            builder.claim("device_id", deviceId)
        }

        return builder
            .signWith(key)
            .compact()
    }

    /**
     * Mobile Refresh Token 생성 (Rotation 지원).
     *
     * family_id: Token Family ID (최초 로그인 시 생성, UUID)
     * token_id: 개별 Token ID (매 갱신 시 새로 생성, UUID)
     * @return audience="mobile" claim 이 박힌 refresh JWT (Spec #760).
     */
    fun createRefreshToken(userId: Long, familyId: String, tokenId: String): String {
        val now = Date()
        val expiry = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .claim("audience", AUDIENCE_MOBILE)
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
     * 토큰에서 role 추출 (SF DKRetail__AppAuthority__c picklist value).
     *
     * 부재 시 null — 호출부가 401(재로그인 필요) 처리.
     */
    fun getRoleFromToken(token: String): String? {
        return parseClaims(token).get("role", String::class.java)
    }

    /**
     * 토큰에서 agreement_flag 추출
     */
    fun getAgreementFlagFromToken(token: String): Boolean {
        return parseClaims(token).get("agreement_flag", java.lang.Boolean::class.java)?.booleanValue() ?: false
    }

    /**
     * 토큰에서 password_change_required 추출 (Spec #584).
     * 클레임이 없는 구 토큰은 `false` 로 간주하여 동작 호환을 유지한다.
     */
    fun getPasswordChangeRequiredFromToken(token: String): Boolean {
        return parseClaims(token).get("password_change_required", java.lang.Boolean::class.java)?.booleanValue() ?: false
    }

    /**
     * 토큰에서 device_id 추출 (단말 바인딩 검증 대상 토큰만 보유).
     * 클레임이 없는 토큰(검증 면제/본 기능 배포 이전)은 `null` → 매 요청 단말 검증 skip.
     */
    fun getDeviceIdFromToken(token: String): String? {
        return try {
            parseClaims(token).get("device_id", String::class.java)
        } catch (_: Exception) {
            null
        }
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
     * 토큰의 audience claim 추출 — "web" / "mobile" / null (구 토큰).
     *
     * Spec #760 — Mobile JWT 로 Web 호출 / Web JWT 로 Mobile 호출 차단을 위해
     * 각 SecurityFilterChain 이 자신의 audience 만 수용하도록 분기.
     * 본 spec 배포 이전 발급된 토큰은 audience claim 부재 → `null` 반환.
     */
    fun getAudienceFromToken(token: String): String? {
        return try {
            parseClaims(token).get("audience", String::class.java)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * 토큰을 블랙리스트에 추가 (로그아웃 시 사용).
     *
     * Redis에 `blacklist:<sha256(token)>` 키를 토큰 잔여 만료시간만큼 TTL로 저장한다.
     * 다중 인스턴스 간 공유 + TTL 자동 정리. (구 인메모리 ConcurrentHashMap 대체)
     */
    fun blacklistToken(token: String) {
        try {
            val claims = parseClaims(token)
            val ttlMillis = claims.expiration.time - System.currentTimeMillis()
            if (ttlMillis > 0) {
                redisTemplate.opsForValue().set(
                    "blacklist:${hashToken(token)}", "1", Duration.ofMillis(ttlMillis)
                )
            }
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
        return redisTemplate.hasKey("blacklist:${hashToken(token)}") == true
    }

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    /** 블랙리스트 Redis 키 길이를 제한하기 위해 토큰을 SHA-256 해시로 변환. */
    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        /** Spec #760 — Web 토큰 audience claim 값. */
        const val AUDIENCE_WEB = "web"

        /** Spec #760 — Mobile 토큰 audience claim 값. */
        const val AUDIENCE_MOBILE = "mobile"
    }
}
