-- spec #801: SF 권한 모델 전면 적용에 따른 legacy permission 테이블 폐기.
--
-- 폐기 사유:
--   - UserPermission / RolePermission entity 및 repository 가 spec #801 에서 폐기됨
--   - 권한 부여 SoT 가 SF Profile + PermissionSetAssignment + PermissionSetFlags 로 단일화
--   - backend code 측 RolePermissionMatrix Kotlin SoT + RolePermissionSyncRunner 부팅 sync 도 폐기
--
-- 운영 데이터 손실 없음: cut-over 전 운영 SF org 에서 admin user 에게 PermissionSet 사전 부여
-- (Q5 옵션 1) + Stage 1 적재 후 SF 권한 모델로 자연스럽게 대체.

DROP TABLE IF EXISTS powersales.user_permission;
DROP TABLE IF EXISTS powersales.role_permission;
