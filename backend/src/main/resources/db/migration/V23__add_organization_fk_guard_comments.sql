-- Organization 테이블 FK 참조 방지 안전장치 (#384)
-- SAP DELETE_INSERT 동기화로 PK가 매번 변경되므로 FK 참조를 금지한다.

COMMENT ON TABLE organization IS 'SAP DELETE_INSERT 동기화 대상. PK(organization_id)가 매 동기화마다 변경되므로 FK 참조 금지. 조직 참조 시 cc_cd5 코드값을 사용할 것';

COMMENT ON COLUMN organization.organization_id IS 'AUTO INCREMENT PK - FK 참조 금지 (DELETE_INSERT 동기화로 값이 매번 변경됨)';
