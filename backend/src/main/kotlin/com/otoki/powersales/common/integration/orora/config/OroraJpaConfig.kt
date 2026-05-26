package com.otoki.powersales.common.integration.orora.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * ORORA MSSQL JPA 컨텍스트 (Spec #823).
 *
 * Spec #695 가 등록한 [com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig.ororaDataSource]
 * 위에 별도 `EntityManagerFactory` + `TransactionManager` 를 등록한다 — 메인 RDS 측
 * `entityManagerFactory` / `transactionManager` 와 격리.
 *
 * ## 활성 환경
 * `@Profile("dev | prod")` — VPC Peering 한정. local / test 프로파일에서는 본 Config 와
 * 하위의 모든 빈 (`ororaEntityManagerFactory`, `ororaTransactionManager`,
 * [com.otoki.powersales.orora.repository.DailySalesHistoryRepository]) 이 컨텍스트에 등록되지 않는다.
 *
 * ## ⚠️ 절대 수정 불가 (Spec §1.3 #7 read-only 가드)
 * - **Hibernate `hbm2ddl.auto = none`**: 자동 DDL 생성/실행 차단
 * - **Hibernate `connection.autocommit = true`**: Hikari read-only=true 정합
 * - **`jakarta.persistence.query.timeout = 30000`**: SELECT 지연 상한
 * - JPA Repository 의 mutation API 노출은 [com.otoki.powersales.orora.repository.DailySalesHistoryRepository]
 *   가 `Repository<>` marker 만 상속하여 컴파일 시점에 차단됨
 *
 * ## JPA scope 격리 (Q6 박제)
 * `@EnableJpaRepositories(basePackages = ["com.otoki.powersales.orora.entity", "com.otoki.powersales.orora.repository"])`
 * 로 ORORA 측 Repository scan 범위를 좁힘. 메인 측 자동 구성이 본 패키지를 스캔하지 않도록
 * 메인 측 명시적 `@EnableJpaRepositories` + `@EntityScan` 의 `excludeFilters` 가 별도 보강 필요
 * ([com.otoki.powersales.OtokiPowerSalesApplication] 또는 별도 Main JPA Config).
 */
@Configuration
@Profile("orora-disabled")
@EnableJpaRepositories(
	basePackages = ["com.otoki.powersales.orora.entity", "com.otoki.powersales.orora.repository"],
	entityManagerFactoryRef = "ororaEntityManagerFactory",
	transactionManagerRef = "ororaTransactionManager",
)
class OroraJpaConfig {
	@Bean
	fun ororaEntityManagerFactory(
		builder: EntityManagerFactoryBuilder,
		@Qualifier("ororaDataSource") ororaDataSource: DataSource,
	): LocalContainerEntityManagerFactoryBean = builder
		.dataSource(ororaDataSource)
		.packages("com.otoki.powersales.orora.entity")
		.persistenceUnit("orora")
		.properties(
			mapOf(
				"hibernate.dialect" to "org.hibernate.dialect.SQLServerDialect",
				// L3: 자동 DDL 차단 — ORORA 측에 어떤 schema 변경도 발사되지 않음
				"hibernate.hbm2ddl.auto" to "none",
				// Hikari read-only=true 정합 — JPA 트랜잭션이 autocommit 모드로 동작
				"hibernate.connection.autocommit" to "true",
				"hibernate.connection.provider_disables_autocommit" to "false",
				// SELECT 지연 상한 — ORORA 측 view 응답 지연이 backend API 응답성을 침해하지 않도록
				"jakarta.persistence.query.timeout" to "30000",
				"hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS" to "5000",
			),
		)
		.build()

	@Bean
	fun ororaTransactionManager(
		@Qualifier("ororaEntityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf).apply {
		// ORORA 트랜잭션 default timeout 30초 — Hikari connection-timeout 5초 + SELECT 지연 상한과 정합
		defaultTimeout = 30
	}
}
