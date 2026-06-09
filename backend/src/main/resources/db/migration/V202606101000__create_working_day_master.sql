-- 영업일관리마스터 (SF WorkingDayMaster__c) 테이블 신규 생성.
--
-- 목적: 월매출 현황 "기준 진도율"(레거시 SF `calcBusinessRateOnlyThisMonth`) 의 영업일수 산출 source.
--   레거시는 `WorkingDayMaster__c` 에서 `WorkingDateCheck__c = 1` (영업일) row 를 기간 count 한다.
--   평일/공휴일 규칙을 코드로 유추하지 않고 운영이 직접 관리하는 영업일 달력 그대로 복제해야
--   레거시와 영업일수가 일치하므로, 동 마스터를 SF → RDS 단방향 마이그레이션(Stage1) 으로 적재한다.
--
-- 데이터 권위: SF (HC sync 대상 아님 — Heroku PG 미존재. SF → RDS 단방향 마이그레이션).
-- 스키마 권위: entity `WorkingDayMaster` (@Column).
-- 표준 필드: Id / Name(AutoNumber) / WorkingDate__c(Date) / WorkingDateCheck__c(Number) + audit + IsDeleted.

CREATE TABLE powersales.working_day_master (
    working_day_master_id    BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                     VARCHAR(18),
    name                     VARCHAR(80),
    working_date             DATE,
    -- SF WorkingDateCheck__c (Number, scale 0) — 1 = 영업일.
    working_date_check       INTEGER,
    is_deleted               BOOLEAN,
    owner_sfid               VARCHAR(18),
    created_by_sfid          VARCHAR(18),
    last_modified_by_sfid    VARCHAR(18),
    -- BaseEntity
    created_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_working_day_master_sfid UNIQUE (sfid)
);

-- 기준 진도율 산출은 (working_date BETWEEN ... AND working_date_check = 1) 범위 count 가 hot path.
CREATE INDEX idx_working_day_master_date_check
    ON powersales.working_day_master (working_date, working_date_check);
