package com.otoki.powersales.common.integration.orora.config

import com.otoki.powersales.common.integration.orora.health.OroraHealthIndicator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * Spec #695 — `@Profile("dev | prod")` 가드 효력 검증.
 *
 * local / test 프로파일에서는 ORORA 관련 빈이 컨텍스트에 등록되지 않아야 한다 — VPC Peering 이
 * dev / prod 에만 구성되어 있어 local/test 환경에서는 ORORA 환경변수가 미설정이어도
 * 정상 기동이 보장되어야 한다.
 */
@DisplayName("OroraDataSource 프로파일 가드 검증")
class OroraDataSourceProfileGuardTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			org.springframework.boot.autoconfigure.AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(OroraDataSourceConfig::class.java, OroraHealthIndicator::class.java)

	@Test
	@DisplayName("local 프로파일에서는 ororaDataSource / OroraHealthIndicator 빈이 등록되지 않는다")
	fun `local profile excludes orora beans`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).doesNotHaveBean("ororaDataSource")
				assertThat(context).doesNotHaveBean(OroraHealthIndicator::class.java)
			}
	}

	@Test
	@DisplayName("test 프로파일에서는 ororaDataSource / OroraHealthIndicator 빈이 등록되지 않는다")
	fun `test profile excludes orora beans`() {
		runner.withPropertyValues("spring.profiles.active=test")
			.run { context ->
				assertThat(context).doesNotHaveBean("ororaDataSource")
				assertThat(context).doesNotHaveBean(OroraHealthIndicator::class.java)
			}
	}

	@Test
	@DisplayName("ORORA 환경변수 전혀 미설정 상태에서도 local 프로파일은 정상 기동된다")
	fun `local profile starts up without orora env vars`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasNotFailed()
			}
	}
}
