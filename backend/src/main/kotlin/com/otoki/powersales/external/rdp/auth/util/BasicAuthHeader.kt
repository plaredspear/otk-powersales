package com.otoki.powersales.external.rdp.auth.util

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * RFC 6749 §2.3.1 `client_secret_basic` 헤더 파서.
 *
 * 형식: `Authorization: Basic <base64(URL-encode(client_id) ":" URL-encode(client_secret))>`
 *
 * 손상된 헤더(잘못된 base64 / `:` 누락 / URL-decode 실패 등)는 모두 `null` 을 반환한다.
 * 호출자는 본문 자격증명으로 fallback 하거나 INVALID_CLIENT 로 진행한다.
 */
object BasicAuthHeader {

    private const val PREFIX = "Basic "

    data class Credentials(val clientId: String, val clientSecret: String)

    fun parse(authHeader: String?): Credentials? {
        if (authHeader.isNullOrBlank()) return null
        if (!authHeader.startsWith(PREFIX, ignoreCase = true)) return null

        val encoded = authHeader.substring(PREFIX.length).trim()
        if (encoded.isEmpty()) return null

        val decoded = runCatching {
            String(Base64.getDecoder().decode(encoded), StandardCharsets.UTF_8)
        }.getOrNull() ?: return null

        // secret 이 ':' 를 포함할 수 있으므로 lastIndexOf 가 아닌 첫 번째 ':' 으로 split
        val colonIdx = decoded.indexOf(':')
        if (colonIdx < 0) return null

        val rawId = decoded.substring(0, colonIdx)
        val rawSecret = decoded.substring(colonIdx + 1)

        return runCatching {
            Credentials(
                clientId = URLDecoder.decode(rawId, StandardCharsets.UTF_8),
                clientSecret = URLDecoder.decode(rawSecret, StandardCharsets.UTF_8)
            )
        }.getOrNull()
    }
}
