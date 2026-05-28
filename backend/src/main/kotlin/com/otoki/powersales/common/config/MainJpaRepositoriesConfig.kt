package com.otoki.powersales.common.config

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManagerFactory
import org.hibernate.SessionFactory
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.resource.beans.spi.ManagedBeanRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.boot.jpa.EntityManagerFactoryBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.hibernate.SpringBeanContainer
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
 *
 * ## SpringBeanContainer 명시 주입 (`hibernate.resource.beans.container`)
 * Spring Boot `JpaBaseConfiguration` 이 자동 EMF 를 만들 때는 `SpringBeanContainer` 를
 * Hibernate 에 자동 등록해, `@EntityListeners(SomeListener::class)` 의 listener 가
 * `@Component` 일 때 Spring bean 으로 인스턴스화되어 `@PersistenceContext` / `@Autowired`
 * 주입이 동작한다. 본 Config 가 `EntityManagerFactoryBuilder.build()` 를 직접 호출하면서
 * 자동 EMF 경로를 우회하면 `SpringBeanContainer` 등록이 누락되어 Hibernate 가 listener 를
 * reflection 으로 `new` 인스턴스화 → `lateinit` 필드가 비어 `UninitializedPropertyAccessException`.
 * 본 클래스는 `OwnerUserDefaultListener` (32개 entity 부착) 가 사용하는 `entityManager` 가
 * 정상 주입되도록 명시적으로 컨테이너를 properties 에 부착한다.
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
		beanFactory: ConfigurableListableBeanFactory,
	): LocalContainerEntityManagerFactoryBean = builder
		.dataSource(dataSource)
		.packages("com.otoki.powersales")
		.persistenceUnit("main")
		.properties(
			mapOf(
				"hibernate.resource.beans.container" to SpringBeanContainer(beanFactory),
			),
		)
		.build()

	@Bean
	@Primary
	fun transactionManager(
		@Qualifier("entityManagerFactory") emf: EntityManagerFactory,
	): PlatformTransactionManager = JpaTransactionManager(emf)

	/**
	 * TEMP — 진단 빈. 부팅 완료 후 Hibernate `SessionFactory` 의 ServiceRegistry 에서 BeanContainer 가
	 * 실제로 `SpringBeanContainer` 로 등록되었는지 확인한다. lateinit 미초기화 사고 검증용. 제거 예정.
	 */
	@Bean
	fun beanContainerProbe(@Qualifier("entityManagerFactory") emf: EntityManagerFactory): BeanContainerProbe =
		BeanContainerProbe(emf)

	class BeanContainerProbe(private val emf: EntityManagerFactory) {
		private val log = LoggerFactory.getLogger(javaClass)

		@PostConstruct
		fun probe() {
			val sf = emf.unwrap(SessionFactory::class.java) as SessionFactoryImplementor
			val mbr: ManagedBeanRegistry? = try {
				sf.serviceRegistry.getService(ManagedBeanRegistry::class.java)
			} catch (e: Exception) {
				log.warn("[DIAG] failed to get ManagedBeanRegistry: {}", e.message)
				null
			}
			val isSpringContainerRegistry = mbr != null &&
				mbr.javaClass.simpleName.contains("ContainerManagedLifecycleStrategy", ignoreCase = true).let {
					// 클래스명 휴리스틱 + reflection 으로 내부 BeanContainer 필드 확인
					mbr.javaClass.declaredFields.any { f ->
						f.type == org.hibernate.resource.beans.container.spi.BeanContainer::class.java
					}
				}
			log.warn(
				"[DIAG] main EMF ManagedBeanRegistry = {} (hasBeanContainer={})",
				mbr?.javaClass?.name ?: "<null>",
				isSpringContainerRegistry,
			)
			// 더 간단한 검증 — EMF 의 properties map 에 직접 확인
			val props = emf.properties
			val beanContainerProp = props["hibernate.resource.beans.container"]
			log.warn(
				"[DIAG] hibernate.resource.beans.container property = {} (isSpringBeanContainer={})",
				beanContainerProp?.javaClass?.name ?: "<null>",
				beanContainerProp is SpringBeanContainer,
			)
		}
	}
}
