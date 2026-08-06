package com.otoki.powersales.platform.batch

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.io.File

/**
 * 프로파일 설정 파일 배치 규약 가드.
 *
 * dev/prod 전용 설정을 `application.yml` 의 `---` 멀티 도큐먼트에서 `application-{profile}.yml` 로
 * 분리했다. 이 구조가 깨지면 [ProfileConfigReader] 를 쓰는 배치 활성화 가드들이 base 값만 보고
 * "설정이 살아 있다" 고 통과해버릴 수 있어(= 가드가 조용히 무력화), 배치 정지 회귀를 놓친다.
 * 따라서 배치 플래그 값 자체와 별개로 **파일 구조**를 여기서 못 박는다.
 */
@DisplayName("프로파일 설정 파일 배치 규약 — dev/prod 전용 설정은 application-{profile}.yml")
class ProfileConfigLayoutTest {

    private fun docs(name: String): List<Map<String, Any?>> {
        val file = File("src/main/resources/$name")
        check(file.exists()) { "설정 파일을 찾지 못함: ${file.absolutePath}" }
        return file.inputStream().use { Yaml().loadAll(it).filterIsInstance<Map<String, Any?>>() }
    }

    @Suppress("UNCHECKED_CAST")
    private fun onProfile(doc: Map<String, Any?>): Any? =
        (((doc["spring"] as? Map<String, Any?>)
            ?.get("config") as? Map<String, Any?>)
            ?.get("activate") as? Map<String, Any?>)
            ?.get("on-profile")

    @Test
    @DisplayName("dev / prod 프로파일 파일이 존재하고 실제 설정을 담는다")
    fun `profile files exist and carry settings`() {
        listOf("dev" to "ottogi-nonsap-dev-imagerepository-s3", "prod" to "ottogi-nonsap-prd-imagerepository-s3")
            .forEach { (profile, expectedBucket) ->
                val config = ProfileConfigReader.effectiveConfig(profile)
                // 프로파일 파일이 비어 있거나 경로가 어긋나면 base 값("")이 잡힌다 — 그 회귀를 값으로 고정.
                assertThat(ProfileConfigReader.valueAt(config, "app.aws.s3.sf-image-bucket"))
                    .withFailMessage("$profile 프로파일 설정이 반영되지 않음 — application-$profile.yml 확인")
                    .isEqualTo(expectedBucket)
                // base 병합도 살아 있어야 한다 (프로파일 파일이 base 를 통째로 대체하지 않음).
                assertThat(ProfileConfigReader.valueAt(config, "app.sf.resend.max-attempt")).isNotNull()
            }
    }

    @Test
    @DisplayName("application.yml 에 남는 profile document 는 dev·prod 공통(Secrets Manager import) 하나뿐")
    fun `only the shared dev-prod document remains in application yml`() {
        val profileDocs = docs("application.yml").filter { onProfile(it) != null }

        // dev 전용 / prod 전용 문서가 다시 들어오면 설정이 두 곳으로 갈라져 어느 쪽이 이기는지 헷갈린다.
        assertThat(profileDocs.map { onProfile(it) })
            .withFailMessage("dev/prod 전용 설정은 application-{profile}.yml 로 옮긴다 — application.yml 에는 공통 문서만 남긴다")
            .containsExactly("dev | prod")
    }

    @Test
    @DisplayName("프로파일 파일은 단일 문서 — on-profile 을 다시 선언하지 않는다")
    fun `profile files are single documents without on-profile`() {
        listOf("application-dev.yml", "application-prod.yml").forEach { name ->
            val parsed = docs(name)
            assertThat(parsed).describedAs("$name 은 단일 문서여야 한다").hasSize(1)
            assertThat(onProfile(parsed.single()))
                .describedAs("$name 은 파일명으로 이미 프로파일이 결정된다")
                .isNull()
        }
    }
}
