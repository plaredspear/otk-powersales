-- 모바일 제안/물류클레임 등록 dual-write 추적 컬럼 추가.
--
-- SuggestionService.create() 가 DB INSERT 후 SF Apex REST(IF_REST_MOBILE_ProposalRegist,
-- backend → SF `/ProposalRegist`) 로 직접 전송하며, 그 전송 lifecycle 을 추적한다:
--   - sf_send_status        : PENDING(전송대기) / SENT(전송완료) / SEND_FAILED(전송실패)
--   - sf_sent_at            : SF push 성공 시각 (SENT 전환 시점)
--   - sf_send_fail_message  : SF push 실패 시 RESULT_MSG / 오류 요약 박제 (max 1000)
--   - sf_send_attempt_count : 전송 시도 횟수 (초기 0, 시도마다 +1)
--
-- SF 매핑 없음 — backend 내부 lifecycle 전용 (클레임 dual-write V202605270133 정합).
-- 기존 마이그레이션 적재(SF origin) row 는 sf_send_status=NULL 로 남아 본 경로 전송 대상이 아님을 표현.

ALTER TABLE powersales.suggestion
    ADD COLUMN sf_send_status        VARCHAR(20),
    ADD COLUMN sf_sent_at            TIMESTAMP,
    ADD COLUMN sf_send_fail_message  VARCHAR(1000),
    ADD COLUMN sf_send_attempt_count INT NOT NULL DEFAULT 0;
