-- Appointment.appoint_date VARCHAR(8) yyyyMMdd → DATE 타입 정합
-- SF Appointment__c.AppointmentDate__c 의 SF `date` 타입과 PostgreSQL `DATE` 자연 대응 정렬.
-- (sf-meta-diff/Appointment__c.md §9 Q2 결정 — Entity LocalDate + DB DATE)

ALTER TABLE appointment
    ALTER COLUMN appoint_date TYPE DATE USING to_date(appoint_date, 'YYYYMMDD');
