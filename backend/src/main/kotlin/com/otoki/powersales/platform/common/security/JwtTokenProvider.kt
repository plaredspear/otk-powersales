package com.otoki.powersales.platform.common.security

import com.otoki.powersales.platform.auth.token.RefreshTokenAudience
import com.otoki.powersales.platform.auth.token.RefreshTokenStore
import com.otoki.powersales.platform.common.util.TimeZones
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeParseException
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
    @Value("\${jwt.refresh-expiration-long}") private val refreshExpirationLong: Long,
    /**
     * 발급시각 컷오프 — KST ISO-8601 local datetime (예: `2026-08-15T02:00:00`). 빈 값이면 비활성.
     * 의미와 운영 절차는 [minIssuedAtMillis] KDoc 참조.
     */
    @Value("\${jwt.min-issued-at:}") private val minIssuedAt: String = "",
    /** access token 블랙리스트 전용 — refresh 계열은 [refreshTokenStore] (DB SoT) 가 담당한다. */
    private val redisTemplate: RedisTemplate<String, String>,
    private val refreshTokenStore: RefreshTokenStore,
) {

    private val log = LoggerFactory.getLogger(JwtTokenProvider::class.java)

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(secret.toByteArray())
    }

    /**
     * 이 시각(epoch millis) 이전에 발급된 토큰을 전부 무효로 본다. `0` = 컷오프 비활성.
     *
     * ## 왜 필요한가 — 데이터 마이그레이션 후 전원 강제 재로그인
     *
     * JWT subject 는 `employee.id` 다. 마이그레이션으로 PK 가 재부여되면 잔존 토큰이 **다른 사원으로
     * 인증될 수 있어**, 만료를 기다리는(access 최대 1h, refresh 최대 60일) 방식으로는 부족하다.
     * refresh token 삭제나 단말 초기화도 access token 잔여 TTL / 단말 검증 면제 사번 구멍이 남는다.
     * 컷오프는 서명·만료와 같은 층에서 로컬 판정하므로 그 구멍이 없다.
     *
     * ## 왜 "기동 시각" 이 아니라 명시적 시각인가
     *
     * 다중 인스턴스(EB) 에서 기동 시각을 쓰면 인스턴스마다 컷오프가 어긋나고, 이후 재기동·오토스케일
     * 마다 전원이 다시 로그아웃된다. 명시 시각은 몇 번을 재기동해도 결과가 같다(멱등).
     *
     * ## 적용 범위 — 모바일 전용
     *
     * 본 클래스는 모바일 인증 전용이며 웹 admin 은 별도 인프라(`WebJwtService` /
     * `WebJwtAuthenticationFilter`) 를 쓴다. 따라서 컷오프는 **모바일 세션만** 끊는다.
     *
     * ## 운영 절차
     *
     * 마이그레이션 컷오버 완료 후 `JWT_MIN_ISSUED_AT` 에 컷오버 시각(KST)을 넣고 재기동한다.
     * 형식 오류면 기동을 실패시킨다 — 조용히 미적용된 채 뜨면 무효화가 안 된 사실을 아무도 모른다.
     */
    private val minIssuedAtMillis: Long = parseMinIssuedAt(minIssuedAt)

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
     * @param longLived 자동로그인 ON 세션 여부. true 면 장수명 TTL([refreshExpirationLong])로
     *  발급하고 `long_lived=true` 클레임을 심는다. 회전(refreshAccessToken)은 이 클레임을 읽어
     *  동일 TTL 을 유지하므로, ON 세션은 회전 후에도 장수명이 이어진다. false(기본)면 7일 TTL.
     * @return audience="mobile" claim 이 박힌 refresh JWT (Spec #760).
     */
    fun createRefreshToken(
        userId: Long,
        familyId: String,
        tokenId: String,
        longLived: Boolean = false
    ): String {
        val now = Date()
        val expiry = Date(now.time + refreshTtlMillis(longLived))

        return Jwts.builder()
            .subject(userId.toString())
            .claim("type", "refresh")
            .claim("audience", AUDIENCE_MOBILE)
            .claim("family_id", familyId)
            .claim("token_id", tokenId)
            .claim("long_lived", longLived)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /** 자동로그인 여부에 따른 refresh token TTL(ms) — ON=장수명(60일), OFF=기본(7일). */
    private fun refreshTtlMillis(longLived: Boolean): Long =
        if (longLived) refreshExpirationLong else refreshExpiration

    /**
     * 토큰 검증 (서명, 만료, 발급시각 컷오프, 블랙리스트 확인).
     *
     * 판정 순서는 **로컬 검증(서명/만료/컷오프) 먼저, 블랙리스트(Redis) 나중**이다.
     * 위조/만료 토큰은 Redis 를 건드리지 않고 탈락하므로, 무인증 트래픽이 Redis 부하로 번지지 않는다.
     */
    fun validateToken(token: String): Boolean {
        return try {
            val claims = parseClaims(token)
            if (claims.expiration.before(Date())) return false
            if (isBeforeCutoff(claims.issuedAt)) return false
            !isBlacklisted(token)
        } catch (e: ExpiredJwtException) {
            false
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 발급시각 컷오프([minIssuedAtMillis]) 이전에 발급된 토큰인지 — **거부 사유 구분 전용**.
     *
     * [validateToken] 이 이미 컷오프를 반영하므로 인증 판정에는 쓰지 않는다. 호출부는 이 값으로
     * 401 응답의 에러 코드를 `SESSION_INVALIDATED` 로 분기해, 앱이 "세션 만료"(재갱신 시도) 대신
     * **즉시 재로그인 안내**로 처리하게 한다. 사유가 뭉개지면 사용자는 갱신 실패를 통신 장애로
     * 오해하고, 앱은 무의미한 refresh 왕복을 한 번 더 한다.
     *
     * 만료 토큰도 판정 대상이다 — 컷오프 이전 발급이면 만료 여부와 무관하게 재로그인이 정답이며,
     * jjwt 의 [ExpiredJwtException] 은 검증된 claims 를 함께 실어 주므로 `iat` 를 그대로 읽는다.
     * 서명 위조/파싱 불가는 `false` 를 반환한다 (컷오프와 무관한 거부이며 [validateToken] 이 막는다).
     */
    fun isIssuedBeforeCutoff(token: String): Boolean {
        if (minIssuedAtMillis == 0L) return false
        val issuedAt = try {
            parseClaims(token).issuedAt
        } catch (e: ExpiredJwtException) {
            e.claims.issuedAt
        } catch (e: Exception) {
            return false
        }
        return isBeforeCutoff(issuedAt)
    }

    /**
     * `iat` 가 컷오프 이전인지. `iat` 부재는 **이전으로 간주**한다 —
     * 현재 발급 경로는 모두 `iat` 를 심으므로, 없는 토큰은 컷오프 이후 발급이 아님이 확실하다.
     */
    private fun isBeforeCutoff(issuedAt: Date?): Boolean {
        if (minIssuedAtMillis == 0L) return false
        return issuedAt == null || issuedAt.time < minIssuedAtMillis
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
     * refresh 토큰의 long_lived(자동로그인 ON) 클레임 추출.
     * 클레임이 없는 구 토큰은 `false`(7일 세션)로 간주해 동작 호환을 유지한다.
     */
    fun getLongLivedFromToken(token: String): Boolean {
        return parseClaims(token).get("long_lived", java.lang.Boolean::class.java)?.booleanValue() ?: false
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

    // ========== Refresh Token Rotation (DB SoT — RefreshTokenStore 위임) ==========
    //
    // 이전에는 본 구간이 Redis 직접 호출이었고, 그 탓에 Redis 장애 = 로그인/토큰갱신 전면 불가였다.
    // 블랙리스트만 Redis 에 남기고(매 요청 조회) refresh 계열은 DB 를 SoT 로 삼는다.
    // 채널 격리는 [RefreshTokenAudience.MOBILE] — 웹은 WebRefreshTokenStore 가 WEB 으로 같은 저장소를 쓴다.

    /**
     * Refresh Token 메타데이터 저장.
     *
     * @param longLived 자동로그인 ON 세션이면 저장 TTL 도 장수명(60일)으로 맞춘다. JWT TTL 과
     *  일치시키지 않으면 7일 뒤 메타데이터가 먼저 소멸해, 아직 유효한 refresh JWT 가
     *  [consumeRefreshToken]=false 로 탈취 오탐되어 family 가 무효화된다.
     */
    fun storeRefreshToken(tokenId: String, userId: Long, familyId: String, longLived: Boolean = false) {
        refreshTokenStore.store(
            RefreshTokenAudience.MOBILE, tokenId, userId, familyId, refreshTtlMillis(longLived)
        )
    }

    /**
     * Refresh Token 을 원자적으로 소비 (검증 + 회수 동시).
     *
     * @return `false` = 이미 사용됐거나 만료된 토큰 → 재사용(탈취) 판정. 호출부가
     *  [revokeTokenFamily] 후 `TOKEN_REUSE_DETECTED` 로 차단한다.
     *  구 `isRefreshTokenStored` + `deleteRefreshToken` 2단계는 동시 요청에서 둘 다 통과해
     *  탈취 감지가 무력화됐다 ([RefreshTokenStore.consume] KDoc 참조).
     */
    fun consumeRefreshToken(tokenId: String): Boolean =
        refreshTokenStore.consume(RefreshTokenAudience.MOBILE, tokenId)

    /**
     * userId 기반 Refresh Token 회수 (로그아웃 / 비밀번호 변경 / 단말 초기화).
     *
     * 구 Redis 구현은 `user_refresh:<userId>` 포인터가 가리키는 최신 1건만 지웠으나, 현재는 해당
     * 사용자의 MOBILE refresh token 을 전량 회수한다 ([RefreshTokenStore.deleteByUserId] 참조).
     */
    fun deleteRefreshTokenByUserId(userId: Long) {
        refreshTokenStore.deleteByUserId(RefreshTokenAudience.MOBILE, userId)
    }

    /**
     * Token Family 전체 무효화 (탈취 감지 시).
     *
     * 차단 기간은 장수명 TTL([refreshExpirationLong]) 기준이다 — 구 구현은 기본 TTL(7일)을 썼으나
     * 자동로그인 ON 세션의 refresh JWT 는 60일까지 유효해, 7일 뒤 차단이 풀리면 탈취된 토큰이
     * 되살아나는 구멍이 있었다. 차단은 family 내 최장수명 토큰보다 오래 유지되어야 한다.
     */
    fun revokeTokenFamily(familyId: String) {
        refreshTokenStore.revokeFamily(RefreshTokenAudience.MOBILE, familyId, refreshExpirationLong)
    }

    /** Token Family 가 무효화되었는지 확인. */
    fun isTokenFamilyRevoked(familyId: String): Boolean =
        refreshTokenStore.isFamilyRevoked(RefreshTokenAudience.MOBILE, familyId)

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

    /**
     * 블랙리스트 등재 여부. **Redis 장애 시 "미등재"(false) 로 fallback 한다.**
     *
     * 조회 실패를 토큰 무효로 처리하면 (구 구현) Redis 장애가 곧 전 사용자 401 로 번진다 —
     * 모바일 401 인터셉터가 이를 세션 만료로 오인해 강제 로그아웃시키고, 재로그인마저
     * refresh token 저장(Redis) 실패로 막혀 완전 락아웃이 된다. 반면 본 fallback 의 대가는
     * 장애 구간 동안 로그아웃 처리된 access token 이 잔여 TTL(최대 `jwt.expiration` = 1h) 만큼
     * 살아남는 것이며, 가용성 사고보다 작은 리스크로 판단한다.
     *
     * 동일 판단이 [ActiveDeviceStore] 에도 적용되어 있다 (캐시 부재를 차단으로 처리하지 않음).
     * 서명/만료는 본 메서드와 무관하게 [validateToken] 이 로컬에서 판정하므로, 이 fallback 이
     * 위조·만료 토큰을 통과시키지는 않는다.
     */
    private fun isBlacklisted(token: String): Boolean {
        return try {
            redisTemplate.hasKey("blacklist:${hashToken(token)}") == true
        } catch (e: DataAccessException) {
            log.warn("블랙리스트 조회 실패 — 미등재로 간주하고 통과(Redis 장애 fallback)", e)
            false
        }
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

        /**
         * `jwt.min-issued-at` 파싱 — KST local datetime → epoch millis. 빈 값이면 `0`(비활성).
         *
         * 형식 오류는 기동 실패로 처리한다. 무시하고 뜨면 "무효화했다고 믿는데 실제로는 전 사용자
         * 세션이 그대로" 인 상태가 되며, 이 설정을 켜는 시점(마이그레이션 컷오버)에는 그 오해가
         * 곧 사고다.
         */
        private fun parseMinIssuedAt(raw: String): Long {
            val value = raw.trim()
            if (value.isEmpty()) return 0L
            return try {
                LocalDateTime.parse(value).atZone(TimeZones.SEOUL_ZONE).toInstant().toEpochMilli()
            } catch (e: DateTimeParseException) {
                throw IllegalStateException(
                    "jwt.min-issued-at 형식 오류: '$raw' — KST 기준 ISO-8601 local datetime 이어야 합니다 (예: 2026-08-15T02:00:00)",
                    e
                )
            }
        }
    }
}
