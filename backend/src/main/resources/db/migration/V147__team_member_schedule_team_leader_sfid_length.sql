-- sf-meta-diff Q1 [grey] 옵션 (b) — `teamleadersfid__c` SF length=100 정합.
-- entity `team_leader_sfid` VARCHAR(18) → VARCHAR(100) 확장 (절단 위험 회피).
-- 실 운영값은 SF Id 18자 고정 추정. SF org 메타 length=18 좁힘 시 entity 도 동반 축소 예정.

ALTER TABLE powersales.team_member_schedule
    ALTER COLUMN team_leader_sfid TYPE VARCHAR(100);
