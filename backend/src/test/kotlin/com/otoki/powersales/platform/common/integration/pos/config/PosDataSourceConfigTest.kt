package com.otoki.powersales.platform.common.integration.pos.config

import com.otoki.powersales.platform.common.integration.pos.config.PosDataSourceConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import javax.sql.DataSource

/**
 * `PosDataSourceConfig` 단위 테스트.
 *
 * `posDataSource` 빈이 모든 환경에서 등록되고 PostgreSQL driver / URL / Hikari 파라미터가 정상
 * 바인딩되는지 검증. 실제 POS DB 연결은 수행하지 않는다.
 */
@DisplayName("PosDataSourceConfig 단위 테스트")
class PosDataSourceConfigTest {

	private val runner = ApplicationContextRunner()
		.withConfiguration(
			AutoConfigurations.of(PropertyPlaceholderAutoConfiguration::class.java),
		)
		.withUserConfiguration(PosDataSourceConfig::class.java)
		.withPropertyValues(
			"app.datasource.pos.host=pos.internal",
			"app.datasource.pos.port=5432",
			"app.datasource.pos.database=POS_DB",
			"app.datasource.pos.username=pos_ro",
			"app.datasource.pos.password=secret",
		)

	@Test
	@DisplayName("posDataSource 빈이 PostgreSQL driver / URL / Hikari 파라미터와 함께 등록된다")
	fun `registers posDataSource bean with postgres driver and hikari params`() {
		runner.run { context ->
			assertThat(context).hasBean("posDataSource")

			val hikari = context.getBean("posDataSource", DataSource::class.java) as HikariDataSource
			assertThat(hikari.driverClassName).isEqualTo("org.postgresql.Driver")
			assertThat(hikari.jdbcUrl).isEqualTo("jdbc:postgresql://pos.internal:5432/POS_DB")
			assertThat(hikari.username).isEqualTo("pos_ro")
			assertThat(hikari.poolName).isEqualTo("posHikariPool")
			assertThat(hikari.isReadOnly).isTrue()
			assertThat(hikari.isAutoCommit).isTrue()
			assertThat(hikari.initializationFailTimeout).isEqualTo(-1)
		}
	}

	@Test
	@DisplayName("test 프로파일에서도 posDataSource 빈이 등록된다 (모든 환경 활성)")
	fun `test profile also registers posDataSource bean`() {
		runner.withPropertyValues("spring.profiles.active=test").run { context ->
			assertThat(context).hasBean("posDataSource")
		}
	}
}
