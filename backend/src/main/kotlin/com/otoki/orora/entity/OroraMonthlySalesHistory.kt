package com.otoki.orora.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal

/**
 * ORORA MSSQL view `ECRM_ABCCUST_MH_V` 직매핑 entity (NS00030 — 거래처별 월별 매출 이력 수신).
 *
 * ## 권위 출처
 * - **DB**: ORORA MSSQL `ECRM_ABCCUST_MH_V` view (read-only)
 * - **DataSource**: `ororaDataSource` ([com.otoki.powersales.platform.common.integration.orora.config.OroraDataSourceConfig])
 * - **EntityManagerFactory**: `ororaEntityManagerFactory` ([com.otoki.powersales.platform.common.integration.orora.config.OroraJpaConfig])
 * - **활성 환경**: dev / prod 만 (VPC Peering 한정)
 *
 * ## 원본 SQL (NS00030)
 * ```
 * SELECT SAPAccountCode, SalesDate,
 *        ABCClosingAmount1, ABCClosingAmount2, ABCClosingAmount3, ABCClosingAmount4,
 *        ShipClosingAmount1, ShipClosingAmount2, ShipClosingAmount3, ShipClosingAmount4
 * FROM ECRM_ABCCUST_MH_V
 * WHERE SalesDate = '$CurrentSalesMonth$'
 * ```
 *
 * ## 클래스명이 `Orora` 접두사인 이유
 * [OroraDailySalesHistory] 와 동일 — Hibernate `entity name` (= simple class name) 충돌 회피.
 *
 * ## SF 레거시 동등 매핑
 * SF `MonthlySalesHistory__c` SObject 의 데이터 source 와 동일. SF Apex
 * (`IF_REST_ORORA_ReceiveMonthlySalesHistory.cls` / `Batch_OroraMonthlySalesHistory_M2.cls`)
 * 가 SAP PO RESTAdapter 경유로 받았던 원본 데이터를 본 신규 시스템은 직접 SELECT.
 *
 * SF 메타 cross-check 결과:
 * - `SAPAccountCode__c`: Text(100), 선행 `000` 제거는 application 단 (SF Apex 동등) 정책 → 본 entity 는 원본 보관
 * - `SalesDate__c`: SF 는 date 로 저장하면서 SalesYear/SalesMonth 분리 추출. 본 entity 는 **ORORA 원본 문자열 그대로 보관**
 *   (왜곡 정책 폐기, [OroraDailySalesHistory] 와 동일 deviation 박제)
 * - `ABCClosingAmount1~4`, `ShipClosingAmount1~4`: Number(precision=18, scale=0), 원 단위 정수
 *   - ABCClosingAmount1: 전산마감실적_상온 (원)
 *   - ABCClosingAmount2: 전산마감실적_라면 (원)
 *   - ABCClosingAmount3: 전산마감실적_냉장냉동 (원)
 *   - ABCClosingAmount4: 전산마감실적_유지 (원)
 *   - ShipClosingAmount1: 물류마감실적_상온 (원)
 *   - ShipClosingAmount2: 물류마감실적_라면 (원)
 *   - ShipClosingAmount3: 물류마감실적_냉장냉동 (원)
 *   - ShipClosingAmount4: 물류마감실적_유지 (원)
 *
 * ## ⚠️ 절대 수정 불가 ([OroraDailySalesHistory] 와 동일 가드)
 * - **L1 Flyway 격리**: 메인 RDS DataSource 만 Flyway 대상
 * - **L2 Hikari `read-only=true`**: `ororaDataSource` Hikari pool 이 모든 connection 을 read-only 로 시작
 * - **L3 ORORA DB user 권한**: 운영 영역
 * - **Entity immutable**: 모든 필드 `val` (Kotlin) — JPA dirty checking 으로 인한 UPDATE 발사 자체 차단
 * - **`@Immutable`**: Hibernate 가 entity 를 read-only 로 인식 → dirty checking / flush 스킵
 * - **View 자연 read-only**: ORORA `ECRM_ABCCUST_MH_V` 가 view 라 INSERT/UPDATE 대상 안 됨
 *
 * ## 복합 PK
 * DB 측 PK 부재 (view 일반 성질). application 단에서 `(sapAccountCode, salesDate)` 복합 unique 가정.
 * SF `Externalkey__c` (= `SAPAccountCode + SalesYear + SalesMonth`, unique=true) 는 본 entity 의
 * `(sapAccountCode, salesDate)` 1쌍에서 derive 되는 값이므로 PK 의미상 동등.
 */
@Entity
@Immutable
@Table(name = "ECRM_ABCCUST_MH_V")
@IdClass(OroraMonthlySalesHistoryId::class)
class OroraMonthlySalesHistory(
	@Id
	@Column(name = "SAPAccountCode")
	val sapAccountCode: String,

	@Id
	@Column(name = "SalesDate")
	val salesDate: String,

	@Column(name = "ABCClosingAmount1", precision = 18, scale = 0)
	val abcClosingAmount1: BigDecimal? = null,

	@Column(name = "ABCClosingAmount2", precision = 18, scale = 0)
	val abcClosingAmount2: BigDecimal? = null,

	@Column(name = "ABCClosingAmount3", precision = 18, scale = 0)
	val abcClosingAmount3: BigDecimal? = null,

	@Column(name = "ABCClosingAmount4", precision = 18, scale = 0)
	val abcClosingAmount4: BigDecimal? = null,

	@Column(name = "ShipClosingAmount1", precision = 18, scale = 0)
	val shipClosingAmount1: BigDecimal? = null,

	@Column(name = "ShipClosingAmount2", precision = 18, scale = 0)
	val shipClosingAmount2: BigDecimal? = null,

	@Column(name = "ShipClosingAmount3", precision = 18, scale = 0)
	val shipClosingAmount3: BigDecimal? = null,

	@Column(name = "ShipClosingAmount4", precision = 18, scale = 0)
	val shipClosingAmount4: BigDecimal? = null,
) {
	/**
	 * SF formula `ABCClosingSumAmount__c = ABCClosingAmount1__c + ABCClosingAmount2__c +
	 * ABCClosingAmount3__c + ABCClosingAmount4__c` 동등 산출.
	 *
	 * SF formula 의 `formulaTreatBlanksAs=BlankAsZero` 정합 — null 컬럼은 `ZERO` 치환.
	 */
	val abcClosingSumAmount: BigDecimal
		get() = (abcClosingAmount1 ?: BigDecimal.ZERO) +
			(abcClosingAmount2 ?: BigDecimal.ZERO) +
			(abcClosingAmount3 ?: BigDecimal.ZERO) +
			(abcClosingAmount4 ?: BigDecimal.ZERO)

	/**
	 * SF formula `ShipClosingSumAmount__c = ShipClosingAmount1__c + ShipClosingAmount2__c +
	 * ShipClosingAmount3__c + ShipClosingAmount4__c` 동등 산출.
	 *
	 * SF formula 의 `formulaTreatBlanksAs=BlankAsZero` 정합 — null 컬럼은 `ZERO` 치환.
	 */
	val shipClosingSumAmount: BigDecimal
		get() = (shipClosingAmount1 ?: BigDecimal.ZERO) +
			(shipClosingAmount2 ?: BigDecimal.ZERO) +
			(shipClosingAmount3 ?: BigDecimal.ZERO) +
			(shipClosingAmount4 ?: BigDecimal.ZERO)

	/**
	 * SF formula `ClosingAmountSum__c = ABCClosingSumAmount__c + ShipClosingSumAmount__c` 동등 산출
	 * (= `MonthlySalesHistory__c` 의 마감실적 합계).
	 *
	 * SF `UpdateLastMonthRevenueBatch.cls` 의 `LastMonthRevenue__c` 적재 source 와 동등 — 신규 시스템의
	 * `DisplayWorkSchedule.lastMonthRevenue` 갱신 / `TeamMemberScheduleSearchService` 의 6개월 평균
	 * ABC 마감실적 산출에 사용.
	 */
	val closingAmountSum: BigDecimal
		get() = abcClosingSumAmount + shipClosingSumAmount
}
