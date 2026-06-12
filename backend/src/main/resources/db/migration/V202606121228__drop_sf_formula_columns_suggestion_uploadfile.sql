-- SF Formula 필드 정합 제거 (V96 §6.7 정책 잔여분 2건)
-- 정책: Formula 필드는 DB 컬럼 추가 X + @SFField 미부여. Formula 는 FK 조인 / 파생으로 해소.
-- 이번 2건은 Spec #740(V96) 일괄 제거 시 누락된 동일 성격 잔여분.

-- Suggestion.ProductCode__c (formula = DKRetail__ProductId__r.DKRetail__ProductCode__c)
-- → suggestion.product FK 조인 (suggestion.product?.productCode) 으로 해소.
ALTER TABLE suggestion DROP COLUMN IF EXISTS product_code;

-- UploadFile.Date__c (formula = CreatedDate 의 날짜부)
-- → upload_file.created_at 파생 (created_at.toLocalDate()) 으로 해소.
ALTER TABLE upload_file DROP COLUMN IF EXISTS file_date;
