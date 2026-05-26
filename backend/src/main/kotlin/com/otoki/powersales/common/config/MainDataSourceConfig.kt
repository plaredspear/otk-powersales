package com.otoki.powersales.common.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import javax.sql.DataSource

/**
 * 메인 RDS DataSource 명시 등록.
 *
 * ORORA 외부 DB DataSource ([com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig])
 * 가 dev/prod 프로파일에서 별도 DataSource Bean 을 등록하면, Spring Boot 의
 * `DataSourceAutoConfiguration` 은 컨텍스트에 이미 DataSource 가 존재한다고 판단하여
 * 메인 RDS DataSource 자동 생성을 중단한다. 그 결과 Flyway / JPA 가 ORORA DataSource 를
 * 잡고 부팅에 실패한다 (Spec #695 v1.0 부팅 사고).
 *
 * 본 Config 는 그 자동구성을 명시적 빈 등록으로 대체하여:
 * - 메인 DataSource 를 `@Primary` 로 노출 (JPA / Hibernate 가 자동 선택)
 * - 메인 DataSource 에 `@FlywayDataSource` 를 부착하여 Flyway 가 명시적으로 메인만 잡도록 함
 *   (`@Primary` 보다 우선순위가 높아 두 번째 방어선 역할)
 * - ORORA DataSource 는 `@Primary` 도 `@FlywayDataSource` 도 아니므로
 *   `@Qualifier("ororaDataSource")` 로만 주입됨
 *
 * 설정 키:
 * - `spring.datasource.{url, username, password, driver-class-name}` — 연결 정보
 * - `spring.datasource.hikari.*` — 풀 튜닝
 *
 * 둘 다 기존 `application.yml` 의 `spring.datasource.*` 그대로 사용.
 */
@Configuration
class MainDataSourceConfig {
	@Bean
	@Primary
	@ConfigurationProperties("spring.datasource")
	fun dataSourceProperties(): DataSourceProperties = DataSourceProperties()

	@Bean(name = ["dataSource"])
	@Primary
	@FlywayDataSource
	@ConfigurationProperties("spring.datasource.hikari")
	fun dataSource(properties: DataSourceProperties): DataSource =
		properties.initializeDataSourceBuilder()
			.type(HikariDataSource::class.java)
			.build()
}
