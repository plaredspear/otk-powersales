package com.otoki.powersales.common.integration.orora.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import javax.sql.DataSource

/**
 * ORORA 외부 MSSQL DB 직결용 별도 DataSource Bean.
 *
 * - dev / prod 프로파일에서만 활성화된다 (VPC Peering 이 dev / prod 에만 구성됨).
 * - 기존 메인 RDS [DataSource] 와 분리된 빈 이름 `ororaDataSource` 로 등록한다.
 * - read-only 강제 + HikariCP `initializationFailTimeout=-1` 로 ORORA DB 도달 불가 시에도
 *   메인 애플리케이션 기동을 차단하지 않는다.
 * - 본 빈에는 `@Primary` 도 `@FlywayDataSource` 도 부착하지 않는다 — 메인 RDS DataSource
 *   ([com.otoki.powersales.common.config.MainDataSourceConfig]) 가 그 역할을 단독으로 맡으며,
 *   Flyway / JPA / Hibernate 는 메인만 자동 선택해야 한다. 본 빈은 항상
 *   `@Qualifier("ororaDataSource")` 를 통해서만 주입한다.
 */
@Configuration
@Profile("dev | prod")
@EnableConfigurationProperties(OroraDataSourceProperties::class)
class OroraDataSourceConfig(
	private val properties: OroraDataSourceProperties,
) {
	@Bean(name = ["ororaDataSource"])
	fun ororaDataSource(): DataSource {
		val hikariConfig = HikariConfig().apply {
			poolName = properties.hikari.poolName
			driverClassName = "com.microsoft.sqlserver.jdbc.SQLServerDriver"
			jdbcUrl = buildJdbcUrl()
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

	/**
	 * MSSQL JDBC URL 조립.
	 * - 포트는 명시하지 않는다 (MSSQL 1433 기본 사용).
	 * - `encrypt=false` 가 v1.0 기본값 — Orora 측 TLS 가용성 미확정.
	 * - `trustServerCertificate` 는 encrypt=true 일 때만 부착한다 (false 일 때는 의미 없는 파라미터 제거).
	 */
	private fun buildJdbcUrl(): String {
		val base = "jdbc:sqlserver://${properties.host};databaseName=${properties.database};encrypt=${properties.encrypt}"
		return if (properties.encrypt) {
			"$base;trustServerCertificate=${properties.trustServerCertificate}"
		} else {
			base
		}
	}
}
