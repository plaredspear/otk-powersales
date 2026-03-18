-- #283 공휴일 마스터 테이블 생성 + 2026년 법정공휴일 시드 데이터

CREATE TABLE holiday_master (
    id BIGSERIAL PRIMARY KEY,
    holiday_date DATE NOT NULL UNIQUE,
    name VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    year INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_holiday_master_year ON holiday_master (year);

-- 2026년 대한민국 법정 공휴일 시드 데이터
INSERT INTO holiday_master (holiday_date, name, type, year) VALUES
    ('2026-01-01', '신정', '법정공휴일', 2026),
    ('2026-01-28', '설날 연휴', '법정공휴일', 2026),
    ('2026-01-29', '설날', '법정공휴일', 2026),
    ('2026-01-30', '설날 연휴', '법정공휴일', 2026),
    ('2026-03-01', '삼일절', '법정공휴일', 2026),
    ('2026-05-05', '어린이날', '법정공휴일', 2026),
    ('2026-05-24', '부처님오신날', '법정공휴일', 2026),
    ('2026-06-06', '현충일', '법정공휴일', 2026),
    ('2026-08-15', '광복절', '법정공휴일', 2026),
    ('2026-09-24', '추석 연휴', '법정공휴일', 2026),
    ('2026-09-25', '추석', '법정공휴일', 2026),
    ('2026-09-26', '추석 연휴', '법정공휴일', 2026),
    ('2026-10-03', '개천절', '법정공휴일', 2026),
    ('2026-10-09', '한글날', '법정공휴일', 2026),
    ('2026-12-25', '성탄절', '법정공휴일', 2026);
