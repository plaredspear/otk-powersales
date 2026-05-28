package com.otoki.powersales.common.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
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
 * - `entityManagerFactory` (`@Primary`) — 메인 RDS [DataSource] 위에 root `com.otoki.powersales`
 *   1개를 entity scan. ORORA entity 는 `com.otoki.orora.*` root 분리로 자동 제외됨.
 *   ([com.otoki.powersales.common.config.MainDataSourceConfig] 의 `@EntityScan` 과 동일 범위지만,
 *   `EntityManagerFactoryBuilder` 는 명시적 `.packages()` 호출이 없으면 entity 가 비어
 *   "No persistence unit found" 로 실패하므로 명시 호출 필수)
 * - `transactionManager` (`@Primary`) — 메인 EMF 의 `JpaTransactionManager`
 * - `@EnableJpaRepositories("com.otoki.powersales")` — root 1개만 명시. ORORA repository 는
 *   `com.otoki.orora.repository` root 분리로 자동 제외.
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
		.packages("com.otoki.powersales")
		.persistenceUnit("main")
		.build()

	@Bean
	@Primary
	fun transactionManager(
		@Qualifier("entityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf)
}
