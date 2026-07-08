-- external_api_log 에 응답 데이터 건수 컬럼 추가.
-- SF outbound 응답 본문(JSON)에서 추출한 데이터 레코드 배열 크기를 기록한다.
-- SF 응답 형식이 아닌 호출(SAP / Naver / 배열을 회수하지 않는 SF 호출)은 NULL 로 남는다.
ALTER TABLE external_api_log ADD COLUMN response_count INTEGER;
