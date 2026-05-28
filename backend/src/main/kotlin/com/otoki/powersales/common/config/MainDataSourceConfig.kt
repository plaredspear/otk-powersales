package com.otoki.powersales.common.config

import com.zaxxer.hikari.HikariDataSource
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.flyway.autoconfigure.FlywayDataSource
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties
import org.springframework.boot.persistence.autoconfigure.EntityScan
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
 * ## ORORA entity 격리
 * `@EntityScan(basePackages = [...])` 으로 root 1단계 하위 패키지를 명시 나열 (ORORA 제외).
 * `com.otoki.powersales.orora.entity` 는 [com.otoki.powersales.common.integration.orora.config.OroraJpaConfig]
 * 의 ororaEntityManagerFactory 가 전담.
 *
 * 미적용 시: 메인 EMF 가 [com.otoki.powersales.orora.entity.OroraDailySalesHistory] 를
 * metamodel 에 등록 → PostgreSQL 메인 RDS 에서 `ECRM_MULCUST_MH_V` view 부재로
 * `SchemaManagementException: missing table [ECRM_MULCUST_MH_V]` 부팅 실패.
 *
 * ## ORORA repository 격리
 * 메인 측 Repository scan 은 [MainJpaRepositoriesConfig] 가 명시적 `@EnableJpaRepositories`
 * 로 전담하며, ORORA repository (`com.otoki.powersales.orora.repository.*`) 는 exclude 한다.
 * ORORA 측 repository 는 [com.otoki.powersales.common.integration.orora.config.OroraJpaConfig]
 * 의 `ororaEntityManagerFactory` + `ororaTransactionManager` 가 전담.
 *
 * **`com.otoki.powersales` 하위에 신규 top-level 패키지 추가 시 본 @EntityScan 에 동시 등록
 * 의무** (ORORA 외). 누락 시 해당 패키지 entity 가 메인 EMF 에서 사라져 빈 주입 실패로 즉시 검출.
 *
 * 설정 키:
 * - `spring.datasource.{url, username, password, driver-class-name}` — 연결 정보
 * - `spring.datasource.hikari.*` — 풀 튜닝
 *
 * 둘 다 기존 `application.yml` 의 `spring.datasource.*` 그대로 사용.
 */
@Configuration
@EntityScan(
	basePackages = [
		"com.otoki.powersales.account",
		"com.otoki.powersales.admin",
		"com.otoki.powersales.agreement",
		"com.otoki.powersales.auth",
		"com.otoki.powersales.batch",
		"com.otoki.powersales.claim",
		"com.otoki.powersales.common",
		"com.otoki.powersales.draft",
		"com.otoki.powersales.education",
		"com.otoki.powersales.employee",
		"com.otoki.powersales.inspection",
		"com.otoki.powersales.leave",
		"com.otoki.powersales.notice",
		"com.otoki.powersales.order",
		"com.otoki.powersales.organization",
		"com.otoki.powersales.product",
		"com.otoki.powersales.productexpiration",
		"com.otoki.powersales.promotion",
		"com.otoki.powersales.safetycheck",
		"com.otoki.powersales.sales",
		"com.otoki.powersales.sap",
		"com.otoki.powersales.schedule",
		"com.otoki.powersales.sf",
		"com.otoki.powersales.sfmigration",
		"com.otoki.powersales.suggestion",
		"com.otoki.powersales.user",
	],
)
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
