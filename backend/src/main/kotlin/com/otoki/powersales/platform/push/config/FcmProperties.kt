package com.otoki.powersales.platform.push.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * FCM 발송 설정 (`app.push.fcm.*`).
 *
 * `credentialJson` 은 Firebase 서비스 계정 키 JSON 원문으로, 운영에서는 AWS Secrets Manager / 환경변수로
 * 주입한다. 미설정(blank) 이면 [com.otoki.powersales.platform.push.sender.RealFcmSender] 가 초기화되지
 * 않고 발송은 no-op (부팅/배치 실패 없이 graceful skip) 으로 동작한다.
 *
 * @property enabled 발송 활성 여부 (기본 false — credential 주입 + 명시적 활성화 시에만 실제 발송)
 * @property credentialJson Firebase 서비스 계정 키 JSON 원문 (null/blank 면 발송 비활성)
 */
@ConfigurationProperties(prefix = "app.push.fcm")
data class FcmProperties(
    val enabled: Boolean = false,
    val credentialJson: String? = null,
)
