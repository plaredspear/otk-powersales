package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * 전문행사조 마스터 배치([PPTMasterSyncBatch] / [PPTMasterExpireBatch]) 가
 * dev / prod 프로파일에서 발화하도록 `application.yml` 의 enabled 플래그가
 * 켜져 있는지 정적 검증.
 *
 * 두 배치는 `@ConditionalOnProperty(... matchIfMissing = false)` + `@Profile("dev | prod")`
 * 라서, 빈 생성·발화에는 환경별 yml 에 `app.batch.ppt-master.<job>.enabled=true` 가
 * 명시되어 있어야 한다. 본 테스트는 누군가 이 플래그를 제거/비활성화해 두 배치가
 * 조용히 멈추는 회귀를 방지한다. (SF Batch_PPTMaster1/2 운영 정합 — 항상 ON 이어야 함)
 *
 * `application.yml` 은 `---` 멀티 도큐먼트라 SnakeYAML `loadAll` 로 문서별로 분리해
 * 각 `spring.config.activate.on-profile` 문서의 `app.batch.ppt-master.*.enabled` 를 확인한다.
 *
 * 주의: classpath 에는 `src/test/resources/application.yml` (테스트 전용 단일 문서) 이
 * main 보다 우선해 올라오므로, 검증 대상인 운영 `application.yml` 은
 * `src/main/resources` 경로를 직접 읽는다 (테스트 실행 cwd = `backend/`).
 */
@DisplayName("전문행사조 배치 프로파일 활성화 가드 — dev/prod 에서 enabled")
class PPTMasterBatchProfileEnabledTest {

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
    private fun pptEnabled(doc: Map<String, Any?>, job: String): Any? {
        return (((((doc["app"] as? Map<String, Any?>)
            ?.get("batch") as? Map<String, Any?>)
            ?.get("ppt-master") as? Map<String, Any?>)
            ?.get(job) as? Map<String, Any?>)
            ?.get("enabled"))
    }

    @Test
    @DisplayName("dev 프로파일에서 sync / expire 가 모두 enabled=true")
    fun `dev profile enables both ppt master batches`() {
        val dev = docForProfile("dev")
        assertThat(pptEnabled(dev, "sync"))
            .withFailMessage("dev 프로파일에서 PPTMasterSyncBatch 가 비활성 — SF Batch_PPTMaster1 정합 위배")
            .isEqualTo(true)
        assertThat(pptEnabled(dev, "expire"))
            .withFailMessage("dev 프로파일에서 PPTMasterExpireBatch 가 비활성 — SF Batch_PPTMaster2 정합 위배")
            .isEqualTo(true)
    }

    @Test
    @DisplayName("prod 프로파일에서 sync / expire 가 모두 enabled=true")
    fun `prod profile enables both ppt master batches`() {
        val prod = docForProfile("prod")
        assertThat(pptEnabled(prod, "sync"))
            .withFailMessage("prod 프로파일에서 PPTMasterSyncBatch 가 비활성 — SF Batch_PPTMaster1 정합 위배")
            .isEqualTo(true)
        assertThat(pptEnabled(prod, "expire"))
            .withFailMessage("prod 프로파일에서 PPTMasterExpireBatch 가 비활성 — SF Batch_PPTMaster2 정합 위배")
            .isEqualTo(true)
    }
}
