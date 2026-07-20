-- 외부 시스템 식별자를 RDP → OVIP 로 정정하면서 audit 테이블도 함께 rename.
-- 선행 V202607171759 는 이미 적용된 환경이 있어 checksum 보호상 수정하지 않고, 본 파일로 정정한다.
-- 테이블/PK 컬럼/인덱스 세 축 모두 rename 해야 OvipInboundAudit 엔티티 매핑과 정합한다.
ALTER TABLE rdp_inbound_audit RENAME TO ovip_inbound_audit;

ALTER TABLE ovip_inbound_audit RENAME COLUMN rdp_inbound_audit_id TO ovip_inbound_audit_id;

ALTER INDEX idx_rdp_inbound_audit_client_created RENAME TO idx_ovip_inbound_audit_client_created;

ALTER INDEX idx_rdp_inbound_audit_event_created RENAME TO idx_ovip_inbound_audit_event_created;

-- PK 제약은 인라인 PRIMARY KEY 선언이라 이름이 Postgres 자동 생성값(rdp_inbound_audit_pkey)이다.
-- 테이블 rename 이 제약명까지 바꾸지 않으므로 <테이블>_pkey 컨벤션 정합을 위해 별도 rename.
-- 자동 생성명에 의존하므로 존재할 때만 수행한다.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'rdp_inbound_audit_pkey') THEN
        ALTER TABLE ovip_inbound_audit RENAME CONSTRAINT rdp_inbound_audit_pkey TO ovip_inbound_audit_pkey;
    END IF;
END $$;
