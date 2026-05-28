package com.otoki.powersales.common.integration.orora.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * ORORA MSSQL JPA 컨텍스트.
 *
 * [com.otoki.powersales.common.integration.orora.config.OroraDataSourceConfig.ororaDataSource]
 * 위에 별도 `EntityManagerFactory` + `TransactionManager` 를 등록한다 — 메인 RDS 측
 * `entityManagerFactory` / `transactionManager` 와 격리.
 *
 * ## 활성 환경
 * 모든 환경 (local / test / dev / prod) 에서 빈 등록. VPN 장애로 dev/prod 에서 ORORA 도달
 * 불가 시에도 본 EMF 는 lazy 하게 connection 을 잡으므로 메인 부팅 차단 없음.
 * local/test 에서는 ORORA 호출 site 가 없어 connection acquire 자체가 발생하지 않는다.
 *
 * ## ⚠️ 절대 수정 불가 (read-only 가드)
 * - **Hibernate `hbm2ddl.auto = none`**: 자동 DDL 생성/실행 차단
 * - **Hibernate `connection.autocommit = true`**: Hikari read-only=true 정합
 * - **`jakarta.persistence.query.timeout = 30000`**: SELECT 지연 상한
 * - JPA Repository 의 mutation API 노출은 [com.otoki.orora.repository.OroraDailySalesHistoryRepository]
 *   / [com.otoki.orora.repository.OroraMonthlySalesHistoryRepository] 가 `Repository<>` marker 만
 *   상속하여 컴파일 시점에 차단됨
 *
 * ## JPA scope 격리 — 패키지 root 분리
 * ORORA entity/repository 가 `com.otoki.orora.*` root 로 분리되어 있어, 메인 측
 * [com.otoki.powersales.common.config.MainJpaRepositoriesConfig] 의
 * `@EnableJpaRepositories("com.otoki.powersales")` 가 자동으로 ORORA 를 포함하지 않는다.
 * 본 Config 의 `@EnableJpaRepositories("com.otoki.orora.repository")` 는 ORORA repository 만
 * ORORA EMF/TM 에 bind.
 */
@Configuration
@EnableJpaRepositories(
	basePackages = ["com.otoki.orora.repository"],
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
		.packages("com.otoki.orora.entity")
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
				// VPN 장애 / local 환경 부팅 시 ORORA DB 도달 불가 상황에서도 메인 부팅이 차단되지
				// 않도록 Hibernate 가 JDBC 메타데이터를 부팅 시점에 조회하지 않게 강제.
				// 본 옵션 없이 dialect 만 지정하면 Hibernate 5.4+ 부터는 메타데이터를 조회하지 않지만,
				// 일부 환경에서 여전히 connection 을 잡는 경로가 있어 명시적으로 false 부착.
				"hibernate.boot.allow_jdbc_metadata_access" to "false",
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
