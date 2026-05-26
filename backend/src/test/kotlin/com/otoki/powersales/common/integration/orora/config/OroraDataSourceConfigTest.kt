package com.otoki.powersales.common.integration.orora.config

import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * Spec #695 — `OroraDataSourceConfig` 단위 테스트.
 *
 * dev / prod 프로파일에서 `ororaDataSource` 빈이 등록되고 §6.2 의 Hikari 파라미터가
 * 정상 바인딩되는지 검증한다. 실제 MSSQL 연결은 수행하지 않는다 (driver class 로딩 + URL
 * 조립 + Hikari 파라미터 매핑 수준).
 */
@DisplayName("OroraDataSourceConfig 단위 테스트")
@Disabled("ORORA 임시 비활성화 (@Profile(\"orora-disabled\")) — dev/prod 에서 ororaDataSource 빈 부재가 의도된 상태. ORORA 재활성화 시 본 어노테이션 제거")
class OroraDataSourceConfigTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			org.springframework.boot.autoconfigure.AutoConfigurations.of(
				PropertyPlaceholderAutoConfiguration::class.java,
			),
		)
		.withUserConfiguration(OroraDataSourceConfig::class.java)
		.withPropertyValues(
			"app.datasource.orora.host=mssql.internal",
			"app.datasource.orora.database=ORORA_DB",
			"app.datasource.orora.username=orora_ro",
			"app.datasource.orora.password=secret",
			"app.datasource.orora.encrypt=false",
			"app.datasource.orora.trust-server-certificate=false",
		)

	@Test
	@DisplayName("dev 프로파일 활성 시 ororaDataSource 빈이 등록되고 MSSQL driver / URL / Hikari 파라미터가 §6.2 와 일치한다")
	fun `dev profile registers ororaDataSource bean with MSSQL driver and configured hikari params`() {
		runner.withPropertyValues("spring.profiles.active=dev")
			.run { context ->
				assertThat(context).hasBean("ororaDataSource")

				val dataSource = context.getBean("ororaDataSource", DataSource::class.java)
				assertThat(dataSource).isInstanceOf(HikariDataSource::class.java)

				val hikari = dataSource as HikariDataSource
				assertThat(hikari.driverClassName).isEqualTo("com.microsoft.sqlserver.jdbc.SQLServerDriver")
				assertThat(hikari.jdbcUrl).isEqualTo("jdbc:sqlserver://mssql.internal;databaseName=ORORA_DB;encrypt=false")
				assertThat(hikari.username).isEqualTo("orora_ro")
				assertThat(hikari.poolName).isEqualTo("ororaHikariPool")
				assertThat(hikari.maximumPoolSize).isEqualTo(5)
				assertThat(hikari.minimumIdle).isEqualTo(1)
				assertThat(hikari.idleTimeout).isEqualTo(600_000)
				assertThat(hikari.maxLifetime).isEqualTo(1_500_000)
				assertThat(hikari.keepaliveTime).isEqualTo(120_000)
				assertThat(hikari.connectionTestQuery).isEqualTo("SELECT 1")
				assertThat(hikari.connectionTimeout).isEqualTo(5_000)
				assertThat(hikari.validationTimeout).isEqualTo(3_000)
				assertThat(hikari.isReadOnly).isTrue()
				assertThat(hikari.isAutoCommit).isTrue()
				assertThat(hikari.initializationFailTimeout).isEqualTo(-1)
			}
	}

	@Test
	@DisplayName("prod 프로파일 활성 시 ororaDataSource 빈이 등록된다")
	fun `prod profile registers ororaDataSource bean`() {
		runner.withPropertyValues("spring.profiles.active=prod")
			.run { context ->
				assertThat(context).hasBean("ororaDataSource")
			}
	}

	@Test
	@DisplayName("encrypt=true 인 경우 JDBC URL 에 trustServerCertificate 파라미터가 부착된다")
	fun `encrypt true appends trustServerCertificate to jdbc url`() {
		runner.withPropertyValues(
			"spring.profiles.active=dev",
			"app.datasource.orora.encrypt=true",
			"app.datasource.orora.trust-server-certificate=true",
		).run { context ->
			val hikari = context.getBean("ororaDataSource", DataSource::class.java) as HikariDataSource
			assertThat(hikari.jdbcUrl)
				.isEqualTo("jdbc:sqlserver://mssql.internal;databaseName=ORORA_DB;encrypt=true;trustServerCertificate=true")
		}
	}

	@Test
	@DisplayName("encrypt=false 일 때는 trustServerCertificate 가 JDBC URL 에 부착되지 않는다")
	fun `encrypt false omits trustServerCertificate from jdbc url`() {
		runner.withPropertyValues(
			"spring.profiles.active=dev",
			"app.datasource.orora.encrypt=false",
			"app.datasource.orora.trust-server-certificate=true",
		).run { context ->
			val hikari = context.getBean("ororaDataSource", DataSource::class.java) as HikariDataSource
			assertThat(hikari.jdbcUrl).doesNotContain("trustServerCertificate")
			assertThat(hikari.jdbcUrl).endsWith("encrypt=false")
		}
	}
}
