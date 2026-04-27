package com.otoki.powersales.sap.auth.service

import com.otoki.powersales.sap.auth.config.SapAuthProperties
import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.stereotype.Component
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

/**
 * SAP 인바운드 전용 JWT 발급/검증. Mobile/Admin JWT 와는 별도 시그니처 키를 사용한다.
 */
@Component
class SapJwtCodec(
    private val properties: SapAuthProperties
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
}

data class IssuedToken(
    val token: String,
    val jti: String,
    val expiresIn: Int
)
