-- FK 누락 인덱스 2차 일괄 추가 (20건 / 10개 테이블)
--
-- V8(`add_fk_missing_indexes`) 이후 SF 정합 마이그레이션으로 owner/audit FK 인덱스는
-- 대부분 추가되었으나, 조인·필터에 실제 사용되는 다음 FK(@JoinColumn) 컬럼들이
-- 인덱스로 커버되지 않은 채 남아 있었다. PostgreSQL 은 FK 제약/관계 매핑이 있어도
-- child 측 컬럼에 자동 인덱스를 만들지 않으므로, 누락 시 조인/WHERE 필터가 Seq Scan 된다.
--
-- 명명 규칙: idx_<table>_<column> (V8 동일). 모든 식별자 63 byte 이내.
-- product_expiration 은 @HerokuOnly + ConstraintMode.NO_CONSTRAINT (DB FK 제약 없음) 이나,
-- employeeId IN 필터 등 실사용 조회 경로가 있어 비-unique 일반 인덱스를 추가한다 (unique 아님 → sfid 하이브리드 충돌 무관).
-- team_member_schedule 의 account_id / employee_id 는 (account_id, working_date) /
-- (employee_id, working_date) 복합 인덱스가 선두 컬럼으로 이미 커버하므로 본 배치에서 제외.

-- display_work_schedule: 진열스케줄마스터 목록 조인/필터 핵심
CREATE INDEX idx_display_work_schedule_account_id  ON powersales.display_work_schedule (account_id);
CREATE INDEX idx_display_work_schedule_employee_id ON powersales.display_work_schedule (employee_id);

-- claim: 거래처별 클레임 조회 (employee_id 는 idx_claim_employee_created 선두로 커버됨)
CREATE INDEX idx_claim_account_id ON powersales.claim (account_id);

-- product_expiration (@HerokuOnly): 사원/거래처/상품 조인 필터
CREATE INDEX idx_product_expiration_account_id  ON powersales.product_expiration (account_id);
CREATE INDEX idx_product_expiration_employee_id ON powersales.product_expiration (employee_id);
CREATE INDEX idx_product_expiration_product_id  ON powersales.product_expiration (product_id);

-- promotion: 거래처별 행사 / 대표상품 조인
CREATE INDEX idx_promotion_account_id         ON powersales.promotion (account_id);
CREATE INDEX idx_promotion_primary_product_id ON powersales.promotion (primary_product_id);

-- promotion_employee: 행사사원 ↔ 행사 조인
CREATE INDEX idx_promotion_employee_employee_id  ON powersales.promotion_employee (employee_id);
CREATE INDEX idx_promotion_employee_promotion_id ON powersales.promotion_employee (promotion_id);

-- push_message: 발송 사원 / audit 조인
CREATE INDEX idx_push_message_employee_id          ON powersales.push_message (employee_id);
CREATE INDEX idx_push_message_created_by_id        ON powersales.push_message (created_by_id);
CREATE INDEX idx_push_message_last_modified_by_id  ON powersales.push_message (last_modified_by_id);

-- push_message_receiver: 수신자 ↔ 메시지 조인
CREATE INDEX idx_push_message_receiver_employee_id     ON powersales.push_message_receiver (employee_id);
CREATE INDEX idx_push_message_receiver_push_message_id ON powersales.push_message_receiver (push_message_id);

-- notice: 작성 사원 조인
CREATE INDEX idx_notice_employee_id ON powersales.notice (employee_id);

-- daily_sales_history: 거래처별 일매출 조회
CREATE INDEX idx_daily_sales_history_account_id ON powersales.daily_sales_history (account_id);

-- team_member_schedule: 대체휴무 / 행사사원 / 팀리더 조인
CREATE INDEX idx_team_member_schedule_alt_holiday_id        ON powersales.team_member_schedule (alt_holiday_id);
CREATE INDEX idx_team_member_schedule_promotion_employee_id ON powersales.team_member_schedule (promotion_employee_id);
CREATE INDEX idx_team_member_schedule_team_leader_id        ON powersales.team_member_schedule (team_leader_id);
