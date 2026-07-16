-- team_member_schedule.name ("TS{00000000}" 8자리) 시퀀스.
-- SF AutoNumber(DKRetail__TeamMemberSchedule__c.Name, displayFormat "TS{00000000}") 재현.
-- TeamMemberScheduleNameGenerator.next() 의 nextval('team_member_schedule_name_seq') 참조.
CREATE SEQUENCE IF NOT EXISTS team_member_schedule_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 name 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (환경별 데이터 차이 흡수).
-- 데이터가 없으면 1 유지. 채번 시점에도 GREATEST 보정을 하므로 여기서는 초기 동기화만 담당한다.
SELECT setval(
    'team_member_schedule_name_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
           FROM team_member_schedule
          WHERE name ~ '^TS[0-9]+$'),
        0
    ) + 1,
    false
);
