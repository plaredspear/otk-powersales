-- ErpOrder sf-meta-diff 정합 (ERP_Order__c diff Q1~Q5):
--
-- (1) Q1/Q2: audit FK 타입 Employee → User (CreatedById / LastModifiedById 의 referenceTo == [User] 정합, §6.4 v2.7).
-- (2) Q3: 문자열 컬럼 4종 길이 확장 (SF 운영 메타 length 정합, 절단 위험 해소).
--     - sap_order_number  VARCHAR(20)  → VARCHAR(80)   (SF Name length 80)
--     - sap_account_code  VARCHAR(20)  → VARCHAR(100)  (SF SAPAccountCode__c length 100)
--     - employee_code     VARCHAR(20)  → VARCHAR(50)   (SF EmployeeCode__c length 50)
--     - employee_name     VARCHAR(50)  → VARCHAR(100)  (SF EmployeeName__c length 100)
-- (3) Q4: date 컬럼 2종 VARCHAR(8) yyyyMMdd → DATE (SF DeliveryRequestDate__c / OrderDate__c 의 type=date 자연 대응).
-- (4) Q5: order_sales_amount DOUBLE PRECISION → BIGINT (SF TotalOrderAmount__c precision=18, scale=0 정수 도메인 자연 대응).
--
-- 데이터 처리:
--   - 기존 created_by_id / last_modified_by_id 값은 Employee.employee_id → User.user_id 와 의미 정합 깨짐 → NULL 초기화.
--   - created_by_sfid / last_modified_by_sfid sync buffer 보존됨 — sf-migrate Phase 2 lookup (`<관계>_sfid` → `user.sfid` → 로컬 PK) 으로 FK 재채움.
--   - delivery_request_date / order_date 의 기존 VARCHAR(8) yyyyMMdd 값은 TO_DATE 변환.
--   - order_sales_amount DOUBLE 값은 정수 캐스팅 (SF scale=0 이므로 손실 없음).
--
-- 관련 마이그레이션:
--   V77 (audit FK Employee 도입), V107 (Owner* DROP — SF prod 메타 OwnerId 부재),
--   V128 (OrderRequest 동일 패턴 — audit FK User 전환).

-- ============================================================================
-- (1) audit FK 재정의 — Employee → User
-- ============================================================================

ALTER TABLE powersales.erp_order
    DROP CONSTRAINT fk_erp_order_created_by,
    DROP CONSTRAINT fk_erp_order_last_modified_by;

UPDATE powersales.erp_order
    SET created_by_id       = NULL,
        last_modified_by_id = NULL;

ALTER TABLE powersales.erp_order
    ADD CONSTRAINT fk_erp_order_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_erp_order_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales."user" (user_id)
        ON DELETE SET NULL;

-- created_by_id / last_modified_by_id 인덱스는 V77 에서 생성 — 컬럼명 변동 없음, 유지.

-- ============================================================================
-- (2) 문자열 컬럼 4종 length 확장
-- ============================================================================

ALTER TABLE powersales.erp_order
    ALTER COLUMN sap_order_number TYPE VARCHAR(80),
    ALTER COLUMN sap_account_code TYPE VARCHAR(100),
    ALTER COLUMN employee_code    TYPE VARCHAR(50),
    ALTER COLUMN employee_name    TYPE VARCHAR(100);

-- ============================================================================
-- (3) date 컬럼 2종 VARCHAR(8) yyyyMMdd → DATE
-- ============================================================================

ALTER TABLE powersales.erp_order
    ALTER COLUMN delivery_request_date TYPE DATE
        USING CASE
            WHEN delivery_request_date IS NULL OR btrim(delivery_request_date) = '' THEN NULL
            ELSE to_date(delivery_request_date, 'YYYYMMDD')
        END,
    ALTER COLUMN order_date TYPE DATE
        USING CASE
            WHEN order_date IS NULL OR btrim(order_date) = '' THEN NULL
            ELSE to_date(order_date, 'YYYYMMDD')
        END;

-- ============================================================================
-- (4) order_sales_amount DOUBLE PRECISION → BIGINT
-- ============================================================================

ALTER TABLE powersales.erp_order
    ALTER COLUMN order_sales_amount TYPE BIGINT
        USING CASE
            WHEN order_sales_amount IS NULL THEN NULL
            ELSE order_sales_amount::BIGINT
        END;
