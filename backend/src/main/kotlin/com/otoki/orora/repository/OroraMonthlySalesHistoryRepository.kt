package com.otoki.orora.repository

import com.otoki.orora.entity.OroraMonthlySalesHistory
import com.otoki.orora.entity.OroraMonthlySalesHistoryId
import org.springframework.data.repository.Repository

/**
 * ORORA `ECRM_ABCCUST_MH_V` view 의 [OroraMonthlySalesHistory] 조회 전용 Repository.
 *
 * ## 클래스명이 `Orora` 접두사인 이유
 * [OroraDailySalesHistoryRepository] 와 동일 — Spring Bean name 충돌 회피.
 *
 * ## 절대 수정 불가 가드 (컴파일 시점)
 * 본 인터페이스는 Spring Data 의 marker [Repository] 만 상속한다 — `JpaRepository` / `CrudRepository`
 * 가 노출하는 `save` / `saveAll` / `delete` / `deleteAll` / `deleteById` 등 mutation API 가
 * **인터페이스에 부재**. 향후 누군가 본 Repository 로 INSERT/UPDATE/DELETE 를 시도하면
 * **빌드 단계에서 컴파일 에러로 검출**된다.
 *
 * ## 조회 메소드
 * Spring Data 의 query method derivation 은 marker [Repository] 직속 인터페이스에서도 정상 동작
 * (Spring Data Commons 의 `QueryLookupStrategy` 가 처리). `JpaRepository` / `CrudRepository` 가
 * 추가로 노출하는 것은 mutation API 뿐이며 query derivation 자체는 marker 패턴에서 지원.
 *
 * - [findBySalesDateAndSapAccountCodeIn]: 단월 + 다중 거래처 — `getDetail`, `getSummary` 의 단월 조회용
 * - [findBySalesDateInAndSapAccountCodeIn]: 다월 일괄 + 다중 거래처 — `buildMonthlyTrend` 의 12개월
 *   (6개월 + 전년 동기간 6개월) 1 trip 조회용
 *
 * 두 메소드의 `salesDate` 매개변수 형식은 `YYYYMM` 6자 문자열 (예: `"202605"`) — ORORA view 의
 * SalesDate 컬럼 원본 형식과 동일. SF Apex `Batch_OroraMonthlySalesHistory_M2.cls` 운영 실측 정합.
 *
 * ## 활성 환경
 * 모든 환경. [com.otoki.powersales.platform.common.integration.orora.config.OroraJpaConfig] 의
 * `@EnableJpaRepositories` scope 에 의해 ORORA EMF/TM 에 bind 됨. local/test 에서는 호출 site 가
 * 없어 connection acquire 자체가 발생하지 않으며, dev/prod VPN 장애 시에는 호출 site 만
 * `CannotCreateTransactionException` 으로 실패하고 메인 기능은 무영향. 호출 site 의 graceful
 * fallback 책임은 [com.otoki.powersales.domain.sales.service.OroraMonthlySalesHistoryQueryGateway] 가 부담.
 */
interface OroraMonthlySalesHistoryRepository : Repository<OroraMonthlySalesHistory, OroraMonthlySalesHistoryId> {

	/**
	 * 단월 + 다중 거래처 조회.
	 *
	 * @param salesDate ORORA `SalesDate` 원본 6자 문자열 (`YYYYMM`)
	 * @param sapAccountCodes 거래처 코드 collection — 빈 collection 입력 시 빈 결과 반환
	 */
	fun findBySalesDateAndSapAccountCodeIn(
		salesDate: String,
		sapAccountCodes: Collection<String>,
	): List<OroraMonthlySalesHistory>

	/**
	 * 다월 일괄 + 다중 거래처 조회.
	 *
	 * @param salesDates ORORA `SalesDate` 원본 6자 문자열 collection (`YYYYMM`)
	 * @param sapAccountCodes 거래처 코드 collection — 빈 collection 입력 시 빈 결과 반환
	 */
	fun findBySalesDateInAndSapAccountCodeIn(
		salesDates: Collection<String>,
		sapAccountCodes: Collection<String>,
	): List<OroraMonthlySalesHistory>

	/**
	 * 단월 + 거래처 코드 범위(`from` ~ `to`, 양끝 포함) 조회.
	 *
	 * 거래처 코드는 ORORA view 원본 형식(선행 `000` 포함) 의 문자열 범위 비교. ORORA 월별 매출 적재
	 * 배치(Spec #855)가 거래처 코드 범위를 청크로 분할해 한 달치 월별 row 를 받아오는 용도.
	 * 레거시 `IF_REST_ORORA_ReceiveMonthlySalesHistory` 의 `From_cust`/`To_cust` 범위 동등.
	 *
	 * @param salesDate ORORA `SalesDate` 원본 6자 문자열 (`YYYYMM`)
	 * @param fromSapAccountCode 거래처 코드 범위 시작 (포함)
	 * @param toSapAccountCode 거래처 코드 범위 끝 (포함)
	 */
	fun findBySalesDateAndSapAccountCodeBetween(
		salesDate: String,
		fromSapAccountCode: String,
		toSapAccountCode: String,
	): List<OroraMonthlySalesHistory>
}
