-- team_member_schedule.professional_promotion_team partial index
--
-- 여사원 일정관리 화면의 "전문행사조 dropdown" 은 SF picklist 5값 고정이므로 enum 반환으로
-- DB DISTINCT 쿼리는 제거되었지만, 같은 컬럼이 filter 조건 (`WHERE professional_promotion_team IN (...)`)
-- 으로 다수 도메인 쿼리에 쓰인다 (TeamMemberScheduleRepositoryCustomImpl.professionalPromotionTeamIn).
-- 운영 누적 row 가 큰 테이블이라 본 컬럼 단독 인덱스가 없으면 filter 쿼리도 Seq Scan.
--
-- partial 조건은 실제 쿼리가 거의 항상 NOT NULL + 미삭제 row 만 탐색하므로 인덱스 크기 축소 +
-- Index Only Scan 가능성 확보.

CREATE INDEX IF NOT EXISTS idx_team_member_schedule_promotion_team_active
    ON powersales.team_member_schedule (professional_promotion_team)
    WHERE professional_promotion_team IS NOT NULL
      AND professional_promotion_team <> ''
      AND (is_deleted IS NULL OR is_deleted = false);
