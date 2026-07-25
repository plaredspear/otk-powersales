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
import com.otoki.powersales.domain.org.employee.dto.response.FemaleEmployeeListMetaResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetDeviceResponse
import com.otoki.powersales.domain.org.employee.dto.response.ResetPasswordResponse
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeCredentialService
import com.otoki.powersales.domain.org.employee.service.AdminEmployeeService
import com.otoki.powersales.domain.activity.schedule.dto.response.EmployeeWorkHistoryResponse
import com.otoki.powersales.domain.activity.schedule.service.EmployeeWorkHistoryService
import com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver
import com.otoki.powersales.domain.org.organization.branchmapping.BranchCodeExpander
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
 * 조장은 여사원 조직을 관리하는 직책이라 여사원 현황에 함께 노출한다.
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
 * ## 지점 스코프 단일 출처 — [WomenScheduleBranchResolver]
 *
 * 목록 조회 조건(`/meta` 의 지점 셀렉터 옵션) 과 목록/엑셀 조회의 지점 보안 스코프
 * ([resolveBranchScope]) 가 **동일 resolver** 를 공유한다. 과거에는 셀렉터만 resolver 를 쓰고
 * 조회는 `@CurrentDataScope` (본인 지점 1건) 를 써서, 셀렉터에 노출된 형제/상위 지점을 고르면
 * 빈 목록이 되는 드리프트가 있었다.
 */
@RestController
@RequestMapping("/api/v1/admin/female-employees")
@PermissionResource("female_employee")
class AdminFemaleEmployeeController(
    private val adminEmployeeService: AdminEmployeeService,
    private val employeeWorkHistoryService: EmployeeWorkHistoryService,
    private val adminEmployeeCredentialService: AdminEmployeeCredentialService,
    private val womenScheduleBranchResolver: WomenScheduleBranchResolver,
    private val branchCodeExpander: BranchCodeExpander,
) {

    companion object {
        /** 여사원 현황에 노출할 직책 — 여사원 + 조장(여사원 조직 관리자). */
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
     * [WomenScheduleBranchResolver] 결과를 컨트롤러가 조립해 붙인다.
     *
     * 셀렉터 옵션과 목록/엑셀 조회 스코프는 **동일 출처**([WomenScheduleBranchResolver]) 를 공유한다
     * ([resolveBranchScope] 참조) — 셀렉터에 노출된 지점을 고르면 항상 그 지점 결과가 나온다.
     */
    @GetMapping("/meta")
    @RequiresSfPermission(entity = "female_employee", operation = SfPermissionOperation.READ)
    fun getFemaleEmployeeListMeta(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<FemaleEmployeeListMetaResponse>> {
        val base = adminEmployeeService.getFemaleEmployeeListMetaStatic()
        val branchOptions = womenScheduleBranchResolver.resolveBranches(principal)
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
     * 여사원 현황 목록 조회.
     *
     * 지점 스코프는 [resolveBranchScope] 로 산출한다 — `/meta` 셀렉터 옵션과 동일 출처
     * ([WomenScheduleBranchResolver]) 이므로 셀렉터에 보이는 지점을 고르면 그 지점 결과가 나온다.
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
     * 1건) 를 썼기 때문에, 셀렉터가 [WomenScheduleBranchResolver] 로 조직 트리 전체를 옵션으로 내려주면서도
     * 그 중 본인 소속 지점이 아닌 지점을 고르면 `NoAccess` → 빈 목록이 되는 스코프 드리프트가 있었다.
     * 본 메서드는 스코프를 셀렉터와 같은 resolver 로 통일해 그 불일치를 제거한다.
     *
     * 반환한 [DataScope] 는 `getEmployees`/`exportEmployees` 의 `applyBranchScope = true` 경로에서
     * `effectiveBranchCodes(requestedBranch)` 로 소비된다 — 즉 IDOR 판정(요청 지점이 권한 집합에 속하는지)은
     * 기존 [DataScope] 규칙을 그대로 재사용하고, **권한 집합의 출처만** 셀렉터와 일치시킨다.
     *
     * - 전사 권한자 (시스템 관리자 / 영업지원 / 본부장·사업부장·영업부장 —
     *   [WomenScheduleBranchResolver.isAllBranchesUser]): `isAllBranches = true`
     *   → 지점 미선택 시 전사, 선택 시 그 지점.
     * - 지점 권한자 (지점장 / 조장 / 여사원): 본인 costCenterCode 의 조직 트리 지점 코드 집합.
     *   [BranchCodeExpander] 로 이력 코드(BranchMapping — 동일 지점의 조직 개편 전/후 코드) 까지 확장한다.
     *
     * 확장한 이력 코드가 실제 필터에 반영되는 범위는 지점 선택 여부에 따라 다르다 —
     * `DataScope.effectiveBranchCodes` 가 지점 **선택 시** 요청 코드 1건만 `Filtered` 로 남기므로 이력 코드는
     * IDOR 통과 판정에만 쓰이고, **미선택 시**에는 확장 집합 전체가 필터로 들어간다. (근무기간 조회
     * [com.otoki.powersales.domain.activity.schedule.service.AdminAttendInfoService.getMembers] 는 선택 지점
     * 1건을 확장해 필터에 쓰므로 선택 시 매칭 폭이 본 메서드보다 넓다.)
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
        if (womenScheduleBranchResolver.isAllBranchesUser(principal)) {
            // 전사 권한자는 어떤 지점을 골라도 허용 — 셀렉터 옵션도 전 지점이다.
            return DataScope(branchCodes = emptyList(), isAllBranches = true)
        }
        // 셀렉터 옵션과 동일한 화이트리스트를 1회 산출해 IDOR 검증과 스코프 조립에 함께 쓴다.
        val allowedCodes = womenScheduleBranchResolver.resolveBranches(principal).map { it.branchCode }
        if (requested != null && requested !in allowedCodes) return null
        return DataScope(
            branchCodes = branchCodeExpander.expand(allowedCodes).toList(),
            isAllBranches = false,
        )
    }
}
