-- safety_check_submission 의 (employee_id, working_date, display_work_schedule_id) unique 제약 제거.
--
-- 근거: 본 테이블은 Heroku 전용 (safetycheck__workschedule__member) 으로, 레거시 원본에는
-- PK/unique 제약이 없어 같은 직원이 같은 날 같은 일정으로 복수 안전점검 row 를 허용했다
-- (Heroku insertSafeChk 에 dedup/upsert 키 없음). 신규 스키마가 정규화 의도로 unique 제약을
-- 걸었으나, Stage 2 FK Resolve 가 display_work_schedule_id 를 sfid 로 채우는 순간 레거시의
-- 중복이 표면화되어 UPDATE 가 unique 위반으로 깨졌다.
-- 또한 진열점검(display_work_schedule)·순회점검(team_member_schedule) 은 상호배타라 제약 컬럼
-- 구성 자체도 레거시 의미와 어긋난다. 레거시 동등성을 우선해 제약을 제거한다. 신규 등록 시
-- 중복 방지가 필요하면 애플리케이션 레벨에서 처리한다.
ALTER TABLE powersales.safety_check_submission
    DROP CONSTRAINT IF EXISTS uq_safety_check_employee_date_schedule;
