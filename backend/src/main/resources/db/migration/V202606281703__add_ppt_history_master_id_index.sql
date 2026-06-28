-- 전문행사조 이력의 원인 마스터 FK(masterId) 인덱스.
-- FK join(이력→마스터→거래처) + ON DELETE SET NULL 자식 탐색을 커버한다.
-- 무인덱스 FK 는 마스터 삭제마다 이력 풀스캔을 유발하므로 데이터 규모와 무관하게 부여한다.
-- (V202606281620 컬럼/FK 추가가 이미 dev DB 에 적용된 뒤라, 인덱스는 별도 신규 마이그레이션으로 분리.)
CREATE INDEX idx_ppt_history_master_id
    ON professional_promotion_team_history (professional_promotion_team_master_id);
