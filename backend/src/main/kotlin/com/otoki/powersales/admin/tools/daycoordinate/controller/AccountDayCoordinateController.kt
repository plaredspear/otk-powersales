package com.otoki.powersales.admin.tools.daycoordinate.controller

import com.otoki.powersales.admin.tools.daycoordinate.dto.AccountDayCoordinateResponse
import com.otoki.powersales.admin.tools.daycoordinate.dto.UpdateAccountDayCoordinateRequest
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverride
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverrideStore
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 개발자 도구 > 대시보드 > 이동매장 좌표 예외 — 요일/좌표 런타임 변경 컨트롤러.
 *
 * 대상 거래처(제이마트 `1015773`) 는 요일에 따라 영업 위치가 바뀌어 출근등록 GPS 검증 기준 좌표를
 * 요일별로 달리 적용한다 ([AccountDayCoordinateOverride]). 이동 요일이나 장소가 바뀔 때 배포 없이
 * 조정할 수 있도록 Redis 오버레이를 노출한다.
 *
 * 로그 레벨/기능 활성화/지점 스코프 방식과 동일하게 entity CRUD 성격이 아니므로
 * `@RequiresSfPermission` 대신 [SystemAdminProfilePolicy.isSystemAdmin] 로 직접 가드한다.
 */
@RestController
@RequestMapping("/api/v1/admin/tools/account-day-coordinate")
class AccountDayCoordinateController(
    private val store: AccountDayCoordinateOverrideStore,
) {

    @GetMapping
    fun get(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AccountDayCoordinateResponse>> {
        requireSystemAdmin(principal)
        return ResponseEntity.ok(ApiResponse.success(currentResponse()))
    }

    @PostMapping
    fun update(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: UpdateAccountDayCoordinateRequest,
    ): ResponseEntity<ApiResponse<AccountDayCoordinateResponse>> {
        requireSystemAdmin(principal)
        val dayOfWeek = AccountDayCoordinateOverride.dayOfWeekOrNull(request.dayOfWeek)
            ?: throw BusinessException(
                errorCode = "INVALID_DAY_OF_WEEK",
                message = "알 수 없는 요일: ${request.dayOfWeek}",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        store.setCoordinate(
            AccountDayCoordinateOverride.DayCoordinate(
                dayOfWeek = dayOfWeek,
                latitude = request.latitude,
                longitude = request.longitude,
                label = request.label.trim(),
            ),
        )
        return ResponseEntity.ok(
            ApiResponse.success(currentResponse(), "이동매장 좌표 예외가 변경되었습니다"),
        )
    }

    /** 저장값을 지워 코드 기본값(수요일 양구점) 으로 되돌린다. */
    @DeleteMapping
    fun reset(
        @AuthenticationPrincipal principal: WebUserPrincipal,
    ): ResponseEntity<ApiResponse<AccountDayCoordinateResponse>> {
        requireSystemAdmin(principal)
        store.reset()
        return ResponseEntity.ok(
            ApiResponse.success(currentResponse(), "이동매장 좌표 예외가 기본값으로 초기화되었습니다"),
        )
    }

    private fun currentResponse(): AccountDayCoordinateResponse {
        val current = store.getCoordinate()
        val default = AccountDayCoordinateOverride.DEFAULT_COORDINATE
        return AccountDayCoordinateResponse(
            externalKey = AccountDayCoordinateOverride.TARGET_EXTERNAL_KEY,
            dayOfWeek = current.dayOfWeek.name,
            latitude = current.latitude,
            longitude = current.longitude,
            label = current.label,
            customized = store.isCustomized(),
            defaultDayOfWeek = default.dayOfWeek.name,
            defaultLatitude = default.latitude,
            defaultLongitude = default.longitude,
            defaultLabel = default.label,
        )
    }

    private fun requireSystemAdmin(principal: WebUserPrincipal) {
        if (!SystemAdminProfilePolicy.isSystemAdmin(principal.profileName)) {
            throw BusinessException(
                errorCode = "PERMISSION_DENIED",
                message = "이동매장 좌표 예외 변경은 시스템 관리자만 사용할 수 있습니다",
                httpStatus = HttpStatus.FORBIDDEN,
            )
        }
    }
}
