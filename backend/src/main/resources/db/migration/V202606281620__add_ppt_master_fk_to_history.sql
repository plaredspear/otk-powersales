-- 전문행사조 이력에 원인 마스터 FK 추가.
-- 마스터를 특정할 수 있는 경로(생성/수정/확정/sync/만료)는 마스터 id 를 채우고,
-- 삭제로 인한 해제 경로는 마스터가 이미 제거되므로 NULL 로 둔다.
-- 마스터 삭제 시 이력의 FK 가 dangling 되지 않도록 ON DELETE SET NULL 로 보호한다.
ALTER TABLE professional_promotion_team_history
    ADD COLUMN professional_promotion_team_master_id BIGINT NULL;

ALTER TABLE professional_promotion_team_history
    ADD CONSTRAINT fk_ppt_history_master
        FOREIGN KEY (professional_promotion_team_master_id)
            REFERENCES professional_promotion_team_master (professional_promotion_team_master_id)
            ON DELETE SET NULL;

-- FK join(이력→마스터→거래처) + ON DELETE SET NULL 자식 탐색 커버.
-- 무인덱스 FK 는 마스터 삭제마다 이력 풀스캔을 유발하므로 데이터 규모와 무관하게 부여한다.
CREATE INDEX idx_ppt_history_master_id
    ON professional_promotion_team_history (professional_promotion_team_master_id);
