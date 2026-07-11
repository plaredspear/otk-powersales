-- professional_promotion_team_history.name ("PH{0000000}" PH + 7자리) 채번 시퀀스.
-- SF AutoNumber(ProfessionalPromotionTeamHistory__c.Name, displayFormat PH{0000000}) 와 동일 번호 공간.
-- AdminPPTMasterService.updateEmployeeTeam() 의 이력 생성 채번에서 참조.
-- 마스터 시퀀스(professional_promotion_team_master_name_seq, PM) 와 동일 패턴의 별개 객체.
CREATE SEQUENCE IF NOT EXISTS professional_promotion_team_history_name_seq
    START WITH 1
    INCREMENT BY 1
    NO CYCLE;

-- 기존 name 의 숫자 부분 최대값 + 1 로 시퀀스 동기화 (마이그레이션 레코드 추월 보장).
-- 데이터가 없으면 1 유지. 채번 시에도 GREATEST(nextval, MAX+1) 로 시점 의존 없이 재보정한다.
SELECT setval(
    'professional_promotion_team_history_name_seq',
    COALESCE(
        (SELECT MAX(NULLIF(regexp_replace(name, '\D', '', 'g'), '')::bigint)
           FROM professional_promotion_team_history
          WHERE name ~ '^PH[0-9]+$'),
        0
    ) + 1,
    false
);
