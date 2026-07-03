package com.otoki.powersales._migration.sf.stage1

/**
 * SF migration Stage 1 — entity 별 컬럼 매핑 메타데이터.
 *
 * scripts/sf-data-migration/common.kts 의 `FieldMapping` / `EntityMetadata` 를 backend 로
 * 이관 (POC 단계 — ErpOrderProduct 1개만). 정식 구축 시 전체 entity 로 확장.
 *
 * 본 메타는 S3 stream → PG COPY 직결 path 에서 컬럼 순서 / NOT NULL pre-filter /
 * placeholder 변환의 권위 정의로 사용된다.
 */
data class FieldMapping(
    val sfFieldName: String,
    val dbColumnName: String,
    val nullable: Boolean = true,
    /**
     * DB 컬럼이 NOT NULL 인데 SF 원본이 NULL/blank 인 row 가 존재하는 경우의 placeholder.
     * null (기본) → 변환 없이 SF 원본 NULL 그대로 보냄.
     * 값 지정 → SF 원본이 NULL/blank 일 때 placeholder 로 대체.
     */
    val nullPlaceholder: String? = null,
    /**
     * SF `Date` 타입 (CSV export 시 ISO `yyyy-MM-dd`, 10자) 을 DB `varchar(8)` `yyyyMMdd` 로
     * 변환할지 여부.
     * true → 하이픈 제거 (예: "2021-03-01" → "20210301"). `daily_sales_history.sales_date` 는
     *        `varchar(8)` `yyyyMMdd` + externalKey (`sapAccountCode + salesDate`) 키를 구성하므로,
     *        마이그레이션도 동일 포맷이어야 SF `Externalkey__c` (SAPAccountCode + yyyyMMdd) 와 정합.
     *        SF `Date` 컬럼이 DB 에서 `LocalDate`/`date` 로 매핑된 경우는 false 유지 (yyyy-MM-dd 가 그대로 정상 적재).
     */
    val dateToYyyymmdd: Boolean = false,
) {
    /**
     * 마이그레이션 적재 직전 SF 원본값을 DB 컬럼 포맷에 맞게 정규화.
     * 현재는 [dateToYyyymmdd] 변환만 담당. 적재 service 가 본 메서드를 단일 통로로 사용.
     */
    fun normalize(raw: String?): String? {
        if (raw.isNullOrBlank()) return raw
        if (dateToYyyymmdd) {
            // "yyyy-MM-dd" 또는 "yyyy-MM-dd'T'HH:mm:ss..." → 앞 10자(날짜)만 취해 하이픈 제거.
            val datePart = if (raw.length >= 10) raw.substring(0, 10) else raw
            return datePart.replace("-", "")
        }
        return raw
    }
}

data class EntityMetadata(
    val targetName: String,
    /**
     * SF SObject API 명. SOQL 출처 entity 는 필수, XML 메타 출처 entity (spec #790) 는 null.
     *
     * null 인 경우 `verify-metadata.main.kts` 의 `@SFField` 정합 검사 + `extract-csv.sh` 의 SOQL 출력
     * 양쪽에서 자동 skip — XML 메타 (`sf force source retrieve`) 가 별도 경로로 처리.
     */
    val sObjectName: String?,
    val schemaName: String = "powersales",
    val tableName: String,
    val csvFileName: String,
    val fields: List<FieldMapping>,
    /**
     * Stage 1 INSERT 시 추가로 채워야 하는 정적 컬럼 (NOT NULL placeholder).
     * 예: user.password (NOT NULL) → "" placeholder.
     */
    val extraStaticColumns: Map<String, String?> = emptyMap(),
    /**
     * Stage 1 재실행 시 기존 row 를 모두 비우고 새로 적재할지 여부.
     *
     * true 가 필요한 entity 의 공통 패턴: **DB 자연 키 (UNIQUE 제약) 컬럼이 Stage1
     * 적재 시점에 NULL** 이라 INSERT ... ON CONFLICT DO NOTHING 의 충돌 매칭이
     * 일어나지 않아 재실행 시 row 가 누적된다 (PG 의 NULL distinct 정책).
     *
     * 예:
     * - sharing_rule_condition: UNIQUE (sharing_rule_id, condition_order) — Stage1 시점 sharing_rule_id NULL
     * - permission_set_field_permission: partial UNIQUE WHERE permission_set_id IS NOT NULL — Stage1 시점 permission_set_id NULL
     * - permission_set_flags: sfid UNIQUE — Stage1 시점 sfid NULL (Stage2 fk substep 후 채움)
     *
     * sfid 등 자연 키 UNIQUE 가 Stage1 시점에 NOT NULL 로 채워지는 entity 는
     * ON CONFLICT 가 멱등성 보장 → preClear = false 기본값 유지.
     *
     * 실행: 적재 service 가 본 entity 의 target table 을 TRUNCATE ... RESTART
     * IDENTITY CASCADE 로 비운 후 staging → INSERT 진행 (같은 트랜잭션 내).
     */
    val preClear: Boolean = false,
    /**
     * INSERT 시 ON CONFLICT 처리 정책. null (기본) → `ON CONFLICT DO NOTHING`
     * (충돌 컬럼 미지정 — 어떤 UNIQUE 제약 충돌이든 row drop).
     *
     * 지정 시 `ON CONFLICT (<conflictColumn>) DO UPDATE SET <c> = COALESCE(EXCLUDED.<c>, table.<c>) ...`
     * 로 기존 row 의 지정 컬럼을 staging 값으로 보강한다 (EXCLUDED 가 NULL 이면 기존값 보존).
     *
     * 필요 사례 — Profile: `LocalDataInitializer.seedProfiles()` 가 dev/local 환경에서
     * name-only (sfid=NULL) row 를 먼저 시드하면, 후속 Stage1 의 SF 원본 row (sfid 정상) 가
     * `DO NOTHING` 무타깃에서 name UNIQUE 충돌로 전량 묵살되어 sfid 가 영영 NULL 로 남는다
     * (→ Stage2 FK Resolve 의 user.profile_sfid ↔ profile.sfid JOIN 전건 실패).
     * `ON CONFLICT (name) DO UPDATE SET sfid = COALESCE(EXCLUDED.sfid, profile.sfid)` 로
     * 재실행 시 seed row 의 sfid 를 SF 원본값으로 자동 보강.
     */
    val conflictUpdate: ConflictUpdate? = null,
)

/**
 * ON CONFLICT (conflictColumn) DO UPDATE 정책.
 *
 * @param conflictColumn 충돌 판정 기준 UNIQUE 컬럼 (단일).
 * @param updateColumns DO UPDATE SET 대상 컬럼들. 각 컬럼은
 *   `<c> = COALESCE(EXCLUDED.<c>, <table>.<c>)` 로 생성되어 EXCLUDED 가 NULL 이면 기존값 보존.
 * @param conflictPredicate partial unique index 를 arbiter 로 지정할 때의 WHERE 술어.
 *   null (기본) → 일반 UNIQUE 제약 (`ON CONFLICT (col) DO UPDATE`).
 *   지정 시 → `ON CONFLICT (col) WHERE <predicate> DO UPDATE`. PostgreSQL 은 partial unique
 *   index (예: `CREATE UNIQUE INDEX ... (sfid) WHERE sfid IS NOT NULL`, V5/V57) 를 arbiter 로
 *   추론하려면 인덱스 述語를 ON CONFLICT 에 그대로 명시해야 한다. 누락 시 런타임에
 *   `there is no unique or exclusion constraint matching the ON CONFLICT specification` 실패.
 *   predicate 문자열은 인덱스 정의의 WHERE 절과 정확히 일치해야 한다 (예: "sfid IS NOT NULL").
 *
 * ## 다중 UNIQUE 함정 (arbiter 선택 기준)
 * ON CONFLICT (col) 은 **그 col 의 충돌만** 처리한다. 한 테이블에 UNIQUE 가 2개 이상이면,
 * arbiter 아닌 다른 UNIQUE 를 위반하는 staging 행에서 그 위반이 안 잡혀 예외로 터진다
 * (DO NOTHING 시절엔 어느 UNIQUE 든 조용히 skip 돼 감춰졌던 문제). 따라서 arbiter 는
 * **항상 채워지는(NOT NULL) 자연키** 를 우선 택해, 충돌이 곧 '동일 레코드' dedup 이 되게 한다.
 * 그래서 erp_order=sap_order_number / employee=employee_code / account=external_key /
 * product=product_code 로 sfid 대신 자연키를 arbiter 로 쓴다.
 *
 * ## 알려진 잔여 다중 UNIQUE (arbiter 외 partial UNIQUE — 데이터 1:1 정합 전제로 무충돌)
 * 아래 entity 는 arbiter 외에도 partial UNIQUE (WHERE ... IS NOT NULL) 를 하나 더 갖는다.
 * arbiter 키와 이 컬럼이 정상 1:1 이면 충돌하지 않지만, 데이터에 불일치가 있으면 같은 예외 가능.
 * 재적재 중 해당 제약명으로 duplicate key 가 나면 그 entity 도 자연키 arbiter 로 전환한다.
 *   - team_member_schedule : promotion_emp_id_ext (대부분 NULL → 저위험)
 *   - claim                : name (SF Name, 대개 유니크 → 저위험)
 *   - employee / account / product / erp_order : sfid (자연키 arbiter 로 전환 후 sfid 가 잔여)
 */
data class ConflictUpdate(
    val conflictColumn: String,
    val updateColumns: List<String>,
    val conflictPredicate: String? = null,
)
