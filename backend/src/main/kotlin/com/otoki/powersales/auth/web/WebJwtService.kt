package com.otoki.powersales.auth.web

import com.otoki.powersales.common.security.JwtTokenProvider
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import java.util.Date
import javax.crypto.SecretKey

/**
 * Web 인증 토큰 발급/검증 (Spec #760).
 *
 * Mobile 의 [JwtTokenProvider] 와 동일한 secret 을 공유하되 token claim 구성만 분리:
 * - subject = `User.username` (Mobile: `Employee.id` 문자열)
 * - audience = `"web"` (Mobile: `"mobile"`) — cross-platform 토큰 사용 차단
 * - 추가 claim: `user_id`, `profile_name`, `is_sales_support`, `password_change_required`
 *
 * Refresh Rotation 의 Redis 메타데이터는 Mobile 과 공용 키 공간을 회피하기 위해 별도 prefix(`web_refresh:`)
 * 사용 — 본 spec 범위는 Web 토큰 발급/파싱까지이며, 실 Redis 저장은 [WebRefreshTokenStore] 가 담당한다.
 */
class WebJwtService(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.expiration}") private val accessExpiration: Long,
    @Value("\${jwt.refresh-expiration}") private val refreshExpiration: Long,
) {

    private val key: SecretKey by lazy { Keys.hmacShaKeyFor(secret.toByteArray()) }

    /**
     * Web Access Token 발급.
     *
     * subject = `User.username`, audience = "web". 권한 산출 정보 (`profileName`, `isSalesSupport`,
     * `role`) 는 필터에서 토큰만으로 principal 복원 가능하도록 claim 으로 포함.
     *
     * `role` 은 Employee 미존재 (예: ADMIN-* 부트스트랩 직후) 시 null 일 수 있어 nullable 로 직렬화.
     *
     * permissions claim 은 의도적으로 포함하지 않는다 — spec #808 expandAllDataBits 일반화 이후
     * 365+ key 로 확장되면서 JWT 가 8KB 초과해 nginx `large_client_header_buffers` 한도를 초과
     * (`400 Request Header Or Cookie Too Large`). 가드 평가 시점에 [AdminPermissionCache] 가
     * userId 로 lazy lookup (5min TTL).
     *
     * 로그인 응답 body 의 `user.permissions` (WebUserSummary) 는 web admin UI 의 메뉴 visibility
     * 결정에 사용되므로 유지 — 본 변경은 JWT claim 한 곳에서만 permissions 를 제거.
     */
    fun createAccessToken(principal: WebUserPrincipal, role: String?, impersonatedBy: Long? = null): String {
        val now = Date()
        val expiry = Date(now.time + accessExpiration)
        val builder = Jwts.builder()
            .subject(principal.username)
            .claim("type", "access")
            .claim("audience", JwtTokenProvider.AUDIENCE_WEB)
            .claim("user_id", principal.userId)
            .claim("employee_id", principal.employeeId)
            .claim("employee_code", principal.employeeCode)
            .claim("cost_center_code", principal.costCenterCode)
            .claim("profile_name", principal.profileName)
            .claim("is_sales_support", principal.isSalesSupport)
            .claim("password_change_required", principal.passwordChangeRequired)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiry)
        // 대행 토큰 — 실제 관리자 user_id. 일반 토큰은 claim 미부착 (§851 §2.0).
        if (impersonatedBy != null) {
            builder.claim("impersonated_by", impersonatedBy)
        }
        return builder.signWith(key).compact()
    }

    /**
     * Web Refresh Token 발급 (Rotation 지원).
     *
     * family_id: 최초 로그인 시 생성된 family UUID — 재발급 시 동일 유지
     * token_id: 매 갱신 시 새로 생성되는 개별 token UUID — 재사용 감지 키
     */
    fun createRefreshToken(
        username: String,
        userId: Long,
        familyId: String,
        tokenId: String,
        impersonatedBy: Long? = null,
    ): String {
        val now = Date()
        val expiry = Date(now.time + refreshExpiration)
        val builder = Jwts.builder()
            .subject(username)
            .claim("type", "refresh")
            .claim("audience", JwtTokenProvider.AUDIENCE_WEB)
            .claim("user_id", userId)
            .claim("family_id", familyId)
            .claim("token_id", tokenId)
            .issuedAt(now)
            .expiration(expiry)
        // 대행 refresh — claim 보존하여 access 만료 후에도 대행 세션 유지 (§851 Q1).
        if (impersonatedBy != null) {
            builder.claim("impersonated_by", impersonatedBy)
        }
        return builder.signWith(key).compact()
    }

    /**
     * 토큰 검증 — 서명 + 만료 + audience="web" 확인.
     *
     * audience 가 누락이거나 "mobile" 인 토큰은 Web FilterChain 에서 거부 (false 반환).
     */
    fun validateAccessToken(token: String): Boolean {
        return try {
            val claims = parseClaims(token)
            if (claims.expiration.before(Date())) return false
            val audience = claims.get("audience", String::class.java)
            val type = claims.get("type", String::class.java)
            audience == JwtTokenProvider.AUDIENCE_WEB && type == "access"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Refresh Token 검증 — 서명 + 만료 + audience="web" + type="refresh".
     */
    fun validateRefreshToken(token: String): Boolean {
        return try {
            val claims = parseClaims(token)
            if (claims.expiration.before(Date())) return false
            val audience = claims.get("audience", String::class.java)
            val type = claims.get("type", String::class.java)
            audience == JwtTokenProvider.AUDIENCE_WEB && type == "refresh"
        } catch (_: Exception) {
            false
        }
    }

    /**
     * 토큰이 만료 상태인지 (서명은 유효) — 401 응답 시 `TOKEN_EXPIRED` 코드 분기 입력.
     */
    fun isTokenExpired(token: String): Boolean = try {
        parseClaims(token)
        false
    } catch (_: ExpiredJwtException) {
        true
    } catch (_: Exception) {
        false
    }

    /** subject (= username) 추출. */
    fun getUsernameFromToken(token: String): String = parseClaims(token).subject

    /** user_id claim 추출 (User PK). */
    fun getUserIdFromToken(token: String): Long = parseClaims(token).get("user_id", java.lang.Long::class.java).toLong()

    /** employee_id claim 추출 (Employee PK) — Employee 미존재 시 null. */
    fun getEmployeeIdFromToken(token: String): Long? =
        parseClaims(token).get("employee_id", java.lang.Long::class.java)?.toLong()

    /** profile_name claim 추출. Profile.name SoT. */
    fun getProfileNameFromToken(token: String): String? =
        parseClaims(token).get("profile_name", String::class.java)

    /** is_sales_support claim 추출. */
    fun getIsSalesSupportFromToken(token: String): Boolean =
        parseClaims(token).get("is_sales_support", java.lang.Boolean::class.java)?.booleanValue() ?: false

    /** employee_code claim 추출. */
    fun getEmployeeCodeFromToken(token: String): String =
        parseClaims(token).get("employee_code", String::class.java)

    /** cost_center_code claim 추출 — Employee 미존재 또는 미할당 시 null. */
    fun getCostCenterCodeFromToken(token: String): String? =
        parseClaims(token).get("cost_center_code", String::class.java)

    /** password_change_required claim 추출. */
    fun getPasswordChangeRequiredFromToken(token: String): Boolean =
        parseClaims(token).get("password_change_required", java.lang.Boolean::class.java)?.booleanValue() ?: false

    /** role claim 추출 — Employee 미존재 시 null. */
    fun getRoleFromToken(token: String): String? =
        parseClaims(token).get("role", String::class.java)

    /** family_id claim 추출 (refresh token rotation). */
    fun getFamilyIdFromToken(token: String): String =
        parseClaims(token).get("family_id", String::class.java)

    /** token_id claim 추출 (refresh token rotation). */
    fun getTokenIdFromToken(token: String): String =
        parseClaims(token).get("token_id", String::class.java)

    /** impersonated_by claim 추출 (대행 토큰의 실제 관리자 user_id) — 일반 토큰은 null. */
    fun getImpersonatedByFromToken(token: String): Long? =
        parseClaims(token).get("impersonated_by", java.lang.Long::class.java)?.toLong()

    /** access token 만료 시간 (초 단위). */
    fun getAccessTokenExpirationSeconds(): Int = (accessExpiration / 1000).toInt()

    /** refresh token 만료 시간 (밀리초). */
    fun getRefreshExpirationMillis(): Long = refreshExpiration

    private fun parseClaims(token: String): Claims =
        Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
}
