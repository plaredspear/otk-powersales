-- 여사원 일정관리 거래처 조회 쿼리 인덱스
--
-- AdminTeamScheduleService.getAccounts → AccountRepository.findByBranchCodeInAndAccountGroupIn
-- 실행 SQL: SELECT * FROM powersales.account
--           WHERE branch_code IN (?, ?) AND account_group IN (?, ?)
--
-- 기존 account 테이블은 sfid (unique) / FK 컬럼 / owner_user_id 외에 인덱스가 없어
-- 본 쿼리가 Seq Scan 으로 실행됨. branch_code 가 high-selectivity equality 필터라
-- leading column 으로 두고 account_group 을 trailing 으로 포함.
--
-- leading prefix 가 일치하는 다른 호출처도 함께 사용 가능:
--   - findByBranchCodeIn (branchCodes only)
--   - findByBranchCodeAndAccountGroupInAndIsDeletedNot

CREATE INDEX IF NOT EXISTS idx_account_branch_code_account_group
    ON powersales.account (branch_code, account_group);
