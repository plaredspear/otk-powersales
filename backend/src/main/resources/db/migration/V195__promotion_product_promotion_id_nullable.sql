-- promotion_product.promotion_id NOT NULL → NULL 허용
--
-- 배경:
--   Stage1 적재 시점에는 promotion_sfid 만 채워지고 promotion_id 는 NULL — Stage2-A FK resolve
--   가 sfid → id 변환을 수행하기 전 상태가 정상 흐름. V159 가 promotion_id 를 NOT NULL 로
--   정의했고 UNIQUE 제약도 함께 부여해 INSERT 자체가 실패 ("null value in column promotion_id
--   violates not-null constraint").
--
-- 해결:
--   promotion_id NULL 허용. Suggestion (V173) 의 account_id / employee_id 와 동일 컨벤션.
--   UNIQUE 제약은 유지 — PostgreSQL UNIQUE 는 NULL 을 동일값으로 취급하지 않아 NULL row 다수
--   허용 + Stage2 채움 후에는 행사 1건당 PromotionProduct 1건 invariant 정상 작동.
--
-- 운영 영향:
--   Stage1 적재 row 는 promotion_id NULL 상태로 들어옴. Stage2-A FK resolve 가 promotion_sfid
--   → promotion_id 자동 변환 후 정상화. service create 경로 (AdminPromotionService.upsertPromotionProduct)
--   는 항상 non-null promotionId 전달이라 운영 invariant 유지.

ALTER TABLE powersales.promotion_product
    ALTER COLUMN promotion_id DROP NOT NULL;
