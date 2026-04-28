-- 스펙 #564: scheduled_job_run 컬럼을 전사 컨벤션(TIMESTAMP WITHOUT TIME ZONE + UTC wall clock)에 정렬.
--
-- 배경: Spec #548 에서 #550(전사 timezone 마이그레이션) 도입 가정으로 `TIMESTAMP WITH TIME ZONE`
-- 을 선제 적용했으나 #550 이 폐기되었다. 본 스펙에서 #548 의 컬럼을 전사 컨벤션으로 되돌린다.
-- USING 캐스팅에서 기존 값을 UTC 로 해석하여 wall clock 만 보존 — 절대 시점 손실 없음.

ALTER TABLE powersales.scheduled_job_run
    ALTER COLUMN started_at TYPE TIMESTAMP(3) WITHOUT TIME ZONE
        USING started_at AT TIME ZONE 'UTC';

ALTER TABLE powersales.scheduled_job_run
    ALTER COLUMN ended_at TYPE TIMESTAMP(3) WITHOUT TIME ZONE
        USING ended_at AT TIME ZONE 'UTC';

ALTER TABLE powersales.scheduled_job_run
    ALTER COLUMN created_at TYPE TIMESTAMP(3) WITHOUT TIME ZONE
        USING created_at AT TIME ZONE 'UTC';
