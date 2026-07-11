-- 여사원일정(team_member_schedule) → 근태정보(attend_info) FK 신설.
-- SAP 인바운드(AttendInfoToScheduleConverter) 가 연차류 근태를 날짜별 연차 일정으로 펼칠 때
-- 원본 AttendInfo 를 가리키는 파생 링크 (1 AttendInfo → N schedule, FK 는 N 쪽에 위치).
-- SF 원본에 두 오브젝트 간 lookup 이 없어 sfid resolve 대상 아님 → attend_info_sfid 컬럼 없이 신규 인바운드 forward 전용.

ALTER TABLE powersales.team_member_schedule
    ADD COLUMN attend_info_id BIGINT NULL;

ALTER TABLE powersales.team_member_schedule
    ADD CONSTRAINT fk_team_member_schedule_attend_info
        FOREIGN KEY (attend_info_id) REFERENCES powersales.attend_info (attend_info_id)
        ON DELETE SET NULL;

CREATE INDEX idx_team_member_schedule_attend_info_id ON powersales.team_member_schedule (attend_info_id);
