-- DailySalesHistory__c SF 정합 — 누락 writeable 필드 `OwnerId` (소유자) polymorphic 적재.
--
-- 단일 권위: Salesforce Object (`DailySalesHistory__c`)
-- 필드: OwnerId (SF reference, referenceTo = [Group, User]) — polymorphic R-2
-- 패턴: MonthlySalesHistory (V145) / SalesProgressRateMaster 와 동일 — owner_sfid sync buffer +
--       owner_user_id (User FK) / owner_group_id (Group FK) XOR.
--
-- 데이터 처리:
--   - daily_sales_history 는 기존 owner 컬럼이 없어 신규 ADD 만 (rename/초기화 불요).
--   - owner_sfid 는 Stage1 적재가 채우고, Stage2 fk substep 이 prefix(005=User / 00G=Group)로
--     owner_user_id / owner_group_id 분기 채움. (SfFkResolveTables.POLYMORPHIC_OWNER_TABLES 등록)

ALTER TABLE powersales.daily_sales_history
    ADD COLUMN owner_sfid     varchar(18),
    ADD COLUMN owner_user_id  BIGINT,
    ADD COLUMN owner_group_id BIGINT;

-- User / Group FK 제약
ALTER TABLE powersales.daily_sales_history
    ADD CONSTRAINT fk_daily_sales_history_owner_user
        FOREIGN KEY (owner_user_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_daily_sales_history_owner_group
        FOREIGN KEY (owner_group_id) REFERENCES powersales."group" (group_id)
        ON DELETE SET NULL;

-- XOR CHECK — 둘 다 채움 금지 (둘 다 NULL 은 legacy 데이터 보존 위해 허용)
ALTER TABLE powersales.daily_sales_history
    ADD CONSTRAINT chk_daily_sales_history_owner_xor
        CHECK (
            (owner_user_id IS NULL AND owner_group_id IS NULL)
            OR ((owner_user_id IS NOT NULL) <> (owner_group_id IS NOT NULL))
        );

-- FK 인덱스 (PostgreSQL 은 FK 제약 추가 시 자동 인덱스 생성 안 함)
CREATE INDEX idx_daily_sales_history_owner_user_id  ON powersales.daily_sales_history (owner_user_id);
CREATE INDEX idx_daily_sales_history_owner_group_id ON powersales.daily_sales_history (owner_group_id);
