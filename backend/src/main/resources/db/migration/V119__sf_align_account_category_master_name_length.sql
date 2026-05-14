-- sf-meta-diff Q1 옵션 2 채택 — SF `AccountCategoryMaster__c.Name` length 80 정합.
-- SF prod 라이브 권위 (`prod/_raw/AccountCategoryMaster__c.json` Name.length=80) 와
-- 일치하도록 entity / DB 컬럼 length 100 → 80 좁힘.
--
-- 영향: 80자 초과 데이터가 있는 행이 있으면 ALTER 실패 — 운영 적용 전 분포 확인 필요.
-- 본 테이블은 거래처 유형 마스터 (단순 유형명: "할인점" / "체인" 등) 로 초과 가능성 사실상 0.

ALTER TABLE powersales.account_category_master
    ALTER COLUMN name TYPE VARCHAR(80);
