package com.otoki.powersales.platform.batch

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableScheduling

/**
 * 전체 `@Scheduled` 활성화 진입점. 본 패키지의 모든 `*Batch` 클래스가 보유한 `@Scheduled` 메서드를 활성화한다.
 *
 * 처리 로직은 각 도메인의 `<도메인>BatchService` (또는 동등한 service) 에 위치하며, 본 패키지의 batch 진입 클래스는
 * ShedLock + ScheduledJobRunner + service 위임만 하는 thin layer 다.
 *
 * ## 잡별 게이팅 (전역 ON + 개별 OFF 기본)
 * `@EnableScheduling` 은 전역 스케줄링 엔진 스위치라 클래스 단위 선택이 불가능하다. 따라서 엔진은 켜되,
 * **모든 `*Batch` 클래스에 `@ConditionalOnProperty(... "enabled", matchIfMissing = false)` 를 부착**하여
 * 플래그가 명시적으로 `true` 인 잡만 빈이 생성·발화하도록 한다. 기본(플래그 미설정) 은 OFF 이므로,
 * 활성화하려는 잡만 환경별 `application.yml` (또는 환경변수) 에서 `app.batch.<job>.enabled=true` 로 켠다.
 */
@Configuration
@EnableScheduling
class BatchConfig
