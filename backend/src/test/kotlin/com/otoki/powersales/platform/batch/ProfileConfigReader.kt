package com.otoki.powersales.platform.batch

import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * 배치 프로파일 활성화 가드 테스트들이 공유하는 운영 설정 리더.
 *
 * 각 배치 잡은 `@ConditionalOnProperty(... matchIfMissing = false)` 라서 플래그가 프로파일 설정에 없으면
 * 빈 자체가 등록되지 않고 런타임 토글로도 켤 수 없다(실제로 플래그 누락으로 워커가 조용히 멈춘 사건이
 * 있었다). 가드 테스트들은 그 회귀를 막기 위해 운영 설정 파일을 정적으로 읽어 확인한다.
 *
 * dev/prod 전용 설정은 `application-{profile}.yml` 로 분리되어 있다 — 과거 `application.yml` 의
 * `---` 멀티 도큐먼트(`spring.config.activate.on-profile`)였고, 그때는 문서를 스캔했다.
 * 프로파일 파일에 없으면 base(`application.yml` 의 첫 문서)로 fallback 하므로 base 에 못 박은
 * 기본값도 함께 검증된다.
 *
 * 주의: classpath 에는 `src/test/resources/application.yml` (테스트 전용 단일 문서) 이 main 보다
 * 우선해 올라오므로, 검증 대상인 운영 설정은 `src/main/resources` 경로를 직접 읽는다
 * (테스트 실행 cwd = `backend/`).
 */
object ProfileConfigReader {

    /**
     * [profile] 에서 유효한 설정 트리를 반환한다 — `application-<profile>.yml` 을 base 위에 얕은 병합.
     *
     * 병합은 최상위 key 기준이 아니라 재귀적이라, 프로파일 파일이 `app.batch.x` 만 덮어써도 base 의
     * `app.sf.*` 가 사라지지 않는다 (Spring 의 프로퍼티 override 의미와 동일).
     */
    fun effectiveConfig(profile: String): Map<String, Any?> {
        val base = loadDocs(configFile("application.yml")).firstOrNull() ?: emptyMap()
        val profileFile = File("src/main/resources/application-$profile.yml")
        check(profileFile.exists()) { "프로파일 설정 파일을 찾지 못함: ${profileFile.absolutePath}" }
        val override = loadDocs(profileFile).firstOrNull() ?: emptyMap()
        return deepMerge(base, override)
    }

    /** `app.batch.claim-master.sync.enabled` 같은 dot-path 로 값을 읽는다. 경로가 끊기면 null. */
    @Suppress("UNCHECKED_CAST")
    fun valueAt(config: Map<String, Any?>, path: String): Any? =
        path.split('.').fold(config as Any?) { node, key ->
            (node as? Map<String, Any?>)?.get(key)
        }

    private fun configFile(name: String): File =
        File("src/main/resources/$name").also {
            check(it.exists()) { "운영 설정 파일을 찾지 못함: ${it.absolutePath}" }
        }

    private fun loadDocs(file: File): List<Map<String, Any?>> =
        file.inputStream().use { Yaml().loadAll(it).filterIsInstance<Map<String, Any?>>() }

    @Suppress("UNCHECKED_CAST")
    private fun deepMerge(base: Map<String, Any?>, override: Map<String, Any?>): Map<String, Any?> {
        val merged = base.toMutableMap()
        override.forEach { (key, value) ->
            val existing = merged[key]
            merged[key] = if (existing is Map<*, *> && value is Map<*, *>) {
                deepMerge(existing as Map<String, Any?>, value as Map<String, Any?>)
            } else {
                value
            }
        }
        return merged
    }
}
