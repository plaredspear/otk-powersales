package com.otoki.powersales.admin.tools.daycoordinate.controller

import com.otoki.powersales.admin.tools.daycoordinate.dto.AccountDayCoordinateResponse
import com.otoki.powersales.admin.tools.daycoordinate.dto.GeocodeAccountDayCoordinateRequest
import com.otoki.powersales.admin.tools.daycoordinate.dto.GeocodeAccountDayCoordinateResponse
import com.otoki.powersales.admin.tools.daycoordinate.dto.UpdateAccountDayCoordinateRequest
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverride
import com.otoki.powersales.domain.activity.schedule.policy.AccountDayCoordinateOverrideStore
import com.otoki.powersales.domain.activity.schedule.util.AccountCoordinateParser
import com.otoki.powersales.platform.auth.permission.SystemAdminProfilePolicy
import com.otoki.powersales.platform.auth.web.WebUserPrincipal
import com.otoki.powersales.platform.common.dto.ApiResponse
import com.otoki.powersales.platform.common.exception.BusinessException
import com.otoki.powersales.platform.common.naver.NaverApiException
import com.otoki.powersales.platform.common.naver.NaverGeocodeClient
import jakarta.validation.Valid
import org.slf4j.LoggerFactory
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
    private val naverGeocodeClient: NaverGeocodeClient,
) {

    private val log = LoggerFactory.getLogger(AccountDayCoordinateController::class.java)

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

    /**
     * 주소 → 좌표 변환 (저장하지 않음).
     *
     * 거래처 주소 수정 시의 좌표 재조회([AccountNaverGeocodeService.refreshSingleAccount]) 와 같은
     * Naver Geocode API 를 쓰되, **결과를 저장하지 않고 돌려주기만 한다**. 잘못된 좌표가 저장되면
     * 해당 요일 출근등록이 거리 초과로 전면 실패하므로, 운영자가 변환 결과를 확인한 뒤 저장하도록
     * 변환과 저장을 분리했다.
     */
    @PostMapping("/geocode")
    fun geocode(
        @AuthenticationPrincipal principal: WebUserPrincipal,
        @Valid @RequestBody request: GeocodeAccountDayCoordinateRequest,
    ): ResponseEntity<ApiResponse<GeocodeAccountDayCoordinateResponse>> {
        requireSystemAdmin(principal)
        val address = request.address.trim()

        // client 는 실패 시 예외 대신 null 을 반환한다 (배치 진행을 막지 않기 위한 정책).
        // 여기서는 운영자 대면 단건 변환이라 실패를 그대로 502 로 노출해야 한다.
        val response = naverGeocodeClient.geocode(address) ?: throw NaverApiException()

        val first = response.addresses.firstOrNull()
        // 호출은 성공했으나 그 주소로 좌표를 확정하지 못한 경우 — 오타/미등록 주소가 대부분이라
        // 외부 API 장애(502) 와 구분해 400 으로 돌린다.
        val coords = AccountCoordinateParser.parse(first?.y, first?.x)
        if (coords !is AccountCoordinateParser.Coords.Valid) {
            log.info("ACCOUNT_DAY_COORDINATE_GEOCODE_NOT_FOUND user={} address={}", principal.requireEmployeeId(), address)
            throw BusinessException(
                errorCode = "GEOCODE_ADDRESS_NOT_FOUND",
                message = "해당 주소의 좌표를 찾을 수 없습니다. 주소를 확인해 주세요",
                httpStatus = HttpStatus.BAD_REQUEST,
            )
        }

        log.info(
            "ACCOUNT_DAY_COORDINATE_GEOCODE user={} address={} lat={} lng={}",
            principal.requireEmployeeId(), address, coords.latitude, coords.longitude,
        )
        return ResponseEntity.ok(
            ApiResponse.success(
                GeocodeAccountDayCoordinateResponse(
                    latitude = coords.latitude,
                    longitude = coords.longitude,
                    roadAddress = first?.roadAddress,
                    jibunAddress = first?.jibunAddress,
                ),
            ),
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
