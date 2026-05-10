-- spec #650 — SAP 제품 표준가 매핑 destination 정합
-- spec #648 §7-A.7 audit 인계 + v0.4 옵션 A.4 (응답/Web 정합 후 컬럼 deprecation)
ALTER TABLE product DROP COLUMN standard_price;
