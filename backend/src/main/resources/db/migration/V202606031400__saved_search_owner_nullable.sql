-- 저장된 검색 (SavedSearch) — owner_id nullable 화 (Spec #852 v1.3)
-- 시스템 부팅 시 생성하는 기본 공용(SHARED) 프리셋은 특정 소유자가 없으므로 owner_id = NULL 허용.
-- SF 공용 ListView 가 특정 소유자 없이 조직 공유였던 것과 정합.

ALTER TABLE saved_search ALTER COLUMN owner_id DROP NOT NULL;

-- 유니크 제약 재정의 — owner_id 가 NULL 인 시스템 프리셋도 (resource_key, scope, name) 으로 중복 차단.
-- PostgreSQL 기본은 NULL 을 distinct 로 취급해 중복 INSERT 가 가능하므로 NULLS NOT DISTINCT 로 교체.
ALTER TABLE saved_search DROP CONSTRAINT uk_saved_search_owner_name;
ALTER TABLE saved_search
    ADD CONSTRAINT uk_saved_search_owner_name
    UNIQUE NULLS NOT DISTINCT (resource_key, owner_id, scope, name);
