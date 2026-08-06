package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * 유통기한 만료 FCM 알림 배치([ProductExpirationAlertBatch]) 가 dev / prod 프로파일에서
 * 발화하도록 `application.yml` 의 enabled 플래그가 켜져 있는지 정적 검증.
 *
 * 이 배치는 `@ConditionalOnProperty(... matchIfMissing = false)` + `@Profile("dev | prod")`
 * 라서, 빈 생성·발화에는 환경별 yml 에 `app.batch.product-expiration-alert.enabled=true` 가
 * 명시되어 있어야 한다. 본 테스트는 누군가 이 플래그를 제거/비활성화해 알림 배치가
 * 조용히 멈추는 회귀를 방지한다. (레거시 OttogiSalesSchedule.alarm 운영 정합 — 항상 ON 이어야 함)
 *
 * dev/prod 전용 설정은 `application-{profile}.yml` 에 있다 — 운영 설정 로딩은
 * [ProfileConfigReader] 가 담당한다 (base 병합 / test classpath 우회 규칙 포함).
 */
@DisplayName("유통기한 알림 배치 프로파일 활성화 가드 — dev/prod 에서 enabled")
class ProductExpirationAlertBatchProfileEnabledTest {

    private fun docForProfile(profile: String): Map<String, Any?> =
        ProfileConfigReader.effectiveConfig(profile)

    @Suppress("UNCHECKED_CAST")
    private fun alertEnabled(doc: Map<String, Any?>): Any? {
        return ((((doc["app"] as? Map<String, Any?>)
            ?.get("batch") as? Map<String, Any?>)
            ?.get("product-expiration-alert") as? Map<String, Any?>)
            ?.get("enabled"))
    }

    @Test
    @DisplayName("dev 프로파일에서 product-expiration-alert 가 enabled=true")
    fun `dev profile enables product expiration alert batch`() {
        val dev = docForProfile("dev")
        assertThat(alertEnabled(dev))
            .withFailMessage("dev 프로파일에서 ProductExpirationAlertBatch 가 비활성 — 유통기한 알림 미발송 회귀")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("prod 프로파일에서 product-expiration-alert 가 enabled=true")
    fun `prod profile enables product expiration alert batch`() {
        val prod = docForProfile("prod")
        assertThat(alertEnabled(prod))
            .withFailMessage("prod 프로파일에서 ProductExpirationAlertBatch 가 비활성 — 유통기한 알림 미발송 회귀")
            .isEqualTo(true)
    }
}
