package com.otoki.powersales.external.common.outboundlog

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 외부 API 호출 로그에 요청/응답 **본문**을 캡처할지 여부의 SoT.
 *
 * local / dev profile 에서만 본문을 적재한다 (개발 디버깅 목적). 운영(prod) 및 그 외 환경은
 * PII/용량 보호를 위해 status / 소요시간 / 예외만 기록하고 본문은 남기지 않는다.
 *
 * 각 RestClient config 가 [enabled] 를 [ExternalApiLogInterceptor] 의 `captureBody` 로 넘긴다.
 */
@Component
class ExternalApiLogBodyCapture(environment: Environment) {

    /** local 또는 dev profile 이 활성일 때만 true. */
    val enabled: Boolean =
        environment.activeProfiles.any { it in BODY_CAPTURE_PROFILES }

    companion object {
        private val BODY_CAPTURE_PROFILES = setOf("local", "dev")
    }
}
