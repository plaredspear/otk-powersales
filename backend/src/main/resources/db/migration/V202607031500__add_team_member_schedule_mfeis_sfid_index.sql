-- team_member_schedule.monthly_female_employee_integration_schedule_sfid 인덱스 추가.
--
-- 목적: SF 데이터 마이그레이션 Stage 2-A FK Resolve 가속화.
--   Stage 2-A (SfMigrationStage2FkService) 는 powersales 의 모든 *_sfid 컬럼을 스캔하여
--   각 컬럼에 대해 ref 테이블의 sfid 와 LEFT JOIN 하는 UPDATE 로 신규 FK Long id 를 채운다.
--   team_member_schedule 은 1.76M row (V207 사전 분석) 규모라, 인덱스 없이 이 컬럼으로
--   조인하면 대량 Seq Scan 이 발생한다. 본 인덱스로 FK Resolve UPDATE 조인을 가속한다.
--
-- MFEIS 참조가 없는 일정 row 는 sfid 가 NULL 이므로 partial (WHERE ... IS NOT NULL) 로 제외한다
-- (V207 의 employee_id / account_id 인덱스와 동일한 partial 정책).
CREATE INDEX idx_team_member_schedule_mfeis_sfid
    ON powersales.team_member_schedule (monthly_female_employee_integration_schedule_sfid)
    WHERE monthly_female_employee_integration_schedule_sfid IS NOT NULL;
