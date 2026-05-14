-- Spec #759: EmployeeProfileResolver 산출 캐시 컬럼 추가
-- SF AppointmentTriggerHandler.afterInsert 동등 동작을 위해 User 에 profile_type / is_sales_support 보존.

ALTER TABLE "user"
    ADD COLUMN profile_type VARCHAR(40) NOT NULL DEFAULT 'STAFF';

ALTER TABLE "user"
    ADD COLUMN is_sales_support BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN "user".profile_type IS 'EmployeeProfileResolver 산출 — SF Profile 등급 (MARKETING/STAFF/TEAM_LEADER/BRANCH_MANAGER/SALES_MANAGER/BUSINESS_DIRECTOR/DIVISION_HEAD/SALES_REP/SYSTEM_ADMIN)';
COMMENT ON COLUMN "user".is_sales_support IS 'UserRoleResolver 산출 — 영업지원실 / 영업본부 소속 여부 (SF UserRole.Name LIKE %영업지원% OR ==영업본부)';
