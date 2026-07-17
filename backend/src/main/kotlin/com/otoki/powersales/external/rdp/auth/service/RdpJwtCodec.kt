package com.otoki.powersales.external.rdp.auth.service

import com.otoki.powersales.external.rdp.auth.config.RdpAuthProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * RDP 인바운드 전용 JWT 발급/검증. SAP / SF / Mobile / Admin JWT 와 별도 시그니처 키 + 발급자(`iss`) 를
 * 사용하여 토큰 교차 사용을 차단한다.
 */
@Component
class RdpJwtCodec(
    private val properties: RdpAuthProperties
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(properties.jwtSigningKey.toByteArray())
    }

    fun issue(clientId: String, scopes: List<String>, issuedAt: Date = Date()): IssuedToken {
        val ttlMillis = properties.tokenTtlSeconds * 1000
        val expiry = Date(issuedAt.time + ttlMillis)
        val jti = UUID.randomUUID().toString()
        val token = Jwts.builder()
            .id(jti)
            .issuer(ISSUER)
            .subject(clientId)
            .claim("scope", scopes.joinToString(" "))
            .issuedAt(issuedAt)
            .expiration(expiry)
            .signWith(key)
            .compact()
        return IssuedToken(token = token, jti = jti, expiresIn = properties.tokenTtlSeconds.toInt())
    }

    fun parse(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(ISSUER)
            .build()
            .parseSignedClaims(token)
            .payload
    }

    fun isExpired(token: String): Boolean {
        return try {
            parse(token)
            false
        } catch (e: ExpiredJwtException) {
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val ISSUER = "otoki-powersales-rdp"
    }
}

data class IssuedToken(
    val token: String,
    val jti: String,
    val expiresIn: Int
)
