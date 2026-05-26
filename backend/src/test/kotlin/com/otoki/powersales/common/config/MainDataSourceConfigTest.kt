package com.otoki.powersales.common.config

import com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.context.ConfigurableApplicationContext
import javax.sql.DataSource

/**
 * 메인 DataSource 가 ORORA DataSource 와 공존할 때 `@Primary` + `@FlywayDataSource`
 * 우선순위가 메인에 부여되어 Flyway / JPA 가 메인 RDS 만 잡도록 보장하는지 검증한다.
 *
 * Spec #695 v1.0 부팅 사고 (ORORA 빈 등장 → 메인 DataSource 자동구성 중단 →
 * Flyway 가 ORORA 를 잡고 TCP timeout 으로 부팅 실패) 회귀 방지.
 */
@DisplayName("MainDataSourceConfig 단위 테스트")
class MainDataSourceConfigTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			org.springframework.boot.autoconfigure.AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(MainDataSourceConfig::class.java, OroraDataSourceConfig::class.java)
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
	@DisplayName("dev 프로파일에서 메인 dataSource 와 ororaDataSource 가 동시에 등록되고 메인이 @Primary 이다")
	fun `main dataSource is primary alongside ororaDataSource on dev`() {
		runner.withPropertyValues("spring.profiles.active=dev")
			.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context).hasBean("dataSource")
				assertThat(context).hasBean("ororaDataSource")

				val beanFactory = (context as ConfigurableApplicationContext).beanFactory as DefaultListableBeanFactory
				assertThat(beanFactory.getBeanDefinition("dataSource").isPrimary).isTrue()
				assertThat(beanFactory.getBeanDefinition("ororaDataSource").isPrimary).isFalse()
			}
	}

	@Test
	@DisplayName("dev 프로파일에서 메인 dataSource 빈에만 @FlywayDataSource 가 부착되어 있다")
	fun `flyway datasource annotation is attached to main only on dev`() {
		runner.withPropertyValues("spring.profiles.active=dev")
			.run { context ->
				val mainAnnotated = context.findAnnotationOnBean("dataSource", FlywayDataSource::class.java)
				val ororaAnnotated = context.findAnnotationOnBean("ororaDataSource", FlywayDataSource::class.java)

				assertThat(mainAnnotated).isNotNull
				assertThat(ororaAnnotated).isNull()
			}
	}

	@Test
	@DisplayName("DataSource 타입 자동 주입 시 메인 RDS 가 선택된다 (ORORA 가 아님)")
	fun `dataSource autowire picks main not orora on dev`() {
		runner.withPropertyValues("spring.profiles.active=dev")
			.run { context ->
				val resolved = context.getBean(DataSource::class.java)
				assertThat(resolved).isSameAs(context.getBean("dataSource", DataSource::class.java))
				assertThat(resolved).isNotSameAs(context.getBean("ororaDataSource", DataSource::class.java))
			}
	}

	@Test
	@DisplayName("local 프로파일에서는 ororaDataSource 가 부재해도 메인 dataSource 가 정상 등록된다")
	fun `local profile registers only main dataSource`() {
		runner.withPropertyValues("spring.profiles.active=local")
			.run { context ->
				assertThat(context).hasNotFailed()
				assertThat(context).hasBean("dataSource")
				assertThat(context).doesNotHaveBean("ororaDataSource")
				assertThat(context.getBean("dataSource", DataSource::class.java)).isInstanceOf(HikariDataSource::class.java)
			}
	}
}
