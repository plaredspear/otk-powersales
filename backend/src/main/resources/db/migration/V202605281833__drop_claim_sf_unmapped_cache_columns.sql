-- Claim cache 컬럼 8개 제거 — SF DKRetail__Claim__c 에 정합 컬럼이 없거나 (SF Formula / SF 미존재) application 임시 캐시였던 컬럼.
-- 조회는 FK relation (claim.account.name / claim.product.name 등) 으로 전환.
--
-- SF 정합 분석:
--   store_name        : SF 미존재 (Account.Name lookup)
--   product_name      : SF 미존재 (Product.Name lookup)
--   product_code      : SF Formula DKRetail__ProductCode__c (= DKRetail__ProductId__r.DKRetail__ProductCode__c)
--   barcode           : SF Formula DKRetail__Barcode__c (= DKRetail__ProductId__r.DKRetail__Barcode__c)
--   phone             : SF Formula DKRetail__Phone__c (= DKRetail__EmployeeId__r.DKRetail__WorkPhone__c)
--   product_status    : SF Formula DKRetail__ProductStatus__c (= TEXT(DKRetail__ProductId__r.DKRetail__ProductStatus__c))
--   purchase_method_name : SF 미존재 (picklist code 의 label 변환 cache, enum.displayName 으로 즉시 산출)
--   request_type_name    : SF 미존재 (multipicklist code 의 label 변환 cache, enum.displayName 으로 즉시 산출)

ALTER TABLE claim
    DROP COLUMN IF EXISTS store_name,
    DROP COLUMN IF EXISTS product_name,
    DROP COLUMN IF EXISTS product_code,
    DROP COLUMN IF EXISTS barcode,
    DROP COLUMN IF EXISTS phone,
    DROP COLUMN IF EXISTS product_status,
    DROP COLUMN IF EXISTS purchase_method_name,
    DROP COLUMN IF EXISTS request_type_name;
