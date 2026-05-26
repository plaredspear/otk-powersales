package com.otoki.powersales.common.integration.orora.config

import com.otoki.powersales.orora.repository.OroraDailySalesHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Spec #823 §9.3 — Q7 (a) component scan 무결성 검증.
 *
 * `@Profile("dev | prod")` 가드가 [OroraJpaConfig] 와 하위 빈 ([com.otoki.powersales.orora.repository.OroraDailySalesHistoryRepository] 등)
 * 에 정상 적용되어, local / test 프로파일에서는 본 빈들이 컨텍스트에 등록되지 않음을 검증.
 *
 * `@SpringBootTest` 가 아닌 [ApplicationContextRunner] 를 사용하여 본 스펙의 가드만 격리 검증
 * (전체 Spring Boot 컨텍스트 부팅에 무관한 baseline 통합 테스트 이슈 회피).
 */
@DisplayName("ORORA JPA 프로파일 가드 — local/test 무결성 검증")
class OroraJpaProfileGuardTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(OroraJpaConfig::class.java)

	@Test
	@DisplayName("local 프로파일에서 OroraJpaConfig 가 컨텍스트에 등록되지 않는다")
	fun `local profile excludes OroraJpaConfig`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).doesNotHaveBean(OroraJpaConfig::class.java)
				assertThat(context).doesNotHaveBean("ororaEntityManagerFactory")
				assertThat(context).doesNotHaveBean("ororaTransactionManager")
			}
	}

	@Test
	@DisplayName("test 프로파일에서 OroraJpaConfig 가 컨텍스트에 등록되지 않는다")
	fun `test profile excludes OroraJpaConfig`() {
		runner.withPropertyValues("spring.profiles.active=test")
			.run { context ->
				assertThat(context).doesNotHaveBean(OroraJpaConfig::class.java)
				assertThat(context).doesNotHaveBean("ororaEntityManagerFactory")
				assertThat(context).doesNotHaveBean("ororaTransactionManager")
			}
	}

	@Test
	@DisplayName("local 프로파일에서 ORORA 환경변수 미설정 상태로도 정상 기동")
	fun `local profile starts up without orora env vars`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasNotFailed()
			}
	}

	@Test
	@DisplayName("Repository 클래스명이 OroraDailySalesHistoryRepository 인지 검증 (메인 측 sales.DailySalesHistoryRepository 와 분리)")
	fun `repository class name distinguishes from main sales repository`() {
		// 메인 측 sales.repository.DailySalesHistoryRepository 와 본 ORORA Repository 의
		// simple class name 충돌이 없어야 Spring Bean name 충돌 (BeanDefinitionOverrideException)
		// 회피. 본 단언은 향후 클래스명 변경 시 회귀 방지.
		assertThat(OroraDailySalesHistoryRepository::class.simpleName)
			.isEqualTo("OroraDailySalesHistoryRepository")
	}
}
