package com.otoki.powersales.common.integration.orora.config

import com.otoki.powersales.common.integration.orora.health.OroraHealthIndicator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * ORORA DataSource / HealthIndicator 가 모든 환경 (local/test/dev/prod) 에서 등록되는지 검증.
 *
 * VPN 장애 시에도 메인 기능이 정상 부팅되어야 한다는 요구사항을 반영. 부팅 시 connection
 * acquire 실패는 Hikari `initializationFailTimeout=-1` 가 흡수하며, ORORA 환경변수가 비어있는
 * local/test 에서는 호출 site 자체가 없어 acquire 가 발생하지 않는다.
 */
@DisplayName("OroraDataSource 프로파일 가드 검증 — 모든 환경 등록")
class OroraDataSourceProfileGuardTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			org.springframework.boot.autoconfigure.AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(OroraDataSourceConfig::class.java, OroraHealthIndicator::class.java)

	@Test
	@DisplayName("local 프로파일에서도 ororaDataSource / OroraHealthIndicator 빈이 등록된다")
	fun `local profile registers orora beans`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasBean("ororaDataSource")
				assertThat(context).hasSingleBean(OroraHealthIndicator::class.java)
			}
	}

	@Test
	@DisplayName("test 프로파일에서도 ororaDataSource / OroraHealthIndicator 빈이 등록된다")
	fun `test profile registers orora beans`() {
		runner.withPropertyValues("spring.profiles.active=test")
			.run { context ->
				assertThat(context).hasBean("ororaDataSource")
				assertThat(context).hasSingleBean(OroraHealthIndicator::class.java)
			}
	}

	@Test
	@DisplayName("ORORA 환경변수 전혀 미설정 상태에서도 local 프로파일은 정상 기동된다 (initializationFailTimeout=-1)")
	fun `local profile starts up without orora env vars`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context).hasBean("ororaDataSource")
			}
	}
}
