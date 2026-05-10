-- Account.business_number(=SF Account.Sic) 매핑 정정.
--
-- V32 에서 business_number(VARCHAR(20))가 SF Sic 매핑 컬럼으로 추가되었으나, 실제로는
-- V17 에서 추가된 business_license_number(사업자등록번호) 가 SF Account.Sic 와 의미상 동일.
-- business_number 컬럼을 제거하고, business_license_number 에 SF Sic 매핑을 부여한다.
-- business_license_number 길이를 80 으로 확장하여 SF 컬럼 길이 정합.

ALTER TABLE powersales.account
    DROP COLUMN business_number;

ALTER TABLE powersales.account
    ALTER COLUMN business_license_number TYPE VARCHAR(80);
