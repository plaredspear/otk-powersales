-- Account.account_id 및 이를 integer 로 참조하던 FK 컬럼을 integer → bigint 로 확대.
--
-- 배경:
--   account 테이블은 PowerSales 레거시(Heroku Postgres) 원본 스키마를 V1 으로 덤프해 온 것이라
--   account_id 가 integer(4byte) 로 정의되어 있었음. 신규 도메인 테이블은 전부 bigint 라
--   엔티티 id 타입이 Account 만 Int, 나머지는 Long 으로 불일치.
--   서비스 곳곳에 accountId.toInt() / List<Int> 변환이 흩어져 유지보수 부담.
--   엔티티 id 를 Long 으로 통일하기 위해 DB 컬럼을 bigint 로 확대.
--
-- 안전성:
--   integer → bigint 는 widening 변환이라 기존 account_id 값이 손실 없이 그대로 보존됨.
--   PK 시퀀스(account_account_id_seq)는 내부적으로 bigint 라 채번 연속성 유지.
--   FK 정합: PK(account.account_id) 와 그것을 참조하는 모든 child FK 컬럼을 본 파일(= Flyway
--   단일 트랜잭션) 안에서 함께 ALTER 하므로 트랜잭션 내 타입 정합이 유지됨. PK 만 올리고
--   child FK 를 별도 마이그레이션으로 미루면 그 사이 타입 불일치 FK 가 생기므로 동시 변경이 핵심.
--
-- 변경 대상 (현재 운영 스키마 기준 실제 integer 인 컬럼만):
--   - account.account_id (PK, V1 integer)
--   - attendance_log / display_work_schedule / product_expiration /
--     monthly_female_employee_integration_schedule / professional_promotion_team_master /
--     promotion / team_member_schedule (V1 integer FK)
--   - daily_sales_history.account_id (V202606040154 에서 integer 로 추가)
--
-- 변경 제외 (이미 bigint — 본 작업과 무관):
--   - account.parent_id (V58 에서 bigint 로 생성)
--   - monthly_sales_history.account_id (V202606012103 recreate 시 bigint)
--   - claim.store_id / order_request / suggestion / site_activity /
--     sales_progress_rate_master / tmp_* (각 생성 마이그레이션에서 bigint)

-- PK
ALTER TABLE powersales.account
    ALTER COLUMN account_id TYPE bigint;

-- integer 로 참조하던 child FK 컬럼 (integer → bigint)
ALTER TABLE powersales.attendance_log
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.display_work_schedule
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.product_expiration
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.monthly_female_employee_integration_schedule
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.professional_promotion_team_master
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.promotion
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.team_member_schedule
    ALTER COLUMN account_id TYPE bigint;

ALTER TABLE powersales.daily_sales_history
    ALTER COLUMN account_id TYPE bigint;
