-- 앱 아이콘 배지(미확인 푸시 건수) 카운터 컬럼 추가
--
-- iOS 는 APNs payload 의 `aps.badge` 가 "표시할 절대값" 이라 서버가 사용자별 누적 수를 알고 보내야 한다
-- (앱은 백그라운드/종료 상태에서 배지를 스스로 증가시킬 수 없다). Android 도 동일 값을
-- notification_count 로 실어 런처 배지 숫자를 맞춘다.
--
-- 공지 push 읽음 기록 테이블이 없으므로 "읽음" 이 아니라 "앱 확인" 기준 카운터로 둔다:
--   푸시 발송 시 대상 사용자마다 +1, 사용자가 앱을 포그라운드로 열면(배지 clear API) 0 으로 리셋,
--   로그아웃(토큰 해제) 시에도 0 으로 리셋해 다음 사용자에게 이전 배지가 남지 않게 한다.
--
-- employee_info 는 HC sync 테이블이지만 이 컬럼은 app_version_* 과 동일하게 백엔드 전용(HC 매핑 없음).

ALTER TABLE employee_info
    ADD COLUMN push_badge_count INTEGER NOT NULL DEFAULT 0;
