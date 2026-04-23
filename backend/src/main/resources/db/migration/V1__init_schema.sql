-- V1: Squashed initial schema for salesforce2 namespace
--
-- V1~V91 (pre-squash 2026-04-23) 을 현재 dev-db (PostgreSQL 16.6) 스키마 스냅샷
-- 기준으로 통합한 baseline. JPA @Entity 메타데이터와 정렬되어 있어
-- Hibernate ddl-auto: validate 로 검증됨.
--
-- 생성 방법: backend/scripts/migration-squash/ 참고 (dump-schema.sh + 정규화)
-- 롤백: git tag pre-flyway-squash-20260423 에서 이전 V1~V91 복구 가능.

CREATE EXTENSION IF NOT EXISTS hstore;

--
--

--
-- Name: salesforce2; Type: SCHEMA; Schema: -; Owner: -
--

CREATE SCHEMA IF NOT EXISTS salesforce2;

--
-- Name: account; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.account (
    account_id integer NOT NULL,
    sfid character varying(18),
    name character varying(255),
    phone character varying(40),
    mobile_phone character varying(40),
    address1 character varying(120),
    address2 character varying(120),
    representative character varying(100),
    abc_type character varying(20),
    abc_type_code character varying(40),
    external_key character varying(100),
    account_group character varying(10),
    branch_code character varying(100),
    branch_name character varying(250),
    zip_code character varying(100),
    latitude character varying(100),
    longitude character varying(100),
    closing_time1 character varying(50),
    closing_time2 character varying(50),
    closing_time3 character varying(50),
    industry character varying(255),
    werk1_tx character varying(255),
    werk2_tx character varying(255),
    werk3_tx character varying(255),
    is_deleted boolean,
    account_type character varying(20),
    account_status_name character varying(40),
    employee_code character varying(20),
    distribution character varying(1),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: account_account_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.account_account_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: account_account_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.account_account_id_seq OWNED BY salesforce2.account.account_id;

--
-- Name: account_category_master; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.account_category_master (
    id bigint NOT NULL,
    account_code character varying(20) NOT NULL,
    name character varying(100) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: account_category_master_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.account_category_master_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: account_category_master_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.account_category_master_id_seq OWNED BY salesforce2.account_category_master.id;

--
-- Name: agreement_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.agreement_history (
    agreement_history_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    agreement_flag boolean NOT NULL,
    agreement_date date NOT NULL,
    agreement_word_id bigint NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: agreement_history_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.agreement_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: agreement_history_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.agreement_history_id_seq OWNED BY salesforce2.agreement_history.agreement_history_id;

--
-- Name: agreement_word; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.agreement_word (
    agreement_word_id integer NOT NULL,
    name character varying(80),
    contents character varying(8000),
    active boolean,
    active_date date,
    after_active_date date,
    is_deleted boolean,
    sfid character varying(18),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: agreement_word_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.agreement_word_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: agreement_word_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.agreement_word_id_seq OWNED BY salesforce2.agreement_word.agreement_word_id;

--
-- Name: alternative_holiday; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.alternative_holiday (
    alternative_holiday_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    employee_name character varying(50) NOT NULL,
    actual_work_date date NOT NULL,
    target_alt_holiday_date date NOT NULL,
    confirm_alt_holiday_date date,
    status character varying(10) DEFAULT '신규'::character varying NOT NULL,
    change_reason character varying(500),
    created_by character varying(20) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_sfid character varying(18)
);

--
-- Name: alternative_holiday_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.alternative_holiday_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: alternative_holiday_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.alternative_holiday_id_seq OWNED BY salesforce2.alternative_holiday.alternative_holiday_id;

--
-- Name: appointment; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.appointment (
    appointment_id bigint NOT NULL,
    employee_code character varying(20) NOT NULL,
    emp_code_exist boolean DEFAULT false NOT NULL,
    after_org_code character varying(20),
    after_org_name character varying(100),
    jikchak character varying(50),
    jikwee character varying(50),
    jikgub character varying(20),
    work_type character varying(20),
    manage_type character varying(50),
    job_code character varying(20),
    work_area character varying(50),
    jikjong character varying(50),
    appoint_date character varying(8) NOT NULL,
    job_name character varying(100),
    ord_detail_code character varying(20),
    ord_detail_node character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: appointment_appointment_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.appointment_appointment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: appointment_appointment_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.appointment_appointment_id_seq OWNED BY salesforce2.appointment.appointment_id;

--
-- Name: attend_info; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.attend_info (
    id bigint NOT NULL,
    employee_code character varying(20) NOT NULL,
    start_date character varying(8) NOT NULL,
    end_date character varying(8),
    attend_type character varying(50),
    status character varying(20),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: attend_info_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.attend_info_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: attend_info_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.attend_info_id_seq OWNED BY salesforce2.attend_info.id;

--
-- Name: attendance_log; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.attendance_log (
    attendance_log_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    employee_sfid character varying(18),
    employee_id bigint,
    attendance_date timestamp without time zone,
    account_sfid character varying(18),
    account_id integer,
    second_work_type character varying(255),
    reason character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: attendance_log_attendance_log_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.attendance_log_attendance_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: attendance_log_attendance_log_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.attendance_log_attendance_log_id_seq OWNED BY salesforce2.attendance_log.attendance_log_id;

--
-- Name: claim; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim (
    claim_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    store_id bigint NOT NULL,
    store_name character varying(100) NOT NULL,
    product_code character varying(20) NOT NULL,
    product_name character varying(200) NOT NULL,
    date_type character varying(20) NOT NULL,
    date date NOT NULL,
    category_id bigint NOT NULL,
    subcategory_id bigint NOT NULL,
    defect_description character varying(1000) NOT NULL,
    defect_quantity integer NOT NULL,
    purchase_amount integer,
    purchase_method_code character varying(10),
    purchase_method_name character varying(50),
    request_type_code character varying(10),
    request_type_name character varying(50),
    status character varying(20) DEFAULT 'SUBMITTED'::character varying NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_sfid character varying(18),
    account_sfid character varying(18)
);

--
-- Name: claim_categories; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim_categories (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

--
-- Name: claim_categories_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.claim_categories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: claim_categories_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.claim_categories_id_seq OWNED BY salesforce2.claim_categories.id;

--
-- Name: claim_photos; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim_photos (
    id bigint NOT NULL,
    claim_id bigint NOT NULL,
    photo_type character varying(20) NOT NULL,
    url character varying(500) NOT NULL,
    original_file_name character varying(255) NOT NULL,
    file_size bigint NOT NULL,
    content_type character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: claim_photos_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.claim_photos_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: claim_photos_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.claim_photos_id_seq OWNED BY salesforce2.claim_photos.id;

--
-- Name: claim_purchase_methods; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim_purchase_methods (
    code character varying(10) NOT NULL,
    name character varying(50) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

--
-- Name: claim_request_types; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim_request_types (
    code character varying(10) NOT NULL,
    name character varying(50) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

--
-- Name: claim_subcategories; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.claim_subcategories (
    id bigint NOT NULL,
    category_id bigint NOT NULL,
    name character varying(50) NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    is_active boolean DEFAULT true NOT NULL
);

--
-- Name: claim_subcategories_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.claim_subcategories_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: claim_subcategories_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.claim_subcategories_id_seq OWNED BY salesforce2.claim_subcategories.id;

--
-- Name: claims_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.claims_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: claims_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.claims_id_seq OWNED BY salesforce2.claim.claim_id;

--
-- Name: daily_sales_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.daily_sales_history (
    id bigint NOT NULL,
    sap_account_code character varying(20) NOT NULL,
    sales_date character varying(8) NOT NULL,
    external_key character varying(30) NOT NULL,
    erp_sales_amount1 double precision,
    erp_sales_amount2 double precision,
    erp_sales_amount3 double precision,
    erp_distribution_amount1 double precision,
    erp_distribution_amount2 double precision,
    erp_distribution_amount3 double precision,
    ledger_amount double precision,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: daily_sales_history_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.daily_sales_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: daily_sales_history_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.daily_sales_history_id_seq OWNED BY salesforce2.daily_sales_history.id;

--
-- Name: device_version; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.device_version (
    version character varying(10) NOT NULL,
    device character varying(10) NOT NULL,
    create_date timestamp without time zone NOT NULL,
    contents character varying(1000) NOT NULL,
    s3_key character varying(200) NOT NULL,
    file_url character varying(300),
    s3_key_ipa character varying(200),
    file_url_ipa character varying(300)
);

--
-- Name: display_work_schedule; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.display_work_schedule (
    display_work_schedule_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    account_id integer,
    employee_id bigint,
    start_date date,
    end_date date,
    confirmed boolean,
    type_of_work1 character varying(255),
    type_of_work3 character varying(255),
    type_of_work5 character varying(255),
    owner_id bigint,
    cost_center_code character varying(20),
    last_month_revenue bigint,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_sfid character varying(18),
    employee_sfid character varying(18),
    owner_sfid character varying(18),
    type_of_work4 character varying(20)
);

--
-- Name: display_work_schedule_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.display_work_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: display_work_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.display_work_schedule_id_seq OWNED BY salesforce2.display_work_schedule.display_work_schedule_id;

--
-- Name: education_code; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.education_code (
    edu_code_name character varying(50),
    edu_type character varying(10),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    education_code_id bigint NOT NULL,
    edu_code character varying(20) NOT NULL
);

--
-- Name: education_code_education_code_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.education_code ALTER COLUMN education_code_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME salesforce2.education_code_education_code_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: education_post; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.education_post (
    edu_id character varying(20),
    title character varying(150),
    content text,
    education_code character varying(50),
    emp_code character varying(40),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_id bigint,
    education_post_id bigint NOT NULL
);

--
-- Name: education_post_attachment; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.education_post_attachment (
    file_key character varying(30) NOT NULL,
    file_type character varying(10),
    file_original_name character varying(200),
    education_post_attachment_id bigint NOT NULL,
    education_post_id bigint,
    edu_id character varying(20)
);

--
-- Name: education_post_attachment_education_post_attachment_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.education_post_attachment ALTER COLUMN education_post_attachment_id ADD GENERATED ALWAYS AS IDENTITY (
    SEQUENCE NAME salesforce2.education_post_attachment_education_post_attachment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: education_post_education_post_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.education_post ALTER COLUMN education_post_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME salesforce2.education_post_education_post_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: education_view_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.education_view_history (
    viewed_at timestamp without time zone NOT NULL,
    education_view_history_id bigint NOT NULL,
    education_post_id bigint,
    employee_id bigint,
    edu_id character varying(20),
    emp_code character varying(40)
);

--
-- Name: education_view_history_education_view_history_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.education_view_history ALTER COLUMN education_view_history_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME salesforce2.education_view_history_education_view_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: employee; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.employee (
    employee_id bigint NOT NULL,
    sfid character varying(18),
    employee_code character varying(100),
    name character varying(80),
    birth_date character varying(10),
    status character varying(40),
    app_login_active boolean,
    app_authority character varying(255),
    org_name character varying(100),
    cost_center_code character varying(10),
    work_phone character varying(255),
    phone character varying(40),
    home_phone character varying(255),
    start_date date,
    agreement_flag boolean,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    professional_promotion_team character varying(50),
    sex character varying(10),
    end_date date,
    jikchak character varying(100),
    jikwee character varying(40),
    jikgub character varying(40),
    work_type character varying(40),
    job_code character varying(40),
    work_area character varying(100),
    jikjong character varying(40),
    appointment_date date,
    ord_detail_node character varying(255),
    crm_work_start_date date
);

--
-- Name: employee_admin; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.employee_admin (
    employee_code character varying(40) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: employee_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: employee_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.employee_id_seq OWNED BY salesforce2.employee.employee_id;

--
-- Name: employee_info; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.employee_info (
    employee_code character varying(40) NOT NULL,
    password character varying(200),
    password_change_required boolean,
    device_uuid character varying(200),
    fcm_token character varying(200),
    gps_consent boolean,
    gps_consent_date timestamp without time zone,
    last_agreement_number character varying(80),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: erp_order; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.erp_order (
    id bigint NOT NULL,
    sap_order_number character varying(20) NOT NULL,
    sap_account_code character varying(20),
    sap_account_name character varying(100),
    delivery_request_date character varying(8),
    order_date character varying(8),
    employee_code character varying(20),
    employee_name character varying(50),
    order_sales_amount double precision,
    order_channel character varying(10),
    order_channel_nm character varying(50),
    order_type character varying(10),
    order_type_nm character varying(50),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: erp_order_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.erp_order_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: erp_order_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.erp_order_id_seq OWNED BY salesforce2.erp_order.id;

--
-- Name: erp_order_product; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.erp_order_product (
    id bigint NOT NULL,
    erp_order_id bigint NOT NULL,
    sap_order_number character varying(20) NOT NULL,
    line_number character varying(10) NOT NULL,
    external_key character varying(50) NOT NULL,
    product_code character varying(20),
    product_name character varying(100),
    order_quantity double precision,
    unit character varying(10),
    confirm_quantity_box double precision,
    confirm_quantity double precision,
    confirm_unit character varying(10),
    default_reason character varying(100),
    line_item_status character varying(20),
    delivery_status character varying(10),
    shipping_driver_name character varying(50),
    shipping_vehicle character varying(20),
    shipping_driver_phone character varying(20),
    shipping_schedule_time character varying(20),
    shipping_complete_time character varying(20),
    shipping_quantity_box double precision,
    shipping_quantity double precision,
    order_sales_line_amount double precision,
    shipping_amount double precision,
    plant character varying(10),
    plant_nm character varying(50),
    release_quantity double precision,
    release_amount double precision,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: erp_order_product_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.erp_order_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: erp_order_product_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.erp_order_product_id_seq OWNED BY salesforce2.erp_order_product.id;

--
-- Name: product_expiration; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.product_expiration (
    seq integer NOT NULL,
    account_name character varying(100),
    account_code character varying(100),
    employee_id bigint,
    product_name character varying(100),
    product_code character varying(100),
    expiration_date date,
    alarm_date date,
    description text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    product_expiration_id integer NOT NULL,
    account_id integer,
    product_id bigint,
    employee_sfid character varying(18)
);

--
-- Name: expirationdate__mng_seq_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.expirationdate__mng_seq_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: expirationdate__mng_seq_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.expirationdate__mng_seq_seq OWNED BY salesforce2.product_expiration.seq;

--
-- Name: holiday_master; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.holiday_master (
    id bigint NOT NULL,
    holiday_date date NOT NULL,
    name character varying(50) NOT NULL,
    type character varying(20) NOT NULL,
    year integer NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: holiday_master_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.holiday_master_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: holiday_master_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.holiday_master_id_seq OWNED BY salesforce2.holiday_master.id;

--
-- Name: hq_review; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.hq_review (
    hq_review_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    branch_code character varying(100),
    branch_name character varying(100),
    first_day_of_month date,
    evaluation_type character varying(255),
    abc_type_code character varying(255),
    hr_code character varying(255),
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: hq_review_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.hq_review_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: hq_review_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.hq_review_id_seq OWNED BY salesforce2.hq_review.hq_review_id;

--
-- Name: inspection_theme; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.inspection_theme (
    inspection_theme_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    title character varying(250),
    start_date date,
    end_date date,
    department character varying(100),
    branch_code character varying(30),
    public_flag boolean,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: inspection_theme_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.inspection_theme_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: inspection_theme_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.inspection_theme_id_seq OWNED BY salesforce2.inspection_theme.inspection_theme_id;

--
-- Name: login_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.login_history (
    employee_code character varying(80) NOT NULL,
    login_at timestamp without time zone NOT NULL,
    login_history_id bigint NOT NULL
);

--
-- Name: login_history_login_history_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.login_history ALTER COLUMN login_history_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME salesforce2.login_history_login_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: monthly_female_employee_integration_schedule; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.monthly_female_employee_integration_schedule (
    monthly_female_employee_integration_schedule_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    external_key character varying(255),
    year character varying(255),
    month character varying(255),
    account_sfid character varying(18),
    employee_sfid character varying(18),
    cost_center_code character varying(40),
    working_category1 character varying(255),
    working_category3 character varying(255),
    working_category4 character varying(255),
    working_category5 character varying(255),
    emp_branch_name character varying(255),
    professional_promotion_team character varying(255),
    working_days_month numeric(14,4),
    number_of_inputs bigint,
    equivalent_number_of_working_days numeric(14,4),
    converted_headcount numeric(14,4),
    edi_pos bigint,
    this_month_amount bigint,
    account_converted_headcount numeric(14,4),
    employee_input_criteria_master_sfid character varying(18),
    is_deleted boolean,
    employee_id bigint,
    account_id integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: monthly_female_employee_integ_monthly_female_employee_integ_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.monthly_female_employee_integration_schedule ALTER COLUMN monthly_female_employee_integration_schedule_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME salesforce2.monthly_female_employee_integ_monthly_female_employee_integ_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: monthly_sales_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.monthly_sales_history (
    monthly_sales_history_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    account_external_key character varying(1300),
    account_branch_name character varying(1300),
    account_type character varying(1300),
    sales_year character varying(255),
    sales_month character varying(255),
    fm_year double precision,
    fm_month double precision,
    target_month_results double precision,
    last_month_results double precision,
    last_month_target_formula double precision,
    last_month_target_achieved_ratio double precision,
    ship_closing_amount double precision,
    abc_closing_amount1 double precision,
    abc_closing_amount2 double precision,
    abc_closing_amount3 double precision,
    ambient_purpose double precision,
    fridge_purpose double precision,
    is_deleted boolean,
    external_key character varying(30),
    rl_sales double precision,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_id integer
);

--
-- Name: monthly_sales_history_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.monthly_sales_history_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: monthly_sales_history_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.monthly_sales_history_id_seq OWNED BY salesforce2.monthly_sales_history.monthly_sales_history_id;

--
-- Name: notice; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.notice (
    notice_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    scope character varying(255),
    category character varying(255),
    contents text,
    edu_category character varying(255),
    branch character varying(255),
    branch_code character varying(255),
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_sfid character varying(18),
    employee_id bigint
);

--
-- Name: notice_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.notice_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: notice_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.notice_id_seq OWNED BY salesforce2.notice.notice_id;

--
-- Name: organization; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.organization (
    organization_id bigint NOT NULL,
    cc_cd2 character varying(100),
    org_cd2 character varying(100),
    org_nm2 character varying(100),
    cc_cd3 character varying(100),
    org_cd3 character varying(100),
    org_nm3 character varying(100),
    cc_cd4 character varying(100),
    org_cd4 character varying(100),
    org_nm4 character varying(100),
    cc_cd5 character varying(100),
    org_cd5 character varying(100),
    org_nm5 character varying(100),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: organization_organization_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.organization_organization_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: organization_organization_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.organization_organization_id_seq OWNED BY salesforce2.organization.organization_id;

--
-- Name: product; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.product (
    product_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    product_code character varying(100),
    product_type character varying(255),
    product_status character varying(255),
    storage_condition character varying(255),
    shelf_life character varying(30),
    shelf_life_unit character varying(40),
    shelf_life_full character varying(1300),
    category1 character varying(255),
    category2 character varying(255),
    category3 character varying(255),
    category_code1 character varying(100),
    category_code2 character varying(100),
    category_code3 character varying(100),
    unit character varying(40),
    ordering_unit character varying(40),
    conversion_quantity double precision,
    box_receiving_quantity double precision,
    standard_unit_price double precision,
    standard_price double precision,
    super_tax double precision,
    launch_date date,
    logistics_barcode character varying(100),
    taste_gift character varying(1),
    product_features character varying(255),
    selling_point character varying(255),
    purpose character varying(255),
    target_account_type character varying(255),
    allergen character varying(255),
    cross_contamination character varying(255),
    img_ref_path character varying(255),
    img_ref_path_front character varying(255),
    img_ref_path_back character varying(255),
    img_ref_path_txt character varying(255),
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: product_barcode; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.product_barcode (
    product_barcode_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    product_name character varying(255),
    barcode character varying(255),
    unit character varying(255),
    sort_order character varying(255),
    product_id bigint,
    custom_key character varying(255),
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    product_sfid character varying(18)
);

--
-- Name: product_barcode_product_barcode_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.product_barcode_product_barcode_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: product_barcode_product_barcode_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.product_barcode_product_barcode_id_seq OWNED BY salesforce2.product_barcode.product_barcode_id;

--
-- Name: product_expiration_product_expiration_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.product_expiration_product_expiration_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: product_expiration_product_expiration_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.product_expiration_product_expiration_id_seq OWNED BY salesforce2.product_expiration.product_expiration_id;

--
-- Name: product_favorite; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.product_favorite (
    employee_code character varying(80) NOT NULL,
    product_code character varying(80) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: product_product_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.product_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: product_product_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.product_product_id_seq OWNED BY salesforce2.product.product_id;

--
-- Name: product_sync_buffer; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.product_sync_buffer (
    product_sync_buffer_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    product_code character varying(100),
    product_type character varying(255),
    product_status character varying(255),
    storage_condition character varying(255),
    shelf_life character varying(30),
    shelf_life_unit character varying(40),
    category1 character varying(255),
    category2 character varying(255),
    category3 character varying(255),
    category_code1 character varying(100),
    category_code2 character varying(100),
    category_code3 character varying(100),
    unit character varying(40),
    ordering_unit character varying(40),
    conversion_quantity double precision,
    box_receiving_quantity double precision,
    standard_unit_price double precision,
    super_tax double precision,
    launch_date date,
    logistics_barcode character varying(100),
    taste_gift character varying(1),
    product_features character varying(255),
    selling_point character varying(255),
    purpose character varying(255),
    target_account_type character varying(255),
    allergen character varying(255),
    cross_contamination character varying(255),
    img_ref_path character varying(255),
    img_ref_path_front character varying(255),
    img_ref_path_back character varying(255),
    img_ref_path_txt character varying(255),
    update_flag boolean,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: product_sync_buffer_product_sync_buffer_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

ALTER TABLE salesforce2.product_sync_buffer ALTER COLUMN product_sync_buffer_id ADD GENERATED BY DEFAULT AS IDENTITY (
    SEQUENCE NAME salesforce2.product_sync_buffer_product_sync_buffer_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
);

--
-- Name: professional_promotion_team_history; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.professional_promotion_team_history (
    professional_promotion_team_history_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    old_value character varying(50),
    new_value character varying(50) NOT NULL,
    changed_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_sfid character varying(18)
);

--
-- Name: professional_promotion_team_h_professional_promotion_team_h_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.professional_promotion_team_h_professional_promotion_team_h_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: professional_promotion_team_h_professional_promotion_team_h_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.professional_promotion_team_h_professional_promotion_team_h_seq OWNED BY salesforce2.professional_promotion_team_history.professional_promotion_team_history_id;

--
-- Name: professional_promotion_team_master; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.professional_promotion_team_master (
    professional_promotion_team_master_id bigint NOT NULL,
    employee_id bigint NOT NULL,
    account_id integer NOT NULL,
    team_type character varying(50) NOT NULL,
    start_date date NOT NULL,
    end_date date,
    is_confirmed boolean DEFAULT false NOT NULL,
    branch_code character varying(20),
    branch_name character varying(50),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_sfid character varying(18),
    employee_number character varying(20)
);

--
-- Name: professional_promotion_team_m_professional_promotion_team_m_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.professional_promotion_team_m_professional_promotion_team_m_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: professional_promotion_team_m_professional_promotion_team_m_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.professional_promotion_team_m_professional_promotion_team_m_seq OWNED BY salesforce2.professional_promotion_team_master.professional_promotion_team_master_id;

--
-- Name: promotion; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.promotion (
    promotion_id bigint NOT NULL,
    promotion_number character varying(20) NOT NULL,
    promotion_name character varying(200),
    promotion_type_id bigint,
    account_id integer NOT NULL,
    start_date date NOT NULL,
    end_date date NOT NULL,
    primary_product_id bigint,
    other_product character varying(200),
    message character varying(255),
    stand_location character varying(200),
    target_amount bigint,
    actual_amount bigint,
    cost_center_code character varying(100),
    remark character varying(200),
    branch_name character varying(100),
    category character varying(50),
    product_type character varying(50),
    is_closed boolean DEFAULT false NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_sfid character varying(18),
    primary_product_sfid character varying(18),
    deprecated_acc_sfid character varying(18),
    account_code character varying(100),
    actual_amount_won bigint,
    product_code character varying(100),
    owner_sfid character varying(18),
    created_by_sfid character varying(18),
    last_modified_by_sfid character varying(18)
);

--
-- Name: promotion_employee; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.promotion_employee (
    promotion_employee_id bigint NOT NULL,
    promotion_id bigint NOT NULL,
    employee_id bigint,
    schedule_date date,
    work_status character varying(20),
    work_type1 character varying(100),
    work_type3 character varying(100),
    work_type4 character varying(100),
    professional_promotion_team character varying(100),
    team_member_schedule_id bigint,
    promo_close_by_tm boolean DEFAULT false NOT NULL,
    base_price bigint,
    daily_target_count integer,
    target_amount bigint,
    actual_amount bigint,
    primary_product_amount bigint,
    primary_sales_quantity integer,
    primary_sales_price bigint,
    other_sales_amount bigint,
    other_sales_quantity integer,
    s3_image_unique_key character varying(255),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    promotion_sfid character varying(18),
    team_member_schedule_sfid character varying(18)
);

--
-- Name: promotion_employee_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.promotion_employee_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: promotion_employee_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.promotion_employee_id_seq OWNED BY salesforce2.promotion_employee.promotion_employee_id;

--
-- Name: promotion_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.promotion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: promotion_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.promotion_id_seq OWNED BY salesforce2.promotion.promotion_id;

--
-- Name: promotion_type; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.promotion_type (
    id bigint NOT NULL,
    name character varying(50) NOT NULL,
    display_order integer NOT NULL,
    is_active boolean DEFAULT true NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: promotion_type_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.promotion_type_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: promotion_type_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.promotion_type_id_seq OWNED BY salesforce2.promotion_type.id;

--
-- Name: push_message; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.push_message (
    push_message_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    message character varying(500),
    schedule_date timestamp without time zone,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: push_message_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.push_message_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: push_message_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.push_message_id_seq OWNED BY salesforce2.push_message.push_message_id;

--
-- Name: push_message_receiver; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.push_message_receiver (
    push_message_receiver_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    employee_id bigint,
    push_message_id integer,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    employee_sfid character varying(18),
    push_message_sfid character varying(18)
);

--
-- Name: push_message_receiver_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.push_message_receiver_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: push_message_receiver_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.push_message_receiver_id_seq OWNED BY salesforce2.push_message_receiver.push_message_receiver_id;

--
-- Name: safety_check_item; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.safety_check_item (
    safety_check_item_id bigint NOT NULL,
    question_num integer DEFAULT 0 NOT NULL,
    seq_num integer DEFAULT 0 NOT NULL,
    contents character varying(500) NOT NULL,
    use_yn character varying(1) DEFAULT 'Y'::character varying,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: safety_check_item_safety_check_item_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.safety_check_item_safety_check_item_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: safety_check_item_safety_check_item_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.safety_check_item_safety_check_item_id_seq OWNED BY salesforce2.safety_check_item.safety_check_item_id;

--
-- Name: safety_check_submission; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.safety_check_submission (
    master_id character varying(18),
    employee_id bigint,
    working_date date,
    start_time timestamp without time zone,
    complete_time timestamp without time zone,
    yes_check_count integer,
    no_check_count integer,
    equipment1 character varying(10),
    equipment2 character varying(10),
    equipment3 character varying(10),
    equipment4 character varying(10),
    equipment5 character varying(10),
    equipment6 character varying(10),
    equipment7 character varying(10),
    equipment8 character varying(10),
    equipment9 character varying(10),
    precaution character varying(3000),
    precaution_check_count integer,
    traversal_flag character varying(255),
    event_master_id character varying(18),
    complete_work_yn character varying(18),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    safety_check_submission_id bigint NOT NULL,
    display_work_schedule_id bigint,
    team_member_schedule_id bigint,
    employee_sfid character varying(18),
    display_work_schedule_sfid character varying(18),
    team_member_schedule_sfid character varying(18)
);

--
-- Name: safety_check_submission_safety_check_submission_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.safety_check_submission_safety_check_submission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: safety_check_submission_safety_check_submission_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.safety_check_submission_safety_check_submission_id_seq OWNED BY salesforce2.safety_check_submission.safety_check_submission_id;

--
-- Name: sap_sync_log; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.sap_sync_log (
    id bigint NOT NULL,
    api_name character varying(100) NOT NULL,
    request_count integer NOT NULL,
    success_count integer NOT NULL,
    fail_count integer NOT NULL,
    error_detail text,
    duration_ms bigint NOT NULL,
    request_ip character varying(45),
    requested_at timestamp without time zone NOT NULL,
    completed_at timestamp without time zone NOT NULL
);

--
-- Name: sap_sync_log_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.sap_sync_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: sap_sync_log_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.sap_sync_log_id_seq OWNED BY salesforce2.sap_sync_log.id;

--
-- Name: staff_review; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.staff_review (
    staff_review_id integer NOT NULL,
    sfid character varying(18),
    name character varying(80),
    employee_sfid character varying(18),
    employee_name character varying(1300),
    employee_number character varying(1300),
    branch character varying(1300),
    branch_review_sfid character varying(18),
    cost_center_code character varying(1300),
    employee_total_score double precision,
    attendance_score double precision,
    instruction_disobedience_score double precision,
    priority_item_event_score double precision,
    display_event_goal_score double precision,
    account_partnership_score double precision,
    clothes_hygiene_score double precision,
    product_manage_callment_score double precision,
    education_evaluation_score double precision,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: staff_review_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.staff_review_id_seq
    AS integer
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: staff_review_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.staff_review_id_seq OWNED BY salesforce2.staff_review.staff_review_id;

--
-- Name: system_code_master; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.system_code_master (
    id bigint NOT NULL,
    company_code character varying(10) NOT NULL,
    group_code character varying(20) NOT NULL,
    detail_code character varying(20) NOT NULL,
    group_code_name character varying(100),
    detail_code_name character varying(100),
    seq character varying(10),
    external_key character varying(60) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: system_code_master_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.system_code_master_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: system_code_master_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.system_code_master_id_seq OWNED BY salesforce2.system_code_master.id;

--
-- Name: team_member_schedule; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.team_member_schedule (
    team_member_schedule_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(80),
    employee_id bigint,
    working_date date,
    working_type character varying(255),
    working_category1 character varying(255),
    working_category2 character varying(255),
    working_category3 character varying(255),
    working_category4 character varying(255),
    account_id integer,
    team_leader_id bigint,
    alt_holiday_id bigint,
    commute_log_id character varying(18),
    promotion_employee_id bigint,
    commute_report_datetime timestamp without time zone,
    id_field character varying(30),
    traversal_flag character varying(255),
    is_work_report character varying(1300),
    equipment1 character varying(10),
    equipment2 character varying(10),
    equipment3 character varying(10),
    equipment4 character varying(10),
    equipment5 character varying(10),
    equipment6 character varying(10),
    equipment7 character varying(10),
    equipment8 character varying(10),
    equipment9 character varying(10),
    equipment10 character varying(10),
    yes_chk_cnt double precision,
    no_chk_cnt double precision,
    precaution_chk double precision,
    precaution character varying(3000),
    start_time timestamp without time zone,
    complete_time timestamp without time zone,
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_sfid character varying(18),
    employee_sfid character varying(18),
    team_leader_sfid character varying(18),
    alt_holiday_sfid character varying(18),
    promotion_employee_sfid character varying(18)
);

--
-- Name: team_member_schedule_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.team_member_schedule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: team_member_schedule_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.team_member_schedule_id_seq OWNED BY salesforce2.team_member_schedule.team_member_schedule_id;

--
-- Name: tmp_claim; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_claim (
    tmp_claim_id bigint NOT NULL,
    sap_account_name character varying(80),
    sap_account_code character varying(80),
    product_name character varying(100),
    product_code character varying(80),
    expiration_date character varying(80),
    claim_type1 character varying(80),
    claim_type2 character varying(80),
    description text,
    quantity character varying(80),
    claim_image_file_name character varying(200),
    part_image_file_name character varying(200),
    amount character varying(80),
    purchase_method character varying(80),
    receipt_image_file_name character varying(200),
    request_type text,
    employee_code character varying(80),
    claim_type1_name character varying(80),
    claim_type2_name character varying(80),
    purchase_code character varying(80),
    claim_image_extension character varying(80),
    part_image_extension character varying(80),
    receipt_image_extension character varying(80),
    receipt_image_buffer text,
    part_image_buffer text,
    claim_image_buffer text,
    manufacturing_date character varying(80),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    account_id bigint,
    employee_id bigint,
    product_id bigint
);

--
-- Name: tmp_claim_code; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_claim_code (
    tmp_claim_code_id bigint NOT NULL,
    claim1_code character varying(80),
    claim1_name character varying(80),
    claim2_code character varying(80),
    claim2_name character varying(80),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_claim_code_tmp_claim_code_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_claim_code_tmp_claim_code_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_claim_code_tmp_claim_code_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_claim_code_tmp_claim_code_id_seq OWNED BY salesforce2.tmp_claim_code.tmp_claim_code_id;

--
-- Name: tmp_claim_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_claim_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_claim_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_claim_id_seq OWNED BY salesforce2.tmp_claim.tmp_claim_id;

--
-- Name: tmp_claimcode; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_claimcode (
    claim1_code character varying(80),
    claim1_name character varying(80),
    claim2_code character varying(80),
    claim2_name character varying(80),
    inst_date timestamp without time zone,
    upd_date timestamp without time zone
);

--
-- Name: tmp_onsite; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_onsite (
    tmp_onsite_id bigint NOT NULL,
    employee_code character varying(80),
    theme_code character varying(80),
    theme_name character varying(80),
    classification character varying(80),
    sap_account_name character varying(100),
    sap_account_code character varying(80),
    category character varying(100),
    activity_date character varying(80),
    description text,
    product_name character varying(80),
    product_code character varying(80),
    competitor_name character varying(80),
    competitor_activity text,
    sample_tast_flag character varying(1),
    competitor_product_name character varying(80),
    sample_taster_price character varying(80),
    activity_amount character varying(40),
    s3_image_key1 character varying(255),
    s3_image_key2 character varying(255),
    s3_image_file_name1 character varying(80),
    s3_image_file_size1 character varying(80),
    s3_image_file_name2 character varying(80),
    s3_image_file_size2 character varying(80),
    account_id bigint,
    employee_id bigint,
    product_id bigint,
    inspection_theme_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_onsite_tmp_onsite_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_onsite_tmp_onsite_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_onsite_tmp_onsite_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_onsite_tmp_onsite_id_seq OWNED BY salesforce2.tmp_onsite.tmp_onsite_id;

--
-- Name: tmp_order; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_order (
    tmp_order_id bigint NOT NULL,
    employee_code character varying(80),
    account_code character varying(80),
    order_date date,
    total_amount character varying(80),
    account_id bigint,
    employee_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_order_product; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_order_product (
    tmp_order_product_id bigint NOT NULL,
    employee_code character varying(80),
    product_code character varying(80),
    box_cnt character varying(80),
    ea_cnt character varying(80),
    total_cnt character varying(80),
    employee_id bigint,
    product_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_order_product_tmp_order_product_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_order_product_tmp_order_product_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_order_product_tmp_order_product_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_order_product_tmp_order_product_id_seq OWNED BY salesforce2.tmp_order_product.tmp_order_product_id;

--
-- Name: tmp_order_tmp_order_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_order_tmp_order_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_order_tmp_order_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_order_tmp_order_id_seq OWNED BY salesforce2.tmp_order.tmp_order_id;

--
-- Name: tmp_promotion; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_promotion (
    tmp_promotion_id bigint NOT NULL,
    employee_code character varying(80),
    promotion_type character varying(80),
    promotion_name character varying(100),
    promotion_product_name character varying(80),
    promotion_product_code character varying(80),
    base_price character varying(80),
    primary_quantity character varying(80),
    other_product character varying(80),
    other_quantity character varying(80),
    other_total_amount character varying(80),
    image_url character varying(200),
    heroku_promotion_id character varying(80),
    promotion_seq character varying(80),
    image_file_name character varying(80),
    other_change_product character varying(80),
    employee_id bigint,
    product_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_promotion_tmp_promotion_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_promotion_tmp_promotion_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_promotion_tmp_promotion_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_promotion_tmp_promotion_id_seq OWNED BY salesforce2.tmp_promotion.tmp_promotion_id;

--
-- Name: tmp_suggest; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.tmp_suggest (
    tmp_suggest_id bigint NOT NULL,
    category character varying(80),
    product_name character varying(80),
    product_code character varying(80),
    title character varying(100),
    description text,
    employee_code character varying(80),
    s3_image_url1 character varying(80),
    s3_image_url2 character varying(80),
    s3_image_file_name1 character varying(80),
    s3_image_file_name2 character varying(80),
    s3_image_file_size1 character varying(80),
    s3_image_file_size2 character varying(80),
    car_number text,
    account_code text,
    claim_list text,
    claim_date date,
    employee_id bigint,
    product_id bigint,
    account_id bigint,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL
);

--
-- Name: tmp_suggest_tmp_suggest_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.tmp_suggest_tmp_suggest_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: tmp_suggest_tmp_suggest_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.tmp_suggest_tmp_suggest_id_seq OWNED BY salesforce2.tmp_suggest.tmp_suggest_id;

--
-- Name: upload_file; Type: TABLE; Schema: salesforce2; Owner: -
--

CREATE TABLE salesforce2.upload_file (
    upload_file_id bigint NOT NULL,
    sfid character varying(18),
    name character varying(255),
    unique_key character varying(500),
    record_id character varying(18),
    size character varying(50),
    is_deleted boolean,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    parent_type character varying(30) NOT NULL,
    parent_id bigint
);

--
-- Name: upload_file_id_seq; Type: SEQUENCE; Schema: salesforce2; Owner: -
--

CREATE SEQUENCE salesforce2.upload_file_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

--
-- Name: upload_file_id_seq; Type: SEQUENCE OWNED BY; Schema: salesforce2; Owner: -
--

ALTER SEQUENCE salesforce2.upload_file_id_seq OWNED BY salesforce2.upload_file.upload_file_id;

--
-- Name: account account_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account ALTER COLUMN account_id SET DEFAULT nextval('salesforce2.account_account_id_seq'::regclass);

--
-- Name: account_category_master id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account_category_master ALTER COLUMN id SET DEFAULT nextval('salesforce2.account_category_master_id_seq'::regclass);

--
-- Name: agreement_history agreement_history_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_history ALTER COLUMN agreement_history_id SET DEFAULT nextval('salesforce2.agreement_history_id_seq'::regclass);

--
-- Name: agreement_word agreement_word_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_word ALTER COLUMN agreement_word_id SET DEFAULT nextval('salesforce2.agreement_word_id_seq'::regclass);

--
-- Name: alternative_holiday alternative_holiday_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.alternative_holiday ALTER COLUMN alternative_holiday_id SET DEFAULT nextval('salesforce2.alternative_holiday_id_seq'::regclass);

--
-- Name: appointment appointment_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.appointment ALTER COLUMN appointment_id SET DEFAULT nextval('salesforce2.appointment_appointment_id_seq'::regclass);

--
-- Name: attend_info id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attend_info ALTER COLUMN id SET DEFAULT nextval('salesforce2.attend_info_id_seq'::regclass);

--
-- Name: attendance_log attendance_log_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attendance_log ALTER COLUMN attendance_log_id SET DEFAULT nextval('salesforce2.attendance_log_attendance_log_id_seq'::regclass);

--
-- Name: claim claim_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim ALTER COLUMN claim_id SET DEFAULT nextval('salesforce2.claims_id_seq'::regclass);

--
-- Name: claim_categories id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_categories ALTER COLUMN id SET DEFAULT nextval('salesforce2.claim_categories_id_seq'::regclass);

--
-- Name: claim_photos id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_photos ALTER COLUMN id SET DEFAULT nextval('salesforce2.claim_photos_id_seq'::regclass);

--
-- Name: claim_subcategories id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_subcategories ALTER COLUMN id SET DEFAULT nextval('salesforce2.claim_subcategories_id_seq'::regclass);

--
-- Name: daily_sales_history id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.daily_sales_history ALTER COLUMN id SET DEFAULT nextval('salesforce2.daily_sales_history_id_seq'::regclass);

--
-- Name: display_work_schedule display_work_schedule_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.display_work_schedule ALTER COLUMN display_work_schedule_id SET DEFAULT nextval('salesforce2.display_work_schedule_id_seq'::regclass);

--
-- Name: employee employee_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.employee ALTER COLUMN employee_id SET DEFAULT nextval('salesforce2.employee_id_seq'::regclass);

--
-- Name: erp_order id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order ALTER COLUMN id SET DEFAULT nextval('salesforce2.erp_order_id_seq'::regclass);

--
-- Name: erp_order_product id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order_product ALTER COLUMN id SET DEFAULT nextval('salesforce2.erp_order_product_id_seq'::regclass);

--
-- Name: holiday_master id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.holiday_master ALTER COLUMN id SET DEFAULT nextval('salesforce2.holiday_master_id_seq'::regclass);

--
-- Name: hq_review hq_review_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.hq_review ALTER COLUMN hq_review_id SET DEFAULT nextval('salesforce2.hq_review_id_seq'::regclass);

--
-- Name: inspection_theme inspection_theme_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.inspection_theme ALTER COLUMN inspection_theme_id SET DEFAULT nextval('salesforce2.inspection_theme_id_seq'::regclass);

--
-- Name: monthly_sales_history monthly_sales_history_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_sales_history ALTER COLUMN monthly_sales_history_id SET DEFAULT nextval('salesforce2.monthly_sales_history_id_seq'::regclass);

--
-- Name: notice notice_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.notice ALTER COLUMN notice_id SET DEFAULT nextval('salesforce2.notice_id_seq'::regclass);

--
-- Name: organization organization_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.organization ALTER COLUMN organization_id SET DEFAULT nextval('salesforce2.organization_organization_id_seq'::regclass);

--
-- Name: product product_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product ALTER COLUMN product_id SET DEFAULT nextval('salesforce2.product_product_id_seq'::regclass);

--
-- Name: product_barcode product_barcode_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_barcode ALTER COLUMN product_barcode_id SET DEFAULT nextval('salesforce2.product_barcode_product_barcode_id_seq'::regclass);

--
-- Name: product_expiration seq; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_expiration ALTER COLUMN seq SET DEFAULT nextval('salesforce2.expirationdate__mng_seq_seq'::regclass);

--
-- Name: product_expiration product_expiration_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_expiration ALTER COLUMN product_expiration_id SET DEFAULT nextval('salesforce2.product_expiration_product_expiration_id_seq'::regclass);

--
-- Name: professional_promotion_team_history professional_promotion_team_history_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_history ALTER COLUMN professional_promotion_team_history_id SET DEFAULT nextval('salesforce2.professional_promotion_team_h_professional_promotion_team_h_seq'::regclass);

--
-- Name: professional_promotion_team_master professional_promotion_team_master_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master ALTER COLUMN professional_promotion_team_master_id SET DEFAULT nextval('salesforce2.professional_promotion_team_m_professional_promotion_team_m_seq'::regclass);

--
-- Name: promotion promotion_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion ALTER COLUMN promotion_id SET DEFAULT nextval('salesforce2.promotion_id_seq'::regclass);

--
-- Name: promotion_employee promotion_employee_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion_employee ALTER COLUMN promotion_employee_id SET DEFAULT nextval('salesforce2.promotion_employee_id_seq'::regclass);

--
-- Name: promotion_type id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion_type ALTER COLUMN id SET DEFAULT nextval('salesforce2.promotion_type_id_seq'::regclass);

--
-- Name: push_message push_message_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.push_message ALTER COLUMN push_message_id SET DEFAULT nextval('salesforce2.push_message_id_seq'::regclass);

--
-- Name: push_message_receiver push_message_receiver_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.push_message_receiver ALTER COLUMN push_message_receiver_id SET DEFAULT nextval('salesforce2.push_message_receiver_id_seq'::regclass);

--
-- Name: safety_check_item safety_check_item_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_item ALTER COLUMN safety_check_item_id SET DEFAULT nextval('salesforce2.safety_check_item_safety_check_item_id_seq'::regclass);

--
-- Name: safety_check_submission safety_check_submission_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission ALTER COLUMN safety_check_submission_id SET DEFAULT nextval('salesforce2.safety_check_submission_safety_check_submission_id_seq'::regclass);

--
-- Name: sap_sync_log id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.sap_sync_log ALTER COLUMN id SET DEFAULT nextval('salesforce2.sap_sync_log_id_seq'::regclass);

--
-- Name: staff_review staff_review_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.staff_review ALTER COLUMN staff_review_id SET DEFAULT nextval('salesforce2.staff_review_id_seq'::regclass);

--
-- Name: system_code_master id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.system_code_master ALTER COLUMN id SET DEFAULT nextval('salesforce2.system_code_master_id_seq'::regclass);

--
-- Name: team_member_schedule team_member_schedule_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.team_member_schedule ALTER COLUMN team_member_schedule_id SET DEFAULT nextval('salesforce2.team_member_schedule_id_seq'::regclass);

--
-- Name: tmp_claim tmp_claim_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_claim ALTER COLUMN tmp_claim_id SET DEFAULT nextval('salesforce2.tmp_claim_id_seq'::regclass);

--
-- Name: tmp_claim_code tmp_claim_code_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_claim_code ALTER COLUMN tmp_claim_code_id SET DEFAULT nextval('salesforce2.tmp_claim_code_tmp_claim_code_id_seq'::regclass);

--
-- Name: tmp_onsite tmp_onsite_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_onsite ALTER COLUMN tmp_onsite_id SET DEFAULT nextval('salesforce2.tmp_onsite_tmp_onsite_id_seq'::regclass);

--
-- Name: tmp_order tmp_order_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_order ALTER COLUMN tmp_order_id SET DEFAULT nextval('salesforce2.tmp_order_tmp_order_id_seq'::regclass);

--
-- Name: tmp_order_product tmp_order_product_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_order_product ALTER COLUMN tmp_order_product_id SET DEFAULT nextval('salesforce2.tmp_order_product_tmp_order_product_id_seq'::regclass);

--
-- Name: tmp_promotion tmp_promotion_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_promotion ALTER COLUMN tmp_promotion_id SET DEFAULT nextval('salesforce2.tmp_promotion_tmp_promotion_id_seq'::regclass);

--
-- Name: tmp_suggest tmp_suggest_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_suggest ALTER COLUMN tmp_suggest_id SET DEFAULT nextval('salesforce2.tmp_suggest_tmp_suggest_id_seq'::regclass);

--
-- Name: upload_file upload_file_id; Type: DEFAULT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.upload_file ALTER COLUMN upload_file_id SET DEFAULT nextval('salesforce2.upload_file_id_seq'::regclass);

--
-- Name: account_category_master account_category_master_account_code_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account_category_master
    ADD CONSTRAINT account_category_master_account_code_key UNIQUE (account_code);

--
-- Name: account_category_master account_category_master_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account_category_master
    ADD CONSTRAINT account_category_master_pkey PRIMARY KEY (id);

--
-- Name: account account_external_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account
    ADD CONSTRAINT account_external_key_key UNIQUE (external_key);

--
-- Name: account account_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.account
    ADD CONSTRAINT account_pkey PRIMARY KEY (account_id);

--
-- Name: agreement_history agreement_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_history
    ADD CONSTRAINT agreement_history_pkey PRIMARY KEY (agreement_history_id);

--
-- Name: agreement_word agreement_word_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_word
    ADD CONSTRAINT agreement_word_pkey PRIMARY KEY (agreement_word_id);

--
-- Name: alternative_holiday alternative_holiday_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.alternative_holiday
    ADD CONSTRAINT alternative_holiday_pkey PRIMARY KEY (alternative_holiday_id);

--
-- Name: appointment appointment_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.appointment
    ADD CONSTRAINT appointment_pkey PRIMARY KEY (appointment_id);

--
-- Name: attend_info attend_info_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attend_info
    ADD CONSTRAINT attend_info_pkey PRIMARY KEY (id);

--
-- Name: attendance_log attendance_log_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attendance_log
    ADD CONSTRAINT attendance_log_pkey PRIMARY KEY (attendance_log_id);

--
-- Name: attendance_log attendance_log_sfid_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attendance_log
    ADD CONSTRAINT attendance_log_sfid_key UNIQUE (sfid);

--
-- Name: claim_categories claim_categories_name_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_categories
    ADD CONSTRAINT claim_categories_name_key UNIQUE (name);

--
-- Name: claim_categories claim_categories_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_categories
    ADD CONSTRAINT claim_categories_pkey PRIMARY KEY (id);

--
-- Name: claim_photos claim_photos_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_photos
    ADD CONSTRAINT claim_photos_pkey PRIMARY KEY (id);

--
-- Name: claim_purchase_methods claim_purchase_methods_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_purchase_methods
    ADD CONSTRAINT claim_purchase_methods_pkey PRIMARY KEY (code);

--
-- Name: claim_request_types claim_request_types_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_request_types
    ADD CONSTRAINT claim_request_types_pkey PRIMARY KEY (code);

--
-- Name: claim_subcategories claim_subcategories_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_subcategories
    ADD CONSTRAINT claim_subcategories_pkey PRIMARY KEY (id);

--
-- Name: claim claims_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim
    ADD CONSTRAINT claims_pkey PRIMARY KEY (claim_id);

--
-- Name: daily_sales_history daily_sales_history_external_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.daily_sales_history
    ADD CONSTRAINT daily_sales_history_external_key_key UNIQUE (external_key);

--
-- Name: daily_sales_history daily_sales_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.daily_sales_history
    ADD CONSTRAINT daily_sales_history_pkey PRIMARY KEY (id);

--
-- Name: device_version device_version_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.device_version
    ADD CONSTRAINT device_version_pkey PRIMARY KEY (version, device);

--
-- Name: display_work_schedule display_work_schedule_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.display_work_schedule
    ADD CONSTRAINT display_work_schedule_pkey PRIMARY KEY (display_work_schedule_id);

--
-- Name: education_code education_code_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_code
    ADD CONSTRAINT education_code_pkey PRIMARY KEY (education_code_id);

--
-- Name: education_post_attachment education_post_attachment_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_post_attachment
    ADD CONSTRAINT education_post_attachment_pkey PRIMARY KEY (education_post_attachment_id);

--
-- Name: education_post education_post_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_post
    ADD CONSTRAINT education_post_pkey PRIMARY KEY (education_post_id);

--
-- Name: education_view_history education_view_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_view_history
    ADD CONSTRAINT education_view_history_pkey PRIMARY KEY (education_view_history_id);

--
-- Name: employee_admin employee_admin_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.employee_admin
    ADD CONSTRAINT employee_admin_pkey PRIMARY KEY (employee_code);

--
-- Name: employee employee_employee_number_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.employee
    ADD CONSTRAINT employee_employee_number_key UNIQUE (employee_code);

--
-- Name: employee_info employee_info_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.employee_info
    ADD CONSTRAINT employee_info_pkey PRIMARY KEY (employee_code);

--
-- Name: employee employee_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.employee
    ADD CONSTRAINT employee_pkey PRIMARY KEY (employee_id);

--
-- Name: erp_order erp_order_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order
    ADD CONSTRAINT erp_order_pkey PRIMARY KEY (id);

--
-- Name: erp_order_product erp_order_product_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order_product
    ADD CONSTRAINT erp_order_product_pkey PRIMARY KEY (id);

--
-- Name: erp_order erp_order_sap_order_number_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order
    ADD CONSTRAINT erp_order_sap_order_number_key UNIQUE (sap_order_number);

--
-- Name: holiday_master holiday_master_holiday_date_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.holiday_master
    ADD CONSTRAINT holiday_master_holiday_date_key UNIQUE (holiday_date);

--
-- Name: holiday_master holiday_master_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.holiday_master
    ADD CONSTRAINT holiday_master_pkey PRIMARY KEY (id);

--
-- Name: hq_review hq_review_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.hq_review
    ADD CONSTRAINT hq_review_pkey PRIMARY KEY (hq_review_id);

--
-- Name: inspection_theme inspection_theme_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.inspection_theme
    ADD CONSTRAINT inspection_theme_pkey PRIMARY KEY (inspection_theme_id);

--
-- Name: login_history login_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.login_history
    ADD CONSTRAINT login_history_pkey PRIMARY KEY (login_history_id);

--
-- Name: monthly_female_employee_integration_schedule monthly_female_employee_integration_schedule_external_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_female_employee_integration_schedule
    ADD CONSTRAINT monthly_female_employee_integration_schedule_external_key_key UNIQUE (external_key);

--
-- Name: monthly_female_employee_integration_schedule monthly_female_employee_integration_schedule_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_female_employee_integration_schedule
    ADD CONSTRAINT monthly_female_employee_integration_schedule_pkey PRIMARY KEY (monthly_female_employee_integration_schedule_id);

--
-- Name: monthly_sales_history monthly_sales_history_external_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_sales_history
    ADD CONSTRAINT monthly_sales_history_external_key_key UNIQUE (external_key);

--
-- Name: monthly_sales_history monthly_sales_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_sales_history
    ADD CONSTRAINT monthly_sales_history_pkey PRIMARY KEY (monthly_sales_history_id);

--
-- Name: notice notice_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.notice
    ADD CONSTRAINT notice_pkey PRIMARY KEY (notice_id);

--
-- Name: organization organization_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.organization
    ADD CONSTRAINT organization_pkey PRIMARY KEY (organization_id);

--
-- Name: product_barcode product_barcode_custom_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_barcode
    ADD CONSTRAINT product_barcode_custom_key_key UNIQUE (custom_key);

--
-- Name: product_barcode product_barcode_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_barcode
    ADD CONSTRAINT product_barcode_pkey PRIMARY KEY (product_barcode_id);

--
-- Name: product_expiration product_expiration_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_expiration
    ADD CONSTRAINT product_expiration_pkey PRIMARY KEY (product_expiration_id);

--
-- Name: product_favorite product_favorites_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_favorite
    ADD CONSTRAINT product_favorites_pkey PRIMARY KEY (employee_code, product_code);

--
-- Name: product product_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product
    ADD CONSTRAINT product_pkey PRIMARY KEY (product_id);

--
-- Name: product product_product_code_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product
    ADD CONSTRAINT product_product_code_key UNIQUE (product_code);

--
-- Name: product_sync_buffer product_sync_buffer_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_sync_buffer
    ADD CONSTRAINT product_sync_buffer_pkey PRIMARY KEY (product_sync_buffer_id);

--
-- Name: professional_promotion_team_history professional_promotion_team_history_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_history
    ADD CONSTRAINT professional_promotion_team_history_pkey PRIMARY KEY (professional_promotion_team_history_id);

--
-- Name: professional_promotion_team_master professional_promotion_team_master_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master
    ADD CONSTRAINT professional_promotion_team_master_pkey PRIMARY KEY (professional_promotion_team_master_id);

--
-- Name: promotion_employee promotion_employee_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion_employee
    ADD CONSTRAINT promotion_employee_pkey PRIMARY KEY (promotion_employee_id);

--
-- Name: promotion promotion_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion
    ADD CONSTRAINT promotion_pkey PRIMARY KEY (promotion_id);

--
-- Name: promotion promotion_promotion_number_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion
    ADD CONSTRAINT promotion_promotion_number_key UNIQUE (promotion_number);

--
-- Name: promotion_type promotion_type_name_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion_type
    ADD CONSTRAINT promotion_type_name_key UNIQUE (name);

--
-- Name: promotion_type promotion_type_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.promotion_type
    ADD CONSTRAINT promotion_type_pkey PRIMARY KEY (id);

--
-- Name: push_message push_message_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.push_message
    ADD CONSTRAINT push_message_pkey PRIMARY KEY (push_message_id);

--
-- Name: push_message_receiver push_message_receiver_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.push_message_receiver
    ADD CONSTRAINT push_message_receiver_pkey PRIMARY KEY (push_message_receiver_id);

--
-- Name: safety_check_item safety_check_item_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_item
    ADD CONSTRAINT safety_check_item_pkey PRIMARY KEY (safety_check_item_id);

--
-- Name: safety_check_submission safety_check_submission_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission
    ADD CONSTRAINT safety_check_submission_pkey PRIMARY KEY (safety_check_submission_id);

--
-- Name: sap_sync_log sap_sync_log_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.sap_sync_log
    ADD CONSTRAINT sap_sync_log_pkey PRIMARY KEY (id);

--
-- Name: staff_review staff_review_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.staff_review
    ADD CONSTRAINT staff_review_pkey PRIMARY KEY (staff_review_id);

--
-- Name: system_code_master system_code_master_external_key_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.system_code_master
    ADD CONSTRAINT system_code_master_external_key_key UNIQUE (external_key);

--
-- Name: system_code_master system_code_master_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.system_code_master
    ADD CONSTRAINT system_code_master_pkey PRIMARY KEY (id);

--
-- Name: team_member_schedule team_member_schedule_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.team_member_schedule
    ADD CONSTRAINT team_member_schedule_pkey PRIMARY KEY (team_member_schedule_id);

--
-- Name: tmp_claim_code tmp_claim_code_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_claim_code
    ADD CONSTRAINT tmp_claim_code_pkey PRIMARY KEY (tmp_claim_code_id);

--
-- Name: tmp_claim tmp_claim_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_claim
    ADD CONSTRAINT tmp_claim_pkey PRIMARY KEY (tmp_claim_id);

--
-- Name: tmp_onsite tmp_onsite_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_onsite
    ADD CONSTRAINT tmp_onsite_pkey PRIMARY KEY (tmp_onsite_id);

--
-- Name: tmp_order tmp_order_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_order
    ADD CONSTRAINT tmp_order_pkey PRIMARY KEY (tmp_order_id);

--
-- Name: tmp_order_product tmp_order_product_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_order_product
    ADD CONSTRAINT tmp_order_product_pkey PRIMARY KEY (tmp_order_product_id);

--
-- Name: tmp_promotion tmp_promotion_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_promotion
    ADD CONSTRAINT tmp_promotion_pkey PRIMARY KEY (tmp_promotion_id);

--
-- Name: tmp_suggest tmp_suggest_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.tmp_suggest
    ADD CONSTRAINT tmp_suggest_pkey PRIMARY KEY (tmp_suggest_id);

--
-- Name: upload_file upload_file_pkey; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.upload_file
    ADD CONSTRAINT upload_file_pkey PRIMARY KEY (upload_file_id);

--
-- Name: education_post_attachment uq_attachment_post_file_key; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_post_attachment
    ADD CONSTRAINT uq_attachment_post_file_key UNIQUE (education_post_id, file_key);

--
-- Name: education_code uq_education_code_edu_code; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_code
    ADD CONSTRAINT uq_education_code_edu_code UNIQUE (edu_code);

--
-- Name: safety_check_submission uq_safety_check_employee_date_schedule; Type: CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission
    ADD CONSTRAINT uq_safety_check_employee_date_schedule UNIQUE (employee_id, working_date, display_work_schedule_id);

--
-- Name: idx_appointment_date; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_appointment_date ON salesforce2.appointment USING btree (appoint_date);

--
-- Name: idx_appointment_employee; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_appointment_employee ON salesforce2.appointment USING btree (employee_code);

--
-- Name: idx_attend_info_employee; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_attend_info_employee ON salesforce2.attend_info USING btree (employee_code);

--
-- Name: idx_attend_info_start_date; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_attend_info_start_date ON salesforce2.attend_info USING btree (start_date);

--
-- Name: idx_claim_employee_created; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_claim_employee_created ON salesforce2.claim USING btree (employee_id, created_at);

--
-- Name: idx_claim_store; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_claim_store ON salesforce2.claim USING btree (store_id);

--
-- Name: idx_education_view_history_employee; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_education_view_history_employee ON salesforce2.education_view_history USING btree (employee_id);

--
-- Name: idx_education_view_history_post; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_education_view_history_post ON salesforce2.education_view_history USING btree (education_post_id);

--
-- Name: idx_monthly_sales_history_account_id; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_monthly_sales_history_account_id ON salesforce2.monthly_sales_history USING btree (account_id);

--
-- Name: idx_ppth_employee_id; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_ppth_employee_id ON salesforce2.professional_promotion_team_history USING btree (employee_id);

--
-- Name: idx_pptm_account_id; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_pptm_account_id ON salesforce2.professional_promotion_team_master USING btree (account_id);

--
-- Name: idx_pptm_employee_id; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_pptm_employee_id ON salesforce2.professional_promotion_team_master USING btree (employee_id);

--
-- Name: idx_pptm_start_end_date; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_pptm_start_end_date ON salesforce2.professional_promotion_team_master USING btree (start_date, end_date);

--
-- Name: idx_pptm_team_type; Type: INDEX; Schema: salesforce2; Owner: -
--

CREATE INDEX idx_pptm_team_type ON salesforce2.professional_promotion_team_master USING btree (team_type);

--
-- Name: claim_photos claim_photos_claim_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_photos
    ADD CONSTRAINT claim_photos_claim_id_fkey FOREIGN KEY (claim_id) REFERENCES salesforce2.claim(claim_id);

--
-- Name: claim_subcategories claim_subcategories_category_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim_subcategories
    ADD CONSTRAINT claim_subcategories_category_id_fkey FOREIGN KEY (category_id) REFERENCES salesforce2.claim_categories(id);

--
-- Name: claim claims_category_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim
    ADD CONSTRAINT claims_category_id_fkey FOREIGN KEY (category_id) REFERENCES salesforce2.claim_categories(id);

--
-- Name: claim claims_employee_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim
    ADD CONSTRAINT claims_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: claim claims_store_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim
    ADD CONSTRAINT claims_store_id_fkey FOREIGN KEY (store_id) REFERENCES salesforce2.account(account_id);

--
-- Name: claim claims_subcategory_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.claim
    ADD CONSTRAINT claims_subcategory_id_fkey FOREIGN KEY (subcategory_id) REFERENCES salesforce2.claim_subcategories(id);

--
-- Name: agreement_history fk_agreement_history_agreement_word; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_history
    ADD CONSTRAINT fk_agreement_history_agreement_word FOREIGN KEY (agreement_word_id) REFERENCES salesforce2.agreement_word(agreement_word_id);

--
-- Name: agreement_history fk_agreement_history_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.agreement_history
    ADD CONSTRAINT fk_agreement_history_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: alternative_holiday fk_alternative_holiday_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.alternative_holiday
    ADD CONSTRAINT fk_alternative_holiday_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: education_post_attachment fk_attachment_education_post; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_post_attachment
    ADD CONSTRAINT fk_attachment_education_post FOREIGN KEY (education_post_id) REFERENCES salesforce2.education_post(education_post_id);

--
-- Name: attendance_log fk_attendance_log_account; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attendance_log
    ADD CONSTRAINT fk_attendance_log_account FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

--
-- Name: attendance_log fk_attendance_log_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.attendance_log
    ADD CONSTRAINT fk_attendance_log_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: education_post fk_education_post_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_post
    ADD CONSTRAINT fk_education_post_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: education_view_history fk_education_view_history_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_view_history
    ADD CONSTRAINT fk_education_view_history_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: education_view_history fk_education_view_history_post; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.education_view_history
    ADD CONSTRAINT fk_education_view_history_post FOREIGN KEY (education_post_id) REFERENCES salesforce2.education_post(education_post_id);

--
-- Name: erp_order_product fk_erp_order_product_order; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.erp_order_product
    ADD CONSTRAINT fk_erp_order_product_order FOREIGN KEY (erp_order_id) REFERENCES salesforce2.erp_order(id);

--
-- Name: login_history fk_login_history_employee_info; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.login_history
    ADD CONSTRAINT fk_login_history_employee_info FOREIGN KEY (employee_code) REFERENCES salesforce2.employee_info(employee_code) NOT VALID;

--
-- Name: monthly_sales_history fk_monthly_sales_history_account; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_sales_history
    ADD CONSTRAINT fk_monthly_sales_history_account FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

--
-- Name: professional_promotion_team_history fk_ppt_history_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_history
    ADD CONSTRAINT fk_ppt_history_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: professional_promotion_team_master fk_ppt_master_account; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_account FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

--
-- Name: professional_promotion_team_master fk_ppt_master_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master
    ADD CONSTRAINT fk_ppt_master_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: product_barcode fk_product_barcode_product; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.product_barcode
    ADD CONSTRAINT fk_product_barcode_product FOREIGN KEY (product_id) REFERENCES salesforce2.product(product_id);

--
-- Name: safety_check_submission fk_safety_check_submission_display_work_schedule; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_display_work_schedule FOREIGN KEY (display_work_schedule_id) REFERENCES salesforce2.display_work_schedule(display_work_schedule_id);

--
-- Name: safety_check_submission fk_safety_check_submission_employee; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_employee FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: safety_check_submission fk_safety_check_submission_team_member_schedule; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.safety_check_submission
    ADD CONSTRAINT fk_safety_check_submission_team_member_schedule FOREIGN KEY (team_member_schedule_id) REFERENCES salesforce2.team_member_schedule(team_member_schedule_id);

--
-- Name: monthly_female_employee_integration_schedule monthly_female_employee_integration_schedule_account_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_female_employee_integration_schedule
    ADD CONSTRAINT monthly_female_employee_integration_schedule_account_id_fkey FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

--
-- Name: monthly_female_employee_integration_schedule monthly_female_employee_integration_schedule_employee_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.monthly_female_employee_integration_schedule
    ADD CONSTRAINT monthly_female_employee_integration_schedule_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: professional_promotion_team_history professional_promotion_team_history_employee_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_history
    ADD CONSTRAINT professional_promotion_team_history_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
-- Name: professional_promotion_team_master professional_promotion_team_master_account_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master
    ADD CONSTRAINT professional_promotion_team_master_account_id_fkey FOREIGN KEY (account_id) REFERENCES salesforce2.account(account_id);

--
-- Name: professional_promotion_team_master professional_promotion_team_master_employee_id_fkey; Type: FK CONSTRAINT; Schema: salesforce2; Owner: -
--

ALTER TABLE ONLY salesforce2.professional_promotion_team_master
    ADD CONSTRAINT professional_promotion_team_master_employee_id_fkey FOREIGN KEY (employee_id) REFERENCES salesforce2.employee(employee_id);

--
--

