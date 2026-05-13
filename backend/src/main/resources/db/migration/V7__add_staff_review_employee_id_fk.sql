-- 스펙 #546: powersales.staff_review 에 employee_id FK 컬럼 추가.
--
-- 배경: 기존 마이그레이션이 staff_review 의 employee_sfid 만 채우고 있어
-- JPA 가 Employee 와의 ManyToOne 관계를 표현할 수 없었다. 본 마이그레이션은 컬럼 +
-- FK 제약 + 인덱스를 추가하고, 마이그레이션 도구의 사후 UPDATE 가 employee_id 를
-- 채우도록 한다 (마스터 부재 시 NULL 잔존 허용).
-- 참고: 2026-05-13 HC 마이그레이션 정책 폐지 — SalesforceMigrationTool 단독 경로.

ALTER TABLE powersales.staff_review
    ADD COLUMN employee_id BIGINT NULL;

ALTER TABLE powersales.staff_review
    ADD CONSTRAINT fk_staff_review_employee
    FOREIGN KEY (employee_id) REFERENCES powersales.employee (employee_id);

CREATE INDEX idx_staff_review_employee_id
    ON powersales.staff_review (employee_id);
