package com.otoki.powersales.admin.controller

import com.otoki.powersales.domain.activity.promotion.dto.response.BulkConfirmResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.BulkValidationResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.ConfirmByIdsResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTConfirmedReportResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterFormMetaResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterHistoryListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterListResponse
import com.otoki.powersales.domain.activity.promotion.dto.response.PPTMasterResponse
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterBulkValidateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterConfirmByIdsRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterCreateRequest
import com.otoki.powersales.domain.activity.promotion.dto.request.PPTMasterUpdateRequest
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTConfirmedReportService
import com.otoki.powersales.domain.activity.promotion.service.AdminPPTMasterService
import com.otoki.powersales.admin.service.DashboardBranchResolver
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.PageRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 전문행사조 마스터 / 이력 / 확정인원 API.
 *
 * ## 지점 스코프 단일 출처 — [DashboardBranchResolver] (여사원 현황과 동일 목록)
 *
 * 지점 셀렉터 옵션([getBranches]) 과 목록/엑셀 조회의 지점 보안 스코프([resolveBranchScope]) 가
 * **동일 resolver** 를 공유한다. 과거에는 셀렉터가
 * [com.otoki.powersales.domain.activity.schedule.service.WomenScheduleBranchResolver] 를 써서
 * 전사 권한자에게 조직 테이블 전건(FS마케팅팀 · FS판매전략팀 등 여사원 조직 밖 부서 포함) 을
 * 옵션으로 내려주었고, 조회는 `@CurrentDataScope` 로 전건이었다.
 *
 * 2026-07-29 부터 그 resolver 를 [DashboardBranchResolver] 로 교체해 여사원 현황
 * ([AdminFemaleEmployeeController]) · 투입현황 대시보드와 지점 축을 통일한다 — 전사 권한자는
 * 고정 화이트리스트 34개(Retail 32지점 + 영업지원2팀 + CVS전략팀) 만 보고, 지점 미선택 시에도
 * 전건이 아니라 34개 IN 매칭이다. 지점 권한자(지점장/조장/여사원) 는 [DashboardBranchResolver] 가
 * 기존 resolver 에 위임하므로 동작 동일.
 */
@RestController
class AdminPPTMasterController(
    private val adminPPTMasterService: AdminPPTMasterService,
    private val pptConfirmedReportService: AdminPPTConfirmedReportService,
    private val dashboardBranchResolver: DashboardBranchResolver,
) {

    companion object {
        /** 권한 밖 지점 요청 시 사용 — `branchCodes` 가 비어 `effectiveBranchCodes` 가 NoAccess 로 판정한다. */
        private val NO_ACCESS_SCOPE = DataScope(branchCodes = emptyList(), isAllBranches = false)
    }

    /**
     * 전문행사조 화면 지점 셀렉터 옵션 — 마스터/이력/확정인원 3화면 공용.
     *
     * 여사원 현황/투입현황 대시보드와 동일하게 [DashboardBranchResolver] 로 권한별 지점 화이트리스트를
     * 산출한다 (단일 출처). 목록은 곧 해당 사용자가 조회 허용된 지점이며, 각 목록 조회 endpoint 는
     * [resolveBranchScope] 로 **동일 화이트리스트**를 재적용해 임의 branchCode 조회(IDOR) 를 차단한다.
     */
    @GetMapping("/api/v1/admin/ppt-masters/branches")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        val result = dashboardBranchResolver.resolveBranches(principal)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /**
     * 전문행사조 마스터 폼(등록/수정/복제 모달) 렌더링용 메타.
     *
     * 행사마스터 `/promotions/form-meta` 와 동일한 "form 전용 API 분리" 패턴 —
     * 폼 Select 옵션(전문행사조 유형)을 프론트 상수로 하드코딩하지 않고 서버 enum 을 단일 출처로 내려준다.
     */
    @GetMapping("/api/v1/admin/ppt-masters/form-meta")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getFormMeta(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ApiResponse<PPTMasterFormMetaResponse>> {
        return ResponseEntity.ok(ApiResponse.success(adminPPTMasterService.getFormMeta()))
    }

    @GetMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean,
        // 「재직상태」 (재직/휴직/퇴직) — 여사원 현황 목록의 `status` 파라미터와 동일 축.
        @RequestParam(required = false) employmentStatus: String?
    ): ResponseEntity<ApiResponse<PPTMasterListResponse>> {
        val response = adminPPTMasterService.getMasters(
            resolveBranchScope(principal, branchCode), employeeName, employeeCode, teamType, branchCode, validOnly,
            employmentStatus, PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 전문행사조 확정 인원 보고서 조회 (Spec #846). isConfirmed=true.
     *
     * 지점 스코프 — 마스터/이력 조회와 동일하게 본인 소속 지점만 노출하며, 전사 권한은 전체를 본다.
     * `branchCode` 지정 시(다중지점 사용자가 지점 선택) 해당 지점만 필터.
     */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ApiResponse<PPTConfirmedReportResponse>> {
        val response = pptConfirmedReportService.getReport(resolveBranchScope(principal, branchCode), branchCode)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /** 전문행사조 확정 인원 엑셀 다운로드. */
    @GetMapping("/api/v1/admin/ppt-masters/confirmed-report/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportConfirmedReport(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) branchCode: String?,
    ): ResponseEntity<ByteArray> {
        val result = pptConfirmedReportService.exportReport(resolveBranchScope(principal, branchCode), branchCode)
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.getMaster(id)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.CREATE)
    fun createMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterCreateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.createMaster(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PutMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun updateMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long,
        @Valid @RequestBody request: PPTMasterUpdateRequest
    ): ResponseEntity<ApiResponse<PPTMasterResponse>> {
        val response = adminPPTMasterService.updateMaster(id, request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @DeleteMapping("/api/v1/admin/ppt-masters/{id}")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.DELETE)
    fun deleteMaster(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable id: Long
    ): ResponseEntity<Void> {
        adminPPTMasterService.deleteMaster(id)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/api/v1/admin/ppt-masters/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportMasters(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(defaultValue = "true") validOnly: Boolean,
        @RequestParam(required = false) employmentStatus: String?
    ): ResponseEntity<ByteArray> {
        val result = adminPPTMasterService.exportToExcel(
            resolveBranchScope(principal, branchCode), employeeName, employeeCode, teamType, branchCode, validOnly,
            employmentStatus
        )
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-masters/excel-template")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun downloadExcelTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal
    ): ResponseEntity<ByteArray> {
        val bytes = adminPPTMasterService.generateExcelTemplate()
        val filename = "전문행사조마스터_템플릿_${LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)}.xlsx"
        return ExcelResponseUtils.build(bytes, filename)
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.CREATE)
    fun validateBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkValidationResponse>> {
        val response = adminPPTMasterService.validateBulk(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/bulk/confirm")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.CREATE)
    fun confirmBulk(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterBulkValidateRequest
    ): ResponseEntity<ApiResponse<BulkConfirmResponse>> {
        val response = adminPPTMasterService.confirmBulk(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response))
    }

    @PostMapping("/api/v1/admin/ppt-masters/confirm-by-ids")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.EDIT)
    fun confirmByIds(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: PPTMasterConfirmByIdsRequest
    ): ResponseEntity<ApiResponse<ConfirmByIdsResponse>> {
        val response = adminPPTMasterService.confirmByIds(request)
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-masters/{masterId}/history")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @PathVariable masterId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getHistory(masterId, PageRequest.of(page, size))
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @GetMapping("/api/v1/admin/ppt-histories/export")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun exportHistories(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtTo: LocalDate?
    ): ResponseEntity<ByteArray> {
        val result = adminPPTMasterService.exportHistoryToExcel(
            resolveBranchScope(principal, branchCode), employeeName, employeeCode, teamType, branchCode,
            changedAtFrom, changedAtTo
        )
        return ExcelResponseUtils.build(result)
    }

    @GetMapping("/api/v1/admin/ppt-histories")
    @RequiresSfPermission(entity = "professional_promotion_team_master", operation = SfPermissionOperation.READ)
    fun getAllHistory(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeName: String?,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) teamType: String?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtFrom: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) changedAtTo: LocalDate?
    ): ResponseEntity<ApiResponse<PPTMasterHistoryListResponse>> {
        val response = adminPPTMasterService.getAllHistory(
            resolveBranchScope(principal, branchCode), employeeName, employeeCode, teamType, branchCode,
            changedAtFrom, changedAtTo, PageRequest.of(page, size)
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    /**
     * 목록/엑셀 조회의 지점 보안 스코프 산출 — **셀렉터 옵션([getBranches]) 과 동일 출처**.
     *
     * 기존에는 `@CurrentDataScope` 를 그대로 썼기 때문에, 셀렉터는 [DashboardBranchResolver] 화이트리스트를
     * 내려주는데 조회는 전사 권한자에게 전건을 주어 두 축이 갈라졌다 (셀렉터에 없는 지점 소속 데이터가
     * 목록에는 나오지만 셀렉터로는 그 지점만 골라낼 수 없는 상태). 본 메서드는 스코프를 셀렉터와 같은
     * resolver 로 통일해 그 불일치를 제거한다 — 여사원 현황
     * ([AdminFemaleEmployeeController.resolveBranchScope]) 과 동일 패턴.
     *
     * 권한 분기는 [DashboardBranchResolver.resolveBranches] 안에 있어 여기서 다시 나누지 않는다
     * (셀렉터 옵션 = 허용 지점 집합이라는 등식을 코드 구조로 보장):
     * - 전사 권한자 (시스템 관리자 / 영업지원 / 본부장·사업부장·영업부장): 고정 화이트리스트 34개.
     *   **전건 조회가 아니다** — 지점 미선택이면 34개 IN 매칭이며, 34개 밖 지점 소속 전문행사조는
     *   목록에 나오지 않는다 (여사원 현황 · 투입현황 대시보드와 지점 축 일치가 우선).
     * - 지점 권한자 (지점장 / 조장 / 여사원): 본인 costCenterCode 의 조직 트리 지점 코드 집합
     *   (resolver 가 기존 `WomenScheduleBranchResolver` 에 위임 — 동작 변화 없음).
     *
     * `isAllBranches = false` 로 고정하는 이유는 전사 권한자도 셀렉터에 보이는 지점만 조회하게 하기 위함이다
     * (`true` 면 `DataScope.effectiveBranchCodes` 가 미선택 시 `All` 로 빠져 전건이 된다).
     *
     * **주의 1**: 반환하는 [DataScope] 는 `branchCodes` / `isAllBranches` 2차원만 채운 **부분 DataScope** 다.
     * 현재 소비처(마스터/이력/확정인원 조회) 가 지점 2차원만 쓰기 때문에 안전하나, 그 경로에 sharing rule
     * 평가가 도입되면 본 메서드도 함께 갱신해야 한다.
     *
     * **주의 2 — 적용 범위는 목록/엑셀 6개 endpoint 뿐이다**: 단건 상세([getMaster]) 와 마스터별
     * 이력([getHistory]) 은 id 만으로 조회하며 지점 스코프를 적용하지 않는다(본 변경 이전부터 동일).
     * 목록이 화이트리스트로 좁아진 만큼 목록↔상세의 가시 범위 격차는 커졌으므로, 상세에도 스코프를
     * 걸어야 한다면 별도 작업으로 다룬다.
     *
     * @return 권한 밖 지점을 요청했으면 [NO_ACCESS_SCOPE] (서비스가 빈 결과로 응답).
     */
    private fun resolveBranchScope(principal: WebUserPrincipal, branchCode: String?): DataScope {
        val requested = branchCode?.takeIf { it.isNotBlank() }
        val allowedCodes = dashboardBranchResolver.resolveBranches(principal).map { it.branchCode }
        if (requested != null && requested !in allowedCodes) return NO_ACCESS_SCOPE
        return DataScope(branchCodes = allowedCodes, isAllBranches = false)
    }
}
