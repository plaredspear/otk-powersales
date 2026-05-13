-- Spec #602 — Account SF 누락 컬럼 24개 신규 도입.
--
-- 단일 권위: Salesforce Object (`Account`)
-- 정책 (스펙 §6.3):
--   - DB 컬럼명: SF API Name 직역 대신 한국어 라벨 의미 + snake_case 가독성 우선
--   - Lookup 필드는 <관계명>_sfid 단순 명명 (Q3 결정)
--   - Boolean 은 freezer_installed (BOOLEAN)
--   - 통화/숫자(18,0) 는 NUMERIC(18,0)
--   - picklist 6개 중 Type/FreezerType 만 enum + @Convert (Q5 옵션 1) — DB 타입은 VARCHAR 유지

ALTER TABLE powersales.account
    ADD COLUMN account_number      varchar(40),
    ADD COLUMN site                varchar(80),
    ADD COLUMN account_source      varchar(40),
    ADD COLUMN branch_cost_center  varchar(50),
    ADD COLUMN division_code       varchar(100),
    ADD COLUMN sales_dept_code     varchar(100),
    ADD COLUMN logistics_name      varchar(50),
    ADD COLUMN logistics_code      varchar(50),
    ADD COLUMN freezer_installed   boolean,
    ADD COLUMN freezer_type        varchar(20),
    ADD COLUMN remaining_credit    numeric(18, 0),
    ADD COLUMN total_credit        numeric(18, 0),
    ADD COLUMN map_coordinate      varchar(40),
    ADD COLUMN order_end_time      time,
    ADD COLUMN first_installed     date,
    ADD COLUMN description         text,
    ADD COLUMN website             varchar(255),
    ADD COLUMN fax                 varchar(40),
    ADD COLUMN business_number     varchar(20),
    ADD COLUMN annual_revenue      numeric(18, 0),
    ADD COLUMN number_of_employees integer,
    ADD COLUMN parent_sfid         varchar(18),
    ADD COLUMN rating              varchar(20),
    ADD COLUMN ownership           varchar(20);
