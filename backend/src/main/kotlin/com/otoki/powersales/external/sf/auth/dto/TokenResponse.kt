package com.otoki.powersales.external.sf.auth.dto

import com.fasterxml.jackson.annotation.JsonProperty

/**
 * RFC 6749 §5.1 표준 토큰 응답.
 *
 * SF Apex (Named Credential / 직접 HTTP callout) 가 응답에서 `access_token` (snake_case) 키를
 * 찾기 때문에 Jackson 기본 camelCase 직렬화를 `@JsonProperty` 로 명시 오버라이드 한다.
 */
data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,

    @JsonProperty("token_type")
    val tokenType: String,

    @JsonProperty("expires_in")
    val expiresIn: Int,

    val scope: String
)
