package com.otoki.powersales.platform.push.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * FCM 설정 바인딩 진입점 ([FcmProperties] 활성화).
 */
@Configuration
@EnableConfigurationProperties(FcmProperties::class)
class FcmConfig
