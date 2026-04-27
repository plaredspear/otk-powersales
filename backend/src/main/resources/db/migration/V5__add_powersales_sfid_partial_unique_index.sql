-- 스펙 #543: powersales 의 account / product / product_barcode 의 sfid 컬럼에
-- partial UNIQUE 인덱스 (WHERE sfid IS NOT NULL) 를 추가한다.
--
-- 목적: hc-import.sh 가 sfid 기반 ON CONFLICT(sfid) DO UPDATE 로 멱등 import 가능하게 한다.
--
-- 안전장치: CREATE 직전에 각 테이블에서 동일 sfid 가 둘 이상 존재하는지 사전 검증한다.
-- 중복이 발견되면 RAISE EXCEPTION 으로 마이그레이션을 중단하고, 메시지에 테이블명과
-- 중복 sfid 예시 5개를 포함한다. (PL/pgSQL DO 블록 패턴은 본 마이그레이션이 첫 도입)
--
-- ON DELETE: 단일 컬럼 partial unique index 만 추가하며 FK 영향 없음
--   (information_schema.constraint_column_usage 검증: 0개 FK 가 sfid 를 참조).
-- JPA: @Column(unique=true) 로 partial 조건은 표현 불가하므로 엔티티는 변경하지 않는다.
--   ddl-auto = validate 환경에서 인덱스 추가는 검증 대상 아니므로 영향 없음.

DO $$
DECLARE
    dup_count BIGINT;
    dup_examples TEXT;
BEGIN
    -- account
    SELECT COUNT(*), string_agg(sfid, ', ')
      INTO dup_count, dup_examples
    FROM (
        SELECT sfid
        FROM powersales.account
        WHERE sfid IS NOT NULL
        GROUP BY sfid
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC, sfid
        LIMIT 5
    ) t;
    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'powersales.account 에 중복 sfid % 건 (예시 5건: %) — partial unique index 생성 불가',
            dup_count, COALESCE(dup_examples, '(none)');
    END IF;

    -- product
    SELECT COUNT(*), string_agg(sfid, ', ')
      INTO dup_count, dup_examples
    FROM (
        SELECT sfid
        FROM powersales.product
        WHERE sfid IS NOT NULL
        GROUP BY sfid
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC, sfid
        LIMIT 5
    ) t;
    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'powersales.product 에 중복 sfid % 건 (예시 5건: %) — partial unique index 생성 불가',
            dup_count, COALESCE(dup_examples, '(none)');
    END IF;

    -- product_barcode
    SELECT COUNT(*), string_agg(sfid, ', ')
      INTO dup_count, dup_examples
    FROM (
        SELECT sfid
        FROM powersales.product_barcode
        WHERE sfid IS NOT NULL
        GROUP BY sfid
        HAVING COUNT(*) > 1
        ORDER BY COUNT(*) DESC, sfid
        LIMIT 5
    ) t;
    IF dup_count > 0 THEN
        RAISE EXCEPTION
            'powersales.product_barcode 에 중복 sfid % 건 (예시 5건: %) — partial unique index 생성 불가',
            dup_count, COALESCE(dup_examples, '(none)');
    END IF;
END
$$;

CREATE UNIQUE INDEX idx_account_sfid_unique
    ON powersales.account (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_product_sfid_unique
    ON powersales.product (sfid)
    WHERE sfid IS NOT NULL;

CREATE UNIQUE INDEX idx_product_barcode_sfid_unique
    ON powersales.product_barcode (sfid)
    WHERE sfid IS NOT NULL;
