-- branch_mapping 누락 행 보정 — SF BranchMapping__mdt 원본에 없는 행을 채운다.
--
-- 권장 실행 경로는 web 화면이다: 시스템 관리자 > SF 데이터 마이그레이션 (Stage 2)
--   > "지점 코드 맵핑 보정 적재" 카드 (POST /api/v1/admin/sf-migration/stage2/branch-mapping-supplement).
-- 화면 경로는 적재 직후 BranchCodeExpander 의 부팅 1회 메모리 캐시를 재빌드하므로 재기동이 필요 없다.
-- 본 SQL 은 DB 에 직접 넣어야 할 때(예: backend 배포 전 긴급 조치)의 동등 수단이며,
-- 이 경로로 넣은 경우에는 backend 재기동 또는 위 화면 실행으로 캐시를 갱신해야 반영된다.
--
-- 대상 (SoT: backend BranchMappingSupplement.ROWS):
--   E5694 (CVS전략팀) → 5691,5692,5693,5694
--
-- 근거
--   - CVS1팀/CVS2팀은 조직코드(E5692/E5693)와 SF 매핑 키가 일치해 정상 확장된다.
--   - CVS전략팀만 SF 매핑 키가 평문 '5694' 인데(customMetadata/BranchMapping.cvs.md-meta.xml)
--     조직 트리가 산출하는 실제 조직코드는 'E5694' 라, expand('E5694') 가 확장 없이 {E5694} 가 되어
--     CVS전략팀 조장의 거래처 · 여사원 일정 조회가 0건이 된다.
--   - 기존 '5694' 행은 지우지 않는다 — 전사 권한자용 34개 고정 지점 목록(DashboardBranchResolver)이
--     CVS전략팀을 '5694' 로 넘기므로, 키를 rename 하면 그 경로가 반대로 깨진다.
--
-- 멱등 — 이미 있으면 아무것도 하지 않는다 (기존 값 보존). Stage 1 BranchMapping 재적재와도 무충돌
-- (Stage 1 은 PK 충돌 시 DO NOTHING, 'E5694' 는 CSV 에 없어 건드리지 않는다).

INSERT INTO powersales.branch_mapping (branch_code, included_branch_codes, label)
VALUES ('E5694', '5691,5692,5693,5694', 'cvs전략')
ON CONFLICT (branch_code) DO NOTHING;

-- 확인
SELECT branch_code, included_branch_codes, label
FROM powersales.branch_mapping
WHERE branch_code IN ('5694', 'E5694', 'E5692', 'E5693')
ORDER BY branch_code;
