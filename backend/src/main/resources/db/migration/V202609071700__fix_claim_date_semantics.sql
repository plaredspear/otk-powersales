-- Claim 날짜 3종 오적재 정정 — 신규 등록분의 기한일이 발생일자(date) 컬럼에 들어가 있던 것을 분리.
--
-- 배경:
--   SF DKRetail__Claim__c 는 날짜를 3개로 분리한다 — ClaimDate__c(발생일자) / ExpirationDate__c(유통기한) /
--   ManufacturingDate__c(제조일자). 레거시 IF_REST_MOBILE_ClaimRegist 도 Input 3종을 각각 다른 컬럼에 넣고,
--   SF→DB 이관(Stage1Targets.CLAIM) 역시 같은 매핑으로 적재했다.
--   그런데 신규 등록 경로(Claim.forRegistration)는 "기한 종류 + 날짜 1개" 입력을 date(=ClaimDate) 컬럼에 넣고
--   expiration_date / manufacturing_date 를 비워 두었다. 그 결과 date 컬럼에 이관분(발생일자)과
--   신규 등록분(기한일)이라는 두 의미가 섞였고, 모바일 목록 조회가 이 컬럼을 기간 축으로 쓰면서
--   유통기한이 미래인 건이 조회되지 않았다.
--   SF 로 전송된 페이로드는 처음부터 3종 분리가 정확했으므로 SF 측 데이터는 정정 대상이 아니다.
--
-- 본 마이그레이션 (신규 등록분만 대상):
--   1. date 에 들어 있던 기한일을 date_type 에 따라 expiration_date / manufacturing_date 로 이동.
--   2. date 는 발생일자 의미로 되돌린다. 모바일 등록분은 SF 로 보낸 ClaimDate 가 등록일이라 등록일이 정답이고,
--      웹 등록분의 사용자 입력 발생일자는 애초에 저장된 적이 없어 복원 불가 → 등록일로 채운다.
--      created_at 은 TIMESTAMPTZ 이므로 KST 기준 날짜로 변환한다.
--
-- 대상 한정 조건:
--   - sfid IS NULL        : SF origin 이관 row 제외 (이관분은 이미 3종이 올바르게 적재됨)
--   - date_type IS NOT NULL : 신규 등록 경로만 date_type 을 채운다 (이관분은 NULL)
--   - 기한 컬럼 양쪽이 비어 있음 : 이미 분리 적재된 row(코드 수정 이후 등록분) 재처리 방지 → 재실행 안전

UPDATE powersales.claim
SET expiration_date = CASE WHEN date_type = 'EXPIRY_DATE' THEN claim.date ELSE expiration_date END,
    manufacturing_date = CASE WHEN date_type = 'MANUFACTURE_DATE' THEN claim.date ELSE manufacturing_date END,
    date = (created_at AT TIME ZONE 'Asia/Seoul')::date
WHERE sfid IS NULL
  AND date_type IS NOT NULL
  AND claim.date IS NOT NULL
  AND expiration_date IS NULL
  AND manufacturing_date IS NULL;
