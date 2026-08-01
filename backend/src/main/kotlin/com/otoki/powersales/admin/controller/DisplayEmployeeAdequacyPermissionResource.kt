package com.otoki.powersales.admin.controller

/**
 * 진열사원 적합성 2화면 전용 권한 자원 식별자 — 「월별 진열사원 투입적합성」 / 「진열사원 배치 적합성」.
 *
 * ## 왜 `monthly_sales_history` 에서 분리했나
 *
 * 두 화면은 적재 테이블 entity 인 `monthly_sales_history` (SF `MonthlySalesHistory__c`) 를 가드로 썼다.
 * 그 키는 「기준정보 > ORORA 월매출」([AdminMonthlySalesHistoryController] +
 * `/accounts/lookup-for-monthly-sales`) 까지 한꺼번에 여닫아 화면 단위 통제가 불가능했다 —
 * 조장에게 적합성 2화면만 열어주려 해도 ORORA 월매출이 함께 열렸다.
 *
 * 적합성 2화면만 본 가상 자원으로 옮겨 독립 부여/회수한다. ORORA 월매출은 `monthly_sales_history` 를
 * 그대로 유지하므로 이번 분리의 파급을 받지 않는다.
 *
 * [SALES_DASHBOARD_RESOURCE] 가 같은 키에서 대시보드 3화면을 먼저 떼어낸 것과 동일한 패턴이며,
 * 그 분리 이후 `monthly_sales_history` 에 남아 있던 3화면 중 2화면을 본 자원이 다시 가져간다.
 * 결과적으로 `monthly_sales_history` 는 ORORA 월매출 단독 가드가 된다.
 *
 * ## 두 화면을 한 자원으로 묶는 이유
 *
 * 둘 다 여사원 일정 축의 "진열사원 적합성" 을 보는 조회 전용 화면이라 운영상 함께 여닫힌다.
 * 화면별 독립 통제가 필요해지면 그때 자원을 쪼갠다 (지금 쪼개면 부여 지점만 둘로 늘 뿐이다).
 *
 * 각 화면의 **전용 지점 셀렉터**([AdminSalesBranchController] 의 `/input-adequacy/branches` /
 * `/deployment/branches`) 도 본 자원으로 함께 옮긴다 — 셀렉터를 남겨두면 메뉴는 보이는데 지점 목록만
 * 403 이 나는 게이팅 ↔ API 가드 불일치가 된다. 셀렉터가 쓰는 [BranchScopeProfile] 은 화면마다 다르지만
 * (투입적합성 = SALES, 배치 적합성 = MASTER_LIST) 권한 축은 동일하다.
 *
 * ## 부여 경로 — object_permissions 가 아니라 custom_permissions
 *
 * JPA entity 가 없는 가상 자원이라 SF objectPermissions 로는 표현할 수 없다. ProfileFlags /
 * PermissionSetFlags 의 `custom_permissions` JSON 에 `{"display_employee_adequacy": {"allowRead": true}}`
 * 형태로 부여하며, web admin 권한 편집 화면의 "Custom Permissions" 매트릭스에 자동 노출된다
 * (`EntitySfNameRegistry.allResources()` 파생 — 별도 등록 작업 불요).
 *
 * 두 화면 모두 조회 전용이라 가드가 실재하는 operation 은 **READ 단독**이다.
 *
 * ## 승계 없음 (사용자 결정)
 *
 * 기존 `monthly_sales_history` READ 보유자에게 자동 승계하는 마이그레이션은 두지 않는다
 * ([SALES_DASHBOARD_RESOURCE] 분리 때와 동일 결정). 배포 직후 2화면은 아래 두 부류에게만 열린다:
 *
 * - Profile.name = '시스템 관리자' (`SfPermissionResolver` 가 전 자원 CRUD 선주입)
 * - `VIEW_ALL_DATA` / `MODIFY_ALL_DATA` 비트 보유자 (`expandAllDataBits` 가 전 자원으로 펼침)
 *
 * 조장(`6.조장`) 은 Stage 2 의 `leader-display-adequacy-grant` substep 이 부여한다
 * (`SfMigrationStage2Service.GRANTED_LEADER_CUSTOM_PERMISSIONS`). 그 외 프로파일/권한세트는
 * web admin 권한 편집으로 직접 부여해야 한다.
 *
 * @see com.otoki.powersales.platform.auth.permission.PermissionResource
 */
const val DISPLAY_EMPLOYEE_ADEQUACY_RESOURCE = "display_employee_adequacy"
