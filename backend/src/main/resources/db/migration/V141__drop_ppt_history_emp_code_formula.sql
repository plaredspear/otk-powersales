-- sf-meta-diff: ProfessionalPromotionTeamHistory__c.empCode__c Formula 정합
-- README §6.7 정책: Formula 필드는 DB 컬럼 추가 X + @SFField 미부여
-- 사용자 결정 (2026-05-14): 옵션 (b) 채택 — 레거시 SF Formula runtime lookup 동등 회복.
-- SF empCode__c 는 calculatedFormula = EmployeeId__r.DKRetail__EmpCode__c (runtime cross-ref),
-- 저장 시점 freeze 가 아니므로 backend 가 컬럼에 cache 하면 시간 경과에 따라 SF 와 divergence.
-- 조회 시 employee.emp_code 조인으로 동등 동작 회복. Spec #747 카테고리 A Q2 의 시점 cache 정책 폐기.

ALTER TABLE powersales.professional_promotion_team_history DROP COLUMN IF EXISTS emp_code;
