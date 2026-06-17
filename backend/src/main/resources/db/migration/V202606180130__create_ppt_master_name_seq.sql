-- professional_promotion_team_master.name ("PM{0000000}" PM + 7자리) 채번 시퀀스.
-- SF AutoNumber(ProfessionalPromotionTeamMaster__c.Name, displayFormat PM{0000000}) 와 동일 번호 공간.
-- AdminPPTMasterService.createMaster() / confirmBulk() 의 채번에서 참조.
-- 기존 Promotion__c(P-{00000}) / promotion.promotion_number(PM 8자리) 와는 별개 객체.
CREATE SEQUENCE IF NOT EXISTS professional_promotion_team_master_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 name 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (마이그레이션 레코드 추월 보장).
-- 데이터가 없으면 1 유지. 채번 시에도 GREATEST(nextval, MAX+1) 로 시점 의존 없이 재보정한다.
SELECT setval(
    'professional_promotion_team_master_name_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
           FROM professional_promotion_team_master
          WHERE name ~ '^PM[0-9]+$'),
        0
    ) + 1,
    false
);
