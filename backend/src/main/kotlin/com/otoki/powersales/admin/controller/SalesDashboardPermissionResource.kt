package com.otoki.powersales.admin.controller

/**
 * 매출/실적 대시보드 3화면 전용 권한 자원 식별자 — 「월 매출(물류배부)」 / 「월 매출(전산실적)」 / 「POS매출」.
 *
 * ## 왜 `monthly_sales_history` 에서 분리했나
 *
 * 이전에는 3화면 모두 적재 테이블 entity 인 `monthly_sales_history` (SF `MonthlySalesHistory__c`) 를
 * 가드로 썼다. 그런데 그 키는 다음 화면들까지 한꺼번에 여닫아 화면 단위 통제가 불가능했다:
 *
 * - 기준정보 > ORORA 월매출 ([AdminMonthlySalesHistoryController] + `/accounts/lookup-for-monthly-sales`)
 * - 월별 진열사원 투입적합성 / 진열사원 배치 적합성 (지점 셀렉터 포함)
 * - 거래처 상세의 "매출 이력" 탭 (월매출 상세 API 임베드)
 *
 * 3화면만 본 가상 자원으로 옮겨 독립 부여/회수한다. 위 나머지 화면은 `monthly_sales_history` 를
 * 그대로 유지하므로 이번 분리의 파급을 받지 않는다 (거래처 상세 매출 이력 탭만 예외 — 본문은
 * 월매출 대시보드 상세 endpoint 를 그대로 임베드하므로 프론트 게이팅도 본 자원으로 함께 옮겼다).
 *
 * ## 부여 경로 — object_permissions 가 아니라 custom_permissions
 *
 * JPA entity 가 없는 가상 자원이라 SF objectPermissions 로는 표현할 수 없다. ProfileFlags /
 * PermissionSetFlags 의 `custom_permissions` JSON 에 `{"sales_dashboard": {"allowRead": true}}` 형태로
 * 부여하며, web admin 권한 편집 화면의 "Custom Permissions" 매트릭스에 자동 노출된다
 * (`EntitySfNameRegistry.allResources()` 파생 — 별도 등록 작업 불요).
 *
 * ## 승계 없음 (사용자 결정)
 *
 * 기존 `monthly_sales_history` READ 보유자에게 자동 승계하는 마이그레이션은 두지 않는다. 배포 직후
 * 3화면은 아래 두 부류에게만 열린다:
 *
 * - Profile.name = '시스템 관리자' (`SfPermissionResolver` 가 전 자원 CRUD 선주입)
 * - `VIEW_ALL_DATA` / `MODIFY_ALL_DATA` 비트 보유자 (`expandAllDataBits` 가 전 자원으로 펼침)
 *
 * 그 외 프로파일/권한세트는 web admin 권한 편집으로 직접 부여해야 한다.
 *
 * @see com.otoki.powersales.platform.auth.permission.PermissionResource
 */
const val SALES_DASHBOARD_RESOURCE = "sales_dashboard"
