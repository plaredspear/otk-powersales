package com.otoki.powersales.sap.auth.controller

import com.otoki.powersales.sap.auth.dto.TokenRequest
import com.otoki.powersales.sap.auth.dto.TokenResponse
import com.otoki.powersales.sap.auth.service.SapTokenService
import com.otoki.powersales.sap.auth.util.ClientIpResolver
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/sap/oauth")
class SapTokenController(
    private val sapTokenService: SapTokenService
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
        request: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val tokenRequest = TokenRequest(
            grantType = grantType,
            clientId = clientId,
            clientSecret = clientSecret,
            scope = scope
        )
        val response = sapTokenService.issue(tokenRequest, ClientIpResolver.resolve(request))
        return ResponseEntity.ok(response)
    }

    @PostMapping(
        "/token",
        consumes = [MediaType.APPLICATION_JSON_VALUE]
    )
    fun issueTokenJson(
        @RequestBody body: TokenRequest,
        request: HttpServletRequest
    ): ResponseEntity<TokenResponse> {
        val response = sapTokenService.issue(body, ClientIpResolver.resolve(request))
        return ResponseEntity.ok(response)
    }
}
