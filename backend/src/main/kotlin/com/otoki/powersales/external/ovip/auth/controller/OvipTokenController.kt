package com.otoki.powersales.external.ovip.auth.controller

import com.otoki.powersales.external.ovip.auth.dto.TokenRequest
import com.otoki.powersales.external.ovip.auth.dto.TokenResponse
import com.otoki.powersales.external.ovip.auth.service.OvipTokenService
import com.otoki.powersales.external.ovip.auth.util.BasicAuthHeader
import com.otoki.powersales.external.ovip.auth.util.ClientIpResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * OVIP 인바운드 OAuth 2.0 토큰 발급 컨트롤러.
 *
 * 자격증명 전송 방식 (RFC 6749 §2.3.1) — 두 가지 모두 지원:
 * - `client_secret_post`: `client_id` / `client_secret` 을 본문 (form 또는 JSON) 에 포함
 * - `client_secret_basic`: `Authorization: Basic base64(client_id:client_secret)` 헤더 사용
 *
 * 둘 다 도착한 경우 본문 값을 우선한다.
 */
@RestController
@RequestMapping("/api/v1/ovip/oauth")
class OvipTokenController(
    private val ovipTokenService: OvipTokenService
) {

    @PostMapping(
        "/token",
        consumes = [MediaType.APPLICATION_FORM_URLENCODED_VALUE]
    )
    fun issueTokenForm(
        @RequestParam("grant_type", required = false) grantType: String?,
        @RequestParam("client_id", required = false) clientId: String?,
        @RequestParam("client_secret", required = false) clientSecret: String?,
        @RequestParam("scope", required = false) scope: String?,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
        request: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val basic = BasicAuthHeader.parse(authHeader)
        val tokenRequest = TokenRequest(
            grantType = grantType,
            clientId = clientId.takeUnless { it.isNullOrBlank() } ?: basic?.clientId,
            clientSecret = clientSecret.takeUnless { it.isNullOrBlank() } ?: basic?.clientSecret,
            scope = scope
        )
        val response = ovipTokenService.issue(tokenRequest, ClientIpResolver.resolve(request))
        return ResponseEntity.ok(response)
    }

    @PostMapping(
        "/token",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun issueTokenJson(
        @RequestBody body: TokenRequest,
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authHeader: String?,
        request: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val basic = BasicAuthHeader.parse(authHeader)
        val tokenRequest = body.copy(
            clientId = body.clientId.takeUnless { it.isNullOrBlank() } ?: basic?.clientId,
            clientSecret = body.clientSecret.takeUnless { it.isNullOrBlank() } ?: basic?.clientSecret
        )
        val response = ovipTokenService.issue(tokenRequest, ClientIpResolver.resolve(request))
        return ResponseEntity.ok(response)
    }
}
