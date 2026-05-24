-- sharing_rule_condition.logic_connector VARCHAR(10) → VARCHAR(255) SF 정합
--
-- 배경:
--   Stage1 SharingRule 적재 시 PSQLException:
--     "value too long for type character varying(10), column logic_connector: '1 AND (2 or 3)'"
--
--   SF sharingRules-meta.xml 의 <booleanFilter> 본문 element (예: "1 AND (2 OR 3)")
--   가 retrieve 결과로 logic_connector 컬럼에 적재되는데, 본 컬럼의 length=10
--   가 SF 실측 14자 ("1 AND (2 OR 3)") 를 수용하지 못함.
--
-- SF 한도:
--   SF 공식 명시 한도 없음 (메타 booleanFilter). ListView booleanFilter (1000자) 와
--   동일 패턴 추정. 안전 마진으로 VARCHAR(255) 채택 — 실측 14자 + 향후 확장 여유.
--
-- 영향:
--   - 본 컬럼은 자연 키 / FK / UNIQUE 제약 없음 → 길이 확장만으로 안전.
--   - 기존 row 의 값 길이 ≤ 10 이라 데이터 손실 없음.

ALTER TABLE powersales.sharing_rule_condition
    ALTER COLUMN logic_connector TYPE VARCHAR(255);
