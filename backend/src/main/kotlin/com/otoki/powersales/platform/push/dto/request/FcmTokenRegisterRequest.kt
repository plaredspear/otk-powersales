package com.otoki.powersales.platform.push.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

/**
 * FCM 디바이스 토큰 등록 요청 DTO.
 *
 * 모바일 앱이 로그인 성공 / 자동 로그인 / 토큰 갱신(onTokenRefresh) 시점에 전송한다.
 * `fcm_token` 컬럼 길이(200)에 맞춰 검증한다.
 */
data class FcmTokenRegisterRequest(
    @field:NotBlank(message = "FCM 토큰은 필수입니다")
    @field:Size(max = 200, message = "FCM 토큰은 최대 200자입니다")
    val token: String?
)
