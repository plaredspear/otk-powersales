package com.otoki.powersales.platform.common.salesforce

/**
 * Heroku PG `salesforce2.*` 의 **Heroku-only 테이블** 마커.
 *
 * Heroku Connect (HC) 동기 대상이 아닌, Heroku 측이 단독으로 운영하는 테이블에 부착.
 * 해당 데이터의 권위 출처는 Salesforce 가 아닌 **Heroku** 이며, 신규 시스템 마이그레이션 시
 * Heroku DB 에서 직접 추출한다.
 *
 * 판별 기준 — Heroku PG dump (`docs/plan/old_source_260516/파워세일즈 스키마 1.sql.sql`)
 * 의 `CREATE TABLE salesforce2.<table>` 본문에 `_hc_lastop varchar(32)` 컬럼이 **부재** 하면 Heroku-only.
 *
 * - HC sync 대상 (`_hc_lastop` + `_hc_err` + `sfid` 메타 컬럼 보유) → 권위 = SF → entity 에 [SFObject] 부착
 * - HC sync 대상 아님 → 권위 = Heroku → entity 에 본 [HerokuOnly] 부착
 *
 * 자세한 정책은 `.claude/guides/data-migration-source-of-truth.md` 참조.
 *
 * @param value Heroku PG 테이블명 (`salesforce2.` schema prefix 제외, 소문자)
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class HerokuOnly(val value: String)
