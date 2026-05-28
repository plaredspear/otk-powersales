-- promotion.promotion_number ("PM{00000000}" 8자리) 시퀀스.
-- AdminPromotionService.createPromotion() 의 nextval('promotion_number_seq') 참조.
CREATE SEQUENCE IF NOT EXISTS promotion_number_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 promotion_number 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (환경별 데이터 차이 흡수).
-- 데이터가 없으면 1 유지.
SELECT setval(
    'promotion_number_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(promotion_number, '\D', '', 'g'), '')::bigint)
           FROM promotion
          WHERE promotion_number ~ '^PM[0-9]+$'),
        0
    ) + 1,
    false
);
