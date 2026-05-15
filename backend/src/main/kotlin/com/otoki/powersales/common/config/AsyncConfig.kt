package com.otoki.powersales.common.config

import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync

/**
 * 비동기 후처리 (`@Async` + `@TransactionalEventListener(AFTER_COMMIT)`) 활성화.
 *
 * 도입 배경: SF 레거시 `@future` 메서드 (예: `IF_REST_SAP_EmployeeMaster.upsertUser`) 동등 매핑.
 * 메인 트랜잭션 commit 이후 별도 스레드 + 별도 트랜잭션에서 부수 작업을 수행한다.
 *
 * Spring 기본 `SimpleAsyncTaskExecutor` 사용 — 별도 ThreadPoolTaskExecutor 가 필요하면
 * `AsyncConfigurer` 를 구현해 `getAsyncExecutor()` 를 오버라이드한다.
 */
@Configuration
@EnableAsync
class AsyncConfig
