package com.otoki.powersales.sfmigration.stage1

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
)

data class EntityMetadata(
    val targetName: String,
    val sObjectName: String,
    val schemaName: String = "powersales",
    val tableName: String,
    val csvFileName: String,
    val fields: List<FieldMapping>,
    /**
     * Stage 1 INSERT 시 추가로 채워야 하는 정적 컬럼 (NOT NULL placeholder).
     * 예: user.password (NOT NULL) → "" placeholder.
     */
    val extraStaticColumns: Map<String, String?> = emptyMap(),
)
