package com.otoki.powersales.platform.push.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * FCM 발송 설정 (`app.push.fcm.*`).
 *
 * `credentialS3Key` 는 Firebase 서비스 계정 키 JSON 을 담은 S3 객체 key 다 (비공개 버킷, EB 인스턴스
 * IAM 으로 접근). 예: `config/fcm/service-account.json`. 미설정(blank) 이면
 * [com.otoki.powersales.platform.push.sender.RealFcmSender] 가 초기화되지 않고 발송은 no-op
 * (부팅/배치 실패 없이 graceful skip) 으로 동작한다. 키 원문을 코드/yml/환경변수에 직접 두지 않는다.
 *
 * @property enabled 발송 활성 여부 (기본 false — S3 key 주입 + 명시적 활성화 시에만 실제 발송)
 * @property credentialS3Key Firebase 서비스 계정 키 JSON 의 S3 객체 key (null/blank 면 발송 비활성)
 */
@ConfigurationProperties(prefix = "app.push.fcm")
data class FcmProperties(
    val enabled: Boolean = false,
    val credentialS3Key: String? = null,
)
