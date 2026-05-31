package com.otoki.powersales.common.integration.pos.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * POS 외부 PostgreSQL DB 직결용 별도 DataSource Bean (전산매출 `live_tot_sales_dh` 소스).
 *
 * [com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig] 와 동일 정책:
 * - 모든 환경(local/test/dev/prod)에서 빈 등록. `initializationFailTimeout=-1` 로 POS DB 도달
 *   불가 시에도 메인 부팅 차단 없음 (local/test 는 호출 site 부재로 acquire 자체 미발생).
 * - 메인 RDS DataSource 와 분리된 빈 이름 `posDataSource`. `@Primary`/`@FlywayDataSource` 미부착 —
 *   Flyway/JPA 는 메인만 자동 선택. 본 빈은 `@Qualifier("posDataSource")` 로만 주입.
 * - read-only 강제.
 */
@Configuration
@EnableConfigurationProperties(PosDataSourceProperties::class)
class PosDataSourceConfig(
	private val properties: PosDataSourceProperties,
) {
	@Bean(name = ["posDataSource"])
	fun posDataSource(): DataSource {
		val hikariConfig = HikariConfig().apply {
			poolName = properties.hikari.poolName
			driverClassName = "org.postgresql.Driver"
			jdbcUrl = "jdbc:postgresql://${properties.host}:${properties.port}/${properties.database}"
			username = properties.username
			password = properties.password
			maximumPoolSize = properties.hikari.maximumPoolSize
			minimumIdle = properties.hikari.minimumIdle
			idleTimeout = properties.hikari.idleTimeout
			maxLifetime = properties.hikari.maxLifetime
			keepaliveTime = properties.hikari.keepaliveTime
			connectionTestQuery = properties.hikari.connectionTestQuery
			connectionTimeout = properties.hikari.connectionTimeout
			validationTimeout = properties.hikari.validationTimeout
			isReadOnly = properties.hikari.readOnly
			isAutoCommit = properties.hikari.autoCommit
			initializationFailTimeout = properties.hikari.initializationFailTimeout
		}
		return HikariDataSource(hikariConfig)
	}
}
