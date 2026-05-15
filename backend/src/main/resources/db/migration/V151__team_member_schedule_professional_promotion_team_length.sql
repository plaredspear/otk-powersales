-- TeamMemberSchedule.professional_promotion_team SF 정합 — 길이 100 → 255
--
-- SF prod 메타: DKRetail__TeamMemberSchedule__c.ProfessionalPromotionTeam__c (string, length=255)
-- 기존 entity length=100 → SF 길이 미만으로 절단 위험 → SF 길이로 정합.
--
-- 관련: V146 (TeamMemberSchedule SF 정합 1차), V147 (team_leader_sfid 길이 정합)

ALTER TABLE powersales.team_member_schedule
    ALTER COLUMN professional_promotion_team TYPE VARCHAR(255);
