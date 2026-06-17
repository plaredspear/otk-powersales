package com.otoki.powersales.platform.push.sender

import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

/**
 * 로컬(`local`) 환경용 FCM 발송 stub — 실제 전송 없이 로그만 남기고 성공으로 간주한다.
 * Firebase credential 없이도 배치/서비스 흐름을 검증할 수 있게 한다.
 */
@Component
@Profile("local")
class StubFcmSender : FcmSender {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun sendNotificationToTokens(tokens: List<String>, title: String, body: String): FcmSendResult {
        if (tokens.isEmpty()) return FcmSendResult.EMPTY
        log.info("[STUB-FCM] 발송 시뮬레이션 — tokens=${tokens.size}, title='$title', body='$body'")
        return FcmSendResult(successCount = tokens.size, failureCount = 0)
    }
}
