package com.otoki.powersales.platform.common.integration.orora.config

import jakarta.persistence.EntityManagerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
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
 * ORORA MSSQL JPA 컨텍스트.
 *
 * [OroraDataSourceConfig.ororaDataSource]
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
 * - **`jakarta.persistence.query.timeout`**: SELECT 지연 상한 (기본 300초, `ORORA_QUERY_TIMEOUT_MS`
 *   환경변수로 override). ORORA view 가 거래처 chunk 수십개 폭 단일 SELECT 에 수십초 이상 걸릴 수
 *   있어, 짧은 상한은 chunk 적재를 `The query has timed out.` 으로 실패시킨다.
 * - JPA Repository 의 mutation API 노출은 [com.otoki.orora.repository.OroraDailySalesHistoryRepository]
 *   / [com.otoki.orora.repository.OroraMonthlySalesHistoryRepository] 가 `Repository<>` marker 만
 *   상속하여 컴파일 시점에 차단됨
 *
 * ## JPA scope 격리 — 패키지 root 분리
 * ORORA entity/repository 가 `com.otoki.orora.*` root 로 분리되어 있어, 메인 측
 * [com.otoki.powersales.platform.common.config.MainJpaRepositoriesConfig] 의
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
class OroraJpaConfig(
	// ORORA SELECT 지연 상한 (ms). ORORA view 의 무거운 chunk SELECT 가 수십초 이상 걸릴 수 있어
	// 운영에서 환경변수로 조정 가능하게 외부화. 기본 300000ms(300초).
	@Value("\${app.batch.orora.query-timeout-ms:300000}") private val queryTimeoutMs: Long,
) {
	@Bean
	fun ororaEntityManagerFactory(
		builder: EntityManagerFactoryBuilder,
		@Qualifier("ororaDataSource") ororaDataSource: DataSource,
		beanFactory: ConfigurableListableBeanFactory,
	): LocalContainerEntityManagerFactoryBean = builder
		.dataSource(ororaDataSource)
		.packages("com.otoki.orora.entity")
		.persistenceUnit("orora")
		.properties(ororaHibernateProperties(beanFactory, queryTimeoutMs))
		.build()

	@Bean
	fun ororaTransactionManager(
		@Qualifier("ororaEntityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf).apply {
		// ORORA 트랜잭션 default timeout 을 SELECT 지연 상한(query-timeout-ms)과 정합.
		// 트랜잭션 timeout(초) 이 query timeout 보다 짧으면 무거운 chunk 가 트랜잭션 레벨에서 먼저
		// 끊겨 query-timeout 상향 효과가 무력화되므로, query-timeout-ms 를 초로 환산해 동일 상한 적용
		// (ceil — 부분 초 절상). Hikari connection-timeout 5초는 connection acquire 단계 한정이라 무관.
		defaultTimeout = ((queryTimeoutMs + 999) / 1000).toInt()
	}

	companion object {
		/**
		 * ORORA EMF 의 Hibernate properties 빌더.
		 *
		 * 별도 함수로 분리하여 단위 테스트가 빈 부팅 없이 검증할 수 있도록 노출.
		 * 회귀 방지 대상 — 메인 RDS 의 `default_schema=powersales` 가 ORORA EMF 로 전파되어
		 * ORORA MSSQL view 앞에 `powersales.` prefix 가 붙는 사고.
		 */
		fun ororaHibernateProperties(
			beanFactory: ConfigurableListableBeanFactory,
			queryTimeoutMs: Long = 300_000,
		): Map<String, Any> = mapOf(
			"hibernate.dialect" to "org.hibernate.dialect.SQLServerDialect",
			// 메인 RDS 측 application.yml 의 spring.jpa.properties.hibernate.default_schema=powersales
			// 가 EntityManagerFactoryBuilder 의 jpaProperties 로 본 ORORA EMF 에도 전파되어
			// ORORA MSSQL view 앞에 `powersales.` 가 prefix 되는 사고 방지 — 빈 문자열로 명시 override.
			"hibernate.default_schema" to "",
			// L3: 자동 DDL 차단 — ORORA 측에 어떤 schema 변경도 발사되지 않음
			"hibernate.hbm2ddl.auto" to "none",
			// Hikari read-only=true 정합 — JPA 트랜잭션이 autocommit 모드로 동작
			"hibernate.connection.autocommit" to "true",
			"hibernate.connection.provider_disables_autocommit" to "false",
			// SELECT 지연 상한 — ORORA 측 view 응답 지연이 backend API 응답성을 침해하지 않도록
			"jakarta.persistence.query.timeout" to queryTimeoutMs.toString(),
			"hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS" to "5000",
			// VPN 장애 / local 환경 부팅 시 ORORA DB 도달 불가 상황에서도 메인 부팅이 차단되지
			// 않도록 Hibernate 가 JDBC 메타데이터를 부팅 시점에 조회하지 않게 강제.
			// 본 옵션 없이 dialect 만 지정하면 Hibernate 5.4+ 부터는 메타데이터를 조회하지 않지만,
			// 일부 환경에서 여전히 connection 을 잡는 경로가 있어 명시적으로 false 부착.
			"hibernate.boot.allow_jdbc_metadata_access" to "false",
			// @EntityListeners 가 @Component bean 으로 인스턴스화되도록 Spring bean container 등록.
			// 자동 EMF 경로 (Spring Boot JpaBaseConfiguration) 우회 시 누락되는 customizer 를 명시 재현.
			"hibernate.resource.beans.container" to SpringBeanContainer(beanFactory),
		)
	}
}
