-- employee_info 의 PK 를 employee_code 에서 employee_id (= employee.employee_id) 공유 PK 로 전환.
-- Employee 와 employee_info 의 1:1 관계를 non-PK(employee_code) 조인에서 PK 공유 조인으로 정규화한다.
-- employee_code 는 PK 에서 제외하되 HC sync 자연 키로 일반 컬럼으로 잔류.
-- 단일 실행 전제 (PK/UNIQUE/FK ADD 는 멱등 가드 없음 — 기존 마이그레이션 컨벤션 동일).

-- 1. employee_id 컬럼 추가 (멱등)
ALTER TABLE powersales.employee_info ADD COLUMN IF NOT EXISTS employee_id bigint;

-- 2. 백필: employee_code 로 employee 조인해 employee_id 채움
UPDATE powersales.employee_info ei
SET    employee_id = e.employee_id
FROM   powersales.employee e
WHERE  e.employee_code = ei.employee_code
  AND  ei.employee_id IS NULL;

-- 3. 고아 row 가드: employee 와 매칭 안 된 employee_info 가 있으면 PK 전환 불가 → 명시적 실패.
DO $$
DECLARE orphan_cnt bigint;
BEGIN
    SELECT count(*) INTO orphan_cnt FROM powersales.employee_info WHERE employee_id IS NULL;
    IF orphan_cnt > 0 THEN
        RAISE EXCEPTION 'employee_info 에 employee 와 매칭되지 않는 row % 건 존재 (employee_code 미매칭). PK 전환 전 정리 필요.', orphan_cnt;
    END IF;
END $$;

-- 4. login_history → employee_info FK DROP (employee_info.employee_code 더 이상 PK 아님)
--    PK 전환(5)보다 선행해야 한다 — 이 FK 가 employee_info_pkey 인덱스에 의존하므로
--    먼저 떼지 않으면 employee_info_pkey DROP 이 "other objects depend on it" 으로 실패.
ALTER TABLE powersales.login_history DROP CONSTRAINT IF EXISTS fk_login_history_employee_info;

-- 5. PK 전환: employee_code PK DROP → employee_id PK ADD
ALTER TABLE powersales.employee_info ALTER COLUMN employee_id SET NOT NULL;
ALTER TABLE powersales.employee_info DROP CONSTRAINT IF EXISTS employee_info_pkey;
ALTER TABLE powersales.employee_info ADD  CONSTRAINT employee_info_pkey PRIMARY KEY (employee_id);

-- 6. employee_code 일반 컬럼 잔류 + UNIQUE (HC 자연 키 무결성 보존; nullable 유지)
ALTER TABLE powersales.employee_info ADD CONSTRAINT employee_info_employee_code_key UNIQUE (employee_code);

-- 7. employee_info.employee_id → employee.employee_id FK 추가 (공유 PK FK)
ALTER TABLE powersales.employee_info
    ADD CONSTRAINT fk_employee_info_employee
    FOREIGN KEY (employee_id) REFERENCES powersales.employee(employee_id);
