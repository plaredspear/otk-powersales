-- employee 테이블에 성별, 퇴사일 컬럼 추가 (SAP 사원마스터 수신 필드)
ALTER TABLE employee ADD COLUMN sex VARCHAR(10);
ALTER TABLE employee ADD COLUMN end_date DATE;
