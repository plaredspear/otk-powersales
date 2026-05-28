package com.otoki.powersales.common.integration.orora.config

import com.otoki.orora.repository.OroraDailySalesHistoryRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.Profile

/**
 * `OroraJpaConfig` / `OroraDailySalesHistoryRepository` 가 특정 프로파일에 묶이지 않고
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
}
