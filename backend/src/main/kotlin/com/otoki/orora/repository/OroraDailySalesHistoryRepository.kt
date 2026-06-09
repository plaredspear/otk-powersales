package com.otoki.orora.repository

import com.otoki.orora.entity.OroraDailySalesHistory
import com.otoki.orora.entity.OroraDailySalesHistoryId
import org.springframework.data.repository.Repository

/**
 * ORORA `ECRM_MULCUST_MH_V` view 의 [OroraDailySalesHistory] 조회 전용 Repository.
 *
 * ## 클래스명이 `Orora` 접두사인 이유
 * 기존 메인 RDS Repository `com.otoki.powersales.sales.repository.DailySalesHistoryRepository`
 * 와 Spring Bean name 충돌 회피. Spring 의 default bean naming 은 simple class name → camelCase
 * 라 두 Repository 가 같은 simple name 이면 `BeanDefinitionOverrideException` 발생.
 * 본 Repository 의 entity 가 [OroraDailySalesHistory] 인 점과 정합하여 `Orora` 접두사 채택.
 *
 * ## 절대 수정 불가 가드 (컴파일 시점)
 * 본 인터페이스는 Spring Data 의 marker [Repository] 만 상속한다 — `JpaRepository` / `CrudRepository`
 * 가 노출하는 `save` / `saveAll` / `delete` / `deleteAll` / `deleteById` 등 mutation API 가
 * **인터페이스에 부재**. 향후 누군가 본 Repository 로 INSERT/UPDATE/DELETE 를 시도하면
 * **빌드 단계에서 컴파일 에러로 검출**된다.
 *
 * ## 조회 메소드
 * - [findBySalesDateStartingWithAndSapAccountCodeBetween]: 매출월(`YYYYMM`) prefix + 거래처 코드 범위 조회 —
 *   ORORA 일별 매출 적재 배치(Spec #855)가 거래처 코드 범위(레거시 `OroraDailyAccountRange__mdt`
 *   from/to 동등)를 청크로 분할해 한 달치 일별 row 를 받아오는 용도. ORORA view 의 `SalesDate` 는
 *   `YYYYMMDD` 8자 원본 문자열이라 `YYYYMM` prefix 매칭으로 한 달치를 조회한다.
 *
 * ## 활성 환경
 * 모든 환경. [com.otoki.powersales.common.integration.orora.config.OroraJpaConfig] 의
 * `@EnableJpaRepositories` scope 에 의해 ORORA EMF/TM 에 bind 됨. local/test 에서는 호출 site 가
 * 없어 connection acquire 자체가 발생하지 않으며, dev/prod VPN 장애 시에는 호출 site 만
 * `CannotCreateTransactionException` 으로 실패하고 메인 기능은 무영향.
 */
interface OroraDailySalesHistoryRepository : Repository<OroraDailySalesHistory, OroraDailySalesHistoryId> {

	/**
	 * 매출월(`YYYYMM`) prefix + 거래처 코드 범위(`from` ~ `to`, 양끝 포함) 조회.
	 *
	 * 거래처 코드는 ORORA view 원본 형식(선행 `000` 포함, 예: `0001000000`) 의 문자열 범위 비교.
	 * 레거시 `Queueable_OroraDailySalesHistory_M1` 의 `From_cust`/`To_cust` 범위 동등.
	 *
	 * @param salesDatePrefix ORORA `SalesDate`(`YYYYMMDD`) 의 앞 6자 매출월 (`YYYYMM`)
	 * @param fromSapAccountCode 거래처 코드 범위 시작 (포함)
	 * @param toSapAccountCode 거래처 코드 범위 끝 (포함)
	 */
	fun findBySalesDateStartingWithAndSapAccountCodeBetween(
		salesDatePrefix: String,
		fromSapAccountCode: String,
		toSapAccountCode: String,
	): List<OroraDailySalesHistory>
}
