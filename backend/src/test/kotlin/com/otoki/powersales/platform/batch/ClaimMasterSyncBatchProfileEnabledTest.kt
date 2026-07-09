package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * SF 클레임/물류클레임 상태 업데이트 통합 배치([ClaimMasterSyncBatch]) 의 도메인별 enabled 플래그가
 * dev / prod 프로파일에서 켜져 있는지 정적 검증.
 *
 * 통합 잡은 `@ConditionalOnProperty("app.batch.claim-master.sync.enabled", matchIfMissing = false)` +
 * `@Profile("dev | prod")` 로 게이팅되고, 잡 내부 오케스트레이터([ClaimMasterSyncBatchService]) 가
 * claim / logistics 를 각각 `claim-master.sync.enabled` / `logistics-claim-master.sync.enabled` 로 재판정한다.
 *
 * ## 고정하는 게이팅 계약
 *  - claim / logistics 두 플래그가 dev·prod 에서 모두 true (통합으로 둘 다 발화하는 운영 기본).
 *  - **잡 빈 게이팅 = claim 플래그** 라는 결합: `claim-master.sync.enabled=false` 로 잡을 끄면 빈 자체가
 *    비발화 → logistics 도 함께 멎는다. 이 결합을 명시적으로 문서화·고정해, 누군가 claim 플래그를 끄면서
 *    "logistics 는 계속 돌겠지" 라고 오해하는 회귀를 방지한다.
 *
 * `application.yml` 은 `---` 멀티 도큐먼트라 SnakeYAML `loadAll` 로 문서별로 분리해
 * 각 `spring.config.activate.on-profile` 문서의 플래그를 확인한다. (선행 [ProductExpirationAlertBatchProfileEnabledTest] 정합.)
 * 검증 대상은 운영 `src/main/resources/application.yml` — test classpath 우선 로드를 피해 파일을 직접 읽는다.
 */
@DisplayName("클레임/물류클레임 통합 sync 배치 프로파일 활성화 가드 — dev/prod 에서 도메인별 enabled")
class ClaimMasterSyncBatchProfileEnabledTest {

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
    private fun syncEnabled(doc: Map<String, Any?>, domainKey: String): Any? {
        return ((((doc["app"] as? Map<String, Any?>)
            ?.get("batch") as? Map<String, Any?>)
            ?.get(domainKey) as? Map<String, Any?>)
            ?.get("sync") as? Map<String, Any?>)
            ?.get("enabled")
    }

    @Test
    @DisplayName("dev 프로파일에서 claim / logistics 두 도메인 모두 enabled=true")
    fun `dev profile enables both claim domains`() {
        val dev = docForProfile("dev")
        assertThat(syncEnabled(dev, "claim-master"))
            .withFailMessage("dev 에서 claim-master.sync 비활성 — 통합 잡 자체가 비발화(logistics 도 멎음)")
            .isEqualTo(true)
        assertThat(syncEnabled(dev, "logistics-claim-master"))
            .withFailMessage("dev 에서 logistics-claim-master.sync 비활성 — 물류클레임 상태 업데이트 회귀")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("prod 프로파일에서 claim / logistics 두 도메인 모두 enabled=true")
    fun `prod profile enables both claim domains`() {
        val prod = docForProfile("prod")
        assertThat(syncEnabled(prod, "claim-master"))
            .withFailMessage("prod 에서 claim-master.sync 비활성 — 통합 잡 자체가 비발화(logistics 도 멎음)")
            .isEqualTo(true)
        assertThat(syncEnabled(prod, "logistics-claim-master"))
            .withFailMessage("prod 에서 logistics-claim-master.sync 비활성 — 물류클레임 상태 업데이트 회귀")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("잡 빈 게이팅 플래그(claim-master.sync.enabled) 가 @ConditionalOnProperty 의 name 과 정합")
    fun `job bean gating flag matches ConditionalOnProperty name`() {
        // 통합 잡의 @ConditionalOnProperty name 은 하드코딩 문자열이므로, yml 경로와의 정합을 상수/구조로 재확인한다.
        // ClaimMasterSyncBatch.CRON placeholder 가 claim-master 네임스페이스를 쓰는지로 결합을 간접 고정.
        assertThat(ClaimMasterSyncBatch.CRON).contains("app.batch.claim-master.sync.cron")
        assertThat(ClaimMasterSyncBatch.JOB_NAME).isEqualTo("claimMaster.sync")
    }
}
