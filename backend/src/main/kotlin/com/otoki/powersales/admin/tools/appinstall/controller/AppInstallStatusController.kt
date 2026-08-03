package com.otoki.powersales.admin.tools.appinstall.controller

import com.otoki.powersales.domain.org.employee.dto.response.AppUninstalledFemaleStaffSummary
import com.otoki.powersales.domain.org.employee.service.EmployeeAppInstallService
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.platform.common.util.excel.ExcelResponseUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 > 대시보드 > 기능 활성화 — 앱 미설치 추정 여사원 조회 / 명단 다운로드 컨트롤러.
 *
 * 사원 개인정보(사번·이름·소속) 명단을 통째로 내려주므로 **시스템 관리자 전용** 으로 가드한다.
 * 같은 탭의 기능 토글과 동일하게, entity CRUD 성격이 아니라 `@RequiresSfPermission` 대신
 * [SystemAdminProfilePolicy.isSystemAdmin] 으로 컨트롤러 내부에서 직접 판정한다.
 */
@RestController
@RequestMapping("/api/v1/admin/tools/app-install")
class AppInstallStatusController(
    private val employeeAppInstallService: EmployeeAppInstallService,
) {

    @GetMapping("/uninstalled-female-staff")
    fun getUninstalledFemaleStaffSummary(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AppUninstalledFemaleStaffSummary>> {
        requireSystemAdmin(principal)
        return ResponseEntity.ok(
            ApiResponse.success(employeeAppInstallService.getUninstalledFemaleStaffSummary()),
        )
    }

    /** 미설치 추정 여사원 명단 엑셀 다운로드 — 사번 / 이름 / 지점명. 집계 수치와 동일 모수. */
    @GetMapping("/uninstalled-female-staff/export")
    fun exportUninstalledFemaleStaff(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ByteArray> {
        requireSystemAdmin(principal)
        return ExcelResponseUtils.build(employeeAppInstallService.exportUninstalledFemaleStaff())
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "PERMISSION_DENIED",
                message = "앱 설치 현황 조회는 시스템 관리자만 사용할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}
