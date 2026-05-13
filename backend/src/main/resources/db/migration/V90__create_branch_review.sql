-- 스펙 #735: BranchReview 엔티티 신규 생성 + SF Object 정합 (Group A + Reference R-2 + Custom 42)

CREATE TABLE powersales.branch_review (
    branch_review_id                              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    sfid                                          VARCHAR(18) UNIQUE,
    name                                          VARCHAR(80),
    -- 식별 / 시점
    branch_name                                   VARCHAR(100),
    cost_center_code                              VARCHAR(100),
    first_day_of_month                            DATE,
    confirmed                                     BOOLEAN,
    -- 판촉 부문
    employee_evaluation_number                    DOUBLE PRECISION,
    sum_attendance                                DOUBLE PRECISION,
    sum_business_partner_ties                     DOUBLE PRECISION,
    sum_clothes_satellite                         DOUBLE PRECISION,
    sum_display_manage_event_goals                DOUBLE PRECISION,
    sum_educational_evaluation                    DOUBLE PRECISION,
    sum_instructions_default                      DOUBLE PRECISION,
    sum_priority_event_item_manage                DOUBLE PRECISION,
    sum_product_manage_callment                   DOUBLE PRECISION,
    attendance_average                            DOUBLE PRECISION,
    business_partner_ties_average                 DOUBLE PRECISION,
    clothes_satellite_average                     DOUBLE PRECISION,
    display_manage_event_goals_average            DOUBLE PRECISION,
    educational_evaluation_average                DOUBLE PRECISION,
    instructions_default_average                  DOUBLE PRECISION,
    priority_event_item_manage_average            DOUBLE PRECISION,
    product_manage_callment_average               DOUBLE PRECISION,
    sum_total_score_average                       DOUBLE PRECISION,
    sum_total_score                               DOUBLE PRECISION,
    -- 레이디 부문
    employee_evaluation_number_lady               DOUBLE PRECISION,
    sum_attendance_lady                           DOUBLE PRECISION,
    sum_business_partner_ties_lady                DOUBLE PRECISION,
    sum_clothes_satellite_lady                    DOUBLE PRECISION,
    sum_display_manage_event_goals_lady           DOUBLE PRECISION,
    sum_educational_evaluation_lady               DOUBLE PRECISION,
    sum_instructions_default_lady                 DOUBLE PRECISION,
    sum_priority_event_item_manage_lady           DOUBLE PRECISION,
    sum_product_manage_callment_lady              DOUBLE PRECISION,
    attendance_average_lady                       DOUBLE PRECISION,
    business_partner_ties_average_lady            DOUBLE PRECISION,
    clothes_satellite_average_lady                DOUBLE PRECISION,
    display_manage_event_goals_average_lady       DOUBLE PRECISION,
    educational_evaluation_average_lady           DOUBLE PRECISION,
    instructions_default_average_lady             DOUBLE PRECISION,
    priority_event_item_manage_average_lady       DOUBLE PRECISION,
    product_manage_callment_average_lady          DOUBLE PRECISION,
    sum_total_score_average_lady                  DOUBLE PRECISION,
    sum_total_score_lady                          DOUBLE PRECISION,
    -- Group A
    is_deleted                                    BOOLEAN,
    owner_sfid                                    VARCHAR(18),
    owner_id                                      BIGINT,
    created_by_sfid                               VARCHAR(18),
    created_by_id                                 BIGINT,
    last_modified_by_sfid                         VARCHAR(18),
    last_modified_by_id                           BIGINT,
    -- BaseEntity
    created_at                                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    updated_at                                    TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_branch_review_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_branch_review_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    CONSTRAINT fk_branch_review_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL
);

CREATE INDEX idx_branch_review_owner_id ON powersales.branch_review (owner_id);
CREATE INDEX idx_branch_review_created_by_id ON powersales.branch_review (created_by_id);
CREATE INDEX idx_branch_review_last_modified_by_id ON powersales.branch_review (last_modified_by_id);
CREATE INDEX idx_branch_review_first_day_of_month ON powersales.branch_review (first_day_of_month);
CREATE INDEX idx_branch_review_confirmed ON powersales.branch_review (confirmed);
