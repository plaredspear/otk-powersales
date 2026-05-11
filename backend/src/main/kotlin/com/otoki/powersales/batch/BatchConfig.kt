package com.otoki.powersales.batch

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 전체 `@Scheduled` 활성화 진입점. 본 패키지의 모든 `*Batch` 클래스가 보유한 `@Scheduled` 메서드를 활성화한다.
 *
 * 처리 로직은 각 도메인의 `<도메인>BatchService` (또는 동등한 service) 에 위치하며, 본 패키지의 batch 진입 클래스는
 * ShedLock + ScheduledJobRunner + service 위임만 하는 thin layer 다.
 */
@Configuration
@EnableScheduling
class BatchConfig
