package com.otoki.powersales.orora.repository

import com.otoki.powersales.orora.entity.OroraDailySalesHistory
import com.otoki.powersales.orora.entity.OroraDailySalesHistoryId
import org.springframework.context.annotation.Profile
import org.springframework.data.repository.Repository

/**
 * ORORA `ECRM_MULCUST_MH_V` view 의 [OroraDailySalesHistory] 조회 전용 Repository (Spec #823 Q8 옵션 1).
 *
 * ## 클래스명이 `Orora` 접두사인 이유
 * 기존 메인 RDS Repository `com.otoki.powersales.sales.repository.DailySalesHistoryRepository`
 * (SAP inbound 흐름 의존, 본 스펙 범위 외) 와 Spring Bean name 충돌 회피. Spring 의 default bean
 * naming 은 simple class name → camelCase 라 두 Repository 가 같은 simple name 이면
 * `BeanDefinitionOverrideException` 발생. 본 Repository 의 entity 가 [OroraDailySalesHistory]
 * 인 점과 정합하여 `Orora` 접두사 채택.
 *
 * ## 절대 수정 불가 가드 (컴파일 시점)
 * 본 인터페이스는 Spring Data 의 marker [Repository] 만 상속한다 — `JpaRepository` / `CrudRepository`
 * 가 노출하는 `save` / `saveAll` / `delete` / `deleteAll` / `deleteById` 등 mutation API 가
 * **인터페이스에 부재**. 향후 누군가 본 Repository 로 INSERT/UPDATE/DELETE 를 시도하면
 * **빌드 단계에서 컴파일 에러로 검출**된다.
 *
 * ## 조회 메소드
 * 본 스펙 시점에는 marker + 시그니처만. 실제 조회 메소드 (e.g., `findBySapAccountCodeAndSalesDate`,
 * `findBySalesDateBetweenAndSapAccountCodeBetween`) 는 후속 조회 API 스펙에서 명시적으로 추가한다.
 *
 * ## 활성 환경
 * dev / prod 만 ([com.otoki.powersales.common.integration.orora.config.OroraJpaConfig]
 * `@Profile("dev | prod")` 가드 + `@EnableJpaRepositories` scope).
 * local / test 프로파일에서는 본 빈이 컨텍스트에 등록되지 않는다.
 */
@Profile("dev | prod")
interface OroraDailySalesHistoryRepository : Repository<OroraDailySalesHistory, OroraDailySalesHistoryId>
