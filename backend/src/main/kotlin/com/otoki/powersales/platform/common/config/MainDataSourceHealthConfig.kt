package com.otoki.powersales.platform.common.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.health.contributor.HealthContributor
import org.springframework.boot.jdbc.health.DataSourceHealthIndicator
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import javax.sql.DataSource

/**
 * `/actuator/health` 의 `components.db` 헬스 컴포넌트를 메인 RDS DataSource 로만 한정한다.
 *
 * Spring Boot 의 [org.springframework.boot.jdbc.autoconfigure.health.DataSourceHealthContributorAutoConfiguration]
 * 은 컨텍스트의 **모든 `DataSource` 빈**을 모아 자동으로 `db` 헬스 컴포넌트 (또는 composite)
 * 를 등록한다. ORORA DataSource 가 dev/prod 에서 등록되면 그 자동 헬스 컴포넌트가 ORORA
 * 까지 포함하게 되어, `/actuator/health` 호출 시마다 ORORA MSSQL TCP 연결을 시도하고
 * 5초 timeout 으로 실패 → 전체 헬스를 DOWN 으로 만든다.
 *
 * 본 빈은 자동등록과 동일한 이름 `dbHealthContributor` 로 등록되며,
 * 자동등록 측의 `@ConditionalOnMissingBean(name = "dbHealthContributor")` 가
 * 작동하여 자동등록을 양보한다. 그 결과:
 * - `components.db` → 메인 RDS DataSource 만 (PostgreSQL `SELECT 1`)
 * - `components.orora` → ORORA 전용 [com.otoki.powersales.platform.common.integration.orora.health.OroraHealthIndicator]
 *   (예외 시 UNKNOWN 보고 — 전체 헬스에 영향 없음)
 */
@Configuration
class MainDataSourceHealthConfig {
	@Bean(name = ["dbHealthContributor"])
	fun dbHealthContributor(@Qualifier("dataSource") dataSource: DataSource): HealthContributor =
		DataSourceHealthIndicator(dataSource, null)
}
