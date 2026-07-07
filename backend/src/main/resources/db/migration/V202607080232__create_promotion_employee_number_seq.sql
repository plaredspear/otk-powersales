-- promotion_employee.name ("PE{00000000}" 8자리, SF AutoNumber "행사사원#" 동등) 채번 시퀀스.
-- AdminPromotionEmployeeService.createEmployee() 의 getNextPromotionEmployeeNumberSeq() 참조.
CREATE SEQUENCE IF NOT EXISTS promotion_employee_number_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 name 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (환경별 데이터 차이 흡수).
-- SF 마이그레이션 데이터의 PE 번호를 흡수한다. 데이터가 없으면 1 유지.
SELECT setval(
    'promotion_employee_number_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
           FROM promotion_employee
          WHERE name ~ '^PE[0-9]+$'),
        0
    ) + 1,
    false
);
