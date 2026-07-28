package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.PermissionResource
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.entity.AppAuthority
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeDetailResponse
import com.otoki.powersales.domain.org.employee.dto.response.EmployeeListResponse
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterMeta
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterOption
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFilterType
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeFormMetaResponse
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListMetaResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.YearMonth
import java.time.format.DateTimeParseException

/**
 * 여사원 현황 페이지 전용 — role 은 [AppAuthority.WOMAN] ("여사원") + [AppAuthority.LEADER] ("조장") 로 고정.
 * 조장은 여사원 조직을 관리하는 직책이라 여사원 현황에 함께 노출한다 (레거시 인원현황 리포트 정합,
 * [FEMALE_EMPLOYEE_ROLES] 참조).
 *
 * 권한 관리 (`/settings/admin-accounts`) 등 전체 role 을 보여야 하는 화면은
 * [AdminEmployeeController.getEmployees] 를 그대로 사용하고, 본 endpoint 는
 * 여사원 현황 화면에서만 호출한다.
 *
 * ## 권한 자원 — `female_employee` (가상 자원, [PermissionResource])
 *
 * 여사원 현황/상세는 전체 사원 관리(`employee`)와 권한을 분리한다. 조장 등 "여사원만 보는"
 * 직책에 여사원 현황만 부여하고 전체 사원 관리는 막기 위함. 레거시 SF 는 Employee 객체 권한
 * 하나만 두어 두 영역을 구분하지 않았으므로 본 분리는 신규 deviation 이며, SF Custom Permission
 * (`female_employee`) 으로 부여한다 — JPA entity 가 없는 가상 자원이라 [PermissionResource] 로 등록.
 * 상세/근무이력은 [AdminEmployeeController] 의 공용 endpoint 를 쓰지 않고 본 컨트롤러에서
 * `female_employee` 가드로 제공하여 여사원 권한만으로 현황+상세 완결 접근이 되도록 한다.
 *
 * ## 지점 스코프 단일 출처 — [DashboardBranchResolver] (대시보드와 동일 목록)
 *
 * 목록 조회 조건(`/meta` 의 지점 셀렉터 옵션) 과 목록/엑셀 조회의 지점 보안 스코프
 * ([resolveBranchScope]) 가 **동일 resolver** 를 공유한다. 과거에는 셀렉터만 resolver 를 쓰고
 * 조회는 `@CurrentDataScope` (본인 지점 1건) 를 써서, 셀렉터에 노출된 형제/상위 지점을 고르면
 * 빈 목록이 되는 드리프트가 있었다.
 *
 * 2026-07-29 부터 그 resolver 를 [com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver]
 * → [DashboardBranchResolver] 로 교체했다. 여사원 현황과 투입현황 대시보드는 이미 동일 모수
 * (여사원+조장 role, `femaleStaffHeadcountScope`) 로 인원을 세므로, 지점 축까지 같은 목록
 * (전사 권한자 = Retail 32지점 + 영업지원2팀 + CVS전략팀 34개 고정) 을 써야 두 화면의 총원이 일치한다.
 * 지점 권한자(지점장/조장/여사원) 는 [DashboardBranchResolver] 가 기존 resolver 에 위임하므로 동작 동일.
 */
@RestController
@RequestMapping("/api/v1/admin/female-employees")
@PermissionResource("female_employee")
class AdminFemaleEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
    private val employeeWorkHistoryService: EmployeeWorkHistoryService,
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService,
    private val dashboardBranchResolver: DashboardBranchResolver,
) {

    companion object {
        /**
         * 여사원 현황에 노출할 직책 — 여사원 + 조장(여사원 조직 관리자).
         *
         * 레거시 SF 홈 대시보드(조장) 의 인원현황 리포트
         * (`reports/X00/new_report_72Y.report-meta.xml`) 가 `AppAuthority__c IN ('조장','여사원')` 으로
         * 조장을 포함하므로 동일 축을 유지한다. 대시보드 기본현황 집계
         * ([com.otoki.powersales.domain.org.employee.repository.EmployeeRepositoryCustom.findDashboardBasicStatsProjection])
         * 도 같은 role 집합을 쓴다 — 두 화면의 총원이 어긋나지 않도록 함께 유지할 것.
         */
        private val FEMALE_EMPLOYEE_ROLES = listOf(AppAuthority.WOMAN, AppAuthority.LEADER)

        /** 권한 밖 지점 요청 시 사용 — `branchCodes` 가 비어 `effectiveBranchCodes` 가 NoAccess 로 판정한다. */
        private val NO_ACCESS_SCOPE = DataScope(branchCodes = emptyList(), isAllBranches = false)
    }

    /**
     * 여사원 현황 목록 화면 조회 조건 로드 — "권한 기반 조건 로드" 표준 패턴 (행사마스터 `/meta` 정합).
     *
     * 별도 `/branches` endpoint(지점 셀렉터) + web 하드코딩(재직상태·근무형태1·근무형태3·전문행사조)
     * 으로 분산됐던 목록 조건 로드를 단일 응답으로 통합했다 (`/branches` 는 호출처가 없어져 제거).
     * 정적 조건(재직상태·근무형태·전문행사조)과 기본값은 서비스가, 권한 의존 지점 옵션은
     * [DashboardBranchResolver] 결과를 컨트롤러가 조립해 붙인다 — 대시보드 지점 셀렉터와 동일 목록.
     *
     * 셀렉터 옵션과 목록/엑셀 조회 스코프는 **동일 출처**([DashboardBranchResolver]) 를 공유한다
     * ([resolveBranchScope] 참조) — 셀렉터에 노출된 지점을 고르면 항상 그 지점 결과가 나온다.
     */
    @GetMapping("/meta")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeListMeta(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<FemaleEmployeeListMetaResponse>> {
        val base = adminEmployeeService.getFemaleEmployeeListMetaStatic()
        val branchOptions = dashboardBranchResolver.resolveBranches(principal)
            .map { FemaleEmployeeFilterOption(value = it.branchCode, label = it.branchName) }
        val response = base.copy(
            filters = base.filters + FemaleEmployeeFilterMeta(
                key = "costCenterCode",
                type = FemaleEmployeeFilterType.SELECT,
                options = branchOptions,
            ),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 상세 폼(수정 모달) 렌더링용 메타 — "form 전용 API 분리" 패턴
     * (행사마스터 `/promotions/form-meta` · 전문행사조 마스터 `/ppt-masters/form-meta` 정합).
     *
     * 재직상태 / 권한 / 전문행사조 Select 옵션을 내려, web 화면이 하드코딩하던 상수 3종을 대체한다.
     * 목록 조건 로드([getFemaleEmployeeListMeta]) 와 달리 권한 의존 옵션이 없어 전 사용자 동일 응답이며,
     * 상세 진입이 아니라 **모달을 여는 시점에만** 조회한다.
     *
     * 목록 `/meta` 의 전문행사조 옵션과는 구성이 다르다 — 검색 전용 '행사조 전체' 는 빠지고,
     * 미배정 복귀용 '일반' 은 포함된다 ([AdminEmployeeService.getFemaleEmployeeFormMeta] 참조).
     */
    @GetMapping("/form-meta")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeFormMeta(): ResponseEntity<ApiResponse<FemaleEmployeeFormMetaResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminEmployeeService.getFemaleEmployeeFormMeta()))
    }

    /**
     * 여사원 현황 목록 조회.
     *
     * 지점 스코프는 [resolveBranchScope] 로 산출한다 — `/meta` 셀렉터 옵션과 동일 출처
     * ([DashboardBranchResolver]) 이므로 셀렉터에 보이는 지점을 고르면 그 지점 결과가 나온다.
     */
    @GetMapping
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        // 근무형태1(진열/행사) / 근무형태3(고정/격고/순회) — 최근 출근등록 1건 기준 필터.
        @RequestParam(required = false) workType1: String?,
        @RequestParam(required = false) workType3: String?,
        // 전문행사조 — 조명(라면세일조 등) 또는 '일반'(미배정). blank 면 전체.
        @RequestParam(required = false) professionalPromotionTeam: String?,
        // 직무 — 판촉직 / OSC직. 대시보드 "판촉직/OSC직 인원현황" 도넛과 동일한 jobCode 축이며,
        // 'OSC직' 은 구 명칭 '레이디직' 을 함께 조회한다. blank 면 전체.
        @RequestParam(required = false) jobCode: String?,
        @RequestParam(required = false, defaultValue = "0") page: Int,
        @RequestParam(required = false, defaultValue = "20") size: Int,
    ): ResponseEntity<ApiResponse<EmployeeListResponse>> {
        val branchScope = resolveBranchScope(principal, costCenterCode)
            ?: return ResponseEntity.ok(
                ApiResponse.success(
                    EmployeeListResponse(
                        content = emptyList(), page = page, size = size, totalElements = 0, totalPages = 0,
                    ),
                ),
            )
        val response = adminEmployeeService.getEmployees(
            scope = branchScope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            roles = FEMALE_EMPLOYEE_ROLES,
            page = page,
            size = size,
            // SF `SalesMemberListController` / `TeamMemberListController` 의 CostCenterCode 지점 스코프 정합
            applyBranchScope = true,
            workType1 = workType1,
            workType3 = workType3,
            professionalPromotionTeam = professionalPromotionTeam,
            jobCode = jobCode,
            // 레거시 인원현황 리포트 정합 모수 — 여사원 직무 3값 한정 + 테스트/시스템 계정 제외.
            // 대시보드 기본현황 집계와 동일 모수라 두 화면의 총원이 일치한다.
            femaleStaffHeadcountScope = true,
            // 발령명 '면직' 은 퇴직과 동일 취급 (퇴직 조회에 포함 / 재직·휴직 조회에서 제외,
            // 상태는 '퇴직(면직)' 으로 표시) — 여사원 현황 전용
            // ([com.otoki.powersales.domain.org.employee.enums.DismissalPolicy]).
            treatDismissalAsResigned = true,
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 여사원 현황 엑셀 다운로드 — 목록과 동일한 지점 스코프/필터로 전량 추출 (최대 건수 제한 적용). */
    @GetMapping("/export")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun exportFemaleEmployees(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) status: String?,
        @RequestParam(required = false) costCenterCode: String?,
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) workType1: String?,
        @RequestParam(required = false) workType3: String?,
        @RequestParam(required = false) professionalPromotionTeam: String?,
        @RequestParam(required = false) jobCode: String?,
    ): ResponseEntity<ByteArray> {
        // 권한 밖 지점 요청이면 NO_ACCESS_SCOPE (branchCodes 비어 있음) → 서비스가 NoAccess 로 판정해
        // 헤더만 있는 빈 엑셀을 반환한다 (목록의 빈 결과와 동일 취급).
        val branchScope = resolveBranchScope(principal, costCenterCode) ?: NO_ACCESS_SCOPE
        val result = adminEmployeeService.exportEmployees(
            scope = branchScope,
            status = status,
            costCenterCode = costCenterCode,
            keyword = keyword,
            roles = FEMALE_EMPLOYEE_ROLES,
            applyBranchScope = true,
            workType1 = workType1,
            workType3 = workType3,
            professionalPromotionTeam = professionalPromotionTeam,
            jobCode = jobCode,
            // 레거시 인원현황 리포트 정합 모수 — 여사원 직무 3값 한정 + 테스트/시스템 계정 제외.
            // 대시보드 기본현황 집계와 동일 모수라 두 화면의 총원이 일치한다.
            femaleStaffHeadcountScope = true,
            // 발령명 '면직' 은 퇴직과 동일 취급 (퇴직 조회에 포함 / 재직·휴직 조회에서 제외,
            // 상태는 '퇴직(면직)' 으로 표시) — 여사원 현황 전용
            // ([com.otoki.powersales.domain.org.employee.enums.DismissalPolicy]).
            treatDismissalAsResigned = true,
        )
        return ExcelResponseUtils.build(result)
    }

    /**
     * 여사원 단건 상세 조회 — [AdminEmployeeController.getEmployee] 와 동일 응답이나
     * `female_employee` 권한으로 가드. 여사원 현황에서 행 클릭 시 호출.
     */
    @GetMapping("/{employeeId}")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployee(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<EmployeeDetailResponse>> {
        val response = adminEmployeeService.getEmployee(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 상세 — 시간순서별 근무이력(TeamMemberSchedule) 조회. 기본 limit 10.
     */
    @GetMapping("/{employeeId}/work-history")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam(required = false, defaultValue = "10") limit: Int,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val response = employeeWorkHistoryService.getRecentHistory(employeeId, limit)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 근무기간 조회(월별) — 인원 1명 × 지정 월의 근무내역을 일자 오름차순 조회.
     *
     * @param yearMonth `yyyy-MM` (예: 2026-06).
     */
    @GetMapping("/{employeeId}/work-history/monthly")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeMonthlyWorkHistory(
        @PathVariable employeeId: Long,
        @RequestParam yearMonth: String,
    ): ResponseEntity<ApiResponse<EmployeeWorkHistoryResponse>> {
        val parsed = try {
            YearMonth.parse(yearMonth)
        } catch (e: DateTimeParseException) {
            throw IllegalArgumentException("yearMonth 형식이 올바르지 않습니다 (yyyy-MM): $yearMonth")
        }
        val response = employeeWorkHistoryService.getMonthlyHistory(employeeId, parsed)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 여사원 단말 초기화 — [AdminEmployeeController.resetDevice] 와 동일 동작이나
     * `female_employee:EDIT` 권한으로 가드. 여사원 현황 화면에서 조장 등 여사원 권한만 가진
     * 직책이 호출할 수 있도록 분리. 전체 사원 관리(`MANAGE_USERS`)와 별개 권한.
     */
    @PostMapping("/{employeeId}/reset-device")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.EDIT)
    fun resetFemaleEmployeeDevice(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<ResetDeviceResponse>> {
        val response = adminEmployeeCredentialService.resetDevice(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "단말이 초기화되었습니다"))
    }

    /**
     * 여사원 비밀번호 초기화 — [AdminEmployeeController.resetPassword] 와 동일 동작이나
     * `female_employee:EDIT` 권한으로 가드.
     */
    @PostMapping("/{employeeId}/reset-password")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.EDIT)
    fun resetFemaleEmployeePassword(
        @PathVariable employeeId: Long,
    ): ResponseEntity<ApiResponse<ResetPasswordResponse>> {
        val response = adminEmployeeCredentialService.resetPassword(employeeId)
        return ResponseEntity.ok(ApiResponse.success(response, "비밀번호가 초기화되었습니다"))
    }

    /**
     * 목록/엑셀 조회의 지점 보안 스코프 산출 — **`/meta` 셀렉터 옵션과 동일 출처**.
     *
     * 기존에는 `@CurrentDataScope` (→ `DataScope.branchCodes = listOfNotNull(costCenterCode)`, 본인 지점
     * 1건) 를 썼기 때문에, 셀렉터가 resolver 로 조직 트리 전체를 옵션으로 내려주면서도
     * 그 중 본인 소속 지점이 아닌 지점을 고르면 `NoAccess` → 빈 목록이 되는 스코프 드리프트가 있었다.
     * 본 메서드는 스코프를 셀렉터와 같은 resolver 로 통일해 그 불일치를 제거한다.
     *
     * 반환한 [DataScope] 는 `getEmployees`/`exportEmployees` 의 `applyBranchScope = true` 경로에서
     * `effectiveBranchCodes(requestedBranch)` 로 소비된다 — 즉 IDOR 판정(요청 지점이 권한 집합에 속하는지)은
     * 기존 [DataScope] 규칙을 그대로 재사용하고, **권한 집합의 출처만** 셀렉터와 일치시킨다.
     *
     * 권한 분기는 [DashboardBranchResolver.resolveBranches] 안에 있어 여기서 다시 나누지 않는다
     * (셀렉터 옵션 = 허용 지점 집합이라는 등식을 코드 구조로 보장):
     * - 전사 권한자 (시스템 관리자 / 영업지원 / 본부장·사업부장·영업부장): 대시보드 고정 화이트리스트
     *   34개 (Retail 32지점 + 영업지원2팀 + CVS전략팀). **전건 조회가 아니다** — 지점 미선택이면
     *   34개 IN 매칭이며, 이는 대시보드(`AdminDashboardController.getDashboard`) 와 동일 규칙이다.
     *   34개 밖 지점 소속 사원은 목록에 나오지 않는다 (두 화면 총원 일치가 우선).
     * - 지점 권한자 (지점장 / 조장 / 여사원): 본인 costCenterCode 의 조직 트리 지점 코드 집합
     *   (resolver 가 기존 `WomenScheduleBranchResolver` 에 위임 — 동작 변화 없음).
     *
     * ## BranchMapping 확장은 여기서 하지 않는다 (1-hop 보장)
     *
     * 폐기된 옛 코드까지 넓히는 [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander]
     * 확장은 최종 필터 직전인
     * [com.otoki.powersales.domain.org.employee.service.AdminEmployeeService.expandBranchCodes] 한 곳에서만
     * 적용된다 — 지점 선택 시(`Filtered([요청코드])`) / 미선택 시(`Filtered(화이트리스트)`) 모두 그 경로를 탄다.
     *
     * 본 메서드가 확장 결과를 [DataScope] 에 담으면 서비스가 그것을 **다시** 확장해 2-hop 이 되고,
     * 롤업 행(`BranchMapping` KDoc 의 성격 4종 중 롤업 6건) 이 걸리면 현행 타 조직까지 스코프가 넓어진다.
     * 확장 전 원본 코드를 넘기는 이유이며, "화이트리스트 자체를 확장하지 말 것" 이라는
     * [com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander] KDoc 규약과도 일치한다.
     *
     * **주의**: 반환하는 [DataScope] 는 `branchCodes` / `isAllBranches` 2차원만 채운 **부분 DataScope** 다
     * (`AdminDataScopeService` 가 채우는 sharing policy 차원 — userId / profileFlags / evaluatorRules 등은
     * 기본값). 현재 소비처(`getEmployees` / `exportEmployees`) 가 지점 2차원만 쓰기 때문에 안전하나,
     * 그 경로에 sharing rule 평가가 도입되면 본 메서드도 함께 갱신해야 한다.
     *
     * @return 권한 밖 지점을 요청했으면 `null` (호출부가 빈 결과로 응답).
     */
    private fun resolveBranchScope(principal: WebUserPrincipal, costCenterCode: String?): DataScope? {
        val requested = costCenterCode?.takeIf { it.isNotBlank() }
        // 셀렉터 옵션과 동일한 화이트리스트를 1회 산출해 IDOR 검증과 스코프 조립에 함께 쓴다.
        // 전사/지점 권한 분기는 resolver 안에 있으므로 여기서는 권한을 나누지 않는다 —
        // isAllBranches = false 로 고정해 전사 권한자도 셀렉터에 보이는 지점만 조회한다.
        val allowedCodes = dashboardBranchResolver.resolveBranches(principal).map { it.branchCode }
        if (requested != null && requested !in allowedCodes) return null
        // BranchMapping 확장은 여기서 하지 않는다 — 확장 전 원본 코드를 그대로 스코프로 넘겨야
        // 확장이 최종 필터([AdminEmployeeService.expandBranchCodes]) 에서 정확히 1회(1-hop) 적용된다.
        // (여기서도 확장하면 확장 결과가 다시 확장되어 2-hop 이 되고, 롤업 행이 걸리면 타 조직까지 넓어진다
        //  — BranchCodeExpander KDoc "화이트리스트 판정에 쓰지 말 것" 참조.)
        return DataScope(branchCodes = allowedCodes, isAllBranches = false)
    }
}
