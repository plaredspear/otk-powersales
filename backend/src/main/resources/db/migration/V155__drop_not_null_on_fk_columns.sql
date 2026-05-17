-- =========================================================================
-- V155 — FK 컬럼 NOT NULL 제약 일괄 제거
-- =========================================================================
-- 배경:
--   SF 데이터 마이그레이션 (Spec #764) 의 Stage 1 raw INSERT 는 SF 원본
--   sfid (account_sfid 등) 만 채우고 FK id (account_id 등) 는 Stage 2 의
--   resolve 단계에서 채우는 구조. 일부 FK 컬럼이 NOT NULL 로 선언되어
--   Stage 1 raw INSERT 시점에 제약 위반 (예: promotion.account_id NOT NULL
--   위반) 으로 batch abort.
--
-- 결정:
--   현재 프로젝트에서 NOT NULL 인 모든 FK 컬럼을 일괄 nullable 로 전환.
--   FK 자체는 유지하되 NULL 허용. Stage 2 의 resolve 가 완료된 후의
--   실제 데이터는 NOT NULL 이지만 스키마 레벨 제약은 풀어둔다.
--
-- 대상 (7개 컬럼 / 6개 테이블):
--   promotion.account_id
--   order_request.employee_id, order_request.account_id
--   order_request_product.order_request_id
--   erp_order_product.erp_order_id
--   claim.employee_id, claim.account_id
--   claim_photos.claim_id
-- =========================================================================

BEGIN;

-- promotion
ALTER TABLE powersales.promotion         ALTER COLUMN account_id        DROP NOT NULL;

-- order_request
ALTER TABLE powersales.order_request     ALTER COLUMN employee_id       DROP NOT NULL;
ALTER TABLE powersales.order_request     ALTER COLUMN account_id        DROP NOT NULL;

-- order_request_product
ALTER TABLE powersales.order_request_product ALTER COLUMN order_request_id DROP NOT NULL;

-- erp_order_product
ALTER TABLE powersales.erp_order_product ALTER COLUMN erp_order_id      DROP NOT NULL;

-- claim
ALTER TABLE powersales.claim             ALTER COLUMN employee_id       DROP NOT NULL;
ALTER TABLE powersales.claim             ALTER COLUMN account_id        DROP NOT NULL;

-- claim_photos
ALTER TABLE powersales.claim_photos      ALTER COLUMN claim_id          DROP NOT NULL;

COMMIT;
