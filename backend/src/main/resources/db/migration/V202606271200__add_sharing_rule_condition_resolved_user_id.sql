-- sharing_rule_condition 의 audit/owner field (CreatedById / LastModifiedById / OwnerId) condition 의
-- condition_value 는 SF user sfid (18자) 다. application 정책상 sfid 직접 매칭 금지 —
-- Stage2 FK Resolve 시점에 user.sfid lookup 으로 신규 User.id 를 미리 채워두고,
-- 런타임 SharingRulePolicyEvaluator 는 본 FK Long 만 비교한다 (sfid 런타임 resolve 제거).
ALTER TABLE powersales.sharing_rule_condition
    ADD COLUMN condition_resolved_user_id BIGINT
        REFERENCES powersales."user" (user_id);

COMMENT ON COLUMN powersales.sharing_rule_condition.condition_resolved_user_id IS
    'audit/owner field (CreatedById/LastModifiedById/OwnerId) condition 의 condition_value(SF user sfid) → 신규 User.id. Stage2 FK Resolve 가 채움. 비-audit field 또는 sfid 매칭 실패 시 NULL.';
