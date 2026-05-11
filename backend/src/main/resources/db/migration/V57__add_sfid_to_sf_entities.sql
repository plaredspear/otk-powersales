-- @SFObject 매핑 엔티티 중 sfid 컬럼이 누락된 10개 테이블에 sfid 컬럼 + partial UNIQUE index 추가.
--
-- 대상 테이블:
--   promotion, promotion_employee,
--   professional_promotion_team_master, professional_promotion_team_history,
--   organization, erp_order, erp_order_product,
--   attend_info, alternative_holiday, claim
--
-- 패턴: V20 (agreement_history) / V33 (holiday_master) 와 동일.
--   - sfid character varying(18) NULL 허용
--   - partial UNIQUE index (WHERE sfid IS NOT NULL) — 신규 row 는 NULL 이므로 unique 제약에서 제외.
--
-- ON DELETE: 컬럼 추가 + partial UNIQUE index 만 추가하며 FK 영향 없음.
-- JPA: ddl-auto = validate. 본 마이그레이션과 동시에 각 엔티티 클래스에 sfid 필드 매핑 추가.

ALTER TABLE powersales.promotion
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.promotion_employee
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.professional_promotion_team_master
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.professional_promotion_team_history
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.organization
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.erp_order
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.erp_order_product
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.attend_info
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.alternative_holiday
    ADD COLUMN sfid character varying(18);

ALTER TABLE powersales.claim
    ADD COLUMN sfid character varying(18);

CREATE UNIQUE INDEX idx_promotion_sfid
    ON powersales.promotion (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_promotion_employee_sfid
    ON powersales.promotion_employee (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_professional_promotion_team_master_sfid
    ON powersales.professional_promotion_team_master (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_professional_promotion_team_history_sfid
    ON powersales.professional_promotion_team_history (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_organization_sfid
    ON powersales.organization (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_erp_order_sfid
    ON powersales.erp_order (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_erp_order_product_sfid
    ON powersales.erp_order_product (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_attend_info_sfid
    ON powersales.attend_info (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_alternative_holiday_sfid
    ON powersales.alternative_holiday (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_claim_sfid
    ON powersales.claim (sfid)
    WHERE sfid IS NOT NULL;
