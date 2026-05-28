package com.otoki.powersales.common.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * 메인 RDS 측 JPA infra (EMF + TM + Repository scan) 명시 등록.
 *
 * ## 명시 등록이 필요한 이유
 * [com.otoki.powersales.common.integration.orora.config.OroraJpaConfig] 가
 * `LocalContainerEntityManagerFactoryBean` 을 `ororaEntityManagerFactory` 이름으로
 * 등록하면, Spring Boot 의 `JpaBaseConfiguration` 이
 * `@ConditionalOnMissingBean(LocalContainerEntityManagerFactoryBean)` 으로 메인 EMF
 * 자동 생성을 양보한다. 마찬가지로 `JpaRepositoriesAutoConfiguration` 도 명시적
 * `@EnableJpaRepositories` 등장 시 무효화된다.
 *
 * 미적용 시 사고: 메인 100+ Repository (`SapInboundAuditRepository` 등) 가 컨텍스트에
 * 등록되지 않아 빈 주입 실패로 부팅 중단 (커밋 aecd8e3f 사고 회귀 방지).
 *
 * ## 빈 구성
 * - `entityManagerFactory` (`@Primary`) — 메인 RDS [DataSource] 위에 PostgreSQL dialect 로 구성
 * - `transactionManager` (`@Primary`) — 메인 EMF 의 `JpaTransactionManager`
 * - `@EnableJpaRepositories` — `com.otoki.powersales` root scan + `orora.repository.*` exclude
 *
 * ORORA 측 빈은 모두 비-`@Primary` 이므로 `@Qualifier("ororaEntityManagerFactory")` /
 * `@Qualifier("ororaTransactionManager")` 로만 주입된다.
 *
 * ## 별도 Config 분리 이유
 * [MainDataSourceConfig] 와 합치면 `@EnableJpaRepositories` 가 컨텍스트 부팅 시
 * JPA infra 의존을 강제하므로, DataSource 만 검증하는 단위 테스트
 * (`MainDataSourceConfigTest`, `MainDataSourceHealthConfigTest`) 에서 JPA infra
 * 부재로 컨텍스트 부팅이 깨진다. 책임 분리로 DataSource Config 는 JPA 의존 없이 검증 가능.
 */
@Configuration
@EnableJpaRepositories(
	basePackages = ["com.otoki.powersales"],
	excludeFilters = [
		ComponentScan.Filter(
			type = FilterType.REGEX,
			pattern = ["com\\.otoki\\.powersales\\.orora\\.repository\\..*"],
		),
	],
	entityManagerFactoryRef = "entityManagerFactory",
	transactionManagerRef = "transactionManager",
)
class MainJpaRepositoriesConfig {

	@Bean
	@Primary
	fun entityManagerFactory(
		builder: EntityManagerFactoryBuilder,
		@Qualifier("dataSource") dataSource: DataSource,
	): LocalContainerEntityManagerFactoryBean = builder
		.dataSource(dataSource)
		.packages(
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
		)
		.persistenceUnit("main")
		.build()

	@Bean
	@Primary
	fun transactionManager(
		@Qualifier("entityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf)
}
