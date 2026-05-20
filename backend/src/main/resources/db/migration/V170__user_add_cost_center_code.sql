-- User 테이블에 cost_center_code 컬럼 추가 — Employee.cost_center_code derived 캐시.
-- 인증/권한 hot path 에서 Employee lookup 없이 데이터 스코프 (조장 → 같은 부서 여사원) 판정용.
-- SoT: Employee.cost_center_code. 동기화: EmployeeProfileResolver / UserProvisioningService /
-- AppointmentUserProfileUpdater / AdminEmployeeUpdateService / EmployeeUpsertService 의 쓰기 site hook.
-- 운영 길이: Employee.cost_center_code = VARCHAR(20). 동일 길이로 정합.

ALTER TABLE "user" ADD COLUMN cost_center_code VARCHAR(20);
