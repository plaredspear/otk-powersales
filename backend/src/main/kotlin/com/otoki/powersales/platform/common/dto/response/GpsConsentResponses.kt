package com.otoki.powersales.platform.common.dto.response

data class GpsConsentTermsResponse(
    val agreementNumber: String?,
    val contents: String?
)

data class GpsConsentStatusResponse(
    val requiresGpsConsent: Boolean
)

data class GpsConsentRecordResponse(
    val accessToken: String,
    val expiresIn: Int
)
