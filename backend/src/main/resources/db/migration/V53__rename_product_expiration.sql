-- ProductExpiration: 테이블명 변경 + PK 컬럼 추가 (#421)

-- 1. 테이블명 변경
ALTER TABLE salesforce2.expirationdate__mng RENAME TO product_expiration;

-- 2. 기존 PK 제약조건 제거
ALTER TABLE salesforce2.product_expiration DROP CONSTRAINT expirationdate__mng_pkey;

-- 3. 신규 PK 컬럼 추가 (SERIAL = auto-increment)
ALTER TABLE salesforce2.product_expiration ADD COLUMN product_expiration_id SERIAL;

-- 4. 기존 seq 값 복사
UPDATE salesforce2.product_expiration SET product_expiration_id = seq;

-- 5. 시퀀스 현재값 동기화
SELECT setval('salesforce2.product_expiration_product_expiration_id_seq',
              (SELECT COALESCE(MAX(seq), 1) FROM salesforce2.product_expiration));

-- 6. 신규 PK 제약조건 추가
ALTER TABLE salesforce2.product_expiration ADD CONSTRAINT product_expiration_pkey PRIMARY KEY (product_expiration_id);
