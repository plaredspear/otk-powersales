-- V23: display_work_schedule.full_name 및 expirationdate__mng.employee_id의 sfid를 사번(employee_id)으로 전환
-- 멱등성 보장: sfid 형식(15~18자 알파벳+숫자, 'a'/'0'으로 시작)만 변환, 이미 사번인 레코드는 건너뜀

-- 1. display_work_schedule.full_name: sfid → employee_id
UPDATE display_work_schedule dws
SET full_name = e.employee_id
FROM employee e
WHERE dws.full_name = e.sfid
  AND e.sfid IS NOT NULL
  AND e.employee_id IS NOT NULL;

-- 2. expirationdate__mng.employee_id: sfid → employee_id
UPDATE expirationdate__mng em
SET employee_id = e.employee_id
FROM employee e
WHERE em.employee_id = e.sfid
  AND e.sfid IS NOT NULL
  AND e.employee_id IS NOT NULL;
