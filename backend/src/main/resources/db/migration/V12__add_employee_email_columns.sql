-- 스펙 #557: SAP 직원 마스터 인바운드용 이메일 컬럼 추가.
-- WorkEmail / Email 필드를 employee 테이블에 신규 컬럼으로 적재한다.
-- @SFField 미부여 — 신규 시스템 전용 컬럼.

ALTER TABLE powersales.employee
    ADD COLUMN work_email character varying(100),
    ADD COLUMN email character varying(100);
