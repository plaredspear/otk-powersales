package com.otoki.powersales.common.integration.orora.health

import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import javax.sql.DataSource

/**
 * ORORA DB 도달 가능성을 별도 헬스 컴포넌트로 노출한다.
 *
 * - 빈 이름은 `orora` 로 등록 — `/actuator/health` 응답에서 `components.orora.status` 로 노출된다.
 * - dev / prod 프로파일에서만 활성화 (ORORA DataSource 자체가 dev/prod 한정).
 * - DOWN 이어도 전체 헬스를 DOWN 으로 만들지 않는다. Spring Boot Actuator 의 default
 *   StatusAggregator 동작상 본 컴포넌트가 DOWN 이면 전체도 DOWN 으로 집계되지만,
 *   외부 시스템 SLA 와 분리하기 위해 본 인디케이터는 예외 발생 시에도 `Status.UNKNOWN` 으로
 *   보고하여 전체 헬스에 영향을 주지 않는다 (관측은 details 의 `error` 필드로 가능).
 */
@Component("orora")
@Profile("dev | prod")
class OroraHealthIndicator(
	private val ororaDataSource: DataSource,
) : HealthIndicator {
	override fun health(): Health = try {
		ororaDataSource.connection.use { connection ->
			val valid = connection.isValid(VALIDATION_TIMEOUT_SECONDS)
			if (valid) {
				Health.up()
					.withDetail("database", "MSSQL")
					.build()
			} else {
				Health.unknown()
					.withDetail("database", "MSSQL")
					.withDetail("reason", "connection.isValid returned false")
					.build()
			}
		}
	} catch (ex: Exception) {
		Health.unknown()
			.withDetail("database", "MSSQL")
			.withDetail("error", ex.javaClass.simpleName)
			.withDetail("message", ex.message ?: "")
			.build()
	}

	companion object {
		private const val VALIDATION_TIMEOUT_SECONDS = 3
	}
}
