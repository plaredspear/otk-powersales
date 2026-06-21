package com.otoki.orora.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.math.BigDecimal
import com.otoki.powersales.platform.common.entity.DomainName

/**
 * ORORA MSSQL view `ECRM_MULCUST_MH_V` 직매핑 entity (Spec #823).
 *
 * ## 권위 출처
 * - **DB**: ORORA MSSQL `ECRM_MULCUST_MH_V` view (read-only)
 * - **DataSource**: `ororaDataSource` ([com.otoki.powersales.platform.common.integration.orora.config.OroraDataSourceConfig], Spec #695)
 * - **EntityManagerFactory**: `ororaEntityManagerFactory` ([com.otoki.powersales.platform.common.integration.orora.config.OroraJpaConfig])
 * - **활성 환경**: dev / prod 만 (VPC Peering 한정)
 *
 * ## 클래스명이 `Orora` 접두사인 이유
 * 기존 메인 RDS entity `com.otoki.powersales.sales.entity.DailySalesHistory` (SAP inbound 흐름 의존,
 * 본 스펙 범위 외) 와 Hibernate `entity name` (= simple class name) 충돌 회피.
 * Spring Boot 자동 구성이 root package `com.otoki.powersales` 전체를 entity scan 하므로 두 클래스가
 * 같은 metamodel 에 등록되어 entity name 이 같으면 `DuplicateMappingException` 발생.
 * 두 entity 가 모두 사용되는 dev/prod 프로파일에서 안정 부팅 보장을 위해 `Orora` 접두사 채택.
 *
 * ## SF 레거시 동등 매핑
 * SF `DailySalesHistory__c` SObject 의 데이터 source 와 동일. SF Apex
 * (`Queueable_OroraDailySalesHistory_M1.cls`) 가 SAP PO RESTAdapter 경유로 받았던
 * 원본 데이터를 본 신규 시스템은 직접 SELECT.
 *
 * SF 메타 cross-check 결과:
 * - `SAPAccountCode__c`: Text(100), 선행 `000` 제거는 application 단 (SF Apex 동등) 정책 → 본 entity 는 원본 보관
 * - `SalesDate__c`: SF 는 date 로 저장하면서 "당월=today / 전월=월말일" 왜곡. 본 entity 는 **ORORA 원본 YYYYMMDD 8자 문자열 그대로 보관** (왜곡 정책 폐기, deviation 박제)
 * - `ERPSalesAmount__c`, `ERPDistributionAmount__c`: Number(precision=18, scale=0), 원 단위 정수
 *
 * ## ⚠️ 절대 수정 불가 (Spec §1.3 #7 가드 정합)
 * - **L1 Flyway 격리**: 메인 RDS DataSource 만 Flyway 대상 (`MainDataSourceConfig.@FlywayDataSource`)
 * - **L2 Hikari `read-only=true`**: `ororaDataSource` Hikari pool 이 모든 connection 을 read-only 로 시작
 * - **L3 ORORA DB user 권한**: 운영 영역
 * - **Entity immutable**: 모든 필드 `val` (Kotlin) — JPA dirty checking 으로 인한 UPDATE 발사 자체 차단
 * - **`@Immutable`**: Hibernate 가 entity 를 read-only 로 인식 → dirty checking / flush 스킵
 * - **View 자연 read-only**: ORORA `ECRM_MULCUST_MH_V` 가 view 라 INSERT/UPDATE 대상 안 됨
 *
 * ## 복합 PK
 * DB 측 PK 부재 (view 일반 성질). application 단에서 `(sapAccountCode, salesDate)` 복합 unique 가정.
 * SF `Externalkey__c` (= `SAPAccountCode + SalesDate원본문자열`, unique=true) 동등 의미.
 */
@DomainName("ORORA일별매출이력")
@Entity
@Immutable
@Table(name = "ECRM_MULCUST_MH_V")
@IdClass(OroraDailySalesHistoryId::class)
class OroraDailySalesHistory(
	@Id
	@Column(name = "SAPAccountCode")
	val sapAccountCode: String,

	@Id
	@Column(name = "SalesDate")
	val salesDate: String,

	@Column(name = "ERPSalesAmount", precision = 18, scale = 0)
	val erpSalesAmount: BigDecimal? = null,

	@Column(name = "ERPDistributionAmount", precision = 18, scale = 0)
	val erpDistributionAmount: BigDecimal? = null,
)
