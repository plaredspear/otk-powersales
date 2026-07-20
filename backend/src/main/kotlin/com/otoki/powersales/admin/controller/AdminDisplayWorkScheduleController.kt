package com.otoki.powersales.admin.controller

import com.otoki.powersales.platform.auth.permission.RequiresSfPermission
import com.otoki.powersales.platform.auth.permission.SfPermissionOperation
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleCreateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.AdminScheduleUpdateRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleBatchConfirmRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleBatchDeleteRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleConfirmRequest
import com.otoki.powersales.domain.activity.schedule.dto.request.ScheduleExportRequest
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchDeleteResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleBatchUnconfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleConfirmResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleCreateResultDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleDetailDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterMeta
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterOption
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleFilterType
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListItemDto
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleListMetaResponse
import com.otoki.powersales.domain.activity.schedule.dto.response.ScheduleUploadResultDto
import com.otoki.powersales.domain.activity.schedule.enums.SchedulePreset
import com.otoki.powersales.domain.activity.schedule.enums.ScheduleValidData
import com.otoki.powersales.domain.activity.schedule.exception.ScheduleFileRequiredException
import com.otoki.powersales.admin.dto.DataScope
import com.otoki.powersales.admin.dto.EffectiveBranchResult
import com.otoki.powersales.admin.security.CurrentDataScope
import com.otoki.powersales.admin.service.WhitelistBranchScopeResolver
import com.otoki.powersales.domain.activity.schedule.service.AdminDisplayWorkScheduleService
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.dto.response.BranchResponse
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.data.domain.Sort
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/admin/display-work-schedule")
class AdminDisplayWorkScheduleController(
    private val adminDisplayWorkScheduleService: AdminDisplayWorkScheduleService,
    private val whitelistBranchScopeResolver: WhitelistBranchScopeResolver,
) {

    /**
     * 진열스케줄마스터 목록 화면 지점 셀렉터 옵션.
     *
     * 대시보드/행사마스터와 동일하게 전사 권한자는 고정 화이트리스트 34개
     * ([DashboardBranchResolver.DASHBOARD_ALL_BRANCHES]), 그 외는 본인 지점 1건
     * ([WhitelistBranchScopeResolver]). 화면 게이팅과 동일한 display_work_schedule READ 로 가드.
     */
    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/branches")
    fun getScheduleBranches(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<List<BranchResponse>>> {
        return ResponseEntity.ok(ApiResponse.success(whitelistBranchScopeResolver.getBranches(principal)))
    }

    /**
     * 진열스케줄마스터 목록 화면 조회 조건 로드 — "권한 기반 조건/UI 제어" 표준 패턴.
     *
     * 정적 조건(근무유형3/확정상태/텍스트/날짜 필터 + 기본값)은 서비스가, 권한 의존 지점(branchCode)
     * 셀렉터 옵션은 컨트롤러가 [WhitelistBranchScopeResolver] 로 산출해 조립한다. 셀렉터-조회 스코프가
     * 동일 리졸버를 공유해 드리프트를 방지한다(목록/엑셀 [resolveBranchCodes] 와 동일 출처).
     *
     * 지점(branchCode) 옵션 길이로 프론트가 단일/다중을 판별한다(단일이면 Tag, 다중이면 Select).
     * 화면 게이팅과 동일한 display_work_schedule READ 로 가드.
     */
    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/meta")
    fun getScheduleListMeta(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<ScheduleListMetaResponse>> {
        val base = adminDisplayWorkScheduleService.getScheduleListMetaStatic()
        val branchOptions = whitelistBranchScopeResolver.getBranches(principal)
            .map { ScheduleFilterOption(value = it.branchCode, label = it.branchName) }
        val response = base.copy(
            filters = base.filters + ScheduleFilterMeta(
                key = "branchCode",
                type = ScheduleFilterType.SELECT,
                options = branchOptions,
            ),
        )
        return ResponseEntity.ok(ApiResponse.success(response))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/list")
    fun listSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) accountType: String?,
        @RequestParam(required = false) accountStatus: String?,
        @RequestParam(required = false) confirmed: Boolean?,
        @RequestParam(required = false) typeOfWork3: String?,
        @RequestParam(required = false) periodStart: LocalDate?,
        @RequestParam(required = false) periodEnd: LocalDate?,
        @RequestParam(required = false) preset: SchedulePreset?,
        @RequestParam(required = false) validData: ScheduleValidData?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
    ): ResponseEntity<ApiResponse<Page<ScheduleListItemDto>>> {
        val sort = resolveSort(sortBy, sortDir)
        val result = adminDisplayWorkScheduleService.listSchedules(
            scope, page, size, employeeCode, accountName, accountType, accountStatus, confirmed,
            typeOfWork3, periodStart, periodEnd, preset, validData, resolveBranchCodes(principal, branchCode), sort
        )
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    /** 진열스케줄마스터 검색결과 엑셀 다운로드 — 목록과 동일한 가시 범위/필터로 전량 추출 (최대 건수 제한 적용). */
    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/export-all")
    fun exportAllSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @RequestParam(required = false) employeeCode: String?,
        @RequestParam(required = false) accountName: String?,
        @RequestParam(required = false) accountType: String?,
        @RequestParam(required = false) accountStatus: String?,
        @RequestParam(required = false) confirmed: Boolean?,
        @RequestParam(required = false) typeOfWork3: String?,
        @RequestParam(required = false) periodStart: LocalDate?,
        @RequestParam(required = false) periodEnd: LocalDate?,
        @RequestParam(required = false) preset: SchedulePreset?,
        @RequestParam(required = false) validData: ScheduleValidData?,
        @RequestParam(required = false) branchCode: String?,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?,
    ): ResponseEntity<ByteArray> {
        val sort = resolveSort(sortBy, sortDir)
        val result = adminDisplayWorkScheduleService.exportAllSchedules(
            scope, employeeCode, accountName, accountType, accountStatus, confirmed,
            typeOfWork3, periodStart, periodEnd, preset, validData, resolveBranchCodes(principal, branchCode), sort
        )
        return ExcelResponseUtils.build(result)
    }

    private fun resolveSort(sortBy: String?, sortDir: String?): Sort {
        if (sortBy.isNullOrBlank()) return Sort.unsorted()
        val direction = if (sortDir.equals("asc", ignoreCase = true)) Sort.Direction.ASC else Sort.Direction.DESC
        return Sort.by(direction, sortBy)
    }

    /**
     * 목록/엑셀 지점 필터에 넘길 지점 코드 목록 산출 (대시보드/행사마스터 정합).
     *
     * [WhitelistBranchScopeResolver.effectiveBranchCodes] 결과를 목록 쿼리용 `List<String>?` 로 변환한다.
     * 대시보드와 동일하게 전사 권한자도 34개 화이트리스트로 제한되므로, 선택 없이 조회해도 34개 코드로 좁혀진다:
     * - All (지점 사용자 위임 경로에서만 발생 가능) → null (지점 필터 미적용)
     * - Filtered → 해당 지점 코드 (전사 권한자 선택 없음 = 34개 전체 / 선택 시 그 지점 / 지점 사용자 본인 지점)
     * - NoAccess (선택값이 34개 밖 / 본인 지점 밖 / 권한 지점 없음) → emptyList (매칭 0건, IDOR 차단)
     */
    private fun resolveBranchCodes(principal: WebUserPrincipal, branchCode: String?): List<String>? {
        return when (val result = whitelistBranchScopeResolver.effectiveBranchCodes(principal, branchCode)) {
            is EffectiveBranchResult.All -> null
            is EffectiveBranchResult.Filtered -> result.codes
            is EffectiveBranchResult.NoAccess -> emptyList()
        }
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/confirm")
    fun batchConfirm(
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchConfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.batchConfirm(request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PatchMapping("/unconfirm")
    fun batchUnconfirm(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleBatchConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchUnconfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.batchUnconfirm(scope, principal.requireEmployeeId(), request.ids)
        return ResponseEntity.ok(ApiResponse.success(result, "${result.updatedCount}건이 확정 해제되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/{id}")
    fun getScheduleDetail(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
    ): ResponseEntity<ApiResponse<ScheduleDetailDto>> {
        val result = adminDisplayWorkScheduleService.getScheduleDetail(scope, id)
        return ResponseEntity.ok(ApiResponse.success(result))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PutMapping("/{id}")
    fun updateSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long,
        @Valid @RequestBody request: AdminScheduleUpdateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminDisplayWorkScheduleService.updateSchedule(scope, principal.requireEmployeeId(), id, request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 수정되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @PostMapping("/export")
    fun exportSchedules(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleExportRequest
    ): ResponseEntity<ByteArray> {
        val result = adminDisplayWorkScheduleService.exportSchedules(scope, request.ids)
        return ExcelResponseUtils.build(result.bytes, result.filename)
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @PostMapping("/batch-delete")
    fun batchDelete(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: ScheduleBatchDeleteRequest
    ): ResponseEntity<ApiResponse<ScheduleBatchDeleteResultDto>> {
        val result = adminDisplayWorkScheduleService.batchDelete(scope, principal.requireEmployeeId(), request.ids)
        val message = when {
            result.failedCount == 0 -> "${result.deletedCount}건이 삭제되었습니다"
            result.deletedCount == 0 -> "모든 항목 삭제 실패 (${result.failedCount}건)"
            else -> "${result.deletedCount}건 삭제 / ${result.failedCount}건 실패"
        }
        return ResponseEntity.ok(ApiResponse.success(result, message))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.EDIT)
    @DeleteMapping("/{id}")
    fun deleteSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @PathVariable id: Long
    ): ResponseEntity<ApiResponse<Any?>> {
        adminDisplayWorkScheduleService.deleteSchedule(scope, principal.requireEmployeeId(), id)
        return ResponseEntity.ok(ApiResponse.success(null as Any?, "스케줄이 삭제되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.READ)
    @GetMapping("/template")
    fun downloadTemplate(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
    ): ResponseEntity<ByteArray> {
        val result = adminDisplayWorkScheduleService.generateTemplate(scope, principal.requireEmployeeId())
        return ExcelResponseUtils.build(result.bytes, result.filename)
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping
    fun createSchedule(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @CurrentDataScope scope: DataScope,
        @Valid @RequestBody request: AdminScheduleCreateRequest
    ): ResponseEntity<ApiResponse<ScheduleCreateResultDto>> {
        val result = adminDisplayWorkScheduleService.createSchedule(scope, principal.requireEmployeeId(), request)
        return ResponseEntity.ok(ApiResponse.success(result, "스케줄이 등록되었습니다"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping("/upload")
    fun uploadExcel(
        @CurrentDataScope scope: DataScope,
        @RequestParam("file", required = false) file: MultipartFile?
    ): ResponseEntity<ApiResponse<ScheduleUploadResultDto>> {
        if (file == null) {
            throw ScheduleFileRequiredException()
        }
        val result = adminDisplayWorkScheduleService.uploadAndValidate(scope, file)
        return ResponseEntity.ok(ApiResponse.success(result, "검증 완료"))
    }

    @RequiresSfPermission(entity = "display_work_schedule", operation = SfPermissionOperation.CREATE)
    @PostMapping("/upload/confirm")
    fun confirmUpload(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: ScheduleConfirmRequest
    ): ResponseEntity<ApiResponse<ScheduleConfirmResultDto>> {
        val result = adminDisplayWorkScheduleService.confirmUpload(request.uploadId, principal.requireEmployeeId())
        return ResponseEntity.ok(ApiResponse.success(result, "등록이 완료되었습니다"))
    }
}
