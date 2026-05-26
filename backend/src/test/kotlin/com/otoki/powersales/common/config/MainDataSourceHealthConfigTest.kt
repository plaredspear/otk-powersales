package com.otoki.powersales.common.config

import com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.health.contributor.HealthContributor
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator
import org.springframework.boot.test.context.runner.ApplicationContextRunner

/**
 * `dbHealthContributor` 명시 등록 → Spring Boot 의 자동 헬스 인디케이터가
 * ORORA DataSource 를 포함하지 못하도록 격리되는지 검증.
 *
 * 사고 이력: Spec #695 v1.0 ORORA 인프라 PR 후 `/actuator/health` 호출 시 자동등록된
 * `DataSourceHealthIndicator` 가 ORORA MSSQL 에 connection 요청 → 5초 timeout 으로
 * 헬스 응답이 DOWN 으로 떨어지고 ALB 가 인스턴스를 빼는 경로.
 */
@DisplayName("MainDataSourceHealthConfig 단위 테스트")
class MainDataSourceHealthConfigTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			org.springframework.boot.autoconfigure.AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(
			MainDataSourceConfig::class.java,
			MainDataSourceHealthConfig::class.java,
			OroraDataSourceConfig::class.java,
		)
		.withPropertyValues(
			"spring.datasource.url=jdbc:h2:mem:main",
			"spring.datasource.driver-class-name=org.h2.Driver",
			"spring.datasource.username=sa",
			"spring.datasource.password=",
			"app.datasource.orora.host=mssql.internal",
			"app.datasource.orora.database=ORORA_DB",
			"app.datasource.orora.username=orora_ro",
			"app.datasource.orora.password=secret",
		)

	@Test
	@DisplayName("dev 프로파일에서 dbHealthContributor 빈이 우리 정의로 등록된다 (DataSourceHealthIndicator 타입, 메인 DataSource 단일)")
	fun `dbHealthContributor is registered as our explicit bean on dev`() {
		runner.withPropertyValues("spring.profiles.active=dev")
			.run { context ->
				assertThat(context).hasBean("dbHealthContributor")

				val contributor = context.getBean("dbHealthContributor", HealthContributor::class.java)
				assertThat(contributor).isInstanceOf(DataSourceHealthIndicator::class.java)
			}
	}

	@Test
	@DisplayName("local 프로파일에서도 dbHealthContributor 가 메인 DataSource 만 포함하여 등록된다")
	fun `dbHealthContributor is registered on local with main only`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasBean("dbHealthContributor")
				assertThat(context).doesNotHaveBean("ororaDataSource")

				val contributor = context.getBean("dbHealthContributor", HealthContributor::class.java)
				assertThat(contributor).isInstanceOf(DataSourceHealthIndicator::class.java)
			}
	}
}
