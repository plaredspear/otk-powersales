-- 스펙 #733: TeamMemberSchedule SF Object 정합 (Group A + Reference R-2 + 길이 정합 + CostCenterCode 신규)

-- SF 정합 길이 확장 (절단 방지)
-- working_category5: 40 → 255 (SF picklist length 255)
-- second_work_type: 40 → 255 (SF length 255)
ALTER TABLE powersales.team_member_schedule
    ALTER COLUMN working_category5 TYPE VARCHAR(255),
    ALTER COLUMN second_work_type TYPE VARCHAR(255);

-- Q2 결정: CostCenterCode__c 신규 추가
ALTER TABLE powersales.team_member_schedule
    ADD COLUMN cost_center_code VARCHAR(255) NULL;

-- Group A R-2: Owner / CreatedBy / LastModifiedBy
ALTER TABLE powersales.team_member_schedule
    ADD COLUMN owner_sfid VARCHAR(18) NULL,
    ADD COLUMN owner_id BIGINT NULL,
    ADD COLUMN created_by_sfid VARCHAR(18) NULL,
    ADD COLUMN created_by_id BIGINT NULL,
    ADD COLUMN last_modified_by_sfid VARCHAR(18) NULL,
    ADD COLUMN last_modified_by_id BIGINT NULL;

ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT fk_team_member_schedule_owner
        FOREIGN KEY (owner_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_team_member_schedule_created_by
        FOREIGN KEY (created_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL,
    ADD CONSTRAINT fk_team_member_schedule_last_modified_by
        FOREIGN KEY (last_modified_by_id) REFERENCES powersales.employee (employee_id)
        ON DELETE SET NULL;

CREATE INDEX idx_team_member_schedule_owner_id ON powersales.team_member_schedule (owner_id);
CREATE INDEX idx_team_member_schedule_created_by_id ON powersales.team_member_schedule (created_by_id);
CREATE INDEX idx_team_member_schedule_last_modified_by_id ON powersales.team_member_schedule (last_modified_by_id);
