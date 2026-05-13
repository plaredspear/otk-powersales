-- BranchReview Formula 38 컬럼 제거 — sandbox/README.md §6.7 정합 시정
-- 스펙 #735 (V90) 가 SF describe 의 calculated==true 필드 38건을 D 분류로 오인하여 entity + DB 컬럼 추가.
-- §6.7: Formula / Roll-Up Summary 는 DB 컬럼 추가 금지. SF API 동기화 대상 아님.
-- application 코드 어디에서도 본 컬럼 미참조 (grep 검증 완료).

ALTER TABLE powersales.branch_review
    -- 판촉 부문 (Roll-Up Summary 10 + Formula 9)
    DROP COLUMN employee_evaluation_number,
    DROP COLUMN sum_attendance,
    DROP COLUMN sum_business_partner_ties,
    DROP COLUMN sum_clothes_satellite,
    DROP COLUMN sum_display_manage_event_goals,
    DROP COLUMN sum_educational_evaluation,
    DROP COLUMN sum_instructions_default,
    DROP COLUMN sum_priority_event_item_manage,
    DROP COLUMN sum_product_manage_callment,
    DROP COLUMN sum_total_score,
    DROP COLUMN attendance_average,
    DROP COLUMN business_partner_ties_average,
    DROP COLUMN clothes_satellite_average,
    DROP COLUMN display_manage_event_goals_average,
    DROP COLUMN educational_evaluation_average,
    DROP COLUMN instructions_default_average,
    DROP COLUMN priority_event_item_manage_average,
    DROP COLUMN product_manage_callment_average,
    DROP COLUMN sum_total_score_average,
    -- 레이디 부문 (Roll-Up Summary 10 + Formula 9)
    DROP COLUMN employee_evaluation_number_lady,
    DROP COLUMN sum_attendance_lady,
    DROP COLUMN sum_business_partner_ties_lady,
    DROP COLUMN sum_clothes_satellite_lady,
    DROP COLUMN sum_display_manage_event_goals_lady,
    DROP COLUMN sum_educational_evaluation_lady,
    DROP COLUMN sum_instructions_default_lady,
    DROP COLUMN sum_priority_event_item_manage_lady,
    DROP COLUMN sum_product_manage_callment_lady,
    DROP COLUMN sum_total_score_lady,
    DROP COLUMN attendance_average_lady,
    DROP COLUMN business_partner_ties_average_lady,
    DROP COLUMN clothes_satellite_average_lady,
    DROP COLUMN display_manage_event_goals_average_lady,
    DROP COLUMN educational_evaluation_average_lady,
    DROP COLUMN instructions_default_average_lady,
    DROP COLUMN priority_event_item_manage_average_lady,
    DROP COLUMN product_manage_callment_average_lady,
    DROP COLUMN sum_total_score_average_lady;
