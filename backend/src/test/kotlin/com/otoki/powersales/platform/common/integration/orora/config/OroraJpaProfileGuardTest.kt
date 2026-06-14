package com.otoki.powersales.platform.common.integration.orora.config

import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import com.otoki.orora.repository.OroraMonthlySalesHistoryRepository
import com.otoki.powersales.platform.common.integration.orora.config.OroraJpaConfig
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.context.annotation.Profile

/**
 * `OroraJpaConfig` / ORORA Repository 들이 특정 프로파일에 묶이지 않고
 * 모든 환경에서 등록되는지 검증.
 *
 * 과거에는 `@Profile("orora-disabled")` 로 ORORA 측 4개 빈을 모든 환경에서 차단했으나,
 * 이제는 VPN 장애 시에도 메인 부팅이 보장되는 인프라 (initializationFailTimeout=-1) 위에
 * 모든 환경 등록을 유지한다. 본 테스트는 누군가 ORORA 빈에 다시 `@Profile` 가드를
 * 부착하지 못하도록 회귀 방지 역할.
 *
 * Repository 이름 / 메인 Repository 와의 충돌 회피 검증은 함께 유지.
 */
@DisplayName("ORORA JPA 프로파일 가드 — 모든 환경 등록 보장")
class OroraJpaProfileGuardTest {

	@Test
	@DisplayName("OroraJpaConfig 에 @Profile 어노테이션이 부착되지 않는다 (모든 환경 등록 보장)")
	fun `OroraJpaConfig has no Profile annotation`() {
		val profile = OroraJpaConfig::class.java.getAnnotation(Profile::class.java)
		assertThat(profile)
			.withFailMessage(
				"OroraJpaConfig 에 @Profile 이 부착됨: %s. " +
					"VPN 장애 시에도 메인 기능이 정상 부팅되도록 ORORA 빈은 모든 환경에서 등록되어야 한다. " +
					"부팅 시 connection acquire 실패는 Hikari `initializationFailTimeout=-1` 가 흡수.",
				profile?.value?.joinToString(),
			)
			.isNull()
	}

	@Test
	@DisplayName("OroraDailySalesHistoryRepository 에 @Profile 어노테이션이 부착되지 않는다 (모든 환경 등록 보장)")
	fun `OroraDailySalesHistoryRepository has no Profile annotation`() {
		val profile = OroraDailySalesHistoryRepository::class.java.getAnnotation(Profile::class.java)
		assertThat(profile)
			.withFailMessage(
				"OroraDailySalesHistoryRepository 에 @Profile 이 부착됨: %s. " +
					"ORORA repository 는 모든 환경에서 컨텍스트에 등록되어야 한다.",
				profile?.value?.joinToString(),
			)
			.isNull()
	}

	@Test
	@DisplayName("Repository 클래스명이 OroraDailySalesHistoryRepository 인지 검증 (메인 측 sales.DailySalesHistoryRepository 와 분리)")
	fun `repository class name distinguishes from main sales repository`() {
		// 메인 측 sales.repository.DailySalesHistoryRepository 와 본 ORORA Repository 의
		// simple class name 충돌이 없어야 Spring Bean name 충돌 (BeanDefinitionOverrideException)
		// 회피. 본 단언은 향후 클래스명 변경 시 회귀 방지.
		assertThat(OroraDailySalesHistoryRepository::class.simpleName)
			.isEqualTo("OroraDailySalesHistoryRepository")
	}

	@Test
	@DisplayName("OroraMonthlySalesHistoryRepository 에 @Profile 어노테이션이 부착되지 않는다 (모든 환경 등록 보장)")
	fun `OroraMonthlySalesHistoryRepository has no Profile annotation`() {
		val profile = OroraMonthlySalesHistoryRepository::class.java.getAnnotation(Profile::class.java)
		assertThat(profile)
			.withFailMessage(
				"OroraMonthlySalesHistoryRepository 에 @Profile 이 부착됨: %s. " +
					"ORORA repository 는 모든 환경에서 컨텍스트에 등록되어야 한다.",
				profile?.value?.joinToString(),
			)
			.isNull()
	}

	@Test
	@DisplayName("Repository 클래스명이 OroraMonthlySalesHistoryRepository 인지 검증 (Hibernate entity name / Spring Bean name 충돌 회피)")
	fun `monthly repository class name has Orora prefix`() {
		assertThat(OroraMonthlySalesHistoryRepository::class.simpleName)
			.isEqualTo("OroraMonthlySalesHistoryRepository")
	}

	@Test
	@DisplayName("ororaHibernateProperties 의 hibernate.default_schema 가 빈 문자열로 override 되어 있다 (메인 RDS powersales prefix 누수 회귀 방지)")
	fun `default_schema is overridden to empty string`() {
		// 메인 RDS application.yml 의 spring.jpa.properties.hibernate.default_schema=powersales 가
		// EntityManagerFactoryBuilder 의 jpaProperties 로 ORORA EMF 에도 전파되면, ORORA MSSQL
		// view 가 `powersales.ECRM_ABCCUST_MH_V` 형태로 발사되어 invalid object name 에러가 난다.
		// 본 단언은 OroraJpaConfig 가 항상 hibernate.default_schema 를 빈 문자열로 override 하는지 회귀 방지.
		val props = OroraJpaConfig.ororaHibernateProperties(mockk<ConfigurableListableBeanFactory>())

		assertThat(props)
			.withFailMessage(
				"ororaHibernateProperties 에 hibernate.default_schema 키가 없음. " +
					"메인 RDS 의 default_schema=powersales 가 ORORA EMF 에 전파되어 " +
					"ORORA MSSQL view 앞에 `powersales.` prefix 가 붙는 사고를 막으려면 " +
					"본 키를 빈 문자열로 명시 override 해야 함.",
			)
			.containsKey("hibernate.default_schema")

		assertThat(props["hibernate.default_schema"])
			.withFailMessage(
				"hibernate.default_schema 값이 빈 문자열이 아님: %s. " +
					"ORORA MSSQL view (ECRM_*) 앞에 어떤 schema prefix 도 붙으면 안 됨.",
				props["hibernate.default_schema"],
			)
			.isEqualTo("")
	}
}
