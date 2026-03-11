-- 행사조원(PromotionEmployee) 테이블 생성

CREATE TABLE dkretail__promotion_employee__c (
    id              BIGSERIAL PRIMARY KEY,
    promotion_id    BIGINT NOT NULL,
    employee_sfid   VARCHAR(18) NOT NULL,
    schedule_date   DATE NOT NULL,
    work_status     VARCHAR(20) NOT NULL,
    work_type1      VARCHAR(100) NOT NULL,
    work_type3      VARCHAR(100) NOT NULL,
    work_type4      VARCHAR(100),
    professional_promotion_team VARCHAR(100),
    schedule_id     BIGINT,
    promo_close_by_tm BOOLEAN NOT NULL DEFAULT FALSE,
    base_price      BIGINT,
    daily_target_count INTEGER,
    target_amount   BIGINT,
    actual_amount   BIGINT,
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_pe_promotion FOREIGN KEY (promotion_id)
        REFERENCES dkretail__promotion__c(id)
);

CREATE INDEX idx_pe_promotion_id ON dkretail__promotion_employee__c(promotion_id);
CREATE INDEX idx_pe_employee_sfid ON dkretail__promotion_employee__c(employee_sfid);
CREATE INDEX idx_pe_schedule_date ON dkretail__promotion_employee__c(schedule_date);
CREATE INDEX idx_pe_schedule_id ON dkretail__promotion_employee__c(schedule_id);
