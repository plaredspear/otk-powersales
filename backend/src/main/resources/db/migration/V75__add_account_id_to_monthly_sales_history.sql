-- 1. account_id 컬럼 추가 (NULL 허용)
ALTER TABLE salesforce2.monthly_sales_history
    ADD COLUMN account_id INTEGER;

-- 2. 기존 account_external_key 기반으로 account_id 채우기
UPDATE salesforce2.monthly_sales_history msh
SET account_id = a.account_id
FROM salesforce2.account a
WHERE msh.account_external_key = a.external_key;

-- 3. FK 제약조건 추가
ALTER TABLE salesforce2.monthly_sales_history
    ADD CONSTRAINT fk_monthly_sales_history_account
        FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

-- 4. 인덱스 추가
CREATE INDEX idx_monthly_sales_history_account_id
    ON salesforce2.monthly_sales_history(account_id);
