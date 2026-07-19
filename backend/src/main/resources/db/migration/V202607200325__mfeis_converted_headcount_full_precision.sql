-- MFEIS 환산 수치 전정밀도 저장 — SF 레거시 정합.
--
-- 배경: SF `MonthlyFemaleEmployeeIntegrationSchedule__c` 의 환산 필드는 field-meta 상 scale=4 로
-- 정의되어 있으나, describe API 의 `soapType = xsd:double` 이 말해주듯 실제 저장은 IEEE-754 double 이다.
-- scale 은 UI 입력/표시 제약일 뿐 저장 정밀도의 하드 제약이 아니며, Apex 가 계산값을 직접 대입하면
-- (`TeamMemberScheduleTriggerHandler`: `EquivalentNumberOfWorkingDays / workingDaysMonth`, setScale 없음)
-- double 원값이 그대로 적재된다. 운영 엑셀 실측에서 `0.058823529411764705` (= 1/17, 소수 18자리),
-- `0.4444444444444444` (= 4/9) 등 전정밀도 값이 확인되었다.
--
-- 신규는 `refreshIntegration` 이 저장 시점에 `setScale(4, HALF_UP)` 로 잘라 `0.0588` 로 적재했고,
-- 이 손실이 화면의 3자리 반올림 단계에서 증폭되어 근무형태별 인원현황 순회 합계가
-- SF 4.166 vs 신규 4.167 로 어긋났다 (2026-05 강북4지점 실측).
--
-- 조치: 컬럼 scale 을 double 유효자릿수(최대 17자리) 를 담을 수 있는 18 로 확대한다.
-- 환산인원/환산근무일수는 0~1 및 소규모 실수 범위라 정수부 12자리(30-18) 로 충분하다.
-- 기존 적재분은 이미 4자리로 잘려 있으므로, 본 마이그레이션 이후 재집계된 row 부터 전정밀도가 반영된다.
ALTER TABLE monthly_female_employee_integration_schedule
    ALTER COLUMN working_days_month TYPE NUMERIC(30, 18),
    ALTER COLUMN equivalent_number_of_working_days TYPE NUMERIC(30, 18),
    ALTER COLUMN converted_headcount TYPE NUMERIC(30, 18),
    ALTER COLUMN account_converted_headcount TYPE NUMERIC(30, 18);
