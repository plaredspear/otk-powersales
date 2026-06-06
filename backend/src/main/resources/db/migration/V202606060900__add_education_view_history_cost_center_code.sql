-- education_view_history 에 cost_center_code 컬럼 추가.
-- Heroku 원본 education_member_history.costcentercode__c (소속 지점 코드) 를 적재 보존하기 위함.
-- 기존엔 신규 엔티티에 매핑 컬럼이 없어 마이그레이션 시 drop 되던 값을 보존한다.

ALTER TABLE powersales.education_view_history
    ADD COLUMN IF NOT EXISTS cost_center_code character varying(40);
