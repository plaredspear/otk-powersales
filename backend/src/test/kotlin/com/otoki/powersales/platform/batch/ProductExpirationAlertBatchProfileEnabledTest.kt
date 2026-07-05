package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * 유통기한 만료 FCM 알림 배치([ProductExpirationAlertBatch]) 가 dev / prod 프로파일에서
 * 발화하도록 `application.yml` 의 enabled 플래그가 켜져 있는지 정적 검증.
 *
 * 이 배치는 `@ConditionalOnProperty(... matchIfMissing = false)` + `@Profile("dev | prod")`
 * 라서, 빈 생성·발화에는 환경별 yml 에 `app.batch.product-expiration-alert.enabled=true` 가
 * 명시되어 있어야 한다. 본 테스트는 누군가 이 플래그를 제거/비활성화해 알림 배치가
 * 조용히 멈추는 회귀를 방지한다. (레거시 OttogiSalesSchedule.alarm 운영 정합 — 항상 ON 이어야 함)
 *
 * `application.yml` 은 `---` 멀티 도큐먼트라 SnakeYAML `loadAll` 로 문서별로 분리해
 * 각 `spring.config.activate.on-profile` 문서의 `app.batch.product-expiration-alert.enabled` 를 확인한다.
 *
 * 주의: classpath 에는 `src/test/resources/application.yml` (테스트 전용 단일 문서) 이
 * main 보다 우선해 올라오므로, 검증 대상인 운영 `application.yml` 은
 * `src/main/resources` 경로를 직접 읽는다 (테스트 실행 cwd = `backend/`).
 */
@DisplayName("유통기한 알림 배치 프로파일 활성화 가드 — dev/prod 에서 enabled")
class ProductExpirationAlertBatchProfileEnabledTest {

    @Suppress("UNCHECKED_CAST")
    private fun docForProfile(profile: String): Map<String, Any?> {
        val yaml = Yaml()
        val ymlFile = File("src/main/resources/application.yml")
        check(ymlFile.exists()) { "운영 application.yml 을 찾지 못함: ${ymlFile.absolutePath}" }
        val docs = ymlFile.inputStream().use { input ->
            yaml.loadAll(input).filterIsInstance<Map<String, Any?>>()
        }
        return docs.firstOrNull { doc ->
            val onProfile = (((doc["spring"] as? Map<String, Any?>)
                ?.get("config") as? Map<String, Any?>)
                ?.get("activate") as? Map<String, Any?>)
                ?.get("on-profile")
            onProfile == profile
        } ?: error("on-profile=$profile 문서를 application.yml 에서 찾지 못함")
    }

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
