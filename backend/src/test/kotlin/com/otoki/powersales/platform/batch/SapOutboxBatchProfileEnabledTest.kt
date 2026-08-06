package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/**
 * SAP outbox 송신 워커([SapOutboxBatch]) 가 dev / prod 프로파일에서 발화하도록
 * `application.yml` 의 `app.sap.outbox.enabled` 플래그가 켜져 있는지 정적 검증.
 *
 * [SapOutboxBatch] 는 `@ConditionalOnProperty(... matchIfMissing = false)` 라서
 * 플래그가 어떤 프로파일에도 없으면 빈 자체가 등록되지 않고, 런타임 토글로도 켤 수 없다.
 * 실제로 플래그 부재로 전 환경에서 워커가 미등록되어 RETRY 건 재시도가 멈춘 사건이
 * 있었다 — 본 테스트는 그 회귀(플래그 제거/누락으로 워커가 조용히 멈춤)를 방지한다.
 *
 * dev/prod 전용 설정은 `application-{profile}.yml` 에 있다 — 운영 설정 로딩은
 * [ProfileConfigReader] 가 담당한다 (base 병합 / test classpath 우회 규칙 포함).
 */
@DisplayName("SAP outbox 워커 프로파일 활성화 가드 — dev/prod 에서 enabled")
class SapOutboxBatchProfileEnabledTest {

    private fun docForProfile(profile: String): Map<String, Any?> =
        ProfileConfigReader.effectiveConfig(profile)

    @Suppress("UNCHECKED_CAST")
    private fun outboxEnabled(doc: Map<String, Any?>): Any? {
        return ((((doc["app"] as? Map<String, Any?>)
            ?.get("sap") as? Map<String, Any?>)
            ?.get("outbox") as? Map<String, Any?>)
            ?.get("enabled"))
    }

    @Test
    @DisplayName("dev 프로파일에서 SAP outbox 워커가 enabled=true")
    fun `dev profile enables sap outbox worker`() {
        assertThat(outboxEnabled(docForProfile("dev")))
            .withFailMessage("dev 프로파일에서 SapOutboxBatch 가 비활성 — RETRY/잔여 PENDING 재처리가 멈춘다")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("prod 프로파일에서 SAP outbox 워커가 enabled=true")
    fun `prod profile enables sap outbox worker`() {
        assertThat(outboxEnabled(docForProfile("prod")))
            .withFailMessage("prod 프로파일에서 SapOutboxBatch 가 비활성 — RETRY/잔여 PENDING 재처리가 멈춘다")
            .isEqualTo(true)
    }
}
