-- appointment.name ("AP{00000000}" 8자리) 시퀀스.
-- SF Appointment__c Name(AutoNumber AP{00000000}) 정합 — SAP 인바운드 적재 시 채번.
-- AppointmentRepository.getNextAppointmentNameSeq() 참조.
CREATE SEQUENCE IF NOT EXISTS appointment_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 name 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (환경별 데이터 차이 흡수).
-- 데이터가 없으면 1 유지.
SELECT setval(
    'appointment_name_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
           FROM appointment
          WHERE name ~ '^AP[0-9]+$'),
        0
    ) + 1,
    false
);
