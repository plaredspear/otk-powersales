package com.otoki.powersales.domain.activity.order.service

import com.otoki.powersales.domain.activity.order.exception.OrderRegistrationBlockedException
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 주문 등록/재전송 임시 차단 가드 (운영(prod) 환경 한정).
 *
 * 활성 프로파일에 `prod` 가 포함되면 주문 등록([OrderRequestCreateService.create]) 및
 * 재전송([OrderRequestResendService.resend]) 진입부에서 [OrderRegistrationBlockedException] 을 던져
 * SAP 호출·DB 적재 이전에 전면 차단한다. dev/local 은 영향 없이 정상 동작한다.
 *
 * 임시 조치이므로 별도 설정 토글 없이 프로파일로만 판별한다 — 재개하려면 본 가드 호출을 제거하고
 * 재배포한다.
 */
@Component
class OrderRegistrationBlockGuard(environment: Environment) {

    private val blocked: Boolean =
        environment.activeProfiles.any { it in BLOCKED_PROFILES }

    /** prod 환경이면 즉시 [OrderRegistrationBlockedException] 을 던진다. */
    fun assertNotBlocked() {
        if (blocked) {
            throw OrderRegistrationBlockedException()
        }
    }

    companion object {
        private val BLOCKED_PROFILES = setOf("prod")
    }
}
