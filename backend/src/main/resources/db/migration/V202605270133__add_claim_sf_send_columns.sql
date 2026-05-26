-- Spec #829: web admin 클레임 등록 dual-write 추적 컬럼 추가.
--
-- SF outbound push (backend → SF Apex /mobile/ClaimRegist) 의 상태 lifecycle 추적용:
--   - sent_at             : SF push 성공 시각 (status=SENT 전환 시점)
--   - send_fail_message   : SF push 실패 시 RESULT_MSG 박제
--   - send_attempt_count  : 재시도 횟수 추적 (초기 0, 재전송마다 +1)
--
-- SF 매핑 없음 — backend 내부 lifecycle 전용. ClaimStatus 의 SF_PENDING / SENT / SEND_FAILED 상태
-- 전이는 status 컬럼 (length=20) 그대로 사용.

ALTER TABLE powersales.claim
    ADD COLUMN sent_at            TIMESTAMP,
    ADD COLUMN send_fail_message  VARCHAR(1000),
    ADD COLUMN send_attempt_count INT NOT NULL DEFAULT 0;
