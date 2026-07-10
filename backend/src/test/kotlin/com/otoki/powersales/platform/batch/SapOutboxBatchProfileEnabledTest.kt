package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * SAP outbox 송신 워커([SapOutboxBatch]) 가 dev / prod 프로파일에서 발화하도록
 * `application.yml` 의 `app.sap.outbox.enabled` 플래그가 켜져 있는지 정적 검증.
 *
 * [SapOutboxBatch] 는 `@ConditionalOnProperty(... matchIfMissing = false)` 라서
 * 플래그가 어떤 프로파일에도 없으면 빈 자체가 등록되지 않고, 런타임 토글로도 켤 수 없다.
 * 실제로 플래그 부재로 전 환경에서 워커가 미등록되어 RETRY 건 재시도가 멈춘 사건이
 * 있었다 — 본 테스트는 그 회귀(플래그 제거/누락으로 워커가 조용히 멈춤)를 방지한다.
 *
 * `application.yml` 은 `---` 멀티 도큐먼트라 SnakeYAML `loadAll` 로 문서별로 분리해
 * 각 `spring.config.activate.on-profile` 문서의 `app.sap.outbox.enabled` 를 확인한다.
 *
 * 주의: classpath 에는 `src/test/resources/application.yml` (테스트 전용 단일 문서) 이
 * main 보다 우선해 올라오므로, 검증 대상인 운영 `application.yml` 은
 * `src/main/resources` 경로를 직접 읽는다 (테스트 실행 cwd = `backend/`).
 */
@DisplayName("SAP outbox 워커 프로파일 활성화 가드 — dev/prod 에서 enabled")
class SapOutboxBatchProfileEnabledTest {

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
