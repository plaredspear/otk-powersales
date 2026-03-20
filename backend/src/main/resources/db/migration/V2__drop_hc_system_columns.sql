-- Heroku Connect 시스템 컬럼 (_hc_lastop, _hc_err) 제거
-- 더 이상 Heroku Connect를 사용하지 않으므로 불필요한 컬럼을 삭제한다

ALTER TABLE salesforce2.employee DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.monthly_sales_history DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.notice DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.inspection_theme DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.push_message DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.staff_review DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.hq_review DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.team_member_schedule DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.push_message_receiver DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
ALTER TABLE salesforce2.display_work_schedule DROP COLUMN IF EXISTS _hc_lastop, DROP COLUMN IF EXISTS _hc_err;
