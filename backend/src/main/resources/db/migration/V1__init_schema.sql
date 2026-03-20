-- V1: Unified initial schema
-- All tables created with final names, columns, and constraints
-- Matches JPA entity @Table/@Column annotations exactly (Hibernate ddl-auto: validate)

CREATE EXTENSION IF NOT EXISTS hstore;

CREATE SCHEMA IF NOT EXISTS salesforce2;

-- ============================================================
-- 사원 (Employee)
-- ============================================================

CREATE TABLE salesforce2.employee (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    employee_number VARCHAR(100) UNIQUE,
    name VARCHAR(80),
    birth_date VARCHAR(10),
    status VARCHAR(40),
    app_login_active BOOLEAN,
    app_authority VARCHAR(255),
    org_name VARCHAR(100),
    cost_center_code VARCHAR(10),
    work_phone VARCHAR(255),
    phone VARCHAR(40),
    home_phone VARCHAR(255),
    start_date DATE,
    agreement_flag BOOLEAN,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT employee_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.employee_mng (
    employee_number VARCHAR(40) NOT NULL,
    emp_pwd VARCHAR(200),
    pwd_yn BOOLEAN,
    emp_uuid VARCHAR(200),
    emp_token VARCHAR(200),
    gps_yn BOOLEAN,
    gps_yn_date TIMESTAMP,
    last_agreement_number VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT employee_mng_pkey PRIMARY KEY (employee_number)
);

CREATE TABLE salesforce2.employee_his (
    empcode__c VARCHAR(80) NOT NULL,
    inst_date TIMESTAMP NOT NULL,
    CONSTRAINT employee_his_pkey PRIMARY KEY (empcode__c, inst_date)
);

CREATE TABLE salesforce2.employee_admin (
    empcode__c VARCHAR(40) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT employee_admin_pkey PRIMARY KEY (empcode__c)
);

-- ============================================================
-- 거래처 (Account)
-- ============================================================

CREATE TABLE salesforce2.account (
    account_id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(255),
    phone VARCHAR(40),
    mobile_phone VARCHAR(40),
    address1 VARCHAR(120),
    address2 VARCHAR(120),
    representative VARCHAR(100),
    abc_type VARCHAR(20),
    abc_type_code VARCHAR(40),
    external_key VARCHAR(100) UNIQUE,
    account_group VARCHAR(10),
    branch_code VARCHAR(100),
    branch_name VARCHAR(250),
    zip_code VARCHAR(100),
    latitude VARCHAR(100),
    longitude VARCHAR(100),
    closing_time1 VARCHAR(50),
    closing_time2 VARCHAR(50),
    closing_time3 VARCHAR(50),
    industry VARCHAR(255),
    werk1_tx VARCHAR(255),
    werk2_tx VARCHAR(255),
    werk3_tx VARCHAR(255),
    is_deleted BOOLEAN,
    account_type VARCHAR(20),
    account_status_name VARCHAR(40),
    employee_code VARCHAR(20),
    distribution VARCHAR(1),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT account_pkey PRIMARY KEY (account_id)
);

CREATE TABLE salesforce2.account_category_master (
    id BIGSERIAL NOT NULL,
    account_code VARCHAR(20) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT account_category_master_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 조직 (Organization)
-- ============================================================

CREATE TABLE salesforce2.organization (
    organization_id BIGSERIAL NOT NULL,
    cc_cd2 VARCHAR(100),
    org_cd2 VARCHAR(100),
    org_nm2 VARCHAR(100),
    cc_cd3 VARCHAR(100),
    org_cd3 VARCHAR(100),
    org_nm3 VARCHAR(100),
    cc_cd4 VARCHAR(100),
    org_cd4 VARCHAR(100),
    org_nm4 VARCHAR(100),
    cc_cd5 VARCHAR(100),
    org_cd5 VARCHAR(100),
    org_nm5 VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT organization_pkey PRIMARY KEY (organization_id)
);

-- ============================================================
-- 상품 (Product)
-- ============================================================

CREATE TABLE salesforce2.product (
    product_id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    product_code VARCHAR(100) UNIQUE,
    product_type VARCHAR(255),
    product_status VARCHAR(255),
    storage_condition VARCHAR(255),
    shelf_life VARCHAR(30),
    shelf_life_unit VARCHAR(40),
    shelf_life_full VARCHAR(1300),
    category1 VARCHAR(255),
    category2 VARCHAR(255),
    category3 VARCHAR(255),
    category_code1 VARCHAR(100),
    category_code2 VARCHAR(100),
    category_code3 VARCHAR(100),
    unit VARCHAR(40),
    ordering_unit VARCHAR(40),
    conversion_quantity DOUBLE PRECISION,
    box_receiving_quantity DOUBLE PRECISION,
    standard_unit_price DOUBLE PRECISION,
    standard_price DOUBLE PRECISION,
    super_tax DOUBLE PRECISION,
    launch_date DATE,
    logistics_barcode VARCHAR(100),
    taste_gift VARCHAR(1),
    product_features VARCHAR(255),
    selling_point VARCHAR(255),
    purpose VARCHAR(255),
    target_account_type VARCHAR(255),
    allergen VARCHAR(255),
    cross_contamination VARCHAR(255),
    img_ref_path VARCHAR(255),
    img_ref_path_front VARCHAR(255),
    img_ref_path_back VARCHAR(255),
    img_ref_path_txt VARCHAR(255),
    is_deleted BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT product_pkey PRIMARY KEY (product_id)
);

CREATE TABLE salesforce2.product_barcode (
    product_barcode_id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    product_name VARCHAR(255),
    barcode VARCHAR(255),
    unit VARCHAR(255),
    sort_order VARCHAR(255),
    product_id BIGINT,
    custom_key VARCHAR(255) UNIQUE,
    is_deleted BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT product_barcode_pkey PRIMARY KEY (product_barcode_id)
);

CREATE TABLE salesforce2.if_product (
    id INTEGER NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    product_code VARCHAR(100),
    product_type VARCHAR(255),
    product_status VARCHAR(255),
    storage_condition VARCHAR(255),
    shelf_life VARCHAR(30),
    shelf_life_unit VARCHAR(40),
    category1 VARCHAR(255),
    category2 VARCHAR(255),
    category3 VARCHAR(255),
    category_code1 VARCHAR(100),
    category_code2 VARCHAR(100),
    category_code3 VARCHAR(100),
    unit VARCHAR(40),
    ordering_unit VARCHAR(40),
    conversion_quantity DOUBLE PRECISION,
    box_receiving_quantity DOUBLE PRECISION,
    standard_unit_price DOUBLE PRECISION,
    super_tax DOUBLE PRECISION,
    launch_date DATE,
    logistics_barcode VARCHAR(100),
    taste_gift VARCHAR(1),
    product_features VARCHAR(255),
    selling_point VARCHAR(255),
    purpose VARCHAR(255),
    target_account_type VARCHAR(255),
    allergen VARCHAR(255),
    cross_contamination VARCHAR(255),
    img_ref_path VARCHAR(255),
    img_ref_path_front VARCHAR(255),
    img_ref_path_back VARCHAR(255),
    img_ref_path_txt VARCHAR(255),
    update_flag BOOLEAN,
    is_deleted BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT if_product_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.product_favorites (
    employeecode VARCHAR(80) NOT NULL,
    productcode VARCHAR(80) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT product_favorites_pkey PRIMARY KEY (employeecode, productcode)
);

-- ============================================================
-- 주문 (ERP Order)
-- ============================================================

CREATE TABLE salesforce2.erp_order (
    id BIGSERIAL NOT NULL,
    sap_order_number VARCHAR(20) NOT NULL UNIQUE,
    sap_account_code VARCHAR(20),
    sap_account_name VARCHAR(100),
    delivery_request_date VARCHAR(8),
    order_date VARCHAR(8),
    employee_code VARCHAR(20),
    employee_name VARCHAR(50),
    order_sales_amount DOUBLE PRECISION,
    order_channel VARCHAR(10),
    order_channel_nm VARCHAR(50),
    order_type VARCHAR(10),
    order_type_nm VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT erp_order_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.erp_order_product (
    id BIGSERIAL NOT NULL,
    erp_order_id BIGINT NOT NULL,
    sap_order_number VARCHAR(20) NOT NULL,
    line_number VARCHAR(10) NOT NULL,
    external_key VARCHAR(50) NOT NULL,
    product_code VARCHAR(20),
    product_name VARCHAR(100),
    order_quantity DOUBLE PRECISION,
    unit VARCHAR(10),
    confirm_quantity_box DOUBLE PRECISION,
    confirm_quantity DOUBLE PRECISION,
    confirm_unit VARCHAR(10),
    default_reason VARCHAR(100),
    line_item_status VARCHAR(20),
    delivery_status VARCHAR(10),
    shipping_driver_name VARCHAR(50),
    shipping_vehicle VARCHAR(20),
    shipping_driver_phone VARCHAR(20),
    shipping_schedule_time VARCHAR(20),
    shipping_complete_time VARCHAR(20),
    shipping_quantity_box DOUBLE PRECISION,
    shipping_quantity DOUBLE PRECISION,
    order_sales_line_amount DOUBLE PRECISION,
    shipping_amount DOUBLE PRECISION,
    plant VARCHAR(10),
    plant_nm VARCHAR(50),
    release_quantity DOUBLE PRECISION,
    release_amount DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT erp_order_product_pkey PRIMARY KEY (id),
    CONSTRAINT fk_erp_order_product_order FOREIGN KEY (erp_order_id) REFERENCES salesforce2.erp_order(id)
);

-- ============================================================
-- 일정 (Schedule)
-- ============================================================

CREATE TABLE salesforce2.team_member_schedule (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    employee_id BIGINT,
    working_date DATE,
    working_type VARCHAR(255),
    working_category1 VARCHAR(255),
    working_category2 VARCHAR(255),
    working_category3 VARCHAR(255),
    working_category4 VARCHAR(255),
    account_id INTEGER,
    team_leader_id BIGINT,
    alt_holiday_id BIGINT,
    commute_log_id VARCHAR(18),
    promotion_employee_id BIGINT,
    commute_report_datetime TIMESTAMP,
    id_field VARCHAR(30),
    traversal_flag VARCHAR(255),
    is_work_report VARCHAR(1300),
    equipment1 VARCHAR(10),
    equipment2 VARCHAR(10),
    equipment3 VARCHAR(10),
    equipment4 VARCHAR(10),
    equipment5 VARCHAR(10),
    equipment6 VARCHAR(10),
    equipment7 VARCHAR(10),
    equipment8 VARCHAR(10),
    equipment9 VARCHAR(10),
    equipment10 VARCHAR(10),
    yes_chk_cnt DOUBLE PRECISION,
    no_chk_cnt DOUBLE PRECISION,
    precaution_chk DOUBLE PRECISION,
    precaution VARCHAR(3000),
    start_time TIMESTAMP,
    complete_time TIMESTAMP,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT team_member_schedule_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.display_work_schedule (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    account_id INTEGER,
    employee_id BIGINT,
    start_date DATE,
    end_date DATE,
    confirmed BOOLEAN,
    type_of_work1 VARCHAR(255),
    type_of_work3 VARCHAR(255),
    type_of_work5 VARCHAR(255),
    owner_id BIGINT,
    cost_center_code VARCHAR(20),
    last_month_revenue BIGINT,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT display_work_schedule_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 휴가 (Holiday)
-- ============================================================

CREATE TABLE salesforce2.holiday_master (
    id BIGSERIAL NOT NULL,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT holiday_master_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.alternative_holiday (
    id BIGSERIAL NOT NULL,
    employee_id BIGINT NOT NULL,
    employee_name VARCHAR(50) NOT NULL,
    actual_work_date DATE NOT NULL,
    target_alt_holiday_date DATE NOT NULL,
    confirm_alt_holiday_date DATE,
    status VARCHAR(10) NOT NULL DEFAULT '신규',
    change_reason VARCHAR(500),
    created_by VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT alternative_holiday_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 행사/프로모션 (Promotion)
-- ============================================================

CREATE TABLE salesforce2.promotion (
    id BIGSERIAL NOT NULL,
    promotion_number VARCHAR(20) NOT NULL UNIQUE,
    promotion_name VARCHAR(200),
    promotion_type_id BIGINT,
    account_id INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    primary_product_id BIGINT,
    other_product VARCHAR(200),
    message VARCHAR(255),
    stand_location VARCHAR(200),
    target_amount BIGINT,
    actual_amount BIGINT,
    cost_center_code VARCHAR(100),
    remark VARCHAR(200),
    branch_name VARCHAR(100),
    category VARCHAR(50),
    product_type VARCHAR(50),
    is_closed BOOLEAN NOT NULL DEFAULT FALSE,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT promotion_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.promotion_type (
    id BIGSERIAL NOT NULL,
    name VARCHAR(50) NOT NULL UNIQUE,
    display_order INTEGER NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT promotion_type_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.promotion_employee (
    id BIGSERIAL NOT NULL,
    promotion_id BIGINT NOT NULL,
    employee_id BIGINT,
    schedule_date DATE,
    work_status VARCHAR(20),
    work_type1 VARCHAR(100),
    work_type3 VARCHAR(100),
    work_type4 VARCHAR(100),
    professional_promotion_team VARCHAR(100),
    schedule_id BIGINT,
    promo_close_by_tm BOOLEAN NOT NULL DEFAULT FALSE,
    base_price BIGINT,
    daily_target_count INTEGER,
    target_amount BIGINT,
    actual_amount BIGINT,
    primary_product_amount BIGINT,
    primary_sales_quantity INTEGER,
    primary_sales_price BIGINT,
    other_sales_amount BIGINT,
    other_sales_quantity INTEGER,
    s3_image_unique_key VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT promotion_employee_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 안전점검 (Safety Check)
-- ============================================================

CREATE TABLE salesforce2.safety_check_item (
    safety_check_item_id BIGSERIAL NOT NULL,
    question_num INTEGER NOT NULL DEFAULT 0,
    seq_num INTEGER NOT NULL DEFAULT 0,
    contents VARCHAR(500) NOT NULL,
    use_yn VARCHAR(1) DEFAULT 'Y',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT safety_check_item_pkey PRIMARY KEY (safety_check_item_id)
);

CREATE TABLE salesforce2.safety_check_submission (
    master_id VARCHAR(18) NOT NULL,
    employee_id BIGINT NOT NULL,
    working_date DATE NOT NULL,
    start_time TIMESTAMP,
    complete_time TIMESTAMP,
    yes_check_count INTEGER,
    no_check_count INTEGER,
    equipment1 VARCHAR(10),
    equipment2 VARCHAR(10),
    equipment3 VARCHAR(10),
    equipment4 VARCHAR(10),
    equipment5 VARCHAR(10),
    equipment6 VARCHAR(10),
    equipment7 VARCHAR(10),
    equipment8 VARCHAR(10),
    equipment9 VARCHAR(10),
    precaution VARCHAR(3000),
    precaution_check_count INTEGER,
    traversal_flag VARCHAR(255),
    event_master_id VARCHAR(18),
    complete_work_yn VARCHAR(18),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT safety_check_submission_pkey PRIMARY KEY (master_id, employee_id, working_date)
);

-- ============================================================
-- 공지/메시지 (Notice / Push)
-- ============================================================

CREATE TABLE salesforce2.notice (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    employee_id BIGINT,
    dkretail__scope__c VARCHAR(255),
    dkretail__category__c VARCHAR(255),
    dkretail__contents__c TEXT,
    dkretail__educategory__c VARCHAR(255),
    dkretail__jeejum__c VARCHAR(255),
    dkretail__jeejumcode__c VARCHAR(255),
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT notice_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.upload_file (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(255),
    uniquekey__c VARCHAR(500),
    recordid__c VARCHAR(18),
    size__c VARCHAR(50),
    isdeleted BOOLEAN,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT upload_file_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.push_message (
    id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    message__c VARCHAR(500),
    scheduledate__c TIMESTAMP,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT push_message_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.push_message_receiver (
    id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    employee_id BIGINT,
    message_id INTEGER,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT push_message_receiver_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 교육 (Education)
-- ============================================================

CREATE TABLE salesforce2.education_code (
    edu_code VARCHAR(20) NOT NULL,
    edu_code_nm VARCHAR(50),
    edu_type VARCHAR(10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT education_code_pkey PRIMARY KEY (edu_code)
);

CREATE TABLE salesforce2.education_post (
    edu_id VARCHAR(20) NOT NULL,
    edu_title VARCHAR(150),
    edu_content TEXT,
    edu_code VARCHAR(50),
    empcode__c VARCHAR(40),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT education_post_pkey PRIMARY KEY (edu_id)
);

CREATE TABLE salesforce2.education_post_attachment (
    edu_id VARCHAR(20) NOT NULL,
    edu_file_key VARCHAR(30) NOT NULL,
    edu_file_type VARCHAR(10),
    edu_file_orgnm VARCHAR(200),
    CONSTRAINT education_post_attachment_pkey PRIMARY KEY (edu_id, edu_file_key)
);

CREATE TABLE salesforce2.education_view_history (
    community_id VARCHAR(20) NOT NULL,
    empcode__c VARCHAR(40) NOT NULL,
    inst_date TIMESTAMP NOT NULL,
    name VARCHAR(80),
    costcentercode__c VARCHAR(10),
    CONSTRAINT education_view_history_pkey PRIMARY KEY (community_id, empcode__c, inst_date)
);

-- ============================================================
-- 점검 (Inspection)
-- ============================================================

CREATE TABLE salesforce2.inspection_theme (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    title__c VARCHAR(250),
    startdate__c DATE,
    enddate__c DATE,
    department__c VARCHAR(100),
    branchcode__c VARCHAR(30),
    publicflag__c BOOLEAN,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT inspection_theme_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 매출 (Sales)
-- ============================================================

CREATE TABLE salesforce2.daily_sales_history (
    id BIGSERIAL NOT NULL,
    sap_account_code VARCHAR(20) NOT NULL,
    sales_date VARCHAR(8) NOT NULL,
    external_key VARCHAR(30) NOT NULL UNIQUE,
    erp_sales_amount1 DOUBLE PRECISION,
    erp_sales_amount2 DOUBLE PRECISION,
    erp_sales_amount3 DOUBLE PRECISION,
    erp_distribution_amount1 DOUBLE PRECISION,
    erp_distribution_amount2 DOUBLE PRECISION,
    erp_distribution_amount3 DOUBLE PRECISION,
    ledger_amount DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT daily_sales_history_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.monthly_sales_history (
    id BIGSERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    account_externalkey__c VARCHAR(1300),
    account_branchname__c VARCHAR(1300),
    account_type__c VARCHAR(1300),
    salesyear__c VARCHAR(255),
    salesmonth__c VARCHAR(255),
    fm_year__c DOUBLE PRECISION,
    fm_month__c DOUBLE PRECISION,
    targetmonthresults__c DOUBLE PRECISION,
    lastmonthresults__c DOUBLE PRECISION,
    lastmonthtargetfomula__c DOUBLE PRECISION,
    lastmonthtargetachievedratio__c DOUBLE PRECISION,
    shipclosingamount__c DOUBLE PRECISION,
    abcclosingamount1__c DOUBLE PRECISION,
    abcclosingamount2__c DOUBLE PRECISION,
    abcclosingamount3__c DOUBLE PRECISION,
    ambientpurpose__c DOUBLE PRECISION,
    fridgepurpose__c DOUBLE PRECISION,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    externalkey__c VARCHAR(30) UNIQUE,
    rlsales__c DOUBLE PRECISION,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT monthly_sales_history_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 평가 (Review)
-- ============================================================

CREATE TABLE salesforce2.hq_review (
    id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    branchcode__c VARCHAR(100),
    branchname__c VARCHAR(100),
    firstdayofmonth__c DATE,
    evaluationytype__c VARCHAR(255),
    abctypecode__c VARCHAR(255),
    hr_code_c__c VARCHAR(255),
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT hq_review_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.staff_review (
    id SERIAL NOT NULL,
    sfid VARCHAR(18),
    name VARCHAR(80),
    employee_id BIGINT,
    employeename__c VARCHAR(1300),
    employeenumber__c VARCHAR(1300),
    branch__c VARCHAR(1300),
    branchreviews__c VARCHAR(18),
    costcentercode__c VARCHAR(1300),
    employeetotalscore__c DOUBLE PRECISION,
    attendance__c DOUBLE PRECISION,
    instructionsdefault__c DOUBLE PRECISION,
    priority_eventitemmanage__c DOUBLE PRECISION,
    displaymanageeventgoals__c DOUBLE PRECISION,
    businesspartnerties__c DOUBLE PRECISION,
    clothessatellite__c DOUBLE PRECISION,
    productmanagecallment__c DOUBLE PRECISION,
    educationalevaluation__c DOUBLE PRECISION,
    isdeleted BOOLEAN,
    _hc_lastop VARCHAR(32),
    _hc_err TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT staff_review_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 기타 (Misc)
-- ============================================================

CREATE TABLE salesforce2.agreement_word (
    id SERIAL NOT NULL,
    name VARCHAR(80),
    contents__c VARCHAR(8000),
    active__c BOOLEAN,
    activedate__c DATE,
    afteractivedate__c DATE,
    isdeleted BOOLEAN,
    sfid VARCHAR(18),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT agreement_word_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.agreement_history (
    id BIGSERIAL NOT NULL,
    employeeid__c BIGINT NOT NULL,
    agreementflag__c BOOLEAN NOT NULL,
    agreementdate__c DATE NOT NULL,
    agreementwordid__c BIGINT NOT NULL,
    isdeleted BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT agreement_history_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.device_version (
    version VARCHAR(10) NOT NULL,
    device VARCHAR(10) NOT NULL,
    createdate TIMESTAMP NOT NULL,
    contents VARCHAR(1000) NOT NULL,
    s3_key VARCHAR(200) NOT NULL,
    file_url VARCHAR(300),
    s3_key_ipa VARCHAR(200),
    file_url_ipa VARCHAR(300),
    CONSTRAINT device_version_pkey PRIMARY KEY (version, device)
);

CREATE TABLE salesforce2.expirationdate__mng (
    seq SERIAL NOT NULL,
    account_id VARCHAR(100),
    account_code VARCHAR(100),
    employee_id BIGINT,
    product_id VARCHAR(100),
    product_code VARCHAR(100),
    expiration_date DATE,
    alarm_date DATE,
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT expirationdate__mng_pkey PRIMARY KEY (seq)
);

CREATE TABLE salesforce2.system_code_master (
    id BIGSERIAL NOT NULL,
    company_code VARCHAR(10) NOT NULL,
    group_code VARCHAR(20) NOT NULL,
    detail_code VARCHAR(20) NOT NULL,
    group_code_name VARCHAR(100),
    detail_code_name VARCHAR(100),
    seq VARCHAR(10),
    external_key VARCHAR(60) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT system_code_master_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.sap_sync_log (
    id BIGSERIAL NOT NULL,
    api_name VARCHAR(100) NOT NULL,
    request_count INTEGER NOT NULL,
    success_count INTEGER NOT NULL,
    fail_count INTEGER NOT NULL,
    error_detail TEXT,
    duration_ms BIGINT NOT NULL,
    request_ip VARCHAR(45),
    requested_at TIMESTAMP NOT NULL,
    completed_at TIMESTAMP NOT NULL,
    CONSTRAINT sap_sync_log_pkey PRIMARY KEY (id)
);

-- ============================================================
-- 인사발령/근태 (Appointment / AttendInfo)
-- ============================================================

CREATE TABLE salesforce2.appointment (
    appointment_id BIGSERIAL NOT NULL,
    employee_code VARCHAR(20) NOT NULL,
    emp_code_exist BOOLEAN NOT NULL DEFAULT FALSE,
    after_org_code VARCHAR(20),
    after_org_name VARCHAR(100),
    jikchak VARCHAR(50),
    jikwee VARCHAR(50),
    jikgub VARCHAR(20),
    work_type VARCHAR(20),
    manage_type VARCHAR(50),
    job_code VARCHAR(20),
    work_area VARCHAR(50),
    jikjong VARCHAR(50),
    appoint_date VARCHAR(8) NOT NULL,
    job_name VARCHAR(100),
    ord_detail_code VARCHAR(20),
    ord_detail_node VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT appointment_pkey PRIMARY KEY (appointment_id)
);
CREATE INDEX idx_appointment_employee ON salesforce2.appointment (employee_code);
CREATE INDEX idx_appointment_date ON salesforce2.appointment (appoint_date);

CREATE TABLE salesforce2.attend_info (
    id BIGSERIAL NOT NULL,
    employee_code VARCHAR(20) NOT NULL,
    start_date VARCHAR(8) NOT NULL,
    end_date VARCHAR(8),
    attend_type VARCHAR(50),
    status VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT attend_info_pkey PRIMARY KEY (id)
);
CREATE INDEX idx_attend_info_employee ON salesforce2.attend_info (employee_code);
CREATE INDEX idx_attend_info_start_date ON salesforce2.attend_info (start_date);

-- ============================================================
-- 클레임 임시 (TmpClaim / TmpClaimCode)
-- ============================================================

CREATE TABLE salesforce2.tmp_claim (
    id BIGSERIAL NOT NULL,
    tmp_sapaccountname VARCHAR(80),
    tmp_sapaccountcode VARCHAR(80),
    tmp_productname VARCHAR(100),
    tmp_productcode VARCHAR(80),
    tmp_expirationdate VARCHAR(80),
    tmp_claimtype1 VARCHAR(80),
    tmp_claimtype2 VARCHAR(80),
    tmp_description TEXT,
    tmp_quantity VARCHAR(80),
    tmp_claimimagefilename VARCHAR(200),
    tmp_partimagefilename VARCHAR(200),
    tmp_amount VARCHAR(80),
    tmp_purchasemethod VARCHAR(80),
    tmp_receiptimagefilename VARCHAR(200),
    tmp_requesttype TEXT,
    tmp_employeecode VARCHAR(80),
    tmp_claimtype1_name VARCHAR(80),
    tmp_claimtype2_name VARCHAR(80),
    tmp_purchasecode VARCHAR(80),
    tmp_claimimageextension VARCHAR(80),
    tmp_partimageextension VARCHAR(80),
    tmp_receiptimageextension VARCHAR(80),
    tmp_receiptimagebuffer TEXT,
    tmp_partimagebuffer TEXT,
    tmp_claimimagebuffer TEXT,
    tmp_manufacturingdate VARCHAR(80),
    claim_name VARCHAR(80),
    claim_sequence VARCHAR(80),
    action_code VARCHAR(20),
    claim_status VARCHAR(40),
    claim_content TEXT,
    reason_type VARCHAR(80),
    cosmos_key VARCHAR(80),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT tmp_claim_pkey PRIMARY KEY (id)
);

CREATE TABLE salesforce2.tmp_claimcode (
    claim1_code VARCHAR(80),
    claim1_name VARCHAR(80),
    claim2_code VARCHAR(80),
    claim2_name VARCHAR(80),
    inst_date TIMESTAMP,
    upd_date TIMESTAMP
);
