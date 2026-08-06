package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
 * dev/prod 전용 설정은 `application-{profile}.yml` 에 있다 — 운영 설정 로딩은
 * [ProfileConfigReader] 가 담당한다 (base 병합 / test classpath 우회 규칙 포함).
 */
@DisplayName("전문행사조 배치 프로파일 활성화 가드 — dev/prod 에서 enabled")
class PPTMasterBatchProfileEnabledTest {

    private fun docForProfile(profile: String): Map<String, Any?> =
        ProfileConfigReader.effectiveConfig(profile)

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
