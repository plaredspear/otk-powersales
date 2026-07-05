package com.otoki.powersales.platform.common.config

import com.otoki.powersales.platform.common.exception.FeatureNotYetEnabledException
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * 운영(prod) 환경에서 아직 열지 않은 등록 기능을 차단하는 게이트.
 *
 * 주문 등록 / 주문(제품) 클레임 등록 / 물류 클레임 등록은 SF dual-write 연동을 포함하며,
 * 운영 환경에서는 관련 부서 협의 전까지 열지 않는다. 각 등록 서비스 진입부에서
 * [assertRegistrationEnabled] 를 호출하면 prod 프로파일일 때만 [FeatureNotYetEnabledException] 을
 * 던져 400 으로 응답한다. dev/local/test 등 그 외 프로파일에서는 무동작(통과).
 *
 * prod 여부 판별은 `Environment.activeProfiles` 대조 방식 관례를 따른다
 * (예: [com.otoki.powersales.platform.batch.toggle.ScheduledJobToggleStore]).
 */
@Component
class ProdFeatureGate(
    environment: Environment,
) {
    /** prod 프로파일이 활성일 때만 true. */
    private val isProd: Boolean =
        environment.activeProfiles.any { it == PROD_PROFILE }

    /**
     * 운영 환경이면 등록을 차단한다. prod 가 아니면 무동작.
     *
     * @throws FeatureNotYetEnabledException prod 프로파일에서 호출된 경우
     */
    fun assertRegistrationEnabled() {
        if (isProd) {
            throw FeatureNotYetEnabledException()
        }
    }

    companion object {
        private const val PROD_PROFILE = "prod"
    }
}
