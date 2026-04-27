package com.otoki.powersales.common.config

import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import javax.sql.DataSource

/**
 * 다중 인스턴스 환경에서 스케줄 잡이 중복 실행되는 것을 방지한다 (Spec #545).
 *
 * - 잠금 백엔드: PostgreSQL `powersales.shedlock` 테이블 (V6 마이그레이션으로 생성).
 * - `defaultLockAtMostFor` 는 모든 잡에 명시적 `lockAtMostFor` 가 지정되도록 강제하기 위한 안전망 역할만 한다.
 */
@Configuration
@EnableSchedulerLock(defaultLockAtMostFor = "PT15M")
class SchedulerLockConfig {

    @Bean
    fun lockProvider(dataSource: DataSource): JdbcTemplateLockProvider =
        JdbcTemplateLockProvider(
            JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(JdbcTemplate(dataSource))
                .withTableName("powersales.shedlock")
                .usingDbTime()
                .build()
        )
}
