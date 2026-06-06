#!/usr/bin/env kotlin

/**
 * `@HCColumn` ↔ HerokuStage1Targets 리플렉션 메타 정합 검증 안내.
 *
 * Heroku Stage 1 적재 메타는 backend `HerokuStage1Targets` 가 `@HerokuOnly` + `@HCColumn`
 * 리플렉션으로 자동 생성한다(어노테이션 단일 출처 SoT). 따라서 SF 처럼 손수 적은 메타 카탈로그와
 * 어노테이션의 drift 가 구조적으로 발생하지 않는다.
 *
 * 실제 정합 검증(리플렉션 추출 결과 ↔ 실제 엔티티 컬럼)은 backend 단위 테스트
 * `HerokuStage1TargetsTest` 가 CI 에서 자동 수행한다. 본 스크립트는 그 사실을 안내하고,
 * 운영자가 export 한 CSV 헤더가 `@HCColumn` value 와 일치하는지 확인하는 절차를 출력한다.
 *
 * 실행: kotlin scripts/heroku-data-migration/verify-metadata.main.kts
 */

println(
    """
    ============================================================
    Heroku 마이그레이션 — 적재 메타 정합 검증
    ============================================================

    [1] 리플렉션 메타 ↔ 엔티티 어노테이션 정합
        backend 단위 테스트가 자동 검증합니다:

          cd backend && ./gradlew test --tests \
            "com.otoki.powersales.herokumigration.stage1.HerokuStage1TargetsTest"

        검증 내용:
          - @HerokuOnly 19개 엔티티 등록 (ProductSyncBuffer 제외)
          - @Table → tableName / @HerokuOnly → csvFileName / @HCColumn → 컬럼 매핑
          - FK *_id / serial PK 매핑 제외 (Stage1 NULL 적재)
          - EmployeeInfo 공유 PK resolve 표시
          - 의존성 순서 (부모 → 자식)

    [2] 적재 가능 target 일람 (런타임 확인)
        backend 가 떠 있으면 다음 endpoint 로 (targetName, csvFileName) 일람 확인:

          GET /api/v1/admin/heroku-migration/stage1/targets

    [3] export CSV 헤더 정합 (운영자 수동)
        TablePlus 에서 SELECT * FROM salesforce2.<table> 로 export 하면 헤더가 Heroku
        원본 컬럼명(= @HCColumn value)과 자동 일치합니다. alias / 컬럼 순서 변경 / 일부 컬럼
        누락 시 매핑이 깨지므로 가공 없이 전체 컬럼 export 하세요.

    ============================================================
    """.trimIndent(),
)
