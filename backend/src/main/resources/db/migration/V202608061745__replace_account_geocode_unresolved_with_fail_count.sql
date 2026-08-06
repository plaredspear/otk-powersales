-- 거래처 좌표변환(Naver Geocode) 실패 억제 방식을 boolean 플래그 → 실패 횟수 카운터로 교체.
--
-- 배경: geocode_unresolved 는 "주소로 좌표를 못 찾음" 을 1회 만에 영구 실패로 확정해 재조회 대상에서
-- 제외했다. 응답이 일시적으로 비어 오는 경우까지 단 한 번에 영구 배제되고, 몇 번 시도했는지 운영자가
-- 알 수 없다는 한계가 있었다.
--
-- geocode_fail_count = "주소로 좌표를 확정하지 못한(addresses 비었거나 x/y 없음) 누적 횟수".
-- 상한(GeocodeRetryPolicy.MAX_FAIL_COUNT) 이상이면 배치 재조회 + 출근등록 온디맨드 보강 양쪽에서 제외한다.
-- 호출 자체가 실패한 일시 오류(HTTP/네트워크/파싱)는 카운트하지 않는다 — Naver 장애 복구 후 자연 재시도.
-- 거래처 주소(address1) 변경 시 0 으로 초기화되어 재시도가 재개된다.
--
-- 백필: 기존 영구 실패 판정(TRUE) 은 상한값 3 으로 이관해 종전과 동일하게 제외 상태를 유지한다.
-- SF sync 대상이 아닌 신규 로컬 전용 컬럼 (Account 의 @SFField 미부여).
ALTER TABLE account ADD COLUMN geocode_fail_count INTEGER NOT NULL DEFAULT 0;

UPDATE account SET geocode_fail_count = 3 WHERE geocode_unresolved IS TRUE;

ALTER TABLE account DROP COLUMN geocode_unresolved;
