package com.otoki.powersales.external.sf.outbound

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * SF outbound 환경 설정 — `sf.outbound.*` prefix (Spec #829).
 *
 * - `apexBaseUrl`: Apex REST endpoint prefix (예: `https://ottogi.my.salesforce.com/services/apexrest/mobile`).
 *   본 prefix 에 `/ClaimRegist` 등 endpoint suffix 가 붙어 호출된다.
 * - `oauth.*`: OAuth 2.0 password grant 파라미터. dev/prod 는 Secret Manager 분기 권고.
 */
@ConfigurationProperties(prefix = "sf.outbound")
data class SfOutboundProperties(
    val apexBaseUrl: String = "",
    val oauth: OAuthProps = OAuthProps(),
) {
    data class OAuthProps(
        val tokenUrl: String = "",
        val clientId: String = "",
        val clientSecret: String = "",
        val username: String = "",
        val password: String = "",
    )
}
