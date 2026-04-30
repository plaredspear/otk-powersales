-- Spec #565 P1-B
-- employee.sex (VARCHAR(10), 한글 "남"/"여" 저장) -> employee.gender (VARCHAR(10), enum name "MALE"/"FEMALE" 저장)
--
-- 1) 컬럼 rename: sex -> gender (타입/길이/nullable 유지)
ALTER TABLE employee RENAME COLUMN sex TO gender;

-- 2) 데이터 변환: "남" -> "MALE", "여" -> "FEMALE", 기타/null -> NULL (fail-safe 정규화)
UPDATE employee
SET gender = CASE
    WHEN gender = '남' THEN 'MALE'
    WHEN gender = '여' THEN 'FEMALE'
    ELSE NULL
END;
