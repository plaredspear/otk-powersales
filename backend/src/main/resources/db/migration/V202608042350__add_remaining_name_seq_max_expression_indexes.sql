-- 채번 쿼리(MAX 보정) 전수 대응 — V202608042320 (team_member_schedule / promotion_employee) 의 나머지 6건.
--
-- 프로젝트의 모든 SF AutoNumber 재현 채번은 동일 패턴이다:
--   SELECT setval(seq, GREATEST(nextval(seq), COALESCE((SELECT MAX(<num-expr>) FROM <t>
--                                                        WHERE <col> ~ '^XX[0-9]+$'), 0) + 1) ...)
-- MAX 대상이 표현식이라 인덱스가 없으면 채번 1회마다 대상 테이블을 전건 스캔한다.
-- 채번 쿼리의 표현식/조건과 문자 그대로 동일한 표현식 부분 인덱스를 만들어 backward index scan
-- (ORDER BY expr DESC LIMIT 1) 으로 치환되게 한다.
--
-- 특히 아래 3경로는 **건별 채번을 루프에서 반복**하므로 스캔이 N 배로 증폭된다 (인덱스 부재 시 치명적):
--   - AppointmentInsertService        : SAP 인사발령 인바운드 — 행마다 getNextAppointmentNameSeq
--   - AdminPPTMasterService(벌크 업로드): item 마다 generateMasterName
--   - PPTMasterBatchService(매일 01:00): 사원마다 updateEmployeeTeam → generateHistoryName
-- 나머지(promotion / promotion_product / site_activity)는 요청당 1회지만 같은 전건 스캔이라 함께 해소한다.
--
-- 표현식은 두 계열이 있고, 각 채번 쿼리의 것을 그대로 따른다:
--   (a) NULLIF(regexp_replace(col,'\D','','g'),'')::bigint   — promotion / ppt_master / ppt_history / appointment
--   (b) SUBSTRING(col FROM 3)::bigint                        — promotion_product / site_activity
-- regexp_replace / NULLIF / SUBSTRING / text→bigint 캐스팅은 모두 IMMUTABLE 이라 인덱스 표현식으로 사용 가능.
--
-- 주의: CONCURRENTLY 미사용 (Flyway 트랜잭션 내 실행 불가 — 기존 인덱스 마이그레이션과 동일 방식).

-- (a) regexp_replace 계열
CREATE INDEX idx_promotion_number_seq_num
    ON powersales.promotion
       (((NULLIF(regexp_replace(promotion_number, '\D', '', 'g'), ''))::bigint))
    WHERE promotion_number ~ '^PM[0-9]+$';

CREATE INDEX idx_ppt_master_name_seq_num
    ON powersales.professional_promotion_team_master
       (((NULLIF(regexp_replace(name, '\D', '', 'g'), ''))::bigint))
    WHERE name ~ '^PM[0-9]+$';

CREATE INDEX idx_ppt_history_name_seq_num
    ON powersales.professional_promotion_team_history
       (((NULLIF(regexp_replace(name, '\D', '', 'g'), ''))::bigint))
    WHERE name ~ '^PH[0-9]+$';

CREATE INDEX idx_appointment_name_seq_num
    ON powersales.appointment
       (((NULLIF(regexp_replace(name, '\D', '', 'g'), ''))::bigint))
    WHERE name ~ '^AP[0-9]+$';

-- (b) SUBSTRING 계열
CREATE INDEX idx_promotion_product_name_seq_num
    ON powersales.promotion_product
       (((SUBSTRING(name FROM 3))::bigint))
    WHERE name ~ '^PS[0-9]+$';

CREATE INDEX idx_site_activity_name_seq_num
    ON powersales.site_activity
       (((SUBSTRING(name FROM 3))::bigint))
    WHERE name ~ '^SA[0-9]+$';
