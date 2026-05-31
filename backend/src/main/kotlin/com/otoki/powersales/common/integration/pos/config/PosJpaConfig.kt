package com.otoki.powersales.common.integration.pos.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.hibernate.SpringBeanContainer
import org.springframework.transaction.PlatformTransactionManager
import javax.sql.DataSource

/**
 * POS PostgreSQL JPA 컨텍스트 (전산매출 조회 전용).
 *
 * [com.otoki.powersales.common.integration.orora.config.OroraJpaConfig] 와 동일 격리 패턴:
 * - `posDataSource` 위에 별도 `EntityManagerFactory`/`TransactionManager` 등록 (메인 RDS 와 격리).
 * - entity/repository 가 `com.otoki.pos.*` root 로 분리 → 메인
 *   [com.otoki.powersales.common.config.MainJpaRepositoriesConfig] 의 scope 에 미포함.
 *
 * ## read-only 가드
 * - `hbm2ddl.auto = none` (자동 DDL 차단)
 * - `connection.autocommit = true` (Hikari read-only 정합)
 * - `default_schema = ""` — 메인 RDS 의 `powersales` 스키마 prefix 전파 차단
 * - `allow_jdbc_metadata_access = false` — POS DB 도달 불가 시에도 부팅 차단 방지
 */
@Configuration
@EnableJpaRepositories(
	basePackages = ["com.otoki.pos.repository"],
	entityManagerFactoryRef = "posEntityManagerFactory",
	transactionManagerRef = "posTransactionManager",
)
class PosJpaConfig {
	@Bean
	fun posEntityManagerFactory(
		builder: EntityManagerFactoryBuilder,
		@Qualifier("posDataSource") posDataSource: DataSource,
		beanFactory: ConfigurableListableBeanFactory,
	): LocalContainerEntityManagerFactoryBean = builder
		.dataSource(posDataSource)
		.packages("com.otoki.pos.entity")
		.persistenceUnit("pos")
		.properties(posHibernateProperties(beanFactory))
		.build()

	@Bean
	fun posTransactionManager(
		@Qualifier("posEntityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf).apply {
		defaultTimeout = 30
	}

	companion object {
		fun posHibernateProperties(beanFactory: ConfigurableListableBeanFactory): Map<String, Any> = mapOf(
			"hibernate.dialect" to "org.hibernate.dialect.PostgreSQLDialect",
			// 메인 RDS default_schema=powersales 전파 차단 (POS 는 public 스키마)
			"hibernate.default_schema" to "",
			"hibernate.hbm2ddl.auto" to "none",
			"hibernate.connection.autocommit" to "true",
			"hibernate.connection.provider_disables_autocommit" to "false",
			"jakarta.persistence.query.timeout" to "30000",
			"hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS" to "5000",
			"hibernate.boot.allow_jdbc_metadata_access" to "false",
			"hibernate.resource.beans.container" to SpringBeanContainer(beanFactory),
		)
	}
}
