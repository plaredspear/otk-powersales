-- 스펙 #545: 다중 인스턴스 스케줄 잡 보호용 ShedLock 표준 테이블 생성.
--
-- ShedLock 5.x JdbcTemplate Provider 가 요구하는 표준 컬럼 5개를 갖는다.
--   - name        : 락 식별자 (PK). @SchedulerLock(name = ...) 와 1:1 매핑
--   - lock_until  : 락 만료 시각. 이 시각 이전에는 다른 인스턴스가 락 획득 불가
--   - locked_at   : 락이 마지막으로 획득된 시각
--   - locked_by   : 락 보유 인스턴스 식별자 (호스트명 + 프로세스 + 랜덤). ShedLock 자동 생성
--
-- 추가 인덱스 없음 (PK 단일 조회만 발생).

CREATE TABLE powersales.shedlock (
    name        VARCHAR(64)                    NOT NULL,
    lock_until  TIMESTAMP(3) WITH TIME ZONE    NOT NULL,
    locked_at   TIMESTAMP(3) WITH TIME ZONE    NOT NULL,
    locked_by   VARCHAR(255)                   NOT NULL,
    PRIMARY KEY (name)
);
